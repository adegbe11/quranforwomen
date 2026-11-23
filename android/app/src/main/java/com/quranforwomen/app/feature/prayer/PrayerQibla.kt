package com.quranforwomen.app.feature.prayer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TurnLeft
import androidx.compose.material.icons.outlined.TurnRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.ui.components.LocalBottomInset
import com.quranforwomen.app.ui.components.TopBar
import com.quranforwomen.app.ui.components.TopBarHeight
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Signed angle in -180..180, so turns always go the short way. */
private fun qiblaNormalized(angle: Double): Double {
    var a = angle % 360
    if (a > 180) a -= 360
    if (a < -180) a += 360
    return a
}

private fun qiblaTurnText(offset: Double): String {
    val degrees = abs(offset).roundToInt()
    return if (offset > 0) "Turn right $degrees°" else "Turn left $degrees°"
}

private val QiblaGuidanceStyle = TextStyle(fontFamily = AppFont.serif, fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold)

/**
 * A compass that turns with her. A small Kaaba rides the rim; when it reaches the pointer
 * at the top she is facing the Qibla. The closer she gets, the more the dial answers.
 */
@Composable
internal fun PrayerQiblaContent() {
    val graph = LocalGraph.current
    val prayer = graph.prayer
    val router = LocalRouter.current
    val reduce = LocalReduceMotion.current
    val haptics = rememberHaptics()

    DisposableEffect(Unit) {
        prayer.startHeading()
        onDispose { prayer.stopHeading() }
    }

    val bearing = prayer.qiblaBearing
    val heading = prayer.heading
    val hasCompass = remember { prayer.hasCompass }

    // Heading without the jump at north, so the dial always turns the short way.
    val dial = remember { Animatable(0f) }
    var hasDial by remember { mutableStateOf(false) }
    var aligned by remember { mutableStateOf(false) }
    // The dial swings in and settles on first appearance, like a real compass.
    val swing = remember { Animatable(if (reduce) 0f else -150f) }
    LaunchedEffect(Unit) {
        if (!reduce) {
            delay(250)
            swing.animateTo(0f, prayerSpring(1.1f, 0.42f))
        }
    }

    LaunchedEffect(heading) {
        val new = heading ?: return@LaunchedEffect
        if (bearing != null) {
            val off = abs(qiblaNormalized(bearing - new))
            // Lock in under 4 degrees, let go past 7, so it does not flicker at the edge.
            val nowAligned = if (aligned) off < 7 else off < 4
            if (nowAligned != aligned) {
                aligned = nowAligned
                if (nowAligned) haptics.success()
            }
        }
        if (!hasDial) {
            dial.snapTo(new.toFloat())
            hasDial = true
        } else {
            // Add only the short way round, so 359 to 1 is a 2 degree turn.
            val current = dial.targetValue
            val target = current + qiblaNormalized(new - current).toFloat()
            if (reduce) dial.snapTo(target) else dial.animateTo(target, spring(dampingRatio = 0.82f, stiffness = 120f))
        }
    }

    // Gentle ticks every 5 degrees in the last 30.
    val tickStep = if (heading != null && bearing != null) {
        val off = qiblaNormalized(bearing - heading)
        if (abs(off) < 30) (off / 5).toInt() else null
    } else null
    PrayerOnChange(tickStep) { if (tickStep != null && !aligned) haptics.tick() }

    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = LocalBottomInset.current + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(Modifier.fillMaxSize().background(Palette.background)) {
        Column(
            Modifier.fillMaxSize().padding(top = top + TopBarHeight, bottom = bottom),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.xl),
        ) {
            if (bearing != null) {
                // Degrees she still needs to turn. Positive means turn right.
                val offset = heading?.let { qiblaNormalized(bearing - it) }
                val closeness = offset?.let { maxOf(0.0, 1 - abs(it) / 45).toFloat() } ?: 0f
                val label = when {
                    heading == null -> "The Qibla is ${bearing.roundToInt()} degrees from north"
                    aligned -> "You are facing the Qibla"
                    else -> qiblaTurnText(offset ?: 0.0)
                }

                Spacer(Modifier.weight(1f))
                QiblaDial(
                    bearing = bearing.toFloat(),
                    dial = if (heading == null) 0f else dial.value,
                    swing = swing.value,
                    closeness = closeness,
                    aligned = aligned,
                    modifier = Modifier.size(290.dp).clearAndSetSemantics { contentDescription = label },
                )
                QiblaGuidance(
                    bearing = bearing,
                    offset = offset,
                    aligned = aligned,
                    hasHeading = heading != null,
                    hasCompass = hasCompass,
                    unreliable = prayer.compassUnreliable,
                    modifier = Modifier.padding(horizontal = Space.xl).focusIn(600),
                )
                qiblaDistance(prayer.latitude, prayer.longitude)?.let { distance ->
                    Row(
                        Modifier.focusIn(800),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(symbol("location"), null, tint = Palette.textSecondary, modifier = Modifier.size(15.dp))
                        Text(distance, style = TextStyles.footnote, color = Palette.textSecondary)
                    }
                }
                Spacer(Modifier.weight(1f))
            } else {
                PrayerLocationSetup(Modifier.padding(Space.page))
                Spacer(Modifier.weight(1f))
            }
        }
        TopBar(title = "Qibla", backgroundAlpha = 0f, onBack = { router.pop(); Unit })
    }
}

