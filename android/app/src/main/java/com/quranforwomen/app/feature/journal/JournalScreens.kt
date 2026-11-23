package com.quranforwomen.app.feature.journal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.DuaStage
import com.quranforwomen.app.data.JournalEntry
import com.quranforwomen.app.data.JournalMood
import com.quranforwomen.app.data.SourceKind
import com.quranforwomen.app.data.arabicIndic
import com.quranforwomen.app.feature.notes.NotesHomeActions
import com.quranforwomen.app.feature.notes.notesHomeItems
import com.quranforwomen.app.nav.JournalStart
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.AppAlert
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.AppSheet
import com.quranforwomen.app.ui.components.AppSwitch
import com.quranforwomen.app.ui.components.AppTextField
import com.quranforwomen.app.ui.components.ArabicText
import com.quranforwomen.app.ui.components.BarIcon
import com.quranforwomen.app.ui.components.CardSurface
import com.quranforwomen.app.ui.components.Chip
import com.quranforwomen.app.ui.components.Eyebrow
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.ListRow
import com.quranforwomen.app.ui.components.PetalBurst
import com.quranforwomen.app.ui.components.PrimaryButton
import com.quranforwomen.app.ui.components.RollingNumber
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.RowIcon
import com.quranforwomen.app.ui.components.rowPress
import com.quranforwomen.app.ui.components.SearchField
import com.quranforwomen.app.ui.components.SectionHeader
import com.quranforwomen.app.ui.components.SegmentedControl
import com.quranforwomen.app.ui.components.SourceLabel
import com.quranforwomen.app.ui.components.SwitchRow
import com.quranforwomen.app.ui.components.TextAction
import com.quranforwomen.app.ui.components.TintedButton
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.inkReveal
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.scrollSettle
import com.quranforwomen.app.ui.components.shareFile
import com.quranforwomen.app.ui.components.shareText
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.BookInk
import com.quranforwomen.app.ui.theme.Ink
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import com.quranforwomen.app.ui.theme.Radius
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as JTextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// Tab root

private enum class JournalSection(val title: String) { journal("Journal"), notes("Notes") }

/** The Journal tab: journal pages and lesson notes, behind the lock when she turned it on. */
@Composable
fun JournalTabScreen() {
    val graph = LocalGraph.current
    val journal = graph.journal
    val sp = graph.prefs.sp
    val reduce = LocalReduceMotion.current

    var section by rememberSaveable {
        mutableStateOf(if (sp.getString(JournalPrefKey.section, "Journal") == "Notes") JournalSection.notes else JournalSection.journal)
    }
    val hidePreviews = remember { sp.getBoolean(JournalPrefKey.hidePreviews, false) }
    var filter by rememberSaveable { mutableStateOf(JournalFilter.all) }
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var notesQuery by rememberSaveable { mutableStateOf("") }
    val locked = journal.lockEnabled && !journal.isUnlocked
    val listState = rememberLazyListState()

    val filtered = remember(journal.entries, filter, query) {
        val base = journal.sortedEntries.filter(filter::matches)
        val q = query.trim()
        if (q.isEmpty()) base else base.filter { e ->
            listOf(e.title, e.body, e.gratitude, e.tags.joinToString(" "), e.origin ?: "", e.verse?.id ?: "", e.answeredNote)
                .any { it.contains(q, ignoreCase = true) }
        }
    }

    Box(Modifier.fillMaxSize()) {
        // The content stays mounted under the lock, so nothing she was doing is thrown away.
        AppScreen(
            title = section.title,
            book = true,
            listState = listState,
            spacing = Space.s,
            modifier = if (locked) Modifier.clearAndSetSemantics { } else Modifier,
            actions = {
                if (journal.isAccessible) {
                    if (section == JournalSection.journal) {
                        JournalHomeActions(filter, { filter = it }, showCalendar, { showCalendar = it })
                    } else {
                        NotesHomeActions()
                    }
                }
            },
            header = {
                Spacer(Modifier.height(Space.s))
                SegmentedControl(
                    options = JournalSection.entries,
                    selected = section,
                    label = { it.title },
                    onSelect = {
                        section = it
                        sp.edit().putString(JournalPrefKey.section, it.title).apply()
                    },
                )
                Spacer(Modifier.height(Space.xs))
            },
        ) {
            when (section) {
                JournalSection.journal -> journalHomeItems(
                    hasEntries = journal.entries.isNotEmpty(),
                    filtered = filtered,
                    filter = filter,
                    onClearFilter = { filter = JournalFilter.all },
                    query = query,
                    onQuery = { query = it },
                    showCalendar = showCalendar,
                    hidePreviews = hidePreviews,
                    justSaved = journal.justSaved,
                )
                JournalSection.notes -> notesHomeItems(
                    notes = journal.sortedNotes,
                    hasNotes = journal.notes.isNotEmpty(),
                    query = notesQuery,
                    onQuery = { notesQuery = it },
                )
            }
        }

        AnimatedVisibility(
            visible = locked,
            enter = if (reduce) fadeIn() else fadeIn() + scaleIn(initialScale = 1.04f),
            exit = if (reduce) fadeOut() else fadeOut(tween(550)) + scaleOut(tween(550), targetScale = 1.04f),
            modifier = Modifier.fillMaxSize(),
        ) {
            LockedJournal(onUnlock = { journal.isUnlocked = true })
        }
    }
}

// Lock

@Composable
private fun LockedJournal(onUnlock: () -> Unit) {
    val context = LocalContext.current
    var failed by remember { mutableStateOf(false) }
    var failCount by remember { mutableIntStateOf(0) }
    val authenticate = {
        journalAuthenticate(context, "Unlock your journal") { ok ->
            if (ok) onUnlock() else { failed = true; failCount++ }
        }
    }

    LaunchedEffect(Unit) { authenticate() }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        var wasStopped = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> wasStopped = true
                Lifecycle.Event.ON_RESUME -> if (wasStopped) { wasStopped = false; authenticate() }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BookInk.page)
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.l, Alignment.CenterVertically),
    ) {
        Icon(
            symbol("lock"), null, tint = BookInk.rule,
            modifier = Modifier.size(40.dp).journalBounceOn(failCount),
        )
        Text(
            "Your journal is private",
            style = TextStyles.title2.copy(fontFamily = AppFont.serif),
            color = BookInk.ink,
            modifier = Modifier.semantics { heading() },
        )
        PrimaryButton("Unlock Journal", onClick = authenticate, modifier = Modifier.padding(horizontal = Space.xxl))
        if (failed) {
            Text("Could not unlock. Try again.", style = TextStyles.footnote, color = BookInk.ribbon)
        }
    }
}

