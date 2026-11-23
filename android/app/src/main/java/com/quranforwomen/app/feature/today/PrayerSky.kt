package com.quranforwomen.app.feature.today

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.drawText
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.Prayer
import com.quranforwomen.app.data.PrayerSchedule
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.glass
import com.quranforwomen.app.ui.components.hasPermission
import com.quranforwomen.app.ui.components.openAppSettings
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.rememberLocationRequest
import com.quranforwomen.app.ui.components.rememberNotificationRequest
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

// Sky model

/** Height of the sky at the top of Today. */
internal val SkyHeight = 430.dp

private fun rgb(hex: Long) = Color(0xFF000000 or hex)

/** Blends two colours in plain RGB, so skies move between prayer times the same way as on the iPhone. */
internal fun skyMix(a: Color, b: Color, t: Float): Color = Color(
    red = (a.red + (b.red - a.red) * t).coerceIn(0f, 1f),
    green = (a.green + (b.green - a.green) * t).coerceIn(0f, 1f),
    blue = (a.blue + (b.blue - a.blue) * t).coerceIn(0f, 1f),
    alpha = (a.alpha + (b.alpha - a.alpha) * t).coerceIn(0f, 1f),
)

/** Top, middle and horizon colours of the sky at one moment. */
internal data class SkyPalette(val top: Color, val middle: Color, val horizon: Color, /** 0 to 1. Above 0.5, text turns white. */ val darkness: Float) {
    fun mixed(other: SkyPalette, t: Float) = SkyPalette(
        skyMix(top, other.top, t),
        skyMix(middle, other.middle, t),
        skyMix(horizon, other.horizon, t),
        darkness + (other.darkness - darkness) * t,
    )

    val isDark: Boolean get() = darkness > 0.5f

    companion object {
        val Night = SkyPalette(rgb(0x071321), rgb(0x0F2236), rgb(0x1D3550), 1f)
        val Fajr = SkyPalette(rgb(0x16223F), rgb(0x4B4F7E), rgb(0xE3A3B4), 0.85f)
        val Sunrise = SkyPalette(rgb(0x8FC3DF), rgb(0xE9D3C9), rgb(0xF8D7A8), 0.2f)
        val Dhuhr = SkyPalette(rgb(0x7DBEDF), rgb(0xB9DDEA), rgb(0xF4F1E6), 0.1f)
        val Asr = SkyPalette(rgb(0x9CC4D8), rgb(0xF0DDC0), rgb(0xF5DCE3), 0.15f)
        val Maghrib = SkyPalette(rgb(0x2A3460), rgb(0xB86A87), rgb(0xF2A779), 0.75f)
    }
}

