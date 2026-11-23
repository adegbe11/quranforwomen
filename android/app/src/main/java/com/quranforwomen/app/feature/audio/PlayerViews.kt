package com.quranforwomen.app.feature.audio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.Reciter
import com.quranforwomen.app.data.RepeatMode
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.data.arabicIndic
import com.quranforwomen.app.feature.quran.PulsingWaveform
import com.quranforwomen.app.feature.quran.QuranTranslationBody
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.ArabicText
import com.quranforwomen.app.ui.components.RollingNumber
import com.quranforwomen.app.ui.components.SavedEmpty
import com.quranforwomen.app.ui.components.inkReveal
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.LocalDark
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlin.math.max

/**
 * The compact player: a floating glass pill above the tab bar while recitation is active.
 * Tapping it opens the full player.
 */
@Composable
fun MiniPlayer(modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    val graph = LocalGraph.current
    val player = graph.player
    val quran = graph.quran
    val router = LocalRouter.current
    val dark = LocalDark.current

    // Keep showing the last verse while the pill slides away after stopping.
    val last = remember { arrayOfNulls<VerseRef>(1) }
    player.current?.let { last[0] = it }
    val current = player.current ?: last[0] ?: return
    val surah = quran.surah(current.surah)
    val glassFill = if (dark) Color(0xF7171E1B) else Color(0xF8FFFFFF)

    Row(
        modifier
            .fillMaxWidth()
            .shadow(12.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.12f), spotColor = Color.Black.copy(alpha = 0.12f))
            .clip(CircleShape)
            .background(glassFill)
            .border(0.5.dp, Palette.border.copy(alpha = 0.6f), CircleShape)
            .padding(horizontal = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xxs),
    ) {
        Row(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .pressable(scale = 0.97f, onClickLabel = "Opens the player") { router.push(Route.Player) }
                .semantics(mergeDescendants = true) {
                    contentDescription = "Now playing ${surah.name}, verse ${current.ayah}"
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(Palette.accentWash), contentAlignment = Alignment.Center) {
                PulsingWaveform(Palette.accent, Modifier.size(17.dp), active = player.isPlaying)
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    surah.name,
                    style = TextStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                    color = Palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (player.didFail) {
                    Text("Could not load audio", style = TextStyles.caption, color = Palette.blushStrong, maxLines = 1)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Verse ", style = TextStyles.caption, color = Palette.textSecondary)
                        RollingNumber(current.ayah, TextStyles.caption, Palette.textSecondary)
                        Text(
                            " · ${player.reciter.name}",
                            style = TextStyles.caption,
                            color = Palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        PlayPauseButton(40.dp)

        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .pressable(scale = 0.9f) { player.stop() }
                .semantics { contentDescription = "Stop recitation" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(symbol("xmark"), null, tint = Palette.textSecondary, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun PlayPauseButton(size: Dp) {
    val player = LocalGraph.current.player
    val reduce = LocalReduceMotion.current
    Box(
        Modifier
            .size(max(size.value, 44f).dp)
            .pressable(scale = 0.92f) { player.togglePlayPause() }
            .semantics { contentDescription = if (player.isLoading) "Loading" else if (player.isPlaying) "Pause" else "Play" },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(size).clip(CircleShape).background(Palette.accentFill), contentAlignment = Alignment.Center) {
            if (player.isLoading) {
                CircularProgressIndicator(
                    color = Palette.onAccentFill,
                    strokeWidth = if (size > 50.dp) 3.dp else 2.dp,
                    modifier = Modifier.size(size * 0.4f),
                )
            } else {
                AnimatedContent(
                    targetState = player.isPlaying,
                    transitionSpec = {
                        if (reduce) fadeIn() togetherWith fadeOut()
                        else (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut())
                    },
                    label = "playPause",
                ) { playing ->
                    Icon(symbol(if (playing) "pause.fill" else "play.fill"), null, tint = Palette.onAccentFill, modifier = Modifier.size(size * 0.45f))
                }
            }
        }
    }
}

/**
 * Focused playback: the verse being recited, with simple controls.
 * Motion: each new verse rises in softly while the last one lifts away, and its Arabic inks in
 * right to left. The verse number rolls, a slow glow breathes behind the play button while
 * recitation plays, and the skip, repeat and reciter symbols give a small bounce when used.
 */
@Composable
fun FullPlayerScreen() {
    val graph = LocalGraph.current
    val player = graph.player
    val quran = graph.quran
    val router = LocalRouter.current
    val reduce = LocalReduceMotion.current
    val current = player.current
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val lift = with(LocalDensity.current) { 18.dp.roundToPx() }

    LaunchedEffect(current == null) {
        if (current == null && router.current?.route == Route.Player) router.pop()
    }

    Box(Modifier.fillMaxSize().background(Palette.background)) {
        Column(Modifier.fillMaxSize().padding(top = top)) {
            Box(Modifier.fillMaxWidth().height(52.dp)) {
                Box(
                    Modifier.align(Alignment.TopCenter).padding(top = 6.dp).size(width = 36.dp, height = 5.dp)
                        .clip(CircleShape).background(Palette.textSecondary.copy(alpha = 0.3f))
                )
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = Space.xs)
                        .size(44.dp)
                        .clip(CircleShape)
                        .pressable(scale = 0.9f) { router.pop() }
                        .semantics { contentDescription = "Close" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Palette.accent, modifier = Modifier.size(28.dp))
                }
            }

            if (current == null) {
                SavedEmpty(symbol("headphones"), "Nothing playing")
                return@Column
            }

            val surah = quran.surah(current.surah)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.xl)
                    .padding(top = Space.l, bottom = Space.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.xl),
            ) {
                Column(
                    Modifier.semantics(mergeDescendants = true) { },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(surah.name, style = TextStyles.title3.copy(fontFamily = AppFont.serif), color = Palette.textPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Verse ", style = TextStyles.subheadline, color = Palette.textSecondary)
                        RollingNumber(current.ayah, TextStyles.subheadline.copy(fontFeatureSettings = "tnum"), Palette.textSecondary)
                        Text(" of ${surah.verseCount}", style = TextStyles.subheadline, color = Palette.textSecondary)
                    }
                }

                // Old and new verse overlap while they cross, so nothing jumps.
                AnimatedContent(
                    targetState = current,
                    transitionSpec = {
                        if (reduce) fadeIn(Motion.gentle()) togetherWith fadeOut(Motion.gentle())
                        else (fadeIn(Motion.smooth()) + slideInVertically(Motion.smooth()) { lift }) togetherWith
                            (fadeOut(Motion.smooth()) + slideOutVertically(Motion.smooth()) { -lift })
                    },
                    label = "verse",
                ) { ref ->
                    Column(verticalArrangement = Arrangement.spacedBy(Space.xl)) {
                        ArabicText(
                            "${quran.text(ref)} ﴿${ref.ayah.arabicIndic}﴾",
                            size = max(graph.prefs.arabicSize, 30f),
                            center = true,
                            color = Palette.textPrimary,
                            modifier = Modifier.inkReveal(120, 950),
                        )
                        QuranTranslationBody(ref)
                    }
                }
            }

            PlayerControls()
        }
    }
}

@Composable
private fun PlayerControls() {
    val player = LocalGraph.current.player
    val haptics = rememberHaptics()
    val reduce = LocalReduceMotion.current
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var repeatOpen by remember { mutableStateOf(false) }
    var reciterOpen by remember { mutableStateOf(false) }
    var rangeOpen by remember { mutableStateOf(false) }
    var backTaps by remember { mutableIntStateOf(0) }
    var nextTaps by remember { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxWidth()
            .background(Palette.surface.copy(alpha = 0.6f))
            .padding(horizontal = Space.xl)
            .padding(top = Space.l, bottom = Space.xl + bottom),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.l),
    ) {
        AnimatedVisibility(visible = player.didFail, enter = fadeIn(Motion.gentle()), exit = fadeOut(Motion.gentle())) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(symbol("wifi.exclamationmark"), null, tint = Palette.blushStrong, modifier = Modifier.size(15.dp))
                Text("Could not load audio.", style = TextStyles.footnote, color = Palette.blushStrong)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xxl)) {
            SkipButton(symbol("backward.fill"), "Previous verse", backTaps) {
                if (!reduce) backTaps++
                player.previous()
            }
            Box(contentAlignment = Alignment.Center) {
                BreathingGlow(player.isPlaying, Modifier.size(72.dp))
                PlayPauseButton(72.dp)
            }
            SkipButton(symbol("forward.fill"), "Next verse", nextTaps) {
                if (!reduce) nextTaps++
                player.next()
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box {
                val repeatTitle = player.loopTitle() ?: player.repeatMode.title
                MenuLabel(
                    symbol("repeat"),
                    "Repeat: $repeatTitle",
                    "Repeat, $repeatTitle",
                    bounceKey = repeatTitle,
                ) { repeatOpen = true }
                DropdownMenu(expanded = repeatOpen, onDismissRequest = { repeatOpen = false }, containerColor = Palette.surface) {
                    listOf(RepeatMode.Off, RepeatMode.Times(3), RepeatMode.Times(5), RepeatMode.Forever).forEach { mode ->
                        PickerItem(mode.title, player.loopRange == null && player.repeatMode == mode) {
                            repeatOpen = false
                            if (player.loopRange != null) player.clearLoop()
                            if (player.repeatMode != mode) {
                                haptics.tick()
                                player.updateRepeatMode(mode)
                            }
                        }
                    }
                    PickerItem("A range of verses", player.loopRange != null) {
                        repeatOpen = false
                        rangeOpen = true
                    }
                }
            }
            Spacer(Modifier.weight(1f).widthIn(min = Space.s))
            Box {
                MenuLabel(
                    symbol("person.wave.2"),
                    player.reciter.name,
                    "Reciter, ${player.reciter.name}",
                    bounceKey = player.reciterID,
                ) { reciterOpen = true }
                DropdownMenu(expanded = reciterOpen, onDismissRequest = { reciterOpen = false }, containerColor = Palette.surface) {
                    Reciter.all.forEach { reciter ->
                        PickerItem(reciter.name, player.reciterID == reciter.id) {
                            reciterOpen = false
                            if (player.reciterID != reciter.id) {
                                haptics.tick()
                                player.reciterID = reciter.id
                            }
                        }
                    }
                }
            }
        }

        PlayerExtrasRow()
    }

    if (rangeOpen) {
        player.current?.let { RangeRepeatSheet(it.surah, it.ayah, onDismiss = { rangeOpen = false }) }
    }
}

