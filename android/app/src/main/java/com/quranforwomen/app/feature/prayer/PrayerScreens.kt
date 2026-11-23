package com.quranforwomen.app.feature.prayer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.Prayer
import com.quranforwomen.app.data.PrayerSchedule
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.AppSwitch
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.ListRow
import com.quranforwomen.app.ui.components.PetalBurst
import com.quranforwomen.app.ui.components.RollingNumber
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.scrollSettle
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Radius
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs

// Prayer times

/**
 * Today's prayers on a thread under a live sky. The sky matches the real hour,
 * the countdown ticks to the next prayer, and each prayer she marks answers her.
 */
@Composable
fun PrayerTimesScreen() {
    val graph = LocalGraph.current
    val prayer = graph.prayer
    val router = LocalRouter.current

    var day by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var forward by rememberSaveable { mutableStateOf(true) }
    val schedule = if (prayer.hasLocation) prayer.schedule(day) else null

    AppScreen(title = "Prayer Times", largeTitle = false) {
        item(key = "hero") {
            PrayerHero(Modifier.dealIn(0, baseDelay = 20).scrollSettle())
        }
        if (schedule != null) {
            item(key = "day") {
                PrayerDaySection(
                    day = day,
                    forward = forward,
                    onShift = { days -> forward = days > 0; day = day.plusDays(days.toLong()) },
                    modifier = Modifier.scrollSettle(),
                )
            }
            item(key = "links") {
                GroupedSection(Modifier.dealIn(1, baseDelay = 450).scrollSettle()) {
                    ListRow(
                        title = "Qibla direction",
                        icon = symbol("location.north.line"),
                        onClick = { router.push(Route.Qibla) },
                    )
                    RowDivider()
                    ListRow(
                        title = "Prayer settings",
                        icon = symbol("slider.horizontal.3"),
                        value = if (prayer.notifyAtPrayerTimes) "Reminders on" else null,
                        onClick = { router.push(Route.PrayerSettings) },
                    )
                    RowDivider()
                    ListRow(
                        title = "Islamic calendar",
                        icon = symbol("calendar"),
                        onClick = { router.push(Route.IslamicCalendar) },
                    )
                    RowDivider()
                    ListRow(
                        title = "Make-up prayers",
                        icon = symbol("arrow.counterclockwise"),
                        onClick = { router.push(Route.MissedPrayers) },
                    )
                }
            }
        } else {
            item(key = "setup") {
                PrayerLocationSetup(
                    Modifier
                        .dealIn(1, baseDelay = 250)
                        .scrollSettle()
                        .clip(RoundedCornerShape(Radius.tile))
                        .background(Palette.surface)
                        .padding(Space.m)
                )
            }
        }
        item(key = "rest") {
            PrayerRestDays(Modifier.dealIn(2, baseDelay = 450).scrollSettle())
        }
    }
}

@Composable
fun QiblaScreen() {
    PrayerQiblaContent()
}

@Composable
fun PrayerSettingsScreen() {
    PrayerSettingsContent()
}

// Live sky