/** Where the sky is for a given moment and prayer schedule. */
internal class SkyState(
    val palette: SkyPalette,
    /** Position of the sun (day) or moon (night) along its arc, 0 to 1. */
    val progress: Float,
    val isNight: Boolean,
    /** Prayer times along the current arc (name to 0..1), so the sky reads as a prayer clock. Empty without a location. */
    val marks: List<Pair<String, Float>> = emptyList(),
) {
    val isDark: Boolean get() = palette.isDark

    companion object {
        fun at(now: ZonedDateTime, schedule: PrayerSchedule?): SkyState {
            val day = now.toLocalDate().atStartOfDay(now.zone)
            fun time(p: Prayer, fallbackHours: Double): Long =
                (schedule?.time(p) ?: day.plusSeconds((fallbackHours * 3600).toLong())).toInstant().toEpochMilli()

            val fajr = time(Prayer.fajr, 5.0)
            val sunrise = time(Prayer.sunrise, 6.5)
            val dhuhr = time(Prayer.dhuhr, 12.5)
            val asr = time(Prayer.asr, 15.75)
            val maghrib = time(Prayer.maghrib, 18.5)
            val isha = time(Prayer.isha, 20.0)
            val t = now.toInstant().toEpochMilli()
            val minute = 60_000L

            // Anchor colours at each prayer time and blend between them.
            val anchors = listOf(
                fajr - 40 * minute to SkyPalette.Night,
                fajr to SkyPalette.Fajr,
                sunrise + 20 * minute to SkyPalette.Sunrise,
                dhuhr to SkyPalette.Dhuhr,
                asr to SkyPalette.Asr,
                maghrib - 25 * minute to SkyPalette.Asr.mixed(SkyPalette.Maghrib, 0.4f),
                maghrib to SkyPalette.Maghrib,
                isha to SkyPalette.Night,
            )
            var palette = SkyPalette.Night
            if (t >= anchors.first().first && t < anchors.last().first) {
                for (i in 0 until anchors.size - 1) {
                    if (t >= anchors[i].first && t < anchors[i + 1].first) {
                        val span = (anchors[i + 1].first - anchors[i].first).toDouble()
                        val raw = if (span > 0) (t - anchors[i].first) / span else 0.0
                        val eased = raw * raw * (3 - 2 * raw)
                        palette = anchors[i].second.mixed(anchors[i + 1].second, eased.toFloat())
                    }
                }
            }

            val fullDay = 24 * 3_600_000L
            fun along(at: Long, start: Long, end: Long) = ((at - start).toDouble() / (end - start)).toFloat()
            return if (t in sunrise until maghrib) {
                val marks = if (schedule == null) emptyList() else listOf(
                    "Sunrise" to 0f, "Dhuhr" to along(dhuhr, sunrise, maghrib), "Asr" to along(asr, sunrise, maghrib), "Maghrib" to 1f,
                )
                SkyState(palette, along(t, sunrise, maghrib), isNight = false, marks = marks)
            } else {
                val start = if (t >= maghrib) maghrib else maghrib - fullDay
                val end = if (t >= maghrib) fajr + fullDay else fajr
                val ishaAt = if (t >= maghrib) isha else isha - fullDay
                val p = ((t - start).toDouble() / (end - start)).coerceIn(0.0, 1.0)
                val marks = if (schedule == null) emptyList() else listOf(
                    "Maghrib" to 0f, "Isha" to along(ishaAt, start, end), "Fajr" to 1f,
                )
                SkyState(palette, p.toFloat(), isNight = true, marks = marks)
            }
        }
    }
}

// Background

/** The sky gradient, a soft glow where the light meets the horizon, and stars at night. */
@Composable
internal fun SkyBackground(palette: SkyPalette, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.verticalGradient(0f to palette.top, 0.52f to palette.middle, 1f to palette.horizon))
            val radius = 320.dp.toPx()
            drawRect(
                Brush.radialGradient(
                    listOf(palette.horizon.copy(alpha = 0.9f), palette.horizon.copy(alpha = 0f)),
                    center = Offset(size.width / 2, size.height * 1.05f),
                    radius = radius,
                )
            )
        }
        if (palette.darkness > 0.6f) {
            StarField(twinkles = !reduce, opacity = ((palette.darkness - 0.6f) / 0.4f).coerceIn(0f, 1f), modifier = Modifier.matchParentSize())
        }
    }
}

private class Star(val x: Float, val y: Float, val size: Float, val phase: Float)

/** A tiny repeatable random generator so the stars never jump between launches. */
private class SkyRandom(seed: ULong) {
    private var state: ULong = seed * 0x9E3779B97F4A7C15uL

    fun next(): ULong {
        state += 0x9E3779B97F4A7C15uL
        var z = state
        z = (z xor (z shr 30)) * 0xBF58476D1CE4E5B9uL
        z = (z xor (z shr 27)) * 0x94D049BB133111EBuL
        return z xor (z shr 31)
    }

    fun unit(): Double = (next() shr 11).toDouble() / (1uL shl 53).toDouble()
}

private val Stars: List<Star> by lazy {
    val g = SkyRandom(7uL)
    List(55) {
        val x = g.unit()
        val y = g.unit() * 0.75
        val s = 0.8 + g.unit() * 1.4
        val phase = g.unit() * 2 * PI
        Star(x.toFloat(), y.toFloat(), s.toFloat(), phase.toFloat())
    }
}

/** One full twinkle, sin(t * 0.8), in seconds. */
private const val TwinklePeriod = (2 * PI / 0.8).toFloat()

