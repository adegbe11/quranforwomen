package com.quranforwomen.app.feature.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.HighlightColor
import com.quranforwomen.app.data.RepeatMode
import com.quranforwomen.app.data.Surah
import com.quranforwomen.app.data.TranslationStore
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.data.arabicIndic
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.AppSheet
import com.quranforwomen.app.ui.components.ArabicText
import com.quranforwomen.app.ui.components.BarIcon
import com.quranforwomen.app.ui.components.LotusPart
import com.quranforwomen.app.ui.components.TextAction
import com.quranforwomen.app.ui.components.TopBarHeight
import com.quranforwomen.app.ui.components.TranslationText
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.inkReveal
import com.quranforwomen.app.ui.components.lotusPath
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.BookInk
import com.quranforwomen.app.ui.theme.Ink
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import com.quranforwomen.app.ui.theme.Radius
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The reading surface. Motion here only ever serves the reading: it shows where she is,
 * follows the recitation, and marks the end. Nothing moves while she is simply reading.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SurahReaderContent(surahArg: Int, startVerse: Int?) {
    // Never trust an out of range number from a link.
    val surahID = surahArg.coerceIn(1, 114)
    val graph = LocalGraph.current
    val quran = graph.quran
    val library = graph.library
    val player = graph.player
    val prefs = graph.prefs
    val reduce = LocalReduceMotion.current
    val scope = rememberCoroutineScope()

    val surah = quran.surah(surahID)
    val listState = rememberLazyListState()
    var selected by remember { mutableStateOf<VerseRef?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var isReady by remember { mutableStateOf(false) }
    var didInitialScroll by rememberSaveable { mutableStateOf(false) }
    val firstVerseIndex = if (surah.showsBasmala) 2 else 1

    val visible by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.mapNotNull { it.key as? Int } }
    }
    val position by remember(surah) {
        derivedStateOf {
            val first = visible.minOrNull()
            if (first == null || surah.verseCount <= 1) (if (visible.isEmpty()) 0f else 1f)
            else (first - 1).toFloat() / (surah.verseCount - 1)
        }
    }

    LaunchedEffect(Unit) {
        if (!didInitialScroll) {
            didInitialScroll = true
            library.recordVisit(VerseRef(surahID, (startVerse ?: 1).coerceIn(1, surah.verseCount)))
            if (startVerse != null && startVerse > 1) {
                delay(60)
                listState.scrollToItem(firstVerseIndex + startVerse.coerceAtMost(surah.verseCount) - 1)
            }
        }
        // Only start saving the reading position once the first scroll settled.
        delay(400)
        isReady = true
    }

    LaunchedEffect(isReady) {
        if (!isReady) return@LaunchedEffect
        snapshotFlow { visible }.collect { verses ->
            val first = verses.minOrNull() ?: return@collect
            library.markLastRead(VerseRef(surahID, first))
            if (surah.verseCount in verses) library.markCompleted(surahID)
        }
    }

    // Load the transliteration once if she reads with it.
    LaunchedEffect(graph.transliteration.show) {
        if (graph.transliteration.show) graph.transliteration.prepare()
    }

    // Follow the recitation, verse by verse.
    LaunchedEffect(surahID) {
        snapshotFlow { player.current }.drop(1).collect { current ->
            if (current == null || current.surah != surahID) return@collect
            val index = firstVerseIndex + current.ayah - 1
            val offset = -(listState.layoutInfo.viewportSize.height / 3)
            if (reduce) listState.scrollToItem(index, offset) else listState.animateScrollToItem(index, offset)
        }
    }

    val arabicSize = prefs.arabicSize
    val translationSize = prefs.translationSize
    val showTranslation = prefs.showTranslation
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(Modifier.fillMaxSize()) {
        AppScreen(
            title = surah.name,
            book = true,
            largeTitle = false,
            listState = listState,
            spacing = 0.dp,
            actions = {
                val playingHere = player.current?.surah == surahID && player.isPlaying
                BarIcon(
                    symbol(if (playingHere) "pause.fill" else "headphones"),
                    if (playingHere) "Pause" else "Listen",
                ) {
                    if (player.current?.surah == surahID) {
                        player.togglePlayPause()
                    } else {
                        val from = visible.minOrNull() ?: 1
                        player.updateRepeatMode(RepeatMode.Off)
                        player.play(VerseRef(surahID, from))
                    }
                }
                BarIcon(symbol("textformat.size"), "Reading settings") { showSettings = true }
                ReaderMoreMenu(surahID)
            },
        ) {
            item(key = "header") {
                Column {
                    ReaderSurahHeader(surah, Modifier.padding(top = Space.l, bottom = Space.xl))
                    HifzProgress(surahID, surah.verseCount)
                }
            }
            if (surah.showsBasmala) {
                item(key = "basmala") {
                    Box(Modifier.fillMaxWidth().padding(bottom = Space.xl)) {
                        BloomGlow(delayMillis = 600, modifier = Modifier.matchParentSize())
                        ArabicText(
                            quran.basmala,
                            size = arabicSize * 0.92f,
                            center = true,
                            color = BookInk.ink,
                            modifier = Modifier
                                .inkReveal(550, 1000)
                                .clearAndSetSemantics { contentDescription = "Bismillah ir-Rahman ir-Rahim" },
                        )
                    }
                }
            }
            items(surah.verseCount, key = { it + 1 }) { i ->
                val ayah = i + 1
                val ref = VerseRef(surahID, ayah)
                ReaderVerseRow(
                    ref = ref,
                    text = surah.ayahs[i],
                    hasSajdah = ayah in surah.sajdas,
                    isPlaying = player.isCurrent(ref),
                    isSelected = selected == ref,
                    isBookmarked = library.isBookmarked(ref),
                    highlight = library.highlight(ref),
                    note = library.note(ref)?.text,
                    arabicSize = arabicSize,
                    translationSize = translationSize,
                    showTranslation = showTranslation,
                    showTransliteration = graph.transliteration.show,
                    onTap = { selected = ref },
                )
            }
            item(key = "footer") {
                NextSurahFooter(
                    current = surah,
                    reachedEnd = surah.verseCount in visible,
                    modifier = Modifier.padding(top = Space.xxl),
                )
            }
        }
        ReadingLine(position, Modifier.align(Alignment.TopStart).padding(top = top + TopBarHeight))
    }

    selected?.let { ref -> QuranVerseActionsSheet(ref, onDismiss = { selected = null }) }

    if (showSettings) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        AppSheet(onDismiss = { showSettings = false }, sheetState = sheetState) {
            Box(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = Space.xs)) {
                Text(
                    "Reading", style = TextStyles.headline, color = Ink.text,
                    modifier = Modifier.align(Alignment.Center).semantics { heading() },
                )
                TextAction(
                    "Done",
                    bold = true,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = { scope.launch { sheetState.hide() }.invokeOnCompletion { showSettings = false } },
                )
            }
            ReadingSettingsScroll(showsLanguageLink = false, surahID = surahID)
        }
    }
}

