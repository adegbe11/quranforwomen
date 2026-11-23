package com.quranforwomen.app.feature.journal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.JournalEntry
import com.quranforwomen.app.data.JournalMood
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.feature.notes.NotesVerseDetector
import com.quranforwomen.app.nav.JournalStart
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.ui.components.AppSwitch
import com.quranforwomen.app.ui.components.AppTextField
import com.quranforwomen.app.ui.components.ArabicText
import com.quranforwomen.app.ui.components.CardSurface
import com.quranforwomen.app.ui.components.TextAction
import com.quranforwomen.app.ui.components.TintedButton
import com.quranforwomen.app.ui.components.TopBar
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.scrollSettle
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.BookInk
import com.quranforwomen.app.ui.theme.LocalInBook
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

/** Opens the right editor for how the page was started. */
@Composable
fun JournalEditorScreen(start: JournalStart) {
    val journal = LocalGraph.current.journal
    val router = LocalRouter.current
    when (start) {
        is JournalStart.Template -> {
            val template = JournalContent.template(start.templateId)
            if (template != null) TemplateFill(template) else LaunchedEffect(Unit) { router.pop() }
        }
        is JournalStart.Edit -> {
            val entry = remember(start.entryId) { journal.entry(start.entryId) }
            if (entry != null) JournalEditor(entry, prompt = null, isNew = false) else LaunchedEffect(Unit) { router.pop() }
        }
        is JournalStart.Prompt -> JournalEditor(JournalEntry(origin = "Prompt", isDua = start.isDua), start.text, isNew = true)
        is JournalStart.Verse -> JournalEditor(
            JournalEntry(verse = start.ref, origin = "From a verse"),
            "What does this verse say to you today?",
            isNew = true,
        )
        JournalStart.Dua -> JournalEditor(JournalEntry(mood = JournalMood.makingDua, isDua = true), "What are you asking Allah for?", isNew = true)
        JournalStart.Blank -> JournalEditor(JournalEntry(), prompt = null, isNew = true)
    }
}

/** A full screen page with Cancel and Done in the bar, set like a book. */
@Composable
internal fun JournalEditorFrame(
    title: String,
    background: Color,
    doneLabel: String = "Done",
    doneEnabled: Boolean,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalInBook provides true) {
        JournalPrivacyCover(Modifier.background(background)) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))) {
                TopBar(
                    title = title,
                    backgroundAlpha = 0f,
                    leading = { TextAction("Cancel", onCancel) },
                    actions = { TextAction(doneLabel, onDone, enabled = doneEnabled, bold = true) },
                )
                Box(Modifier.weight(1f)) { content() }
                bottomBar?.invoke()
            }
        }
    }
}

