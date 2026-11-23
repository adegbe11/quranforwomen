package com.quranforwomen.app.feature.you

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranforwomen.app.BuildConfig
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.Notifications
import com.quranforwomen.app.data.ReminderKind
import com.quranforwomen.app.data.ReminderScheduler
import com.quranforwomen.app.data.TranslationOption
import com.quranforwomen.app.data.TranslationStore
import com.quranforwomen.app.ui.components.AppAlert
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.AppSwitch
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.LotusMark
import com.quranforwomen.app.ui.components.LotusPart
import com.quranforwomen.app.ui.components.PetalBurst
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.RowIcon
import com.quranforwomen.app.ui.components.TextAction
import com.quranforwomen.app.ui.components.TintedButton
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.lotusPath
import com.quranforwomen.app.ui.components.openAppSettings
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.rememberNotificationRequest
import com.quranforwomen.app.ui.components.rowPress
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Radius
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.delay
import kotlin.math.min

// Reminders

/**
 * Her two gentle reminders. Each section deals in, and when notifications are off
 * for the app, the note about it slides down into place.
 */
@Composable
internal fun YouReminders() {
    val context = LocalContext.current
    val prefs = LocalGraph.current.prefs
    val reduce = LocalReduceMotion.current
    var denied by remember {
        mutableStateOf((prefs.reminderEnabled || prefs.eveningReminderEnabled) && !Notifications.allowed(context))
    }
    // Coming back from the phone's settings with notifications allowed clears the note.
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        if (Notifications.allowed(context)) denied = false
        onPauseOrDispose { }
    }

    AppScreen(title = "Reminders", largeTitle = false) {
        item(key = "quran") {
            ReminderSection("Quran moment", ReminderKind.quran, { denied = it }, Modifier.dealIn(0, baseDelay = 120))
        }
        item(key = "evening") {
            ReminderSection("Evening remembrance", ReminderKind.evening, { denied = it }, Modifier.dealIn(1, baseDelay = 120))
        }
        item(key = "denied") {
            AnimatedVisibility(
                visible = denied,
                enter = if (reduce) fadeIn() else slideInVertically(Motion.smooth()) { -it / 2 } + fadeIn(Motion.smooth()),
                exit = if (reduce) fadeOut() else slideOutVertically(Motion.smooth()) { -it / 2 } + fadeOut(Motion.smooth()),
            ) {
                GroupedSection {
                    Column(
                        Modifier.fillMaxWidth().padding(Space.m),
                        verticalArrangement = Arrangement.spacedBy(Space.s),
                    ) {
                        Text("Notifications are off.", style = TextStyles.subheadline, color = Palette.textSecondary)
                        TintedButton("Open Settings", { context.openAppSettings() }, fullWidth = false)
                    }
                }
            }
        }
    }
}

/**
 * One reminder. Its icon bounces as she turns it on, the time row opens, and a sample
 * notification drops in. Changing the time rolls the time shown on the sample.
 */