// Home

private enum class JournalFilter(val title: String, val symbolName: String) {
    all("All pages", "book.pages"),
    duas("My duas", "hands.and.sparkles"),
    answered("Answered duas", "sparkles"),
    gratitude("Gratitude", "heart"),
    quran("Quran reflections", "book"),
    favorites("Favorites", "star");

    fun matches(e: JournalEntry): Boolean = when (this) {
        all -> true
        duas -> e.isDua
        answered -> e.isDua && e.duaStage.isAnswered
        gratitude -> e.gratitude.isNotEmpty() || e.mood == JournalMood.grateful ||
            (e.origin ?: "").contains("grat", ignoreCase = true) || (e.origin ?: "").contains("bless", ignoreCase = true)
        quran -> e.verse != null
        favorites -> e.favorite
    }
}

@Composable
private fun JournalHomeActions(
    filter: JournalFilter,
    onFilter: (JournalFilter) -> Unit,
    showCalendar: Boolean,
    onCalendar: (Boolean) -> Unit,
) {
    val router = LocalRouter.current
    var viewMenu by remember { mutableStateOf(false) }
    var newMenu by remember { mutableStateOf(false) }

    Box {
        BarIcon(symbol("line.3.horizontal.decrease.circle"), "View options") { viewMenu = true }
        JournalMenu(viewMenu, { viewMenu = false }) {
            JournalFilter.entries.forEach { f ->
                JournalMenuItem(f.title, journalIcon(f.symbolName), checked = f == filter, onClick = {
                    onFilter(f)
                    viewMenu = false
                })
            }
            JournalMenuItem("Calendar view", symbol("calendar"), checked = showCalendar, onClick = {
                onCalendar(!showCalendar)
                viewMenu = false
            })
            JournalMenuDivider()
            JournalMenuItem("Journal Settings", symbol("gearshape"), onClick = {
                viewMenu = false
                router.push(Route.JournalSettings)
            })
        }
    }
    Box {
        BarIcon(symbol("plus"), "New page") { newMenu = true }
        JournalMenu(newMenu, { newMenu = false }) {
            JournalMenuItem("New Page", symbol("square.and.pencil"), onClick = {
                newMenu = false
                router.push(Route.JournalEditor(JournalStart.Blank))
            })
            JournalMenuItem("New Dua", symbol("hands.and.sparkles"), onClick = {
                newMenu = false
                router.push(Route.JournalEditor(JournalStart.Dua))
            })
            JournalMenuDivider()
            JournalMenuItem("From a Template", symbol("doc.text"), onClick = {
                newMenu = false
                router.push(Route.JournalTemplates)
            })
            JournalMenuItem("From a Prompt", symbol("text.bubble"), onClick = {
                newMenu = false
                router.push(Route.JournalPrompts)
            })
        }
    }
}

private val dayFormat: DateTimeFormatter get() = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
private val timeFormat: DateTimeFormatter get() = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

private fun dayLabel(day: LocalDate): String {
    val today = LocalDate.now()
    return when (day) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> day.format(dayFormat)
    }
}

private fun JournalEntry.localTime(): LocalTime = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalTime()

/** A calm list of pages, newest first. One + menu, like Apple's Journal. */
private fun LazyListScope.journalHomeItems(
    hasEntries: Boolean,
    filtered: List<JournalEntry>,
    filter: JournalFilter,
    onClearFilter: () -> Unit,
    query: String,
    onQuery: (String) -> Unit,
    showCalendar: Boolean,
    hidePreviews: Boolean,
    justSaved: String?,
) {
    item(key = "journal-search") {
        SearchField(query, onQuery, "Search your journal", Modifier.padding(bottom = Space.xs))
    }

    if (query.isEmpty() && filter == JournalFilter.all) {
        item(key = "journal-suggestion") {
            TodaySuggestion(Modifier.padding(bottom = Space.m).focusIn(50).scrollSettle())
        }
    }

    if (filter != JournalFilter.all) {
        item(key = "journal-filter") {
            Row {
                Chip(filter.title, selected = false, onClick = onClearFilter, icon = symbol("xmark.circle.fill"))
            }
        }
    }

    if (!hasEntries) {
        item(key = "journal-empty") {
            Column(
                Modifier.fillMaxWidth().padding(top = Space.l),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                val router = LocalRouter.current
                Text("No pages yet", style = TextStyles.title3.copy(fontFamily = AppFont.serif), color = BookInk.ink)
                TintedButton(
                    "New Page", { router.push(Route.JournalEditor(JournalStart.Blank)) },
                    fullWidth = false, modifier = Modifier.padding(top = Space.xs),
                )
            }
        }
        return
    }

    if (showCalendar) {
        item(key = "journal-calendar") {
            val router = LocalRouter.current
            JournalCalendar(filtered, hidePreviews, Modifier.scrollSettle()) { router.push(Route.JournalEntry(it.id)) }
        }
        return
    }

    if (filtered.isEmpty()) {
        item(key = "journal-noresults") { JournalSearchEmpty(query) }
        return
    }

    val order = filtered.withIndex().associate { it.value.id to it.index }
    filtered.groupBy { it.localDate }.forEach { (day, items) ->
        item(key = "day-$day") {
            Text(
                dayLabel(day),
                style = TextStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                color = BookInk.caption,
                modifier = Modifier
                    .padding(top = Space.s)
                    .semantics { heading() }
                    .focusIn(120 + minOf(order[items.first().id] ?: 0, 8) * 50),
            )
        }
        items.forEach { entry ->
            item(key = "entry-${entry.id}") {
                val router = LocalRouter.current
                EntryCard(
                    entry = entry,
                    hidePreviews = hidePreviews,
                    glow = justSaved == entry.id,
                    modifier = Modifier.dealIn(minOf(order[entry.id] ?: 0, 8), baseDelay = 150, step = 50).scrollSettle(),
                ) { router.push(Route.JournalEntry(entry.id)) }
            }
        }
    }
}

/** One quiet suggestion, not a banner. */
@Composable
private fun TodaySuggestion(modifier: Modifier = Modifier) {
    val router = LocalRouter.current
    val question = JournalContent.question()
    CardSurface(modifier, onClick = { router.push(Route.JournalEditor(JournalStart.Prompt(question))) }) {
        Text("Today's question", style = TextStyles.subheadline, color = BookInk.caption)
        Spacer(Modifier.height(Space.xs))
        Text(question, style = TextStyles.title3.copy(fontFamily = AppFont.serif), color = BookInk.ink)
        Spacer(Modifier.height(Space.xs + 2.dp))
        Text("Write", style = TextStyles.subheadline.copy(fontWeight = FontWeight.SemiBold), color = BookInk.rule)
    }
}