// Free writing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalEditor(initial: JournalEntry, prompt: String?, isNew: Boolean) {
    val graph = LocalGraph.current
    val journal = graph.journal
    val quran = graph.quran
    val router = LocalRouter.current
    val reduce = LocalReduceMotion.current
    var entry by rememberSaveable(saver = journalJsonSaver(JournalEntry.serializer())) { mutableStateOf(initial) }
    var tagsText by rememberSaveable { mutableStateOf(initial.tags.joinToString(", ")) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }
    val bodyFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        if (isNew && prompt != null) runCatching { bodyFocus.requestFocus() }
    }

    // The page deepens to a darker leaf on heavy days.
    val bg by animateColorAsState(
        if (entry.mood?.isHeavy == true) BookInk.pageEdge else BookInk.page,
        tween(600),
        label = "leaf",
    )

    fun save() {
        val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        journal.save(entry.copy(tags = tags))
        router.pop()
    }

    JournalEditorFrame(
        title = if (isNew) "New Page" else "Edit Page",
        background = bg,
        // Nothing can be saved while the journal is locked.
        doneEnabled = !entry.isEmpty && journal.isAccessible,
        onCancel = { router.pop() },
        onDone = ::save,
        bottomBar = if (journal.isAccessible) ({
            EditorBottomBar(
                isDua = entry.isDua,
                showDetails = showDetails,
                onAttachVerse = { entry = entry.copy(verse = graph.content.verseOfTheDay()) },
                onToggleDua = { entry = entry.copy(isDua = !entry.isDua) },
                onToggleDetails = { showDetails = !showDetails },
            )
        }) else null,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.page, vertical = Space.l),
            verticalArrangement = Arrangement.spacedBy(Space.l),
        ) {
            Masthead(entry.date, Modifier.focusIn(50)) { pickingDate = true }
            MoodRow(entry.mood) { mood ->
                entry = entry.copy(
                    mood = if (entry.mood == mood) null else mood,
                    isDua = if (mood == JournalMood.makingDua) true else entry.isDua,
                )
            }

            if (prompt != null) {
                Text(
                    prompt,
                    style = TextStyles.headline.copy(fontFamily = AppFont.serif),
                    color = BookInk.rule,
                    modifier = Modifier
                        .focusIn(200)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BookInk.gild.copy(alpha = 0.2f))
                        .padding(horizontal = Space.s, vertical = Space.xs),
                )
            }

            AppTextField(
                entry.title, { entry = entry.copy(title = it) }, "Title",
                style = TextStyles.title2.copy(fontFamily = AppFont.serif),
                filled = false,
                maxLines = 4,
            )
            AppTextField(
                entry.body, { entry = entry.copy(body = it) }, "Write",
                modifier = Modifier.focusRequester(bodyFocus),
                style = TextStyles.body.copy(fontFamily = AppFont.serif, lineHeight = 28.sp),
                filled = false,
                minLines = 10,
            )

            AnimatedVisibility(entry.verse != null, enter = fadeIn(Motion.gentle()), exit = fadeOut(Motion.gentle())) {
                val verse = entry.verse
                if (verse != null) AttachedVerse(verse, Modifier.scrollSettle()) { entry = entry.copy(verse = null) }
            }

            val mentioned = remember(entry.body, entry.verse) {
                NotesVerseDetector.find(entry.body, quran, limit = 3).filter { it != entry.verse }
            }
            if (mentioned.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text("Verses you mentioned", style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold), color = BookInk.caption)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        mentioned.forEach { ref ->
                            TintedButton("+ ${quran.surah(ref.surah).name} ${ref.id}", { entry = entry.copy(verse = ref) }, fullWidth = false)
                        }
                    }
                }
            }

            AnimatedVisibility(showDetails, enter = fadeIn(Motion.gentle()), exit = fadeOut(Motion.gentle())) {
                EditorDetails(
                    gratitude = entry.gratitude,
                    onGratitude = { entry = entry.copy(gratitude = it) },
                    tags = tagsText,
                    onTags = { tagsText = it },
                    isDua = entry.isDua,
                    onDua = { entry = entry.copy(isDua = it) },
                    favorite = entry.favorite,
                    onFavorite = { entry = entry.copy(favorite = it) },
                )
            }
        }
    }

    if (pickingDate) {
        EntryDatePicker(entry.date, onDismiss = { pickingDate = false }) { entry = entry.copy(date = it) }
    }
}

@Composable
private fun Masthead(date: Long, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val zoned = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault())
    val locale = Locale.getDefault()
    val full = zoned.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))
    Row(
        modifier
            .pressable(scale = 0.97f, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "Date: $full. Change date" },
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        AnimatedContent(
            targetState = zoned.dayOfMonth,
            transitionSpec = {
                val up = targetState > initialState
                (slideInVertically(spring(dampingRatio = 0.8f, stiffness = 500f)) { if (up) it else -it } + fadeIn()) togetherWith
                    (slideOutVertically(spring(dampingRatio = 0.8f, stiffness = 500f)) { if (up) -it else it } + fadeOut())
            },
            label = "day",
        ) { day ->
            Text(
                "%02d".format(day),
                style = TextStyles.largeTitle.copy(fontFamily = AppFont.serif, fontSize = 44.sp, lineHeight = 50.sp, fontWeight = FontWeight.Light),
                color = BookInk.ink,
            )
        }
        Column(Modifier.padding(bottom = 6.dp)) {
            Text(zoned.format(DateTimeFormatter.ofPattern("EEEE", locale)), style = TextStyles.subheadline, color = BookInk.caption)
            Text(zoned.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)), style = TextStyles.subheadline, color = BookInk.caption)
        }
        Icon(symbol("calendar"), null, tint = BookInk.rule, modifier = Modifier.padding(bottom = 8.dp).size(15.dp))
    }
}

