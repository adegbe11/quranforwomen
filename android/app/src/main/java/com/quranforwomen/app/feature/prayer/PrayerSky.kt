package com.quranforwomen.app.feature.prayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.quranforwomen.app.data.Prayer
import com.quranforwomen.app.data.PrayerSchedule
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import kotlinx.coroutines.delay
import java.time.ZonedDateTime
import kotlin.math.sin
import kotlin.random.Random

// Sky model

/** A colour as plain numbers, so skies can blend smoothly between prayer times. */
internal data class PrayerSkyRGB(val r: Float, val g: Float, val b: Float) {
    fun mixed(o: PrayerSkyRGB, t: Float) = PrayerSkyRGB(r + (o.r - r) * t, g + (o.g - g) * t, b + (o.b - b) * t)
    val color: Color get() = Color(r, g, b)

    companion object {
        fun hex(v: Int) = PrayerSkyRGB(((v shr 16) and 0xFF) / 255f, ((v shr 8) and 0xFF) / 255f, (v and 0xFF) / 255f)
    }
}

/** Top, middle and horizon colours of the sky at one moment. */
internal data class PrayerSkyPalette(val top: PrayerSkyRGB, val middle: PrayerSkyRGB, val horizon: PrayerSkyRGB, val darkness: Float) {
    fun mixed(o: PrayerSkyPalette, t: Float) = PrayerSkyPalette(
        top.mixed(o.top, t), middle.mixed(o.middle, t), horizon.mixed(o.horizon, t), darkness + (o.darkness - darkness) * t,
    )

    companion object {
        private fun p(a: Int, b: Int, c: Int, d: Float) = PrayerSkyPalette(PrayerSkyRGB.hex(a), PrayerSkyRGB.hex(b), PrayerSkyRGB.hex(c), d)
        val night = p(0x071321, 0x0F2236, 0x1D3550, 1f)
        val fajr = p(0x16223F, 0x4B4F7E, 0xE3A3B4, 0.85f)
        val sunrise = p(0x8FC3DF, 0xE9D3C9, 0xF8D7A8, 0.2f)
        val dhuhr = p(0x7DBEDF, 0xB9DDEA, 0xF4F1E6, 0.1f)
        val asr = p(0x9CC4D8, 0xF0DDC0, 0xF5DCE3, 0.15f)
        val maghrib = p(0x2A3460, 0xB86A87, 0xF2A779, 0.75f)
    }
}

/** Where the sky is for a moment and a prayer schedule. */
internal class PrayerSkyState(date: ZonedDateTime, schedule: PrayerSchedule?) {
    val palette: PrayerSkyPalette
    /** Position of the sun (day) or moon (night) along its arc, 0 to 1. */
    val progress: Float
    val isNight: Boolean
    val isDark: Boolean get() = palette.darkness > 0.5f

    init {
        val day = date.toLocalDate().atStartOfDay(date.zone)
        fun time(p: Prayer, fallbackHour: Double): Double =
            (schedule?.time(p) ?: day.plusSeconds((fallbackHour * 3600).toLong())).toEpochSecond().toDouble()
        val now = date.toEpochSecond().toDouble()
        val fajr = time(Prayer.fajr, 5.0)
        val sunrise = time(Prayer.sunrise, 6.5)
        val dhuhr = time(Prayer.dhuhr, 12.5)
        val asr = time(Prayer.asr, 15.75)
        val maghrib = time(Prayer.maghrib, 18.5)
        val isha = time(Prayer.isha, 20.0)

        // Anchor colours at each prayer time and blend between them.
        val anchors = listOf(
            fajr - 40 * 60 to PrayerSkyPalette.night,
            fajr to PrayerSkyPalette.fajr,
            sunrise + 20 * 60 to PrayerSkyPalette.sunrise,
            dhuhr to PrayerSkyPalette.dhuhr,
            asr to PrayerSkyPalette.asr,
            maghrib - 25 * 60 to PrayerSkyPalette.asr.mixed(PrayerSkyPalette.maghrib, 0.4f),
            maghrib to PrayerSkyPalette.maghrib,
            isha to PrayerSkyPalette.night,
        )
        var pal = PrayerSkyPalette.night
        if (now >= anchors.first().first && now < anchors.last().first) {
            for (i in 0 until anchors.size - 1) {
                if (now >= anchors[i].first && now < anchors[i + 1].first) {
                    val span = anchors[i + 1].first - anchors[i].first
                    val raw = if (span > 0) (now - anchors[i].first) / span else 0.0
                    val eased = raw * raw * (3 - 2 * raw)
                    pal = anchors[i].second.mixed(anchors[i + 1].second, eased.toFloat())
                }
            }
        }
        palette = pal

        if (now >= sunrise && now < maghrib) {
            isNight = false
            progress = ((now - sunrise) / (maghrib - sunrise)).toFloat()
        } else {
            isNight = true
            val start = if (now >= maghrib) maghrib else maghrib - 24 * 3600
            val end = if (now >= maghrib) fajr + 24 * 3600 else fajr
            progress = ((now - start) / (end - start)).coerceIn(0.0, 1.0).toFloat()
        }
    }
}

