package com.quranforwomen.app.feature.explore

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quranforwomen.app.ui.components.LotusPart
import com.quranforwomen.app.ui.components.lotusPath
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Palette
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** The lotus mark, drawn up to a fraction of its full length, so it can draw itself in. */
@Composable
internal fun ExploreDrawnLotus(progress: Float, color: Color, strokeWidth: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val p = progress.coerceIn(0f, 1f)
        if (p <= 0f) return@Canvas
        val full = lotusPath(LotusPart.All, Rect(Offset.Zero, size)).asAndroidPath()
        val measure = android.graphics.PathMeasure(full, false)
        val lengths = mutableListOf<Float>()
        do { lengths += measure.length } while (measure.nextContour())
        var remaining = lengths.sum() * p
        val out = android.graphics.Path()
        measure.setPath(full, false)
        var i = 0
        do {
            if (remaining <= 0f || i >= lengths.size) break
            val length = lengths[i++]
            measure.getSegment(0f, min(length, remaining), out, true)
            remaining -= length
        } while (measure.nextContour())
        drawPath(
            out.asComposePath(),
            color,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/** Twelve small gold and emerald sparks that fly out from the centre and fade. Nothing with animations off. */
@Composable
internal fun ExploreSparks(fire: Int, from: Dp, to: Dp, modifier: Modifier = Modifier) {
    if (LocalReduceMotion.current) return
    val t = remember { Animatable(1f) }
    LaunchedEffect(fire) {
        if (fire > 0) {
            t.snapTo(0f)
            t.animateTo(1f, tween(750, easing = LinearEasing))
        }
    }
    val gold = Color(0xFFD3A94A)
    val green = Palette.accentFill
    Canvas(modifier.size(1.dp)) {
        val p = t.value
        if (fire == 0 || p >= 1f) return@Canvas
        val time = p * 0.75f
        val k = (time / 0.7f).coerceIn(0f, 1f)
        val ease = 1f - (1f - k).pow(3)
        val distance = (from.toPx() + (to.toPx() - from.toPx()) * ease)
        val opacity = if (time < 0.4f) 1f else (1f - (time - 0.4f) / 0.35f).coerceIn(0f, 1f)
        val scale = if (time < 0.2f) 1f + 0.3f * (time / 0.2f) else 1.3f - 0.8f * ((time - 0.2f) / 0.5f).coerceIn(0f, 1f)
        val r = 2.5.dp.toPx() * scale
        for (i in 0 until 12) {
            val angle = i / 12.0 * 2 * Math.PI
            val c = center + Offset((cos(angle) * distance).toFloat(), (sin(angle) * distance).toFloat())
            drawCircle((if (i % 2 == 0) gold else green).copy(alpha = opacity), radius = r, center = c)
        }
    }
}
