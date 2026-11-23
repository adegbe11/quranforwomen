package com.quranforwomen.app.feature.you

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.HighlightColor
import com.quranforwomen.app.data.Surah
import com.quranforwomen.app.data.VerseNote
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.data.arabicIndic
import com.quranforwomen.app.nav.AppTab
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.ArabicText
import com.quranforwomen.app.ui.components.CardSurface
import com.quranforwomen.app.ui.components.Eyebrow
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.LocalUndo
import com.quranforwomen.app.ui.components.RollingNumber
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.SavedEmpty
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.inkReveal
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.rowPress
import com.quranforwomen.app.ui.components.scrollSettle
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.min

private val listSpring = spring<Float>(dampingRatio = 0.85f, stiffness = 250f)

// Saved ayahs

/**
 * Her saved verses. One is featured and can be turned over to another,
 * the rest deal in, and a removed verse can be brought back with Undo.
 */
@Composable
internal fun YouSavedAyahs() {
    val graph = LocalGraph.current
    val library = graph.library
    val quran = graph.quran
    val router = LocalRouter.current
    val undo = LocalUndo.current
    val haptics = rememberHaptics()
    val reduce = LocalReduceMotion.current

    var quranOrder by rememberSaveable { mutableStateOf(false) }
    var featuredId by rememberSaveable { mutableStateOf<String?>(null) }
    var flips by rememberSaveable { mutableIntStateOf(0) }

    val bookmarks = library.bookmarks
    LaunchedEffect(Unit) { if (featuredId == null) featuredId = bookmarks.randomOrNull()?.id }
    val featured = featuredId?.let(VerseRef::parse)?.takeIf { library.isBookmarked(it) }
    // Newest saves are at the front of the list.
    val ordered = if (quranOrder) bookmarks.sorted() else bookmarks

    fun turn() {
        val next = library.bookmarks.filter { it.id != featuredId }.randomOrNull() ?: return
        haptics.tick()
        flips++
        featuredId = next.id
    }

    fun remove(ref: VerseRef) {
        val at = library.bookmarks.indexOf(ref)
        library.removeBookmark(ref)
        if (featuredId == ref.id) featuredId = library.bookmarks.randomOrNull()?.id
        undo.show("Removed ${quran.displayName(ref)}") { library.restoreBookmark(ref, at) }
    }

    AppScreen(title = "Saved Ayahs", spacing = Space.s) {
        if (bookmarks.isEmpty()) {
            item(key = "empty") {
                SavedEmpty(
                    icon = symbol("bookmark"),
                    title = "No saved ayahs",
                    actionLabel = "Open the Quran",
                    onAction = { router.tab = AppTab.quran },
                )
            }
        } else {
            item(key = "top") {
                Column(
                    Modifier.padding(bottom = Space.xs),
                    verticalArrangement = Arrangement.spacedBy(Space.l),
                ) {
                    YouCountHeader(bookmarks.size, if (bookmarks.size == 1) "verse saved" else "verses saved")
                    if (featured != null) {
                        FeaturedVerse(
                            ref = featured,
                            flips = flips,
                            canTurn = bookmarks.size > 1,
                            onTurn = ::turn,
                            modifier = Modifier.dealIn(0, baseDelay = 100),
                        )
                    }
                    SortSwitch(quranOrder, { quranOrder = it }, Modifier.dealIn(1, baseDelay = 100))
                }
            }
            itemsIndexed(ordered, key = { _, ref -> ref.id }) { index, ref ->
                SavedVerseRow(
                    ref = ref,
                    onRemove = { remove(ref) },
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = if (reduce) null else listSpring,
                            placementSpec = if (reduce) null else spring(dampingRatio = 0.85f, stiffness = 250f),
                            fadeOutSpec = if (reduce) null else listSpring,
                        )
                        .dealIn(min(index, 8), baseDelay = 200, step = 50)
                        .scrollSettle(),
                )
            }
        }
    }
}

/** A large count with a plain label, rolled up as the screen opens. */
@Composable
private fun YouCountHeader(count: Int, label: String) {
    Row(
        Modifier
            .focusIn(50)
            .semantics(mergeDescendants = true) { contentDescription = "$count $label" },
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RollingNumber(
            youRolledUp(count, 100),
            style = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Light),
            color = Palette.textPrimary,
        )
        Text(label, style = TextStyles.subheadline, color = Palette.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
    }
}

