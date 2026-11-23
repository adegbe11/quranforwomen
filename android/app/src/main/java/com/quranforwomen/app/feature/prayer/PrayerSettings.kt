package com.quranforwomen.app.feature.prayer

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.R
import com.quranforwomen.app.data.AsrMethod
import com.quranforwomen.app.data.CalculationMethod
import com.quranforwomen.app.data.Notifications
import com.quranforwomen.app.data.Prayer
import com.quranforwomen.app.data.PrayerSound
import com.quranforwomen.app.data.canScheduleExact
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.AppSwitch
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.ListRow
import com.quranforwomen.app.ui.components.LotusMark
import com.quranforwomen.app.ui.components.RollingNumber
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.openAppSettings
import com.quranforwomen.app.ui.components.openExactAlarmSettings
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.rememberLocationRequest
import com.quranforwomen.app.ui.components.rowPress
import com.quranforwomen.app.ui.components.scrollSettle
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Radius
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Every prayer setting, each answering as she changes it: times roll to their new values,
 * the bell rings, the chosen sound plays, and a sample notification shows what she will get.
 */
@Composable
internal fun PrayerSettingsContent() {
    val graph = LocalGraph.current
    val prayer = graph.prayer
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()

    var denied by remember { mutableStateOf(false) }
    var playing by remember { mutableStateOf<PrayerSound?>(null) }
    var playToken by remember { mutableIntStateOf(0) }
    val preview = remember { PrayerSoundPreview(context.applicationContext) }
    DisposableEffect(Unit) { onDispose { preview.stop() } }

    // Exact alarms can be switched off on Android 12 and later. Checked again when she comes back.
    var exact by remember { mutableStateOf(canScheduleExact(context)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val now = canScheduleExact(context)
                if (now != exact) {
                    exact = now
                    prayer.refreshNotifications()
                }
                if (denied && Notifications.allowed(context)) denied = false
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    val askExact = com.quranforwomen.app.ui.components.rememberExactAlarmPrompt()
    val askNotifications = rememberNotificationRequestCompat { granted ->
        denied = !granted
        prayer.updateNotify(granted)
        if (granted) askExact()
    }

    fun setNotify(on: Boolean) {
        if (on) askNotifications() else prayer.updateNotify(false)
    }

    fun play(sound: PrayerSound) {
        if (prayer.sound != sound) haptics.tick()
        prayer.updateSound(sound)
        playing = sound
        val duration = preview.play(sound)
        playToken += 1
        val token = playToken
        scope.launch {
            delay(duration.coerceIn(1200L, 8000L))
            if (token == playToken) playing = null
        }
    }

    val notify = prayer.notifyAtPrayerTimes

    AppScreen(title = "Prayer Settings", largeTitle = false) {
        if (prayer.hasLocation) {
            item(key = "times") { PrayerTimesPreview(Modifier.focusIn(50).scrollSettle()) }
        }
        item(key = "calculation") { PrayerCalculationSection(Modifier.dealIn(0, baseDelay = 150).scrollSettle()) }
        item(key = "notifications") {
            GroupedSection(Modifier.dealIn(1, baseDelay = 150).scrollSettle().animateContentSize(), title = "Notifications") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .rowPress { setNotify(!notify) }
                        .semantics(mergeDescendants = true) { role = Role.Switch }
                        .padding(horizontal = Space.m),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.m),
                ) {
                    Crossfade(notify, label = "bell", modifier = Modifier.width(26.dp).prayerBounce(notify, times = 2)) { on ->
                        Icon(
                            if (on) Icons.Filled.Notifications else Icons.Outlined.Notifications, null,
                            tint = Palette.accent, modifier = Modifier.size(22.dp),
                        )
                    }
                    Text("Notify at prayer times", style = TextStyles.body, color = Palette.textPrimary, modifier = Modifier.weight(1f))
                    AppSwitch(checked = notify, onCheckedChange = { setNotify(it) })
                }
                AnimatedVisibility(visible = denied, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Text(
                        "Notifications are off in Settings.",
                        style = TextStyles.footnote, color = Palette.blushStrong,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { context.openAppSettings() }
                            .padding(start = Space.m, end = Space.m, bottom = Space.s),
                    )
                }
                AnimatedVisibility(visible = notify, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Box(Modifier.fillMaxWidth().background(Palette.accentWash.copy(alpha = 0.5f)).padding(Space.m)) {
                        PrayerNotificationSample(prayer.nextPrayer()?.first?.title ?: "Asr")
                    }
                }
                if (Build.VERSION.SDK_INT >= 31 && notify && !exact) {
                    ListRow(
                        title = "Exact times",
                        icon = symbol("clock"),
                        value = "Off",
                        onClick = { context.openExactAlarmSettings() },
                    )
                }
            }
        }
        if (notify) {
            item(key = "sound") {
                GroupedSection(Modifier.animateItem().dealIn(0, baseDelay = 100).scrollSettle(), title = "Sound") {
                    PrayerSound.entries.forEachIndexed { index, sound ->
                        if (index > 0) RowDivider()
                        PrayerSoundRow(sound, isPlaying = playing == sound, isChosen = prayer.sound == sound) { play(sound) }
                    }
                }
            }
            item(key = "remind") {
                PrayerRemindFor(Modifier.animateItem().dealIn(1, baseDelay = 100).scrollSettle())
            }
        }
        item(key = "ramadan") {
            com.quranforwomen.app.feature.ramadan.RamadanReminderSection(Modifier.animateItem().dealIn(2, baseDelay = 150).scrollSettle())
        }
        item(key = "location") { PrayerLocationSection(Modifier.animateItem().dealIn(2, baseDelay = 150).scrollSettle()) }
    }
}

