package com.quranforwomen.app.feature.explore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.FinishedPlan
import com.quranforwomen.app.data.GentleReminder
import com.quranforwomen.app.data.GentleReminders
import com.quranforwomen.app.data.QuranStore
import com.quranforwomen.app.data.ReadingPlan
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.feature.reminders.GentleReminderSection
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.AppAlert
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.CardSurface
import com.quranforwomen.app.ui.components.Eyebrow
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.ListRow
import com.quranforwomen.app.ui.components.PetalBurst
import com.quranforwomen.app.ui.components.PrimaryButton
import com.quranforwomen.app.ui.components.RollingNumber
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.SectionHeader
import com.quranforwomen.app.ui.components.Stepper
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.rowPress
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.LocalDark
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Radius
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** The plans she can choose. Page plans read the whole Quran; surah plans read one surah a day. */
private data class PlanOption(val key: String, val title: String, val detail: String, val days: Int? = null)

private const val CUSTOM = "custom"
private const val SHORT_SURAHS = "short"
private const val ALL_SURAHS = "surahs"

private val pageOptions = listOf(
    PlanOption("p30", "30 days", "One juz a day.", 30),
    PlanOption("p60", "60 days", "Half a juz a day.", 60),
    PlanOption("p90", "90 days", "A third of a juz a day.", 90),
    PlanOption("p120", "4 months", "A quarter juz a day.", 120),
    PlanOption("p365", "One year", "", 365),
    PlanOption(CUSTOM, "Custom", ""),
)

private val surahOptions = listOf(
    PlanOption(SHORT_SURAHS, "Short surahs", "Juz 30, surah 78 to 114."),
    PlanOption(ALL_SURAHS, "One surah at a time", "All 114 surahs in order."),
)

/** First and last verse that fall on a range of mushaf pages. */
private fun QuranStore.versesOnPages(pages: IntRange): Pair<VerseRef, VerseRef>? {
    var first: VerseRef? = null
    var last: VerseRef? = null
    for (surah in surahs) {
        surah.pages.forEachIndexed { index, page ->
            if (page in pages) {
                val ref = VerseRef(surah.id, index + 1)
                if (first == null) first = ref
                last = ref
            }
        }
    }
    val f = first ?: return null
    val l = last ?: return null
    return f to l
}

/**
 * Read the Quran at her own pace. Choosing shows what each plan means,
 * a ring fills as she reads, and the days she has read fill in. Missed days are never counted against her.
 */