@Composable
private fun ReminderSection(title: String, kind: ReminderKind, onDenied: (Boolean) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val prefs = LocalGraph.current.prefs
    val haptics = rememberHaptics()
    val reduce = LocalReduceMotion.current
    var picking by remember { mutableStateOf(false) }

    val isOn = if (kind == ReminderKind.quran) prefs.reminderEnabled else prefs.eveningReminderEnabled
    val minutes = if (kind == ReminderKind.quran) prefs.reminderMinutes else prefs.eveningReminderMinutes

    fun setOn(on: Boolean) {
        if (kind == ReminderKind.quran) prefs.reminderEnabled = on else prefs.eveningReminderEnabled = on
    }

    fun minutesNow() = if (kind == ReminderKind.quran) prefs.reminderMinutes else prefs.eveningReminderMinutes

    val request = rememberNotificationRequest { ok ->
        val wasOn = if (kind == ReminderKind.quran) prefs.reminderEnabled else prefs.eveningReminderEnabled
        if (ok) {
            ReminderScheduler.enable(context, minutesNow(), kind)
            if (!wasOn) haptics.tap()
        }
        setOn(ok)
        onDenied(!ok)
    }

    fun setEnabled(on: Boolean) {
        if (on) {
            request()
        } else {
            ReminderScheduler.disable(context, kind)
            setOn(false)
        }
    }

    val iconBounce = youBounce(isOn)
    val source = remember { MutableInteractionSource() }

    GroupedSection(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = isOn, interactionSource = source, indication = null, role = Role.Switch, onValueChange = ::setEnabled)
                .padding(horizontal = Space.m, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(26.dp), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = isOn,
                    transitionSpec = {
                        if (reduce) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        else (scaleIn(Motion.reward(), initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut())
                    },
                    label = "reminderIcon",
                ) { on ->
                    Icon(
                        reminderIcon(kind, on), null, tint = Palette.accent,
                        modifier = Modifier.size(21.dp).graphicsLayer { scaleX = iconBounce; scaleY = iconBounce },
                    )
                }
            }
            Spacer(Modifier.width(Space.m))
            Text(title, style = TextStyles.body, color = Palette.textPrimary, modifier = Modifier.weight(1f))
            AppSwitch(isOn, ::setEnabled)
        }

        AnimatedVisibility(
            visible = isOn,
            enter = if (reduce) fadeIn() else expandVertically(Motion.gentle()) + fadeIn(Motion.gentle()),
            exit = if (reduce) fadeOut() else shrinkVertically(Motion.gentle()) + fadeOut(Motion.gentle()),
        ) {
            Column {
                RowDivider()
                val timeText = youTimeText(context, minutes / 60, minutes % 60)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .rowPress { picking = true }
                        .padding(horizontal = Space.m, vertical = 6.dp)
                        .semantics(mergeDescendants = true) { role = Role.Button },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(26.dp), contentAlignment = Alignment.Center) {
                        Icon(symbol("clock"), null, tint = Palette.accent, modifier = Modifier.size(21.dp))
                    }
                    Spacer(Modifier.width(Space.m))
                    Text("Time", style = TextStyles.body, color = Palette.textPrimary, modifier = Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Palette.accentWash)
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                    ) {
                        Text(timeText, style = TextStyles.body, color = Palette.accent)
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Palette.accentWash.copy(alpha = 0.5f))
                        .padding(Space.m)
                ) {
                    ReminderSample(kind, timeText, minutes)
                }
            }
        }
    }

    if (picking) {
        ReminderTimeDialog(
            minutes = minutes,
            onDismiss = { picking = false },
            onPick = { picked ->
                if (kind == ReminderKind.quran) prefs.reminderMinutes = picked else prefs.eveningReminderMinutes = picked
                setEnabled(true)
            },
        )
    }
}

private fun reminderIcon(kind: ReminderKind, on: Boolean): ImageVector = when (kind) {
    ReminderKind.quran -> if (on) Icons.AutoMirrored.Filled.MenuBook else symbol("book")
    ReminderKind.evening -> if (on) Icons.Filled.NightsStay else symbol("moon.stars")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(minutes: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = minutes / 60,
        initialMinute = minutes % 60,
        is24Hour = android.text.format.DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.surface,
        shape = RoundedCornerShape(24.dp),
        confirmButton = {
            TextAction("Done", { onPick(state.hour * 60 + state.minute); onDismiss() }, bold = true)
        },
        dismissButton = { TextAction("Cancel", onDismiss) },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = Palette.accentWash,
                    clockDialSelectedContentColor = Palette.onAccentFill,
                    clockDialUnselectedContentColor = Palette.textPrimary,
                    selectorColor = Palette.accentFill,
                    containerColor = Palette.surface,
                    periodSelectorBorderColor = Palette.border,
                    periodSelectorSelectedContainerColor = Palette.accentWash,
                    periodSelectorUnselectedContainerColor = Color.Transparent,
                    periodSelectorSelectedContentColor = Palette.accent,
                    periodSelectorUnselectedContentColor = Palette.textSecondary,
                    timeSelectorSelectedContainerColor = Palette.accentWash,
                    timeSelectorUnselectedContainerColor = Palette.surfaceSunken,
                    timeSelectorSelectedContentColor = Palette.accent,
                    timeSelectorUnselectedContentColor = Palette.textPrimary,
                ),
            )
        },
    )
}

