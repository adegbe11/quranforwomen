package com.quranforwomen.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.data.SourceKind
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.BookInk
import com.quranforwomen.app.ui.theme.Ink
import com.quranforwomen.app.ui.theme.LocalDark
import com.quranforwomen.app.ui.theme.LocalInBook
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Radius
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Buttons

/** The one main action on a screen. Large, calm, emerald (or book brown on book pages). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clip(CircleShape)
            .background(Ink.fill)
            .pressable(scale = 0.98f, enabled = enabled, onClick = onClick)
            .padding(horizontal = Space.l),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Ink.onFill, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Space.xs))
        }
        Text(text, style = TextStyles.headline, color = Ink.onFill)
    }
}

/** A softer action that sits beside or below a primary action. */
@Composable
fun TintedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fullWidth: Boolean = true,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = 50.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .clip(CircleShape)
            .background(Ink.wash)
            .pressable(scale = 0.98f, enabled = enabled, onClick = onClick)
            .padding(horizontal = Space.l),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (LocalInBook.current) BookInk.caption else Palette.accent
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Space.xs))
        }
        Text(text, style = TextStyles.headline, color = color)
    }
}

/** A plain text action, like an iOS bar button. */
@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, bold: Boolean = false) {
    Box(
        modifier
            .heightIn(min = 44.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .pressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Space.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = if (bold) TextStyles.headline else TextStyles.body, color = Ink.accent)
    }
}

// Surfaces

/** A card: no border, separated by tone and space like iOS. In a book, a fine gilt rule edges the leaf. */
@Composable
fun CardSurface(
    modifier: Modifier = Modifier,
    padding: Dp = Space.l,
    fill: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val inBook = LocalInBook.current
    val shape = RoundedCornerShape(Radius.card)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(scale = 0.985f, onClick = onClick) else Modifier)
            .clip(shape)
            .background(fill ?: Ink.surface)
            .then(if (inBook) Modifier.border(0.8.dp, BookInk.gild.copy(alpha = 0.35f), shape) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** The warm cream page behind a screen. */
@Composable
fun Modifier.pageBackground(): Modifier = background(Palette.background)

/**
 * A screen set like a book, in the colours of PrayerKey's Bible: the lit leaf behind it,
 * and every card, list, row, label and button on it drawn in book colours.
 */
@Composable
fun BookPage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalInBook provides true) {
        Box(modifier.fillMaxSize().background(BookInk.leaf)) { content() }
    }
}

/**
 * Frosted glass, readable on any background: a translucent tint with a hairline edge.
 * onDark is for glass that sits on the night sky or a photo.
 */
fun Modifier.glass(shape: Shape, onDark: Boolean = false, tint: Color? = null): Modifier =
    this
        .clip(shape)
        .background(tint ?: if (onDark) Color.Black.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.55f))
        .border(0.6.dp, if (onDark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.7f), shape)

// Text building blocks