@Composable
fun ReadingPlanScreen() {
    val graph = LocalGraph.current
    val context = LocalContext.current
    val library = graph.library
    val history = graph.planHistory
    val plan = library.plan
    val haptics = rememberHaptics()
    var confirmEnd by remember { mutableStateOf(false) }
    var choice by rememberSaveable { mutableStateOf("p30") }
    var customDays by rememberSaveable { mutableIntStateOf(45) }

    LaunchedEffect(Unit) { history.sync(library.plan) }

    fun mark(day: Int, p: ReadingPlan) {
        val wasRead = day in p.finishedDays
        library.toggleFinished(day)
        history.sync(library.plan)
        if (!wasRead) haptics.success()
    }

    fun startChosen() {
        when (choice) {
            CUSTOM -> library.startPlan(customDays)
            SHORT_SURAHS -> library.startSurahPlan(78, 114)
            ALL_SURAHS -> library.startSurahPlan(1, 114)
            else -> library.startPlan(pageOptions.firstOrNull { it.key == choice }?.days ?: 30)
        }
    }

    AppScreen(title = "Reading Plan") {
        if (plan == null) {
            item(key = "choose") {
                ChoosePlan(
                    choice = choice,
                    customDays = customDays,
                    onChoose = { key ->
                        if (key != choice) {
                            choice = key
                            haptics.tick()
                        }
                    },
                    onCustomDays = { customDays = it },
                    onStart = ::startChosen,
                    modifier = Modifier.animateItem(),
                )
            }
        } else {
            val today = plan.dayIndex()
            item(key = "active") {
                ActivePlanCard(plan, today, onMark = { mark(it, plan) }, modifier = Modifier.animateItem())
            }
            if (plan.isComplete) {
                item(key = "complete") { PlanComplete(plan.coversWholeQuran, Modifier.animateItem()) }
            }
            item(key = "reminder") {
                GentleReminderSection(GentleReminder.plan, "Daily reminder", Modifier.animateItem().dealIn(1, baseDelay = 300))
            }
            item(key = "days") {
                Column(Modifier.animateItem(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    SectionHeader("All days", Modifier.focusIn(300))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        (0 until plan.days).chunked(7).forEach { week ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                week.forEach { day ->
                                    PlanDayCell(
                                        day = day,
                                        isRead = day in plan.finishedDays,
                                        isToday = day == today,
                                        // A diagonal ripple across the first weeks.
                                        modifier = Modifier
                                            .weight(1f)
                                            .dealIn(min(day / 7 + day % 7, 14), baseDelay = 350, step = 30),
                                    ) { mark(day, plan) }
                                }
                                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
            if (history.finished.isNotEmpty()) {
                item(key = "finished") {
                    FinishedPlans(history.finished, Modifier.animateItem().dealIn(2, baseDelay = 400))
                }
            }
            item(key = "end") {
                val danger = if (LocalDark.current) Color(0xFFFF6B6B) else Color(0xFFC62828)
                Box(Modifier.fillMaxWidth().animateItem(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .heightIn(min = 44.dp)
                            .pressable { confirmEnd = true }
                            .padding(horizontal = Space.m),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("End Plan", style = TextStyles.body, color = danger)
                    }
                }
            }
        }
    }

    if (confirmEnd) {
        AppAlert(
            title = "End this reading plan?",
            confirmLabel = "End Plan",
            onConfirm = {
                GentleReminders.turnOff(context, GentleReminder.plan)
                library.endPlan()
            },
            onDismiss = { confirmEnd = false },
            destructive = true,
        )
    }
}

// Choose

@Composable
private fun ChoosePlan(
    choice: String,
    customDays: Int,
    onChoose: (String) -> Unit,
    onCustomDays: (Int) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduce = LocalReduceMotion.current
    val history = LocalGraph.current.planHistory
    val selected = (pageOptions + surahOptions).firstOrNull { it.key == choice } ?: pageOptions[0]
    val bySurah = choice == SHORT_SURAHS || choice == ALL_SURAHS
    val days = when (choice) {
        CUSTOM -> customDays
        SHORT_SURAHS -> 114 - 78 + 1
        ALL_SURAHS -> 114
        else -> selected.days ?: 30
    }
    val title = if (choice == CUSTOM) "$customDays days" else selected.title

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.l)) {
        PlanPreview(days, title, selected.detail, bySurah, Modifier.dealIn(0, baseDelay = 100))

        GroupedSection(Modifier.dealIn(1, baseDelay = 100), title = "Whole Quran") {
            pageOptions.forEachIndexed { index, option ->
                if (index > 0) RowDivider(inset = Space.m)
                PlanChoiceRow(option, option.key == choice) { onChoose(option.key) }
            }
            AnimatedVisibility(
                visible = choice == CUSTOM,
                enter = if (reduce) fadeIn() else expandVertically(Motion.gentle()) + fadeIn(Motion.gentle()),
                exit = if (reduce) fadeOut() else shrinkVertically(Motion.gentle()) + fadeOut(Motion.gentle()),
            ) {
                Column {
                    RowDivider(inset = Space.m)
                    CustomDaysRows(customDays, onCustomDays)
                }
            }
        }

        GroupedSection(Modifier.dealIn(2, baseDelay = 100), title = "By surah") {
            surahOptions.forEachIndexed { index, option ->
                if (index > 0) RowDivider(inset = Space.m)
                PlanChoiceRow(option, option.key == choice) { onChoose(option.key) }
            }
        }

        PrimaryButton("Start Plan", onStart, Modifier.dealIn(3, baseDelay = 100))

        if (history.finished.isNotEmpty()) {
            FinishedPlans(history.finished, Modifier.dealIn(4, baseDelay = 100))
        }
    }
}

/** One plan in the list. The check springs in beside the one she picks. */
@Composable
private fun PlanChoiceRow(option: PlanOption, isOn: Boolean, onClick: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val p by animateFloatAsState(if (isOn) 1f else 0f, if (reduce) snap() else exploreSpring(0.38f, 0.7f), label = "check")
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .rowPress(onClick)
            .semantics(mergeDescendants = true) {
                selected = isOn
                role = Role.RadioButton
            }
            .padding(horizontal = Space.m, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            option.title,
            style = TextStyles.body.copy(fontWeight = if (isOn) FontWeight.SemiBold else FontWeight.Normal),
            color = Palette.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            symbol("checkmark"), null, tint = Palette.accent,
            modifier = Modifier.size(20.dp).graphicsLayer {
                val s = if (reduce) 1f else 0.4f + 0.6f * p
                scaleX = s; scaleY = s
                alpha = p.coerceIn(0f, 1f)
            },
        )
    }
}

/** Any number of days from a week to two years. The slider moves fast, the stepper fine tunes. */
@Composable
private fun CustomDaysRows(days: Int, onChange: (Int) -> Unit) {
    val haptics = rememberHaptics()
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = Space.m, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f).semantics(mergeDescendants = true) { }) {
                RollingNumber(days, style = TextStyles.body, color = Palette.textPrimary)
                Text(" days", style = TextStyles.body, color = Palette.textPrimary)
            }
            Stepper(days, onChange, 7..730)
        }
        Slider(
            value = days.toFloat(),
            onValueChange = {
                val next = it.roundToInt().coerceIn(7, 730)
                if (next != days) {
                    haptics.tick()
                    onChange(next)
                }
            },
            valueRange = 7f..730f,
            colors = SliderDefaults.colors(
                thumbColor = Palette.accentFill,
                activeTrackColor = Palette.accentFill,
                inactiveTrackColor = Palette.accentWash,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .padding(start = Space.m, end = Space.m, bottom = Space.xs)
                .semantics { contentDescription = "Days" },
        )
    }
}

