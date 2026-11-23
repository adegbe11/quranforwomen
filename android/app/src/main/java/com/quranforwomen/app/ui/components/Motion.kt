package com.quranforwomen.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color as GColor
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import kotlinx.coroutines.delay

/**
 * A quiet press response for whole cards, tiles and buttons: it dips a little and softens,
 * then springs back. No ripple, the way iOS does it.
 */
fun Modifier.pressable(
    scale: Float = 0.97f,
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) scale else 1f, Motion.press(), label = "press")
    val a by animateFloatAsState(if (pressed) 0.88f else 1f, Motion.press(), label = "pressAlpha")
    this
        .graphicsLayer { scaleX = s; scaleY = s; alpha = a }
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            role = role,
            onClickLabel = onClickLabel,
            onClick = onClick,
        )
}

/**
 * Content arrives one piece after another, like cards being dealt, then settles.
 * Plays once per screen visit; with animations off it simply appears.
 */
fun Modifier.dealIn(index: Int, baseDelay: Int = 150, step: Int = 70): Modifier = composed {
    val reduce = LocalReduceMotion.current
    var shown by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val progress = remember { Animatable(if (shown || reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            delay((baseDelay + index * step).toLong())
            progress.animateTo(1f, Motion.settle())
        }
        shown = true
    }
    graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * 24.px(density)
        val sc = 0.97f + 0.03f * p
        scaleX = sc; scaleY = sc
        transformOrigin = TransformOrigin(0.5f, 0f)
    }
}

/** Comes into focus: fades in from a soft blur, rising a few points. */
fun Modifier.focusIn(delayMillis: Int = 0): Modifier = composed {
    val reduce = LocalReduceMotion.current
    var shown by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val progress = remember { Animatable(if (shown || reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) progress.animateTo(1f, Motion.entrance(delayMillis))
        shown = true
    }
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * 6.px(density)
        // A real blur needs Android 12; below that the fade and rise carry it.
        if (android.os.Build.VERSION.SDK_INT >= 31 && p < 1f) {
            val r = (1f - p) * 10.px(density)
            renderEffect = android.graphics.RenderEffect.createBlurEffect(r.coerceAtLeast(0.01f), r.coerceAtLeast(0.01f), android.graphics.Shader.TileMode.DECAL).asComposeRenderEffect()
        } else {
            renderEffect = null
        }
    }
}

/** Reveals Arabic from right to left, the direction it is written, with a soft leading edge. */
fun Modifier.inkReveal(delayMillis: Int = 0, durationMillis: Int = 1100): Modifier = composed {
    val reduce = LocalReduceMotion.current
    var shown by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val progress = remember { Animatable(if (shown || reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) progress.animateTo(1f, tween(durationMillis, delayMillis, androidx.compose.animation.core.FastOutSlowInEasing))
        shown = true
    }
    this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val p = progress.value
            if (p >= 1f) return@drawWithContent
            val edge = 60.px(density)
            val w = size.width
            // The visible part grows from the right edge leftward.
            val revealStart = w - (w + edge) * p
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to GColor.Black, 1f to GColor.Transparent,
                    startX = revealStart, endX = revealStart + edge,
                ),
                blendMode = BlendMode.DstOut,
            )
        }
}

/** Cards ease in as they scroll into view at the bottom edge, and soften as they leave at the top. */
fun Modifier.scrollSettle(): Modifier = composed {
    if (LocalReduceMotion.current) return@composed this
    val screenH = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.toFloat() * density }
    var factor by remember { mutableFloatStateOf(1f) }
    this
        .onGloballyPositioned { coords ->
            val b = coords.boundsInWindow()
            val zone = screenH * 0.12f
            factor = when {
                b.bottom < zone -> (b.bottom / zone).coerceIn(0f, 1f)
                b.top > screenH - zone -> ((screenH - b.top) / zone).coerceIn(0f, 1f)
                else -> 1f
            }
        }
        .graphicsLayer {
            val sc = 0.95f + 0.05f * factor
            scaleX = sc; scaleY = sc
            alpha = 0.55f + 0.45f * factor
        }
}

/** A number that rolls to its new value, digits sliding up or down. */
@Composable
fun RollingNumber(value: Int, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            if (reduce) fadeIn(tween(120)) togetherWith fadeOut(tween(120))
            else {
                val up = targetState > initialState
                (slideInVertically(spring(dampingRatio = 0.8f, stiffness = 500f)) { if (up) it else -it } + fadeIn()) togetherWith
                    (slideOutVertically(spring(dampingRatio = 0.8f, stiffness = 500f)) { if (up) -it else it } + fadeOut())
            }
        },
        label = "rolling",
        modifier = modifier,
    ) { n ->
        Row { Text(n.toString(), style = style, color = color) }
    }
}

/** Plays a short sequence once, for screens that stage their entrance. Returns true when done. */
@Composable
fun rememberStaged(delayMillis: Long): Boolean {
    val reduce = LocalReduceMotion.current
    var ready by rememberSaveable { androidx.compose.runtime.mutableStateOf(reduce) }
    LaunchedEffect(Unit) {
        if (!ready) { delay(delayMillis); ready = true }
    }
    return ready
}

internal fun Int.px(density: Float): Float = this * density
