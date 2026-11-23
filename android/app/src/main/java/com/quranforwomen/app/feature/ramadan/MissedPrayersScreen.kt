package com.quranforwomen.app.feature.ramadan

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.MissedPrayersStore
import com.quranforwomen.app.data.Prayer
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.CardSurface
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.LocalUndo
import com.quranforwomen.app.ui.components.PetalBurst
import com.quranforwomen.app.ui.components.RollingNumber
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.TextAction
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.scrollSettle
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Radius
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.launch

internal fun Prayer.missedSymbol(): String = when (this) {
    Prayer.fajr -> "sun.horizon"
    Prayer.sunrise -> "sunrise"
    Prayer.dhuhr -> "sun.max"
    Prayer.asr -> "sun.min"
    Prayer.maghrib -> "sun.horizon"
    Prayer.isha -> "moon.stars"
}

/** Her own count of prayers to make up. She sets the numbers; each one she marks counts down. Private. */
@Composable
fun MissedPrayersScreen() {
    val store = LocalGraph.current.missedPrayers
    val haptics = rememberHaptics()
    val undo = LocalUndo.current
    var editing by rememberSaveable { mutableStateOf(false) }

    AppScreen(
        title = "Make-up Prayers",
        largeTitle = false,
        actions = {
            TextAction(if (editing) "Done" else "Edit", onClick = { editing = !editing; haptics.tick() }, bold = editing)
        },
    ) {
        item(key = "summary") {
            MissedSummary(store, Modifier.dealIn(0, baseDelay = 20).scrollSettle())
        }
        item(key = "prayers") {
            GroupedSection(Modifier.dealIn(1, baseDelay = 200).scrollSettle()) {
                Prayer.prayers.forEachIndexed { i, prayer ->
                    if (i > 0) RowDivider()
                    MissedPrayerRow(
                        prayer = prayer,
                        left = store.left(prayer),
                        editing = editing,
                        index = i,
                        onSet = { store.setLeft(prayer, it) },
                        onMark = {
                            if (store.markOne(prayer)) {
                                haptics.success()
                                undo.show("${prayer.title} marked") { store.undoOne(prayer) }
                            } else {
                                haptics.reject()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MissedSummary(store: MissedPrayersStore, modifier: Modifier = Modifier) {
    CardSurface(modifier, padding = Space.xl) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MissedFigure(store.totalLeft, "to make up", Modifier.weight(1f).focusIn(150))
            Box(Modifier.width(0.5.dp).height(34.dp).background(Palette.border))
            MissedFigure(store.madeUp, "made up", Modifier.weight(1f).focusIn(250))
        }
    }
}

@Composable
private fun MissedFigure(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.semantics(mergeDescendants = true) { contentDescription = "$value $label" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RollingNumber(
            value,
            style = TextStyles.title.copy(fontFamily = AppFont.serif, fontWeight = androidx.compose.ui.text.font.FontWeight.Light),
            color = Palette.textPrimary,
        )
        Text(label, style = TextStyles.caption, color = Palette.textSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MissedPrayerRow(
    prayer: Prayer,
    left: Int,
    editing: Boolean,
    index: Int,
    onSet: (Int) -> Unit,
    onMark: () -> Unit,
) {
    val reduce = LocalReduceMotion.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .dealIn(index, baseDelay = 260, step = 50)
            .padding(start = Space.m, end = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(symbol(prayer.missedSymbol()), null, tint = Palette.accent, modifier = Modifier.width(26.dp).size(22.dp))
        Spacer(Modifier.width(Space.m))
        Text(prayer.title, style = TextStyles.body, color = Palette.textPrimary, modifier = Modifier.weight(1f))
        AnimatedContent(
            targetState = editing,
            transitionSpec = {
                if (reduce) fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                else (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.9f))
            },
            label = "missedMode",
        ) { isEditing ->
            if (isEditing) {
                MissedNumberField(prayer, left, onSet)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RollingNumber(left, TextStyles.body, if (left > 0) Palette.textPrimary else Palette.textSecondary)
                    Spacer(Modifier.width(Space.xs))
                    MissedCheck(prayer.title, enabled = left > 0, onMark = onMark)
                }
            }
        }
    }
}

@Composable
private fun MissedNumberField(prayer: Prayer, left: Int, onSet: (Int) -> Unit) {
    var text by remember(prayer) { mutableStateOf(if (left == 0) "" else left.toString()) }
    BasicTextField(
        value = text,
        onValueChange = { raw ->
            val digits = raw.filter(Char::isDigit).take(5)
            text = digits
            onSet(digits.toIntOrNull()?.coerceAtMost(MissedPrayersStore.MAX) ?: 0)
        },
        singleLine = true,
        textStyle = TextStyles.body.copy(color = Palette.textPrimary, textAlign = TextAlign.End),
        cursorBrush = SolidColor(Palette.accent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        modifier = Modifier
            .width(96.dp)
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(Radius.control))
            .background(Palette.accentWash)
            .padding(horizontal = Space.s, vertical = 10.dp)
            .semantics { contentDescription = "${prayer.title} prayers to make up" },
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterEnd) {
                if (text.isEmpty()) Text("0", style = TextStyles.body, color = Palette.textSecondary.copy(alpha = 0.6f))
                inner()
            }
        },
    )
}

/** Marks one made up: it dips, springs back and sends out a few petals. */
@Composable
private fun MissedCheck(name: String, enabled: Boolean, onMark: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val scope = rememberCoroutineScope()
    val pop = remember { Animatable(1f) }
    var burst by remember { mutableIntStateOf(0) }
    Box(
        Modifier
            .size(44.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.35f }
            .pressable(scale = 0.9f, enabled = enabled, onClickLabel = "Mark one $name prayed") {
                onMark()
                burst++
                if (!reduce) scope.launch {
                    pop.snapTo(0.8f)
                    pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 500f))
                }
            }
            .semantics { contentDescription = "Mark one $name prayed" },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                .clip(CircleShape)
                .background(Palette.accentFill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(symbol("checkmark"), null, tint = Palette.onAccentFill, modifier = Modifier.size(17.dp))
        }
        PetalBurst(trigger = burst, color = Palette.accent)
    }
}
