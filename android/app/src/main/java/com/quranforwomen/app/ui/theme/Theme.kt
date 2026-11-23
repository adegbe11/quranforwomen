package com.quranforwomen.app.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.R
import com.quranforwomen.app.data.Appearance

/** True when the app is drawn in dark appearance. */
val LocalDark = staticCompositionLocalOf { false }

/** True on screens set like a book, see BookPage. Cards, rows, labels and buttons switch to book colours. */
val LocalInBook = compositionLocalOf { false }

/** True when she turned animations off in the phone's settings. Motion then becomes simple fades or nothing. */
val LocalReduceMotion = staticCompositionLocalOf { false }

private fun hex(v: Long) = Color(0xFF000000 or v)

@Composable
@ReadOnlyComposable
private fun pick(light: Long, dark: Long): Color = if (LocalDark.current) hex(dark) else hex(light)

/**
 * Semantic colours. Screens use these names, never raw hex values.
 * Brand: Deep Emerald #064E3B, Blush #F5DCE3, Warm Cream #FAF7F0.
 * Dark values keep text at 4.5:1 contrast or better.
 */
object Palette {
    /** Emerald for icons, links and selected states drawn on the background. */
    val accent @Composable @ReadOnlyComposable get() = pick(0x064E3B, 0x7FD1AE)
    /** Emerald for filled buttons. */
    val accentFill @Composable @ReadOnlyComposable get() = pick(0x064E3B, 0x0F6B50)
    val onAccentFill = Color.White
    /** A quiet emerald wash for tinted buttons and selected rows. */
    val accentWash @Composable @ReadOnlyComposable get() = pick(0xE4EEE9, 0x16271F)

    /** Blush, used sparingly for warmth and highlights. */
    val blush @Composable @ReadOnlyComposable get() = pick(0xF5DCE3, 0x3D2830)
    val blushStrong @Composable @ReadOnlyComposable get() = pick(0xB24D6B, 0xE9A9BC)

    val background @Composable @ReadOnlyComposable get() = pick(0xFAF7F0, 0x0E1311)
    val surface @Composable @ReadOnlyComposable get() = pick(0xFFFFFF, 0x171E1B)
    val surfaceSunken @Composable @ReadOnlyComposable get() = pick(0xF3EFE5, 0x121816)

    val textPrimary @Composable @ReadOnlyComposable get() = pick(0x17251F, 0xEDEAE2)
    val textSecondary @Composable @ReadOnlyComposable get() = pick(0x5F6D66, 0xA3ADA7)
    val border @Composable @ReadOnlyComposable get() = pick(0xE7E5DD, 0x27302C)

    /** Paper for the welcome illustration. Stays warm in both appearances. */
    val paper @Composable @ReadOnlyComposable get() = pick(0xFFFDF7, 0xE9E3D4)
    val paperShade @Composable @ReadOnlyComposable get() = pick(0xEDE5D3, 0xCFC6B2)
    val ink = hex(0x1F2A24)

    // Fixed brand tones, the same in both appearances.
    val emerald = hex(0x064E3B)
    val emeraldDeep = hex(0x032A20)
    val emeraldLight = hex(0x0B6A50)
    val cream = hex(0xFAF7F0)
    val gold = hex(0xE9CF8F)
}

/**
 * The book: the colours of PrayerKey's Bible, used wherever she reads the Quran itself.
 * Light is the classic edition (cream leaves, brown ink, gilt, a red rubric for verse numbers).
 * Dark is the black edition (near black leaves, soft white ink, silver), so reading at night stays calm.
 */
object BookInk {
    val page @Composable @ReadOnlyComposable get() = pick(0xF6EFDC, 0x1B1B1D)
    val pageLit @Composable @ReadOnlyComposable get() = pick(0xFDFAF0, 0x232325)
    val pageEdge @Composable @ReadOnlyComposable get() = pick(0xE6D9B6, 0x111113)
    val ink @Composable @ReadOnlyComposable get() = pick(0x221C12, 0xEDEDED)
    val caption @Composable @ReadOnlyComposable get() = pick(0x5A4828, 0xAEAEB2)
    val gild @Composable @ReadOnlyComposable get() = pick(0xC9A26D, 0xBDBDC2)
    val rule @Composable @ReadOnlyComposable get() = pick(0x8C6E32, 0x8E8E93)
    val rubric @Composable @ReadOnlyComposable get() = pick(0x9A1B2B, 0xB9B9BE)
    val ribbon @Composable @ReadOnlyComposable get() = pick(0x8C1F2B, 0xD1D1D6)
    val glow @Composable @ReadOnlyComposable get() = pick(0xF6ECCF, 0x3A3A3C)

    /** The lit leaf, brighter at the spine (right) and deeper at the edge (left). */
    val leaf: Brush
        @Composable @ReadOnlyComposable get() = Brush.horizontalGradient(listOf(pageEdge, page, pageLit))
}