// Background

@Composable
internal fun PrayerSkyBackground(palette: PrayerSkyPalette, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        0f to palette.top.color,
                        0.55f to palette.middle.color,
                        1f to palette.horizon.color,
                    )
                )
                // A soft glow where the light meets the horizon.
                drawRect(
                    Brush.radialGradient(
                        listOf(palette.horizon.color.copy(alpha = 0.9f), palette.horizon.color.copy(alpha = 0f)),
                        center = Offset(size.width * 0.5f, size.height * 1.05f),
                        radius = 320.dp.toPx(),
                    )
                )
            }
    ) {
        if (palette.darkness > 0.6f) {
            PrayerStarField(alpha = ((palette.darkness - 0.6f) / 0.4f).coerceIn(0f, 1f), twinkles = !reduce)
        }
    }
}

private class PrayerStar(val x: Float, val y: Float, val size: Float, val phase: Float)

private val prayerStars: List<PrayerStar> by lazy {
    val r = Random(7)
    List(55) {
        PrayerStar(r.nextFloat(), r.nextFloat() * 0.75f, 0.8f + r.nextFloat() * 1.4f, r.nextFloat() * 2 * Math.PI.toFloat())
    }
}

/** Fixed star positions that twinkle slowly, fading toward the horizon. */
@Composable
private fun PrayerStarField(alpha: Float, twinkles: Boolean) {
    val t by produceState(0f, twinkles) {
        if (!twinkles) return@produceState
        while (true) {
            withFrameMillis { value = it / 1000f }
            delay(83)
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        for (star in prayerStars) {
            val glow = if (twinkles) 0.55f + 0.45f * sin(t * 0.8f + star.phase) else 0.8f
            val fade = 1f - star.y
            val r = star.size.dp.toPx() / 2
            drawCircle(
                Color.White.copy(alpha = (glow * alpha * fade).coerceIn(0f, 1f)),
                radius = r,
                center = Offset(star.x * size.width + r, star.y * size.height + r),
            )
        }
    }
}

// Arc

/** The path of the sun or moon across the sky, with the body at its real position. */
@Composable
internal fun PrayerCelestialArc(progress: Float, isNight: Boolean, ink: Color, modifier: Modifier = Modifier) {
    val moon = Color(0xFFF4EBCF)
    val sunCore = Color(0xFFFFF6DA)
    val sunEdge = Color(0xFFFFD98A)
    BoxWithConstraints(modifier) {
        val w = maxWidth
        val h = maxHeight
        val sx = 28.dp
        val ex = w - 28.dp
        val y0 = h * 0.62f
        val cx = w / 2
        val cy = h * -0.45f
        val t = progress.coerceIn(0f, 1f)
        val u = 1 - t
        val px = sx * (u * u) + cx * (2 * u * t) + ex * (t * t)
        val py = y0 * (u * u) + cy * (2 * u * t) + y0 * (t * t)

        Canvas(Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(sx.toPx(), y0.toPx())
                quadraticTo(cx.toPx(), cy.toPx(), ex.toPx(), y0.toPx())
            }
            drawPath(
                path, ink.copy(alpha = 0.22f),
                style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx()))),
            )
            val c = Offset(px.toPx(), py.toPx())
            val glowColor = if (isNight) moon.copy(alpha = 0.45f) else sunEdge.copy(alpha = 0.7f)
            val glowR = (if (isNight) 30.dp else 40.dp).toPx()
            drawCircle(Brush.radialGradient(listOf(glowColor, Color.Transparent), center = c, radius = glowR), radius = glowR, center = c)
            if (!isNight) {
                val r = 13.dp.toPx()
                drawCircle(Brush.radialGradient(listOf(sunCore, sunEdge), center = c, radius = r), radius = r, center = c)
            }
        }
        if (isNight) {
            Icon(
                symbol("moon.fill"), null, tint = moon,
                modifier = Modifier.offset(x = px - 12.dp, y = py - 12.dp).size(24.dp),
            )
        }
    }
}
