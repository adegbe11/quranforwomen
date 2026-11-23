package com.quranforwomen.app.feature.ramadan

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.FastKind
import com.quranforwomen.app.data.Hijri
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.AppSheet
import com.quranforwomen.app.ui.components.ChoiceRow
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.ListRow
import com.quranforwomen.app.ui.components.PetalBurst
import com.quranforwomen.app.ui.components.RollingNumber
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.Stepper
import com.quranforwomen.app.ui.components.TextAction
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.scrollSettle
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Radius
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.abs
import java.time.format.TextStyle as DateTextStyle

internal val CalendarGold = Color(0xFFC9A24A)

internal fun FastKind.symbolName(): String = when (this) {
    FastKind.ramadan -> "moon.stars"
    FastKind.voluntary -> "sparkles"
    FastKind.makeUp -> "arrow.counterclockwise"
}

private fun hijriTitle(index: Int) = "${Hijri.monthName(Hijri.monthOf(index))} ${Hijri.yearOf(index)}"

/** "Feb · Mar 2027", the Gregorian months a Hijri month covers. */
private fun gregorianRange(index: Int): String {
    val start = Hijri.start(index)
    val end = start.plusDays(Hijri.length(index) - 1L)
    val locale = Locale.getDefault()
    val m1 = start.month.getDisplayName(DateTextStyle.SHORT, locale)
    val m2 = end.month.getDisplayName(DateTextStyle.SHORT, locale)
    return when {
        start.year != end.year -> "$m1 ${start.year} · $m2 ${end.year}"
        start.month == end.month -> "$m1 ${start.year}"
        else -> "$m1 · $m2 ${end.year}"
    }
}

private data class CalendarSpecial(val title: String, val hijriDay: Int, val date: LocalDate)

/** This month's widely known days. The last ten nights are listed once, where they begin. */
private fun specialDays(index: Int): List<CalendarSpecial> {
    val month = Hijri.monthOf(index)
    val start = Hijri.start(index)
    return (1..Hijri.length(index)).mapNotNull { d ->
        val title = Hijri.specialDay(month, d) ?: return@mapNotNull null
        if (month == Hijri.RAMADAN && d > 21) null else CalendarSpecial(title, d, start.plusDays(d - 1L))
    }
}

private fun calendarSlide(forward: Boolean, reduce: Boolean): ContentTransform =
    if (reduce) {
        fadeIn(tween(150)) togetherWith fadeOut(tween(150))
    } else {
        (slideInHorizontally(spring(dampingRatio = 0.86f, stiffness = 380f)) { if (forward) it else -it } + fadeIn()) togetherWith
            (slideOutHorizontally(spring(dampingRatio = 0.86f, stiffness = 380f)) { if (forward) -it else it } + fadeOut())
    }

/**
 * The Islamic calendar: Hijri months with their Gregorian dates, the widely known days marked,
 * and her fasts. Swipe or use the arrows to move between months. Tap a day to mark a fast.
 */
@Composable
fun IslamicCalendarScreen() {
    val graph = LocalGraph.current
    val haptics = rememberHaptics()
    val today = remember { LocalDate.now() }
    val todayH = remember(today) { Hijri.of(today) }
    val current = Hijri.index(todayH.year, todayH.month)

    var index by rememberSaveable { mutableIntStateOf(current) }
    var forward by rememberSaveable { mutableStateOf(true) }
    var picked by remember { mutableStateOf<LocalDate?>(null) }
    val specials = remember(index) { specialDays(index) }

    fun shift(by: Int) {
        forward = by > 0
        index += by
        haptics.tick()
    }

    AppScreen(
        title = "Islamic Calendar",
        largeTitle = false,
        actions = {
            if (index != current) {
                TextAction("Today", onClick = {
                    forward = current > index
                    index = current
                    haptics.tick()
                })
            }
        },
    ) {
        item(key = "month") {
            CalendarMonthCard(
                index = index,
                forward = forward,
                today = today,
                onShift = { shift(it) },
                onPick = { picked = it },
                modifier = Modifier.dealIn(0, baseDelay = 20).scrollSettle(),
            )
        }
        if (specials.isNotEmpty()) {
            item(key = "days") {
                GroupedSection(Modifier.dealIn(1, baseDelay = 250).scrollSettle(), title = "This month") {
                    specials.forEachIndexed { i, special ->
                        if (i > 0) RowDivider(inset = 40.dp)
                        CalendarSpecialRow(special, Hijri.monthName(Hijri.monthOf(index)))
                    }
                }
            }
        }
        item(key = "fasting") {
            CalendarFastingSection(todayH.year, today.year, Modifier.dealIn(2, baseDelay = 250).scrollSettle())
        }
        item(key = "note") {
            Text(
                "Dates can differ by a day where you live.",
                style = TextStyles.footnote, color = Palette.textSecondary,
                modifier = Modifier.padding(horizontal = Space.m).focusIn(500),
            )
        }
    }

    picked?.let { day -> CalendarFastSheet(day, onDismiss = { picked = null }) }
}