/** Wraps the shared notification request so the callback always sees the latest state. */
@Composable
private fun rememberNotificationRequestCompat(onResult: (Boolean) -> Unit): () -> Unit {
    val latest = androidx.compose.runtime.rememberUpdatedState(onResult)
    return com.quranforwomen.app.ui.components.rememberNotificationRequest { latest.value(it) }
}

// Calculation

@Composable
private fun PrayerCalculationSection(modifier: Modifier = Modifier) {
    val prayer = LocalGraph.current.prayer
    val haptics = rememberHaptics()
    var open by remember { mutableStateOf(false) }
    val method = prayer.calculationMethod

    GroupedSection(modifier, title = "Calculation") {
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .rowPress { open = true }
                    .semantics(mergeDescendants = true) { role = Role.DropdownList }
                    .padding(horizontal = Space.m),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                Icon(symbol("globe"), null, tint = Palette.accent, modifier = Modifier.width(26.dp).size(22.dp).prayerBounce(method))
                Text("Method", style = TextStyles.body, color = Palette.textPrimary)
                AnimatedContent(
                    method, label = "method",
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier.weight(1f),
                ) { m ->
                    Text(
                        m.title, style = TextStyles.body, color = Palette.textSecondary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Icon(
                    symbol("chevron.up.chevron.down"), null,
                    tint = Palette.textSecondary.copy(alpha = 0.6f), modifier = Modifier.size(16.dp),
                )
            }
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                containerColor = Palette.surface,
                shape = RoundedCornerShape(Radius.control),
            ) {
                CalculationMethod.entries.forEach { m ->
                    DropdownMenuItem(
                        text = { Text(m.title, style = TextStyles.body, color = Palette.textPrimary) },
                        trailingIcon = if (m == method) ({ Icon(symbol("checkmark"), null, tint = Palette.accent) }) else null,
                        onClick = {
                            open = false
                            if (m != method) {
                                haptics.tick()
                                prayer.updateMethod(m)
                            }
                        },
                    )
                }
            }
        }
        RowDivider()
        Column(Modifier.padding(Space.m), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                Icon(symbol("sun.min"), null, tint = Palette.accent, modifier = Modifier.width(26.dp).size(22.dp))
                Text("Asr time", style = TextStyles.body, color = Palette.textPrimary)
            }
            PrayerAsrSwitch()
        }
    }
}

