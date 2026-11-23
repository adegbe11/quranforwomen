package com.quranforwomen.app.feature.reminders

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.GentleReminder
import com.quranforwomen.app.data.GentleReminders
import com.quranforwomen.app.data.Notifications
import com.quranforwomen.app.feature.you.youBounce
import com.quranforwomen.app.feature.you.youTimeText
import com.quranforwomen.app.ui.components.AppSwitch
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.TextAction
import com.quranforwomen.app.ui.components.TintedButton
import com.quranforwomen.app.ui.components.openAppSettings
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.rememberNotificationRequest
import com.quranforwomen.app.ui.components.rowPress
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.Ink
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles

/**
 * One optional daily reminder as a grouped section. The bell bounces as she turns it on,
 * the time row opens beneath it, and if notifications are off a short note slides in.
 * Colours follow the page, so it sits on the emerald screens and on book pages alike.
 */
@Composable
internal fun GentleReminderSection(reminder: GentleReminder, title: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = LocalGraph.current.gentleReminders
    val haptics = rememberHaptics()
    val reduce = LocalReduceMotion.current
    var picking by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(prefs.isOn(reminder) && !Notifications.allowed(context)) }
    // Coming back from the phone's settings with notifications allowed clears the note.
    LifecycleResumeEffect(Unit) {
        if (Notifications.allowed(context)) denied = false
        onPauseOrDispose { }
    }

    val isOn = prefs.isOn(reminder)
    val minutes = prefs.minutes(reminder)

    val request = rememberNotificationRequest { ok ->
        val wasOn = prefs.isOn(reminder)
        if (ok) {
            GentleReminders.enable(context, reminder, prefs.minutes(reminder))
            if (!wasOn) haptics.tap()
        }
        prefs.setOn(reminder, ok)
        denied = !ok
    }

    fun setEnabled(on: Boolean) {
        if (on) {
            request()
        } else {
            GentleReminders.disable(context, reminder)
            prefs.setOn(reminder, false)
            denied = false
        }
    }

    val bounce = youBounce(isOn)
    val source = remember { MutableInteractionSource() }

    GroupedSection(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .toggleable(value = isOn, interactionSource = source, indication = null, role = Role.Switch, onValueChange = ::setEnabled)
                .padding(horizontal = Space.m, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(26.dp), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = isOn,
                    transitionSpec = {
                        if (reduce) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        else (scaleIn(Motion.reward(), initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut())
                    },
                    label = "gentleBell",
                ) { on ->
                    Icon(
                        if (on) Icons.Filled.Notifications else Icons.Outlined.Notifications, null, tint = Ink.accent,
                        modifier = Modifier.size(21.dp).graphicsLayer { scaleX = bounce; scaleY = bounce },
                    )
                }
            }
            Spacer(Modifier.width(Space.m))
            Text(title, style = TextStyles.body, color = Ink.text, modifier = Modifier.weight(1f))
            AppSwitch(isOn, ::setEnabled)
        }

        AnimatedVisibility(
            visible = isOn,
            enter = if (reduce) fadeIn() else expandVertically(Motion.gentle()) + fadeIn(Motion.gentle()),
            exit = if (reduce) fadeOut() else shrinkVertically(Motion.gentle()) + fadeOut(Motion.gentle()),
        ) {
            Column {
                RowDivider()
                val timeText = youTimeText(context, minutes / 60, minutes % 60)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .rowPress { picking = true }
                        .padding(horizontal = Space.m, vertical = 6.dp)
                        .semantics(mergeDescendants = true) { role = Role.Button },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(26.dp), contentAlignment = Alignment.Center) {
                        Icon(symbol("clock"), null, tint = Ink.accent, modifier = Modifier.size(21.dp))
                    }
                    Spacer(Modifier.width(Space.m))
                    Text("Time", style = TextStyles.body, color = Ink.text, modifier = Modifier.weight(1f))
                    AnimatedContent(
                        targetState = timeText,
                        transitionSpec = { fadeIn(Motion.gentle()) togetherWith fadeOut(Motion.gentle()) },
                        label = "gentleTime",
                    ) { text ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Ink.wash)
                                .padding(horizontal = 11.dp, vertical = 6.dp)
                        ) {
                            Text(text, style = TextStyles.body, color = Ink.accent)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = denied,
            enter = if (reduce) fadeIn() else expandVertically(Motion.smooth()) + fadeIn(Motion.smooth()),
            exit = if (reduce) fadeOut() else shrinkVertically(Motion.smooth()) + fadeOut(Motion.smooth()),
        ) {
            Column {
                RowDivider()
                Column(
                    Modifier.fillMaxWidth().padding(Space.m),
                    verticalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    Text("Notifications are off.", style = TextStyles.subheadline, color = Ink.secondary)
                    TintedButton("Open Settings", { context.openAppSettings() }, fullWidth = false)
                }
            }
        }
    }

    if (picking) {
        GentleTimeDialog(
            minutes = minutes,
            onDismiss = { picking = false },
            onPick = { picked ->
                prefs.setMinutes(reminder, picked)
                setEnabled(true)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GentleTimeDialog(minutes: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = minutes / 60,
        initialMinute = minutes % 60,
        is24Hour = android.text.format.DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Ink.surface,
        shape = RoundedCornerShape(24.dp),
        confirmButton = {
            TextAction("Done", { onPick(state.hour * 60 + state.minute); onDismiss() }, bold = true)
        },
        dismissButton = { TextAction("Cancel", onDismiss) },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = Ink.wash,
                    clockDialSelectedContentColor = Ink.onFill,
                    clockDialUnselectedContentColor = Ink.text,
                    selectorColor = Ink.fill,
                    containerColor = Ink.surface,
                    periodSelectorBorderColor = Ink.border,
                    periodSelectorSelectedContainerColor = Ink.wash,
                    periodSelectorUnselectedContainerColor = Color.Transparent,
                    periodSelectorSelectedContentColor = Ink.accent,
                    periodSelectorUnselectedContentColor = Ink.secondary,
                    timeSelectorSelectedContainerColor = Ink.wash,
                    timeSelectorUnselectedContainerColor = Ink.wash.copy(alpha = 0.5f),
                    timeSelectorSelectedContentColor = Ink.accent,
                    timeSelectorUnselectedContentColor = Ink.text,
                ),
            )
        },
    )
}
