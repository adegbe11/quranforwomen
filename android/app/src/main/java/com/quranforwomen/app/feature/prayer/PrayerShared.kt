package com.quranforwomen.app.feature.prayer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.Prayer
import com.quranforwomen.app.ui.components.TintedButton
import com.quranforwomen.app.ui.components.openAppSettings
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberLocationRequest
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.CoroutineScope
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.max

/** The warm gold used for the Kaaba band and the all five banner. */
internal val PrayerGold = Color(0xFFC9A24A)

/** Numbers that keep their width while they tick. */
internal fun TextStyle.prayerDigits(): TextStyle = copy(fontFeatureSettings = "tnum")

/** A SwiftUI style spring (response in seconds, damping fraction) for Compose. */
internal fun <T> prayerSpring(response: Float, damping: Float): SpringSpec<T> {
    val w = 2 * PI.toFloat() / response
    return spring(dampingRatio = damping, stiffness = w * w)
}

/** Short clock time in her phone's 12 or 24 hour style. */
@Composable
internal fun rememberPrayerClock(): DateTimeFormatter {
    val context = LocalContext.current
    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    return remember(is24) { DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm a", Locale.getDefault()) }
}

/** "a minute", "12 minutes", "2 hours 5 min". */
internal fun prayerRelative(from: ZonedDateTime, to: ZonedDateTime): String {
    val minutes = max(0L, Duration.between(from, to).seconds / 60).toInt()
    if (minutes < 60) return if (minutes <= 1) "a minute" else "$minutes minutes"
    val h = minutes / 60
    val m = minutes % 60
    val hours = if (h == 1) "1 hour" else "$h hours"
    return if (m == 0) hours else "$hours $m min"
}

/** The icon for each time of day. */
internal val Prayer.prayerTimeIcon: ImageVector
    @Composable get() = when (this) {
        Prayer.fajr -> symbol("sun.horizon")
        Prayer.sunrise -> symbol("sunrise")
        Prayer.dhuhr -> symbol("sun.max")
        Prayer.asr -> symbol("sun.min")
        Prayer.maghrib -> Icons.Outlined.WbTwilight
        Prayer.isha -> symbol("moon.stars")
    }

/** Runs a block when the key changes, but not on first composition. */
@Composable
internal fun PrayerOnChange(key: Any?, block: suspend CoroutineScope.() -> Unit) {
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(key) {
        if (first[0]) { first[0] = false; return@LaunchedEffect }
        block()
    }
}

/** A small bounce when the key changes, like an SF Symbol bounce. */
internal fun Modifier.prayerBounce(key: Any?, from: Float = 0.75f, times: Int = 1): Modifier = composed {
    val reduce = LocalReduceMotion.current
    val s = remember { Animatable(1f) }
    PrayerOnChange(key) {
        if (!reduce) repeat(times) {
            s.snapTo(from)
            s.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f))
        }
    }
    graphicsLayer { scaleX = s.value; scaleY = s.value }
}

/** Presses in, overshoots, then settles, when the key changes. */
internal fun Modifier.prayerPop(key: Any?, low: Float = 0.82f, high: Float = 1.12f): Modifier = composed {
    val reduce = LocalReduceMotion.current
    val s = remember { Animatable(1f) }
    PrayerOnChange(key) {
        if (!reduce) {
            s.animateTo(low, tween(100))
            s.animateTo(high, spring(dampingRatio = 0.5f, stiffness = 700f))
            s.animateTo(1f, spring(dampingRatio = 1f, stiffness = 300f))
        }
    }
    graphicsLayer { scaleX = s.value; scaleY = s.value }
}

/** Text whose changing characters roll, like a numeric content transition. */
@Composable
internal fun PrayerRollingText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    countsDown: Boolean = false,
) {
    val reduce = LocalReduceMotion.current
    Row(modifier) {
        val chars = text.toList()
        chars.forEachIndexed { i, c ->
            key(chars.size - i) {
                AnimatedContent(
                    targetState = c,
                    transitionSpec = {
                        if (reduce) EnterTransition.None togetherWith ExitTransition.None
                        else {
                            val dir = if (countsDown) -1 else 1
                            (slideInVertically(prayerSpring(0.3f, 0.9f)) { it * dir } + fadeIn(tween(150))) togetherWith
                                (slideOutVertically(prayerSpring(0.3f, 0.9f)) { -it * dir } + fadeOut(tween(150))) using
                                SizeTransform(clip = false)
                        }
                    },
                    label = "char",
                ) { ch -> Text(ch.toString(), style = style, color = color, maxLines = 1, softWrap = false) }
            }
        }
    }
}

/** Asks for her location, or offers the phone's settings if she turned it off. */
@Composable
internal fun PrayerLocationSetup(modifier: Modifier = Modifier) {
    val prayer = LocalGraph.current.prayer
    val context = LocalContext.current
    val request = rememberLocationRequest()
    Column(modifier.fillMaxWidth().padding(vertical = Space.xs), verticalArrangement = Arrangement.spacedBy(Space.m)) {
        Text("Location", style = TextStyles.headline, color = Palette.textPrimary, modifier = Modifier.semantics { heading() })
        if (prayer.locationDenied) {
            Text("Location is off in Settings.", style = TextStyles.subheadline, color = Palette.blushStrong)
            TintedButton("Open Settings", { context.openAppSettings() })
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(CircleShape)
                    .background(Palette.accentFill)
                    .pressable(scale = 0.98f, enabled = !prayer.isLocating, role = Role.Button) { request() },
                contentAlignment = Alignment.Center,
            ) {
                if (prayer.isLocating) {
                    CircularProgressIndicator(color = Palette.onAccentFill, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text("Use My Location", style = TextStyles.headline, color = Palette.onAccentFill)
                }
            }
        }
    }
}
