package com.quranforwomen.app.feature.quran

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quranforwomen.app.ui.theme.AppFont
import kotlinx.coroutines.delay

private val MushafGold = listOf(MushafInk.goldLight, MushafInk.gold, MushafInk.goldDeep)

/**
 * The closed mushaf: a leather board with tooled gold. Every gold line is drawn three times,
 * a shadow below, the metal, and a highlight above, so it reads as pressed into the board.
 * Drawn rather than a photograph: crisp at any size, and nothing to license.
 */
@Composable
internal fun MushafCover(shining: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(10.dp)
    BoxWithConstraints(
        modifier
            .shadow(16.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
    ) {
        val density = LocalDensity.current
        val wPx = constraints.maxWidth.toFloat()
        Canvas(Modifier.fillMaxSize()) { drawCover() }
        val gold = Brush.verticalGradient(MushafGold)
        val shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, with(density) { 1.5.dp.toPx() }), with(density) { 2.dp.toPx() })
        Column(
            Modifier.align(Alignment.Center).offset(y = maxHeight * 0.03f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(maxHeight * 0.018f),
        ) {
            Text(
                "القرآن الكريم",
                style = TextStyle(
                    fontFamily = AppFont.arabic,
                    fontSize = with(density) { (wPx * 0.13f).toSp() },
                    brush = gold, shadow = shadow, textAlign = TextAlign.Center,
                ),
            )
            Text(
                "The Holy Quran",
                style = TextStyle(
                    fontFamily = AppFont.serif,
                    fontWeight = FontWeight.Medium,
                    fontSize = with(density) { (wPx * 0.07f).toSp() },
                    brush = gold, shadow = shadow, textAlign = TextAlign.Center,
                ),
            )
        }
        if (shining) MushafCoverShine()
    }
}

private fun DrawScope.drawCover() {
    val w = size.width
    val h = size.height
    drawRect(
        Brush.radialGradient(
            listOf(MushafInk.boardLit, MushafInk.board, MushafInk.boardDeep),
            center = center, radius = maxOf(w, h) * 0.75f,
        )
    )
    val gold = Brush.linearGradient(MushafGold, start = Offset.Zero, end = Offset(w, h))

    // Border band: two rules with a running line of stars between them.
    emboss(gold) { brush, blend ->
        insetRect(w * 0.045f) { drawRect(brush, it.topLeft, it.size, style = Stroke(2.dp.toPx()), blendMode = blend) }
        insetRect(w * 0.105f) { drawRect(brush, it.topLeft, it.size, style = Stroke(1.dp.toPx()), blendMode = blend) }
    }
    starRun(inset = w * 0.075f, spacing = w * 0.075f)
    for (corner in 0 until 4) {
        val x = if (corner % 2 == 0) w * 0.075f else w - w * 0.075f
        val y = if (corner < 2) w * 0.075f else h - w * 0.075f
        val star = mushafStarPath(Offset(x, y), w * 0.06f)
        emboss(gold) { brush, blend -> drawPath(star, brush, blendMode = blend) }
    }

    // The arch panel, like a mihrab.
    val arch = archPath(Rect(w * 0.17f, w * 0.17f, w - w * 0.17f, h - w * 0.17f))
    val archInner = archPath(Rect(w * 0.2f, w * 0.2f, w - w * 0.2f, h - w * 0.2f))
    emboss(gold) { brush, blend -> drawPath(arch, brush, style = Stroke(1.6.dp.toPx()), blendMode = blend) }
    emboss(gold) { brush, blend -> drawPath(archInner, brush, style = Stroke(0.8.dp.toPx()), blendMode = blend) }

    val top = mushafStarPath(Offset(w / 2, h * 0.27f), w * 0.045f)
    val bottom = mushafStarPath(Offset(w / 2, h * 0.8f), w * 0.045f)
    emboss(gold) { brush, blend -> drawPath(top, brush, blendMode = blend) }
    emboss(gold) { brush, blend -> drawPath(bottom, brush, blendMode = blend) }
}

private inline fun DrawScope.insetRect(inset: Float, block: (Rect) -> Unit) {
    block(Rect(inset, inset, size.width - inset, size.height - inset))
}

/** Draws its content three times: a dark groove, gold, and a thin highlight. */
private inline fun DrawScope.emboss(gold: Brush, draw: DrawScope.(Brush, BlendMode) -> Unit) {
    translate(top = 1.2.dp.toPx()) { draw(SolidColor(Color.Black.copy(alpha = 0.55f)), BlendMode.SrcOver) }
    draw(gold, BlendMode.SrcOver)
    translate(top = -0.6.dp.toPx()) { draw(SolidColor(Color.White.copy(alpha = 0.25f)), BlendMode.Plus) }
}

/** A line of small stars around the border band. */
private fun DrawScope.starRun(inset: Float, spacing: Float) {
    val r = spacing * 0.16f
    fun star(x: Float, y: Float) = drawPath(mushafStarPath(Offset(x, y), r), MushafInk.gold)
    var x = inset + spacing
    while (x < size.width - inset - spacing / 2) {
        star(x, inset); star(x, size.height - inset)
        x += spacing
    }
    var y = inset + spacing
    while (y < size.height - inset - spacing / 2) {
        star(inset, y); star(size.width - inset, y)
        y += spacing
    }
}

/** A pointed arch at the top and bottom, the shape of the central panel on a mushaf cover. */
private fun archPath(rect: Rect): Path {
    val w = rect.width
    val shoulder = rect.height * 0.16f
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

/** Light catching the tooled gold, as if the book were turned in her hands. */
@Composable
private fun MushafCoverShine() {
    val across = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            across.snapTo(0f)
            delay(1200)
            across.animateTo(1f, tween(1600, easing = FastOutSlowInEasing))
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val band = w * 0.45f
        val x = -w * 0.6f + w * 1.8f * across.value
        rotate(18f, pivot = Offset(x + band / 2, h / 2)) {
            drawRect(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, MushafInk.goldLight.copy(alpha = 0.28f), Color.Transparent),
                    startX = x, endX = x + band,
                ),
                topLeft = Offset(x, -h * 0.5f), size = Size(band, h * 2f),
                blendMode = BlendMode.Plus,
            )
        }
    }
}

/** A warm light that rises from the first page as the book opens, with a few gold specks. */
@Composable
internal fun MushafOpeningLight(bloom: () -> Float, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val b = bloom()
        if (b <= 0.001f) return@Canvas
        val c = center
        val r = 260.dp.toPx() * (0.6f + 0.55f * b)
        drawCircle(
            Brush.radialGradient(listOf(MushafInk.goldLight.copy(alpha = 0.55f), Color.Transparent), center = c, radius = r),
            radius = r, center = c, alpha = b.coerceIn(0f, 1f), blendMode = BlendMode.Plus,
        )
        for (i in 0 until 10) {
            val x = (((i * 37) % 100) / 100f - 0.5f) * 260.dp.toPx()
            val rest = 60.dp.toPx()
            val risen = (-140f - (i % 4) * 30f).dp.toPx()
            val y = rest + (risen - rest) * b
            val s = (2 + i % 3).dp.toPx()
            drawCircle(
                MushafInk.goldLight, radius = s / 2, center = c + Offset(x, y),
                alpha = b.coerceIn(0f, 1f), blendMode = BlendMode.Plus,
            )
        }
    }
}