/** What a plan means: pages (or a surah) a day, rolling as she changes it, and the day she would finish. */
@Composable
private fun PlanPreview(days: Int, title: String, detail: String, bySurah: Boolean, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val perDay = if (bySurah) 1 else max(1, (ReadingPlan.TOTAL_PAGES.toDouble() / days).roundToInt())
    val unit = when {
        bySurah -> "surah a day"
        perDay == 1 -> "page a day"
        else -> "pages a day"
    }
    val finish = LocalDate.now().plusDays(days - 1L)
    val finishText = "Finish by ${finish.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))}"
    val calendar = remember { Animatable(1f) }
    var seen by remember { mutableStateOf(days) }
    LaunchedEffect(days) {
        if (seen == days || reduce) return@LaunchedEffect
        seen = days
        calendar.snapTo(0.7f)
        calendar.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
    }

    CardSurface(modifier.semantics(mergeDescendants = true) { }, padding = Space.xl) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Crossfade(title, label = "title") { Eyebrow(it) }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                RollingNumber(
                    perDay,
                    style = TextStyle(fontSize = 56.sp, lineHeight = 60.sp, fontWeight = FontWeight.Light),
                    color = Palette.textPrimary,
                )
                Crossfade(unit, label = "unit") {
                    Text(
                        it,
                        style = TextStyles.headline,
                        color = Palette.textSecondary,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
            }
            AnimatedVisibility(detail.isNotEmpty()) {
                Crossfade(detail, label = "detail") {
                    Text(it, style = TextStyles.subheadline, color = Palette.textSecondary)
                }
            }
            Box(Modifier.padding(vertical = Space.xs)) { RowDivider(inset = 0.dp) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                Icon(
                    symbol("calendar"), null, tint = Palette.accent,
                    modifier = Modifier.size(18.dp).graphicsLayer { scaleX = calendar.value; scaleY = calendar.value },
                )
                Crossfade(finishText, label = "finish") {
                    Text(it, style = TextStyles.subheadline.copy(fontWeight = FontWeight.Medium), color = Palette.textPrimary)
                }
            }
        }
    }
}

/** Plans she has finished, with when each began and ended. */
@Composable
private fun FinishedPlans(plans: List<FinishedPlan>, modifier: Modifier = Modifier) {
    val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    fun day(millis: Long) = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().format(format)
    GroupedSection(modifier, title = "Finished plans") {
        plans.forEachIndexed { index, done ->
            if (index > 0) RowDivider()
            ListRow(
                title = done.title,
                icon = Icons.Outlined.CheckCircle,
                subtitle = "${day(done.start)} to ${day(done.finish)}",
                showsChevron = false,
            )
        }
    }
}

// Active