/** A window of sky for the real hour, the sun or moon on its arc, and a countdown that ticks each second. */
@Composable
private fun PrayerHero(modifier: Modifier = Modifier) {
    val graph = LocalGraph.current
    val prayer = graph.prayer
    val library = graph.library
    val reduce = LocalReduceMotion.current
    val clock = rememberPrayerClock()

    val now by produceState(ZonedDateTime.now()) {
        while (true) {
            delay(1000 - System.currentTimeMillis() % 1000)
            value = ZonedDateTime.now()
        }
    }
    val schedule = prayer.schedule(now.toLocalDate())
    val sky = PrayerSkyState(now, schedule)
    val ink = if (sky.isDark) Color.White else Palette.ink
    val next = if (library.isResting) null else prayer.nextPrayer(now)

    // The sun or moon starts on the horizon and rises to its real place on first appearance.
    var risen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (!risen) { delay(200); risen = true } }
    val arc by animateFloatAsState(if (risen || reduce) sky.progress else 0f, prayerSpring(1.4f, 0.9f), label = "arc")

    // A soft light washes over the sky the moment a new prayer time begins.
    val flash = remember { Animatable(0f) }
    PrayerOnChange(next?.first) {
        if (!reduce) {
            flash.animateTo(0.35f, tween(400, easing = EaseOut))
            flash.animateTo(0f, tween(1400))
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(Radius.card))
    ) {
        PrayerSkyBackground(sky.palette)
        PrayerCelestialArc(
            progress = arc, isNight = sky.isNight, ink = ink,
            modifier = Modifier.fillMaxWidth().height(250.dp).padding(top = 96.dp).clearAndSetSemantics { },
        )
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = flash.value }.background(Color.White))

        val description = next?.let { "Next prayer, ${it.first.title} at ${clock.format(it.second)}, in ${prayerRelative(now, it.second)}" } ?: ""
        Column(
            Modifier
                .fillMaxSize()
                .padding(Space.l)
                .then(if (next != null) Modifier.clearAndSetSemantics { contentDescription = description } else Modifier),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            when {
                library.isResting -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        Icon(symbol("moon.zzz"), null, tint = ink, modifier = Modifier.size(28.dp))
                        Text(
                            "Rest days",
                            style = TextStyle28Serif, color = ink,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
                next != null -> {
                    Text(
                        "Next prayer",
                        style = TextStyles.subheadline.copy(fontWeight = FontWeight.Medium),
                        color = ink.copy(alpha = 0.75f),
                        modifier = Modifier.focusIn(150),
                    )
                    AnimatedContent(
                        targetState = next.first,
                        transitionSpec = {
                            if (reduce) fadeIn() togetherWith fadeOut()
                            else (slideInVertically(prayerSpring(0.6f, 0.8f)) { it } + fadeIn()) togetherWith
                                (slideOutVertically(prayerSpring(0.6f, 0.8f)) { -it } + fadeOut())
                        },
                        modifier = Modifier.focusIn(250),
                        label = "nextName",
                    ) { p ->
                        Text(
                            p.title,
                            style = androidx.compose.ui.text.TextStyle(fontFamily = AppFont.serif, fontSize = 40.sp, lineHeight = 46.sp, fontWeight = FontWeight.SemiBold),
                            color = ink,
                        )
                    }
                    Text(
                        "at ${clock.format(next.second)}",
                        style = TextStyles.subheadline,
                        color = ink.copy(alpha = 0.8f),
                        modifier = Modifier.focusIn(350),
                    )
                    Spacer(Modifier.weight(1f))
                    PrayerRollingText(
                        text = prayerCountdown(now, next.second),
                        style = androidx.compose.ui.text.TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Light).prayerDigits(),
                        color = ink,
                        countsDown = true,
                        modifier = Modifier.focusIn(450),
                    )
                    PrayerProgressBar(
                        now = now, next = next.second, schedule = schedule, ink = ink,
                        modifier = Modifier.padding(top = 6.dp).focusIn(500),
                    )
                }
                else -> {
                    Text(
                        "Prayer times",
                        style = androidx.compose.ui.text.TextStyle(fontFamily = AppFont.serif, fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold),
                        color = ink,
                    )
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private val TextStyle28Serif = androidx.compose.ui.text.TextStyle(
    fontFamily = AppFont.serif, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold,
)

/** How far she is from the last prayer to the next one. */
@Composable
private fun PrayerProgressBar(now: ZonedDateTime, next: ZonedDateTime, schedule: PrayerSchedule?, ink: Color, modifier: Modifier = Modifier) {
    val prayer = LocalGraph.current.prayer
    val previous = Prayer.prayers.mapNotNull { schedule?.time(it) }.filter { !it.isAfter(now) }.maxOrNull()
        ?: prayer.schedule(now.toLocalDate().minusDays(1))?.time(Prayer.isha)
    val span = previous?.let { Duration.between(it, next).seconds.toFloat() } ?: 0f
    val target = if (span > 0 && previous != null) (Duration.between(previous, now).seconds / span).coerceIn(0f, 1f) else 0f
    val fraction by animateFloatAsState(target, tween(1000, easing = LinearEasing), label = "progress")
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(ink.copy(alpha = 0.18f))
            .clearAndSetSemantics { }
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(maxOf(4.dp, maxWidth * fraction))
                .clip(CircleShape)
                .background(ink.copy(alpha = 0.85f))
        )
    }
}

private fun prayerCountdown(now: ZonedDateTime, target: ZonedDateTime): String {
    val total = maxOf(0L, Duration.between(now, target).seconds)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
}

// Day

private enum class NodeState { Past, Next, Upcoming }

private fun dayTitle(day: LocalDate): String {
    val today = LocalDate.now()
    return when (day) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        today.plusDays(1) -> "Tomorrow"
        else -> day.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
    }
}

