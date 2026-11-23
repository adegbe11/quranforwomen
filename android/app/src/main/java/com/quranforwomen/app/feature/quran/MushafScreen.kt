package com.quranforwomen.app.feature.quran

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.SystemBarIcons
import com.quranforwomen.app.data.QuranStore
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.ui.components.RollingNumber
import com.quranforwomen.app.ui.components.glass
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The colours of a leather bound book, the same edition as PrayerKey's classic Bible:
 * red brown leather tooled in real gold, cream leaves, a red silk ribbon, on a dark desk.
 */
internal object MushafInk {
    val desk = Color(0xFF17130D)
    val deskLit = Color(0xFF2A2016)
    val paper = Color(0xFFF6EFDC)
    val paperLit = Color(0xFFFDFAF0)
    val paperEdge = Color(0xFFE6D9B6)
    val ink = Color(0xFF221C12)
    val caption = Color(0xFF5A4828)
    val rule = Color(0xFF8C6E32)
    val gild = Color(0xFFC9A26D)
    val gold = Color(0xFFD4AF37)
    val goldLight = Color(0xFFF3E5AB)
    val goldDeep = Color(0xFF7A5A15)
    val board = Color(0xFF5A1E18)
    val boardDeep = Color(0xFF1B0807)
    val boardLit = Color(0xFF87382A)
    val ribbon = Color(0xFF9A1B2B)
    val ribbonDeep = Color(0xFF8C1F2B)
}

private const val MushafKeyPage = "mushaf.page"
private const val MushafKeyRibbon = "mushaf.ribbon"