/** The moods. The filled capsule glides from one to the next. */
@Composable
private fun MoodRow(selected: JournalMood?, onSelect: (JournalMood) -> Unit) {
    val haptics = rememberHaptics()
    val density = LocalDensity.current
    val reduce = LocalReduceMotion.current
    val positions = remember { mutableStateMapOf<JournalMood, Pair<Float, Float>>() }
    val x = remember { Animatable(0f) }
    val w = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val target = selected?.let { positions[it] }
    val gild = BookInk.gild

    LaunchedEffect(selected, target) {
        if (target == null) {
            alpha.animateTo(0f, tween(150))
            return@LaunchedEffect
        }
        if (alpha.value < 0.05f || reduce) {
            x.snapTo(target.first); w.snapTo(target.second)
            alpha.animateTo(1f, tween(180))
        } else {
            val spec = spring<Float>(dampingRatio = 0.75f, stiffness = 260f)
            launch { x.animateTo(target.first, spec) }
            launch { w.animateTo(target.second, spec) }
        }
    }

    Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
        Box(
            Modifier
                .offset { IntOffset(x.value.roundToInt(), 0) }
                .width(with(density) { w.value.toDp() })
                .heightIn(min = 36.dp)
                .height(36.dp)
                .graphicsLayer { this.alpha = alpha.value }
                .clip(CircleShape)
                .background(BookInk.rule)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            JournalMood.entries.forEachIndexed { index, mood ->
                val isSel = selected == mood
                val gildAlpha by androidx.compose.animation.core.animateFloatAsState(if (isSel) 0f else 1f, tween(200), label = "gild")
                val fg by animateColorAsState(if (isSel) BookInk.pageLit else BookInk.rule, tween(200), label = "fg")
                Row(
                    Modifier
                        .onPlaced { c -> positions[mood] = c.positionInParent().x to c.size.width.toFloat() }
                        .dealIn(index, baseDelay = 120, step = 40)
                        .height(36.dp)
                        .clip(CircleShape)
                        .drawBehind { drawRect(gild.copy(alpha = 0.2f * gildAlpha)) }
                        .pressable {
                            haptics.tick()
                            onSelect(mood)
                        }
                        .semantics { this.selected = isSel; role = Role.Button }
                        .padding(horizontal = Space.s),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(symbol(mood.symbolName), null, tint = fg, modifier = Modifier.size(16.dp).journalBounceOn(isSel))
                    Text(mood.title, style = TextStyles.footnote.copy(fontWeight = FontWeight.Medium), color = fg)
                }
            }
        }
    }
}

@Composable
private fun AttachedVerse(verse: VerseRef, modifier: Modifier = Modifier, onRemove: () -> Unit) {
    val quran = LocalGraph.current.quran
    CardSurface(modifier, padding = Space.m, fill = BookInk.gild.copy(alpha = 0.2f * 0.5f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(quran.displayName(verse), style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold), color = BookInk.rule, modifier = Modifier.weight(1f))
            Box(
                Modifier.size(44.dp).clip(CircleShape).pressable(scale = 0.9f, onClick = onRemove).semantics { contentDescription = "Remove verse" },
                contentAlignment = Alignment.Center,
            ) { Icon(symbol("xmark.circle.fill"), null, tint = BookInk.caption, modifier = Modifier.size(20.dp)) }
        }
        ArabicText(quran.text(verse), size = 22f, maxLines = 4)
        Spacer(Modifier.height(Space.xs))
        JournalTranslationBody(verse)
    }
}