/** One saved verse, chosen to show first. Turning it tips the card over to another. */
@Composable
private fun FeaturedVerse(ref: VerseRef, flips: Int, canTurn: Boolean, onTurn: () -> Unit, modifier: Modifier) {
    val quran = LocalGraph.current.quran
    val router = LocalRouter.current
    val reduce = LocalReduceMotion.current
    val tilt = remember { Animatable(0f) }
    val spin = remember { Animatable(0f) }
    val seen = remember { intArrayOf(flips) }
    LaunchedEffect(flips) {
        if (seen[0] == flips) return@LaunchedEffect
        seen[0] = flips
        if (reduce) return@LaunchedEffect
        val tipSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 320f)
        spin.snapTo(0f)
        coroutineScope {
            launch { spin.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
            tilt.animateTo(1f, tipSpring)
            tilt.animateTo(0f, tipSpring)
        }
    }

    CardSurface(
        modifier.graphicsLayer {
            rotationY = tilt.value * 14f
            cameraDistance = 10f * density
        },
        padding = Space.xl,
        fill = Palette.blush.copy(alpha = 0.45f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("From your saved verses", Modifier.weight(1f))
            if (canTurn) {
                Row(
                    Modifier
                        .heightIn(min = 44.dp)
                        .pressable(scale = 0.95f, onClick = onTurn)
                        .semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        symbol("arrow.triangle.2.circlepath"), null, tint = Palette.accent,
                        modifier = Modifier.size(17.dp).graphicsLayer { rotationZ = spin.value * 180f },
                    )
                    Text("Another", style = TextStyles.subheadline.copy(fontWeight = FontWeight.Medium), color = Palette.accent)
                }
            }
        }
        Spacer(Modifier.height(Space.m))
        AnimatedContent(
            targetState = ref,
            transitionSpec = {
                if (reduce) fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                else (scaleIn(spring(dampingRatio = 0.8f, stiffness = 300f), initialScale = 0.94f) + fadeIn()) togetherWith fadeOut()
            },
            label = "featured",
        ) { shown ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .pressable { router.push(Route.Reader(shown.surah, shown.ayah)) },
                verticalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                ArabicText(
                    "${quran.text(shown)} ﴿${shown.ayah.arabicIndic}﴾",
                    size = 26f,
                    modifier = Modifier.inkReveal(delayMillis = 150, durationMillis = 1000),
                )
                YouTranslationBody(shown)
                Text(
                    quran.displayName(shown),
                    style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold),
                    color = Palette.accent,
                )
            }
        }
    }
}

