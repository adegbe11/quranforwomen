package com.quranforwomen.app.feature.dhikr

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.style.TextOverflow
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.ArabicSearch
import com.quranforwomen.app.data.CustomDhikr
import com.quranforwomen.app.data.DhikrPhrase
import com.quranforwomen.app.data.SourceKind
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.SourceLabel
import com.quranforwomen.app.ui.components.AppAlert
import com.quranforwomen.app.ui.components.ArabicText
import com.quranforwomen.app.ui.components.LocalBottomInset
import com.quranforwomen.app.ui.components.LocalIsRoot
import com.quranforwomen.app.ui.components.RollingNumber
import com.quranforwomen.app.ui.components.TopBar
import com.quranforwomen.app.ui.components.TopBarHeight
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.inkReveal
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BeadGold = Color(0xFFC9A24A)
private val SparkGold = Color(0xFFD3A94A)

/**
 * A quiet digital tasbih. One phrase, one large count, one generous tap surface.
 * Motion follows a tasbih in her hand: beads gather, a marker moves bead to bead,
 * each round of beads releases with a wave, and her target ends in a small golden bloom.
 */
@Composable
fun DhikrScreen() {
    val graph = LocalGraph.current
    val content = graph.content
    val library = graph.library
    val prefs = graph.prefs
    val router = LocalRouter.current
    val isRoot = LocalIsRoot.current
    val reduce = LocalReduceMotion.current
    val haptics = rememberHaptics()

    val extras = graph.dhikrExtras

    if (content.dhikr.isEmpty()) return

    val phrase = content.dhikr(prefs.dhikrSelection) ?: content.dhikr[0]
    /** Her own phrase, when she chose one. Never mixed into the sourced list. */
    val personal = extras.customDhikr(prefs.dhikrSelection)
    /** The id her count is kept under: the sourced phrase id, or her own phrase's key. */
    val countID = personal?.countID ?: phrase.id
    val suggestedTarget = personal?.target ?: phrase.target
    // 0 means no target. -1 means use the phrase's suggested target.
    val targetSetting = prefs.dhikrTarget
    val target: Int? = when (targetSetting) {
        -1 -> suggestedTarget.takeIf { it > 0 }
        0 -> null
        else -> targetSetting
    }
    val count = library.dhikrCount(countID)
    val allIDs = content.dhikr.map { it.id } + extras.custom.map { it.countID }
    val beads = minOf(target ?: 33, 33).coerceAtLeast(1)

    var confirmReset by remember { mutableStateOf(false) }
    var completions by remember { mutableIntStateOf(0) }
    var rounds by remember { mutableIntStateOf(0) }
    /** Which way the phrase slides when she changes it. */
    var forward by remember { mutableStateOf(true) }
    var targetMenu by remember { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var goalMenu by remember { mutableStateOf(false) }
    val targetBounce = remember { Animatable(1f) }
    var targetChanges by remember { mutableIntStateOf(0) }
    LaunchedEffect(targetChanges) {
        if (targetChanges > 0 && !reduce) {
            targetBounce.snapTo(0.7f)
            targetBounce.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
        }
    }

    fun select(id: String, isForward: Boolean? = null) {
        if (id == prefs.dhikrSelection) return
        val from = allIDs.indexOf(countID).coerceAtLeast(0)
        val to = allIDs.indexOf(id).coerceAtLeast(0)
        forward = isForward ?: (to > from)
        prefs.dhikrSelection = id
        haptics.tick()
    }

    /**
     * Moves to the next or previous phrase, wrapping around. Her own phrases and the
     * sourced list each wrap within themselves, so the two are never mixed.
     */
    fun step(offset: Int) {
        val group = if (personal != null) extras.custom.map { it.countID } else content.dhikr.map { it.id }
        val index = group.indexOf(countID)
        if (index < 0 || group.isEmpty()) return
        select(group[(index + offset + group.size) % group.size], isForward = offset > 0)
    }

    fun increment() {
        val next = count + 1
        val goal = extras.goal
        val before = extras.total()
        library.setDhikrCount(countID, next)
        extras.recordCount()
        extras.playClick()
        val goalReached = goal != null && before < goal && before + 1 >= goal
        // A soft tap for each count, a firmer one at the end of each round, success at her target
        // and when today's total reaches her daily goal.
        if (target != null && next == target) {
            completions++
            haptics.success()
        } else if (next % beads == 0) {
            rounds++
            if (goalReached) haptics.success() else haptics.tap()
        } else {
            if (goalReached) haptics.success() else haptics.tick()
        }
    }

    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = LocalBottomInset.current + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(Modifier.fillMaxSize().background(Palette.background)) {
        Column(
            Modifier.fillMaxSize().padding(top = top + TopBarHeight, bottom = bottom),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PhraseHeader(
                shown = ShownPhrase(countID, phrase, personal),
                all = content.dhikr,
                custom = extras.custom,
                forward = forward,
                reduce = reduce,
                onSelect = { select(it) },
                onStep = { step(it) },
                onManage = { router.push(Route.CustomDhikr) },
                modifier = Modifier.padding(top = Space.l).padding(horizontal = Space.xl),
            )

            Spacer(Modifier.heightIn(min = Space.l).weight(1f))

            Counter(
                count = count,
                target = target,
                beads = beads,
                rounds = rounds,
                completions = completions,
                reduce = reduce,
            )

            Spacer(Modifier.heightIn(min = Space.l).weight(1f))

            Box(
                Modifier
                    .padding(horizontal = Space.xl)
                    .padding(bottom = Space.xl)
                    .dealIn(0, baseDelay = 350),
            ) {
                TapSurface(
                    count = count,
                    target = target,
                    reduce = reduce,
                    onCount = { increment() },
                )
                // Today's total against her daily goal. It lets taps through to the surface.
                val goal = extras.goal
                androidx.compose.animation.AnimatedVisibility(
                    visible = goal != null,
                    enter = if (reduce) fadeIn() else scaleIn(Motion.reward(), initialScale = 0.8f, transformOrigin = TransformOrigin(0f, 0f)) + fadeIn(),
                    exit = if (reduce) fadeOut() else scaleOut(targetScale = 0.8f, transformOrigin = TransformOrigin(0f, 0f)) + fadeOut(),
                    modifier = Modifier.align(Alignment.TopStart).padding(Space.s),
                ) {
                    var shownGoal by remember { mutableIntStateOf(goal ?: 0) }
                    if (goal != null) shownGoal = goal
                    DhikrGoalRing(today = extras.total(), goal = shownGoal)
                }
            }
        }

        TopBar(
            title = "Dhikr",
            backgroundAlpha = 0f,
            onBack = if (isRoot) null else ({ router.pop(); Unit }),
            actions = {
                Box {
                    DhikrBarIcon(
                        icon = symbol("target"),
                        label = "Target",
                        scale = targetBounce.value,
                    ) { targetMenu = true }
                    DropdownMenu(
                        expanded = targetMenu,
                        onDismissRequest = { targetMenu = false },
                        shape = RoundedCornerShape(14.dp),
                        containerColor = Palette.surface,
                    ) {
                        listOf(
                            -1 to when {
                                personal == null -> "Suggested (${phrase.target})"
                                suggestedTarget > 0 -> "Your target ($suggestedTarget)"
                                else -> "Your target (none)"
                            },
                            33 to "33",
                            99 to "99",
                            100 to "100",
                            0 to "No target",
                        ).forEach { (value, title) ->
                            MenuChoice(title, selected = targetSetting == value) {
                                targetMenu = false
                                if (prefs.dhikrTarget != value) {
                                    prefs.dhikrTarget = value
                                    targetChanges++
                                    haptics.tick()
                                }
                            }
                        }
                    }
                }
                DhikrBarIcon(
                    icon = symbol("arrow.counterclockwise"),
                    label = "Reset",
                    enabled = count > 0,
                ) { confirmReset = true }
                // Sound, daily goal, history and her own phrases.
                Box {
                    DhikrBarIcon(icon = symbol("ellipsis.circle"), label = "More") { moreMenu = true }
                    DropdownMenu(
                        expanded = moreMenu,
                        onDismissRequest = { moreMenu = false },
                        shape = RoundedCornerShape(14.dp),
                        containerColor = Palette.surface,
                    ) {
                        DhikrMenuRow("Counting Sound", icon = DhikrIcons.sound, checked = extras.soundOn) {
                            extras.updateSoundOn(!extras.soundOn)
                            haptics.tick()
                        }
                        DhikrMenuRow(
                            if (extras.goal != null) "Daily Goal (${extras.goal})" else "Daily Goal",
                            icon = DhikrIcons.goal,
                        ) {
                            moreMenu = false
                            goalMenu = true
                        }
                        HorizontalDivider(color = Palette.border)
                        DhikrMenuRow("History", icon = DhikrIcons.barChart) {
                            moreMenu = false
                            router.push(Route.DhikrHistory)
                        }
                        DhikrMenuRow("Your Own Dhikr", icon = symbol("pencil")) {
                            moreMenu = false
                            router.push(Route.CustomDhikr)
                        }
                    }
                    DropdownMenu(
                        expanded = goalMenu,
                        onDismissRequest = { goalMenu = false },
                        shape = RoundedCornerShape(14.dp),
                        containerColor = Palette.surface,
                    ) {
                        listOf(0, 100, 300, 500, 1000).forEach { value ->
                            MenuChoice(if (value == 0) "Off" else "$value", selected = (extras.goal ?: 0) == value) {
                                goalMenu = false
                                if ((extras.goal ?: 0) != value) {
                                    extras.updateGoal(value)
                                    haptics.tick()
                                }
                            }
                        }
                    }
                }
            },
        )
    }

    if (confirmReset) {
        AppAlert(
            title = "Reset the count to zero?",
            confirmLabel = "Reset",
            destructive = true,
            onConfirm = { library.setDhikrCount(countID, 0) },
            onDismiss = { confirmReset = false },
        )
    }
}

// Parts

/** What the header shows: a sourced phrase, or her own words when she chose them. */
private data class ShownPhrase(val id: String, val phrase: DhikrPhrase, val personal: CustomDhikr?) {
    val name: String get() = personal?.text ?: phrase.transliteration
}

@Composable
private fun PhraseHeader(
    shown: ShownPhrase,
    all: List<DhikrPhrase>,
    custom: List<CustomDhikr>,
    forward: Boolean,
    reduce: Boolean,
    onSelect: (String) -> Unit,
    onStep: (Int) -> Unit,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var picker by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.s)) {
        Box(Modifier.focusIn(50)) {
            Row(
                Modifier
                    .heightIn(min = 44.dp)
                    .clip(CircleShape)
                    .background(Palette.accentWash)
                    .pressable(scale = 0.96f) { picker = true }
                    .semantics(mergeDescendants = true) { contentDescription = "Choose dhikr. Current: ${shown.name}" }
                    .padding(horizontal = Space.m, vertical = Space.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AnimatedContent(
                    targetState = shown.name,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    label = "phraseName",
                    modifier = Modifier.widthIn(max = 240.dp),
                ) { Text(it, style = TextStyles.headline, color = Palette.accent, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Icon(symbol("chevron.up.chevron.down"), null, tint = Palette.accent, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(
                expanded = picker,
                onDismissRequest = { picker = false },
                shape = RoundedCornerShape(14.dp),
                containerColor = Palette.surface,
            ) {
                all.forEach { item ->
                    MenuChoice(item.transliteration, selected = item.id == shown.id) {
                        picker = false
                        onSelect(item.id)
                    }
                }
                if (custom.isNotEmpty()) {
                    HorizontalDivider(color = Palette.border)
                    Text(
                        "Your Own Words",
                        style = TextStyles.footnote,
                        color = Palette.textSecondary,
                        modifier = Modifier.padding(horizontal = Space.m, vertical = Space.xs).semantics { heading() },
                    )
                    custom.forEach { item ->
                        MenuChoice(item.text, selected = item.countID == shown.id) {
                            picker = false
                            onSelect(item.countID)
                        }
                    }
                }
                HorizontalDivider(color = Palette.border)
                DhikrMenuRow("Your Own Dhikr", icon = symbol("pencil")) {
                    picker = false
                    onManage()
                }
            }
        }

        // The phrase slides in the direction she swiped, and the Arabic writes itself in.
        val swipeMin = with(density) { 24.dp.toPx() }
        AnimatedContent(
            targetState = shown,
            transitionSpec = {
                if (reduce) fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                else {
                    val spec = spring<IntOffset>(dampingRatio = 0.86f, stiffness = 200f)
                    (slideInHorizontally(spec) { if (forward) it else -it } + fadeIn()) togetherWith
                        (slideOutHorizontally(spec) { if (forward) -it else it } + fadeOut())
                }
            },
            contentKey = { it.id },
            label = "phrase",
            modifier = Modifier
                .fillMaxWidth()
                .pointerSwipe(swipeMin, onStep)
                .semantics(mergeDescendants = true) {
                    customActions = listOf(
                        CustomAccessibilityAction("Next dhikr") { onStep(1); true },
                        CustomAccessibilityAction("Previous dhikr") { onStep(-1); true },
                    )
                },
        ) { s ->
            key(s.id) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    val own = s.personal
                    if (own != null) {
                        // Her own words: shown as written, and marked as personal.
                        if (ArabicSearch.containsArabic(own.text)) {
                            ArabicText(
                                own.text,
                                size = 34f,
                                center = true,
                                color = Palette.textPrimary,
                                modifier = Modifier.padding(top = Space.xs).inkReveal(150, 900),
                            )
                        } else {
                            Text(
                                own.text,
                                style = TextStyles.title2.copy(fontFamily = AppFont.serif, fontWeight = FontWeight.Normal),
                                color = Palette.textPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = Space.xs).focusIn(150),
                            )
                        }
                        Box(Modifier.focusIn(450)) { SourceLabel(SourceKind.Personal, "Your own words") }
                    } else {
                        val p = s.phrase
                        ArabicText(
                            p.arabic,
                            size = 38f,
                            center = true,
                            color = Palette.textPrimary,
                            modifier = Modifier.padding(top = Space.xs).inkReveal(150, 900),
                        )
                        Text(
                            p.translation,
                            style = TextStyles.body.copy(fontFamily = AppFont.serif),
                            color = Palette.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.focusIn(450),
                        )
                        Text(
                            p.source,
                            style = TextStyles.caption,
                            color = Palette.textSecondary.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.focusIn(550),
                        )
                    }
                }
            }
        }
    }
}

/** A horizontal swipe changes the phrase, left for the next one, right for the previous one. */
@Composable
private fun Modifier.pointerSwipe(minDistance: Float, onStep: (Int) -> Unit): Modifier {
    // The gesture outlives recompositions, so it always calls the latest step action.
    val latestStep by androidx.compose.runtime.rememberUpdatedState(onStep)
    return this.then(
        Modifier.pointerInput(Unit) {
            var total = 0f
            detectHorizontalDragGestures(
                onDragStart = { total = 0f },
                onDragEnd = { if (abs(total) > minDistance) latestStep(if (total < 0) 1 else -1) },
                onHorizontalDrag = { change, amount ->
                    change.consume()
                    total += amount
                },
            )
        }
    )
}

@Composable
private fun Counter(
    count: Int,
    target: Int?,
    beads: Int,
    rounds: Int,
    completions: Int,
    reduce: Boolean,
) {
    val filled = if (count == 0) 0 else ((count - 1) % beads) + 1
    val complete = target != null && count >= target && count > 0

    // A small lift with every count, a bigger one at her target.
    val lift = remember { Animatable(1f) }
    var lastCount by remember { mutableIntStateOf(count) }
    LaunchedEffect(count) {
        if (count != lastCount && !reduce) {
            lift.animateTo(if (complete) 1.16f else 1.06f, tween(80))
            lift.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 400f))
        }
        lastCount = count
    }

    // The glow breathes, and blooms when she reaches her target.
    val glowScale = remember { Animatable(0.9f) }
    val glowAlpha = remember { Animatable(0.4f) }
    LaunchedEffect(completions) {
        if (completions > 0 && !reduce) {
            launch { glowScale.animateTo(1.5f, Motion.smooth()); glowScale.animateTo(0.9f, Motion.smooth()) }
            glowAlpha.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
            glowAlpha.animateTo(0.4f, tween(800, easing = FastOutSlowInEasing))
        }
    }

    val labelColor by animateColorAsState(if (complete) Palette.accent else Palette.textSecondary, label = "targetColor")
    val a11y = if (target != null) "$count of $target" else "$count"

    Box(
        Modifier.size(264.dp).clearAndSetSemantics { contentDescription = a11y },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(200.dp)
                .graphicsLayer {
                    scaleX = glowScale.value; scaleY = glowScale.value; alpha = glowAlpha.value
                }
                .blur(30.dp, BlurredEdgeTreatment.Unbounded)
                .clip(CircleShape)
                .background(Palette.accentWash)
        )

        BeadRing(total = beads, filled = filled, count = count, round = rounds, complete = complete, reduce = reduce)

        CompletionSparks(trigger = completions, reduce = reduce)

        Column(
            Modifier.width(180.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.xxs),
        ) {
            RollingNumber(
                value = count,
                style = TextStyles.largeTitle.copy(
                    fontSize = if (count >= 10000) 48.sp else if (count >= 1000) 60.sp else 76.sp,
                    lineHeight = 84.sp,
                    fontWeight = FontWeight.Light,
                    fontFeatureSettings = "tnum",
                ),
                color = Palette.textPrimary,
                modifier = Modifier.graphicsLayer { scaleX = lift.value; scaleY = lift.value },
            )
            if (target != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AnimatedVisibility(
                        visible = complete,
                        enter = scaleIn(spring(dampingRatio = 0.68f, stiffness = 190f)) + fadeIn(),
                        exit = scaleOut() + fadeOut(),
                    ) {
                        Icon(symbol("checkmark"), null, tint = Palette.accent, modifier = Modifier.size(17.dp))
                    }
                    AnimatedContent(
                        targetState = if (complete) "Complete" else "of $target",
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                        label = "targetLabel",
                    ) {
                        Text(
                            it,
                            style = TextStyles.subheadline.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
                            color = labelColor,
                        )
                    }
                }
            }
        }
    }
}