/** Two choices with a selection that glides between them. */
@Composable
private fun PrayerAsrSwitch() {
    val prayer = LocalGraph.current.prayer
    val haptics = rememberHaptics()
    val reduce = LocalReduceMotion.current
    val selected = prayer.asr
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Palette.accentWash)
            .padding(4.dp)
    ) {
        val w = (maxWidth - 4.dp) / 2
        val x by animateDpAsState(
            if (selected == AsrMethod.hanafi) w + 4.dp else 0.dp,
            if (reduce) tween(0) else prayerSpring(0.38f, 0.78f),
            label = "asrThumb",
        )
        Box(Modifier.offset(x = x).width(w).height(38.dp).clip(CircleShape).background(Palette.accentFill))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            AsrMethod.entries.forEach { m ->
                val isSel = m == selected
                val color by animateColorAsState(if (isSel) Palette.onAccentFill else Palette.accent, label = "asrText")
                val label = if (m == AsrMethod.standard) "Standard" else "Hanafi"
                val hint = if (m == AsrMethod.standard) "Shafi'i, Maliki and Hanbali" else "Asr comes a little later"
                Box(
                    Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(CircleShape)
                        .pressable(scale = 0.97f) {
                            if (!isSel) {
                                haptics.tick()
                                prayer.updateAsr(m)
                            }
                        }
                        .clearAndSetSemantics {
                            contentDescription = "$label. $hint"
                            this.selected = isSel
                            role = Role.RadioButton
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = TextStyles.subheadline.copy(fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium),
                        color = color,
                    )
                }
            }
        }
    }
}

// Sound

@Composable
private fun PrayerSoundRow(sound: PrayerSound, isPlaying: Boolean, isChosen: Boolean, onClick: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val wave = if (isPlaying && !reduce) {
        val t = rememberInfiniteTransition(label = "wave")
        t.animateFloat(1f, 0.45f, infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "waveAlpha").value
    } else 1f
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .rowPress(onClick)
            .clearAndSetSemantics {
                contentDescription = sound.title
                selected = isChosen
                role = Role.Button
                if (isPlaying) stateDescription = "Playing"
            }
            .padding(horizontal = Space.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Crossfade(isPlaying, label = "speaker", modifier = Modifier.width(26.dp)) { on ->
            Icon(
                if (on) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Outlined.VolumeDown, null,
                tint = Palette.accent,
                modifier = Modifier.size(22.dp).graphicsLayer { alpha = if (on) wave else 1f },
            )
        }
        Column(Modifier.weight(1f).animateContentSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(sound.title, style = TextStyles.body, color = Palette.textPrimary)
            AnimatedVisibility(isPlaying, enter = fadeIn(), exit = fadeOut()) {
                Text("Playing", style = TextStyles.caption, color = Palette.accent)
            }
        }
        AnimatedVisibility(isChosen, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            Icon(symbol("checkmark"), null, tint = Palette.accent, modifier = Modifier.size(22.dp))
        }
    }
}

/** Plays a prayer sound so she can hear it before choosing. */
private class PrayerSoundPreview(private val context: Context) {
    private var player: MediaPlayer? = null
    private var ringtone: Ringtone? = null

    /** Plays the sound and returns roughly how long it lasts, in milliseconds. */
    fun play(sound: PrayerSound): Long {
        stop()
        return runCatching {
            when (sound) {
                PrayerSound.bell -> {
                    val attrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    val p = MediaPlayer.create(context, R.raw.prayer_bell, attrs, 0) ?: return@runCatching 1200L
                    player = p
                    p.setOnCompletionListener { it.release(); if (player === it) player = null }
                    p.start()
                    p.duration.toLong().takeIf { it > 0 } ?: 1200L
                }
                PrayerSound.system -> {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ringtone = RingtoneManager.getRingtone(context, uri)?.also { it.play() }
                    1200L
                }
            }
        }.getOrDefault(1200L)
    }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { ringtone?.stop() }
        ringtone = null
    }
}

// Which prayers