/** A small uppercase label above a card's content. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    Text(
        text.uppercase(),
        style = TextStyles.eyebrow,
        color = color ?: Ink.eyebrow,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() }) {
        Text(title, style = TextStyles.title3, color = Ink.text)
        if (subtitle != null) Text(subtitle, style = TextStyles.subheadline, color = Ink.secondary)
    }
}

/** Arabic scripture or supplication. Always right to left, always the Quran font. */
@Composable
fun ArabicText(
    text: String,
    modifier: Modifier = Modifier,
    size: Float = 28f,
    center: Boolean = false,
    color: Color? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text,
            modifier = modifier.fillMaxWidth(),
            style = TextStyle(
                fontFamily = AppFont.arabic,
                fontSize = size.sp,
                lineHeight = (size * 1.85f).sp,
                textAlign = if (center) TextAlign.Center else TextAlign.Start,
                textDirection = TextDirection.Rtl,
                localeList = LocaleList("ar"),
            ),
            color = color ?: Ink.text,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Translation text in the editorial serif. */
@Composable
fun TranslationText(
    text: String,
    modifier: Modifier = Modifier,
    size: Float = 17f,
    color: Color? = null,
    rightToLeft: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
) {
    CompositionLocalProvider(LocalLayoutDirection provides if (rightToLeft) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        Text(
            text,
            modifier = modifier.fillMaxWidth(),
            style = TextStyle(fontFamily = AppFont.serif, fontSize = size.sp, lineHeight = (size * 1.45f).sp),
            color = color ?: Ink.text,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Shows where a text comes from. Quran and Sunnah look different on purpose. */
@Composable
fun SourceLabel(kind: SourceKind, reference: String, modifier: Modifier = Modifier) {
    val inBook = LocalInBook.current
    val fg = when {
        inBook -> if (kind == SourceKind.Personal) BookInk.ribbon else BookInk.caption
        else -> if (kind == SourceKind.Personal) Palette.blushStrong else Palette.accent
    }
    val bg = when {
        inBook -> if (kind == SourceKind.Personal) BookInk.ribbon.copy(alpha = 0.12f) else BookInk.gild.copy(alpha = 0.2f)
        else -> if (kind == SourceKind.Personal) Palette.blush else Palette.accentWash
    }
    val icon = symbol(
        when (kind) {
            SourceKind.Quran -> "book.closed"
            SourceKind.Hadith -> "text.quote"
            SourceKind.Personal -> "pencil"
        }
    )
    Row(
        modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Source: $reference" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(6.dp))
        Text(reference, style = TextStyles.footnote.copy(fontWeight = FontWeight.Medium), color = fg)
    }
}

// Small controls

/** A round icon button used for Listen and Save style actions. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    size: Dp = 44.dp,
    onClick: () -> Unit,
) {
    val inBook = LocalInBook.current
    val fg = if (inBook) (if (isActive) BookInk.ribbon else BookInk.rule) else Palette.accent
    val bg = if (inBook) (if (isActive) BookInk.ribbon.copy(alpha = 0.14f) else BookInk.gild.copy(alpha = 0.2f))
    else (if (isActive) Palette.blush else Palette.accentWash)
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .pressable(scale = 0.92f, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = { (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut()) },
            label = "icon",
        ) { Icon(it, null, tint = fg, modifier = Modifier.size(20.dp)) }
    }
}

/** Bookmark toggle. Saving bounces the bookmark and sends out a few petals; removing is quiet. */
@Composable
fun SaveButton(
    isSaved: Boolean,
    modifier: Modifier = Modifier,
    savedLabel: String = "Saved",
    unsavedLabel: String = "Save",
    onClick: () -> Unit,
) {
    val haptics = rememberHaptics()
    val bounce = remember { Animatable(1f) }
    var burst by remember { mutableStateOf(0) }
    val reduce = LocalReduceMotion.current
    LaunchedEffect(burst) {
        if (burst > 0 && !reduce) {
            bounce.snapTo(0.8f)
            bounce.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.4f, stiffness = 500f))
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        PetalBurst(trigger = burst)
        CircleIconButton(
            icon = symbol(if (isSaved) "bookmark.fill" else "bookmark"),
            label = if (isSaved) savedLabel else unsavedLabel,
            isActive = isSaved,
            modifier = Modifier.graphicsLayer { scaleX = bounce.value; scaleY = bounce.value },
        ) {
            haptics.tick()
            if (!isSaved) burst++
            onClick()
        }
    }
}

/** A few blush petals that drift out from a point and fade. Draws nothing with animations off. */
@Composable
fun PetalBurst(trigger: Int, modifier: Modifier = Modifier, count: Int = 7, color: Color? = null) {
    if (LocalReduceMotion.current) return
    val progress = remember { Animatable(1f) }
    val seeds = remember(trigger) { List(count) { Random.nextFloat() } }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
        }
    }
    val tint = color ?: Palette.blushStrong
    Canvas(modifier.size(1.dp)) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        for (i in 0 until count) {
            val angle = (i.toFloat() / count) * 2 * Math.PI + seeds[i] * 0.6
            val dist = (18 + 22 * seeds[i]) * density * p
            val c = Offset(cos(angle).toFloat() * dist, sin(angle).toFloat() * dist - 10 * density * p)
            val r = (3.2f - 1.2f * p) * density
            drawOval(tint.copy(alpha = (1f - p) * 0.9f), topLeft = c - Offset(r, r * 0.6f), size = androidx.compose.ui.geometry.Size(r * 2, r * 1.2f))
        }
    }
}

/** A list row icon in brand emerald. */
@Composable
fun RowIcon(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(Ink.wash),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Ink.accent, modifier = Modifier.size(17.dp)) }
}

// Grouped lists

/** An inset grouped list on the cream page, the way iOS Settings and Health do it. */
@Composable
fun GroupedSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val inBook = LocalInBook.current
    val shape = RoundedCornerShape(Radius.tile)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        if (title != null) {
            Text(
                title, style = TextStyles.title3, color = Ink.text,
                modifier = Modifier.padding(bottom = Space.xxs).semantics { heading() },
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Ink.surface)
                .then(if (inBook) Modifier.border(0.8.dp, BookInk.gild.copy(alpha = 0.35f), shape) else Modifier),
            content = content,
        )
        if (footer != null) {
            Text(footer, style = TextStyles.footnote, color = Ink.secondary, modifier = Modifier.padding(horizontal = Space.m))
        }
    }
}