/** Fixed star positions that twinkle slowly, fading toward the horizon. Drawn in one Canvas. */
@Composable
private fun StarField(twinkles: Boolean, opacity: Float, modifier: Modifier) {
    // Stars twinkle over seconds, so 8 updates a second looks the same as 60 and costs far less battery.
    // Waiting for a frame after each pause means nothing runs while the screen is not showing.
    val time: State<Float> = remember { mutableFloatStateOf(0f) }
    if (twinkles) {
        LaunchedEffect(Unit) {
            val start = System.nanoTime()
            while (true) {
                withFrameNanos { now -> (time as MutableFloatState).floatValue = ((now - start) / 1e9f) % TwinklePeriod }
                delay(125)
            }
        }
    }
    Canvas(modifier) {
        val t = time.value
        for (s in Stars) {
            val glow = if (twinkles) 0.55f + 0.45f * sin(t * 0.8f + s.phase) else 0.8f
            val fade = 1f - s.y
            val d = s.size.dp.toPx()
            drawOval(
                Color.White.copy(alpha = (glow * fade * opacity).coerceIn(0f, 1f)),
                topLeft = Offset(s.x * size.width, s.y * size.height),
                size = Size(d, d),
            )
        }
    }
}

// Arc

/** The path of the sun or moon across the sky, with the body at its real position. */
@Composable
internal fun CelestialArc(
    progress: () -> Float,
    isNight: Boolean,
    ink: Color,
    modifier: Modifier = Modifier,
    marks: List<Pair<String, Float>> = emptyList(),
) {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = TextStyles.caption.copy(fontSize = 10.sp, color = ink.copy(alpha = 0.6f))
    Canvas(modifier.clearAndSetSemantics { }) {
        val w = size.width
        val h = size.height
        val inset = 28.dp.toPx()
        val sx = inset
        val sy = h * 0.62f
        val ex = w - inset
        val cx = w / 2
        val cy = -h * 0.45f
        val path = Path().apply {
            moveTo(sx, sy)
            quadraticTo(cx, cy, ex, sy)
        }
        drawPath(
            path, ink.copy(alpha = 0.22f),
            style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx()))),
        )
        fun point(at: Float): Offset {
            val k = at.coerceIn(0f, 1f)
            val v = 1 - k
            return Offset(v * v * sx + 2 * v * k * cx + k * k * ex, v * v * sy + 2 * v * k * cy + k * k * sy)
        }
        val t = progress().coerceIn(0f, 1f)
        // Each prayer is a small mark on the arc. Only the next one is named, so the sky reads as
        // "the sun is heading for Asr" without crowding the greeting above it.
        val next = marks.firstOrNull { it.second > t + 0.01f }
        for ((name, at) in marks) {
            val m = point(at)
            drawCircle(ink.copy(alpha = 0.55f), 2.5.dp.toPx(), m)
            if (name == next?.first && at - t > 0.08f) {
                val layout = measurer.measure(name, labelStyle)
                drawText(layout, topLeft = Offset(m.x - layout.size.width / 2f, m.y + 6.dp.toPx()))
            }
        }
        val p = point(t)
        if (isNight) {
            val moon = Color(0xFFF4EBCF)
            val glow = 28.dp.toPx()
            drawCircle(Brush.radialGradient(listOf(moon.copy(alpha = 0.5f), moon.copy(alpha = 0f)), center = p, radius = glow), glow, p)
            val r = 10.dp.toPx()
            val outer = Path().apply { addOval(Rect(p, r)) }
            val bite = Path().apply { addOval(Rect(p + Offset(r * 0.55f, -r * 0.45f), r * 0.85f)) }
            val crescent = Path().apply { op(outer, bite, PathOperation.Difference) }
            drawPath(crescent, moon)
        } else {
            val sun = Color(0xFFFFD98A)
            val glow = 36.dp.toPx()
            drawCircle(
                Brush.radialGradient(0f to sun.copy(alpha = 0.9f), 0.36f to sun.copy(alpha = 0.55f), 1f to sun.copy(alpha = 0f), center = p, radius = glow),
                glow, p,
            )
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFFFFF6DA), sun), center = p, radius = 14.dp.toPx()),
                13.dp.toPx(), p,
            )
        }
    }
}

// Header

/**
 * The top of Today: a sky that matches the real time of day, the sun or moon moving
 * along its arc, and the five prayers she can tick.
 */