/** Per bead animation values. */
private class BeadAnim {
    val gather = Animatable(0f)
    val fill = Animatable(0f)
    val lift = Animatable(1f)
    val wave = Animatable(1f)
    val gold = Animatable(0f)
}

/**
 * Beads around the count, like a tasbih in her hand.
 * They gather one by one on arrival, a marker moves to each new bead along the ring,
 * a wave runs through them at the end of each round, and they release one after another.
 */
@Composable
private fun BeadRing(total: Int, filled: Int, count: Int, round: Int, complete: Boolean, reduce: Boolean) {
    val anims = remember(total) { List(total) { BeadAnim() } }
    var lastFilled by remember(total) { mutableIntStateOf(filled) }
    val releasing = filled < lastFilled
    val accentFill = Palette.accentFill
    val border = Palette.border
    val accent = Palette.accent

    for (i in 0 until total) {
        val a = anims[i]
        key(total, i) {
            // Gather on arrival.
            LaunchedEffect(Unit) {
                a.fill.snapTo(if (i < filled) 1f else 0f)
                a.gold.snapTo(if (complete) 1f else 0f)
                if (reduce) { a.gather.snapTo(1f); return@LaunchedEffect }
                delay(100L + i * 16L)
                a.gather.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = 158f))
            }
            // Fill and release. Beads release in turn, last to first.
            LaunchedEffect(filled) {
                val on = i < filled
                val target = if (on) 1f else 0f
                val lifted = if (i == filled - 1 && !reduce) 1.45f else 1f
                if (reduce) { a.fill.snapTo(target); a.lift.snapTo(1f); return@LaunchedEffect }
                if (releasing) delay((total - i) * 12L)
                launch { a.lift.animateTo(lifted, spring(dampingRatio = 0.55f, stiffness = 322f)) }
                a.fill.animateTo(target, spring(dampingRatio = 0.55f, stiffness = 322f))
            }
            // A wave through the beads at the end of each round.
            LaunchedEffect(round) {
                if (round == 0 || reduce) return@LaunchedEffect
                delay(i * 12L)
                a.wave.animateTo(1.6f, tween(120, easing = FastOutSlowInEasing))
                a.wave.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f))
            }
            // The beads turn gold one after another at her target.
            LaunchedEffect(complete) {
                if (reduce) { a.gold.snapTo(if (complete) 1f else 0f); return@LaunchedEffect }
                delay(i * 15L)
                a.gold.animateTo(if (complete) 1f else 0f, tween(500, easing = FastOutSlowInEasing))
            }
        }
    }
    LaunchedEffect(filled) { lastFilled = filled }

    // The marker: a soft light that travels along the ring to the newest bead.
    val markerAngle = remember { Animatable(((count - 1).coerceAtLeast(0)).toFloat()) }
    LaunchedEffect(count) {
        if (count > 0) markerAngle.animateTo((count - 1).toFloat(), spring(dampingRatio = 0.72f, stiffness = 273f))
        else markerAngle.snapTo(0f)
    }
    val markerAlpha by androidx.compose.animation.core.animateFloatAsState(
        if (count > 0 && !reduce) 1f else 0f, tween(200), label = "marker",
    )
    val markerGold by androidx.compose.animation.core.animateFloatAsState(if (complete) 1f else 0f, tween(500), label = "markerGold")

    Canvas(Modifier.size(264.dp)) {
        val size = minOf(this.size.width, this.size.height)
        val radius = size / 2 - 10.dp.toPx()
        val beadDp = (2 * PI.toFloat() * (radius / density) / total * 0.55f).coerceIn(6f, 11f)
        val bead = beadDp * density
        val c = center
        for (i in 0 until total) {
            val a = anims[i]
            val angle = i.toFloat() / total * 2 * PI.toFloat() - PI.toFloat() / 2
            val on = lerp(border, lerp(accentFill, BeadGold, a.gold.value), a.fill.value.coerceIn(0f, 1f))
            val scale = a.gather.value * a.lift.value * a.wave.value
            val alpha = a.gather.value.coerceIn(0f, 1f)
            if (scale <= 0.001f) continue
            drawCircle(
                color = on.copy(alpha = alpha),
                radius = bead / 2 * scale,
                center = Offset(c.x + cos(angle) * radius, c.y + sin(angle) * radius),
            )
        }
        if (markerAlpha > 0f) {
            val angle = markerAngle.value / total * 2 * PI.toFloat() - PI.toFloat() / 2
            val p = Offset(c.x + cos(angle) * radius, c.y + sin(angle) * radius)
            val tint = lerp(accent, BeadGold, markerGold).copy(alpha = 0.25f * markerAlpha)
            drawCircle(
                brush = Brush.radialGradient(listOf(tint, tint, tint.copy(alpha = 0f)), center = p, radius = bead * 1.5f + 3.dp.toPx()),
                radius = bead * 1.5f + 3.dp.toPx(),
                center = p,
            )
        }
    }
}

