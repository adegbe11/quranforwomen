package com.quranforwomen.app.feature.you

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.Appearance
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.AppSwitch
import com.quranforwomen.app.ui.components.CardSurface
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.LotusMark
import com.quranforwomen.app.ui.components.RollingNumber
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.delay
import java.time.ZonedDateTime

// Public entry points. Destinations.kt calls these.

/** Only her own things. Every setting lives behind one row. */
@Composable
fun YouScreen() {
    val graph = LocalGraph.current
    val library = graph.library
    val router = LocalRouter.current

    AppScreen(title = "You") {
        item(key = "header") {
            YouHeader(
                saved = library.bookmarks.size,
                pages = graph.journal.entries.size,
                finished = library.completedSurahs.size,
                modifier = Modifier.dealIn(0, baseDelay = 50),
            )
        }
        item(key = "saved") {
            GroupedSection(Modifier.focusIn(200), title = "Saved") {
                YouLinkRow("Saved Ayahs", symbol("bookmark"), 0, count = library.bookmarks.size) { router.push(Route.SavedAyahs) }
                RowDivider()
                YouLinkRow("Highlights & Notes", symbol("highlighter"), 1, count = library.annotatedVerses.size) { router.push(Route.Annotations) }
                RowDivider()
                YouLinkRow("Saved Duas", symbol("heart"), 2, count = library.savedDuaIDs.size) { router.push(Route.SavedDuas) }
                RowDivider()
                YouLinkRow("Your Own Words", symbol("pencil"), 3, count = library.personalDuas.size) { router.push(Route.PersonalDuas) }
            }
        }
        item(key = "reading") {
            GroupedSection(Modifier.focusIn(400), title = "Reading") {
                YouLinkRow("Reading Progress", symbol("book"), 4) { router.push(Route.ReadingProgress) }
                RowDivider()
                YouLinkRow(
                    "Reading Plan", symbol("calendar"), 5,
                    value = library.plan?.let { "Day ${it.dayIndex() + 1}" },
                ) { router.push(Route.ReadingPlan) }
                RowDivider()
                YouLinkRow("History", symbol("clock.arrow.circlepath"), 6, count = library.history.size) { router.push(Route.ReadingHistory) }
            }
        }
        item(key = "settings") {
            GroupedSection(Modifier.focusIn(550)) {
                YouLinkRow("Islamic Calendar", symbol("calendar"), 6) { router.push(Route.IslamicCalendar) }
                RowDivider()
                YouLinkRow("Settings", symbol("gearshape"), 6) { router.push(Route.Settings) }
            }
        }
    }
}

/** Every preference in one place. Motion answers her changes: she sees and feels each setting take effect. */
@Composable
fun SettingsScreen() {
    val graph = LocalGraph.current
    val prefs = graph.prefs
    val router = LocalRouter.current
    val context = LocalContext.current

    // The next prayer refreshes on each new minute.
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L - System.currentTimeMillis() % 60_000L + 50L)
            now = ZonedDateTime.now()
        }
    }
    val nextPrayer = graph.prayer.nextPrayer(now)?.let { (prayer, time) ->
        "${prayer.title} ${youTimeText(context, time.hour, time.minute)}"
    }

    AppScreen(title = "Settings") {
        item(key = "appearance") {
            GroupedSection(Modifier.focusIn(50), title = "Appearance") {
                AppearancePicker(selected = prefs.appearance, onSelect = { prefs.appearance = it })
            }
        }
        item(key = "reading") {
            GroupedSection(Modifier.focusIn(200), title = "Reading") {
                SettingRow("Language & Translation", Icons.Outlined.Translate, 0, graph.translations.selected.nativeName) { router.push(Route.Language) }
                RowDivider()
                SettingRow("Text Size & Recitation", symbol("textformat.size"), 1) { router.push(Route.ReadingSettings) }
                RowDivider()
                SettingRow("Downloads", symbol("arrow.down.circle"), 2) { router.push(Route.Downloads) }
            }
        }
        item(key = "daily") {
            GroupedSection(Modifier.focusIn(320), title = "Daily life") {
                SettingRow(
                    "Reminders", Icons.Outlined.Notifications, 3,
                    if (prefs.reminderEnabled || prefs.eveningReminderEnabled) "On" else "Off",
                ) { router.push(Route.Reminder) }
                RowDivider()
                SettingRow("Prayer Times", symbol("clock"), 4, nextPrayer) { router.push(Route.PrayerTimes) }
                RowDivider()
                SettingRow("Qibla", symbol("location.north.line"), 5) { router.push(Route.Qibla) }
            }
        }
        item(key = "app") {
            GroupedSection(Modifier.focusIn(440), title = "App") {
                HapticsRow(Modifier.dealIn(6, baseDelay = 200, step = 50))
                RowDivider()
                SettingRow("Journal", symbol("lock"), 7) { router.push(Route.JournalSettings) }
            }
        }
        item(key = "about") {
            GroupedSection(Modifier.focusIn(550)) {
                SettingRow("Privacy & About", Icons.Outlined.PanTool, 8) { router.push(Route.About) }
            }
        }
    }
}