// Entry card

@Composable
private fun EntryCard(
    entry: JournalEntry,
    hidePreviews: Boolean,
    glow: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val answered = entry.isDua && entry.duaStage.isAnswered
    Box(modifier) {
        CardSurface(padding = Space.m, onClick = onClick) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(if (answered) JournalGold.mark else BookInk.rule))
                val meta = TextStyles.caption
                entry.mood?.let { Text(it.title, style = meta, color = BookInk.caption) }
                entry.origin?.let {
                    Text("· $it", style = meta, color = BookInk.caption, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                }
                Spacer(Modifier.weight(1f))
                if (entry.favorite) Icon(symbol("star.fill"), "Favorite", tint = JournalGold.mark, modifier = Modifier.size(13.dp))
                Text(entry.localTime().format(timeFormat), style = meta, color = BookInk.caption)
            }
            Spacer(Modifier.height(Space.xs))
            if (hidePreviews) {
                Text("Private page", style = TextStyles.subheadline, color = BookInk.caption)
            } else {
                Text(entry.displayTitle, style = TextStyles.headline, color = BookInk.ink)
                // Without a title, the first line already shows as the title, so the preview starts after it.
                val preview = entry.body.replace("## ", "").let { body ->
                    if (entry.title.isEmpty()) body.lines().dropWhile { it.isBlank() }.drop(1).joinToString(separator = "\n").trim() else body
                }
                if (preview.isNotEmpty()) {
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        preview,
                        style = TextStyles.subheadline.copy(fontFamily = AppFont.serif),
                        color = BookInk.caption,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (entry.verse != null || entry.isDua) {
                    Spacer(Modifier.height(Space.xs))
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalAlignment = Alignment.CenterVertically) {
                        entry.verse?.let { SourceLabel(SourceKind.Quran, "Quran ${it.id}") }
                        if (entry.isDua) DuaStagePill(entry.duaStage)
                    }
                }
            }
        }
        if (glow) ArrivalGlow(Modifier.matchParentSize())
    }
}

@Composable
private fun DuaStagePill(stage: DuaStage) {
    Text(
        stage.title,
        style = TextStyles.caption.copy(fontWeight = FontWeight.SemiBold),
        color = if (stage.isAnswered) JournalGold.text else BookInk.ribbon,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (stage.isAnswered) JournalGold.wash else BookInk.ribbon.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** A soft blush ring around a page she just saved, fading as it settles. */
@Composable
private fun ArrivalGlow(modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val fade = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        fade.animateTo(0f, tween(if (reduce) 300 else 1800, delayMillis = 400, easing = FastOutSlowInEasing))
    }
    val shape = RoundedCornerShape(Radius.card)
    Box(
        modifier
            .clip(shape)
            .background(BookInk.ribbon.copy(alpha = 0.12f * 0.35f * fade.value))
            .border(2.dp, BookInk.ribbon.copy(alpha = 0.6f * fade.value), shape)
    )
}

// Calendar

/** A month of her journal. Days with pages carry a soft dot; tapping one lists that day's pages below. */
@Composable
private fun JournalCalendar(
    entries: List<JournalEntry>,
    hidePreviews: Boolean,
    modifier: Modifier = Modifier,
    open: (JournalEntry) -> Unit,
) {
    val reduce = LocalReduceMotion.current
    val haptics = rememberHaptics()
    var monthIndex by rememberSaveable { mutableIntStateOf(YearMonth.now().let { it.year * 12 + it.monthValue - 1 }) }
    var selectedEpoch by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    val selected = LocalDate.ofEpochDay(selectedEpoch)
    val month = YearMonth.of(monthIndex / 12, monthIndex % 12 + 1)
    val byDay = remember(entries) { entries.groupBy { it.localDate } }
    val count = entries.count { YearMonth.from(it.localDate) == month }
    val locale = Locale.getDefault()
    val firstDay = WeekFields.of(locale).firstDayOfWeek
    val leading = (month.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
    val days: List<LocalDate?> = List(leading) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
    val today = LocalDate.now()
    val chosen = byDay[selected].orEmpty().sortedByDescending { it.date }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.l)) {
        CardSurface {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CalendarArrow("chevron.left", "Previous month") { monthIndex-- }
                Spacer(Modifier.weight(1f))
                AnimatedContent(
                    targetState = month,
                    transitionSpec = { fadeIn(Motion.gentle()) togetherWith fadeOut(Motion.gentle()) },
                    label = "month",
                ) { m ->
                    Text(m.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)), style = TextStyles.headline, color = BookInk.rule)
                }
                Spacer(Modifier.weight(1f))
                CalendarArrow("chevron.right", "Next month") { monthIndex++ }
            }
            Spacer(Modifier.height(Space.m))
            Row(Modifier.fillMaxWidth().clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 0 until 7) {
                    Text(
                        firstDay.plus(i.toLong()).getDisplayName(JTextStyle.NARROW, locale),
                        style = TextStyles.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                        color = BookInk.caption,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
            days.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 0 until 7) {
                        val day = week.getOrNull(i)
                        if (day == null) {
                            Spacer(Modifier.weight(1f).height(44.dp))
                            continue
                        }
                        val items = byDay[day].orEmpty()
                        val isSelected = day == selected
                        val shape = RoundedCornerShape(10.dp)
                        val fill by animateFloatAsState(
                            if (isSelected) 1f else 0f,
                            if (reduce) tween(0) else spring(dampingRatio = 0.75f, stiffness = 400f),
                            label = "daySelected",
                        )
                        val gild = BookInk.gild
                        Column(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .clip(shape)
                                .drawBehind {
                                    if (fill > 0.01f) {
                                        val s = 0.7f + 0.3f * fill
                                        val w = size.width * s
                                        val h = size.height * s
                                        drawRoundRect(
                                            gild.copy(alpha = 0.32f * fill.coerceIn(0f, 1f)),
                                            topLeft = Offset((size.width - w) / 2, (size.height - h) / 2),
                                            size = androidx.compose.ui.geometry.Size(w, h),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx() * s),
                                        )
                                    }
                                }
                                .border(1.dp, if (day == today) BookInk.rule else Color.Transparent, shape)
                                .pressable(scale = 0.95f, enabled = items.isNotEmpty()) {
                                    if (selectedEpoch != day.toEpochDay()) {
                                        selectedEpoch = day.toEpochDay()
                                        haptics.tick()
                                    }
                                }
                                .semantics {
                                    contentDescription = "${day.format(dateFormat)}, ${if (items.size == 1) "1 page" else "${items.size} pages"}"
                                    this.selected = isSelected
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                "${day.dayOfMonth}",
                                style = TextStyles.subheadline.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
                                color = BookInk.ink,
                            )
                            Spacer(Modifier.height(3.dp))
                            Box(
                                Modifier.size(5.dp).clip(CircleShape)
                                    .background(if (items.isEmpty()) Color.Transparent else BookInk.rule.copy(alpha = 0.7f))
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(Space.s))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                RollingNumber(count, TextStyles.footnote, BookInk.caption)
                Text(if (count == 1) " page this month" else " pages this month", style = TextStyles.footnote, color = BookInk.caption)
            }
        }

        AnimatedContent(
            targetState = selectedEpoch to chosen.map { it.id },
            transitionSpec = {
                if (reduce) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                else fadeIn(Motion.gentle()) togetherWith fadeOut(Motion.gentle())
            },
            label = "dayPages",
        ) { (epoch, _) ->
            val day = LocalDate.ofEpochDay(epoch)
            val pages = byDay[day].orEmpty().sortedByDescending { it.date }
            if (pages.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    Text(
                        dayLabel(day),
                        style = TextStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
                        color = BookInk.caption,
                        modifier = Modifier.semantics { heading() }.focusIn(20),
                    )
                    pages.forEachIndexed { index, entry ->
                        EntryCard(
                            entry = entry,
                            hidePreviews = hidePreviews,
                            glow = false,
                            modifier = Modifier.dealIn(minOf(index, 6), baseDelay = 50, step = 50),
                        ) { open(entry) }
                    }
                }
            } else {
                Spacer(Modifier.height(0.dp))
            }
        }
    }
}