/**
 * The Quran as a book. The closed cover comes first, a band of light passing over its gold.
 * It opens like a real mushaf, hinged on the right, with a warm light rising from the page beneath,
 * and the pages turn right to left with a page curl that follows her finger. A ribbon drops in to keep
 * her place, the gilt edge riffles the actual pages under her finger, and closing swings the cover shut first.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MushafScreen(page: Int?) {
    val graph = LocalGraph.current
    val quran = graph.quran
    val sp = graph.prefs.sp
    val router = LocalRouter.current
    val reduce = LocalReduceMotion.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val pages = remember { buildMushafPages(quran) }
    val turn = remember {
        val target = page ?: sp.getInt(MushafKeyPage, 1)
        MushafTurnState(pages.indexOfLast { it.number <= target }.coerceAtLeast(0), pages.size)
    }
    var ribbonPage by remember { mutableIntStateOf(sp.getInt(MushafKeyRibbon, 0)) }
    var opened by rememberSaveable { mutableStateOf(false) }
    var coverGone by rememberSaveable { mutableStateOf(false) }
    var chrome by rememberSaveable { mutableStateOf(true) }
    var closing by remember { mutableStateOf(false) }
    var scrubbing by remember { mutableStateOf<Int?>(null) }
    val coverAngle = remember { Animatable(0f) }
    val bloom = remember { Animatable(0f) }

    SystemBarIcons(light = true)
    MushafStatusBar(hidden = opened && !chrome)

    LaunchedEffect(turn) {
        snapshotFlow { turn.index }.collect { i ->
            pages.getOrNull(i)?.let { sp.edit().putInt(MushafKeyPage, it.number).apply() }
        }
    }

    fun setRibbon(value: Int) {
        ribbonPage = value
        sp.edit().putInt(MushafKeyRibbon, value).apply()
        haptics.tick()
    }

    fun openBook() {
        if (opened) return
        haptics.soft()
        if (reduce) {
            opened = true
            coverGone = true
            return
        }
        opened = true
        scope.launch { coverAngle.animateTo(100f, spring(dampingRatio = 0.86f, stiffness = 48.7f)) }
        scope.launch {
            delay(150)
            bloom.animateTo(1f, tween(600, easing = LinearOutSlowInEasing))
            delay(150)
            bloom.animateTo(0f, tween(900, easing = FastOutLinearInEasing))
        }
        // Take the cover away once it has swung past edge on.
        scope.launch {
            delay(700)
            if (!closing) coverGone = true
        }
    }

    // The cover swings back over the pages, then the book closes.
    fun closeBook() {
        if (closing) return
        if (reduce) {
            router.pop()
            return
        }
        closing = true
        scope.launch {
            coverAngle.snapTo(100f)
            coverGone = false
            // Swing it shut once it has been drawn open.
            withFrameNanos { }
            launch { coverAngle.animateTo(0f, spring(dampingRatio = 0.9f, stiffness = 109.7f)) }
            delay(650)
            router.pop()
        }
    }

    val pagesAlpha by animateFloatAsState(if (opened) 1f else 0f, tween(if (reduce) 300 else 250), label = "pages")
    val topInset = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
    // The page keeps still and leaves room for the bar and the gilt edge,
    // which fade in and out in the margins around it.
    val bookArea = Modifier
        .fillMaxSize()
        .padding(start = Space.xs, end = Space.xs, top = topInset + 52.dp, bottom = bottomInset + 48.dp)

    Box(
        Modifier
            .fillMaxSize()
            .background(MushafInk.desk)
            .background(Brush.radialGradient(listOf(MushafInk.deskLit, MushafInk.desk), radius = with(density) { 520.dp.toPx() }))
    ) {
        MushafBook(
            pages = pages,
            quran = quran,
            turn = turn,
            ribbonPage = ribbonPage,
            opened = opened,
            onTapMiddle = { chrome = !chrome },
            modifier = bookArea.graphicsLayer { alpha = pagesAlpha },
        )

        // The light that rises from the page as the book opens.
        MushafOpeningLight(bloom = { bloom.value })

        if (!coverGone) {
            MushafCover(
                shining = !opened && !reduce,
                // Hinged on the right, like a mushaf, so the cover swings open to the right.
                modifier = bookArea
                    .graphicsLayer {
                        rotationY = -coverAngle.value
                        transformOrigin = TransformOrigin(1f, 0.5f)
                        cameraDistance = 6f * density.density
                    }
                    .clearAndSetSemantics {
                        contentDescription = "The Holy Quran"
                        role = Role.Button
                        onClick(label = "Open") { openBook(); true }
                    }
                    .pointerInput(Unit) { detectTapGestures { openBook() } }
                    .pointerInput(Unit) {
                        var dx = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dx = 0f },
                            onHorizontalDrag = { change, amount ->
                                dx += amount
                                change.consume()
                                if (dx > 40.dp.toPx()) openBook()
                            },
                        )
                    },
            )
        }

        // The bar slides down from the top and the gilt edge rises from the bottom.
        AnimatedVisibility(
            visible = opened && chrome && !closing,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            MushafTopBar(
                page = pages.getOrNull(turn.index),
                quran = quran,
                ribbonPage = ribbonPage,
                topInset = topInset,
                onClose = { closeBook() },
                onRibbon = { setRibbon(it) },
                onGoToRibbon = {
                    val target = pages.indexOfFirst { it.number == ribbonPage }
                    if (target >= 0) {
                        val diff = target - turn.index
                        if (!reduce && abs(diff) == 1) turn.turn(scope, diff > 0, 1200f) else turn.jump(target)
                    }
                },
            )
        }
        AnimatedVisibility(
            visible = opened && chrome && !closing,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            MushafGiltEdge(
                pages = pages,
                quran = quran,
                index = turn.index,
                scrubbing = scrubbing,
                onScrub = { new ->
                    val old = scrubbing
                    scrubbing = new
                    // Riffling: the page under her finger is the page she sees.
                    if (new != null && new != turn.index) turn.jump(new)
                    if (old != null && new != null && old != new) haptics.tick()
                },
                onStep = { forward ->
                    val to = turn.index + if (forward) 1 else -1
                    if (to in pages.indices) turn.jump(to)
                },
                modifier = Modifier
                    .padding(horizontal = Space.page)
                    .padding(bottom = bottomInset + Space.s),
            )
        }
    }
}

/** Hides the status bar while she reads with the bar away, as the iPhone does. */
@Composable
private fun MushafStatusBar(hidden: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(hidden) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        if (controller != null) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (hidden) controller.hide(WindowInsetsCompat.Type.statusBars())
            else controller.show(WindowInsetsCompat.Type.statusBars())
        }
        onDispose { controller?.show(WindowInsetsCompat.Type.statusBars()) }
    }
}

@Composable
private fun MushafTopBar(
    page: MushafPageData?,
    quran: QuranStore,
    ribbonPage: Int,
    topInset: androidx.compose.ui.unit.Dp,
    onClose: () -> Unit,
    onRibbon: (Int) -> Unit,
    onGoToRibbon: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val bounce = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val reduce = LocalReduceMotion.current
    val here = page?.number
    val marked = here != null && ribbonPage == here

    fun ribbon(value: Int) {
        menu = false
        onRibbon(value)
        if (!reduce) scope.launch {
            bounce.animateTo(1.25f, tween(120))
            bounce.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 400f))
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = topInset + Space.xs)
            .padding(horizontal = Space.page),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MushafGlassButton(symbol("xmark"), "Close", onClose)

        Spacer(Modifier.weight(1f))

        val first = page?.verses?.firstOrNull()
        if (page != null && first != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.semantics(mergeDescendants = true) { heading() },
            ) {
                Text(
                    quran.surah(first.surah).name,
                    style = TextStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
                Row {
                    val faint = Color.White.copy(alpha = 0.75f)
                    Text("Juz ${quran.juz(first)} · Page ", style = TextStyles.caption, color = faint)
                    RollingNumber(page.number, TextStyles.caption, faint)
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Box {
            MushafGlassButton(
                symbol(if (marked) "bookmark.fill" else "bookmark"),
                "Ribbon",
                onClick = { menu = true },
                iconModifier = Modifier.graphicsLayer { scaleX = bounce.value; scaleY = bounce.value },
            )
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                containerColor = MushafInk.deskLit,
            ) {
                if (here != null) {
                    if (marked) {
                        DropdownMenuItem(
                            text = { Text("Remove Ribbon", color = Color.White) },
                            leadingIcon = { Icon(symbol("bookmark.slash"), null, tint = Color.White) },
                            onClick = { ribbon(0) },
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Place Ribbon Here", color = Color.White) },
                            leadingIcon = { Icon(symbol("bookmark"), null, tint = Color.White) },
                            onClick = { ribbon(here) },
                        )
                    }
                }
                if (ribbonPage > 0 && ribbonPage != here) {
                    DropdownMenuItem(
                        text = { Text("Go to Ribbon, Page $ribbonPage", color = Color.White) },
                        leadingIcon = { Icon(symbol("arrow.uturn.right"), null, tint = Color.White) },
                        onClick = { menu = false; onGoToRibbon() },
                    )
                }
            }
        }
    }
}

