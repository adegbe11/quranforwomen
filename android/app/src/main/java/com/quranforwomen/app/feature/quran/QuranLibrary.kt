package com.quranforwomen.app.feature.quran

import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.ArabicSearch
import com.quranforwomen.app.data.QuranStore
import com.quranforwomen.app.data.Surah
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.ArabicText
import com.quranforwomen.app.ui.components.CardSurface
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.ListRow
import com.quranforwomen.app.ui.components.RollingNumber
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.SavedEmpty
import com.quranforwomen.app.ui.components.SearchField
import com.quranforwomen.app.ui.components.SegmentedControl
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rowPress
import com.quranforwomen.app.ui.components.scrollSettle
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Radius
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.Normalizer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * The Quran tab. Motion: the place she left off comes into focus and its line fills
 * to where she stopped, the first surahs are dealt in, and rows settle as they scroll.
 * Browse and search share one scrolling page and crossfade between each other.
 */
@Composable
internal fun QuranLibraryContent() {
    val graph = LocalGraph.current
    val quran = graph.quran
    val library = graph.library
    val translations = graph.translations
    val router = LocalRouter.current

    var query by rememberSaveable { mutableStateOf("") }
    val trimmed = query.trim()
    var results by remember { mutableStateOf(LibrarySearchResults()) }
    var isSearching by remember { mutableStateOf(false) }
    val sp = graph.prefs.sp
    var browseMode by remember { mutableStateOf(LibraryBrowseMode.read(sp)) }

    LaunchedEffect(trimmed) {
        if (trimmed.isEmpty()) {
            results = LibrarySearchResults()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(220)
        val found = runLibrarySearch(trimmed, quran, translations.selectedVerses())
        results = found
        isSearching = false
    }

    AppScreen(
        title = "Quran",
        spacing = 0.dp,
        header = {
            Spacer(Modifier.height(Space.s))
            SearchField(query, { query = it }, "Surah, verse, or words")
            Spacer(Modifier.height(Space.l))
        },
    ) {
        if (trimmed.isEmpty()) {
            browse(
                quran, library.lastRead, library.completedSurahs, library.favouriteSurahs,
                mode = browseMode,
                onMode = { browseMode = it; it.save(sp) },
                onOpen = { router.push(it) },
            )
        } else {
            searchResults(trimmed, quran, results, isSearching, library.completedSurahs, onOpen = { router.push(it) })
        }
    }
}

private fun LazyListScope.browse(
    quran: QuranStore,
    lastRead: VerseRef?,
    completed: Set<Int>,
    favourites: List<Int>,
    mode: LibraryBrowseMode,
    onMode: (LibraryBrowseMode) -> Unit,
    onOpen: (Route) -> Unit,
) {
    item(key = "mushaf") {
        LibraryMushafCard(
            onOpen = { onOpen(Route.Mushaf(null)) },
            modifier = Modifier.animateItem().padding(bottom = Space.xl).focusIn(20).scrollSettle(),
        )
    }
    if (lastRead != null) {
        item(key = "continue") {
            LibraryContinueCard(
                ref = lastRead,
                onOpen = { onOpen(Route.Reader(lastRead.surah, lastRead.ayah)) },
                modifier = Modifier.animateItem().padding(bottom = Space.xl).focusIn(50).scrollSettle(),
            )
        }
    }
    if (favourites.isNotEmpty()) {
        item(key = "favourites-title") { GroupTitle("Favourites", Modifier.animateItem().dealIn(0, baseDelay = 80)) }
        itemsIndexed(favourites, key = { _, id -> "favourite-$id" }) { index, id ->
            GroupCell(
                index, favourites.size, Modifier.animateItem().dealIn(0, baseDelay = 80).scrollSettle(), inset = 70,
                bottomGap = index == favourites.lastIndex,
            ) {
                LibraryFavouriteHold(id, onOpen = { onOpen(Route.Reader(id, null)) }) { press ->
                    LibrarySurahRow(
                        surah = quran.surah(id),
                        isComplete = id in completed,
                        popDelayMillis = null,
                        isFavourite = true,
                        modifier = press.padding(horizontal = Space.m, vertical = Space.xs),
                    )
                }
            }
        }
    }
    item(key = "links") {
        GroupedSection(Modifier.animateItem().padding(bottom = Space.xl).dealIn(0, baseDelay = 120).scrollSettle()) {
            ListRow("Topics, plans and quiz", icon = symbol("sparkles"), onClick = { onOpen(Route.Explore) })
            RowDivider()
            ListRow("Highlights and notes", icon = symbol("highlighter"), onClick = { onOpen(Route.Annotations) })
        }
    }
    item(key = "browse-mode") {
        SegmentedControl(
            options = LibraryBrowseMode.entries,
            selected = mode,
            label = { it.title },
            onSelect = onMode,
            modifier = Modifier.animateItem().padding(bottom = Space.xl).dealIn(1, baseDelay = 120),
        )
    }
    when (mode) {
        LibraryBrowseMode.Surah -> surahList(quran, completed, favourites, onOpen)
        LibraryBrowseMode.Juz -> juzList(quran, onOpen)
        LibraryBrowseMode.Hizb -> hizbList(quran, onOpen)
    }
}

private fun LazyListScope.surahList(
    quran: QuranStore,
    completed: Set<Int>,
    favourites: List<Int>,
    onOpen: (Route) -> Unit,
) {
    item(key = "surahs-title") { GroupTitle("114 Surahs", Modifier.animateItem()) }
    val count = quran.surahs.size
    itemsIndexed(quran.surahs, key = { _, s -> "surah-${s.id}" }) { index, surah ->
        val deal = if (index < OPENING_DEAL) Modifier.dealIn(minOf(index, 8), baseDelay = 200, step = 50) else Modifier
        GroupCell(index, count, Modifier.animateItem().then(deal).scrollSettle(), inset = 70) {
            LibraryFavouriteHold(surah.id, onOpen = { onOpen(Route.Reader(surah.id, null)) }) { press ->
                LibrarySurahRow(
                    surah = surah,
                    isComplete = surah.id in completed,
                    popDelayMillis = if (index < OPENING_DEAL) 400 + minOf(index, 8) * 50 else 100,
                    isFavourite = surah.id in favourites,
                    modifier = press.padding(horizontal = Space.m, vertical = Space.xs),
                )
            }
        }
    }
}

/** The 30 juz, each opening the reader at its first verse. */
private fun LazyListScope.juzList(quran: QuranStore, onOpen: (Route) -> Unit) {
    item(key = "juz-title") { GroupTitle("30 Juz", Modifier.animateItem()) }
    val starts = quran.juzStarts
    itemsIndexed(starts, key = { i, _ -> "juz-${i + 1}" }) { index, start ->
        val deal = if (index < OPENING_DEAL) Modifier.dealIn(minOf(index, 8), baseDelay = 200, step = 50) else Modifier
        GroupCell(index, starts.size, Modifier.animateItem().then(deal).scrollSettle(), inset = 70) {
            LibraryPartRow(
                number = index + 1,
                title = "Juz ${index + 1}",
                start = start,
                popDelayMillis = if (index < OPENING_DEAL) 400 + minOf(index, 8) * 50 else null,
                modifier = Modifier.rowPress { onOpen(Route.Reader(start.surah, start.ayah)) }
                    .padding(horizontal = Space.m, vertical = Space.xs),
            )
        }
    }
}

/** The 60 hizbs. Each row opens the hizb, and its three quarter marks open the reader there. */
private fun LazyListScope.hizbList(quran: QuranStore, onOpen: (Route) -> Unit) {
    item(key = "hizb-title") { GroupTitle("60 Hizbs", Modifier.animateItem()) }
    val hizbs = quran.hizbs
    itemsIndexed(hizbs, key = { i, _ -> "hizb-${i + 1}" }) { index, quarters ->
        val deal = if (index < OPENING_DEAL) Modifier.dealIn(minOf(index, 8), baseDelay = 200, step = 50) else Modifier
        GroupCell(index, hizbs.size, Modifier.animateItem().then(deal).scrollSettle(), inset = 70) {
            Column {
                LibraryPartRow(
                    number = index + 1,
                    title = "Hizb ${index + 1}",
                    start = quarters[0],
                    popDelayMillis = if (index < OPENING_DEAL) 400 + minOf(index, 8) * 50 else null,
                    modifier = Modifier.rowPress { onOpen(Route.Reader(quarters[0].surah, quarters[0].ayah)) }
                        .padding(start = Space.m, end = Space.m, top = Space.xs),
                )
                Row(
                    Modifier.fillMaxWidth().padding(start = 70.dp, end = Space.m, bottom = Space.s),
                    horizontalArrangement = Arrangement.spacedBy(Space.xs),
                ) {
                    for (q in 1..3) {
                        LibraryQuarterChip(
                            hizb = index + 1,
                            quarter = q,
                            start = quarters[q],
                            modifier = Modifier.weight(1f),
                            onOpen = { onOpen(Route.Reader(quarters[q].surah, quarters[q].ayah)) },
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.searchResults(
    query: String,
    quran: QuranStore,
    results: LibrarySearchResults,
    isSearching: Boolean,
    completed: Set<Int>,
    onOpen: (Route) -> Unit,
) {
    results.jump?.let { jump ->
        item(key = "jump") {
            GroupedSection(Modifier.animateItem().padding(bottom = Space.xl).scrollSettle()) {
                ListRow(
                    "Go to ${quran.surah(jump.surah).name}, verse ${jump.ayah}",
                    icon = symbol("arrow.right.circle"),
                    onClick = { onOpen(Route.Reader(jump.surah, jump.ayah)) },
                )
            }
        }
    }
    if (results.surahs.isNotEmpty()) {
        item(key = "found-surahs-title") { GroupTitle("Surahs", Modifier.animateItem()) }
        itemsIndexed(results.surahs, key = { _, id -> "found-surah-$id" }) { index, id ->
            GroupCell(
                index, results.surahs.size, Modifier.animateItem().scrollSettle(), inset = 70,
                bottomGap = index == results.surahs.lastIndex,
            ) {
                LibraryFavouriteHold(id, onOpen = { onOpen(Route.Reader(id, null)) }) { press ->
                    LibrarySurahRow(
                        surah = quran.surah(id),
                        isComplete = id in completed,
                        popDelayMillis = null,
                        isFavourite = LocalGraph.current.library.isFavourite(id),
                        modifier = press.padding(horizontal = Space.m, vertical = Space.xs),
                    )
                }
            }
        }
    }
    if (results.verses.isNotEmpty()) {
        val title = if (results.verses.size >= SEARCH_LIMIT) "First $SEARCH_LIMIT verses" else "Verses"
        item(key = "found-verses-title") { GroupTitle(title, Modifier.animateItem()) }
        itemsIndexed(results.verses, key = { _, ref -> "found-verse-${ref.id}" }) { index, ref ->
            GroupCell(index, results.verses.size, Modifier.animateItem().scrollSettle(), inset = 16) {
                LibrarySearchVerseRow(
                    ref = ref,
                    modifier = Modifier.rowPress { onOpen(Route.Reader(ref.surah, ref.ayah)) }.padding(horizontal = Space.m, vertical = Space.xs),
                )
            }
        }
    }
    if (isSearching && results.isEmpty) {
        item(key = "searching") {
            Box(Modifier.animateItem().fillMaxWidth().padding(top = Space.xxl), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Palette.accent, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
            }
        }
    } else if (results.isEmpty) {
        item(key = "no-results") {
            SavedEmpty(
                icon = symbol("magnifyingglass"),
                title = "No Results for “$query”",
                detail = "Check the spelling or try a new search.",
                modifier = Modifier.animateItem().padding(top = Space.xl),
            )
        }
    }
}

/** Rows deal in only on the first screen, so a long scroll never waits on them. */
private const val OPENING_DEAL = 12

@Composable
private fun GroupTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = TextStyles.title3,
        color = Palette.textPrimary,
        modifier = modifier.padding(bottom = Space.xs + Space.xxs).semantics { heading() },
    )
}

/** One row of a titled group drawn on a single rounded surface, item by item so the list stays lazy. */
@Composable
private fun LazyItemScope.GroupCell(
    index: Int,
    count: Int,
    modifier: Modifier,
    inset: Int,
    bottomGap: Boolean = false,
    content: @Composable () -> Unit,
) {
    val first = index == 0
    val last = index == count - 1
    val shape = RoundedCornerShape(
        topStart = if (first) Radius.tile else 0.dp, topEnd = if (first) Radius.tile else 0.dp,
        bottomStart = if (last) Radius.tile else 0.dp, bottomEnd = if (last) Radius.tile else 0.dp,
    )
    Column(modifier.padding(bottom = if (last && bottomGap) Space.xl else 0.dp)) {
        Box(Modifier.fillMaxWidth().clip(shape).background(Palette.surface)) {
            content()
            if (!first) Box(Modifier.align(Alignment.TopStart)) { RowDivider(inset.dp) }
        }
    }
}

// Continue reading

/** Where she left off, as a card. It comes into focus, and a fine line fills to the verse she reached. */
@Composable
private fun LibraryContinueCard(ref: VerseRef, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val quran = LocalGraph.current.quran
    val surah = quran.surah(ref.surah)
    CardSurface(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Continue ${surah.name}, verse ${ref.ayah} of ${surah.verseCount}"
        },
        padding = Space.l,
        fill = Palette.surface,
        onClick = onOpen,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(symbol("book"), null, tint = Palette.textSecondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Continue reading", style = TextStyles.subheadline, color = Palette.textSecondary)
                }
                Text(surah.name, style = TextStyles.title3, color = Palette.textPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Verse ", style = TextStyles.subheadline, color = Palette.textSecondary)
                    RollingNumber(ref.ayah, TextStyles.subheadline, Palette.textSecondary)
                    Text(" of ${surah.verseCount}", style = TextStyles.subheadline, color = Palette.textSecondary)
                }
            }
            Spacer(Modifier.width(Space.xs))
            Text(
                surah.arabicName,
                style = TextStyle(fontFamily = AppFont.arabic, fontSize = 24.sp),
                color = Palette.accent,
            )
        }
        Spacer(Modifier.height(Space.m))
        ProgressLine(ref.ayah.toFloat() / max(surah.verseCount, 1))
    }
}

/** A slim line that fills to her place in the surah when it appears, and glides to a new place when it changes. */
@Composable
private fun ProgressLine(value: Float) {
    val reduce = LocalReduceMotion.current
    var shown by rememberSaveable { mutableStateOf(reduce) }
    LaunchedEffect(Unit) {
        if (!shown) {
            delay(450)
            shown = true
        }
    }
    val fraction by animateFloatAsState(
        if (shown) value.coerceIn(0.02f, 1f) else 0f,
        if (reduce) snap() else spring(dampingRatio = 0.85f, stiffness = 70f),
        label = "progressLine",
    )
    val wash = Palette.accentWash
    val fillA = Palette.accentFill
    val fillB = Palette.accent
    Canvas(Modifier.fillMaxWidth().height(5.dp)) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(wash, cornerRadius = r)
        val w = size.width * fraction
        if (w > 0f) {
            drawRoundRect(
                Brush.horizontalGradient(listOf(fillA, fillB), startX = 0f, endX = w),
                size = Size(w, size.height), cornerRadius = r,
            )
        }
    }
}

// Surah rows

/**
 * One surah in a list. When given a pop delay, its number badge pops in as the row lands.
 * The read mark scales in with a small bounce when it appears.
 */
@Composable
private fun LibrarySurahRow(
    surah: Surah,
    isComplete: Boolean,
    popDelayMillis: Int?,
    modifier: Modifier = Modifier,
    isFavourite: Boolean = false,
) {
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
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Surah ${surah.id}, ${surah.name}, ${surah.meaning}, ${surah.verseCount} verses" +
                    (if (isComplete) ", Read" else "") + (if (isFavourite) ", favourite" else "")
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
                surah.id.toString(),
                style = TextStyles.subheadline.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                color = Palette.accent,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(surah.name, style = TextStyles.headline, color = Palette.textPrimary)
                AnimatedVisibility(
                    visible = isComplete,
                    enter = if (reduce) fadeIn() else scaleIn(Motion.reward(), initialScale = 0.4f) + fadeIn(),
                    exit = if (reduce) fadeOut() else scaleOut(targetScale = 0.4f) + fadeOut(),
                ) {
                    Icon(symbol("checkmark.circle.fill"), null, tint = Palette.accent, modifier = Modifier.size(14.dp))
                }
                AnimatedVisibility(
                    visible = isFavourite,
                    enter = if (reduce) fadeIn() else scaleIn(Motion.reward(), initialScale = 0.4f) + fadeIn(),
                    exit = if (reduce) fadeOut() else scaleOut(targetScale = 0.4f) + fadeOut(),
                ) {
                    Icon(Icons.Filled.Favorite, null, tint = Palette.blushStrong, modifier = Modifier.size(14.dp))
                }
            }
            Text(
                "${surah.meaning} · ${surah.verseCount} verses",
                style = TextStyles.subheadline,
                color = Palette.textSecondary,
            )
        }
        Text(
            surah.arabicName,
            style = TextStyle(fontFamily = AppFont.arabic, fontSize = 21.sp),
            color = Palette.textPrimary,
        )
    }
}