@Composable
private fun CalendarArrow(name: String, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).pressable(scale = 0.9f, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Icon(journalIcon(name), null, tint = BookInk.rule, modifier = Modifier.size(24.dp)) }
}

// Entry detail

/**
 * One journal page. The page comes into focus piece by piece, the attached verse's Arabic is
 * revealed right to left like ink, and favoriting swaps the star with a bounce and a burst of petals.
 */
@Composable
fun JournalEntryScreen(id: String) {
    val graph = LocalGraph.current
    val journal = graph.journal
    val quran = graph.quran
    val router = LocalRouter.current
    val context = LocalContext.current
    val haptics = rememberHaptics()
    var updatingDua by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val entry = journal.entry(id)

    JournalPrivacyCover {
        AppScreen(
            title = "",
            largeTitle = false,
            book = true,
            spacing = Space.l,
            actions = {
                // Nothing on the page can be shared or changed while the journal is locked.
                if (entry != null && journal.isAccessible) {
                    var burst by remember { mutableIntStateOf(0) }
                    Box(contentAlignment = Alignment.Center) {
                        PetalBurst(burst)
                        SwapIcon(
                            name = if (entry.favorite) "star.fill" else "star",
                            label = if (entry.favorite) "Remove from favorites" else "Favorite",
                            bounceKey = entry.favorite,
                        ) {
                            haptics.tick()
                            if (!entry.favorite) burst++
                            journal.toggleFavorite(entry)
                        }
                    }
                    BarIcon(symbol("square.and.arrow.up"), "Share") { context.shareText(entry.plainText) }
                    var more by remember { mutableStateOf(false) }
                    Box {
                        BarIcon(symbol("ellipsis.circle"), "More") { more = true }
                        JournalMenu(more, { more = false }) {
                            JournalMenuItem("Edit", symbol("pencil"), onClick = {
                                more = false
                                router.push(Route.JournalEditor(JournalStart.Edit(entry.id)))
                            })
                            JournalMenuItem("Delete", symbol("trash"), destructive = true, onClick = {
                                more = false
                                confirmDelete = true
                            })
                        }
                    }
                }
            },
        ) {
            if (entry == null) {
                item(key = "missing") {
                    com.quranforwomen.app.ui.components.SavedEmpty(symbol("doc"), "Page not found")
                }
                return@AppScreen
            }
            item(key = "head") {
                Column(Modifier.padding(top = Space.l).focusIn(50), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    if (entry.mood != null || entry.origin != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val style = TextStyles.footnote.copy(fontWeight = FontWeight.Medium)
                            entry.mood?.let {
                                Icon(symbol(it.symbolName), null, tint = BookInk.rule, modifier = Modifier.size(15.dp))
                                Text(it.title, style = style, color = BookInk.rule)
                            }
                            entry.origin?.let { Text("· $it", style = style, color = BookInk.rule) }
                        }
                    }
                    Text(
                        entry.displayTitle,
                        style = TextStyles.title.copy(fontFamily = AppFont.serif, fontWeight = FontWeight.SemiBold),
                        color = BookInk.ink,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        Instant.ofEpochMilli(entry.date).atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)),
                        style = TextStyles.subheadline,
                        color = BookInk.caption,
                    )
                }
            }
            entry.verse?.let { verse ->
                item(key = "verse") {
                    CardSurface(
                        Modifier.focusIn(150).scrollSettle(),
                        fill = BookInk.gild.copy(alpha = 0.2f * 0.6f),
                        onClick = { router.push(Route.Reader(verse.surah, verse.ayah)) },
                    ) {
                        ArabicText("${quran.text(verse)} ﴿${verse.ayah.arabicIndic}﴾", size = 22f, modifier = Modifier.inkReveal(300))
                        Spacer(Modifier.height(Space.s))
                        JournalTranslationBody(verse)
                        Spacer(Modifier.height(Space.s))
                        Text(quran.displayName(verse), style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold), color = BookInk.rule)
                    }
                }
            }
            item(key = "body") { JournalMarkdownBody(entry.body, Modifier.focusIn(220)) }
            if (entry.gratitude.isNotEmpty()) {
                item(key = "gratitude") {
                    CardSurface(
                        Modifier.dealIn(0, baseDelay = 300).scrollSettle(),
                        padding = Space.m,
                        fill = BookInk.gild.copy(alpha = 0.2f * 0.5f),
                    ) {
                        Text("Grateful for", style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold), color = BookInk.rule)
                        Spacer(Modifier.height(4.dp))
                        Text(entry.gratitude, style = TextStyles.body.copy(fontFamily = AppFont.serif), color = BookInk.ink)
                    }
                }
            }
            if (entry.isDua) {
                item(key = "dua") {
                    DuaJourneyCard(entry, Modifier.dealIn(1, baseDelay = 300).scrollSettle()) { updatingDua = true }
                }
            }
            if (entry.tags.isNotEmpty()) {
                item(key = "tags") {
                    Text(
                        entry.tags.joinToString("  ") { "#$it" },
                        style = TextStyles.footnote,
                        color = BookInk.caption,
                        modifier = Modifier.focusIn(400),
                    )
                }
            }
        }
    }

    if (entry != null && updatingDua) {
        DuaJourneySheet(entry) { updatingDua = false }
    }
    if (entry != null && confirmDelete) {
        AppAlert(
            title = "Delete this page?",
            message = "This cannot be undone.",
            confirmLabel = "Delete Page",
            destructive = true,
            onConfirm = {
                journal.delete(entry)
                router.pop()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** A bar icon whose symbol swaps with a soft scale, and bounces when its state changes. */
@Composable
private fun SwapIcon(name: String, label: String, bounceKey: Any?, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .pressable(scale = 0.9f, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = name,
            transitionSpec = { (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut()) },
            label = "swap",
            modifier = Modifier.journalBounceOn(bounceKey),
        ) { n ->
            Icon(journalIcon(n), null, tint = Ink.accent, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun DuaJourneyCard(entry: JournalEntry, modifier: Modifier = Modifier, onUpdate: () -> Unit) {
    val answered = entry.duaStage.isAnswered
    val dateShort = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    Box(modifier) {
        if (answered) GoldenBloom(entry.duaStage, Modifier.matchParentSize())
        CardSurface(fill = if (answered) JournalGold.card else BookInk.pageLit) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dua journey", style = TextStyles.headline, color = BookInk.ink, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.weight(1f))
                Box(contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val color = if (answered) JournalGold.text else BookInk.rule
                        AnimatedVisibility(answered, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                            Icon(symbol("sparkles"), null, tint = color, modifier = Modifier.size(16.dp).journalBounceOn(entry.duaStage))
                        }
                        AnimatedContent(entry.duaStage, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "stage") { s ->
                            Text(s.title, style = TextStyles.subheadline.copy(fontWeight = FontWeight.SemiBold), color = color)
                        }
                    }
                    AnsweredBurst(entry.duaStage, active = answered)
                }
            }
            if (entry.nextStep.isNotEmpty()) {
                Spacer(Modifier.height(Space.s))
                Text("Next step: ${entry.nextStep}", style = TextStyles.subheadline, color = BookInk.ink)
            }
            if (answered) {
                if (entry.answeredNote.isNotEmpty()) {
                    Spacer(Modifier.height(Space.s))
                    Text(entry.answeredNote, style = TextStyles.body.copy(fontFamily = AppFont.serif), color = BookInk.ink)
                }
                entry.answeredAt?.let { at ->
                    val asked = Instant.ofEpochMilli(entry.date).atZone(ZoneId.systemDefault()).format(dateShort)
                    val done = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).format(dateShort)
                    Spacer(Modifier.height(Space.s))
                    Text("Asked $asked. Answered $done.", style = TextStyles.caption, color = BookInk.caption)
                }
            }
            Spacer(Modifier.height(Space.s))
            TintedButton("Update This Dua", onUpdate)
        }
    }
}

/**
 * "Where is this dua now?" The stages sit on a vertical path. The current step glows and the
 * path fills up to it; moving to an answered stage blooms gold with a few sparks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DuaJourneySheet(entry: JournalEntry, onClose: () -> Unit) {
    val journal = LocalGraph.current.journal
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var stage by rememberSaveable { mutableStateOf(entry.duaStage) }
    var nextStep by rememberSaveable { mutableStateOf(entry.nextStep) }
    var answeredNote by rememberSaveable { mutableStateOf(entry.answeredNote) }
    val close: () -> Unit = { scope.launch { sheetState.hide() }.invokeOnCompletion { onClose() } }

    AppSheet(onDismiss = onClose, book = true, sheetState = sheetState, skipPartiallyExpanded = false) {
        Box(Modifier.fillMaxWidth().padding(horizontal = Space.xs)) {
            TextAction("Cancel", close, Modifier.align(Alignment.CenterStart))
            Text("Dua Journey", style = TextStyles.headline, color = BookInk.ink, modifier = Modifier.align(Alignment.Center))
            TextAction(
                "Save",
                {
                    journal.save(
                        entry.copy(
                            duaStage = stage,
                            nextStep = nextStep.trim(),
                            answeredNote = if (stage.isAnswered) answeredNote.trim() else "",
                        )
                    )
                    close()
                },
                Modifier.align(Alignment.CenterEnd),
                enabled = journal.isAccessible,
                bold = true,
            )
        }
        JournalPrivacyCover(Modifier.heightIn(max = 640.dp), fill = false) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Space.page, vertical = Space.s),
                verticalArrangement = Arrangement.spacedBy(Space.l),
            ) {
                JournalFormSection("Where is this dua now?") {
                    StagePath(stage) { step ->
                        if (step == stage) return@StagePath
                        if (step.isAnswered) haptics.success() else haptics.tick()
                        stage = step
                    }
                }
                JournalFormSection("Next step") {
                    AppTextField(nextStep, { nextStep = it }, "Optional", Modifier.padding(Space.m), minLines = 1, maxLines = 6, filled = false)
                }
                AnimatedVisibility(stage.isAnswered, enter = fadeIn() + androidx.compose.animation.expandVertically(), exit = fadeOut() + androidx.compose.animation.shrinkVertically()) {
                    JournalFormSection("What happened") {
                        AppTextField(answeredNote, { answeredNote = it }, "How did Allah answer?", Modifier.padding(Space.m), minLines = 3, maxLines = 8, filled = false)
                    }
                }
                Text(
                    "The Prophet ﷺ taught that every sincere dua is answered: it is given, saved for the Hereafter, or a harm of the same size is turned away. (Musnad Ahmad)",
                    style = TextStyles.footnote,
                    color = BookInk.caption,
                    modifier = Modifier.padding(horizontal = Space.m),
                )
            }
        }
    }
}

/** A titled group of form rows, like an iOS Form section. */
@Composable
internal fun JournalFormSection(header: String?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (header != null) {
            Text(
                header,
                style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold),
                color = BookInk.caption,
                modifier = Modifier.padding(horizontal = Space.m).semantics { heading() },
            )
        }
        GroupedSection(content = content)
    }
}

