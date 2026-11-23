package com.quranforwomen.app.feature.audio

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.AudioPlayer
import com.quranforwomen.app.data.PlaybackSpeed
import com.quranforwomen.app.data.RecitationDownloads
import com.quranforwomen.app.data.RepeatMode
import com.quranforwomen.app.data.SleepTimer
import com.quranforwomen.app.ui.components.AppSheet
import com.quranforwomen.app.ui.components.ChoiceRow
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.ListRow
import com.quranforwomen.app.ui.components.PetalBurst
import com.quranforwomen.app.ui.components.PrimaryButton
import com.quranforwomen.app.ui.components.RollingNumber
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.SegmentedControl
import com.quranforwomen.app.ui.components.TextAction
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.Ink
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil

/** What the repeat label says while a range of verses is repeating. */
internal fun AudioPlayer.loopTitle(): String? {
    val range = loopRange ?: return null
    val verses = if (range.first == range.last) "Verse ${range.first}" else "Verses ${range.first} to ${range.last}"
    return if (loopMode == RepeatMode.Off) verses else "$verses · ${loopMode.title}"
}

// Full player row

/**
 * Speed, sleep timer and offline download, under the repeat and reciter menus.
 * Each symbol gives a small bounce when its value changes.
 */
@Composable
internal fun PlayerExtrasRow() {
    val graph = LocalGraph.current
    val player = graph.player
    val downloads = graph.recitations
    val haptics = rememberHaptics()
    val current = player.current ?: return
    var speedOpen by remember { mutableStateOf(false) }
    var sleepOpen by remember { mutableStateOf(false) }

    val minutesLeft = rememberMinutesLeft(player.sleepEndsAt)
    val sleepText = when (val timer = player.sleepTimer) {
        null -> "Sleep"
        SleepTimer.EndOfSurah -> "End of surah"
        is SleepTimer.Minutes -> "${minutesLeft ?: timer.minutes} min"
    }
    val sleepDescription = when (player.sleepTimer) {
        null -> "Sleep timer, off"
        SleepTimer.EndOfSurah -> "Sleep timer, end of surah"
        is SleepTimer.Minutes -> "Sleep timer, ${minutesLeft ?: 0} minutes left"
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box {
            ExtraLabel(
                Icons.Outlined.Speed,
                PlaybackSpeed.label(player.speed),
                "Playback speed, ${PlaybackSpeed.label(player.speed)}",
                bounceKey = player.speed,
            ) { speedOpen = true }
            DropdownMenu(expanded = speedOpen, onDismissRequest = { speedOpen = false }, containerColor = Palette.surface) {
                PlaybackSpeed.choices.forEach { value ->
                    ExtraPickerItem(PlaybackSpeed.label(value), player.speed == value) {
                        speedOpen = false
                        if (player.speed != value) {
                            haptics.tick()
                            player.updateSpeed(value)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f).widthIn(min = Space.xs))
        Box {
            ExtraLabel(
                symbol("moon.zzz"),
                sleepText,
                sleepDescription,
                bounceKey = player.sleepTimer ?: "off",
            ) { sleepOpen = true }
            DropdownMenu(expanded = sleepOpen, onDismissRequest = { sleepOpen = false }, containerColor = Palette.surface) {
                SleepTimer.choices.forEach { timer ->
                    ExtraPickerItem(timer.title, player.sleepTimer == timer) {
                        sleepOpen = false
                        haptics.tick()
                        player.setSleep(timer)
                    }
                }
                if (player.sleepTimer != null) {
                    ExtraPickerItem("Turn off", false) {
                        sleepOpen = false
                        haptics.tick()
                        player.setSleep(null)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f).widthIn(min = Space.xs))
        RecitationDownloadLabel(player.reciterID, current.surah, downloads)
    }
}

/** Whole minutes left on the sleep timer, ticking once a second. */
@Composable
private fun rememberMinutesLeft(endsAt: Long?): Int? {
    val left by produceState<Int?>(null, endsAt) {
        if (endsAt == null) {
            value = null
            return@produceState
        }
        while (true) {
            val ms = endsAt - SystemClock.elapsedRealtime()
            value = ceil(ms.coerceAtLeast(0L) / 60_000.0).toInt()
            if (ms <= 0) break
            delay(1_000)
        }
    }
    return left
}

@Composable
private fun RecitationDownloadLabel(reciterID: String, surah: Int, downloads: RecitationDownloads) {
    val haptics = rememberHaptics()
    val state = downloads.state(reciterID, surah)
    val percent = (downloads.progress(reciterID, surah) * 100).toInt()
    var burst by remember { mutableIntStateOf(0) }
    val previous = remember { arrayOf(state) }
    LaunchedEffect(state) {
        val old = previous[0]
        previous[0] = state
        if (old == RecitationDownloads.State.Downloading && state == RecitationDownloads.State.Ready) {
            haptics.success()
            burst++
        }
    }
    val (icon, text, description) = when (state) {
        RecitationDownloads.State.NotDownloaded -> Triple(symbol("arrow.down.circle"), "Download", "Download this surah")
        RecitationDownloads.State.Downloading -> Triple(null, "$percent%", "Downloading, $percent percent. Cancel")
        RecitationDownloads.State.Ready -> Triple(symbol("checkmark.circle.fill"), "Downloaded", "Downloaded")
        RecitationDownloads.State.Failed -> Triple(symbol("wifi.exclamationmark"), "Retry", "Download failed. Retry")
    }
    Box(contentAlignment = Alignment.Center) {
        ExtraLabel(
            icon,
            text,
            description,
            bounceKey = state,
            enabled = state != RecitationDownloads.State.Ready,
            progress = if (state == RecitationDownloads.State.Downloading) downloads.progress(reciterID, surah) else null,
        ) {
            when (state) {
                RecitationDownloads.State.Downloading -> downloads.cancel(reciterID, surah)
                RecitationDownloads.State.Ready -> Unit
                else -> {
                    haptics.soft()
                    downloads.download(reciterID, surah)
                }
            }
        }
        PetalBurst(trigger = burst)
    }
}

/** A small bounce for a symbol each time the key changes, like iOS's bounce effect. */
@Composable
private fun rememberExtraBounce(key: Any): Animatable<Float, *> {
    val reduce = LocalReduceMotion.current
    val bounce = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(key) {
        if (first) { first = false; return@LaunchedEffect }
        if (!reduce) {
            bounce.snapTo(0.75f)
            bounce.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
        }
    }
    return bounce
}

@Composable
private fun ExtraLabel(
    icon: ImageVector?,
    text: String,
    description: String,
    bounceKey: Any,
    enabled: Boolean = true,
    progress: Float? = null,
    onClick: () -> Unit,
) {
    val bounce = rememberExtraBounce(bounceKey)
    val shown by animateFloatAsState(progress ?: 0f, Motion.gentle(), label = "downloadProgress")
    Row(
        Modifier
            .heightIn(min = 44.dp)
            .widthIn(max = 150.dp)
            .then(if (enabled) Modifier.pressable(scale = 0.95f, onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(18.dp).graphicsLayer { scaleX = bounce.value; scaleY = bounce.value }, contentAlignment = Alignment.Center) {
            if (progress != null) {
                CircularProgressIndicator(
                    progress = { shown },
                    color = Palette.accent,
                    trackColor = Palette.accent.copy(alpha = 0.2f),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
            } else if (icon != null) {
                Icon(icon, null, tint = Palette.accent, modifier = Modifier.size(18.dp))
            }
        }
        AnimatedContent(
            targetState = text,
            transitionSpec = { fadeIn(Motion.gentle()) togetherWith fadeOut(Motion.gentle()) },
            label = "extraLabel",
        ) {
            Text(
                it,
                style = TextStyles.subheadline.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
                color = Palette.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ExtraPickerItem(title: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(title, style = TextStyles.body, color = Palette.textPrimary) },
        onClick = onClick,
        leadingIcon = {
            Box(Modifier.size(20.dp)) {
                if (selected) Icon(symbol("checkmark"), null, tint = Palette.accent, modifier = Modifier.size(20.dp))
            }
        },
    )
}

// Reading settings

/** Recitation speed as a segmented control, for the reading settings. */
@Composable
internal fun PlaybackSpeedRow() {
    val player = LocalGraph.current.player
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Space.m, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Text("Speed", style = TextStyles.body, color = Ink.text)
        SegmentedControl(
            options = PlaybackSpeed.choices,
            selected = player.speed,
            label = { PlaybackSpeed.label(it) },
            onSelect = { player.updateSpeed(it) },
            modifier = Modifier.semantics { contentDescription = "Playback speed" },
        )
    }
}

/** Downloads one surah for the current reciter, from the reader's settings. */
@Composable
internal fun SurahRecitationDownloadRow(surahID: Int) {
    val graph = LocalGraph.current
    val downloads = graph.recitations
    val reciterID = graph.player.reciterID
    val haptics = rememberHaptics()
    val reduce = LocalReduceMotion.current
    val state = downloads.state(reciterID, surahID)
    val surahName = graph.quran.surah(surahID).name
    var burst by remember { mutableIntStateOf(0) }
    val previous = remember { arrayOf(state) }
    LaunchedEffect(state) {
        val old = previous[0]
        previous[0] = state
        if (old == RecitationDownloads.State.Downloading && state == RecitationDownloads.State.Ready) {
            haptics.success()
            burst++
        }
    }
    ListRow(
        title = if (state == RecitationDownloads.State.Ready) "$surahName downloaded" else "Download $surahName",
        showsChevron = false,
        modifier = Modifier.semantics {
            stateDescription = when (state) {
                RecitationDownloads.State.Ready -> "Downloaded"
                RecitationDownloads.State.Downloading -> "Downloading"
                RecitationDownloads.State.Failed -> "Failed"
                RecitationDownloads.State.NotDownloaded -> "Not downloaded"
            }
        },
        trailing = {
            Box(contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        if (reduce) fadeIn() togetherWith fadeOut()
                        else (scaleIn(Motion.reward(), initialScale = 0.5f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.5f) + fadeOut())
                    },
                    contentAlignment = Alignment.CenterEnd,
                    label = "surahDownload",
                ) { s ->
                    when (s) {
                        RecitationDownloads.State.Ready ->
                            Icon(symbol("checkmark.circle.fill"), null, tint = Ink.accent, modifier = Modifier.size(22.dp))
                        RecitationDownloads.State.Downloading -> {
                            val p by animateFloatAsState(downloads.progress(reciterID, surahID), if (reduce) snap() else Motion.gentle(), label = "p")
                            CircularProgressIndicator(
                                progress = { p },
                                color = Ink.accent,
                                trackColor = Ink.accent.copy(alpha = 0.2f),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        RecitationDownloads.State.Failed -> Text("Retry", style = TextStyles.body, color = Ink.accent)
                        RecitationDownloads.State.NotDownloaded ->
                            Icon(symbol("arrow.down.circle"), null, tint = Ink.accent, modifier = Modifier.size(22.dp))
                    }
                }
                PetalBurst(trigger = burst)
            }
        },
        onClick = {
            when (state) {
                RecitationDownloads.State.Downloading -> downloads.cancel(reciterID, surahID)
                RecitationDownloads.State.Ready -> Unit
                else -> {
                    haptics.soft()
                    downloads.download(reciterID, surahID)
                }
            }
        },
    )
}

// Repeat a range

/**
 * Pick a first and last verse in the surah and how many times to play them.
 * The numbers roll as she steps them, and the parts deal in one after another.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RangeRepeatSheet(surahID: Int, startAyah: Int, onDismiss: () -> Unit, onStarted: () -> Unit = {}) {
    val graph = LocalGraph.current
    val player = graph.player
    val surah = graph.quran.surah(surahID)
    val count = surah.verseCount
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val existing = player.loopRange?.takeIf { player.current?.surah == surahID }

    var from by remember { mutableIntStateOf(existing?.first ?: startAyah.coerceIn(1, count)) }
    var to by remember { mutableIntStateOf(existing?.last ?: (startAyah + 4).coerceIn(1, count)) }
    var mode by remember { mutableStateOf<RepeatMode>(if (existing != null) player.loopMode else RepeatMode.Times(3)) }
    val modes = listOf(RepeatMode.Off, RepeatMode.Times(3), RepeatMode.Times(5), RepeatMode.Times(10), RepeatMode.Forever)

    val close: (after: () -> Unit) -> Unit = { after ->
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            after()
        }
    }

    AppSheet(onDismiss = onDismiss, sheetState = sheetState) {
        Box(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = Space.xs)) {
            TextAction("Cancel", onClick = { close {} }, modifier = Modifier.align(Alignment.CenterStart))
            Text(
                "Repeat a range", style = TextStyles.headline, color = Palette.textPrimary,
                modifier = Modifier.align(Alignment.Center).semantics { heading() },
            )
        }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.page)
                .padding(top = Space.s),
            verticalArrangement = Arrangement.spacedBy(Space.l),
        ) {
            Text(
                surah.name,
                style = TextStyles.title3,
                color = Palette.textPrimary,
                modifier = Modifier.focusIn(60),
            )
            GroupedSection(Modifier.dealIn(0, baseDelay = 100, step = 60)) {
                VerseStepperRow("From verse", from, 1..count) {
                    from = it
                    if (to < it) to = it
                }
                RowDivider(Space.m)
                VerseStepperRow("To verse", to, 1..count) {
                    to = it
                    if (from > it) from = it
                }
            }
            GroupedSection(Modifier.dealIn(1, baseDelay = 100, step = 60), title = "Repeat") {
                modes.forEachIndexed { i, m ->
                    if (i > 0) RowDivider(Space.m)
                    ChoiceRow(if (m == RepeatMode.Off) "Once" else m.title, mode == m, { mode = m })
                }
            }
            PrimaryButton(
                "Play",
                onClick = {
                    haptics.tap()
                    player.repeatRange(surahID, from, to, mode)
                    close(onStarted)
                },
                icon = symbol("play.fill"),
                modifier = Modifier.dealIn(2, baseDelay = 100, step = 60),
            )
        }
    }
}

/** A verse number with minus and plus. The number rolls as it changes. */
@Composable
private fun VerseStepperRow(title: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    val haptics = rememberHaptics()
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = Space.m, vertical = Space.xs)
            .semantics(mergeDescendants = true) {
                contentDescription = title
                stateDescription = "$value"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = TextStyles.body, color = Ink.text, modifier = Modifier.weight(1f))
        StepCircle("minus", "Fewer", value > range.first) {
            haptics.tick()
            onChange((value - 1).coerceIn(range))
        }
        Box(Modifier.widthIn(min = 48.dp), contentAlignment = Alignment.Center) {
            RollingNumber(value, TextStyles.headline.copy(fontFeatureSettings = "tnum"), Ink.text)
        }
        StepCircle("plus", "More", value < range.last) {
            haptics.tick()
            onChange((value + 1).coerceIn(range))
        }
    }
}

@Composable
private fun StepCircle(kind: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.35f }
            .pressable(scale = 0.9f, enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(Ink.wash), contentAlignment = Alignment.Center) {
            Text(if (kind == "minus") "-" else "+", style = TextStyles.title3, color = Ink.accent)
        }
    }
}