@Composable
internal fun PrayerSkyHeader(now: ZonedDateTime, schedule: PrayerSchedule?, sky: SkyState, modifier: Modifier = Modifier) {
    val library = LocalGraph.current.library
    val reduce = LocalReduceMotion.current
    val ink = if (sky.isDark) Color.White else Palette.ink

    val colorSpec = if (reduce) snap<Color>() else tween(1200, easing = FastOutSlowInEasing)
    val top by animateColorAsState(sky.palette.top, colorSpec, label = "skyTop")
    val middle by animateColorAsState(sky.palette.middle, colorSpec, label = "skyMiddle")
    val horizon by animateColorAsState(sky.palette.horizon, colorSpec, label = "skyHorizon")
    val darkness by animateFloatAsState(sky.palette.darkness, if (reduce) snap() else tween(1200, easing = FastOutSlowInEasing), label = "skyDark")
    val palette = SkyPalette(top, middle, horizon, darkness)

    // The sun or moon starts on the horizon and rises to its real place on first appearance.
    var risen by rememberSaveable { mutableStateOf(false) }
    val arc = remember { Animatable(if (risen || reduce) sky.progress else 0f) }
    LaunchedEffect(sky.progress, reduce) {
        when {
            reduce -> arc.snapTo(sky.progress)
            !risen -> {
                delay(150)
                arc.animateTo(sky.progress, spring(dampingRatio = 0.9f, stiffness = 20f))
                risen = true
            }
            else -> arc.animateTo(sky.progress, tween(1600, easing = FastOutSlowInEasing))
        }
    }

    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(modifier.fillMaxWidth().height(SkyHeight)) {
        SkyBackground(palette, Modifier.matchParentSize())
        CelestialArc(
            progress = { arc.value },
            isNight = sky.isNight,
            ink = ink,
            marks = sky.marks,
            modifier = Modifier.fillMaxWidth().padding(top = 104.dp).height(200.dp),
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(start = Space.page, end = Space.page, top = max(statusTop.value + 14f, 40f).dp),
        ) {
            Text(
                skyDateLine(now),
                style = TextStyles.subheadline,
                color = ink.copy(alpha = 0.75f),
                modifier = Modifier.focusIn(100),
            )
            Text(
                "Assalamu Alaikum",
                style = TextStyle(fontFamily = AppFont.serif, fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.SemiBold),
                color = ink,
                modifier = Modifier.padding(top = 4.dp).semantics { heading() }.focusIn(200),
            )
            NextPrayerChip(now, ink, library.isResting, Modifier.padding(top = Space.s).focusIn(450))

            Spacer(Modifier.weight(1f))

            if (library.isResting) {
                Text(
                    "Rest days. Prayer is paused.",
                    style = TextStyles.subheadline,
                    color = ink.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 40.dp),
                )
            } else {
                PrayerDots(schedule, ink, Modifier.padding(bottom = 44.dp))
            }
        }
    }
}

@Composable
private fun NextPrayerChip(now: ZonedDateTime, ink: Color, resting: Boolean, modifier: Modifier) {
    val graph = LocalGraph.current
    val prayer = graph.prayer
    val router = LocalRouter.current
    val context = LocalContext.current
    val reduce = LocalReduceMotion.current
    val onDark = ink == Color.White
    val requestLocation = rememberLocationRequest()
    var denied by remember { mutableStateOf(false) }
    val askExact = com.quranforwomen.app.ui.components.rememberExactAlarmPrompt()
    val requestNotifications = rememberNotificationRequest { granted ->
        if (granted) { prayer.updateNotify(true); askExact() } else denied = true
    }

    when {
        !prayer.hasLocation -> SkyChip(
            icon = symbol("location"),
            text = if (prayer.isLocating) "Finding your location" else "Set up prayer times",
            ink = ink, onDark = onDark, modifier = modifier,
            pulse = prayer.isLocating && !reduce,
        ) {
            // Once she has said no for good, only the phone's settings can turn location back on.
            val activity = context.findActivity()
            val blocked = prayer.locationDenied && activity != null &&
                !context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (blocked) context.openAppSettings() else requestLocation()
        }

        !prayer.notifyAtPrayerTimes && !resting -> SkyChip(
            icon = symbol("bell.badge"),
            text = if (denied) "Allow notifications in Settings" else "Remind me at each prayer",
            ink = ink, onDark = onDark, modifier = modifier,
        ) {
            if (denied) context.openAppSettings() else requestNotifications()
        }

        !resting -> {
            val next = prayer.nextPrayer(now) ?: return
            val name = next.first.title
            // The next prayer opens Prayer Times, with rest days, the Qibla and prayer settings.
            SkyChip(
                icon = symbol("clock"),
                text = "$name in ${relativeTime(now, next.second)}",
                ink = ink, onDark = onDark, modifier = modifier,
                rolling = true,
                a11yLabel = "Next prayer: $name at ${todayShortTime(context, next.second)}",
                clickLabel = "Open prayer times",
            ) { router.push(Route.PrayerTimes) }
        }
    }
}

