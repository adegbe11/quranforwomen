package com.quranforwomen.app.feature.you

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.HighlightColor
import com.quranforwomen.app.data.TranslationStore
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.ui.components.TranslationText
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rowPress
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.LocalDark
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The red used for destructive actions, the same as the app's alerts. */
internal val youDanger: Color
    @Composable get() = if (LocalDark.current) Color(0xFFFF6B6B) else Color(0xFFC62828)

/** A short caption style, like iOS caption 2. */
internal val youCaption2 = TextStyles.caption.copy(fontSize = 11.sp, lineHeight = 13.sp)

/** A number that rolls up from zero the first time it appears, then follows the value. */
@Composable
internal fun youRolledUp(value: Int, delayMillis: Int): Int {
    val reduce = LocalReduceMotion.current
    var started by rememberSaveable { mutableStateOf(false) }
    var shown by rememberSaveable { mutableIntStateOf(if (reduce) value else 0) }
    LaunchedEffect(value) {
        if (!started) {
            started = true
            if (!reduce && value != 0) delay(delayMillis.toLong())
        }
        shown = value
    }
    return shown
}

/** The row's icon gives one small bounce as the row lands. */
internal fun Modifier.youIconLand(delayMillis: Int): Modifier = composed {
    val reduce = LocalReduceMotion.current
    var done by rememberSaveable { mutableStateOf(false) }
    val scale = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (!done && !reduce) {
            delay(delayMillis.toLong())
            scale.animateTo(1.2f, tween(110))
            scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f))
        }
        done = true
    }
    graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}

/** Bounces once each time the trigger changes (after the first value). */
@Composable
internal fun youBounce(trigger: Any?): Float {
    val reduce = LocalReduceMotion.current
    val scale = remember { Animatable(1f) }
    val first = remember { arrayOf<Any?>(trigger) }
    LaunchedEffect(trigger) {
        if (first[0] == trigger) return@LaunchedEffect
        first[0] = trigger
        if (reduce) return@LaunchedEffect
        scale.animateTo(1.22f, tween(110))
        scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f))
    }
    return scale.value
}

/** A card press with a long press for a small menu, the Android form of a context menu. */
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.youPressHold(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    longClickLabel: String? = null,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.97f else 1f, Motion.press(), label = "pressHold")
    this
        .graphicsLayer { scaleX = s; scaleY = s }
        .combinedClickable(
            interactionSource = source,
            indication = null,
            role = Role.Button,
            onLongClickLabel = longClickLabel,
            onLongClick = onLongClick,
            onClick = onClick,
        )
}

/** A one item menu with a destructive action, shown after a long press. */
@Composable
internal fun YouRemoveMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    label: String,
    icon: ImageVector,
    onRemove: () -> Unit,
) {
    val red = youDanger
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, containerColor = Palette.surface) {
        DropdownMenuItem(
            text = { Text(label, style = TextStyles.body, color = red) },
            leadingIcon = { Icon(icon, null, tint = red) },
            onClick = { onDismiss(); onRemove() },
        )
    }
}

/**
 * A grouped list row: an emerald icon, a title, something on the right and a chevron.
 * Presses highlight softly like a native list cell.
 */
@Composable
internal fun YouRow(
    title: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    landDelayMillis: Int? = null,
    titleColor: Color? = null,
    showsChevron: Boolean = true,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .rowPress(onClick)
            .padding(horizontal = Space.m, vertical = 12.dp)
            .semantics(mergeDescendants = true) { role = Role.Button },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(Modifier.width(26.dp), contentAlignment = Alignment.Center) {
                Icon(
                    icon, null, tint = Palette.accent,
                    modifier = Modifier
                        .size(21.dp)
                        .then(if (landDelayMillis != null) Modifier.youIconLand(landDelayMillis) else Modifier),
                )
            }
            Spacer(Modifier.width(Space.m))
        }
        Text(
            title, style = TextStyles.body, color = titleColor ?: Palette.textPrimary,
            modifier = Modifier.weight(1f),
        )
        trailing()
        if (showsChevron) {
            Spacer(Modifier.width(Space.xxs))
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight, null,
                tint = Palette.textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** A secondary value on the right of a row. Changes cross fade. */
@Composable
internal fun YouRowValue(text: String?) {
    val reduce = LocalReduceMotion.current
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            if (reduce) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            else fadeIn(Motion.smooth()) togetherWith fadeOut(Motion.smooth())
        },
        label = "rowValue",
    ) { value ->
        if (value != null) {
            Text(
                value, style = TextStyles.body, color = Palette.textSecondary, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = Space.xs),
            )
        }
    }
}

/** Translation for a verse, or a calm note while it downloads. */
@Composable
internal fun YouTranslationBody(ref: VerseRef, size: Float = 17f) {
    val translations = LocalGraph.current.translations
    val scope = rememberCoroutineScope()
    val text = translations.text(ref)
    if (text != null) {
        TranslationText(text, size = size, rightToLeft = translations.selected.isRightToLeft)
        return
    }
    when (translations.selectedState) {
        TranslationStore.State.Downloading -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            CircularProgressIndicator(Modifier.size(14.dp), color = Palette.textSecondary, strokeWidth = 2.dp)
            Text("Preparing the translation", style = TextStyles.footnote, color = Palette.textSecondary)
        }
        TranslationStore.State.Failed, TranslationStore.State.NotDownloaded -> Row(
            Modifier
                .heightIn(min = 44.dp)
                .pressable { scope.launch { translations.prepareSelected() } },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(symbol("arrow.down.circle"), null, tint = Palette.accent, modifier = Modifier.size(16.dp))
            Text(
                "Download translation",
                style = TextStyles.footnote.copy(fontWeight = FontWeight.Medium),
                color = Palette.accent,
            )
        }
        TranslationStore.State.Ready -> Unit
    }
}

/** A highlight's small colour dot. */
internal val HighlightColor.youSwatch: Color
    get() = when (this) {
        HighlightColor.emerald -> Color(0xFF3E9C7A)
        HighlightColor.blush -> Color(0xFFD98AA2)
        HighlightColor.gold -> Color(0xFFD3A94A)
        HighlightColor.sky -> Color(0xFF6E9CC6)
    }

/** Soft fills that keep text readable in both appearances. */
@Composable
internal fun HighlightColor.youFill(): Color {
    val dark = LocalDark.current
    val v = when (this) {
        HighlightColor.emerald -> if (dark) 0x1C3329 else 0xDCEDE5
        HighlightColor.blush -> if (dark) 0x3D2830 else 0xF7E1E8
        HighlightColor.gold -> if (dark) 0x3A3220 else 0xF6ECCF
        HighlightColor.sky -> if (dark) 0x1E2B38 else 0xDFEAF4
    }
    return Color(0xFF000000 or v.toLong())
}

/** A time of day the way her phone shows it, 12 or 24 hour. */
internal fun youTimeText(context: Context, hour: Int, minute: Int): String {
    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    val format = DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm a", Locale.getDefault())
    return LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)).format(format)
}