/**
 * What the reminder will look like. It drops in with a small settle,
 * and gives a tiny nudge with the time rolling when she changes the time.
 */
@Composable
private fun ReminderSample(kind: ReminderKind, timeText: String, minutes: Int) {
    val reduce = LocalReduceMotion.current
    var shown by remember { mutableStateOf(reduce) }
    LaunchedEffect(Unit) {
        if (!shown) {
            delay(150)
            shown = true
        }
    }
    val appear by animateFloatAsState(
        if (shown) 1f else 0f,
        spring(dampingRatio = 0.68f, stiffness = 160f),
        label = "sampleIn",
    )
    val nudge = remember { Animatable(1f) }
    val last = remember { intArrayOf(minutes) }
    LaunchedEffect(minutes) {
        if (last[0] == minutes) return@LaunchedEffect
        last[0] = minutes
        if (reduce) return@LaunchedEffect
        nudge.animateTo(1.025f, tween(120))
        nudge.animateTo(1f, spring(dampingRatio = 1f, stiffness = 300f))
    }
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val a = appear
                translationY = (1f - a) * -24.dp.toPx()
                alpha = a.coerceIn(0f, 1f)
                val s = (0.96f + 0.04f * a) * nudge.value
                scaleX = s; scaleY = s
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
            .shadow(6.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.12f), spotColor = Color.Black.copy(alpha = 0.12f))
            .clip(shape)
            .background(Palette.surface.copy(alpha = 0.94f))
            .padding(10.dp)
            .clearAndSetSemantics {
                contentDescription = "Sample reminder at $timeText: ${kind.title}. ${kind.body}"
            },
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Palette.accentFill)
                .padding(7.dp)
        ) {
            LotusMark(Modifier.fillMaxSize(), color = Color.White, strokeWidth = 1.2.dp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    kind.title,
                    style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold),
                    color = Palette.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(Space.xs))
                AnimatedContent(
                    targetState = timeText,
                    transitionSpec = {
                        if (reduce) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        else (slideInVertically(spring(dampingRatio = 0.8f, stiffness = 500f)) { it } + fadeIn()) togetherWith
                            (slideOutVertically(spring(dampingRatio = 0.8f, stiffness = 500f)) { -it } + fadeOut())
                    },
                    label = "sampleTime",
                ) { text ->
                    Text(text, style = youCaption2, color = Palette.textSecondary)
                }
            }
            Text(kind.body, style = TextStyles.footnote, color = Palette.textPrimary)
        }
    }
}

// Language

/**
 * Translation choices deal in one after another. Choosing one moves the check mark
 * to it, and a download in progress fades in and out beside it.
 */
