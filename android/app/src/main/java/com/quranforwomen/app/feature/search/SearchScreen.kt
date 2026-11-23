package com.quranforwomen.app.feature.search

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.ArabicSearch
import com.quranforwomen.app.data.DhikrPhrase
import com.quranforwomen.app.data.Dua
import com.quranforwomen.app.data.JournalEntry
import com.quranforwomen.app.data.QuranStore
import com.quranforwomen.app.data.Surah
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.ArabicText
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.ListRow
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.inkReveal
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.rowPress
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.LocalDark
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Radius
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.pow

/** Recent searches, newest first, kept only on this phone. */
private const val RECENT_KEY = "search.recent"
/** Same key the journal settings use for "Hide page previews". */
private const val HIDE_PREVIEWS_KEY = "journal.hidePreviews"
private const val VERSE_LIMIT = 60

private val suggestions = listOf("Ayat al-Kursi", "2:255", "Patience", "Mercy", "Forgiveness", "Maryam")

private fun <T> searchSpring(response: Float, damping: Float) =
    spring<T>(dampingRatio = damping, stiffness = (2 * PI / response).pow(2).toFloat())

private data class QuranResults(
    val jump: VerseRef? = null,
    val surahs: List<Int> = emptyList(),
    val verses: List<VerseRef> = emptyList(),
) {
    val isEmpty: Boolean get() = jump == null && surahs.isEmpty() && verses.isEmpty()
}

// Folding

private val marks = Regex("\\p{Mn}+")

/** Lowercase without accents, so "Ayat" finds "Āyat". */
private fun plain(text: String): String =
    marks.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "").lowercase(Locale.ROOT)

/** Lowercase and drop punctuation, so "al baqara" matches "Al-Baqarah". */
private fun fold(text: String): String = plain(text).filter { it.isLetterOrDigit() }

/** The last translation searched, folded once and kept, so typing stays quick. */
private object FoldedTranslation {
    private var source: List<List<String>>? = null
    private var folded: List<List<String>> = emptyList()

    @Synchronized
    fun of(verses: List<List<String>>): List<List<String>> {
        if (source !== verses) {
            folded = verses.map { s -> s.map(::plain) }
            source = verses
        }
        return folded
    }
}

/**
 * Finds surahs by name or number, jumps to "2:255", and searches verse text
 * in Arabic (ignoring diacritics) or in the downloaded translation. Runs off the main thread.
 */
private suspend fun searchQuran(query: String, quran: QuranStore, translation: List<List<String>>?): QuranResults =
    withContext(Dispatchers.Default) {
        var jump: VerseRef? = null
        // "2:255" or "2 255"
        val numbers = query.split(':', ' ', '.').filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
        if (numbers.size == 2 && quran.contains(VerseRef(numbers[0], numbers[1]))) {
            jump = VerseRef(numbers[0], numbers[1])
        }

        val folded = fold(query)
        val isArabic = ArabicSearch.containsArabic(query)
        val normalizedQuery = ArabicSearch.normalize(query)
        val asNumber = query.toIntOrNull()
        val surahs = quran.surahs.filter { surah ->
            if (asNumber != null && asNumber == surah.id) return@filter true
            if (isArabic) return@filter normalizedQuery.isNotEmpty() && ArabicSearch.normalize(surah.arabicName).contains(normalizedQuery)
            if (folded.isEmpty()) return@filter false
            fold(surah.name).contains(folded) || fold(surah.meaning).contains(folded)
        }.map { it.id }

        if (query.length < 2 || asNumber != null || jump != null) {
            return@withContext QuranResults(jump, surahs)
        }

        val source = if (isArabic) quran.normalizedVerses() else translation?.let { FoldedTranslation.of(it) }
        if (source == null) return@withContext QuranResults(jump, surahs)

        val needle = if (isArabic) normalizedQuery else plain(query)
        val found = mutableListOf<VerseRef>()
        outer@ for ((s, verses) in source.withIndex()) {
            ensureActive()
            for ((a, text) in verses.withIndex()) {
                if (text.contains(needle)) {
                    found += VerseRef(s + 1, a + 1)
                    if (found.size >= VERSE_LIMIT) break@outer
                }
            }
        }
        QuranResults(jump, surahs, found)
    }