// Basmala glow

/** A soft light that blooms once behind the Basmala, then settles to a faint warmth. */
@Composable
private fun BloomGlow(delayMillis: Int, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val color = BookInk.glow
    var phase by rememberSaveable { mutableIntStateOf(if (reduce) 2 else 0) }
    LaunchedEffect(Unit) {
        if (phase == 0) {
            delay(delayMillis.toLong())
            phase = 1
            delay(1400)
            phase = 2
        }
    }
    val grow by animateFloatAsState(if (phase == 0) 0.4f else 1f, if (reduce) snap() else tween(1200, easing = FastOutSlowInEasing), label = "bloomScale")
    val alpha by animateFloatAsState(
        when (phase) { 0 -> 0f; 1 -> 0.9f; else -> 0.35f },
        if (reduce) snap() else tween(if (phase == 2) 1600 else 1200, easing = FastOutSlowInEasing),
        label = "bloomAlpha",
    )
    Canvas(modifier) {
        if (alpha <= 0f) return@Canvas
        val r = size.width / 2
        scale(grow, grow * (size.height / size.width).coerceAtLeast(0.01f)) {
            drawCircle(
                Brush.radialGradient(
                    0f to color, 0.55f to color.copy(alpha = 0.8f), 1f to color.copy(alpha = 0f),
                    center = center, radius = r,
                ),
                radius = r, center = center, alpha = alpha,
            )
        }
    }
}

