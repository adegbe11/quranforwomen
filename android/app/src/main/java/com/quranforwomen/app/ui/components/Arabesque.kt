package com.quranforwomen.app.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Arabesque language of the app: the eight pointed star lattice (khatam), gold foil and jewel tones.
 * Everything is drawn from geometry, so it is sharp at any size and needs no pictures.
 */
object Arabesque {
    /** Antique gold foil, light catching across the surface. */
    val goldStops = listOf(
        Color(0xFF8A6A2B), Color(0xFFE9CF8F), Color(0xFFB8913F), Color(0xFFF3E2B0), Color(0xFF9C7A34),
    )
    val gold = Color(0xFFD4AF6A)
    val ivory = Color(0xFFF7EEDC)

    fun goldBrush(width: Float): Brush = Brush.linearGradient(goldStops, start = Offset.Zero, end = Offset(width, width * 0.35f))

    /** Deep jewel tones, top colour then bottom colour. */
    @Immutable
    data class Jewel(val top: Color, val bottom: Color)

    val jewels = listOf(
        Jewel(Color(0xFF0F5B45), Color(0xFF0A3A2C)), // emerald
        Jewel(Color(0xFF1F4E79), Color(0xFF13294B)), // sapphire
        Jewel(Color(0xFF8E2D4C), Color(0xFF55162D)), // ruby rose
        Jewel(Color(0xFF563A7E), Color(0xFF2E1F4A)), // amethyst
        Jewel(Color(0xFF17656B), Color(0xFF0D3B40)), // teal
        Jewel(Color(0xFF7A4E1D), Color(0xFF4A2E12)), // amber
    )

    /**
     * Line segments of the khatam lattice: in each cell an eight pointed star made of two squares,
     * joined to its neighbours along the axes and the diagonals. Each segment is x0, y0, x1, y1.
     */
    fun lattice(width: Float, height: Float, cell: Float, offsetX: Float = 0f, offsetY: Float = 0f): List<FloatArray> {
        val out = ArrayList<FloatArray>()
        val r = cell * 0.30f
        val half = cell / 2f
        var cy = offsetY - cell
        while (cy < height + cell) {
            var cx = offsetX - cell
            while (cx < width + cell) {
                val pts = (0 until 8).map { k ->
                    val a = Math.toRadians(k * 45.0)
                    floatArrayOf(cx + (r * cos(a)).toFloat(), cy + (r * sin(a)).toFloat())
                }
                // Two squares make the star.
                for (start in 0..1) {
                    for (k in 0 until 4) {
                        val p = pts[(start + 2 * k) % 8]
                        val q = pts[(start + 2 * k + 2) % 8]
                        out += floatArrayOf(p[0], p[1], q[0], q[1])
                    }
                }
                // Axis points reach to the cell edges, diagonal points to the corners.
                for (k in 0 until 8) {
                    val a = Math.toRadians(k * 45.0)
                    val reach = if (k % 2 == 0) half else half * 1.4142f
                    out += floatArrayOf(
                        pts[k][0], pts[k][1],
                        cx + (reach * cos(a)).toFloat(), cy + (reach * sin(a)).toFloat(),
                    )
                }
                cx += cell
            }
            cy += cell
        }
        return out
    }

    /** A filled eight pointed star, for medallions and frame corners. */
    fun starPath(center: Offset, radius: Float, inner: Float = 0.62f): Path = Path().apply {
        for (k in 0 until 16) {
            val a = Math.toRadians(k * 22.5 - 90.0)
            val rr = if (k % 2 == 0) radius else radius * inner
            val x = center.x + (rr * cos(a)).toFloat()
            val y = center.y + (rr * sin(a)).toFloat()
            if (k == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

/** The star lattice drawn faintly over a surface. */
fun Modifier.arabesque(color: Color, alpha: Float = 0.16f, cell: Dp = 44.dp, stroke: Dp = 0.8.dp): Modifier = drawWithCache {
    val segs = Arabesque.lattice(size.width, size.height, cell.toPx(), cell.toPx() / 2, cell.toPx() / 2)
    val w = stroke.toPx()
    onDrawBehind {
        for (s in segs) drawLine(color, Offset(s[0], s[1]), Offset(s[2], s[3]), w, StrokeCap.Round, alpha = alpha)
    }
}

/** A double gold rule with a small star at each corner, like the border of a fine Mushaf. */
fun DrawScope.ornateFrame(inset: Float, gap: Float, outer: Float, inner: Float, alpha: Float = 1f) {
    val brush = Arabesque.goldBrush(size.width)
    drawRect(
        brush, topLeft = Offset(inset, inset),
        size = androidx.compose.ui.geometry.Size(size.width - 2 * inset, size.height - 2 * inset),
        style = Stroke(outer), alpha = alpha,
    )
    val i2 = inset + gap
    drawRect(
        brush, topLeft = Offset(i2, i2),
        size = androidx.compose.ui.geometry.Size(size.width - 2 * i2, size.height - 2 * i2),
        style = Stroke(inner), alpha = alpha * 0.8f,
    )
    val star = gap * 1.6f
    listOf(
        Offset(inset, inset), Offset(size.width - inset, inset),
        Offset(inset, size.height - inset), Offset(size.width - inset, size.height - inset),
    ).forEach { drawPath(Arabesque.starPath(it, star), brush, alpha = alpha) }
}