@Composable
private fun PrayerRemindFor(modifier: Modifier = Modifier) {
    val prayer = LocalGraph.current.prayer
    val haptics = rememberHaptics()
    val enabled = prayer.enabledPrayers
    val count = Prayer.prayers.count { it in enabled }
    GroupedSection(modifier, title = "Remind me for") {
        Column(
            Modifier.fillMaxWidth().padding(vertical = Space.m),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Row(Modifier.fillMaxWidth()) {
                Prayer.prayers.forEachIndexed { index, item ->
                    PrayerReminderChip(
                        prayer = item,
                        isOn = item in enabled,
                        modifier = Modifier.weight(1f).dealIn(index, baseDelay = 200, step = 50),
                    ) {
                        haptics.tick()
                        prayer.updateEnabled(if (item in enabled) enabled - item else enabled + item)
                    }
                }
            }
            AnimatedContent(count == 0, label = "count", transitionSpec = { fadeIn() togetherWith fadeOut() }) { none ->
                if (none) {
                    Text("No prayer reminders", style = TextStyles.footnote, color = Palette.textSecondary)
                } else {
                    Row(Modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
                        RollingNumber(count, TextStyles.footnote.prayerDigits(), Palette.textSecondary)
                        Text(" of 5 prayers", style = TextStyles.footnote, color = Palette.textSecondary)
                    }
                }
            }
        }
    }
}

