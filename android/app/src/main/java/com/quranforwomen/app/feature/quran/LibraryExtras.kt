package com.quranforwomen.app.feature.quran

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.ReadingVisit
import com.quranforwomen.app.data.Surah
import com.quranforwomen.app.data.arabicIndic
import com.quranforwomen.app.nav.AppTab
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.AppAlert
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.BarIcon
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.SavedEmpty
import com.quranforwomen.app.ui.components.TextAction
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.rowPress
import com.quranforwomen.app.ui.components.scrollSettle
import com.quranforwomen.app.ui.components.shareFile
import com.quranforwomen.app.ui.components.shareText
import com.quranforwomen.app.ui.components.sharedCacheFile
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.Ink
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

// Browse by surah, juz or hizb

internal enum class LibraryBrowseMode(val title: String) {
    Surah("Surah"), Juz("Juz"), Hizb("Hizb");

    fun save(sp: SharedPreferences) { sp.edit().putString(KEY, name.lowercase()).apply() }

    companion object {
        /** Same key and values as the iPhone app ("quran.browse"). */
        private const val KEY = "quran.browse"

        fun read(sp: SharedPreferences): LibraryBrowseMode {
            val raw = runCatching { sp.getString(KEY, null) }.getOrNull()
            return entries.firstOrNull { it.name.lowercase() == raw } ?: Surah
        }
    }
}

/** A surah row's press, plus a long press menu to add it to Favourites or take it out. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryFavouriteHold(surah: Int, onOpen: () -> Unit, content: @Composable (Modifier) -> Unit) {
    val library = LocalGraph.current.library
    val haptics = rememberHaptics()
    var menu by remember { mutableStateOf(false) }
    val favourite = library.isFavourite(surah)
    val label = if (favourite) "Remove from Favourites" else "Add to Favourites"
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val wash = Palette.accentWash.copy(alpha = 0.7f)
    Box {
        content(
            Modifier
                .drawBehind { if (pressed || menu) drawRect(wash) }
                .combinedClickable(
                    interactionSource = source,
                    indication = null,
                    onLongClickLabel = label,
                    onLongClick = { haptics.tap(); menu = true },
                    onClick = onOpen,
                )
                .semantics {
                    customActions = listOf(CustomAccessibilityAction(label) { library.toggleFavourite(surah); true })
                }
        )
        DropdownMenu(
            expanded = menu,
            onDismissRequest = { menu = false },
            shape = RoundedCornerShape(14.dp),
            containerColor = Palette.surface,
        ) {
            DropdownMenuItem(
                text = { Text(label, style = TextStyles.body, color = Palette.textPrimary) },
                leadingIcon = {
                    Icon(if (favourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null, tint = Palette.blushStrong)
                },
                onClick = {
                    menu = false
                    haptics.tick()
                    library.toggleFavourite(surah)
                },
            )
        }
    }
}

/** A juz or hizb row: its number, where it starts, and that surah's Arabic name. The badge pops in as it lands. */
@Composable
internal fun LibraryPartRow(number: Int, title: String, start: VerseRef, popDelayMillis: Int?, modifier: Modifier = Modifier) {
    val quran = LocalGraph.current.quran
    val surah = quran.surah(start.surah)
    val reduce = LocalReduceMotion.current
    var landed by rememberSaveable { mutableStateOf(popDelayMillis == null || reduce) }
    val pop = remember { Animatable(if (landed) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!landed && popDelayMillis != null) {
            delay(popDelayMillis.toLong())
            pop.animateTo(1f, Motion.reward())
            landed = true
        }
    }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$title, starts at ${surah.name} verse ${start.ayah}"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Box(
            Modifier
                .size(38.dp)
                .graphicsLayer {
                    val p = pop.value
                    val s = 0.55f + 0.45f * p
                    scaleX = s; scaleY = s
                    alpha = p.coerceIn(0f, 1f)
                }
                .clip(RoundedCornerShape(11.dp))
                .background(Palette.accentWash),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                number.toString(),
                style = TextStyles.subheadline.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                color = Palette.accent,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = TextStyles.headline, color = Palette.textPrimary)
            Text(
                "${surah.name} · ${start.surah}:${start.ayah}",
                style = TextStyles.subheadline.copy(fontFeatureSettings = "tnum"),
                color = Palette.textSecondary,
            )
        }
        Text(surah.arabicName, style = TextStyle(fontFamily = AppFont.arabic, fontSize = 21.sp), color = Palette.textPrimary)
    }
}