/** A glass capsule on the sky. */
@Composable
private fun SkyChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    ink: Color,
    onDark: Boolean,
    modifier: Modifier = Modifier,
    pulse: Boolean = false,
    rolling: Boolean = false,
    a11yLabel: String? = null,
    clickLabel: String? = null,
    onClick: () -> Unit,
) {
    val style = TextStyles.subheadline.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum")
    Box(
        modifier
            .pressable(scale = 0.96f, onClickLabel = clickLabel, onClick = onClick)
            .then(
                if (a11yLabel != null) Modifier.clearAndSetSemantics {
                    contentDescription = a11yLabel
                    role = Role.Button
                    onClick(label = clickLabel) { onClick(); true }
                } else Modifier
            )
            .padding(vertical = 4.dp),
    ) {
        Row(
            Modifier
                .heightIn(min = 36.dp)
                .glass(CircleShape, onDark = onDark)
                .padding(horizontal = Space.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (pulse) PulsingIcon(icon, ink) else Icon(icon, null, tint = ink, modifier = Modifier.size(17.dp))
            if (rolling) RollingText(text, style, ink, down = true) else Text(text, style = style, color = ink, maxLines = 1)
        }
    }
}

@Composable
private fun PulsingIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    val alpha = rememberInfiniteTransition(label = "pulse").animateFloat(
        1f, 0.35f, infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulseAlpha",
    )
    Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp).graphicsLayer { this.alpha = alpha.value })
}

// Prayer dots

@Composable
private fun PrayerDots(schedule: PrayerSchedule?, ink: Color, modifier: Modifier) {
    val library = LocalGraph.current.library
    val haptics = rememberHaptics()
    val doneCount = Prayer.prayers.count { library.prayed(it.name) }
    var lastCount by remember { mutableIntStateOf(doneCount) }
    LaunchedEffect(doneCount) {
        if (doneCount > lastCount) {
            if (doneCount == 5) haptics.success() else haptics.soft()
        }
        lastCount = doneCount
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
        Row(Modifier.fillMaxWidth()) {
            Prayer.prayers.forEachIndexed { index, item ->
                PrayerDotButton(
                    prayer = item,
                    time = schedule?.time(item),
                    isDone = library.prayed(item.name),
                    ink = ink,
                    modifier = Modifier.weight(1f).dealIn(index, baseDelay = 400, step = 50),
                ) { library.togglePrayed(item.name) }
            }
        }
        AnimatedVisibility(doneCount == 5, enter = fadeIn(), exit = fadeOut()) {
            Text("Alhamdulillah, all five today.", style = TextStyles.footnote, color = ink.copy(alpha = 0.75f))
        }
    }
}