/** A list row: optional icon, title, optional subtitle and value, and a chevron. Presses highlight softly. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    value: String? = null,
    showsChevron: Boolean = true,
    titleColor: Color? = null,
    /** When set, the icon gives one small bounce this long after the row appears. */
    landDelayMillis: Int? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val reduce = LocalReduceMotion.current
    val land = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (landDelayMillis != null && !reduce) {
            kotlinx.coroutines.delay(landDelayMillis.toLong())
            land.snapTo(0.75f)
            land.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.4f, stiffness = 400f))
        }
    }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.rowPress(onClick) else Modifier)
            .padding(horizontal = Space.m, vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                icon, null, tint = Ink.accent,
                modifier = Modifier.width(26.dp).size(22.dp).graphicsLayer { scaleX = land.value; scaleY = land.value },
            )
            Spacer(Modifier.width(Space.m))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = TextStyles.body, color = titleColor ?: Ink.text)
            if (subtitle != null) {
                Text(subtitle, style = TextStyles.subheadline, color = Ink.secondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (value != null) {
            Spacer(Modifier.width(Space.xs))
            Text(value, style = TextStyles.body, color = Ink.secondary, maxLines = 1, modifier = Modifier.widthIn(max = 180.dp), overflow = TextOverflow.Ellipsis)
        }
        trailing?.invoke(this)
        if (showsChevron) {
            Spacer(Modifier.width(Space.xxs))
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight, null,
                tint = Ink.secondary.copy(alpha = 0.5f), modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Rows highlight softly while pressed, like a native list cell. */
@Composable
fun Modifier.rowPress(onClick: () -> Unit): Modifier {
    val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val wash = if (LocalInBook.current) BookInk.gild.copy(alpha = 0.18f) else Palette.accentWash.copy(alpha = 0.7f)
    return this
        .drawBehind { if (pressed) drawRect(wash) }
        .clickable(interactionSource = source, indication = null, onClick = onClick)
}

/** Hairline between rows, inset like iOS. */
@Composable
fun RowDivider(inset: Dp = 58.dp) {
    Box(Modifier.padding(start = inset).fillMaxWidth().height(0.5.dp).background(Ink.border))
}

// Empty states

/** A calm empty state: an icon, a short title, and at most one action. */
@Composable
fun SavedEmpty(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(vertical = Space.xxxl, horizontal = Space.xl).focusIn(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(Ink.wash), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Ink.accent, modifier = Modifier.size(28.dp))
        }
        Text(title, style = TextStyles.title3, color = Ink.text, textAlign = TextAlign.Center)
        if (detail != null) Text(detail, style = TextStyles.subheadline, color = Ink.secondary, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Space.xs))
            TintedButton(actionLabel, onAction, fullWidth = false)
        }
    }
}

// The lotus

enum class LotusPart { Center, Left, Right, Water, All }

/** The parts of the lotus mark, each its own path, so it can be drawn live piece by piece. */
fun lotusPath(part: LotusPart, rect: Rect): Path {
    val w = rect.width
    val h = rect.height
    val base = Offset(rect.center.x, rect.bottom - h * 0.08f)
    val p = Path()
    if (part == LotusPart.Center || part == LotusPart.All) {
        p.moveTo(base.x, base.y)
        p.quadraticTo(rect.center.x - w * 0.2f, rect.top + h * 0.45f, rect.center.x, rect.top)
        p.quadraticTo(rect.center.x + w * 0.2f, rect.top + h * 0.45f, base.x, base.y)
    }
    if (part == LotusPart.Left || part == LotusPart.All) {
        p.moveTo(base.x, base.y)
        p.quadraticTo(rect.left + w * 0.12f, rect.bottom - h * 0.08f, rect.left + w * 0.06f, rect.top + h * 0.38f)
        p.quadraticTo(rect.center.x - w * 0.14f, rect.top + h * 0.42f, base.x, base.y)
    }
    if (part == LotusPart.Right || part == LotusPart.All) {
        p.moveTo(base.x, base.y)
        p.quadraticTo(rect.right - w * 0.12f, rect.bottom - h * 0.08f, rect.right - w * 0.06f, rect.top + h * 0.38f)
        p.quadraticTo(rect.center.x + w * 0.14f, rect.top + h * 0.42f, base.x, base.y)
    }
    if (part == LotusPart.Water || part == LotusPart.All) {
        p.moveTo(rect.left + w * 0.18f, rect.bottom)
        p.quadraticTo(rect.center.x, rect.bottom - h * 0.1f, rect.right - w * 0.18f, rect.bottom)
    }
    return p
}

/** The small lotus mark, drawn as one stroke. */
@Composable
fun LotusMark(modifier: Modifier = Modifier, color: Color? = null, strokeWidth: Dp = 1.6.dp) {
    val tint = color ?: Ink.accent
    Canvas(modifier) {
        drawPath(
            lotusPath(LotusPart.All, Rect(Offset.Zero, size)),
            tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
    }
}

/** True while drawn in dark appearance. */
val isDark: Boolean @Composable get() = LocalDark.current
