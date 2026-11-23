package com.quranforwomen.app.feature.quran

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.quranforwomen.app.data.QuranStore
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * The page being turned. The spine is on the right, so a leaf lifts from its left edge and
 * turns right to left, the way a mushaf is read.
 *
 * A turn always moves one leaf: going forward it is the open page, going back it is the page before,
 * which comes back from the far side of the spine. progress is how far that leaf has turned,
 * 0 lying open and 1 turned over, so a turn back simply runs from 1 to 0.
 */
@Stable
internal class MushafTurnState(initial: Int, val count: Int) {
    var index by mutableIntStateOf(initial)
        private set

    /** The leaf being turned, or null when the book lies still. The page under it is leaf + 1. */
    var leaf by mutableStateOf<Int?>(null)
        private set

    var progress by mutableFloatStateOf(0f)

    /** Where her finger took hold of the leaf, from the top of the page. */
    var grabY by mutableFloatStateOf(0f)
        private set

    /** How far the held corner has been lifted up or down, which tilts the fold. */
    var lift by mutableFloatStateOf(0f)

    var onTurned: () -> Unit = {}

    private var job: Job? = null
    private var target = 0f

    fun begin(leafIndex: Int, start: Float, y: Float, tilt: Float = 0f) {
        leaf = leafIndex
        progress = start
        grabY = y
        lift = tilt
    }

    fun canForward() = index < count - 1
    fun canBack() = index > 0

    private fun commit(to: Float) {
        val l = leaf ?: return
        val next = if (to >= 0.5f) l + 1 else l
        leaf = null
        progress = 0f
        lift = 0f
        if (next != index) {
            index = next
            onTurned()
        }
    }

    /** Lets a turn that is still settling land at once, so a new touch starts from a still page. */
    fun finishNow() {
        val running = job
        if (running != null && running.isActive) {
            running.cancel()
            commit(target)
        }
        job = null
    }

    fun settle(scope: CoroutineScope, to: Float, velocity: Float, spec: AnimationSpec<Float>) {
        target = to
        val from = progress
        job = scope.launch {
            animate(from, to, velocity, spec) { value, _ -> progress = value }
            commit(to)
        }
    }

    /** Opens straight at a page, without turning. */
    fun jump(to: Int) {
        finishNow()
        leaf = null
        progress = 0f
        index = to.coerceIn(0, count - 1)
    }

    /** A tap on the edge turns one page the whole way. */
    fun turn(scope: CoroutineScope, forward: Boolean, height: Float) {
        finishNow()
        if (forward && !canForward() || !forward && !canBack()) return
        if (forward) begin(index, 0f, height * 0.82f, -height * 0.1f)
        else begin(index - 1, 1f, height * 0.82f, -height * 0.1f)
        settle(scope, if (forward) 1f else 0f, 0f, tween(640, easing = FastOutSlowInEasing))
    }
}

private val ReleaseSpring = spring(dampingRatio = 1f, stiffness = 240f, visibilityThreshold = 0.0005f)

/**
 * The open book. Pages follow her finger: drag to the right to turn forward, to the left to go back,
 * tap the left or right edge to turn, tap the middle for the bar. With animations off a page simply fades.
 */