@Composable
fun SavedAyahsScreen() = YouSavedAyahs()

@Composable
fun AnnotationsScreen() = YouAnnotations()

@Composable
fun ReadingProgressScreen() = YouReadingProgress()

@Composable
fun ReminderScreen() = YouReminders()

@Composable
fun LanguageScreen() = YouLanguage()

@Composable
fun DownloadsScreen() = YouDownloads()

@Composable
fun AboutScreen() = YouAbout()

// You

/** A row on You. Its count rolls up as it lands, or it shows a short value. */
@Composable
private fun YouLinkRow(
    title: String,
    icon: ImageVector,
    index: Int,
    count: Int = 0,
    value: String? = null,
    onClick: () -> Unit,
) {
    YouRow(
        title = title,
        icon = icon,
        onClick = onClick,
        modifier = Modifier.dealIn(index, baseDelay = 200, step = 60),
        landDelayMillis = 250 + index * 60,
    ) {
        if (count > 0) {
            RollingNumber(
                youRolledUp(count, 300 + index * 60),
                style = TextStyles.body,
                color = Palette.textSecondary,
                modifier = Modifier.padding(start = Space.xs),
            )
        } else {
            YouRowValue(value)
        }
    }
}

/** Her own milestones, counted up gently. Not scores, never compared. */
@Composable
private fun YouHeader(saved: Int, pages: Int, finished: Int, modifier: Modifier = Modifier) {
    CardSurface(modifier, padding = Space.xl) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            LotusMark(Modifier.size(width = 34.dp, height = 24.dp), color = Palette.accentFill, strokeWidth = 1.5.dp)
            Text(
                "Your space",
                style = TextStyles.title2.copy(fontFamily = AppFont.serif, fontWeight = FontWeight.SemiBold),
                color = Palette.accentFill,
                modifier = Modifier.semantics { heading() },
            )
        }
        Spacer(Modifier.height(Space.l))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Milestone(saved, if (saved == 1) "verse saved" else "verses saved", 350, Modifier.weight(1f))
            MilestoneDivider()
            Milestone(pages, if (pages == 1) "page written" else "pages written", 450, Modifier.weight(1f))
            MilestoneDivider()
            Milestone(finished, if (finished == 1) "surah finished" else "surahs finished", 550, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Milestone(value: Int, label: String, delayMillis: Int, modifier: Modifier) {
    Column(
        modifier.semantics(mergeDescendants = true) { contentDescription = "$value $label" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        RollingNumber(
            youRolledUp(value, delayMillis),
            style = TextStyle(fontFamily = AppFont.serif, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Light),
            color = Palette.textPrimary,
        )
        Text(label, style = TextStyles.caption, color = Palette.textSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MilestoneDivider() {
    Box(Modifier.width(0.5.dp).height(34.dp).background(Palette.border))
}

// Settings

@Composable
private fun SettingRow(title: String, icon: ImageVector, index: Int, value: String? = null, onClick: () -> Unit) {
    YouRow(
        title = title,
        icon = icon,
        onClick = onClick,
        modifier = Modifier.dealIn(index, baseDelay = 200, step = 50),
        landDelayMillis = 250 + index * 50,
    ) { YouRowValue(value) }
}

/** A small phone preview of each appearance. The selection ring glides between them. */
@Composable
private fun AppearancePicker(selected: Appearance, onSelect: (Appearance) -> Unit) {
    val haptics = rememberHaptics()
    val options = Appearance.entries
    val gap = Space.s
    BoxWithConstraints(Modifier.fillMaxWidth().padding(Space.m)) {
        val tileW = (maxWidth - gap * (options.size - 1)) / options.size
        val index = options.indexOf(selected).coerceAtLeast(0)
        val ringX by animateDpAsState(
            (tileW + gap) * index,
            spring(dampingRatio = 1f, stiffness = 160f),
            label = "appearanceRing",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            options.forEachIndexed { i, option ->
                AppearanceTile(
                    option = option,
                    isSelected = option == selected,
                    modifier = Modifier.width(tileW).dealIn(i, baseDelay = 100, step = 60),
                ) {
                    if (option != selected) {
                        haptics.tick()
                        onSelect(option)
                    }
                }
            }
        }
        Box(
            Modifier
                .offset(x = ringX - 4.dp, y = (-4).dp)
                .size(width = tileW + 8.dp, height = 86.dp)
                .border(2.5.dp, Palette.accent, RoundedCornerShape(14.dp))
        )
    }
}

@Composable
private fun AppearanceTile(option: Appearance, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val light = Color(0xFFFAF7F0)
    val dark = Color(0xFF0E1311)
    val background = when (option) {
        Appearance.light -> Brush.horizontalGradient(listOf(light, light))
        Appearance.dark -> Brush.horizontalGradient(listOf(dark, dark))
        Appearance.system -> Brush.horizontalGradient(0f to light, 0.5f to light, 0.5f to dark, 1f to dark)
    }
    val line = if (option == Appearance.dark) Color(0xFF27302C) else Color(0xFFE7E5DD)
    Column(
        modifier
            .pressable(scale = 0.95f, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "${option.title} appearance"
                selected = isSelected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(78.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(background)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(Modifier.size(width = 26.dp, height = 5.dp).clip(CircleShape).background(Palette.accentFill))
            Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(3.dp)).background(line))
            Box(Modifier.size(width = 34.dp, height = 6.dp).clip(RoundedCornerShape(3.dp)).background(line.copy(alpha = 0.6f)))
        }
        Text(
            option.title,
            style = TextStyles.footnote.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
            color = if (isSelected) Palette.accent else Palette.textSecondary,
            maxLines = 1,
        )
    }
}

/** Turning haptics on plays a sample tap, so she feels what she enabled. */
@Composable
private fun HapticsRow(modifier: Modifier = Modifier) {
    val prefs = LocalGraph.current.prefs
    val haptics = rememberHaptics()
    var sample by remember { mutableIntStateOf(0) }
    val bounce = youBounce(sample)
    val source = remember { MutableInteractionSource() }

    fun set(on: Boolean) {
        prefs.hapticsOn = on
        if (on) {
            sample++
            haptics.tap()
        }
    }

    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = prefs.hapticsOn,
                interactionSource = source,
                indication = null,
                role = Role.Switch,
                onValueChange = ::set,
            )
            .padding(horizontal = Space.m, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(26.dp), contentAlignment = Alignment.Center) {
            Icon(
                symbol("hand.tap"), null, tint = Palette.accent,
                modifier = Modifier.size(21.dp).graphicsLayer { scaleX = bounce; scaleY = bounce },
            )
        }
        Spacer(Modifier.width(Space.m))
        Text("Haptics", style = TextStyles.body, color = Palette.textPrimary, modifier = Modifier.weight(1f))
        AppSwitch(prefs.hapticsOn, ::set)
    }
}