/** A prayer she can include in reminders. It fills with a soft bounce when turned on. */
@Composable
private fun PrayerReminderChip(prayer: Prayer, isOn: Boolean, modifier: Modifier = Modifier, toggle: () -> Unit) {
    val p by animateFloatAsState(if (isOn) 1f else 0f, prayerSpring(0.35f, 0.62f), label = "chip")
    val fill = Palette.accentFill
    val iconColor by animateColorAsState(if (isOn) Palette.onAccentFill else Palette.accent, label = "chipIcon")
    Column(
        modifier
            .pressable(scale = 0.97f, onClick = toggle)
            .clearAndSetSemantics {
                contentDescription = prayer.title
                stateDescription = if (isOn) "Reminder on" else "Reminder off"
                selected = isOn
                role = Role.Checkbox
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(46.dp)
                .prayerPop(isOn, low = 0.86f, high = 1.08f)
                .clip(CircleShape)
                .background(Palette.accentWash)
                .drawBehind {
                    val s = p.coerceAtLeast(0f)
                    if (s > 0.01f) drawCircle(fill.copy(alpha = s.coerceAtMost(1f)), size.minDimension / 2 * s.coerceAtMost(1.1f))
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(prayer.prayerTimeIcon, null, tint = iconColor, modifier = Modifier.size(20.dp).prayerBounce(isOn))
        }
        Text(
            prayer.title,
            style = TextStyles.caption.copy(fontWeight = FontWeight.SemiBold),
            color = if (isOn) Palette.textPrimary else Palette.textSecondary,
            maxLines = 1,
        )
    }
}

// Location

@Composable
private fun PrayerLocationSection(modifier: Modifier = Modifier) {
    val prayer = LocalGraph.current.prayer
    val context = LocalContext.current
    val reduce = LocalReduceMotion.current
    val request = rememberLocationRequest()
    val pulse = if (prayer.isLocating && !reduce) {
        val t = rememberInfiniteTransition(label = "locating")
        t.animateFloat(1f, 0.35f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse").value
    } else 1f
    GroupedSection(modifier, title = "Location") {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = Space.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            Icon(
                if (prayer.hasLocation) Icons.Filled.NearMe else symbol("location"), null,
                tint = Palette.accent,
                modifier = Modifier.width(26.dp).size(22.dp).graphicsLayer { alpha = pulse },
            )
            AnimatedContent(
                if (prayer.isLocating) "Finding your location" else (prayer.placeName ?: "Not set"),
                label = "place",
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
            ) { text ->
                Text(text, style = TextStyles.body, color = Palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Crossfade(prayer.isLocating, label = "locate") { locating ->
                if (locating) {
                    CircularProgressIndicator(color = Palette.accent, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    val label = when {
                        prayer.locationDenied -> "Open Settings"
                        prayer.hasLocation -> "Update"
                        else -> "Use My Location"
                    }
                    Box(
                        Modifier
                            .heightIn(min = 44.dp)
                            .pressable(scale = 0.96f) {
                                if (prayer.locationDenied) context.openAppSettings() else request()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            style = TextStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                            color = Palette.accent,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Palette.accentWash)
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
    }
}

// Preview pieces

/** Today's five times. They roll to new values and glow briefly when a setting changes them. */
@Composable
private fun PrayerTimesPreview(modifier: Modifier = Modifier) {
    val prayer = LocalGraph.current.prayer
    val schedule = prayer.schedule(LocalDate.now())
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.tile))
            .background(Palette.surface)
            .padding(Space.m),
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Text("Today's times", style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold), color = Palette.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Prayer.prayers.forEachIndexed { index, item ->
                PrayerTimeCell(item, schedule?.time(item), Modifier.weight(1f).dealIn(index, baseDelay = 100, step = 40))
            }
        }
    }
}

@Composable
private fun PrayerTimeCell(prayer: Prayer, time: ZonedDateTime?, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val clock = rememberPrayerClock()
    val minutes = time?.let { it.hour * 60 + it.minute } ?: 0
    val glow = remember { Animatable(0f) }
    PrayerOnChange(minutes) {
        if (!reduce) {
            glow.animateTo(1f, tween(200, easing = EaseOut))
            glow.animateTo(0f, tween(900))
        }
    }
    val wash = Palette.accentWash
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .drawBehind { drawRect(wash, alpha = glow.value) }
            .semantics(mergeDescendants = true) {}
            .padding(vertical = Space.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(prayer.prayerTimeIcon, null, tint = Palette.accent, modifier = Modifier.size(15.dp))
        Text(prayer.title, style = TextStyles.caption.copy(fontWeight = FontWeight.SemiBold), color = Palette.textPrimary, maxLines = 1)
        PrayerFitText(
            time?.let { clock.format(it) } ?: "--",
            TextStyles.caption.prayerDigits(),
            Palette.textSecondary,
            Modifier.fillMaxWidth(),
        )
    }
}

/** One line of text that shrinks to at most three quarters of its size to fit, and rolls when it changes. */
@Composable
private fun PrayerFitText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    var scale by remember(text) { mutableStateOf(1f) }
    var width by remember { mutableIntStateOf(0) }
    Box(modifier.onSizeChanged { width = it.width }, contentAlignment = Alignment.Center) {
        AnimatedContent(text, label = "time", transitionSpec = { fadeIn() togetherWith fadeOut() }) { t ->
            Text(
                t,
                style = style.copy(fontSize = style.fontSize * scale),
                color = color,
                maxLines = 1,
                softWrap = false,
                onTextLayout = { layout ->
                    if (width > 0 && layout.size.width > width && scale > 0.75f) scale = (scale - 0.05f).coerceAtLeast(0.75f)
                },
            )
        }
    }
}

/** What a prayer notification looks like. It drops in with a small settle. */
@Composable
private fun PrayerNotificationSample(prayerName: String) {
    val reduce = LocalReduceMotion.current
    val shown = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (shown.value < 1f) {
            delay(150)
            shown.animateTo(1f, prayerSpring(0.5f, 0.68f))
        }
    }
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val p = shown.value
                translationY = (1f - p) * -24.dp.toPx()
                alpha = p.coerceIn(0f, 1f)
                val s = 0.96f + 0.04f * p
                scaleX = s; scaleY = s
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
            .shadow(8.dp, shape, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
            .background(Palette.surface, shape)
            .clearAndSetSemantics {
                contentDescription = "Sample notification: Time for $prayerName. Come to prayer. With an I prayed button."
            }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Palette.accentFill).padding(7.dp),
            ) { LotusMark(Modifier.size(20.dp), color = Color.White, strokeWidth = 1.2.dp) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Time for $prayerName",
                        style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold),
                        color = Palette.textPrimary, modifier = Modifier.weight(1f),
                    )
                    Text("now", style = TextStyle(fontSize = 11.sp, lineHeight = 13.sp), color = Palette.textSecondary)
                }
                Text(
                    "Hayya 'ala as-salah. Come to prayer.",
                    style = TextStyles.footnote, color = Palette.textPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Palette.accentWash),
            contentAlignment = Alignment.Center,
        ) {
            Text("I prayed", style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold), color = Palette.accent)
        }
    }
}