@Composable
private fun QiblaGuidance(
    bearing: Double,
    offset: Double?,
    aligned: Boolean,
    hasHeading: Boolean,
    hasCompass: Boolean,
    unreliable: Boolean,
    modifier: Modifier = Modifier,
) {
    val reduce = LocalReduceMotion.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        val mode = when {
            !hasHeading -> 0
            aligned -> 1
            else -> 2
        }
        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                if (reduce || targetState != 1) fadeIn() togetherWith fadeOut()
                else (scaleIn(initialScale = 0.9f) + fadeIn()) togetherWith fadeOut()
            },
            label = "guidance",
        ) { m ->
            when (m) {
                0 -> Text("${bearing.roundToInt()}° from north", style = QiblaGuidanceStyle, color = Palette.textPrimary, textAlign = TextAlign.Center)
                1 -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(symbol("checkmark.circle.fill"), null, tint = Palette.accent, modifier = Modifier.size(22.dp).prayerBounce(aligned))
                    Text("You are facing the Qibla", style = QiblaGuidanceStyle, color = Palette.accent)
                }
                else -> {
                    val off = offset ?: 0.0
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AnimatedContent(off > 0, label = "turnIcon") { right ->
                            Icon(
                                if (right) Icons.Outlined.TurnRight else Icons.Outlined.TurnLeft,
                                null, tint = Palette.accent, modifier = Modifier.size(22.dp),
                            )
                        }
                        PrayerRollingText(qiblaTurnText(off), QiblaGuidanceStyle.prayerDigits(), Palette.textPrimary)
                    }
                }
            }
        }
        if (!hasCompass) {
            Text("Compass not available", style = TextStyles.subheadline, color = Palette.textSecondary)
        } else if (hasHeading && unreliable) {
            Text("Move your phone in a figure eight", style = TextStyles.subheadline, color = Palette.blushStrong, textAlign = TextAlign.Center)
        }
    }
}

/** Straight line distance to the Kaaba, in her local units. */
private fun qiblaDistance(lat: Double?, lon: Double?): String? {
    if (lat == null || lon == null) return null
    val r = Math.PI / 180
    val dLat = (21.4225 - lat) * r
    val dLon = (39.8262 - lon) * r
    val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat * r) * cos(21.4225 * r) * sin(dLon / 2) * sin(dLon / 2)
    val km = 6371 * 2 * atan2(sqrt(a), sqrt(1 - a))
    val miles = Locale.getDefault().country in setOf("US", "GB", "LR", "MM")
    val value = if (miles) km / 1.609344 else km
    val number = NumberFormat.getIntegerInstance().format(value.roundToInt())
    return "$number ${if (miles) "mi" else "km"} to Makkah"
}

/**
 * The compass face. Ticks gather on arrival, the face turns against her heading,
 * and the Kaaba on the rim glows brighter as she gets closer.
 */
