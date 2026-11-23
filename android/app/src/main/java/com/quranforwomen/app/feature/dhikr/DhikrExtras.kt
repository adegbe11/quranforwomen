package com.quranforwomen.app.feature.dhikr

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.ArabicSearch
import com.quranforwomen.app.data.CustomDhikr
import com.quranforwomen.app.data.SourceKind
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.AppSheet
import com.quranforwomen.app.ui.components.AppTextField
import com.quranforwomen.app.ui.components.ArabicText
import com.quranforwomen.app.ui.components.BarIcon
import com.quranforwomen.app.ui.components.CardSurface
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.ListRow
import com.quranforwomen.app.ui.components.LocalUndo
import com.quranforwomen.app.ui.components.RollingNumber
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.SavedEmpty
import com.quranforwomen.app.ui.components.SegmentedControl
import com.quranforwomen.app.ui.components.SourceLabel
import com.quranforwomen.app.ui.components.TextAction
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.rowPress
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Radius
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.delay

private val GoalGold = Color(0xFFC9A24A)

// Daily goal ring

/**
 * A small ring for today's total against her daily goal. It fills gently with each count
 * and warms to gold, with one soft bloom, when the goal is reached.
 */
@Composable
internal fun DhikrGoalRing(today: Int, goal: Int, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val reached = goal > 0 && today >= goal
    val progress by animateFloatAsState(
        if (goal > 0) (today.toFloat() / goal).coerceIn(0f, 1f) else 0f,
        if (reduce) snap() else Motion.gentle(),
        label = "goalProgress",
    )
    val ringColor by animateColorAsState(if (reached) GoalGold else Palette.accent, tween(500), label = "goalColor")
    val track = Palette.border
    val bloom = remember { Animatable(1f) }
    var wasReached by remember { mutableStateOf(reached) }
    LaunchedEffect(reached) {
        if (reached && !wasReached && !reduce) {
            bloom.animateTo(1.25f, tween(140, easing = FastOutSlowInEasing))
            bloom.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 400f))
        }
        wasReached = reached
    }
    Row(
        modifier
            .clip(CircleShape)
            .background(Palette.surface.copy(alpha = 0.85f))
            .padding(horizontal = Space.s, vertical = 6.dp)
            .clearAndSetSemantics {
                contentDescription = "Daily goal"
                stateDescription = "$today of $goal today"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Box(
            Modifier.size(28.dp).graphicsLayer { scaleX = bloom.value; scaleY = bloom.value },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 4.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                if (progress > 0f) {
                    drawArc(ringColor, -90f, 360f * progress, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = reached,
                enter = scaleIn(spring(dampingRatio = 0.6f, stiffness = 300f)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                Icon(symbol("checkmark"), null, tint = GoalGold, modifier = Modifier.size(13.dp))
            }
        }
        val textColor by animateColorAsState(if (reached) Palette.accent else Palette.textSecondary, label = "goalText")
        val style = TextStyles.footnote.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")
        Row {
            RollingNumber(today, style, textColor)
            Text(" of $goal", style = style, color = textColor)
        }
    }
}

// History

/**
 * Her total dhikr counts for each of the last 30 days, as quiet bars. Nothing is marked
 * as missed. The bars rise in one after another, and tapping a bar shows that day.
 */
@Composable
fun DhikrHistoryScreen() {
    val store = LocalGraph.current.dhikrExtras
    val router = LocalRouter.current
    val haptics = rememberHaptics()
    val days = store.history()
    val last7 = days.takeLast(7).sumOf { it.second }
    val last30 = days.sumOf { it.second }
    var selected by rememberSaveable { mutableStateOf(days.last().first.toEpochDay()) }

    AppScreen(title = "History", largeTitle = false, spacing = Space.xl) {
        if (last30 == 0) {
            item(key = "empty") {
                SavedEmpty(
                    icon = DhikrIcons.barChart,
                    title = "No dhikr counted yet",
                    actionLabel = "Count Dhikr",
                    onAction = { router.pop() },
                )
            }
        } else {
            item(key = "chart") {
                HistoryChart(
                    days = days,
                    selectedDay = selected,
                    modifier = Modifier.padding(top = Space.s).dealIn(0, baseDelay = 100),
                ) { day ->
                    if (day != selected) {
                        haptics.tick()
                        selected = day
                    }
                }
            }
            item(key = "totals") {
                GroupedSection(Modifier.dealIn(1, baseDelay = 250)) {
                    ListRow(title = "Today", value = "${store.total()}", showsChevron = false)
                    RowDivider(inset = Space.m)
                    ListRow(title = "Last 7 days", value = "$last7", showsChevron = false)
                    RowDivider(inset = Space.m)
                    ListRow(title = "Last 30 days", value = "$last30", showsChevron = false)
                }
            }
        }
    }
}

/** Material icons the shared symbol map does not name. */
internal object DhikrIcons {
    val barChart: ImageVector get() = Icons.Outlined.BarChart
    val sound: ImageVector get() = Icons.AutoMirrored.Outlined.VolumeUp
    val goal: ImageVector get() = Icons.Outlined.DonutLarge
}

@Composable
private fun HistoryChart(
    days: List<Pair<LocalDate, Int>>,
    selectedDay: Long,
    modifier: Modifier = Modifier,
    onSelect: (Long) -> Unit,
) {
    val reduce = LocalReduceMotion.current
    val peak = maxOf(days.maxOfOrNull { it.second } ?: 0, 1)
    val shown = days.firstOrNull { it.first.toEpochDay() == selectedDay } ?: days.last()
    val isToday = shown.first == LocalDate.now()
    val dayFormat = remember { DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()) }
    val shortFormat = remember { DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()) }
    val a11yFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    // The bars rise one after another when the screen opens.
    val rise = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduce) {
            delay(200)
            rise.animateTo(1f, tween(900, easing = LinearOutSlowInEasing))
        }
    }

    CardSurface(modifier = modifier, padding = Space.m, fill = Palette.surface) {
        Column(
            Modifier.focusIn(50).semantics(mergeDescendants = true) { },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            AnimatedContent(
                targetState = if (isToday) "Today" else shown.first.format(dayFormat),
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                label = "historyDay",
            ) { Text(it, style = TextStyles.subheadline.copy(fontWeight = FontWeight.Medium), color = Palette.textSecondary) }
            RollingNumber(
                shown.second,
                TextStyles.largeTitle.copy(fontWeight = FontWeight.Light, fontFeatureSettings = "tnum"),
                Palette.textPrimary,
            )
        }
        Spacer(Modifier.height(Space.m))
        Row(
            Modifier.fillMaxWidth().height(150.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEachIndexed { index, (date, total) ->
                val epoch = date.toEpochDay()
                val isShown = epoch == shown.first.toEpochDay()
                // Each bar starts a little after the one before it.
                val start = index / (days.size + 10f)
                val local = ((rise.value - start) / (1f - start)).coerceIn(0f, 1f)
                val fraction = if (total == 0) 0f else maxOf(total.toFloat() / peak, 0.04f)
                val color by animateColorAsState(
                    when {
                        total == 0 -> Palette.border
                        isShown -> Palette.accent
                        else -> Palette.accent.copy(alpha = 0.35f)
                    },
                    if (reduce) snap() else Motion.press(),
                    label = "bar",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pressable(scale = 0.9f) { onSelect(epoch) }
                        .semantics {
                            contentDescription = date.format(a11yFormat)
                            stateDescription = "$total"
                            this.selected = isShown
                        },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(if (total == 0) 3.dp else (150.dp * fraction * local).coerceAtLeast(3.dp))
                            .clip(RoundedCornerShape(3.dp))
                            .background(color)
                    )
                }
            }
        }
        Spacer(Modifier.height(Space.m))
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
            Text(days.first().first.format(shortFormat), style = TextStyles.caption, color = Palette.textSecondary)
            Spacer(Modifier.weight(1f))
            Text("Today", style = TextStyles.caption, color = Palette.textSecondary)
        }
    }
}