@Composable
private fun CalendarMonthCard(
    index: Int,
    forward: Boolean,
    today: LocalDate,
    onShift: (Int) -> Unit,
    onPick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduce = LocalReduceMotion.current
    val threshold = with(LocalDensity.current) { 56.dp.toPx() }
    val shiftLatest by rememberUpdatedState(onShift)
    val locale = Locale.getDefault()
    val firstDay = remember(locale) { WeekFields.of(locale).firstDayOfWeek }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.tile))
            .background(Palette.surface)
            // Swipe the card to move between months. Vertical scrolling is left alone.
            .pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (abs(total) > threshold) shiftLatest(if (total < 0) 1 else -1) },
                    onDragCancel = { total = 0f },
                    onHorizontalDrag = { _, amount -> total += amount },
                )
            }
            .padding(vertical = Space.s),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.xs), verticalAlignment = Alignment.CenterVertically) {
            CalendarArrow(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous month") { onShift(-1) }
            AnimatedContent(
                targetState = index,
                transitionSpec = { calendarSlide(forward, reduce) },
                modifier = Modifier.weight(1f).clipToBounds(),
                label = "monthTitle",
            ) { i ->
                Column(
                    Modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(hijriTitle(i), style = TextStyles.headline, color = Palette.textPrimary)
                    Text(gregorianRange(i), style = TextStyles.footnote, color = Palette.textSecondary)
                }
            }
            CalendarArrow(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next month") { onShift(1) }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = Space.s).clearAndSetSemantics {}) {
            for (i in 0 until 7) {
                Text(
                    firstDay.plus(i.toLong()).getDisplayName(DateTextStyle.NARROW, locale),
                    style = TextStyles.caption, color = Palette.textSecondary, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        AnimatedContent(
            targetState = index,
            transitionSpec = { calendarSlide(forward, reduce) },
            modifier = Modifier.fillMaxWidth().clipToBounds(),
            label = "monthGrid",
        ) { i ->
            CalendarMonthGrid(i, today, firstDay, onPick)
        }
    }
}

@Composable
private fun CalendarArrow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .pressable(scale = 0.9f, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Palette.accent, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun CalendarMonthGrid(index: Int, today: LocalDate, firstDay: DayOfWeek, onPick: (LocalDate) -> Unit) {
    val start = remember(index) { Hijri.start(index) }
    val length = remember(index) { Hijri.length(index) }
    val month = Hijri.monthOf(index)
    val lead = (start.dayOfWeek.value - firstDay.value + 7) % 7
    val rows = (lead + length + 6) / 7
    Column(Modifier.fillMaxWidth().padding(horizontal = Space.s)) {
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val n = r * 7 + c - lead
                    Box(Modifier.weight(1f).height(54.dp), contentAlignment = Alignment.Center) {
                        if (n in 0 until length) {
                            CalendarDayCell(
                                date = start.plusDays(n.toLong()),
                                hijriDay = n + 1,
                                month = month,
                                today = today,
                                index = r * 7 + c,
                                onPick = onPick,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * One day: the Hijri number, the Gregorian number under it, a gold dot on a widely known day,
 * and her fast as a filled circle (Ramadan), a ring (voluntary) or a soft wash (making up).
 * Rest days carry a soft blush. Days without a fast stay plain.
 */
@Composable
private fun CalendarDayCell(
    date: LocalDate,
    hijriDay: Int,
    month: Int,
    today: LocalDate,
    index: Int,
    onPick: (LocalDate) -> Unit,
) {
    val graph = LocalGraph.current
    val kind = graph.fasting.kind(date)
    val restSince = graph.library.restSince
    val resting = restSince?.let {
        val from = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
        !date.isBefore(from) && !date.isAfter(today)
    } ?: false
    val special = Hijri.specialDay(month, hijriDay)
    val isToday = date == today
    val canMark = !date.isAfter(today)

    // Keeps the last mark drawn while it fades out.
    var drawn by remember { mutableStateOf(kind) }
    var burst by remember { mutableIntStateOf(0) }
    var previous by remember { mutableStateOf(kind) }
    LaunchedEffect(kind) {
        if (kind != null) drawn = kind
        if (kind != null && kind != previous) burst++
        previous = kind
    }
    val pop by animateFloatAsState(
        if (kind != null) 1f else 0f,
        spring(dampingRatio = 0.55f, stiffness = 500f),
        label = "fastMark",
    )

    val fill = Palette.accentFill
    val accent = Palette.accent
    val wash = Palette.accentWash
    val shape = RoundedCornerShape(12.dp)
    val dateText = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault()))
    val label = listOfNotNull(
        "$hijriDay ${Hijri.monthName(month)}, $dateText",
        kind?.title,
        special,
        if (isToday) "Today" else null,
        if (resting) "Rest days" else null,
    ).joinToString(". ")

    Box(
        Modifier
            .size(48.dp)
            .dealIn(index, baseDelay = 120, step = 8)
            .clip(shape)
            .then(if (resting) Modifier.background(Palette.blush.copy(alpha = 0.6f)) else Modifier)
            .then(if (isToday) Modifier.border(1.5.dp, accent, shape) else Modifier)
            .then(if (canMark) Modifier.pressable(scale = 0.9f, onClickLabel = "Mark a fast") { onPick(date) } else Modifier)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            Modifier.size(40.dp).graphicsLayer {
                val s = 0.6f + 0.4f * pop
                scaleX = s; scaleY = s
                alpha = pop.coerceIn(0f, 1f)
            },
        ) {
            val r = size.minDimension / 2
            when (drawn) {
                FastKind.ramadan -> drawCircle(fill, r)
                FastKind.voluntary -> drawCircle(accent, r - 1.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                FastKind.makeUp -> drawCircle(wash, r)
                null -> Unit
            }
        }
        val onFill = kind == FastKind.ramadan
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$hijriDay",
                style = TextStyles.callout.copy(fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium, lineHeight = 18.sp),
                color = when {
                    onFill -> Palette.onAccentFill
                    isToday -> accent
                    else -> Palette.textPrimary
                },
            )
            Text(
                "${date.dayOfMonth}",
                style = TextStyles.caption.copy(fontSize = 10.sp, lineHeight = 12.sp),
                color = if (onFill) Palette.onAccentFill.copy(alpha = 0.8f) else Palette.textSecondary,
            )
        }
        if (special != null) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(CalendarGold),
            )
        }
        PetalBurst(trigger = burst, count = 6)
    }
}

@Composable
private fun CalendarSpecialRow(special: CalendarSpecial, monthName: String) {
    val format = remember { DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()) }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = Space.m, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(CalendarGold))
        Spacer(Modifier.width(Space.m))
        Column(Modifier.weight(1f)) {
            Text(special.title, style = TextStyles.body, color = Palette.textPrimary)
            Text(
                "${special.hijriDay} $monthName · ${special.date.format(format)}",
                style = TextStyles.subheadline, color = Palette.textSecondary,
            )
        }
    }
}

