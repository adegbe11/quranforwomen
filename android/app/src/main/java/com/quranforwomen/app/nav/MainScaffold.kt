package com.quranforwomen.app.nav

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.feature.audio.MiniPlayer
import com.quranforwomen.app.ui.components.LocalBottomInset
import com.quranforwomen.app.ui.components.LocalIsRoot
import com.quranforwomen.app.ui.components.LocalTab
import com.quranforwomen.app.ui.components.LocalUndo
import com.quranforwomen.app.ui.components.UndoBar
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.theme.LocalDark
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import com.quranforwomen.app.ui.theme.Palette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val TabBarHeight = 64.dp
private val MiniPlayerHeight = 60.dp

/** The five tabs, each with its own stack, the floating tab bar, the search circle and the mini player. */
@Composable
fun MainScaffold() {
    val router = LocalRouter.current
    val player = LocalGraph.current.player
    val holder = rememberSaveableStateHolder()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val fullScreen = router.current?.route?.hidesTabBar == true
    val hasPlayer = player.current != null
    val reduce = LocalReduceMotion.current

    val barShift by animateFloatAsState(if (fullScreen) 1f else 0f, Motion.gentle(), label = "barShift")
    val inset by animateDpAsState(
        TabBarHeight + 12.dp + (if (hasPlayer) MiniPlayerHeight + 8.dp else 0.dp),
        Motion.gentle(), label = "inset",
    )

    Box(Modifier.fillMaxSize().background(Palette.background)) {
        CompositionLocalProvider(LocalBottomInset provides if (fullScreen) 0.dp else inset) {
            AnimatedContent(
                targetState = router.tab,
                transitionSpec = {
                    if (reduce) fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                    else (fadeIn(tween(220, 60)) + scaleIn(tween(320, easing = Motion.entranceEasing), initialScale = 0.985f)) togetherWith fadeOut(tween(140))
                },
                label = "tabs",
            ) { tab ->
                holder.SaveableStateProvider(tab.name) {
                    CompositionLocalProvider(LocalTab provides tab) {
                        StackHost(tab) { TabRoot(tab) }
                    }
                }
            }
        }

        // Bottom chrome: undo bar, mini player and tab bar. Slides away under full screen routes.
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer { translationY = barShift * (TabBarHeight + navBottom + 160.dp).toPx() }
                .padding(bottom = navBottom + 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UndoBar(LocalUndo.current)
            AnimatedVisibility(
                visible = hasPlayer,
                enter = slideInVertically(spring(dampingRatio = 0.85f, stiffness = 300f)) { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                MiniPlayer(Modifier.padding(horizontal = 16.dp).height(MiniPlayerHeight))
            }
            TabBar()
        }
    }
}

@Composable
private fun TabBar() {
    val router = LocalRouter.current
    val haptics = rememberHaptics()
    val dark = LocalDark.current
    val glassFill = if (dark) Color(0xF7171E1B) else Color(0xF8FFFFFF)
    val edge = if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .height(TabBarHeight)
                .shadow(18.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
                .clip(CircleShape)
                .background(glassFill)
                .border(0.6.dp, edge, CircleShape)
                .padding(4.dp),
        ) {
            val tabs = AppTab.entries
            val itemWidth = maxWidth / tabs.size
            val index = tabs.indexOf(router.tab)
            // The selection lozenge glides between tabs with a little stretch, like a drop of glass.
            val x by animateDpAsState(itemWidth * index, spring(dampingRatio = 0.72f, stiffness = 380f), label = "pill")
            Box(
                Modifier
                    .offset(x = x)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Palette.accentWash)
            )
            Row(Modifier.fillMaxSize()) {
                tabs.forEach { tab ->
                    val selected = tab == router.tab
                    TabItem(tab, selected, Modifier.weight(1f)) {
                        if (!selected) haptics.tick()
                        router.select(tab)
                    }
                }
            }
        }
        // Search sits apart in its own circle, as on iOS 26.
        Box(
            Modifier
                .size(TabBarHeight)
                .shadow(18.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
                .clip(CircleShape)
                .background(glassFill)
                .border(0.6.dp, edge, CircleShape)
                .pressable(scale = 0.9f) {
                    haptics.tick()
                    if (router.current?.route != Route.Search) router.push(Route.Search)
                }
                .semantics { contentDescription = "Search" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Search, null, tint = Palette.textPrimary, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun TabItem(tab: AppTab, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val (outline, filled) = tabIcons(tab)
    val tint = if (selected) Palette.accent else Palette.textSecondary
    val lift by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = 500f), label = "lift")
    Column(
        modifier
            .fillMaxHeight()
            .clip(CircleShape)
            .pressable(scale = 0.9f, onClick = onClick)
            .semantics {
                this.selected = selected
                role = Role.Tab
                contentDescription = tab.title
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            if (selected) filled else outline, null, tint = tint,
            modifier = Modifier.size(24.dp).graphicsLayer { val s = 1f + 0.08f * lift; scaleX = s; scaleY = s },
        )
        Spacer(Modifier.height(2.dp))
        Text(tab.title, fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, color = tint, maxLines = 1)
    }
}

private fun tabIcons(tab: AppTab): Pair<ImageVector, ImageVector> = when (tab) {
    AppTab.today -> Icons.Outlined.WbSunny to Icons.Filled.WbSunny
    AppTab.quran -> Icons.AutoMirrored.Outlined.MenuBook to Icons.AutoMirrored.Filled.MenuBook
    AppTab.duas -> Icons.Outlined.VolunteerActivism to Icons.Filled.VolunteerActivism
    AppTab.journal -> Icons.Outlined.EditNote to Icons.Filled.EditNote
    AppTab.you -> Icons.Outlined.AccountCircle to Icons.Filled.AccountCircle
}

/**
 * One tab's stack. Pushing slides the new screen in from the side while the one beneath eases back
 * and dims, as on iPhone; full screen routes rise from the bottom instead. Android's predictive back
 * gesture drives the same motion with her finger, and lets go or snaps back.
 */
@Composable
fun StackHost(tab: AppTab, root: @Composable () -> Unit) {
    val router = LocalRouter.current
    val stack = router.stack(tab)
    val holder = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()
    val reduce = LocalReduceMotion.current

    var shown by remember { mutableStateOf(stack.toList()) }
    val top = remember { Animatable(1f) }

    LaunchedEffect(tab) {
        snapshotFlow { stack.toList() }.collect { target ->
            val before = shown
            when {
                target.size > before.size && target.take(before.size) == before -> {
                    shown = target
                    if (reduce) top.snapTo(1f) else {
                        top.snapTo(0f)
                        top.animateTo(1f, spring(dampingRatio = 1f, stiffness = 320f))
                    }
                }
                target.size < before.size && before.take(target.size) == target -> {
                    if (!reduce) top.animateTo(0f, spring(dampingRatio = 1f, stiffness = 420f))
                    shown = target
                    top.snapTo(1f)
                    before.drop(target.size).forEach { holder.removeState(it.id) }
                }
                else -> {
                    before.filter { it !in target }.forEach { holder.removeState(it.id) }
                    shown = target
                    top.snapTo(1f)
                }
            }
        }
    }

    PredictiveBackHandler(enabled = stack.isNotEmpty()) { progress ->
        try {
            progress.collect { event -> top.snapTo(1f - event.progress * 0.9f) }
            router.pop()
        } catch (e: CancellationException) {
            scope.launch { top.animateTo(1f, spring(dampingRatio = 1f, stiffness = 500f)) }
            throw e
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val count = shown.size
        val p = top.value
        val topEntry = shown.lastOrNull()
        val vertical = topEntry?.route?.fullScreen == true

        @Composable
        fun Layer(key: Any, isRoot: Boolean, layer: Modifier, content: @Composable () -> Unit) {
            holder.SaveableStateProvider(key) {
                CompositionLocalProvider(LocalIsRoot provides isRoot) {
                    Box(layer.fillMaxSize()) { content() }
                }
            }
        }

        // The screen underneath, only while something moves over it.
        if (count > 0 && p < 1f) {
            val under = shown.getOrNull(count - 2)
            val mod = Modifier.graphicsLayer {
                if (!vertical) translationX = -0.28f * widthPx * p
                else { val s = 1f - 0.04f * p; scaleX = s; scaleY = s }
            }
            Box(Modifier.fillMaxSize()) {
                if (under == null) Layer("root", true, mod, root)
                else Layer(under.id, false, mod) { RouteScreen(under.route) }
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f * p)))
            }
        }

        if (topEntry == null) {
            Layer("root", true, Modifier, root)
        } else {
            val mod = Modifier
                .graphicsLayer {
                    if (vertical) translationY = (1f - p) * heightPx
                    else translationX = (1f - p) * widthPx
                    if (p < 1f) {
                        shadowElevation = 24.dp.toPx() * p
                        shape = RoundedCornerShape(if (vertical) 28.dp else 0.dp)
                        clip = vertical
                    }
                }
            Layer(topEntry.id, false, mod) { RouteScreen(topEntry.route) }
        }
    }
}