/** Picks the book colour on book screens and the brand colour elsewhere. */
object Ink {
    val text @Composable @ReadOnlyComposable get() = if (LocalInBook.current) BookInk.ink else Palette.textPrimary
    val secondary @Composable @ReadOnlyComposable get() = if (LocalInBook.current) BookInk.caption else Palette.textSecondary
    val accent @Composable @ReadOnlyComposable get() = if (LocalInBook.current) BookInk.rule else Palette.accent
    val wash @Composable @ReadOnlyComposable get() = if (LocalInBook.current) BookInk.gild.copy(alpha = 0.2f) else Palette.accentWash
    val surface @Composable @ReadOnlyComposable get() = if (LocalInBook.current) BookInk.pageLit else Palette.surface
    val border @Composable @ReadOnlyComposable get() = if (LocalInBook.current) BookInk.rule.copy(alpha = 0.25f) else Palette.border
    val eyebrow @Composable @ReadOnlyComposable get() = if (LocalInBook.current) BookInk.rubric else Palette.accent
    val fill @Composable @ReadOnlyComposable get() = if (LocalInBook.current) BookInk.rule else Palette.accentFill
    val onFill @Composable @ReadOnlyComposable get() = if (LocalInBook.current) BookInk.pageLit else Palette.onAccentFill
    val warm @Composable @ReadOnlyComposable get() = if (LocalInBook.current) BookInk.ribbon else Palette.blushStrong
    val warmWash @Composable @ReadOnlyComposable get() = if (LocalInBook.current) BookInk.ribbon.copy(alpha = 0.12f) else Palette.blush
}

object Space {
    val xxs = 4.dp
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
    /** Standard horizontal page margin. */
    val page = 20.dp
}

object Radius {
    val card = 20.dp
    val tile = 16.dp
    val control = 12.dp
}

object AppFont {
    /** Amiri Quran (SIL Open Font License), made for Quranic text with full diacritics. */
    val arabic = FontFamily(Font(R.font.amiri_quran))
    /** Editorial serif for translations and a few headlines. */
    val serif = FontFamily.Serif
}

/** The type scale, sized like iOS text styles so both apps read the same. */
object TextStyles {
    val largeTitle = TextStyle(fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.2.sp)
    val title = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
    val title2 = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
    val title3 = TextStyle(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold)
    val headline = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 17.sp, lineHeight = 23.sp)
    val callout = TextStyle(fontSize = 16.sp, lineHeight = 21.sp)
    val subheadline = TextStyle(fontSize = 15.sp, lineHeight = 20.sp)
    val footnote = TextStyle(fontSize = 13.sp, lineHeight = 18.sp)
    val caption = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
    val eyebrow = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
    val serifTitle = TextStyle(fontFamily = AppFont.serif, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold)
}

/**
 * Motion rules, from Apple's fluid interface guidance:
 * springs for anything she touches (interruptible, carries her momentum),
 * easing only for the app announcing something, entrances under 0.6 seconds.
 */
object Motion {
    /** Expo out: fast start, long soft landing. */
    val entranceEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    fun <T> press() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 900f)
    fun <T> gentle() = spring<T>(dampingRatio = 1f, stiffness = 300f)
    fun <T> smooth() = spring<T>(dampingRatio = 1f, stiffness = 200f)
    /** A small, earned overshoot, for moments of completion. */
    fun <T> reward() = spring<T>(dampingRatio = 0.68f, stiffness = 190f)
    /** Things that land after a push or a deal. */
    fun <T> settle() = spring<T>(dampingRatio = 0.82f, stiffness = 110f)
    fun <T> entrance(delayMillis: Int = 0) = tween<T>(600, delayMillis, entranceEasing)
}

@Composable
fun QuranTheme(appearance: Appearance, content: @Composable () -> Unit) {
    val dark = when (appearance) {
        Appearance.system -> isSystemInDarkTheme()
        Appearance.light -> false
        Appearance.dark -> true
    }
    val context = LocalContext.current
    val reduceMotion = remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    CompositionLocalProvider(LocalDark provides dark, LocalReduceMotion provides reduceMotion) {
        val scheme = if (dark) {
            darkColorScheme(
                primary = hex(0x7FD1AE), onPrimary = hex(0x0E1311), primaryContainer = hex(0x16271F),
                secondary = hex(0xE9A9BC), background = hex(0x0E1311), surface = hex(0x171E1B),
                onBackground = hex(0xEDEAE2), onSurface = hex(0xEDEAE2), onSurfaceVariant = hex(0xA3ADA7),
                surfaceContainerLow = hex(0x171E1B), surfaceContainer = hex(0x171E1B), surfaceContainerHigh = hex(0x1C2420),
                outline = hex(0x27302C), outlineVariant = hex(0x27302C),
            )
        } else {
            lightColorScheme(
                primary = hex(0x064E3B), onPrimary = Color.White, primaryContainer = hex(0xE4EEE9),
                secondary = hex(0xB24D6B), background = hex(0xFAF7F0), surface = hex(0xFFFFFF),
                onBackground = hex(0x17251F), onSurface = hex(0x17251F), onSurfaceVariant = hex(0x5F6D66),
                surfaceContainerLow = hex(0xFFFFFF), surfaceContainer = hex(0xFFFFFF), surfaceContainerHigh = hex(0xFBF9F4),
                outline = hex(0xE7E5DD), outlineVariant = hex(0xE7E5DD),
            )
        }
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