/** Gold and emerald sparks that fly out from the count when she reaches her target. */
@Composable
private fun CompletionSparks(trigger: Int, reduce: Boolean) {
    if (reduce) return
    val distance = remember { Animatable(40f) }
    val time = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        distance.snapTo(40f)
        time.snapTo(0f)
        launch { distance.animateTo(150f, spring(dampingRatio = 0.85f, stiffness = 120f)) }
        time.animateTo(1f, tween(750, easing = LinearEasing))
    }
    val emerald = Palette.accentFill
    Canvas(Modifier.size(264.dp)) {
        val t = time.value * 0.75f
        if (time.value >= 1f) return@Canvas
        val opacity = if (t < 0.4f) 1f else (1f - (t - 0.4f) / 0.35f).coerceIn(0f, 1f)
        val scale = if (t < 0.2f) 1f + 0.3f * (t / 0.2f) else (1.3f - 0.8f * ((t - 0.2f) / 0.5f)).coerceAtLeast(0.5f)
        for (i in 0 until 14) {
            val angle = i / 14f * 2 * PI.toFloat()
            val d = distance.value * density
            drawCircle(
                color = (if (i % 2 == 0) SparkGold else emerald).copy(alpha = opacity),
                radius = 3.dp.toPx() * scale,
                center = Offset(center.x + cos(angle) * d, center.y + sin(angle) * d),
            )
        }
    }
}