/** Newest first or Quran order. The filled capsule glides to her choice. */
@Composable
private fun SortSwitch(quranOrder: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier) {
    val haptics = rememberHaptics()
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Palette.accentWash)
            .padding(4.dp)
    ) {
        val w = (maxWidth - 4.dp) / 2
        val x by animateDpAsState(
            if (quranOrder) w + 4.dp else 0.dp,
            spring(dampingRatio = 0.78f, stiffness = 270f),
            label = "sort",
        )
        Box(
            Modifier
                .offset(x = x)
                .width(w)
                .height(36.dp)
                .clip(CircleShape)
                .background(Palette.accentFill)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(false, true).forEach { byQuran ->
                val isOn = quranOrder == byQuran
                val color by animateColorAsState(
                    if (isOn) Palette.onAccentFill else Palette.accent,
                    Motion.gentle(),
                    label = "sortText",
                )
                Box(
                    Modifier
                        .width(w)
                        .height(36.dp)
                        .pressable(scale = 0.97f) {
                            if (!isOn) {
                                haptics.tick()
                                onChange(byQuran)
                            }
                        }
                        .semantics {
                            selected = isOn
                            role = Role.Tab
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (byQuran) "Quran order" else "Newest first",
                        style = TextStyles.subheadline.copy(fontWeight = if (isOn) FontWeight.SemiBold else FontWeight.Medium),
                        color = color,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** A saved verse in the list. Hold it to remove it. */
@Composable
private fun SavedVerseRow(ref: VerseRef, onRemove: () -> Unit, modifier: Modifier) {
    val graph = LocalGraph.current
    val quran = graph.quran
    val router = LocalRouter.current
    val haptics = rememberHaptics()
    var menu by remember { mutableStateOf(false) }
    Box(modifier) {
        CardSurface(
            Modifier
                .youPressHold(
                    onClick = { router.push(Route.Reader(ref.surah, ref.ayah)) },
                    onLongClick = { haptics.tap(); menu = true },
                    longClickLabel = "Remove from Saved",
                )
                .semantics {
                    customActions = listOf(CustomAccessibilityAction("Remove from Saved") { onRemove(); true })
                },
            padding = Space.m,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(
                    quran.displayName(ref),
                    style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold),
                    color = Palette.accent,
                )
                ArabicText(quran.text(ref), size = 22f, maxLines = 3)
                graph.translations.text(ref)?.let { text ->
                    Text(
                        text,
                        style = TextStyles.subheadline.copy(fontFamily = AppFont.serif),
                        color = Palette.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        YouRemoveMenu(menu, { menu = false }, "Remove from Saved", symbol("bookmark.slash"), onRemove)
    }
}

// Highlights and notes

/**
 * Her highlights and notes. The count rolls up, the verses deal in and settle as she
 * scrolls, each highlight's colour swatch pops in, and a removed one folds away with Undo.
 */
@Composable
internal fun YouAnnotations() {
    val graph = LocalGraph.current
    val library = graph.library
    val quran = graph.quran
    val router = LocalRouter.current
    val undo = LocalUndo.current
    val reduce = LocalReduceMotion.current
    val refs = library.annotatedVerses

    fun remove(ref: VerseRef) {
        val color = library.highlight(ref)
        val note = library.note(ref)
        if (color != null) library.setHighlight(null, ref)
        if (note != null) library.setNote("", ref)
        undo.show("Removed ${quran.displayName(ref)}") {
            if (color != null) library.setHighlight(color, ref)
            if (note != null) library.setNote(note.text, ref)
        }
    }

    AppScreen(title = "Highlights & Notes", spacing = Space.s) {
        if (refs.isEmpty()) {
            item(key = "empty") {
                SavedEmpty(
                    icon = symbol("highlighter"),
                    title = "No highlights or notes yet",
                    actionLabel = "Open the Quran",
                    onAction = { router.tab = AppTab.quran },
                )
            }
        } else {
            item(key = "header") {
                Box(Modifier.padding(bottom = Space.s)) {
                    YouCountHeader(
                        refs.size,
                        if (refs.size == 1) "verse with a highlight or note" else "verses with a highlight or note",
                    )
                }
            }
            itemsIndexed(refs, key = { _, ref -> ref.id }) { index, ref ->
                AnnotationCard(
                    ref = ref,
                    index = index,
                    color = library.highlight(ref),
                    note = library.note(ref),
                    onRemove = { remove(ref) },
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = if (reduce) null else listSpring,
                            placementSpec = if (reduce) null else spring(dampingRatio = 0.85f, stiffness = 250f),
                            fadeOutSpec = if (reduce) null else listSpring,
                        )
                        .dealIn(min(index, 8), baseDelay = 150, step = 50)
                        .scrollSettle(),
                )
            }
        }
    }
}

@Composable
private fun AnnotationCard(
    ref: VerseRef,
    index: Int,
    color: HighlightColor?,
    note: VerseNote?,
    onRemove: () -> Unit,
    modifier: Modifier,
) {
    val haptics = rememberHaptics()
    var menu by remember { mutableStateOf(false) }
    val step = min(index, 8)
    val removeTitle = when {
        color != null && note != null -> "Remove Highlight and Note"
        color != null -> "Remove Highlight"
        else -> "Remove Note"
    }
    Box(modifier) {
        Column(
            Modifier.semantics {
                customActions = listOf(CustomAccessibilityAction(removeTitle) { onRemove(); true })
            },
            verticalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            YouVerseCard(
                ref = ref,
                arabicSize = 22f,
                inkDelay = if (index < 8) 300 + step * 60 else 200,
                onLongClick = { haptics.tap(); menu = true },
                longClickLabel = removeTitle,
            )
            if (color != null) {
                HighlightTag(color, 450 + step * 50, Modifier.padding(horizontal = Space.s))
            }
            if (note != null) {
                Row(
                    Modifier.padding(horizontal = Space.s).semantics(mergeDescendants = true) {},
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    Icon(symbol("note.text"), null, tint = Palette.accent, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(note.text, style = TextStyles.body.copy(fontFamily = AppFont.serif), color = Palette.textPrimary)
                        Text(
                            Instant.ofEpochMilli(note.updated).atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                            style = TextStyles.caption,
                            color = Palette.textSecondary,
                        )
                    }
                }
            }
        }
        YouRemoveMenu(menu, { menu = false }, removeTitle, symbol("trash"), onRemove)
    }
}

/** A verse as a card: Arabic, translation, its name, and a bookmark if saved. Tints with its highlight. */
@Composable
private fun YouVerseCard(
    ref: VerseRef,
    arabicSize: Float,
    inkDelay: Int?,
    onLongClick: () -> Unit,
    longClickLabel: String,
) {
    val graph = LocalGraph.current
    val quran = graph.quran
    val library = graph.library
    val router = LocalRouter.current
    val reduce = LocalReduceMotion.current
    val saved = library.isBookmarked(ref)
    val fill by animateColorAsState(library.highlight(ref)?.youFill() ?: Palette.surface, Motion.gentle(), label = "verseFill")
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (reduce || !saved) return@LaunchedEffect
        delay(450L + (inkDelay ?: 0))
        bounce.animateTo(1.25f, tween(110))
        bounce.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f))
    }
    CardSurface(
        Modifier.youPressHold(
            onClick = { router.push(Route.Reader(ref.surah, ref.ayah)) },
            onLongClick = onLongClick,
            longClickLabel = longClickLabel,
        ),
        fill = fill,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            ArabicText(
                "${quran.text(ref)} ﴿${ref.ayah.arabicIndic}﴾",
                size = arabicSize,
                modifier = if (inkDelay != null) Modifier.inkReveal(delayMillis = inkDelay) else Modifier,
            )
            YouTranslationBody(ref)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    quran.displayName(ref),
                    style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold),
                    color = Palette.accent,
                    modifier = Modifier.weight(1f),
                )
                if (saved) {
                    Icon(
                        symbol("bookmark.fill"), "Saved", tint = Palette.blushStrong,
                        modifier = Modifier.size(15.dp).graphicsLayer { scaleX = bounce.value; scaleY = bounce.value },
                    )
                }
            }
        }
    }
}