@Composable
private fun PrayerDaySection(day: LocalDate, forward: Boolean, onShift: (Int) -> Unit, modifier: Modifier = Modifier) {
    val graph = LocalGraph.current
    val prayer = graph.prayer
    val library = graph.library
    val reduce = LocalReduceMotion.current
    val haptics = rememberHaptics()
    val density = LocalDensity.current

    // Refreshes the thread as prayer times pass.
    val now by produceState(ZonedDateTime.now()) {
        while (true) { delay(30_000); value = ZonedDateTime.now() }
    }
    val today = now.toLocalDate()
    val canMark = !library.isResting && !day.isAfter(today)
    val prayedCount = Prayer.prayers.count { library.prayed(it.name, day) }

    fun shift(days: Int) {
        haptics.tick()
        onShift(days)
    }
    // The swipe gesture outlives recompositions, so it always calls the latest shift.
    val latestShift by androidx.compose.runtime.rememberUpdatedState<(Int) -> Unit> { shift(it) }

    fun mark(item: Prayer, on: LocalDate) {
        val wasPrayed = library.prayed(item.name, on)
        library.togglePrayed(item.name, on)
        if (wasPrayed) return
        val count = Prayer.prayers.count { library.prayed(it.name, on) }
        if (count == 5) haptics.success() else haptics.soft()
    }

    val slide: androidx.compose.animation.AnimatedContentTransitionScope<LocalDate>.() -> androidx.compose.animation.ContentTransform = {
        if (reduce) fadeIn(tween(200)) togetherWith fadeOut(tween(200))
        else {
            val dir = if (forward) 1 else -1
            (slideInHorizontally(prayerSpring(0.45f, 0.86f)) { it * dir } + fadeIn()) togetherWith
                (slideOutHorizontally(prayerSpring(0.45f, 0.86f)) { -it * dir } + fadeOut())
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            // Swipe the card to move between days. Vertical scrolling is left alone.
            .pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        if (abs(total) > with(density) { 30.dp.toPx() }) latestShift(if (total < 0) 1 else -1)
                        total = 0f
                    },
                    onDragCancel = { total = 0f },
                ) { change, dx -> total += dx; change.consume() }
            },
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Row(Modifier.fillMaxWidth().focusIn(150), verticalAlignment = Alignment.CenterVertically) {
            PrayerDayArrow(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Previous day") { shift(-1) }
            Spacer(Modifier.weight(1f))
            AnimatedContent(day, transitionSpec = slide, modifier = Modifier.clipToBounds(), label = "dayTitle") { d ->
                Text(dayTitle(d), style = TextStyles.headline, color = Palette.accent, modifier = Modifier.semantics { heading() })
            }
            Spacer(Modifier.weight(1f))
            PrayerDayArrow(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next day") { shift(1) }
        }

        AnimatedContent(
            day,
            transitionSpec = slide,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.tile))
                .background(Palette.surface),
            label = "dayCard",
        ) { d ->
            val schedule = prayer.schedule(d)
            val isToday = d == today
            val dayCanMark = !library.isResting && !d.isAfter(today)
            val count = Prayer.prayers.count { library.prayed(it.name, d) }
            val next = if (isToday) schedule?.next(now)?.first else null
            val items = Prayer.entries
            Column(Modifier.fillMaxWidth().animateContentSize(if (reduce) tween(0) else spring(dampingRatio = 0.68f, stiffness = 190f))) {
                items.forEachIndexed { index, item ->
                    val time = schedule?.time(item)
                    val state = when {
                        item == next -> NodeState.Next
                        time != null && !time.isAfter(now) -> NodeState.Past
                        else -> NodeState.Upcoming
                    }
                    PrayerTimelineRow(
                        prayer = item,
                        time = time,
                        state = state,
                        isFirst = index == 0,
                        isLast = index == items.size - 1,
                        isPrayed = library.prayed(item.name, d),
                        showsCheck = item.isPrayer && dayCanMark,
                        index = index,
                    ) { mark(item, d) }
                }
                AnimatedVisibility(
                    visible = dayCanMark && count == 5,
                    enter = if (reduce) fadeIn() else expandVertically(spring(dampingRatio = 0.68f, stiffness = 190f)) + slideInVertically { it } + fadeIn(),
                    exit = if (reduce) fadeOut() else shrinkVertically() + fadeOut(),
                ) { PrayerAllFiveBanner() }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${prayer.placeName ?: "Your location"} · ${prayer.calculationMethod.title}",
                style = TextStyles.footnote, color = Palette.textSecondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (canMark) {
                Row(
                    Modifier.padding(start = Space.xs).semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RollingNumber(prayedCount, TextStyles.footnote.prayerDigits(), Palette.textSecondary)
                    Text(" of 5 prayed", style = TextStyles.footnote, color = Palette.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun PrayerDayArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .pressable(scale = 0.9f, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Palette.accent, modifier = Modifier.size(28.dp))
    }
}

// Timeline

/**
 * One prayer on the day's thread. The thread draws itself down the card,
 * the next prayer's node breathes, and her check answers with a small burst.
 */
@Composable
private fun PrayerTimelineRow(
    prayer: Prayer,
    time: ZonedDateTime?,
    state: NodeState,
    isFirst: Boolean,
    isLast: Boolean,
    isPrayed: Boolean,
    showsCheck: Boolean,
    index: Int,
    toggle: () -> Unit,
) {
    val reduce = LocalReduceMotion.current
    val clock = rememberPrayerClock()
    val drawn = remember { Animatable(if (reduce) 1f else 0f) }
    val land = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        launch {
            delay(250L + index * 70L)
            drawn.animateTo(1f, tween(300, easing = LinearOutSlowInEasing))
        }
        delay(350L + index * 70L)
        land.snapTo(0.75f)
        land.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f))
    }

    val breathing = state == NodeState.Next && !reduce
    val ring = if (breathing) {
        val t = rememberInfiniteTransition(label = "breath")
        t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = EaseOut), RepeatMode.Restart), label = "ring").value
    } else 0f

    val fill = Palette.accentFill
    val border = Palette.border
    val surface = Palette.surface
    val accent = Palette.accent
    val topColor = if (state == NodeState.Upcoming) border else fill
    val bottomColor = if (state == NodeState.Past) fill else border

    Row(
        Modifier
            .fillMaxWidth()
            .dealIn(index, baseDelay = 200, step = 50)
            .heightIn(min = 56.dp)
            .background(if (state == NodeState.Next) Palette.accentWash.copy(alpha = 0.45f) else Color.Transparent)
            .drawBehind {
                val x = Space.m.toPx() + 7.dp.toPx()
                val h = size.height
                val s = drawn.value
                val mid = h / 2
                val w = 2.dp.toPx()
                if (!isFirst) drawLine(topColor, Offset(x, 0f), Offset(x, mid * s), w)
                if (!isLast) drawLine(bottomColor, Offset(x, mid * s), Offset(x, h * s), w)
                val c = Offset(x, mid)
                when (state) {
                    NodeState.Past -> drawCircle(fill, 5.dp.toPx(), c)
                    NodeState.Next -> {
                        if (breathing) {
                            val scale = 1f + 1.4f * ring
                            drawCircle(accent.copy(alpha = 0.8f * (1f - ring)), 6.dp.toPx() * scale, c, style = Stroke(1.5.dp.toPx()))
                        }
                        drawCircle(fill, 6.dp.toPx(), c)
                    }
                    NodeState.Upcoming -> {
                        drawCircle(surface, 5.dp.toPx(), c)
                        drawCircle(border, 4.dp.toPx(), c, style = Stroke(2.dp.toPx()))
                    }
                }
            }
            .padding(start = Space.m, end = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Spacer(Modifier.width(14.dp))
        Icon(
            prayer.prayerTimeIcon, null,
            tint = if (prayer.isPrayer) Palette.accent else Palette.textSecondary,
            modifier = Modifier.size(22.dp).graphicsLayer { scaleX = land.value; scaleY = land.value },
        )
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            Text(
                prayer.title,
                style = if (state == NodeState.Next) TextStyles.headline else TextStyles.body,
                color = if (prayer.isPrayer) Palette.textPrimary else Palette.textSecondary,
                maxLines = 1,
            )
            AnimatedVisibility(
                visible = state == NodeState.Next,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                Text(
                    "Next",
                    style = TextStyles.caption.copy(fontWeight = FontWeight.SemiBold),
                    color = Palette.accent,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Palette.accentWash)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        Text(
            time?.let { clock.format(it) } ?: "--",
            style = TextStyles.body.prayerDigits(),
            color = if (state == NodeState.Next) Palette.accent else Palette.textPrimary,
            maxLines = 1,
        )
        AnimatedVisibility(visible = showsCheck, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            PrayerCheck(prayer.title, isPrayed, toggle)
        }
        if (!showsCheck) Spacer(Modifier.width(8.dp))
    }
}

/** The prayed check: it presses in, fills with a soft overshoot, and sends out a few petals. */
@Composable
private fun PrayerCheck(name: String, isPrayed: Boolean, toggle: () -> Unit) {
    val p by animateFloatAsState(if (isPrayed) 1f else 0f, prayerSpring(0.35f, 0.6f), label = "prayed")
    var burst by remember { mutableIntStateOf(0) }
    PrayerOnChange(isPrayed) { if (isPrayed) burst++ }
    val border = Palette.border
    val fill = Palette.accentFill
    Box(
        Modifier
            .size(44.dp)
            .pressable(scale = 0.9f, onClick = toggle)
            .semantics {
                contentDescription = if (isPrayed) "$name prayed" else "Mark $name as prayed"
                stateDescription = if (isPrayed) "Prayed" else "Not prayed"
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(28.dp).prayerPop(isPrayed), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2
                val a = (1f - p).coerceIn(0f, 1f)
                if (a > 0f) drawCircle(border.copy(alpha = a), r - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
                val pc = p.coerceAtLeast(0f)
                if (pc > 0.01f) drawCircle(fill.copy(alpha = pc.coerceAtMost(1f)), r * pc.coerceAtMost(1.1f))
            }
            Icon(
                symbol("checkmark"), null, tint = Palette.onAccentFill,
                modifier = Modifier.size(16.dp).graphicsLayer {
                    val s = 0.3f + 0.7f * p
                    scaleX = s; scaleY = s
                    alpha = p.coerceIn(0f, 1f)
                },
            )
        }
        PetalBurst(trigger = burst)
    }
}

/** Shown when all five are marked: a warm light and a quiet word of thanks. */
@Composable
private fun PrayerAllFiveBanner() {
    val reduce = LocalReduceMotion.current
    val glow = remember { Animatable(0.7f) }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        shown = true
        if (!reduce) {
            glow.animateTo(1f, tween(800))
            glow.animateTo(0.7f, tween(800))
        }
    }
    Column(Modifier.fillMaxWidth()) {
        RowDivider(inset = 0.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .drawBehind {
                    drawRect(
                        Brush.horizontalGradient(listOf(PrayerGold.copy(alpha = 0.18f), PrayerGold.copy(alpha = 0.04f))),
                        alpha = glow.value,
                    )
                }
                .padding(horizontal = Space.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Icon(symbol("sparkles"), null, tint = PrayerGold, modifier = Modifier.size(20.dp).prayerBounce(shown))
            Text(
                "Alhamdulillah, all five prayed.",
                style = androidx.compose.ui.text.TextStyle(fontFamily = AppFont.serif, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
                color = Palette.textPrimary,
            )
        }
    }
}

// Rest days

@Composable
private fun PrayerRestDays(modifier: Modifier = Modifier) {
    val library = LocalGraph.current.library
    val resting = library.isResting
    val since = library.restSince
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.tile))
                .background(Palette.blush.copy(alpha = 0.5f))
                .pressable(scale = 0.99f) { library.setResting(!resting) }
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = Space.m, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            Icon(
                symbol("moon.zzz"), null, tint = Palette.blushStrong,
                modifier = Modifier.widthIn(min = 26.dp).size(22.dp).prayerBounce(resting),
            )
            Column(Modifier.weight(1f).animateContentSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Rest days", style = TextStyles.body, color = Palette.textPrimary)
                AnimatedVisibility(visible = since != null, enter = fadeIn(), exit = fadeOut()) {
                    val date = since?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                    Text(
                        "Since ${date?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())) ?: ""}",
                        style = TextStyles.caption, color = Palette.textSecondary,
                    )
                }
            }
            AppSwitch(checked = resting, onCheckedChange = { library.setResting(it) })
        }
        Text(
            "Prayer pauses during your period and is not made up.",
            style = TextStyles.footnote, color = Palette.textSecondary,
            modifier = Modifier.padding(horizontal = Space.m),
        )
    }
}