@Composable
private fun StagePath(stage: DuaStage, onSelect: (DuaStage) -> Unit) {
    val stages = DuaStage.entries
    val current = stages.indexOf(stage)
    Column(Modifier.fillMaxWidth().padding(horizontal = Space.m, vertical = Space.xs)) {
        stages.forEachIndexed { index, step ->
            val isCurrent = step == stage
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .pressable(scale = 0.97f) { onSelect(step) }
                    .semantics(mergeDescendants = true) { selected = isCurrent; contentDescription = step.title },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                StageNode(step, index, current, stage)
                Text(
                    step.title,
                    style = TextStyles.body.copy(fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (isCurrent) (if (step.isAnswered) JournalGold.text else BookInk.ink) else BookInk.caption,
                )
            }
        }
    }
}

/** A dot on the path, with the line leading down to the next one. */
@Composable
private fun StageNode(step: DuaStage, index: Int, current: Int, stage: DuaStage) {
    val isCurrent = step == stage
    val reached = index <= current
    val tint = if (stage.isAnswered) JournalGold.mark else BookInk.rule
    val isLast = index == DuaStage.entries.size - 1
    val spec = if (stage.isAnswered) Motion.reward<androidx.compose.ui.unit.Dp>() else Motion.gentle()
    val dot by animateDpAsState(if (isCurrent) 16.dp else 12.dp, spec, label = "dot")
    val halo by animateFloatAsState(if (isCurrent) 1f else 0.4f, Motion.gentle(), label = "halo")
    val haloAlpha by animateFloatAsState(if (isCurrent) 1f else 0f, Motion.gentle(), label = "haloAlpha")
    val lineColor = if (index < current) tint else BookInk.rule.copy(alpha = 0.25f)

    Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
        if (!isLast) {
            Box(Modifier.offset(y = 22.dp).width(2.dp).height(28.dp).clip(CircleShape).background(lineColor))
        }
        if (isCurrent && step.isAnswered) {
            GoldenBloom(stage, Modifier.size(44.dp).clip(CircleShape))
        }
        Box(
            Modifier
                .size(30.dp)
                .graphicsLayer { scaleX = halo; scaleY = halo; alpha = haloAlpha }
                .blur(2.dp, BlurredEdgeTreatment.Unbounded)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.22f))
        )
        Box(
            Modifier
                .size(dot)
                .shadow(if (isCurrent) 6.dp else 0.dp, CircleShape, ambientColor = tint, spotColor = tint)
                .clip(CircleShape)
                .background(if (reached) tint else BookInk.pageLit)
                .border(2.dp, if (reached) tint else BookInk.rule.copy(alpha = 0.25f), CircleShape)
        )
        AnsweredBurst(stage, active = isCurrent && step.isAnswered)
    }
}