@Composable
private fun ActivePlanCard(plan: ReadingPlan, today: Int, onMark: (Int) -> Unit, modifier: Modifier = Modifier) {
    val graph = LocalGraph.current
    val router = LocalRouter.current
    val quran = graph.quran
    val pages = plan.pages(today)
    val range = remember(plan.days, plan.firstSurah, today) { if (plan.isSurahPlan) null else quran.versesOnPages(pages) }
    val fraction = plan.progress.toFloat()
    val percent = (fraction * 100).roundToInt()
    val todayRead = today in plan.finishedDays

    CardSurface(modifier, padding = Space.xl) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.l)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.l)) {
                PlanRing(fraction, today + 1, plan.days, !plan.isSurahPlan, Modifier.size(112.dp))
                Column(Modifier.weight(1f).focusIn(200), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Eyebrow("Today")
                    if (plan.isSurahPlan) {
                        val surah = quran.surah(plan.surah(today))
                        Text(
                            surah.name,
                            style = TextStyles.headline.copy(fontFamily = AppFont.serif),
                            color = Palette.textPrimary,
                        )
                        Text(
                            "Surah ${surah.id} · ${surah.ayahs.size} verses",
                            style = TextStyles.subheadline,
                            color = Palette.textSecondary,
                        )
                    } else {
                        if (range != null) {
                            Text(
                                "${quran.surah(range.first.surah).name} ${range.first.id} to ${quran.surah(range.second.surah).name} ${range.second.id}",
                                style = TextStyles.headline.copy(fontFamily = AppFont.serif),
                                color = Palette.textPrimary,
                            )
                        }
                        Text(
                            if (pages.count() == 1) "Page ${pages.first}" else "Pages ${pages.first} to ${pages.last}",
                            style = TextStyles.subheadline,
                            color = Palette.textSecondary,
                        )
                    }
                    AnimatedContent(percent, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "percent") { p ->
                        Text(
                            if (plan.isSurahPlan) "$p% of the plan read" else "$p% of the Quran read",
                            style = TextStyles.caption.copy(fontWeight = FontWeight.Medium),
                            color = Palette.accent,
                        )
                    }
                }
            }
            if (plan.isSurahPlan) {
                PrimaryButton("Read Today's Portion", onClick = { router.push(Route.Reader(plan.surah(today))) })
            } else if (range != null) {
                PrimaryButton("Read Today's Portion", onClick = {
                    router.push(Route.Reader(range.first.surah, range.first.ayah))
                })
            }
            MarkTodayButton(todayRead) { onMark(today) }
        }
    }
}

/** How much of the Quran she has read, drawn as a ring that fills from the top. */
@Composable
private fun PlanRing(fraction: Float, day: Int, days: Int, wholeQuran: Boolean, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    var drawn by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!drawn) {
            delay(250)
            drawn = true
        }
    }
    val target = if (drawn || reduce) max(fraction, 0.001f) else 0.001f
    val sweep by animateFloatAsState(target, if (reduce) snap() else exploreSpring(1.0f, 0.82f), label = "ring")
    val wash = Palette.accentWash
    val fill = Palette.accentFill
    val percent = (fraction * 100).roundToInt()
    Box(
        modifier.clearAndSetSemantics { contentDescription = "Day $day of $days. $percent percent of the ${if (wholeQuran) "Quran" else "plan"} read." },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2
            drawCircle(wash, radius = size.minDimension / 2 - inset, style = Stroke(stroke))
            drawArc(
                fill, startAngle = -90f, sweepAngle = 360f * sweep.coerceIn(0f, 1f), useCenter = false,
                topLeft = Offset(inset, inset), size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val style = TextStyles.title3.copy(fontFamily = AppFont.serif, fontWeight = FontWeight.SemiBold)
            Row {
                Text("Day ", style = style, color = Palette.textPrimary)
                RollingNumber(day, style = style, color = Palette.textPrimary)
            }
            Text("of $days", style = TextStyles.caption, color = Palette.textSecondary)
        }
    }
}

/** Marks today. The ring fills with a spring, the check pops in, and a few petals drift out. */
@Composable
private fun MarkTodayButton(read: Boolean, onClick: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val p by animateFloatAsState(if (read) 1f else 0f, if (reduce) snap() else exploreSpring(0.35f, 0.6f), label = "mark")
    var burst by remember { mutableIntStateOf(0) }
    var previous by remember { mutableStateOf(read) }
    LaunchedEffect(read) {
        if (read && !previous) burst++
        previous = read
    }
    val accent = Palette.accent
    val fill = Palette.accentFill
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clip(CircleShape)
            .background(Palette.accentWash)
            .pressable(scale = 0.98f, onClick = onClick)
            .semantics(mergeDescendants = true) { stateDescription = if (read) "Read" else "Not read" }
            .padding(horizontal = Space.l),
        horizontalArrangement = Arrangement.spacedBy(Space.s, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                val stroke = 2.dp.toPx()
                val outline = (1f - p).coerceIn(0f, 1f)
                if (outline > 0f) drawCircle(accent.copy(alpha = outline), radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
                val inner = p.coerceIn(0f, 1.2f)
                if (inner > 0.01f) drawCircle(fill.copy(alpha = p.coerceIn(0f, 1f)), radius = size.minDimension / 2 * inner)
            }
            Icon(
                symbol("checkmark"), null, tint = Palette.onAccentFill,
                modifier = Modifier.size(13.dp).graphicsLayer {
                    val s = 0.3f + 0.7f * p
                    scaleX = s; scaleY = s
                    alpha = p.coerceIn(0f, 1f)
                },
            )
            PetalBurst(burst)
        }
        Crossfade(read, label = "markText") {
            Text(if (it) "Read today" else "Mark today as read", style = TextStyles.headline, color = Palette.accent)
        }
    }
}