// Reading line

/** A hairline under the bar that fills as she moves through the surah. */
@Composable
private fun ReadingLine(progress: Float, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val p by animateFloatAsState(progress, if (reduce) snap() else spring(dampingRatio = 0.85f, stiffness = 110f), label = "readingLine")
    val a = BookInk.rule
    val b = BookInk.gild
    val percent = (progress * 100).roundToInt()
    Canvas(
        modifier
            .fillMaxWidth()
            .height(2.5.dp)
            .semantics {
                contentDescription = "Reading position"
                stateDescription = "$percent percent"
            }
    ) {
        val w = max(6.dp.toPx(), size.width * p)
        drawRoundRect(
            Brush.horizontalGradient(listOf(a, b), startX = 0f, endX = w),
            size = Size(w, size.height),
            cornerRadius = CornerRadius(size.height / 2),
        )
    }
}

// Header

@Composable
private fun ReaderSurahHeader(surah: Surah, modifier: Modifier = Modifier) {
    val gild = BookInk.gild
    val rule = BookInk.rule
    Column(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Box(Modifier.fillMaxWidth().height(66.dp).clearAndSetSemantics { }) {
            Canvas(Modifier.matchParentSize()) {
                val r6 = CornerRadius(6.dp.toPx())
                drawRoundRect(gild.copy(alpha = 0.14f), cornerRadius = r6)
                val s1 = 1.2.dp.toPx()
                drawRoundRect(
                    gild, topLeft = Offset(s1 / 2, s1 / 2), size = Size(size.width - s1, size.height - s1),
                    cornerRadius = r6, style = Stroke(s1),
                )
                val s2 = 0.6.dp.toPx()
                val i = 3.dp.toPx() + s2 / 2
                drawRoundRect(
                    rule.copy(alpha = 0.5f), topLeft = Offset(i, i), size = Size(size.width - 2 * i, size.height - 2 * i),
                    cornerRadius = CornerRadius(4.dp.toPx()), style = Stroke(s2),
                )
            }
            Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                StarMark(gild)
                Spacer(Modifier.weight(1f))
                Text(
                    surah.arabicName,
                    style = TextStyle(fontFamily = AppFont.arabic, fontSize = 34.sp, lineHeight = 44.sp),
                    color = BookInk.caption,
                    maxLines = 1,
                    modifier = Modifier.inkReveal(100, 900),
                )
                Spacer(Modifier.weight(1f))
                StarMark(gild)
            }
        }
        Text(
            surah.name,
            style = TextStyles.title2.copy(fontFamily = AppFont.serif),
            color = BookInk.ink,
            modifier = Modifier.focusIn(300),
        )
        Text(
            "${surah.meaning} · ${surah.verseCount} verses · ${surah.revelationPlace}",
            style = TextStyles.subheadline,
            color = BookInk.caption,
            textAlign = TextAlign.Center,
            modifier = Modifier.focusIn(420),
        )
    }
}

@Composable
private fun StarMark(color: Color, size: Int = 14) {
    Canvas(Modifier.size(size.dp)) {
        drawPath(quranStarPath(center, this.size.minDimension / 2), color)
    }
}

// Verse