@Composable
private fun EditorDetails(
    gratitude: String,
    onGratitude: (String) -> Unit,
    tags: String,
    onTags: (String) -> Unit,
    isDua: Boolean,
    onDua: (Boolean) -> Unit,
    favorite: Boolean,
    onFavorite: (Boolean) -> Unit,
) {
    CardSurface(Modifier.scrollSettle(), padding = Space.m) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            DetailField("Grateful for", gratitude, onGratitude, "One thing, big or small")
            DetailField("Tags", tags, onTags, "family, sabr, work")
            DetailToggle("Add to my duas", isDua, onDua)
            DetailToggle("Favorite", favorite, onFavorite)
        }
    }
}

@Composable
private fun DetailField(title: String, value: String, onChange: (String) -> Unit, placeholder: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold), color = BookInk.caption)
        AppTextField(value, onChange, placeholder, filled = false, maxLines = 4)
    }
}

@Composable
private fun DetailToggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).semantics(mergeDescendants = true) { role = Role.Switch },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = TextStyles.body, color = BookInk.ink, modifier = Modifier.weight(1f))
        AppSwitch(checked, onChange)
    }
}

@Composable
private fun EditorBottomBar(
    isDua: Boolean,
    showDetails: Boolean,
    onAttachVerse: () -> Unit,
    onToggleDua: () -> Unit,
    onToggleDetails: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(BookInk.pageLit.copy(alpha = 0.96f))) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(BookInk.rule.copy(alpha = 0.25f)))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.page, vertical = Space.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BarLabel("book", "Attach today's verse", bounceKey = null, onClick = onAttachVerse)
            BarLabel(
                if (isDua) "hands.and.sparkles.fill" else "hands.and.sparkles",
                if (isDua) "In my duas" else "Add to my duas",
                bounceKey = isDua,
                onClick = onToggleDua,
            )
            BarLabel(
                if (showDetails) "ellipsis.circle.more.fill" else "ellipsis.circle.more",
                "More details",
                bounceKey = null,
                onClick = onToggleDetails,
            )
        }
    }
}

@Composable
private fun BarLabel(icon: String, label: String, bounceKey: Any?, onClick: () -> Unit) {
    Row(
        Modifier
            .heightIn(min = 44.dp)
            .pressable(scale = 0.95f, onClick = onClick)
            .semantics(mergeDescendants = true) { role = Role.Button },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = { (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut()) },
            label = "barIcon",
            modifier = Modifier.journalBounceOn(bounceKey),
        ) { name -> Icon(journalIcon(name), null, tint = BookInk.rule, modifier = Modifier.size(20.dp)) }
        Text(label, style = TextStyles.footnote.copy(fontWeight = FontWeight.Medium), color = BookInk.rule, maxLines = 1)
    }
}