/** A verse found by search: reference, two lines of Arabic and of translation. */
@Composable
private fun LibrarySearchVerseRow(ref: VerseRef, modifier: Modifier = Modifier) {
    val graph = LocalGraph.current
    val translation = graph.translations.text(ref)
    Column(
        modifier.fillMaxWidth().padding(vertical = 4.dp).semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Text(
            graph.quran.displayName(ref),
            style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold),
            color = Palette.accent,
        )
        ArabicText(graph.quran.text(ref), size = 21f, maxLines = 2, color = Palette.textPrimary)
        if (translation != null) {
            Text(
                translation,
                style = TextStyles.subheadline.copy(fontFamily = AppFont.serif),
                color = Palette.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// Search

private const val SEARCH_LIMIT = 60

private data class LibrarySearchResults(
    val jump: VerseRef? = null,
    val surahs: List<Int> = emptyList(),
    val verses: List<VerseRef> = emptyList(),
) {
    val isEmpty: Boolean get() = jump == null && surahs.isEmpty() && verses.isEmpty()
}

private val combiningMarks = Regex("\\p{M}+")

/** Lowercase, no accents, letters and digits only, so "al baqara" matches "Al-Baqarah". */
private fun fold(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(combiningMarks, "").lowercase().filter { it.isLetterOrDigit() }

/** Case and accent insensitive form that keeps spaces, for matching words inside a verse. */
private fun foldSoft(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(combiningMarks, "").lowercase()

/**
 * Finds surahs by name or number, jumps to "2:255", and searches verse text in Arabic
 * (ignoring diacritics) or in the downloaded translation.
 */
private suspend fun runLibrarySearch(query: String, quran: QuranStore, translation: List<List<String>>?): LibrarySearchResults {
    var jump: VerseRef? = null
    val numbers = query.split(':', ' ', '.').filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }
    if (numbers.size == 2 && quran.contains(VerseRef(numbers[0], numbers[1]))) {
        jump = VerseRef(numbers[0], numbers[1])
    }

    val asNumber = query.toIntOrNull()
    val folded = fold(query)
    val isArabic = ArabicSearch.containsArabic(query)
    val normalizedQuery = ArabicSearch.normalize(query)
    val surahs = quran.surahs.filter { surah ->
        when {
            asNumber != null && asNumber == surah.id -> true
            isArabic -> normalizedQuery.isNotEmpty() && ArabicSearch.normalize(surah.arabicName).contains(normalizedQuery)
            folded.isEmpty() -> false
            else -> fold(surah.name).contains(folded) || fold(surah.meaning).contains(folded)
        }
    }.map { it.id }

    if (query.length < 2 || asNumber != null || jump != null) return LibrarySearchResults(jump, surahs)

    val source = if (isArabic) quran.normalizedVerses() else translation
    source ?: return LibrarySearchResults(jump, surahs)

    val needle = if (isArabic) normalizedQuery else foldSoft(query)
    val verses = withContext(Dispatchers.Default) {
        val found = ArrayList<VerseRef>()
        outer@ for ((s, list) in source.withIndex()) {
            for ((a, text) in list.withIndex()) {
                val match = if (isArabic) text.contains(needle) else foldSoft(text).contains(needle)
                if (match) {
                    found.add(VerseRef(s + 1, a + 1))
                    if (found.size >= SEARCH_LIMIT) break@outer
                }
            }
        }
        found
    }
    return LibrarySearchResults(jump, surahs, verses)
}

// The Mushaf card

/** The page she is on in the Mushaf, kept under the same key as the iPhone app ("mushaf.page"). */
@Composable
private fun rememberMushafPage(): Int {
    val sp = LocalGraph.current.prefs.sp
    var page by remember { mutableIntStateOf(readMushafPage(sp)) }
    DisposableEffect(sp) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == MUSHAF_PAGE_KEY) page = readMushafPage(prefs)
        }
        sp.registerOnSharedPreferenceChangeListener(listener)
        onDispose { sp.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return page
}

private const val MUSHAF_PAGE_KEY = "mushaf.page"

private fun readMushafPage(sp: SharedPreferences): Int =
    runCatching { sp.getInt(MUSHAF_PAGE_KEY, 1) }.getOrDefault(1).coerceIn(1, 604)

/** The way into the mushaf: the closed book, tilted a little, with the page she is on. */
@Composable
private fun LibraryMushafCard(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val page = rememberMushafPage()
    val reduce = LocalReduceMotion.current
    var lifted by rememberSaveable { mutableStateOf(reduce) }
    val tilt = remember { Animatable(if (lifted) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!lifted) {
            delay(350)
            tilt.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 62f))
            lifted = true
        }
    }
    Row(
        modifier
            .fillMaxWidth()
            .pressable(scale = 0.985f, onClickLabel = "Opens the Quran as a book", onClick = onOpen)
            .clip(RoundedCornerShape(Radius.card))
            .background(Palette.surface)
            .padding(Space.l)
            .semantics(mergeDescendants = true) { contentDescription = "The Mushaf, page $page" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.l),
    ) {
        LibraryMushafCover(
            Modifier
                .size(width = 58.dp, height = 88.dp)
                .graphicsLayer {
                    rotationY = -12f * tilt.value
                    transformOrigin = TransformOrigin(1f, 0.5f)
                    cameraDistance = 6f * density
                    shadowElevation = 6.dp.toPx()
                    shape = RoundedCornerShape(10.dp)
                    clip = false
                }
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "The Mushaf",
                style = TextStyles.title3.copy(fontFamily = AppFont.serif),
                color = Palette.textPrimary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Page ", style = TextStyles.subheadline, color = Palette.textSecondary)
                RollingNumber(page, TextStyles.subheadline, Palette.textSecondary)
            }
        }
        Icon(symbol("book.pages"), null, tint = Palette.accent, modifier = Modifier.size(24.dp))
    }
}