@Composable
private fun ReaderVerseRow(
    ref: VerseRef,
    text: String,
    hasSajdah: Boolean,
    isPlaying: Boolean,
    isSelected: Boolean,
    isBookmarked: Boolean,
    highlight: HighlightColor?,
    note: String?,
    arabicSize: Float,
    translationSize: Float,
    showTranslation: Boolean,
    showTransliteration: Boolean,
    onTap: () -> Unit,
) {
    val haptics = rememberHaptics()
    val reduce = LocalReduceMotion.current
    val staticTarget = when {
        isSelected -> BookInk.pageEdge
        highlight != null -> highlight.quranFill
        else -> Color.Transparent
    }
    val staticFill by animateColorAsState(staticTarget, Motion.gentle(), label = "verseFill")
    val playingAlpha by animateFloatAsState(if (isPlaying) 1f else 0f, tween(550, easing = FastOutSlowInEasing), label = "versePlaying")
    val ruleAlpha by animateFloatAsState(if (isPlaying || isSelected || highlight != null) 0f else 1f, Motion.gentle(), label = "verseRule")
    val gild = BookInk.gild
    val rule = BookInk.rule

    // A soft tap when she saves the verse.
    var lastBookmarked by remember { mutableStateOf(isBookmarked) }
    LaunchedEffect(isBookmarked) {
        if (isBookmarked && !lastBookmarked) haptics.soft()
        lastBookmarked = isBookmarked
    }

    Column(
        Modifier
            .fillMaxWidth()
            .verseRise()
            .drawBehind {
                val inset = 4.dp.toPx()
                val r = CornerRadius(Radius.tile.toPx())
                val tl = Offset(-inset, 0f)
                val sz = Size(size.width + 2 * inset, size.height)
                if (staticFill.alpha > 0f) drawRoundRect(staticFill, tl, sz, r)
                if (playingAlpha > 0f) drawRoundRect(gild.copy(alpha = 0.16f * playingAlpha), tl, sz, r)
                if (ruleAlpha > 0f) {
                    val pad = Space.xxxl.toPx()
                    drawRect(
                        rule.copy(alpha = 0.22f * ruleAlpha),
                        topLeft = Offset(pad, size.height - 1.dp.toPx()),
                        size = Size((size.width - 2 * pad).coerceAtLeast(0f), 1.dp.toPx()),
                    )
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = "Shows play, repeat, save and share",
                onClick = onTap,
            )
            .padding(vertical = Space.l, horizontal = Space.s)
            .semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(
                "${ref.surah}:${ref.ayah}",
                style = TextStyles.caption.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                color = BookInk.rubric,
            )
            if (hasSajdah) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(symbol("arrow.down.to.line"), null, tint = BookInk.rubric, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Sajdah", style = TextStyles.caption.copy(fontWeight = FontWeight.Medium), color = BookInk.rubric)
                }
            }
            if (LocalGraph.current.hifz.isLearned(ref)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(symbol("checkmark.circle.fill"), null, tint = BookInk.gild, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Learned", style = TextStyles.caption.copy(fontWeight = FontWeight.Medium), color = BookInk.caption)
                }
            }
            Spacer(Modifier.weight(1f))
            // A ribbon that drops in when she saves the verse.
            AnimatedVisibility(
                visible = isBookmarked,
                enter = if (reduce) fadeIn() else slideInVertically(spring(dampingRatio = 0.55f, stiffness = 250f)) { -it } + fadeIn(),
                exit = if (reduce) fadeOut() else scaleOut(targetScale = 0.4f) + fadeOut(),
            ) {
                Icon(
                    symbol("bookmark.fill"), null, tint = BookInk.ribbon,
                    modifier = Modifier.size(15.dp).semantics { contentDescription = "Saved" },
                )
            }
            AnimatedVisibility(
                visible = isPlaying,
                enter = if (reduce) fadeIn() else scaleIn() + fadeIn(),
                exit = if (reduce) fadeOut() else scaleOut() + fadeOut(),
            ) {
                PulsingWaveform(BookInk.gild, Modifier.size(14.dp).semantics { contentDescription = "Playing" })
            }
        }

        val graph = LocalGraph.current
        if (graph.words.show || graph.hifz.isOn) {
            VerseWords(ref, text, arabicSize, translationSize)
        } else {
            ArabicText("$text ﴿${ref.ayah.arabicIndic}﴾", size = arabicSize, color = BookInk.ink)
        }

        if (showTransliteration) QuranTransliterationLine(ref, translationSize)

        AnimatedVisibility(visible = isPlaying, enter = fadeIn(), exit = fadeOut()) {
            RecitationLine()
        }

        if (showTranslation) QuranTranslationBody(ref, translationSize)

        if (note != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.control))
                    .background(BookInk.pageEdge.copy(alpha = 0.7f))
                    .padding(Space.s)
                    .clearAndSetSemantics { contentDescription = "Your note: $note" },
                horizontalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                Icon(symbol("note.text"), null, tint = BookInk.caption, modifier = Modifier.size(18.dp).padding(top = 1.dp))
                Text(
                    note,
                    style = TextStyles.subheadline.copy(fontFamily = AppFont.serif),
                    color = BookInk.caption,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Each verse rises gently into place as it enters the screen from the bottom edge. */
@Composable
private fun Modifier.verseRise(): Modifier {
    val reduce = LocalReduceMotion.current
    val view = LocalView.current
    var below by remember { mutableStateOf(false) }
    val p by animateFloatAsState(if (below) 1f else 0f, tween(450, easing = FastOutSlowInEasing), label = "verseRise")
    return this
        .onGloballyPositioned { coords ->
            val bottom = coords.positionInWindow().y + coords.size.height
            below = bottom > view.height
        }
        .graphicsLayer {
            alpha = 1f - 0.65f * p
            translationY = if (reduce) 0f else 18.dp.toPx() * p
        }
}

/** The waveform symbol, gently pulsing like iOS's variable colour effect. */
@Composable
internal fun PulsingWaveform(color: Color, modifier: Modifier = Modifier, active: Boolean = true) {
    val reduce = LocalReduceMotion.current
    val alpha = if (active && !reduce) {
        val t = rememberInfiniteTransition(label = "waveform")
        val a by t.animateFloat(
            0.4f, 1f,
            infiniteRepeatable(tween(650, easing = LinearEasing), AnimRepeatMode.Reverse),
            label = "waveformAlpha",
        )
        a
    } else 1f
    Icon(symbol("waveform"), null, tint = color, modifier = modifier.graphicsLayer { this.alpha = alpha })
}

/** A fine line under the verse being recited, filling right to left with the audio, the way Arabic is read. */
@Composable
private fun RecitationLine() {
    val player = LocalGraph.current.player
    val gild = BookInk.gild
    val p by animateFloatAsState(player.itemProgress, tween(100, easing = LinearEasing), label = "recitation")
    Canvas(Modifier.fillMaxWidth().height(2.dp).clearAndSetSemantics { }) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(gild.copy(alpha = 0.2f), cornerRadius = r)
        val w = size.width * p
        if (w > 0f) drawRoundRect(gild, topLeft = Offset(size.width - w, 0f), size = Size(w, size.height), cornerRadius = r)
    }
}