/** The words she typed, lit up in emerald inside a result. */
private fun lightUp(text: String, needle: String, color: Color): AnnotatedString {
    if (needle.isEmpty() || ArabicSearch.containsArabic(needle)) return AnnotatedString(text)
    val n = plain(needle)
    if (n.isEmpty()) return AnnotatedString(text)
    // Fold character by character, remembering where each folded character came from.
    val folded = StringBuilder()
    val origin = ArrayList<Int>()
    text.forEachIndexed { i, c ->
        for (f in plain(c.toString())) {
            folded.append(f)
            origin += i
        }
    }
    val at = folded.indexOf(n)
    if (at < 0) return AnnotatedString(text)
    val start = origin[at]
    val end = origin[at + n.length - 1] + 1
    return buildAnnotatedString {
        append(text)
        addStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold), start, end)
    }
}

/**
 * One search for everything: surahs, verses, duas and her own journal pages.
 * Results deal in as they arrive, the words she typed light up inside each result,
 * and soft placeholder rows stand in while the Quran is searched.
 */
@Composable
fun SearchScreen() {
    val graph = LocalGraph.current
    val router = LocalRouter.current
    val haptics = rememberHaptics()
    val sp = graph.prefs.sp
    val journal = graph.journal

    var query by rememberSaveable { mutableStateOf("") }
    var recentStore by remember { mutableStateOf(sp.getString(RECENT_KEY, "") ?: "") }
    var results by remember { mutableStateOf(QuranResults()) }
    var searching by remember { mutableStateOf(false) }
    val trimmed = query.trim()
    val recent = recentStore.split("\n").filter { it.isNotEmpty() }

    fun saveRecent(value: String) {
        recentStore = value
        sp.edit().putString(RECENT_KEY, value).apply()
    }

    fun addRecent(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val list = listOf(clean) + recent.filter { !it.equals(clean, ignoreCase = true) }
        saveRecent(list.take(6).joinToString("\n"))
    }

    fun pick(text: String) {
        haptics.tick()
        query = text
        addRecent(text)
    }

    // Waits a moment after typing, then searches the Quran off the main thread.
    LaunchedEffect(trimmed) {
        if (trimmed.isEmpty()) {
            results = QuranResults()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        delay(220)
        val found = searchQuran(trimmed, graph.quran, graph.translations.selectedVerses())
        results = found
        searching = false
    }

    val duas: List<Dua> = remember(trimmed) {
        if (trimmed.isEmpty()) return@remember emptyList()
        val isArabic = ArabicSearch.containsArabic(trimmed)
        if (isArabic) {
            val needle = ArabicSearch.normalize(trimmed)
            graph.content.duas.filter { ArabicSearch.normalize(it.arabic).contains(needle) }
        } else {
            val needle = plain(trimmed)
            graph.content.duas.filter { dua ->
                listOf(dua.title, dua.translation, dua.transliteration ?: "", dua.source).any { plain(it).contains(needle) }
            }
        }
    }
    val dhikr: List<DhikrPhrase> = remember(trimmed) {
        if (trimmed.isEmpty()) return@remember emptyList()
        if (ArabicSearch.containsArabic(trimmed)) {
            val needle = ArabicSearch.normalize(trimmed)
            graph.content.dhikr.filter { ArabicSearch.normalize(it.arabic).contains(needle) }
        } else {
            val needle = plain(trimmed)
            graph.content.dhikr.filter { item ->
                listOf(item.transliteration, item.translation, item.source).any { plain(it).contains(needle) }
            }
        }
    }
    val accessible = journal.isAccessible
    val entries = journal.entries
    val pages: List<JournalEntry> = remember(trimmed, accessible, entries) {
        if (!accessible || trimmed.isEmpty()) return@remember emptyList()
        journal.sortedEntries.filter { e ->
            listOf(e.title, e.body, e.gratitude, e.answeredNote).any { it.contains(trimmed, ignoreCase = true) }
        }
    }

    val focus = remember { FocusRequester() }
    // Focus the field once, when the screen first opens, not again when she comes back to it.
    var focused by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (focused) return@LaunchedEffect
        delay(120)
        runCatching { focus.requestFocus() }
        focused = true
    }

    AppScreen(
        title = "Search",
        spacing = Space.l,
        header = {
            Spacer(Modifier.height(Space.s))
            SearchBox(
                value = query,
                onValueChange = { query = it },
                onSearch = { addRecent(trimmed) },
                focus = focus,
            )
        },
    ) {
        if (trimmed.isEmpty()) {
            if (recent.isNotEmpty()) {
                item(key = "recent") {
                    Column(Modifier.animateItem(), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SearchSectionTitle("Recent", Modifier.weight(1f))
                            Box(
                                Modifier
                                    .heightIn(min = 44.dp)
                                    .pressable { saveRecent("") }
                                    .padding(horizontal = Space.xs),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("Clear", style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold), color = Palette.accent)
                            }
                        }
                        GroupedSection {
                            recent.forEachIndexed { index, item ->
                                key(item) {
                                    if (index > 0) RowDivider()
                                    ListRow(
                                        title = item,
                                        icon = symbol("clock.arrow.circlepath"),
                                        showsChevron = false,
                                        modifier = Modifier.dealIn(index, baseDelay = 50, step = 40),
                                        onClick = { pick(item) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item(key = "try") {
                Column(Modifier.animateItem(), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    SearchSectionTitle("Try")
                    SuggestionChips(suggestions) { pick(it) }
                }
            }
        } else {
            val isKursi = trimmed.contains("kursi", ignoreCase = true)
            val nothing = results.isEmpty && duas.isEmpty() && dhikr.isEmpty() && pages.isEmpty()
            // "Ayat al-Kursi" is a name people search for, so map it to 2:255.
            if (isKursi) {
                item(key = "kursi") {
                    JumpRow("Ayat al-Kursi", "Al-Baqarah 2:255", Modifier.animateItem()) {
                        router.push(Route.Reader(2, 255))
                    }
                }
            }
            results.jump?.let { jump ->
                item(key = "jump-${jump.id}") {
                    JumpRow("Go to ${graph.quran.surah(jump.surah).name}", "Verse ${jump.ayah}", Modifier.animateItem()) {
                        router.push(Route.Reader(jump.surah, jump.ayah))
                    }
                }
            }
            if (results.surahs.isNotEmpty()) {
                item(key = "surahs") {
                    GroupedSection(Modifier.animateItem(), title = "Surahs") {
                        results.surahs.forEachIndexed { index, id ->
                            key(id) {
                                if (index > 0) RowDivider(inset = 70.dp)
                                SurahResultRow(
                                    graph.quran.surah(id),
                                    Modifier.dealIn(minOf(index, 8), baseDelay = 20, step = 35),
                                ) { router.push(Route.Reader(id, null)) }
                            }
                        }
                    }
                }
            }
            if (duas.isNotEmpty()) {
                item(key = "duas") {
                    GroupedSection(Modifier.animateItem(), title = "Duas") {
                        duas.take(12).forEachIndexed { index, dua ->
                            key(dua.id) {
                                if (index > 0) RowDivider(inset = Space.m)
                                TwoLineRow(
                                    title = highlighted(dua.title, trimmed),
                                    detail = dua.source,
                                    modifier = Modifier.dealIn(minOf(index, 8), baseDelay = 40, step = 35),
                                ) { router.push(Route.DuaDetail(dua.id)) }
                            }
                        }
                    }
                }
            }
            if (dhikr.isNotEmpty()) {
                item(key = "dhikr") {
                    GroupedSection(Modifier.animateItem(), title = "Dhikr") {
                        dhikr.take(8).forEachIndexed { index, item ->
                            key(item.id) {
                                if (index > 0) RowDivider(inset = Space.m)
                                // Opens the counter on this phrase.
                                TwoLineRow(
                                    title = highlighted(item.transliteration, trimmed),
                                    detail = item.translation,
                                    modifier = Modifier.dealIn(minOf(index, 8), baseDelay = 50, step = 35),
                                ) {
                                    graph.prefs.dhikrSelection = item.id
                                    router.push(Route.Dhikr)
                                }
                            }
                        }
                    }
                }
            }
            if (pages.isNotEmpty()) {
                item(key = "journal") {
                    val hidePreviews = sp.getBoolean(HIDE_PREVIEWS_KEY, false)
                    val format = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
                    GroupedSection(Modifier.animateItem(), title = "Your journal") {
                        pages.take(12).forEachIndexed { index, entry ->
                            key(entry.id) {
                                if (index > 0) RowDivider(inset = Space.m)
                                TwoLineRow(
                                    title = if (hidePreviews) AnnotatedString("Private page") else highlighted(entry.displayTitle, trimmed),
                                    detail = entry.localDate.format(format),
                                    modifier = Modifier.dealIn(minOf(index, 8), baseDelay = 60, step = 35),
                                ) { router.push(Route.JournalEntry(entry.id)) }
                            }
                        }
                    }
                }
            }
            if (results.verses.isNotEmpty()) {
                item(key = "verses") {
                    GroupedSection(Modifier.animateItem(), title = "Verses") {
                        results.verses.forEachIndexed { index, ref ->
                            key(ref.id) {
                                if (index > 0) RowDivider(inset = Space.m)
                                VerseResultRow(
                                    ref, index, trimmed,
                                    Modifier.dealIn(minOf(index, 8), baseDelay = 60, step = 35),
                                ) { router.push(Route.Reader(ref.surah, ref.ayah)) }
                            }
                        }
                    }
                }
            }
            if (searching && nothing) {
                item(key = "placeholder") {
                    GroupedSection(Modifier.animateItem()) {
                        repeat(3) { i ->
                            if (i > 0) RowDivider(inset = Space.m)
                            PlaceholderRow(i)
                        }
                    }
                }
            } else if (!searching && nothing && !isKursi) {
                item(key = "none") { NoResults(trimmed, Modifier.animateItem()) }
            }
        }
    }
}

@Composable
private fun highlighted(text: String, needle: String): AnnotatedString {
    val color = Palette.accent
    return remember(text, needle, color) { lightUp(text, needle, color) }
}

@Composable
private fun SearchSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = TextStyles.title3,
        color = Palette.textPrimary,
        modifier = modifier.padding(bottom = Space.xxs).semantics { heading() },
    )
}

/** The search field, focused as the screen opens. */
@Composable
private fun SearchBox(value: String, onValueChange: (String) -> Unit, onSearch: () -> Unit, focus: FocusRequester) {
    val dark = LocalDark.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (dark) Color(0xFF1C2420) else Color(0xFFEDE9DF))
            .padding(horizontal = Space.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Icon(symbol("magnifyingglass"), null, tint = Palette.textSecondary, modifier = Modifier.size(20.dp))
        val style = TextStyles.body
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f).focusRequester(focus),
            textStyle = style.copy(color = Palette.textPrimary),
            singleLine = true,
            cursorBrush = SolidColor(Palette.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            "Verses, surahs, duas, your journal",
                            style = style,
                            color = Palette.textSecondary.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
            },
        )
        if (value.isNotEmpty()) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .pressable { onValueChange("") }
                    .semantics { contentDescription = "Clear" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(symbol("xmark.circle.fill"), null, tint = Palette.textSecondary.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Suggestion chips that wrap onto new lines. They deal in with a small bounce. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionChips(items: List<String>, onTap: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        items.forEachIndexed { index, item ->
            Box(
                Modifier
                    .dealIn(index, baseDelay = 120, step = 45)
                    .heightIn(min = 44.dp)
                    .pressable(scale = 0.94f) { onTap(item) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .heightIn(min = 36.dp)
                        .clip(CircleShape)
                        .background(Palette.accentWash)
                        .padding(horizontal = Space.m),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(item, style = TextStyles.subheadline.copy(fontWeight = FontWeight.Medium), color = Palette.accent)
                }
            }
        }
    }
}

/** A direct jump to a verse. It pops in with a small spring. */
@Composable
private fun JumpRow(title: String, detail: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val shown = remember { Animatable(if (reduce) 1f else 0f) }
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        launch { shown.animateTo(1f, searchSpring(0.4f, 0.62f)) }
        bounce.snapTo(0.7f)
        bounce.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
    }
    Row(
        modifier
            .graphicsLayer {
                val s = 0.94f + 0.06f * shown.value
                scaleX = s; scaleY = s
                alpha = shown.value.coerceIn(0f, 1f)
            }
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(Radius.tile))
            .background(Palette.accentWash.copy(alpha = 0.6f))
            .rowPress(onClick)
            .semantics(mergeDescendants = true) { }
            .padding(horizontal = Space.m, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Icon(
            symbol("arrow.right.circle.fill"), null, tint = Palette.accent,
            modifier = Modifier.size(28.dp).graphicsLayer { scaleX = bounce.value; scaleY = bounce.value },
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = TextStyles.headline, color = Palette.textPrimary)
            Text(detail, style = TextStyles.subheadline, color = Palette.textSecondary)
        }
    }
}

/** One surah: its number in a badge, name, meaning and verse count, and the Arabic name. */
@Composable
private fun SurahResultRow(surah: Surah, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .rowPress(onClick)
            .semantics(mergeDescendants = true) { }
            .padding(horizontal = Space.m, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Palette.accentWash)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            Text("${surah.id}", style = TextStyles.subheadline.copy(fontWeight = FontWeight.SemiBold), color = Palette.accent)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(surah.name, style = TextStyles.headline, color = Palette.textPrimary)
            Text("${surah.meaning} · ${surah.verseCount} verses", style = TextStyles.subheadline, color = Palette.textSecondary)
        }
        Text(
            surah.arabicName,
            style = TextStyle(fontFamily = AppFont.arabic, fontSize = 21.sp),
            color = Palette.textPrimary,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/** A dua or a journal page: a title with her words lit up, and a small line under it. */
@Composable
private fun TwoLineRow(title: AnnotatedString, detail: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .rowPress(onClick)
            .semantics(mergeDescendants = true) { }
            .padding(horizontal = Space.m, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, style = TextStyles.body, color = Palette.textPrimary)
        Text(detail, style = TextStyles.footnote, color = Palette.textSecondary)
    }
}

/** A verse found by search: reference, two lines of Arabic and of translation. */
@Composable
private fun VerseResultRow(ref: VerseRef, index: Int, needle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val graph = LocalGraph.current
    val translation = graph.translations.text(ref)
    Column(
        modifier
            .fillMaxWidth()
            .rowPress(onClick)
            .semantics(mergeDescendants = true) { }
            .padding(horizontal = Space.m, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Text(
            graph.quran.displayName(ref),
            style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold),
            color = Palette.accent,
        )
        ArabicText(
            graph.quran.text(ref),
            size = 20f,
            maxLines = 2,
            modifier = Modifier.inkReveal(100 + minOf(index, 6) * 50, 800),
        )
        if (translation != null) {
            Text(
                highlighted(translation, needle),
                style = TextStyles.subheadline.copy(fontFamily = AppFont.serif),
                color = Palette.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A soft stand-in row while results load. */
@Composable
private fun PlaceholderRow(index: Int) {
    val reduce = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "placeholder")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            tween(800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
            initialStartOffset = StartOffset(index * 120),
        ),
        label = "pulse",
    )
    val bar = Palette.border
    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (reduce) 1f else pulse }
            .semantics { contentDescription = "Searching" }
            .padding(horizontal = Space.m, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.width(90.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(bar))
        Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)).background(bar))
        Box(Modifier.width((160 + index * 40).dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(bar))
    }
}

/** Nothing found: a small bounce of the glass and a plain line. */
@Composable
private fun NoResults(text: String, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        bounce.snapTo(0.75f)
        bounce.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f))
    }
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = Space.xl)
            .focusIn(50),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Icon(
            symbol("magnifyingglass"), null, tint = Palette.textSecondary,
            modifier = Modifier.size(34.dp).graphicsLayer { scaleX = bounce.value; scaleY = bounce.value },
        )
        Text(
            "No results for “$text”",
            style = TextStyles.headline,
            color = Palette.textPrimary,
            textAlign = TextAlign.Center,
        )
    }
}