/** Pick the day of a page. Today or earlier; the time of day is kept. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EntryDatePicker(date: Long, onDismiss: () -> Unit, upToToday: Boolean = true, onPick: (Long) -> Unit) {
    val zone = ZoneId.systemDefault()
    val current = Instant.ofEpochMilli(date).atZone(zone)
    val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = current.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = !upToToday || utcTimeMillis <= todayUtc
            override fun isSelectableYear(year: Int) = !upToToday || year <= LocalDate.now().year
        },
    )
    val colors = DatePickerDefaults.colors(
        containerColor = BookInk.pageLit,
        titleContentColor = BookInk.caption,
        headlineContentColor = BookInk.ink,
        weekdayContentColor = BookInk.caption,
        dayContentColor = BookInk.ink,
        selectedDayContainerColor = BookInk.rule,
        selectedDayContentColor = BookInk.pageLit,
        todayContentColor = BookInk.rule,
        todayDateBorderColor = BookInk.rule,
        selectedYearContainerColor = BookInk.rule,
        selectedYearContentColor = BookInk.pageLit,
        yearContentColor = BookInk.ink,
        navigationContentColor = BookInk.rule,
        subheadContentColor = BookInk.caption,
        disabledDayContentColor = BookInk.caption.copy(alpha = 0.35f),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextAction("Done", {
                state.selectedDateMillis?.let { millis ->
                    val day = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    onPick(current.with(day).toInstant().toEpochMilli())
                }
                onDismiss()
            }, bold = true)
        },
        dismissButton = { TextAction("Cancel", onDismiss) },
        colors = colors,
    ) {
        DatePicker(state = state, colors = colors, showModeToggle = false)
    }
}

// Template page

/** A printed-journal page: one section per question. Empty sections are not saved. */
@Composable
private fun TemplateFill(template: JournalTemplate) {
    val graph = LocalGraph.current
    val journal = graph.journal
    val quran = graph.quran
    val router = LocalRouter.current
    val answers: SnapshotStateList<String> = rememberSaveable(
        saver = androidx.compose.runtime.saveable.listSaver(
            save = { it.toList() },
            restore = { it.toMutableStateList() },
        )
    ) { List(template.prompts.size) { "" }.toMutableStateList() }

    fun save() {
        val body = JournalContent.compose(template, answers)
        if (body.isEmpty()) {
            router.pop()
            return
        }
        journal.save(
            JournalEntry(
                title = template.title,
                body = body,
                mood = if (template.isDua) JournalMood.makingDua else null,
                verse = template.verse,
                origin = template.title,
                isDua = template.isDua,
            )
        )
        journal.markTemplateUsed(template.id)
        router.pop()
    }

    JournalEditorFrame(
        title = "",
        background = BookInk.page,
        doneEnabled = journal.isAccessible,
        onCancel = { router.pop() },
        onDone = ::save,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Space.m),
        ) {
            Column(
                Modifier
                    .dealIn(0, baseDelay = 50)
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(8.dp), ambientColor = Color.Black.copy(alpha = 0.06f), spotColor = Color.Black.copy(alpha = 0.06f))
                    .clip(RoundedCornerShape(8.dp))
                    .background(BookInk.pageLit)
                    .padding(Space.xl),
                verticalArrangement = Arrangement.spacedBy(Space.xl),
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text(
                        "✦ ${template.title} ✦",
                        style = TextStyles.title2.copy(fontFamily = AppFont.serif, fontStyle = FontStyle.Italic),
                        color = BookInk.ink,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
                        style = TextStyles.subheadline,
                        color = BookInk.caption,
                    )
                }
                template.verse?.let { verse ->
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                        ArabicText(quran.text(verse), size = 21f, center = true)
                        JournalTranslationBody(verse)
                        Text(quran.displayName(verse), style = TextStyles.caption.copy(fontWeight = FontWeight.SemiBold), color = BookInk.rule)
                    }
                }
                template.prompts.forEachIndexed { i, p ->
                    TemplateSection(
                        p,
                        answers.getOrElse(i) { "" },
                        { if (i in answers.indices) answers[i] = it },
                        Modifier.dealIn(i, baseDelay = 350, step = 80),
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateSection(prompt: TemplatePrompt, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val (min, max) = when (prompt.style) {
        TemplatePrompt.Style.field -> 1 to 3
        TemplatePrompt.Style.list -> 3 to 12
        TemplatePrompt.Style.lines -> 3 to 20
    }
    val rule = BookInk.rule.copy(alpha = 0.35f)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Text(
            prompt.heading.uppercase(),
            style = TextStyles.caption.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
            color = BookInk.rule,
        )
        Text(prompt.question, style = TextStyles.subheadline.copy(fontFamily = AppFont.serif), color = BookInk.caption)
        AppTextField(
            value, onChange,
            placeholder = if (prompt.style == TemplatePrompt.Style.list) "One per line" else "",
            modifier = Modifier
                .drawBehind {
                    drawLine(rule, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
                }
                .padding(vertical = Space.xs),
            style = TextStyles.body.copy(fontFamily = AppFont.serif),
            filled = false,
            minLines = min,
            maxLines = max,
        )
    }
}