/** Translation for a verse, or a calm note while it downloads. */
@Composable
internal fun QuranTranslationBody(ref: VerseRef, size: Float? = null, modifier: Modifier = Modifier) {
    val translations = LocalGraph.current.translations
    val scope = rememberCoroutineScope()
    val text = translations.text(ref)
    if (text != null) {
        TranslationText(
            text,
            modifier = modifier,
            size = size ?: 17f,
            rightToLeft = translations.selected.isRightToLeft,
        )
        return
    }
    when (translations.selectedState) {
        TranslationStore.State.Downloading -> Row(
            modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            CircularProgressIndicator(color = Ink.secondary, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
            Text("Preparing the translation", style = TextStyles.footnote, color = Ink.secondary)
        }
        TranslationStore.State.Failed, TranslationStore.State.NotDownloaded -> Row(
            modifier
                .heightIn(min = 44.dp)
                .pressable(scale = 0.96f) { scope.launch { translations.prepareSelected() } },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(symbol("arrow.down.circle"), null, tint = Ink.accent, modifier = Modifier.size(16.dp))
            Text("Download translation", style = TextStyles.footnote.copy(fontWeight = FontWeight.Medium), color = Ink.accent)
        }
        TranslationStore.State.Ready -> Unit
    }
}

// Footer

/** The end of a surah: a quiet moment, then the way forward. */
@Composable
private fun NextSurahFooter(current: Surah, reachedEnd: Boolean, modifier: Modifier = Modifier) {
    val quran = LocalGraph.current.quran
    val router = LocalRouter.current
    val reduce = LocalReduceMotion.current
    val haptics = rememberHaptics()
    var bloomed by rememberSaveable { mutableStateOf(false) }
    val drawn = remember { Animatable(if (bloomed) 1f else 0f) }
    val bloom = remember { Animatable(if (bloomed) 1f else 0f) }

    LaunchedEffect(reachedEnd) {
        if (!reachedEnd || bloomed) return@LaunchedEffect
        if (reduce) {
            drawn.snapTo(1f)
            bloom.snapTo(1f)
            bloomed = true
            haptics.success()
            return@LaunchedEffect
        }
        launch { drawn.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
        delay(900)
        bloomed = true
        haptics.success()
        bloom.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 110f))
    }

    val glow = BookInk.glow
    val gild = BookInk.gild
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Box(Modifier.size(120.dp).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
            Canvas(
                Modifier.matchParentSize().graphicsLayer {
                    val b = bloom.value
                    val s = 0.3f + 0.7f * b
                    scaleX = s; scaleY = s
                    alpha = b.coerceIn(0f, 1f)
                }
            ) {
                drawCircle(Brush.radialGradient(listOf(glow, glow.copy(alpha = 0f)), center, size.minDimension / 2))
            }
            Canvas(Modifier.size(width = 52.dp, height = 37.dp)) {
                drawTrimmedLotus(drawn.value, gild, 1.6.dp.toPx())
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.graphicsLayer {
                val b = bloom.value.coerceIn(0f, 1f)
                alpha = b
                if (!reduce && android.os.Build.VERSION.SDK_INT >= 31 && b < 1f) {
                    val r = (1f - b) * 6.dp.toPx()
                    renderEffect = android.graphics.RenderEffect
                        .createBlurEffect(r.coerceAtLeast(0.01f), r.coerceAtLeast(0.01f), android.graphics.Shader.TileMode.DECAL)
                        .asComposeRenderEffect()
                } else {
                    renderEffect = null
                }
            },
        ) {
            Text("Alhamdulillah", style = TextStyles.title3.copy(fontFamily = AppFont.serif), color = BookInk.ink)
            Text("You finished ${current.name}.", style = TextStyles.subheadline, color = BookInk.caption)
        }

        if (current.id < 114) {
            val next = quran.surah(current.id + 1)
            Row(
                Modifier
                    .padding(top = Space.s)
                    .heightIn(min = 50.dp)
                    .pressable(scale = 0.98f) { router.push(Route.Reader(next.id, null)) }
                    .clip(CircleShape)
                    .background(Ink.wash)
                    .padding(horizontal = Space.l),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                Text("Next: ${next.name}", style = TextStyles.headline, color = BookInk.caption)
                Icon(symbol("arrow.right"), null, tint = BookInk.caption, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Draws the lotus stroke up to a fraction of its full length, part after part. */
private fun DrawScope.drawTrimmedLotus(fraction: Float, color: Color, width: Float) {
    if (fraction <= 0f) return
    val rect = Rect(Offset.Zero, size)
    val parts = listOf(LotusPart.Center, LotusPart.Left, LotusPart.Right, LotusPart.Water).map { lotusPath(it, rect) }
    val measures = parts.map { PathMeasure().apply { setPath(it, false) } }
    val lengths = measures.map { it.length }
    var remaining = lengths.sum() * fraction.coerceIn(0f, 1f)
    val stroke = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
    for (i in parts.indices) {
        if (remaining <= 0f) break
        val segment = min(remaining, lengths[i])
        val dst = Path()
        measures[i].getSegment(0f, segment, dst, true)
        drawPath(dst, color, style = stroke)
        remaining -= segment
    }
}