@Composable
private fun PrayerDotButton(
    prayer: Prayer,
    time: ZonedDateTime?,
    isDone: Boolean,
    ink: Color,
    modifier: Modifier,
    toggle: () -> Unit,
) {
    val reduce = LocalReduceMotion.current
    val context = LocalContext.current
    val onDark = ink == Color.White
    val scale = remember { Animatable(1f) }
    val ring = remember { Animatable(0f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(isDone) {
        if (first) { first = false; return@LaunchedEffect }
        if (reduce) return@LaunchedEffect
        // Pressed in, then a soft overshoot, like a bead settling.
        launch {
            scale.animateTo(0.84f, tween(100))
            scale.animateTo(1.1f, spring(dampingRatio = 0.55f, stiffness = 700f))
            scale.animateTo(1f, spring(dampingRatio = 1f, stiffness = 300f))
        }
        // A ring of light that opens outward when she marks it.
        if (isDone) {
            ring.snapTo(0.001f)
            ring.animateTo(1f, tween(600, easing = LinearOutSlowInEasing))
            ring.snapTo(0f)
        }
    }
    val check by animateFloatAsState(if (isDone) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "check")
    val base = if (onDark) Color.Black.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.55f)
    val fill by animateColorAsState(if (isDone) Palette.accentFill else base, tween(250), label = "dotFill")
    val timeText = time?.let { todayShortTime(context, it) }

    Column(
        modifier
            .pressable(scale = 0.97f, onClickLabel = if (isDone) "Unmark" else "Mark as prayed", onClick = toggle)
            .clearAndSetSemantics {
                contentDescription = prayer.title
                stateDescription = listOfNotNull(timeText, if (isDone) "prayed" else "not yet").joinToString(", ")
                role = Role.Button
                onClick(label = if (isDone) "Unmark" else "Mark as prayed") { toggle(); true }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(46.dp)
                    .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                    .glass(CircleShape, onDark = onDark, tint = fill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    symbol("checkmark"), null, tint = Color.White,
                    modifier = Modifier.size(20.dp).graphicsLayer {
                        alpha = check.coerceIn(0f, 1f)
                        val s = 0.4f + 0.6f * check
                        scaleX = s; scaleY = s
                    },
                )
            }
            Canvas(Modifier.matchParentSize()) {
                val r = ring.value
                if (r > 0f) {
                    drawCircle(
                        Color.White.copy(alpha = 0.8f * (1f - r)),
                        radius = size.minDimension / 2 * (1f + 0.7f * r),
                        style = Stroke(1.5.dp.toPx()),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(prayer.title, style = TextStyles.caption.copy(fontWeight = FontWeight.SemiBold), color = ink, maxLines = 1)
        if (timeText != null) {
            RollingText(
                timeText,
                TextStyle(fontSize = 11.sp, lineHeight = 13.sp, fontFeatureSettings = "tnum"),
                ink.copy(alpha = 0.7f),
            )
        }
    }
}

// Shared helpers (also used by onboarding)

/** Text that rolls to its new value, like a number counting. */
@Composable
internal fun RollingText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier, down: Boolean = false) {
    val reduce = LocalReduceMotion.current
    AnimatedContent(
        targetState = text,
        modifier = modifier,
        transitionSpec = {
            if (reduce) {
                fadeIn(tween(120)) togetherWith fadeOut(tween(120))
            } else {
                val spec = spring<androidx.compose.ui.unit.IntOffset>(dampingRatio = 0.8f, stiffness = 500f)
                (slideInVertically(spec) { if (down) -it / 2 else it / 2 } + fadeIn()) togetherWith
                    (slideOutVertically(spec) { if (down) it / 2 else -it / 2 } + fadeOut()) using SizeTransform(clip = false)
            }
        },
        label = "rollingText",
    ) { Text(it, style = style, color = color, maxLines = 1) }
}

/** A short clock time, respecting her 12 or 24 hour setting. */
internal fun todayShortTime(context: Context, time: ZonedDateTime): String =
    android.text.format.DateFormat.getTimeFormat(context).format(Date.from(time.toInstant()))

/** "Friday · 18 Rabiʻ II 1448": the weekday, then the Umm al-Qura Hijri date. */
internal fun skyDateLine(now: ZonedDateTime): String {
    val weekday = now.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
    val hijri = runCatching {
        val locale = android.icu.util.ULocale("en@calendar=islamic-umalqura")
        android.icu.text.SimpleDateFormat("d MMMM y", locale).format(Date.from(now.toInstant()))
    }.getOrNull()
    return if (hijri != null) "$weekday · $hijri" else weekday
}

/** "a minute", "25 minutes", "1 hour", "2 hours 10 min". Same as the iPhone. */
private fun relativeTime(from: ZonedDateTime, to: ZonedDateTime): String {
    val minutes = maxOf(0L, java.time.Duration.between(from, to).toMinutes()).toInt()
    if (minutes < 60) return if (minutes <= 1) "a minute" else "$minutes minutes"
    val h = minutes / 60
    val m = minutes % 60
    val hours = if (h == 1) "1 hour" else "$h hours"
    return if (m == 0) hours else "$hours $m min"
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Light (white) or dark status bar icons. Like SystemBarIcons, but leaves the navigation bar alone,
 * since the tab bar below the sky stays on the cream page.
 */
@Composable
internal fun TodayStatusBarIcons(light: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val dark = com.quranforwomen.app.ui.theme.LocalDark.current
    DisposableEffect(light, dark) {
        val window = (view.context.findActivity())?.window ?: return@DisposableEffect onDispose { }
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !light
        // Leaving the sky hands the icons back to the app's normal colour for the theme.
        onDispose { controller.isAppearanceLightStatusBars = !dark }
    }
}