@Composable
private fun CalendarFastingSection(hijriYear: Int, year: Int, modifier: Modifier = Modifier) {
    val fasting = LocalGraph.current.fasting
    GroupedSection(modifier, title = "Fasting") {
        ListRow(
            title = "This Ramadan",
            icon = symbol("moon.stars"),
            showsChevron = false,
            landDelayMillis = 350,
            trailing = { RollingNumber(fasting.ramadanCount(hijriYear), TextStyles.body, Palette.textSecondary) },
        )
        RowDivider()
        ListRow(
            title = "Voluntary this year",
            icon = symbol("sparkles"),
            showsChevron = false,
            landDelayMillis = 410,
            trailing = { RollingNumber(fasting.voluntaryCount(year), TextStyles.body, Palette.textSecondary) },
        )
        RowDivider()
        ListRow(
            title = "Fasts to make up",
            icon = symbol("arrow.counterclockwise"),
            showsChevron = false,
            landDelayMillis = 470,
            trailing = { Stepper(fasting.makeUpLeft, { fasting.setMakeUpLeft(it) }, 0..999) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarFastSheet(date: LocalDate, onDismiss: () -> Unit) {
    val fasting = LocalGraph.current.fasting
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val h = remember(date) { Hijri.of(date) }
    val current = fasting.kind(date)
    val options = buildList {
        if (h.month == Hijri.RAMADAN) add(FastKind.ramadan)
        add(FastKind.voluntary)
        add(FastKind.makeUp)
    }

    fun choose(kind: FastKind?) {
        fasting.setFast(date, kind)
        if (kind != null && kind != current) haptics.success()
        scope.launch { state.hide() }.invokeOnCompletion { onDismiss() }
    }

    AppSheet(onDismiss = onDismiss, sheetState = state) {
        Column(Modifier.padding(horizontal = Space.page).padding(top = Space.s, bottom = Space.m)) {
            Text(
                date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault())),
                style = TextStyles.title3, color = Palette.textPrimary,
                modifier = Modifier.semantics { heading() }.focusIn(40),
            )
            Text(
                "${h.day} ${Hijri.monthName(h.month)} ${h.year}",
                style = TextStyles.subheadline, color = Palette.textSecondary,
                modifier = Modifier.focusIn(100),
            )
        }
        GroupedSection(Modifier.padding(horizontal = Space.page)) {
            options.forEachIndexed { i, kind ->
                if (i > 0) RowDivider()
                ChoiceRow(
                    title = kind.title,
                    selected = current == kind,
                    onClick = { choose(kind) },
                    icon = symbol(kind.symbolName()),
                    modifier = Modifier.dealIn(i, baseDelay = 80, step = 50),
                )
            }
            RowDivider()
            ChoiceRow(
                title = "No fast",
                selected = current == null,
                onClick = { choose(null) },
                icon = symbol("circle.dotted"),
                modifier = Modifier.dealIn(options.size, baseDelay = 80, step = 50),
            )
        }
    }
}