@Composable
internal fun YouLanguage() {
    val translations = LocalGraph.current.translations
    AppScreen(title = "Language & Translation", largeTitle = false, spacing = Space.xs) {
        itemsIndexed(TranslationOption.all, key = { _, option -> option.id }) { index, option ->
            SelectableRow(
                isSelected = translations.selectedID == option.id,
                onClick = { translations.selectedID = option.id },
                modifier = Modifier.dealIn(min(index, 8), baseDelay = 120, step = 50),
            ) {
                OptionLabel(option, Modifier.weight(1f))
                AnimatedVisibility(
                    visible = translations.state(option.id) == TranslationStore.State.Downloading,
                    enter = scaleIn(Motion.gentle()) + fadeIn(Motion.gentle()),
                    exit = scaleOut(Motion.gentle()) + fadeOut(Motion.gentle()),
                ) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp).semantics { contentDescription = "Downloading" },
                        color = Palette.accent,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
    }
}

/** A full width choice row with a check mark. */
@Composable
private fun SelectableRow(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable RowScope.() -> Unit,
) {
    val haptics = rememberHaptics()
    val reduce = LocalReduceMotion.current
    val bg by animateColorAsState(if (isSelected) Palette.accentWash else Palette.surface, Motion.gentle(), label = "selectBg")
    Row(
        modifier
            .fillMaxWidth()
            .pressable {
                if (!isSelected) haptics.tick()
                onClick()
            }
            .clip(RoundedCornerShape(Radius.tile))
            .background(bg)
            .padding(horizontal = Space.m, vertical = 14.dp)
            .semantics(mergeDescendants = true) { selected = isSelected },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        label()
        val bounce = youBounce(isSelected)
        AnimatedContent(
            targetState = isSelected,
            transitionSpec = {
                if (reduce) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                else (scaleIn(Motion.reward(), initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut())
            },
            label = "check",
        ) { on ->
            Icon(
                if (on) Icons.Filled.CheckCircle else Icons.Outlined.Circle, null,
                tint = if (on) Palette.accent else Palette.border,
                modifier = Modifier.size(24.dp).graphicsLayer {
                    val s = if (on) bounce else 1f
                    scaleX = s; scaleY = s
                },
            )
        }
    }
}

@Composable
private fun OptionLabel(option: TranslationOption, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(option.nativeName, style = TextStyles.headline, color = Palette.textPrimary)
        Text(
            if (option.language == option.nativeName) option.translator else "${option.language} · ${option.translator}",
            style = TextStyles.subheadline,
            color = Palette.textSecondary,
        )
    }
}

// Downloads

/** Translations she can keep on this phone. Rows deal in, and each status changes with a soft pop. */
@Composable
internal fun YouDownloads() {
    AppScreen(title = "Downloads", largeTitle = false) {
        item(key = "arabic") {
            GroupedSection(Modifier.focusIn(50)) {
                YouStatRow("Arabic Quran", "Included")
            }
        }
        item(key = "translations") {
            GroupedSection(title = "Translations") {
                TranslationOption.all.forEachIndexed { index, option ->
                    if (index > 0) RowDivider(inset = Space.m)
                    DownloadRow(option, Modifier.dealIn(min(index, 8), baseDelay = 150, step = 50))
                }
            }
        }
        item(key = "transliteration") { YouTransliterationDownloads() }
        item(key = "recitations") { YouRecitationDownloads() }
    }
}

/**
 * One translation. A finished download lands with a check mark that pops in,
 * a small burst of petals and a gentle tap. Swipe left to delete one she is not using.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadRow(option: TranslationOption, modifier: Modifier) {
    val context = LocalContext.current
    val translations = LocalGraph.current.translations
    val haptics = rememberHaptics()
    val reduce = LocalReduceMotion.current
    val state = translations.state(option.id)
    val deletable = state == TranslationStore.State.Ready && option.id != translations.selectedID
    val canDelete by rememberUpdatedState(deletable)

    var burst by remember { mutableIntStateOf(0) }
    val previous = remember { arrayOf(state) }
    LaunchedEffect(state) {
        val old = previous[0]
        previous[0] = state
        if (old == state) return@LaunchedEffect
        if (state == TranslationStore.State.Downloading) haptics.soft()
        if (old == TranslationStore.State.Downloading && state == TranslationStore.State.Ready) {
            haptics.success()
            burst++
        }
    }

    val swipe = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && canDelete) translations.delete(option.id)
            false
        },
    )
    val red = youDanger

    SwipeToDismissBox(
        state = swipe,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = deletable,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(red).padding(end = Space.l),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("Delete", style = TextStyles.headline, color = Color.White)
            }
        },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Palette.surface)
                .heightIn(min = 56.dp)
                .padding(horizontal = Space.m, vertical = 10.dp)
                .semantics {
                    if (deletable) {
                        customActions = listOf(CustomAccessibilityAction("Delete") { translations.delete(option.id); true })
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OptionLabel(option, Modifier.weight(1f))
            Spacer(Modifier.width(Space.s))
            Box(contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        if (reduce) fadeIn() togetherWith fadeOut()
                        else (scaleIn(Motion.reward(), initialScale = 0.5f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.5f) + fadeOut())
                    },
                    contentAlignment = Alignment.CenterEnd,
                    label = "downloadState",
                ) { s ->
                    when (s) {
                        TranslationStore.State.Ready -> Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(symbol("checkmark.circle.fill"), "Downloaded", tint = Palette.accent, modifier = Modifier.size(22.dp))
                            translations.sizeOnDisk(option.id)?.let { size ->
                                Text(
                                    android.text.format.Formatter.formatShortFileSize(context, size),
                                    style = TextStyles.caption,
                                    color = Palette.textSecondary,
                                )
                            }
                        }
                        TranslationStore.State.Downloading -> CircularProgressIndicator(
                            Modifier.size(22.dp).semantics { contentDescription = "Downloading" },
                            color = Palette.accent,
                            strokeWidth = 2.dp,
                        )
                        TranslationStore.State.Failed -> TextAction("Retry", { translations.downloadAsync(option.id) })
                        TranslationStore.State.NotDownloaded -> Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .pressable(scale = 0.9f) { translations.downloadAsync(option.id) }
                                .semantics { contentDescription = "Download ${option.language}" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(symbol("arrow.down.circle"), null, tint = Palette.accent, modifier = Modifier.size(26.dp))
                        }
                    }
                }
                PetalBurst(trigger = burst)
            }
        }
    }
}

// Privacy and about

private val privacyPoints: List<Pair<ImageVector, String>> by lazy {
    listOf(
        Icons.Outlined.PhoneAndroid to "Your data stays on this phone",
        symbol("location") to "Location is only used for prayer times and the Qibla",
        Icons.Outlined.PersonOff to "No account",
        Icons.Outlined.BarChart to "No ads or tracking",
    )
}

private val sources = listOf(
    "Arabic Quran text" to "Tanzil Project, Uthmani",
    "Translations" to "AlQuran Cloud",
    "Recitation" to "Islamic Network",
    "Duas and dhikr" to "Quran and hadith",
    "Arabic font" to "Amiri Quran, SIL OFL",
    "Prayer times" to "Calculated on device",
)

/** Privacy and sources. The lotus draws itself, the name comes into focus, and each point deals in after it. */
@Composable
internal fun YouAbout() {
    val graph = LocalGraph.current
    val haptics = rememberHaptics()
    val reduce = LocalReduceMotion.current
    var confirmErase by remember { mutableStateOf(false) }
    var erased by rememberSaveable { mutableStateOf(false) }
    val red = youDanger
    val trashBounce = youBounce(erased)

    AppScreen(title = "Privacy & About", largeTitle = false) {
        item(key = "header") { AboutHeader() }
        item(key = "privacy") {
            GroupedSection(Modifier.focusIn(300), title = "Your privacy") {
                privacyPoints.forEachIndexed { index, (icon, text) ->
                    if (index > 0) RowDivider()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .dealIn(index, baseDelay = 350, step = 60)
                            .padding(horizontal = Space.m, vertical = Space.s),
                        horizontalArrangement = Arrangement.spacedBy(Space.s),
                    ) {
                        RowIcon(icon)
                        Text(
                            text, style = TextStyles.subheadline, color = Palette.textPrimary,
                            modifier = Modifier.weight(1f).padding(top = 5.dp),
                        )
                    }
                }
            }
        }
        item(key = "sources") {
            GroupedSection(Modifier.focusIn(450), title = "Sources") {
                sources.forEachIndexed { index, (title, detail) ->
                    if (index > 0) RowDivider(inset = Space.m)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .dealIn(index, baseDelay = 500, step = 50)
                            .padding(horizontal = Space.m, vertical = Space.s)
                            .semantics(mergeDescendants = true) {},
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(title, style = TextStyles.subheadline.copy(fontWeight = FontWeight.SemiBold), color = Palette.textPrimary)
                        Text(detail, style = TextStyles.subheadline, color = Palette.textSecondary)
                    }
                }
            }
        }
        item(key = "erase") {
            GroupedSection(Modifier.focusIn(550)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .rowPress { confirmErase = true }
                        .padding(horizontal = Space.m)
                        .semantics(mergeDescendants = true) { role = Role.Button },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(26.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            symbol("trash"), null, tint = red,
                            modifier = Modifier.size(21.dp).graphicsLayer { scaleX = trashBounce; scaleY = trashBounce },
                        )
                    }
                    Spacer(Modifier.width(Space.m))
                    Text("Erase My Data", style = TextStyles.body, color = red)
                }
                AnimatedVisibility(
                    visible = erased,
                    enter = if (reduce) fadeIn() else expandVertically(Motion.gentle()) + fadeIn(Motion.gentle()),
                    exit = fadeOut(),
                ) {
                    Column {
                        RowDivider()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Space.m, vertical = Space.s)
                                .semantics(mergeDescendants = true) {},
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Space.xs),
                        ) {
                            Icon(symbol("checkmark.circle.fill"), null, tint = Palette.accent, modifier = Modifier.size(18.dp))
                            Text("Data erased", style = TextStyles.subheadline, color = Palette.accent)
                        }
                    }
                }
            }
        }
    }

    if (confirmErase) {
        AppAlert(
            title = "Erase all your data on this phone?",
            message = "This cannot be undone.",
            confirmLabel = "Erase My Data",
            destructive = true,
            onConfirm = {
                graph.library.eraseAll()
                graph.journal.eraseAll()
                graph.dhikrExtras.eraseAll()
                graph.planHistory.eraseAll()
                graph.challenges.eraseAll()
                graph.fasting.eraseAll()
                graph.missedPrayers.eraseAll()
                graph.prayer.refreshNotifications()
                erased = true
                haptics.success()
            },
            onDismiss = { confirmErase = false },
        )
    }
}