// Her own dhikr

/**
 * Dhikr phrases in her own words. Marked as personal, kept apart from the sourced list.
 * A new phrase glows for a moment, and a deleted one can be brought back with Undo.
 */
@Composable
fun CustomDhikrScreen() {
    val graph = LocalGraph.current
    val store = graph.dhikrExtras
    val undo = LocalUndo.current
    val haptics = rememberHaptics()
    val reduce = LocalReduceMotion.current

    var isAdding by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var glowing by remember { mutableStateOf<String?>(null) }
    var glowToken by remember { mutableIntStateOf(0) }
    var addedToken by remember { mutableIntStateOf(0) }

    fun glow(id: String) {
        glowing = id
        glowToken++
    }

    LaunchedEffect(glowToken) {
        if (glowToken > 0) {
            delay(1600)
            glowing = null
        }
    }

    val plusBounce = remember { Animatable(1f) }
    LaunchedEffect(addedToken) {
        if (addedToken > 0 && !reduce) {
            plusBounce.snapTo(0.7f)
            plusBounce.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
        }
    }

    fun delete(item: CustomDhikr) {
        val index = store.custom.indexOfFirst { it.id == item.id }
        if (index < 0) return
        store.removeCustom(item)
        haptics.soft()
        undo.show("Dhikr deleted") {
            store.restoreCustom(item, index)
            glow(item.id)
        }
    }

    val items = store.custom

    AppScreen(
        title = "Your Own Dhikr",
        largeTitle = false,
        spacing = 0.dp,
        actions = {
            Box(Modifier.graphicsLayer { scaleX = plusBounce.value; scaleY = plusBounce.value }) {
                BarIcon(symbol("plus"), "Add Dhikr") { isAdding = true }
            }
        },
    ) {
        if (items.isEmpty()) {
            item(key = "empty") {
                SavedEmpty(
                    icon = symbol("pencil.and.scribble"),
                    title = "No dhikr of your own yet",
                    actionLabel = "Add Dhikr",
                    onAction = { isAdding = true },
                )
            }
        } else {
            item(key = "top") { Spacer(Modifier.height(Space.s)) }
            itemsIndexed(items, key = { _, d -> d.id }) { index, item ->
                CustomDhikrRow(
                    item = item,
                    first = index == 0,
                    last = index == items.lastIndex,
                    glowing = glowing == item.id,
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = if (reduce) null else tween(300),
                            placementSpec = if (reduce) null else spring(dampingRatio = 0.85f, stiffness = 300f),
                            fadeOutSpec = if (reduce) null else tween(250),
                        )
                        .dealIn(minOf(index, 8), baseDelay = 150, step = 50),
                    onOpen = { editingId = item.id },
                    onDelete = { delete(item) },
                )
            }
        }
    }

    if (isAdding) {
        CustomDhikrEditor(initialText = "", initialTarget = 33, onDismiss = { isAdding = false }) { text, target ->
            val added = store.addCustom(text, target)
            if (added != null) {
                addedToken++
                haptics.success()
                // Her new phrase is ready on the counter when she goes back.
                graph.prefs.dhikrSelection = added.countID
                glow(added.id)
            }
        }
    }

    val editing = editingId?.let { id -> store.custom.firstOrNull { it.id == id } }
    if (editing != null) {
        CustomDhikrEditor(initialText = editing.text, initialTarget = editing.target, onDismiss = { editingId = null }) { text, target ->
            store.updateCustom(editing, text, target)
            glow(editing.id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDhikrRow(
    item: CustomDhikr,
    first: Boolean,
    last: Boolean,
    glowing: Boolean,
    modifier: Modifier,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val radius = Radius.tile
    val shape = RoundedCornerShape(
        topStart = if (first) radius else 0.dp, topEnd = if (first) radius else 0.dp,
        bottomStart = if (last) radius else 0.dp, bottomEnd = if (last) radius else 0.dp,
    )
    val glow by animateFloatAsState(
        if (glowing) 1f else 0f,
        if (glowing) tween(350, delayMillis = 300, easing = LinearOutSlowInEasing) else tween(1000, easing = FastOutSlowInEasing),
        label = "glow",
    )
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        },
    )
    val blush = Palette.blush
    val surface = Palette.surface
    Column(modifier.fillMaxWidth()) {
        SwipeToDismissBox(
            state = state,
            enableDismissFromStartToEnd = false,
            modifier = Modifier.clip(shape),
            backgroundContent = {
                Box(
                    Modifier.fillMaxSize().background(Color(0xFFC62828)).padding(horizontal = Space.l),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(symbol("trash"), null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            },
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(surface)
                    .background(blush.copy(alpha = 0.9f * glow))
                    .heightIn(min = 56.dp)
                    .rowPress(onOpen)
                    .semantics(mergeDescendants = true) {
                        onClick(label = "Edit") { onOpen(); true }
                        customActions = listOf(CustomAccessibilityAction("Delete") { onDelete(); true })
                    }
                    .padding(horizontal = Space.m, vertical = Space.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    if (ArabicSearch.containsArabic(item.text)) {
                        ArabicText(item.text, size = 22f, maxLines = 2, color = Palette.textPrimary)
                    } else {
                        Text(
                            item.text,
                            style = TextStyles.body.copy(fontFamily = AppFont.serif),
                            color = Palette.textPrimary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    SourceLabel(SourceKind.Personal, "Your own words")
                }
                Text(
                    if (item.target > 0) "${item.target}" else "No target",
                    style = TextStyles.subheadline.copy(fontFeatureSettings = "tnum"),
                    color = Palette.textSecondary,
                )
            }
        }
        if (!last) Box(Modifier.background(surface)) { RowDivider(inset = Space.m) }
    }
}

/** Writing or editing her own dhikr phrase and its target. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDhikrEditor(
    initialText: String,
    initialTarget: Int,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
) {
    val reduce = LocalReduceMotion.current
    val targets = listOf(33, 99, 100, 0)
    var text by rememberSaveable { mutableStateOf(initialText) }
    var target by rememberSaveable { mutableIntStateOf(if (initialTarget in targets) initialTarget else 33) }
    var focused by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(200)
        runCatching { focus.requestFocus() }
    }
    val borderColor by animateColorAsState(
        if (focused) Palette.accent.copy(alpha = 0.55f) else Palette.border,
        if (reduce) snap() else Motion.gentle(), label = "dhikrBorder",
    )

    AppSheet(onDismiss = onDismiss) {
        Box(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = Space.xs)) {
            TextAction("Cancel", onDismiss, modifier = Modifier.align(Alignment.CenterStart))
            Text(
                if (initialText.isEmpty()) "New Dhikr" else "Edit Dhikr",
                style = TextStyles.headline,
                color = Palette.textPrimary,
                modifier = Modifier.align(Alignment.Center).semantics { heading() },
            )
            TextAction(
                "Save",
                {
                    onSave(text, target)
                    onDismiss()
                },
                enabled = text.isNotBlank(),
                bold = true,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
        Column(
            Modifier.padding(Space.page).imePadding(),
            verticalArrangement = Arrangement.spacedBy(Space.l),
        ) {
            Box(Modifier.focusIn(50)) { SourceLabel(SourceKind.Personal, "Your own words") }
            Box(
                Modifier
                    .focusIn(100)
                    .clip(RoundedCornerShape(Radius.control))
                    .border(if (focused) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(Radius.control))
            ) {
                AppTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = "Your words",
                    minLines = 2,
                    maxLines = 6,
                    style = TextStyles.title3.copy(fontFamily = AppFont.serif, fontWeight = FontWeight.Normal),
                    modifier = Modifier
                        .focusRequester(focus)
                        .onFocusChanged { focused = it.isFocused },
                )
            }
            Column(Modifier.dealIn(0, baseDelay = 200), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text("Target", style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold), color = Palette.textSecondary)
                SegmentedControl(
                    options = targets,
                    selected = target,
                    label = { if (it == 0) "None" else "$it" },
                    onSelect = { target = it },
                )
            }
            Spacer(Modifier.height(Space.s))
        }
    }
}

/** A dropdown row with an optional leading icon and a check on the chosen one. */
@Composable
internal fun DhikrMenuRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    checked: Boolean = false,
    onClick: () -> Unit,
) {
    androidx.compose.material3.DropdownMenuItem(
        text = { Text(title, style = TextStyles.body, color = Palette.textPrimary) },
        leadingIcon = icon?.let { { Icon(it, null, tint = Palette.textPrimary, modifier = Modifier.size(20.dp)) } },
        trailingIcon = if (checked) ({ Icon(symbol("checkmark"), null, tint = Palette.accent, modifier = Modifier.size(20.dp)) }) else null,
        onClick = onClick,
        modifier = Modifier.semantics { this.selected = checked },
    )
}