/** A warm golden light that blooms behind an answered dua, then settles. */
@Composable
private fun GoldenBloom(trigger: DuaStage, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val alpha = remember { Animatable(if (reduce) 0.4f else 0f) }
    val scale = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (reduce) { alpha.snapTo(0.4f); return@LaunchedEffect }
        alpha.snapTo(0f)
        launch { scale.animateTo(1.08f, spring(dampingRatio = 0.6f, stiffness = 110f)) }
        alpha.animateTo(0.95f, spring(dampingRatio = 0.6f, stiffness = 110f))
        delay(300)
        launch { scale.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) }
        alpha.animateTo(0.4f, tween(1400, easing = FastOutSlowInEasing))
    }
    Box(
        modifier
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value; this.alpha = alpha.value }
            .blur(26.dp, BlurredEdgeTreatment.Unbounded)
            .clip(RoundedCornerShape(Radius.card))
            .background(JournalGold.wash)
    )
}

/** Sparks of gold that fly out when a dua is marked answered. */
@Composable
private fun AnsweredBurst(trigger: DuaStage, active: Boolean) {
    val reduce = LocalReduceMotion.current
    val progress = remember { Animatable(1f) }
    var last by remember { mutableStateOf(trigger) }
    LaunchedEffect(trigger) {
        if (trigger == last) return@LaunchedEffect
        last = trigger
        if (active && !reduce) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(750, easing = LinearEasing))
        }
    }
    if (reduce) return
    Canvas(Modifier.size(1.dp)) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        val t = p * 0.75f
        // Distance springs out to 54 points; sparks hold, then fade; they swell then shrink.
        val travel = (1f - (1f - (t / 0.6f).coerceAtMost(1f)).let { it * it * it }) * 54f * density
        val opacity = when {
            t < 0.35f -> 1f
            else -> (1f - (t - 0.35f) / 0.4f).coerceIn(0f, 1f)
        }
        val sc = if (t < 0.25f) 1f + 0.2f * (t / 0.25f) else 1.2f - 0.7f * ((t - 0.25f) / 0.5f).coerceAtMost(1f)
        for (i in 0 until 10) {
            val angle = i / 10.0 * 2 * Math.PI
            val c = Offset((cos(angle) * travel).toFloat(), (sin(angle) * travel).toFloat())
            drawCircle(
                (if (i % 2 == 0) JournalGold.mark else JournalGold.light).copy(alpha = opacity),
                radius = 2.5f * density * sc,
                center = c,
            )
        }
    }
}

// Templates

/**
 * Template gallery. The featured card comes into focus, the collection tiles are dealt in
 * with a small bounce of each icon, and search results crossfade in and out as she types.
 */