@Composable
private fun QiblaDial(bearing: Float, dial: Float, swing: Float, closeness: Float, aligned: Boolean, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val gather = remember { Animatable(if (reduce) 1f else 0f) }
    val kaaba = remember { Animatable(if (reduce) 1f else 0f) }
    val line = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        launch { gather.animateTo(1f, tween(GatherMillis, easing = LinearEasing)) }
        launch { delay(500); kaaba.animateTo(1f, prayerSpring(0.5f, 0.55f)) }
        launch { delay(600); line.animateTo(1f, tween(500, easing = EaseOut)) }
    }
    val alignedP by animateFloatAsState(if (aligned) 1f else 0f, prayerSpring(0.4f, 0.6f), label = "aligned")

    val accent = Palette.accent
    val primary = Palette.textPrimary
    val secondary = Palette.textSecondary
    val face = if (aligned) Palette.accentWash else Palette.surface
    val turn = -dial + swing

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val size = minOf(maxWidth, maxHeight)
        val radius = size / 2

        // Light that grows as she nears the Qibla, and pulses once she faces it.
        Canvas(Modifier.fillMaxSize()) {
            val r = radius.toPx() * (0.8f + 0.25f * closeness) * 1.15f
            drawCircle(
                Brush.radialGradient(listOf(accent.copy(alpha = 0.10f + 0.25f * closeness), Color.Transparent), center = center, radius = r),
                radius = r,
            )
        }
        if (aligned && !reduce) {
            val pulse = rememberInfiniteTransition(label = "pulse")
            val t by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = EaseOut), RepeatMode.Restart), label = "t")
            Canvas(Modifier.fillMaxSize()) {
                for (i in 0 until 2) {
                    val e = (t + i * 0.5f) % 1f
                    val s = 0.92f + (1.35f - 0.92f) * e
                    drawCircle(accent.copy(alpha = 0.5f * 0.8f * (1 - e)), radius.toPx() * s, style = Stroke(1.5.dp.toPx()))
                }
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .padding(14.dp)
                .shadow(10.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
                .background(face, CircleShape)
        )

        // The turning face.
        Box(Modifier.fillMaxSize().graphicsLayer { rotationZ = turn }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val r = radius.toPx()
                val elapsed = gather.value * GatherMillis
                for (i in 0 until 72) {
                    val major = i % 18 == 0
                    val mid = i % 6 == 0
                    val local = if (reduce) 1f else FastOutSlowInEasing.transform(((elapsed - i * 6f) / 500f).coerceIn(0f, 1f))
                    if (local <= 0f) continue
                    val len = (if (major) 14.dp else if (mid) 10.dp else 6.dp).toPx() * (0.6f + 0.4f * local)
                    val w = (if (major) 2.5.dp else 1.5.dp).toPx()
                    val color = if (major) primary else secondary.copy(alpha = if (mid) 0.6f else 0.3f)
                    val cy = center.y - (r - 26.dp.toPx())
                    rotate(i * 5f) {
                        drawLine(color.copy(alpha = color.alpha * local), Offset(center.x, cy - len / 2), Offset(center.x, cy + len / 2), w, cap = StrokeCap.Round)
                    }
                }
                // A thin line from the center to the Kaaba.
                val length = r - 40.dp.toPx()
                rotate(bearing) {
                    drawLine(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.35f + 0.5f * closeness), accent.copy(alpha = 0f)),
                            startY = center.y - length, endY = center.y,
                        ),
                        Offset(center.x, center.y), Offset(center.x, center.y - length),
                        strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round, alpha = line.value,
                    )
                }
            }
            listOf("N", "E", "S", "W").forEachIndexed { i, letter ->
                val pos = qiblaPolar(i * 90f, radius - 50.dp)
                Text(
                    letter,
                    style = TextStyles.footnote.copy(fontWeight = if (i == 0) FontWeight.Bold else FontWeight.SemiBold),
                    color = if (i == 0) Palette.blushStrong else Palette.textSecondary,
                    modifier = Modifier
                        .offset(x = pos.first, y = pos.second)
                        .focusIn(300 + i * 60)
                        .graphicsLayer { rotationZ = -turn },
                )
            }
            // The Kaaba on the rim, at the Qibla bearing, kept upright.
            val kPos = qiblaPolar(bearing, radius - 14.dp)
            QiblaKaaba(
                glow = closeness,
                modifier = Modifier
                    .offset(x = kPos.first, y = kPos.second)
                    .graphicsLayer {
                        rotationZ = -turn
                        val s = kaaba.value * (1f + 0.25f * alignedP)
                        scaleX = s; scaleY = s
                        alpha = kaaba.value.coerceIn(0f, 1f)
                    },
            )
        }

        // The phone's own direction: a fixed pointer at the top.
        Icon(
            symbol("arrowtriangle.down.fill"), null,
            tint = if (aligned) Palette.accent else Palette.textSecondary,
            modifier = Modifier
                .offset(y = -radius - 14.dp)
                .size(18.dp)
                .prayerBounce(if (reduce) false else aligned)
                .graphicsLayer { val s = 1f + 0.3f * alignedP; scaleX = s; scaleY = s },
        )

        Box(Modifier.size(8.dp).clip(CircleShape).background(Palette.accentFill))
    }
}

private const val GatherMillis = 72 * 6 + 500

private fun qiblaPolar(degrees: Float, r: Dp): Pair<Dp, Dp> {
    val rad = Math.toRadians(degrees.toDouble())
    return r * sin(rad).toFloat() to r * -cos(rad).toFloat()
}

/** A small Kaaba: a dark cube with a gold band. */
@Composable
private fun QiblaKaaba(glow: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(24.dp)
            .drawBehind {
                val r = size.minDimension / 2 + (4 + 10 * glow).dp.toPx()
                drawCircle(
                    Brush.radialGradient(listOf(PrayerGold.copy(alpha = 0.3f + 0.6f * glow), Color.Transparent), center = center, radius = r),
                    radius = r,
                )
            }
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF1E1E1E))
    ) {
        Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(3.dp).background(PrayerGold))
    }
}
