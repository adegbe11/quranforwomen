package com.quranforwomen.app.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.ui.theme.LocalDark
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.delay

/** Haptics, always gated by her Haptics setting. */
@Stable
class Haptics(private val view: View, private val enabled: () -> Boolean) {
    /** A light tick, for selection changes and counters. */
    fun tick() = perform(HapticFeedbackConstants.CLOCK_TICK)
    /** A firmer tap, for a press that does something. */
    fun tap() = perform(HapticFeedbackConstants.VIRTUAL_KEY)
    /** Something finished well: a target reached, a page saved. */
    fun success() = perform(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS)
    /** Something could not happen. */
    fun reject() = perform(if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS)
    /** A soft landing, like the splash lighting up. */
    fun soft() = perform(if (Build.VERSION.SDK_INT >= 27) HapticFeedbackConstants.KEYBOARD_RELEASE else HapticFeedbackConstants.CLOCK_TICK)

    private fun perform(constant: Int) {
        if (enabled()) view.performHapticFeedback(constant)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    val prefs = LocalGraph.current.prefs
    return remember(view) { Haptics(view) { prefs.hapticsOn } }
}

/** One "Removed. Undo" bar for the whole app, shown above the tab bar. */
@Stable
class UndoState {
    var message by mutableStateOf<String?>(null); private set
    var undo: (() -> Unit)? = null; private set
    var serial by mutableStateOf(0); private set

    fun show(message: String, undo: () -> Unit) {
        this.message = message
        this.undo = undo
        serial++
    }

    fun dismiss() { message = null; undo = null }
}

val LocalUndo = staticCompositionLocalOf { UndoState() }

@Composable
fun UndoBar(state: UndoState, modifier: Modifier = Modifier) {
    val haptics = rememberHaptics()
    LaunchedEffect(state.serial) {
        if (state.message != null) {
            delay(4500)
            state.dismiss()
        }
    }
    AnimatedVisibility(
        visible = state.message != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        val dark = LocalDark.current
        Row(
            modifier = Modifier
                .padding(horizontal = Space.page)
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .shadow(12.dp, RoundedCornerShape(16.dp), clip = false)
                .background(if (dark) Color(0xFF2A322E) else Color(0xFF1F2A24), RoundedCornerShape(16.dp))
                .padding(start = Space.m, end = Space.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(state.message ?: "", style = TextStyles.subheadline, color = Color.White)
            Box(
                Modifier
                    .pressable {
                        haptics.tap()
                        state.undo?.invoke()
                        state.dismiss()
                    }
                    .padding(horizontal = Space.m, vertical = Space.s)
            ) {
                Text("Undo", style = TextStyles.headline, color = Color(0xFF7FD1AE))
            }
        }
    }
}