private object CoverInk {
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFF3E5AB)
    val goldDeep = Color(0xFF7A5A15)
    val board = Color(0xFF5A1E18)
    val boardDeep = Color(0xFF1B0807)
    val boardLit = Color(0xFF87382A)
}

/** An eight pointed star, the common ornament of mushaf borders. */
internal fun quranStarPath(center: Offset, outer: Float): Path {
    val inner = outer * 0.48f
    val path = Path()
    for (i in 0 until 16) {
        val angle = i * PI / 8 - PI / 2
        val r = if (i % 2 == 0) outer else inner
        val x = center.x + (cos(angle) * r).toFloat()
        val y = center.y + (sin(angle) * r).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/** A pointed arch at the top and bottom, the shape of the central panel on a mushaf cover. */
private fun archPath(rect: Rect): Path {
    val w = rect.width
    val h = rect.height
    val shoulder = h * 0.16f
    return Path().apply {
        moveTo(rect.left, rect.top + shoulder)
        quadraticTo(rect.left + w * 0.28f, rect.top + shoulder * 0.15f, rect.center.x, rect.top)
        quadraticTo(rect.right - w * 0.28f, rect.top + shoulder * 0.15f, rect.right, rect.top + shoulder)
        lineTo(rect.right, rect.bottom - shoulder)
        quadraticTo(rect.right - w * 0.28f, rect.bottom - shoulder * 0.15f, rect.center.x, rect.bottom)
        quadraticTo(rect.left + w * 0.28f, rect.bottom - shoulder * 0.15f, rect.left, rect.bottom - shoulder)
        close()
    }
}

/**
 * The closed mushaf: a leather board tooled in gold. Every gold line is drawn three times,
 * a groove below, the metal, and a highlight above, so it reads as pressed into the board.
 */
@Composable
private fun LibraryMushafCover(modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val corner = CornerRadius(10.dp.toPx())
        drawRoundRect(
            Brush.radialGradient(
                listOf(CoverInk.boardLit, CoverInk.board, CoverInk.boardDeep),
                center = center, radius = max(w, h) * 0.75f,
            ),
            cornerRadius = corner,
        )
        val goldBrush = Brush.linearGradient(listOf(CoverInk.goldLight, CoverInk.gold, CoverInk.goldDeep), Offset.Zero, Offset(w, h))
        val groove = SolidColor(Color.Black.copy(alpha = 0.55f))
        val shine = SolidColor(Color.White.copy(alpha = 0.25f))
        fun emboss(draw: DrawScope.(Brush) -> Unit) {
            translate(top = 1.2.dp.toPx()) { draw(groove) }
            draw(goldBrush)
            translate(top = -0.6.dp.toPx()) { draw(shine) }
        }

        // Border band: two rules with a running line of stars between them.
        emboss { b ->
            val a = w * 0.045f
            drawRect(b, Offset(a, a), Size(w - 2 * a, h - 2 * a), style = Stroke(2.dp.toPx()))
            val c = w * 0.105f
            drawRect(b, Offset(c, c), Size(w - 2 * c, h - 2 * c), style = Stroke(1.dp.toPx()))
        }
        val inset = w * 0.075f
        val spacing = w * 0.075f
        val small = spacing * 0.32f / 2
        var x = inset + spacing
        while (x < w - inset - spacing / 2) {
            drawPath(quranStarPath(Offset(x, inset), small), CoverInk.gold)
            drawPath(quranStarPath(Offset(x, h - inset), small), CoverInk.gold)
            x += spacing
        }
        var y = inset + spacing
        while (y < h - inset - spacing / 2) {
            drawPath(quranStarPath(Offset(inset, y), small), CoverInk.gold)
            drawPath(quranStarPath(Offset(w - inset, y), small), CoverInk.gold)
            y += spacing
        }
        for (corner4 in 0 until 4) {
            val cx = if (corner4 % 2 == 0) inset else w - inset
            val cy = if (corner4 < 2) inset else h - inset
            emboss { b -> drawPath(quranStarPath(Offset(cx, cy), w * 0.06f), b) }
        }

        // The arch panel, like a mihrab.
        emboss { b ->
            val p1 = w * 0.17f
            drawPath(archPath(Rect(p1, p1, w - p1, h - p1)), b, style = Stroke(1.6.dp.toPx()))
            val p2 = w * 0.2f
            drawPath(archPath(Rect(p2, p2, w - p2, h - p2)), b, style = Stroke(0.8.dp.toPx()))
        }
        emboss { b -> drawPath(quranStarPath(Offset(w / 2, h * 0.27f), w * 0.045f), b) }
        emboss { b -> drawPath(quranStarPath(Offset(w / 2, h * 0.8f), w * 0.045f), b) }

        // The title, in gold.
        val textBrush = Brush.verticalGradient(listOf(CoverInk.goldLight, CoverInk.gold, CoverInk.goldDeep))
        val textShadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 1.5.dp.toPx()), 1.dp.toPx())
        val arabic = measurer.measure(
            "القرآن الكريم",
            TextStyle(
                brush = textBrush, fontFamily = AppFont.arabic, fontSize = (w * 0.13f).toSp(),
                textAlign = TextAlign.Center, shadow = textShadow,
            ),
        )
        val english = measurer.measure(
            "The Holy Quran",
            TextStyle(
                brush = textBrush, fontFamily = AppFont.serif, fontSize = (w * 0.07f).toSp(),
                fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, shadow = textShadow,
            ),
        )
        val gap = h * 0.018f
        val total = arabic.size.height + gap + english.size.height
        val top = h * 0.53f - total / 2
        drawText(arabic, topLeft = Offset((w - arabic.size.width) / 2, top))
        drawText(english, topLeft = Offset((w - english.size.width) / 2, top + arabic.size.height + gap))
    }
}