@Composable
private fun MushafGlassButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    iconModifier: Modifier = Modifier,
) {
    Box(
        Modifier
            .size(44.dp)
            .pressable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(40.dp).glass(CircleShape, onDark = true), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = iconModifier.size(20.dp))
        }
    }
}

/**
 * Drag along the gilt edge to riffle through the book. Page 1 is on the right, as in a mushaf.
 * A tick for every page passed, and the surah under her finger shown above it.
 */
@Composable
private fun MushafGiltEdge(
    pages: List<MushafPageData>,
    quran: QuranStore,
    index: Int,
    scrubbing: Int?,
    onScrub: (Int?) -> Unit,
    onStep: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = pages.size
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(30.dp)
            .semantics {
                contentDescription = "Page"
                stateDescription = "${pages.getOrNull(index)?.number ?: 1} of $count"
                progressBarRangeInfo = ProgressBarRangeInfo(index.toFloat(), 0f..(count - 1).coerceAtLeast(1).toFloat(), steps = (count - 2).coerceAtLeast(0))
                setProgress { value ->
                    val target = value.roundToInt().coerceIn(0, count - 1)
                    if (target != index) onStep(target > index)
                    true
                }
            }
            .pointerInput(count) {
                awaitEachGesture {
                    fun at(x: Float): Int {
                        val f = 1f - (x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        return (f * (count - 1)).roundToInt()
                    }
                    val down = awaitFirstDown()
                    onScrub(at(down.position.x))
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        onScrub(at(change.position.x))
                    }
                    onScrub(null)
                }
            }
    ) {
        val width = constraints.maxWidth.toFloat()
        val shown = scrubbing ?: index
        val fraction = if (count > 1) shown / (count - 1f) else 0f
        // Right to left: page 1 sits at the right end.
        val x = width * (1f - fraction)
        val knob by animateDpAsState(if (scrubbing == null) 16.dp else 22.dp, spring(dampingRatio = 0.8f, stiffness = 600f), label = "knob")

        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFFB49B5F), Color(0xFFFFF0C8), Color(0xFFDED0A8))))
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((x - knob.toPx() / 2).roundToInt(), 0) }
                .size(knob)
                .shadow(3.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
                .background(MushafInk.goldLight, CircleShape)
        )

        var bubbleWidth by remember { mutableIntStateOf(0) }
        var lastShown by remember { mutableIntStateOf(shown) }
        if (scrubbing != null) lastShown = scrubbing
        AnimatedVisibility(
            visible = scrubbing != null,
            enter = scaleIn(initialScale = 0.8f) + fadeIn(),
            exit = scaleOut(targetScale = 0.8f) + fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset {
                    val left = (x - bubbleWidth / 2f).coerceIn(0f, (width - bubbleWidth).coerceAtLeast(0f))
                    IntOffset(left.roundToInt(), with(density) { -38.dp.roundToPx() })
                }
                .onSizeChanged { bubbleWidth = it.width },
        ) {
            val p = pages.getOrNull(lastShown)
            val first = p?.verses?.firstOrNull()
            if (p != null && first != null) {
                Column(
                    Modifier
                        .background(MushafInk.goldLight, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        quran.surah(first.surah).name,
                        style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold),
                        color = MushafInk.boardDeep,
                    )
                    Row {
                        Text("Page ", style = TextStyles.caption.copy(fontSize = TextStyles.caption.fontSize * 0.92f), color = MushafInk.boardDeep)
                        RollingNumber(p.number, TextStyles.caption.copy(fontSize = TextStyles.caption.fontSize * 0.92f), MushafInk.boardDeep)
                    }
                }
            }
        }
    }
}
