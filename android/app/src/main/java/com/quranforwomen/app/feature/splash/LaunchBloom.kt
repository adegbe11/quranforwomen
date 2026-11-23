package com.quranforwomen.app.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.ui.components.LotusPart
import com.quranforwomen.app.ui.components.lotusPath
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

private val Cream = Color(0xFFFAF7F0)
private val Gold = Color(0xFFE9CF8F)
private val Emerald = Color(0xFF064E3B)

/**
 * The brand splash. The system launch screen is plain emerald; the lotus is then drawn live,
 * petal by petal, a light fills it and a gold shimmer passes once, the name draws together
 * beneath it, and a circle opens from the lotus to reveal the app.
 */
@Composable
fun LaunchBloom(onDone: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val haptics = rememberHaptics()
    val center = remember { Animatable(0f) }
    val sides = remember { Animatable(0f) }
    val water = remember { Animatable(0f) }
    val lit = remember { Animatable(0f) }
    val shimmer = remember { Animatable(-1f) }
    val named = remember { Animatable(0f) }
    val reveal = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (reduce) {
            center.snapTo(1f); sides.snapTo(1f); water.snapTo(1f); lit.snapTo(1f); named.snapTo(1f)
            delay(700)
            reveal.animateTo(1f, tween(350))
            onDone()
            return@LaunchedEffect
        }
        coroutineScope {
            delay(100)
            launch { center.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
            launch { delay(150); sides.animateTo(1f, tween(550, easing = FastOutSlowInEasing)) }
            launch { delay(300); water.animateTo(1f, tween(500, easing = LinearOutSlowInEasing)) }
            delay(650)
            haptics.soft()
            launch { lit.animateTo(1f, tween(600, easing = LinearOutSlowInEasing)) }
            launch { shimmer.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
            delay(100)
            launch { named.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 110f)) }
            delay(800)
            reveal.animateTo(1f, tween(550, easing = FastOutLinearInEasing))
        }
        onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .clearAndSetSemantics { }
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
        contentAlignment = Alignment.Center,
    ) {
        // The emerald ground, deepening as the lotus lights, with a circle cut out to show the app.
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Emerald)
            drawRect(
                Brush.verticalGradient(listOf(Color(0xFF032A20), Emerald, Color(0xFF0B6A50))),
                alpha = lit.value,
            )
            val glowR = 220.dp.toPx() * (0.4f + 0.6f * lit.value)
            drawCircle(
                Brush.radialGradient(listOf(Cream.copy(alpha = 0.16f * lit.value), Color.Transparent), center = this.center, radius = glowR),
                radius = glowR,
            )
            val cover = hypot(size.width, size.height) * 1.1f
            if (reveal.value > 0f) {
                drawCircle(Color.Black, radius = cover * reveal.value / 2, center = this.center, blendMode = BlendMode.Clear)
            }
        }

        val leaving = reveal.value
        Column(
            Modifier
                // Keeps the lotus at the exact centre while the name sits below it.
                .offset(y = 29.dp)
                .graphicsLayer {
                    val s = 1f + 0.35f * leaving + 0.04f * lit.value * (1 - leaving)
                    scaleX = s; scaleY = s
                    alpha = 1f - leaving
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Canvas(Modifier.size(96.dp, 69.dp)) { drawLotus(center.value, sides.value, water.value, lit.value, shimmer.value) }
            Text(
                "Quran for Women",
                style = TextStyle(
                    fontFamily = AppFont.serif, fontSize = 30.sp, fontWeight = FontWeight.SemiBold,
                    letterSpacing = (9f - 8.6f * named.value).sp,
                ),
                color = Cream,
                softWrap = false,
                modifier = Modifier.graphicsLayer { alpha = named.value.coerceIn(0f, 1f) },
            )
        }
    }
}

private fun DrawScope.drawLotus(center: Float, sides: Float, water: Float, lit: Float, shimmer: Float) {
    val rect = Rect(Offset.Zero, size)
    val stroke = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val all = lotusPath(LotusPart.All, rect)
    drawPath(all, Cream.copy(alpha = 0.12f * lit))
    drawPath(trimmed(lotusPath(LotusPart.Center, rect), 0f, center), Cream, style = stroke)
    drawPath(trimmed(lotusPath(LotusPart.Left, rect), 0f, sides), Cream, style = stroke)
    drawPath(trimmed(lotusPath(LotusPart.Right, rect), 0f, sides), Cream, style = stroke)
    drawPath(trimmed(lotusPath(LotusPart.Water, rect), 0.5f - water / 2, 0.5f + water / 2), Cream, style = stroke)
    // One pass of gold light across the lotus, only where the lines are.
    if (shimmer > -1f && shimmer < 1f) {
        val x = size.width / 2 + shimmer * 80.dp.toPx()
        drawPath(
            all,
            Brush.horizontalGradient(
                listOf(Color.Transparent, Gold.copy(alpha = 0.9f), Color.Transparent),
                startX = x - 20.dp.toPx(), endX = x + 20.dp.toPx(),
            ),
            style = stroke,
        )
    }
}

/** The part of a path from one fraction of its length to another, like SwiftUI's trim. */
private fun trimmed(path: Path, from: Float, to: Float): Path {
    val out = android.graphics.Path()
    if (to <= from) return out.asComposePath()
    val src = path.asAndroidPath()
    // A path made of several contours is measured contour by contour.
    val measure = android.graphics.PathMeasure(src, false)
    val lengths = mutableListOf<Float>()
    do { lengths += measure.length } while (measure.nextContour())
    val total = lengths.sum()
    val start = from * total
    val end = to * total
    val walk = android.graphics.PathMeasure(src, false)
    var walked = 0f
    for (len in lengths) {
        val s = (start - walked).coerceIn(0f, len)
        val e = (end - walked).coerceIn(0f, len)
        if (e > s) walk.getSegment(s, e, out, true)
        walked += len
        if (!walk.nextContour()) break
    }
    return out.asComposePath()
}