/** The lotus draws itself line by line, then the name and version come into focus. */
@Composable
private fun AboutHeader() {
    val reduce = LocalReduceMotion.current
    var drawnOnce by rememberSaveable { mutableStateOf(false) }
    val drawn = remember { Animatable(if (drawnOnce || reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (drawn.value < 1f) drawn.animateTo(1f, tween(1400, delayMillis = 100, easing = FastOutSlowInEasing))
        drawnOnce = true
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(Palette.surface)
            .padding(horizontal = Space.m, vertical = Space.l)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Box(
            Modifier
                .graphicsLayer {
                    val s = 0.92f + 0.08f * drawn.value
                    scaleX = s; scaleY = s
                }
                .clip(CircleShape)
                .background(Palette.accentWash)
                .padding(Space.m)
        ) {
            DrawnLotus(drawn.value, Palette.accent, Modifier.size(width = 54.dp, height = 50.dp))
        }
        Text(
            "Quran for Women",
            style = TextStyles.title3.copy(fontFamily = AppFont.serif),
            color = Palette.textPrimary,
            modifier = Modifier.focusIn(450),
        )
        Text(
            "Version ${BuildConfig.VERSION_NAME}",
            style = TextStyles.subheadline,
            color = Palette.textSecondary,
            modifier = Modifier.focusIn(600),
        )
    }
}

/** The lotus mark traced up to a fraction of its length, part after part. */
@Composable
private fun DrawnLotus(progress: Float, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val rect = Rect(Offset.Zero, size)
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val parts = listOf(LotusPart.Center, LotusPart.Left, LotusPart.Right, LotusPart.Water).map { lotusPath(it, rect) }
        if (progress >= 1f) {
            parts.forEach { drawPath(it, color, style = stroke) }
            return@Canvas
        }
        val measure = PathMeasure()
        val lengths = parts.map { measure.setPath(it, false); measure.length }
        var remaining = lengths.sum() * progress.coerceIn(0f, 1f)
        parts.forEachIndexed { i, path ->
            if (remaining <= 0f) return@Canvas
            val take = min(remaining, lengths[i])
            measure.setPath(path, false)
            val segment = Path()
            measure.getSegment(0f, take, segment, true)
            drawPath(segment, color, style = stroke)
            remaining -= take
        }
    }
}