@Composable
fun JournalTemplatesScreen() {
    val journal = LocalGraph.current.journal
    val router = LocalRouter.current
    var query by rememberSaveable { mutableStateOf("") }
    val open: (JournalTemplate) -> Unit = { router.push(Route.JournalEditor(JournalStart.Template(it.id))) }

    AppScreen(title = "Templates", book = true, spacing = Space.s) {
        item(key = "search") {
            SearchField(query, { query = it }, "Dua, gratitude, Ramadan", Modifier.padding(bottom = Space.s))
        }
        if (query.isEmpty()) {
            val recent = journal.recentTemplates.mapNotNull(JournalContent::template)
            val featured = recent.firstOrNull() ?: JournalContent.template("muhasabah")
            if (featured != null) {
                item(key = "featured") {
                    FeaturedTemplate(featured, recent.isEmpty(), Modifier.padding(bottom = Space.m).focusIn(50).scrollSettle()) { open(featured) }
                }
            }
            JournalContent.collections.chunked(2).forEachIndexed { row, pair ->
                item(key = "row-$row") {
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                        pair.forEachIndexed { col, collection ->
                            val index = row * 2 + col
                            CollectionTile(
                                collection,
                                index,
                                Modifier.weight(1f).fillMaxHeight().dealIn(minOf(index, 8), baseDelay = 150, step = 60).scrollSettle(),
                            ) { router.push(Route.TemplateCollection(collection.id)) }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        } else {
            val found = JournalContent.templates.filter { t ->
                (listOf(t.title, t.summary) + t.prompts.map { it.question }).any { it.contains(query, ignoreCase = true) }
            }
            if (found.isEmpty()) {
                item(key = "none") { JournalSearchEmpty(query) }
            }
            found.forEach { t ->
                item(key = "t-${t.id}") {
                    TemplateRow(t, Modifier.animateItem().scrollSettle()) { open(t) }
                }
            }
        }
    }
}

@Composable
private fun FeaturedTemplate(template: JournalTemplate, isSuggestion: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .pressable(scale = 0.985f, onClick = onClick)
            .clip(RoundedCornerShape(Radius.card))
            .background(BookInk.rule)
            .padding(Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Eyebrow(if (isSuggestion) "Try tonight" else "Recently used", color = BookInk.pageLit.copy(alpha = 0.85f))
        Text(template.title, style = TextStyles.title2.copy(fontFamily = AppFont.serif), color = BookInk.pageLit)
        Text(template.summary, style = TextStyles.subheadline, color = BookInk.pageLit.copy(alpha = 0.85f))
        Text(
            "Begin",
            style = TextStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
            color = BookInk.pageLit,
            modifier = Modifier
                .padding(top = Space.xs)
                .clip(CircleShape)
                .background(BookInk.pageLit.copy(alpha = 0.2f))
                .padding(horizontal = Space.m, vertical = Space.xs),
        )
    }
}

@Composable
private fun CollectionTile(collection: TemplateCollection, index: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Radius.card)
    Column(
        modifier
            .pressable(scale = 0.985f, onClick = onClick)
            .heightIn(min = 120.dp)
            .clip(shape)
            .background(BookInk.pageLit)
            .border(0.8.dp, BookInk.gild.copy(alpha = 0.35f), shape)
            .padding(Space.m),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            collection.icon, null, tint = BookInk.rule,
            modifier = Modifier.size(22.dp).journalBounceOnAppear(450 + minOf(index, 8) * 60),
        )
        Column(Modifier.padding(top = Space.s)) {
            Text(collection.title, style = TextStyles.headline, color = BookInk.ink)
            Text("${JournalContent.templates(collection).size} templates", style = TextStyles.caption, color = BookInk.caption)
        }
    }
}

/** One collection of templates. The rows are dealt in and settle softly as they scroll. */
@Composable
fun TemplateCollectionScreen(id: String) {
    val router = LocalRouter.current
    val collection = JournalContent.collection(id)
    AppScreen(title = collection?.title ?: "Templates", book = true, spacing = Space.s) {
        if (collection != null) {
            JournalContent.templates(collection).forEachIndexed { index, t ->
                item(key = t.id) {
                    TemplateRow(t, Modifier.dealIn(minOf(index, 8), baseDelay = 120, step = 50).scrollSettle()) {
                        router.push(Route.JournalEditor(JournalStart.Template(t.id)))
                    }
                }
            }
        }
    }
}

/** A template card. It presses in softly, and the dua mark gives one small bounce when the row arrives. */
@Composable
private fun TemplateRow(template: JournalTemplate, modifier: Modifier = Modifier, onClick: () -> Unit) {
    CardSurface(modifier, padding = Space.m, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(template.title, style = TextStyles.headline, color = BookInk.ink, modifier = Modifier.weight(1f))
            if (template.isDua) {
                Icon(
                    symbol("hands.and.sparkles"), "Adds to your duas", tint = BookInk.ribbon,
                    modifier = Modifier.size(20.dp).journalBounceOnAppear(500),
                )
            }
        }
        Spacer(Modifier.height(Space.xs))
        Text(template.summary, style = TextStyles.subheadline, color = BookInk.caption)
        Spacer(Modifier.height(Space.xs))
        Text(
            template.prompts.joinToString(" · ") { it.heading },
            style = TextStyles.caption,
            color = BookInk.rule,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// Prompts

/**
 * Prompts. The prompt card comes into focus, "Another" crossfades the prompt while its arrows
 * bounce, and the prompt packs are dealt in below.
 */
@Composable
fun JournalPromptsScreen() {
    val router = LocalRouter.current
    val haptics = rememberHaptics()
    val reduce = LocalReduceMotion.current
    var shuffle by rememberSaveable { mutableIntStateOf(0) }

    // Morning prompts before noon, evening prompts after six, shukr in between.
    val hour = LocalTime.now().hour
    val packId = if (hour in 4 until 12) "morning" else if (hour >= 18 || hour < 4) "evening" else "shukr"
    val pack = JournalContent.pack(packId) ?: JournalContent.packs[0]
    val day = LocalDate.now().toEpochDay()
    val prompt = pack.prompts[Math.floorMod(day + shuffle, pack.prompts.size.toLong()).toInt()]

    AppScreen(title = "Prompts", book = true, spacing = Space.s) {
        item(key = "now") {
            CardSurface(
                Modifier.padding(bottom = Space.l).focusIn(50).scrollSettle(),
                padding = Space.xl,
                fill = BookInk.ribbon.copy(alpha = 0.12f * 0.5f),
            ) {
                Eyebrow("For now · ${pack.title}")
                Spacer(Modifier.height(Space.m))
                AnimatedContent(
                    targetState = prompt,
                    transitionSpec = {
                        if (reduce) fadeIn() togetherWith fadeOut()
                        else (fadeIn(Motion.gentle()) + scaleIn(Motion.gentle(), initialScale = 0.98f)) togetherWith fadeOut(Motion.gentle())
                    },
                    label = "prompt",
                ) { text ->
                    Text(text, style = TextStyles.title2.copy(fontFamily = AppFont.serif), color = BookInk.ink, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(Space.m))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                    TintedButton("Write", { router.push(Route.JournalEditor(JournalStart.Prompt(prompt, pack.isDua))) }, fullWidth = false)
                    Row(
                        Modifier
                            .heightIn(min = 44.dp)
                            .pressable {
                                haptics.tick()
                                shuffle++
                            }
                            .semantics(mergeDescendants = true) { contentDescription = "Another prompt" },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(symbol("arrow.triangle.2.circlepath"), null, tint = BookInk.rule, modifier = Modifier.size(18.dp).journalBounceOn(shuffle))
                        Text("Another", style = TextStyles.subheadline.copy(fontWeight = FontWeight.Medium), color = BookInk.rule)
                    }
                }
            }
        }
        item(key = "packs-title") { SectionHeader("Prompt packs", Modifier.focusIn(150)) }
        JournalContent.packs.forEachIndexed { index, p ->
            item(key = p.id) {
                CardSurface(
                    Modifier.dealIn(minOf(index, 8), baseDelay = 200, step = 50).scrollSettle(),
                    padding = Space.m,
                    onClick = { router.push(Route.PromptPack(p.id)) },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                        RowIcon(p.icon)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(p.title, style = TextStyles.headline, color = BookInk.ink)
                            Text("${p.prompts.size} prompts", style = TextStyles.caption, color = BookInk.caption)
                        }
                        Icon(journalIcon("chevron.right"), null, tint = BookInk.caption, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

/** The prompts in one pack, dealt in one after another and settling as they scroll. */
@Composable
fun PromptPackScreen(id: String) {
    val router = LocalRouter.current
    val pack = JournalContent.pack(id)
    AppScreen(title = pack?.title ?: "Prompts", book = true, spacing = Space.s) {
        pack?.prompts?.forEachIndexed { index, prompt ->
            item(key = prompt) {
                CardSurface(
                    Modifier.dealIn(minOf(index, 8), baseDelay = 100, step = 50).scrollSettle(),
                    padding = Space.m,
                    onClick = { router.push(Route.JournalEditor(JournalStart.Prompt(prompt, pack.isDua))) },
                ) {
                    Text(prompt, style = TextStyles.body.copy(fontFamily = AppFont.serif), color = BookInk.ink)
                }
            }
        }
    }
}

// Settings

/**
 * Journal settings. Turning the lock on or off swaps an open and closed lock with one bounce,
 * and Export turns into Share with a small checkmark. The unlock cover appears at once and
 * only fades on the way out, so nothing shows while it locks.
 */
@Composable
fun JournalSettingsScreen() {
    val graph = LocalGraph.current
    val journal = graph.journal
    val sp = graph.prefs.sp
    val context = LocalContext.current
    val haptics = rememberHaptics()
    var hidePreviews by remember { mutableStateOf(sp.getBoolean(JournalPrefKey.hidePreviews, false)) }
    var exportFile by remember { mutableStateOf<File?>(null) }

    // Remove the plain text copy once she leaves this screen.
    DisposableEffect(Unit) {
        onDispose { exportFile?.delete() }
    }

    Box(Modifier.fillMaxSize()) {
        AppScreen(title = "Journal Settings", largeTitle = false, book = true) {
            item(key = "lock") {
                GroupedSection(Modifier.padding(top = Space.l)) {
                    LockRow(journal.lockEnabled) { on ->
                        if (on) {
                            // She is here already, so the journal stays open after locking it.
                            journal.isUnlocked = true
                            journal.lockEnabled = true
                            haptics.soft()
                        } else {
                            journalAuthenticate(context, "Turn off the journal lock") { ok ->
                                if (ok) { journal.lockEnabled = false; haptics.soft() }
                            }
                        }
                    }
                    RowDivider()
                    SwitchRow("Hide page previews", hidePreviews, {
                        hidePreviews = it
                        sp.edit().putBoolean(JournalPrefKey.hidePreviews, it).apply()
                    })
                }
            }
            item(key = "writeReminder") {
                com.quranforwomen.app.feature.reminders.GentleReminderSection(
                    com.quranforwomen.app.data.GentleReminder.journal, "Reminder to write",
                )
            }
            item(key = "export") {
                GroupedSection {
                    AnimatedContent(
                        targetState = exportFile,
                        transitionSpec = { fadeIn(Motion.gentle()) togetherWith fadeOut(Motion.gentle()) },
                        label = "export",
                    ) { file ->
                        if (file != null) {
                            ListRow(
                                title = "Share Export File",
                                icon = symbol("square.and.arrow.up"),
                                showsChevron = false,
                                modifier = Modifier.semantics { contentDescription = "Share Export File. Export is ready" },
                                trailing = {
                                    Icon(
                                        symbol("checkmark.circle.fill"), null, tint = BookInk.rule,
                                        modifier = Modifier.size(22.dp).journalBounceOnAppear(150),
                                    )
                                },
                                onClick = { context.shareFile(file, "text/plain") },
                            )
                        } else {
                            ListRow(
                                title = "Export Journal and Notes",
                                icon = symbol("doc.text"),
                                showsChevron = false,
                                onClick = {
                                    exportFile = journal.exportFile()
                                    if (exportFile != null) haptics.success()
                                },
                            )
                        }
                    }
                    RowDivider()
                    ListRow(title = "Pages", showsChevron = false, trailing = {
                        RollingNumber(journal.entries.size, TextStyles.body, BookInk.caption)
                    })
                    RowDivider(inset = Space.m)
                    ListRow(title = "Notes", showsChevron = false, trailing = {
                        RollingNumber(journal.notes.size, TextStyles.body, BookInk.caption)
                    })
                }
            }
        }

        // Appears at once when locking; only fades away once she has unlocked.
        AnimatedVisibility(
            visible = !journal.isAccessible,
            enter = EnterTransition.None,
            exit = fadeOut(Motion.smooth()),
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(BookInk.page)
                    .clickable(remember { MutableInteractionSource() }, indication = null) {},
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.m, Alignment.CenterVertically),
            ) {
                Icon(symbol("lock"), null, tint = BookInk.rule, modifier = Modifier.size(34.dp))
                TintedButton("Unlock to Change", {
                    journalAuthenticate(context, "Open journal settings") { ok -> if (ok) journal.isUnlocked = true }
                }, fullWidth = false)
            }
        }
    }
}

/** The lock switch. Its icon swaps between an open and a closed lock with one bounce. */
@Composable
private fun LockRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .rowPress { onChange(!enabled) }
            .padding(horizontal = Space.m, vertical = 12.dp)
            .semantics(mergeDescendants = true) { role = Role.Switch },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = enabled,
            transitionSpec = { (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut()) },
            label = "lock",
            modifier = Modifier.width(26.dp).journalBounceOn(enabled),
        ) { on ->
            Icon(journalIcon(if (on) "lock" else "lock.open"), null, tint = BookInk.rule, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(Space.m))
        Text("Lock with fingerprint or screen lock", style = TextStyles.body, color = BookInk.ink, modifier = Modifier.weight(1f))
        AppSwitch(enabled, onChange)
    }
}