@Composable
internal fun MushafBook(
    pages: List<MushafPageData>,
    quran: QuranStore,
    turn: MushafTurnState,
    ribbonPage: Int,
    opened: Boolean,
    onTapMiddle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduce = LocalReduceMotion.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val sizeCache = remember { HashMap<String, Float>() }
    val tapMiddle by rememberUpdatedState(onTapMiddle)
    SideEffect { turn.onTurned = { haptics.tick() } }

    fun step(forward: Boolean, height: Float) {
        if (reduce) {
            if (forward && turn.canForward()) { turn.jump(turn.index + 1); haptics.tick() }
            if (!forward && turn.canBack()) { turn.jump(turn.index - 1); haptics.tick() }
        } else {
            turn.turn(scope, forward, height)
        }
    }

    Box(
        modifier
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Next page") { step(true, 1000f); turn.canForward() },
                    CustomAccessibilityAction("Previous page") { step(false, 1000f); turn.canBack() },
                )
            }
            .pointerInput(reduce) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    turn.finishNow()
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val span = w * 0.9f
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    var start = down.position
                    var last = down.position
                    // 0 still a tap, 1 turning a leaf, 2 something else, 3 a swipe with animations off.
                    var mode = 0
                    var forward = true
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        tracker.addPosition(change.uptimeMillis, change.position)
                        last = change.position
                        if (!change.pressed) break
                        val d = change.position - start
                        if (mode == 0 && d.getDistance() > viewConfiguration.touchSlop) {
                            mode = if (abs(d.x) > abs(d.y)) {
                                forward = d.x > 0
                                val can = if (forward) turn.canForward() else turn.canBack()
                                when {
                                    !can -> 2
                                    reduce -> 3
                                    else -> {
                                        turn.begin(if (forward) turn.index else turn.index - 1, if (forward) 0f else 1f, start.y)
                                        start = change.position
                                        1
                                    }
                                }
                            } else 2
                        }
                        if (mode == 1) {
                            val dx = change.position.x - start.x
                            turn.progress = (if (forward) dx / span else 1f + dx / span).coerceIn(0f, 1f)
                            turn.lift = (change.position.y - start.y) * 0.5f
                            change.consume()
                        }
                    }
                    when (mode) {
                        0 -> when {
                            last.x < w * 0.2f -> step(true, h)
                            last.x > w * 0.8f -> step(false, h)
                            else -> tapMiddle()
                        }
                        1 -> {
                            val v = tracker.calculateVelocity().x / span
                            val p = turn.progress
                            val to = if (forward) {
                                if ((p > 0.33f || v > 1.2f) && v > -0.8f) 1f else 0f
                            } else {
                                if ((p < 0.67f || v < -1.2f) && v < 0.8f) 0f else 1f
                            }
                            turn.settle(scope, to, v, ReleaseSpring)
                        }
                        3 -> step(forward, h)
                    }
                }
            }
    ) {
        if (reduce) {
            Crossfade(turn.index, animationSpec = tween(250), label = "mushafPage") { i ->
                MushafPageView(pages[i], quran, ribbonPage == pages[i].number, opened, sizeCache)
            }
        } else {
            val i = turn.index
            for (slot in (i - 1)..(i + 1)) {
                if (slot !in pages.indices) continue
                key(pages[slot].number) {
                    MushafLeafSlot(slot, turn) { visible ->
                        MushafPageView(pages[slot], quran, ribbonPage == pages[slot].number, visible && opened, sizeCache)
                    }
                }
            }
        }
    }
}

/**
 * Holds one page. The open page draws as it is; the turning leaf is drawn into a layer once
 * and painted twice, the part still lying flat and the folded part, mirrored and lit from behind.
 * The pages either side are kept ready but not drawn, so a turn starts without a stall.
 */
@Composable
private fun MushafLeafSlot(i: Int, turn: MushafTurnState, content: @Composable (visible: Boolean) -> Unit) {
    val leaf = turn.leaf
    val visible = if (leaf == null) turn.index == i else (leaf == i || leaf + 1 == i)
    val layer = rememberGraphicsLayer()
    Box(
        Modifier
            .fillMaxSize()
            .zIndex(if (leaf == i) 1f else 0f)
            .then(if (visible) Modifier else Modifier.clearAndSetSemantics { })
            .drawWithContent {
                val l = turn.leaf
                when {
                    l == i -> {
                        layer.record { this@drawWithContent.drawContent() }
                        drawTurningLeaf(layer, turn.progress, turn.grabY, turn.lift)
                    }
                    l == null && turn.index == i -> drawContent()
                    l != null && l + 1 == i -> drawContent()
                }
            }
    ) { content(visible) }
}

/**
 * The fold. The leaf's left edge, held at grabY, is carried to T. The fold line is halfway between,
 * at right angles; what lies on the near side of it is lifted and laid over, mirrored.
 */