/** A highlight's colour as a small swatch that pops in with a little overshoot. */
@Composable
private fun HighlightTag(color: HighlightColor, delayMillis: Int, modifier: Modifier) {
    val reduce = LocalReduceMotion.current
    var shown by rememberSaveable { mutableStateOf(false) }
    val scale = remember { Animatable(if (shown || reduce) 1f else 0.01f) }
    LaunchedEffect(Unit) {
        if (scale.value < 1f) {
            delay(delayMillis.toLong())
            scale.animateTo(1f, Motion.reward())
        }
        shown = true
    }
    val swatch by animateColorAsState(color.youSwatch, Motion.gentle(), label = "swatch")
    Row(
        modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(10.dp)
                .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                .clip(CircleShape)
                .background(swatch)
        )
        Text(
            "${color.title} highlight",
            style = TextStyles.caption.copy(fontWeight = FontWeight.Medium),
            color = Palette.textSecondary,
        )
    }
}

// Reading progress

/** Calm and factual. No streaks, no scores. */
@Composable
internal fun YouReadingProgress() {
    val graph = LocalGraph.current
    val quran = graph.quran
    val library = graph.library
    val router = LocalRouter.current
    val last = library.lastRead
    val completed = library.completedSurahs

    AppScreen(title = "Reading Progress") {
        item(key = "now") {
            GroupedSection {
                if (last != null) {
                    val position = quran.globalNumber(last)
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = Space.m, vertical = Space.m),
                        verticalArrangement = Arrangement.spacedBy(Space.s),
                    ) {
                        Text("You are reading", style = TextStyles.subheadline, color = Palette.textSecondary)
                        Text(
                            "${quran.surah(last.surah).name}, verse ${last.ayah}",
                            style = TextStyles.title3.copy(fontFamily = AppFont.serif),
                            color = Palette.textPrimary,
                        )
                        GrowBar(position.toFloat() / maxOf(quran.totalVerses, 1))
                        Text(
                            "Verse $position of ${quran.totalVerses} in the Quran",
                            style = TextStyles.footnote,
                            color = Palette.textSecondary,
                        )
                    }
                    RowDivider(inset = Space.m)
                    YouRow(
                        title = "Continue Reading",
                        icon = null,
                        titleColor = Palette.accent,
                        onClick = { router.push(Route.Reader(last.surah, last.ayah)) },
                    )
                } else {
                    Text(
                        "Not started",
                        style = TextStyles.body,
                        color = Palette.textSecondary,
                        modifier = Modifier.padding(horizontal = Space.m, vertical = 14.dp),
                    )
                }
            }
        }
        item(key = "mosaic") {
            GroupedSection(title = "All 114 surahs") {
                SurahMosaic(completed, last?.surah, Modifier.padding(Space.m))
            }
        }
        item(key = "stats") {
            GroupedSection {
                YouStatRow("Surahs read to the end", "${completed.size} of 114")
                RowDivider(inset = Space.m)
                YouStatRow("Saved ayahs", "${library.bookmarks.size}")
            }
        }
        if (completed.isNotEmpty()) {
            item(key = "done") {
                GroupedSection(title = "Read to the end") {
                    completed.sorted().forEachIndexed { i, id ->
                        if (i > 0) RowDivider(inset = 70.dp)
                        YouSurahRow(quran.surah(id)) { router.push(Route.Reader(id, null)) }
                    }
                }
            }
        }
    }
}