private val quarterMarks = listOf("", "¼", "½", "¾")
private val quarterSpoken = listOf("", "one quarter", "half", "three quarters")

/** One quarter mark of a hizb: the fraction and the verse it starts at. */
@Composable
internal fun LibraryQuarterChip(hizb: Int, quarter: Int, start: VerseRef, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val surahName = LocalGraph.current.quran.surah(start.surah).name
    Box(
        modifier
            .heightIn(min = 44.dp)
            .pressable(scale = 0.94f, onClick = onOpen)
            .clip(CircleShape)
            .background(Palette.accentWash)
            .semantics { contentDescription = "Hizb $hizb, ${quarterSpoken[quarter]}, $surahName verse ${start.ayah}" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${quarterMarks[quarter]} · ${start.surah}:${start.ayah}",
            style = TextStyles.subheadline.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
            color = Palette.accent,
            maxLines = 1,
        )
    }
}

// Reading history

/** The last places she opened in the reader, newest first. Rows deal in, and clearing folds them away. */
@Composable
fun ReadingHistoryScreen() {
    val graph = LocalGraph.current
    val library = graph.library
    val router = LocalRouter.current
    val haptics = rememberHaptics()
    var confirmClear by remember { mutableStateOf(false) }
    val history = library.history

    AppScreen(
        title = "History",
        actions = {
            if (history.isNotEmpty()) TextAction("Clear", onClick = { confirmClear = true })
        },
    ) {
        if (history.isEmpty()) {
            item(key = "empty") {
                SavedEmpty(
                    icon = symbol("clock.arrow.circlepath"),
                    title = "No history",
                    modifier = Modifier.animateItem(),
                    actionLabel = "Open the Quran",
                    onAction = { router.tab = AppTab.quran },
                )
            }
        } else {
            item(key = "history") {
                GroupedSection(Modifier.animateItem().scrollSettle()) {
                    history.forEachIndexed { index, visit ->
                        key(visit.ref.surah) {
                            if (index > 0) RowDivider(70.dp)
                            HistoryRow(
                                visit = visit,
                                modifier = Modifier.dealIn(minOf(index, 8), baseDelay = 80, step = 50),
                                onOpen = { router.push(Route.Reader(visit.ref.surah, visit.ref.ayah)) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AppAlert(
            title = "Clear history?",
            confirmLabel = "Clear History",
            destructive = true,
            onConfirm = {
                confirmClear = false
                library.clearHistory()
                haptics.soft()
            },
            onDismiss = { confirmClear = false },
        )
    }
}

/** One place: the surah, the verse, and when she opened it. */
@Composable
private fun HistoryRow(visit: ReadingVisit, modifier: Modifier, onOpen: () -> Unit) {
    val quran = LocalGraph.current.quran
    val context = LocalContext.current
    val surah = quran.surah(visit.ref.surah)
    val time = historyTimeText(context, visit.date)
    Row(
        modifier
            .fillMaxWidth()
            .rowPress(onOpen)
            .heightIn(min = 56.dp)
            .padding(horizontal = Space.m, vertical = 10.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${surah.name}, verse ${visit.ref.ayah}, $time"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Palette.accentWash),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                surah.id.toString(),
                style = TextStyles.subheadline.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                color = Palette.accent,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(surah.name, style = TextStyles.headline, color = Palette.textPrimary)
            Text(
                "Verse ${visit.ref.ayah} of ${surah.verseCount}",
                style = TextStyles.subheadline.copy(fontFeatureSettings = "tnum"),
                color = Palette.textSecondary,
            )
        }
        Text(time, style = TextStyles.subheadline, color = Palette.textSecondary, maxLines = 1)
    }
}

/** The time alone for today, the date and time for earlier days, in the phone's own format. */
private fun historyTimeText(context: Context, millis: Long): String {
    val date = Date(millis)
    val time = android.text.format.DateFormat.getTimeFormat(context).format(date)
    val day = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return if (day == LocalDate.now()) time
    else "${android.text.format.DateFormat.getMediumDateFormat(context).format(date)}, $time"
}

// Reader menu: favourite and share the whole surah

/** The reader's More menu: add the surah to Favourites, or share the whole surah as text. */
@Composable
internal fun ReaderMoreMenu(surahID: Int) {
    val graph = LocalGraph.current
    val library = graph.library
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    val favourite = library.isFavourite(surahID)

    Box {
        BarIcon(symbol("ellipsis.circle"), "More") { open = true }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(14.dp),
            containerColor = Ink.surface,
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        if (favourite) "Remove from Favourites" else "Add to Favourites",
                        style = TextStyles.body, color = Ink.text,
                    )
                },
                leadingIcon = {
                    Icon(if (favourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null, tint = Ink.accent)
                },
                onClick = {
                    open = false
                    haptics.tick()
                    library.toggleFavourite(surahID)
                },
            )
            DropdownMenuItem(
                text = { Text("Share Surah", style = TextStyles.body, color = Ink.text) },
                leadingIcon = { Icon(symbol("square.and.arrow.up"), null, tint = Ink.accent) },
                onClick = {
                    open = false
                    val surah = graph.quran.surah(surahID)
                    val all = graph.translations.selectedVerses()
                    val translation = all?.getOrNull(surahID - 1)?.takeIf { it.size == surah.verseCount }
                    val translator = if (translation != null) graph.translations.selected.translator else null
                    val basmala = if (surah.showsBasmala) graph.quran.basmala else null
                    scope.launch { context.shareSurah(surah, basmala, translation, translator) }
                },
            )
        }
    }
}

/**
 * A whole surah as plain text: its name, the Basmala where it is shown, every verse in Arabic
 * with its number, the downloaded translation under each verse when there is one, and the reference.
 * Matches the iPhone app's text word for word.
 */
internal fun surahShareText(surah: Surah, basmala: String?, translation: List<String>?, translator: String?): String {
    val parts = mutableListOf("${surah.name} · ${surah.meaning}\n${surah.arabicName}")
    if (basmala != null) parts += basmala
    surah.ayahs.forEachIndexed { index, ayah ->
        var verse = "$ayah ﴿${(index + 1).arabicIndic}﴾"
        if (translation != null) verse += "\n${index + 1}. ${translation[index]}"
        parts += verse
    }
    var reference = "Quran ${surah.id}:1-${surah.verseCount} · ${surah.name}"
    if (translator != null) reference += "\nTranslation: $translator"
    parts += reference
    return parts.joinToString("\n\n")
}

/** Long surahs go as a text file, so the share sheet never hits Android's size limit for text. */
private suspend fun Context.shareSurah(surah: Surah, basmala: String?, translation: List<String>?, translator: String?) {
    val text = withContext(Dispatchers.Default) { surahShareText(surah, basmala, translation, translator) }
    if (text.length <= 50_000) {
        shareText(text, surah.name)
    } else {
        val name = surah.name.filter { it.isLetterOrDigit() || it == '-' }.ifEmpty { "Surah-${surah.id}" }
        val file = withContext(Dispatchers.IO) {
            sharedCacheFile("$name.txt").apply { writeText(text, Charsets.UTF_8) }
        }
        shareFile(file, "text/plain", surah.name)
    }
}