private data class Ripple(val id: String = UUID.randomUUID().toString(), val origin: Offset)

@Composable
private fun TapSurface(
    count: Int,
    target: Int?,
    reduce: Boolean,
    onCount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ripples = remember { mutableStateListOf<Ripple>() }
    val scope = rememberCoroutineScope()
    // The tap handler lives across recompositions, so it must always call the latest count action.
    val latestOnCount by androidx.compose.runtime.rememberUpdatedState(onCount)
    var pressed by remember { mutableStateOf(false) }
    val press by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.98f else 1f, Motion.press(), label = "tapPress")
    val shape = RoundedCornerShape(28.dp)

    // Invites the first tap, then answers each one.
    // Only runs before the first tap, so nothing keeps drawing while she counts.
    val pulseAlpha = if (count == 0 && !reduce) {
        val pulse = rememberInfiniteTransition(label = "pulse")
        pulse.animateFloat(
            1f, 0.35f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulseAlpha",
        ).value
    } else 1f
    val bounce = remember { Animatable(1f) }
    var lastCount by remember { mutableIntStateOf(count) }
    LaunchedEffect(count) {
        if (count != lastCount && !reduce) {
            bounce.snapTo(0.8f)
            bounce.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f))
        }
        lastCount = count
    }

    val value = if (target != null) "$count of $target" else "$count"
    Box(
        modifier
            .fillMaxWidth()
            .height(190.dp)
            .graphicsLayer { scaleX = press; scaleY = press }
            .clip(shape)
            .background(Palette.accentWash)
            .pointerInput(reduce) {
                detectTapGestures { offset ->
                    if (!reduce) {
                        val ripple = Ripple(origin = offset)
                        ripples.add(ripple)
                        scope.launch {
                            delay(700)
                            ripples.remove(ripple)
                        }
                    }
                    pressed = true
                    scope.launch {
                        delay(90)
                        pressed = false
                    }
                    latestOnCount()
                }
            }
            .semantics {
                role = Role.Button
                contentDescription = "Count"
                stateDescription = value
                onClick(label = "Add one") { onCount(); true }
            },
        contentAlignment = Alignment.Center,
    ) {
        ripples.forEach { ripple ->
            key(ripple.id) { RippleView(ripple.origin) }
        }
        Icon(
            symbol("hand.tap"), null, tint = Palette.accent,
            modifier = Modifier
                .size(34.dp)
                .graphicsLayer {
                    scaleX = bounce.value; scaleY = bounce.value
                    alpha = if (count == 0 && !reduce) pulseAlpha else 1f
                },
        )
    }
}