@Composable
private fun PickerItem(title: String, selected: Boolean, onClick: () -> Unit) {
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

/** A small bounce for a symbol each time the key changes, like iOS's bounce effect. */
@Composable
private fun rememberBounce(key: Any): Animatable<Float, *> {
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
private fun MenuLabel(icon: ImageVector, text: String, description: String, bounceKey: Any, onClick: () -> Unit) {
    val bounce = rememberBounce(bounceKey)
    Row(
        Modifier
            .heightIn(min = 44.dp)
            .widthIn(max = 220.dp)
            .pressable(scale = 0.95f, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            icon, null, tint = Palette.accent,
            modifier = Modifier.size(18.dp).graphicsLayer { scaleX = bounce.value; scaleY = bounce.value },
        )
        AnimatedContent(
            targetState = text,
            transitionSpec = { fadeIn(Motion.gentle()) togetherWith fadeOut(Motion.gentle()) },
            label = "menuLabel",
        ) {
            Text(
                it,
                style = TextStyles.subheadline.copy(fontWeight = FontWeight.Medium),
                color = Palette.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SkipButton(icon: ImageVector, label: String, taps: Int, onClick: () -> Unit) {
    val bounce = rememberBounce(taps)
    Box(
        Modifier
            .size(56.dp)
            .clip(CircleShape)
            .pressable(scale = 0.9f, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, null, tint = Palette.accent,
            modifier = Modifier.size(32.dp).graphicsLayer { scaleX = bounce.value; scaleY = bounce.value },
        )
    }
}

/**
 * A soft emerald light behind the play button that slowly swells and settles while
 * recitation plays, and fades away on pause. Still and faint with animations off.
 */
@Composable
private fun BreathingGlow(active: Boolean, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val shown by animateFloatAsState(if (active) 1f else 0f, Motion.smooth(), label = "glowShown")
    // Only breathes while recitation plays; paused, it rests and costs nothing.
    val swell = if (active && !reduce) {
        val transition = rememberInfiniteTransition(label = "breathing")
        transition.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), AnimRepeatMode.Reverse),
            label = "swell",
        ).value
    } else 0f
    val color = Palette.accent
    Canvas(modifier) {
        if (shown <= 0f) return@Canvas
        val s = if (reduce) 1.05f else 1.05f + 0.35f * swell
        val a = if (reduce) 0.5f else 0.4f + 0.45f * swell
        val r = (size.minDimension / 2 + 16.dp.toPx()) * s
        drawCircle(
            Brush.radialGradient(
                0f to color.copy(alpha = 0.4f), 0.6f to color.copy(alpha = 0.25f), 1f to Color.Transparent,
                center = center, radius = r,
            ),
            radius = r,
            alpha = a * shown,
        )
    }
}