/** One day of the plan. Read days fill, today's tile breathes softly until it is read. */
@Composable
private fun PlanDayCell(day: Int, isRead: Boolean, isToday: Boolean, modifier: Modifier = Modifier, toggle: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val fillP by animateFloatAsState(if (isRead) 1f else 0f, if (reduce) snap() else exploreSpring(0.35f, 0.65f), label = "dayFill")
    val pop = remember { Animatable(1f) }
    var previous by remember { mutableStateOf(isRead) }
    LaunchedEffect(isRead) {
        if (previous == isRead) return@LaunchedEffect
        previous = isRead
        if (reduce) return@LaunchedEffect
        pop.animateTo(0.86f, tween(100))
        pop.animateTo(1.1f, tween(180, easing = FastOutSlowInEasing))
        pop.animateTo(1f, spring(dampingRatio = 1f, stiffness = 300f))
    }
    val breathe = if (isToday && !isRead && !reduce) {
        val t = rememberInfiniteTransition(label = "today")
        val a by t.animateFloat(0.2f, 0.9f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
        a
    } else 0f
    val shape = RoundedCornerShape(9.dp)
    val fill = Palette.accentFill
    val base = if (isToday) Palette.blush else Palette.surface
    val ring = Palette.blushStrong
    Box(
        modifier
            .height(36.dp)
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .clip(shape)
            .background(base)
            .drawBehind {
                if (fillP > 0.001f) {
                    val s = 0.2f + 0.8f * fillP
                    val w = size.width * s
                    val h = size.height * s
                    drawRoundRect(
                        fill.copy(alpha = fillP.coerceIn(0f, 1f)),
                        topLeft = Offset((size.width - w) / 2, (size.height - h) / 2),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(9.dp.toPx() * s),
                    )
                }
            }
            .then(if (breathe > 0f) Modifier.border(1.5.dp, ring.copy(alpha = breathe), shape) else Modifier)
            .pressable(scale = 0.97f, onClick = toggle)
            .semantics {
                contentDescription = if (isToday) "Day ${day + 1}, today" else "Day ${day + 1}"
                stateDescription = if (isRead) "Read" else "Not read"
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${day + 1}",
            style = TextStyles.caption.copy(fontWeight = FontWeight.Medium),
            color = lerp(Palette.textPrimary, Palette.onAccentFill, fillP.coerceIn(0f, 1f)),
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/** When the whole Quran is read: a lotus draws itself in, with a few gold sparks. */
@Composable
private fun PlanComplete(wholeQuran: Boolean, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    var drawn by rememberSaveable { mutableStateOf(false) }
    var fire by remember { mutableIntStateOf(0) }
    val draw by animateFloatAsState(
        if (drawn || reduce) 1f else 0f,
        if (reduce) snap() else tween(1400, easing = FastOutSlowInEasing),
        label = "lotus",
    )
    LaunchedEffect(Unit) {
        delay(200)
        drawn = true
        delay(700)
        fire++
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(Brush.linearGradient(listOf(Color(0xFFC9A24A).copy(alpha = 0.16f), Palette.accentWash.copy(alpha = 0.6f))))
            .semantics(mergeDescendants = true) { }
            .padding(Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Box(Modifier.height(70.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            ExploreSparks(fire, from = 10.dp, to = 64.dp)
            ExploreDrawnLotus(draw, Palette.accent, 1.8.dp, Modifier.size(64.dp, 46.dp))
        }
        Text(
            if (wholeQuran) "You have read the whole Quran. May Allah accept it from you." else "You finished this plan.",
            style = TextStyles.body.copy(fontFamily = AppFont.serif, fontWeight = FontWeight.SemiBold),
            color = Palette.accent,
            textAlign = TextAlign.Center,
        )
    }
}