/** A title and a value, like a labelled row in iOS Settings. */
@Composable
internal fun YouStatRow(title: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = Space.m, vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = TextStyles.body, color = Palette.textPrimary, modifier = Modifier.weight(1f))
        Text(value, style = TextStyles.body, color = Palette.textSecondary)
    }
}

/** 114 dots, one per surah. They ripple in as a quick wave; her current surah gently pulses. */
@Composable
private fun SurahMosaic(completed: Set<Int>, current: Int?, modifier: Modifier) {
    val columns = 12
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in 0 until (114 + columns - 1) / columns) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0 until columns) {
                    val id = row * columns + col + 1
                    if (id <= 114) {
                        MosaicDot(id, id in completed, id == current, Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MosaicDot(id: Int, isDone: Boolean, isCurrent: Boolean, modifier: Modifier) {
    val graph = LocalGraph.current
    val router = LocalRouter.current
    val reduce = LocalReduceMotion.current
    var shown by rememberSaveable { mutableStateOf(false) }
    val wave = remember { Animatable(if (shown || reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (wave.value < 1f) {
            delay(150L + id * 8L)
            wave.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 250f))
        }
        shown = true
    }
    val surah = graph.quran.surah(id)
    Box(
        modifier
            .aspectRatio(1f)
            .graphicsLayer {
                val p = wave.value
                val s = 0.2f + 0.8f * p
                scaleX = s; scaleY = s
                alpha = p.coerceIn(0f, 1f)
            }
            .pressable(scale = 0.8f) { router.push(Route.Reader(id, null)) }
            .semantics {
                contentDescription = "Surah $id, ${surah.name}"
                stateDescription = when {
                    isDone -> "Read to the end"
                    isCurrent -> "Reading now"
                    else -> "Not yet"
                }
            },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(if (isDone) Palette.accentFill else Palette.accentWash)
        )
        if (isCurrent) {
            val pulse = if (reduce) null else rememberInfiniteTransition(label = "currentSurah").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "pulse",
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val v = pulse?.value ?: 0f
                        val s = 1f + 0.35f * v
                        scaleX = s; scaleY = s
                        alpha = 1f - 0.8f * v
                    }
                    .border(2.dp, Palette.blushStrong, CircleShape)
            )
        }
    }
}

/** A bar that grows to its value when it appears. */
@Composable
private fun GrowBar(value: Float) {
    val reduce = LocalReduceMotion.current
    val progress = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay(300)
            progress.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 40f))
        }
    }
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(Palette.accentWash)
    ) {
        val fraction = (value.coerceIn(0.01f, 1f) * progress.value).coerceIn(0f, 1f)
        Box(
            Modifier
                .fillMaxHeight()
                .width(maxWidth * fraction)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(Palette.accentFill, Palette.accent)))
        )
    }
}

/** One surah in a list, with a number badge and a read mark. */
@Composable
private fun YouSurahRow(surah: Surah, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .rowPress(onClick)
            .padding(horizontal = Space.m, vertical = 10.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Surah ${surah.id}, ${surah.name}, ${surah.meaning}, ${surah.verseCount} verses, read"
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Palette.accentWash),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${surah.id}",
                style = TextStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                color = Palette.accent,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(surah.name, style = TextStyles.headline, color = Palette.textPrimary)
                Icon(symbol("checkmark.circle.fill"), null, tint = Palette.accent, modifier = Modifier.size(13.dp))
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