private fun DrawScope.drawTurningLeaf(layer: GraphicsLayer, rawProgress: Float, grabY: Float, lift: Float) {
    val p = rawProgress.coerceIn(0f, 1f)
    if (p <= 0.0005f) {
        drawLayer(layer)
        return
    }
    if (p >= 0.9995f) return
    val w = size.width
    val h = size.height
    val bend = sin(PI * p).toFloat()
    val c = Offset(0f, grabY)
    val t = Offset(2f * w * p, grabY + lift * bend)
    val m = (c + t) / 2f
    val d = t - c
    val len = d.getDistance().coerceAtLeast(0.001f)
    val n = d / len

    val page = listOf(Offset(0f, 0f), Offset(w, 0f), Offset(w, h), Offset(0f, h))
    val flat = clipHalf(page, m, n, keepAhead = true)
    val lifted = clipHalf(page, m, n, keepAhead = false)
    val reflect = reflection(m, n)
    val flap = lifted.map { reflect.map(it) }

    // The page beneath, darkened in the hollow of the fold.
    if (lifted.size >= 3) {
        clipPath(polygon(lifted)) {
            val reach = 46.dp.toPx() + len * 0.08f
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Black.copy(alpha = 0.34f * bend + 0.06f), Color.Transparent),
                    start = m, end = m - n * reach,
                )
            )
        }
    }

    // The part of the leaf still lying flat.
    if (flat.size >= 3) {
        clipPath(polygon(flat)) {
            drawLayer(layer)
            // The lifted paper casts a soft shadow just past its edge.
            val reach = 22.dp.toPx()
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Black.copy(alpha = 0.26f * bend), Color.Transparent),
                    start = t, end = t + n * reach,
                )
            )
        }
    }

    if (lifted.size < 3) return

    // The back of the leaf: the same page seen through the paper, mirrored and washed with paper.
    withTransform({ transform(reflect) }) {
        clipPath(polygon(lifted)) {
            drawLayer(layer)
            drawRoundRect(
                MushafInk.paperLit.copy(alpha = 0.86f),
                cornerRadius = CornerRadius(6.dp.toPx()),
            )
        }
    }

    // Light on the curve of the paper: a crease at the fold, a bright band, falling off to the edge.
    clipPath(polygon(flap)) {
        drawRect(
            Brush.linearGradient(
                0f to Color.Black.copy(alpha = 0.20f),
                0.12f to Color.Black.copy(alpha = 0.04f),
                0.32f to Color.White.copy(alpha = 0.32f),
                0.7f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.10f),
                start = m, end = t,
            )
        )
    }
}

/** Keeps the part of a polygon on one side of the line through m with normal n. */
private fun clipHalf(poly: List<Offset>, m: Offset, n: Offset, keepAhead: Boolean): List<Offset> {
    fun side(p: Offset): Float {
        val s = (p.x - m.x) * n.x + (p.y - m.y) * n.y
        return if (keepAhead) s else -s
    }
    val out = ArrayList<Offset>(6)
    for (k in poly.indices) {
        val a = poly[k]
        val b = poly[(k + 1) % poly.size]
        val sa = side(a)
        val sb = side(b)
        if (sa >= 0f) out += a
        if ((sa >= 0f) != (sb >= 0f)) {
            val f = sa / (sa - sb)
            out += a + (b - a) * f
        }
    }
    return out
}

private fun polygon(points: List<Offset>): Path = Path().apply {
    points.forEachIndexed { k, p -> if (k == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
    close()
}

/** The mirror across the fold line through m with unit normal n: X' = X - 2((X - m) . n) n. */
private fun reflection(m: Offset, n: Offset): Matrix {
    val dot = m.x * n.x + m.y * n.y
    return Matrix().apply {
        this[0, 0] = 1f - 2f * n.x * n.x
        this[1, 0] = -2f * n.x * n.y
        this[3, 0] = 2f * dot * n.x
        this[0, 1] = -2f * n.x * n.y
        this[1, 1] = 1f - 2f * n.y * n.y
        this[3, 1] = 2f * dot * n.y
    }
}