/** A ring of light that spreads from where her finger touched. */
@Composable
private fun RippleView(origin: Offset) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
    val color = Palette.accent.copy(alpha = 0.18f)
    Canvas(Modifier.fillMaxSize()) {
        val p = progress.value
        val scale = 0.3f + (6f - 0.3f) * p
        drawCircle(color.copy(alpha = color.alpha * (1f - p)), radius = 30.dp.toPx() * scale, center = origin)
    }
}

/** A bar action that can be turned off, with an optional bounce scale. */
@Composable
private fun DhikrBarIcon(
    icon: ImageVector,
    label: String,
    enabled: Boolean = true,
    scale: Float = 1f,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(44.dp)
            .graphicsLayer { alpha = if (enabled) 1f else 0.35f; scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .pressable(scale = 0.9f, enabled = enabled, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Palette.accent, modifier = Modifier.size(22.dp))
    }
}

/** A menu row with a checkmark on the chosen one, like an iOS picker menu. */
@Composable
private fun MenuChoice(title: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(title, style = TextStyles.body, color = Palette.textPrimary) },
        trailingIcon = if (selected) ({ Icon(symbol("checkmark"), null, tint = Palette.accent, modifier = Modifier.size(20.dp)) }) else null,
        onClick = onClick,
        modifier = Modifier.semantics { this.selected = selected },
    )
}
