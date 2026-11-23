package com.quranforwomen.app.feature.notes

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.JournalEntry
import com.quranforwomen.app.data.LessonNote
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.data.arabicIndic
import com.quranforwomen.app.feature.journal.EntryDatePicker
import com.quranforwomen.app.feature.journal.JournalEditorFrame
import com.quranforwomen.app.feature.journal.JournalFormSection
import com.quranforwomen.app.feature.journal.JournalGold
import com.quranforwomen.app.feature.journal.JournalMarkdownBody
import com.quranforwomen.app.feature.journal.JournalMenu
import com.quranforwomen.app.feature.journal.JournalMenuDivider
import com.quranforwomen.app.feature.journal.JournalMenuItem
import com.quranforwomen.app.feature.journal.JournalPrivacyCover
import com.quranforwomen.app.feature.journal.JournalSearchEmpty
import com.quranforwomen.app.feature.journal.JournalTranslationBody
import com.quranforwomen.app.feature.journal.journalBounceOn
import com.quranforwomen.app.feature.journal.journalBounceOnAppear
import com.quranforwomen.app.feature.journal.journalIcon
import com.quranforwomen.app.feature.journal.journalJsonSaver
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.AppAlert
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.AppTextField
import com.quranforwomen.app.ui.components.ArabicText
import com.quranforwomen.app.ui.components.BarIcon
import com.quranforwomen.app.ui.components.CardSurface
import com.quranforwomen.app.ui.components.PrimaryButton
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.SavedEmpty
import com.quranforwomen.app.ui.components.SearchField
import com.quranforwomen.app.ui.components.TextAction
import com.quranforwomen.app.ui.components.TintedButton
import com.quranforwomen.app.ui.components.TopBar
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.hasPermission
import com.quranforwomen.app.ui.components.openAppSettings
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.rowPress
import com.quranforwomen.app.ui.components.scrollSettle
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sin

// Shared bits

internal val LessonNote.Kind.symbolName: String
    get() = when (this) {
        LessonNote.Kind.khutbah -> "building.columns"
        LessonNote.Kind.halaqa -> "person.3"
        LessonNote.Kind.classNote -> "graduationcap"
        LessonNote.Kind.lecture -> "mic"
        LessonNote.Kind.study -> "book"
    }

/** A note arranged from a recording, handed to the editor before it is saved. Memory only. */
private object NotesDrafts {
    private val drafts = mutableMapOf<String, LessonNote>()
    fun put(note: LessonNote) { drafts[note.id] = note }
    fun take(id: String): LessonNote? = drafts.remove(id)
}

private fun Long.formatted(style: FormatStyle): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDate(style))

// Notes home (inside the Journal tab)

/** The + menu for notes. */
@Composable
internal fun NotesHomeActions() {
    val router = LocalRouter.current
    var open by remember { mutableStateOf(false) }
    Box {
        BarIcon(symbol("plus"), "New note") { open = true }
        JournalMenu(open, { open = false }) {
            JournalMenuItem("Record a Lesson", symbol("mic"), onClick = {
                open = false
                router.push(Route.LessonCapture(record = true))
            })
            JournalMenuItem("Write Notes", symbol("square.and.pencil"), onClick = {
                open = false
                router.push(Route.LessonEdit(UUID.randomUUID().toString(), isNew = true))
            })
            JournalMenuDivider()
            JournalMenuItem("Journal Settings", symbol("gearshape"), onClick = {
                open = false
                router.push(Route.JournalSettings)
            })
        }
    }
}

/**
 * Lesson notes. The empty state comes into focus with a small bounce of the waveform; saved
 * notes are dealt in one after another, settle as they scroll, and reflow softly while searching.
 */
internal fun LazyListScope.notesHomeItems(
    notes: List<LessonNote>,
    hasNotes: Boolean,
    query: String,
    onQuery: (String) -> Unit,
) {
    item(key = "notes-search") {
        SearchField(query, onQuery, "Search notes", Modifier.padding(bottom = Space.xs))
    }
    if (!hasNotes) {
        item(key = "notes-empty") {
            val router = LocalRouter.current
            Column(
                Modifier.fillMaxWidth().padding(top = Space.xl).focusIn(50),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                Icon(symbol("waveform"), null, tint = BookInk.rule, modifier = Modifier.size(36.dp).journalBounceOnAppear(400))
                Text("No notes yet", style = TextStyles.title3.copy(fontFamily = AppFont.serif), color = BookInk.ink)
                PrimaryButton(
                    "Record a Lesson", { router.push(Route.LessonCapture(record = true)) },
                    icon = symbol("mic.fill"), modifier = Modifier.padding(top = Space.xs),
                )
                TextAction("Write Notes Instead", { router.push(Route.LessonEdit(UUID.randomUUID().toString(), isNew = true)) })
            }
        }
        return
    }
    val q = query.trim()
    val filtered = if (q.isEmpty()) notes else notes.filter { n ->
        (listOf(n.title, n.speaker, n.takeaway, n.notes, n.transcript, n.kind.title) + n.points + n.quotes)
            .any { it.contains(q, ignoreCase = true) }
    }
    if (filtered.isEmpty()) {
        item(key = "notes-none") { JournalSearchEmpty(query) }
        return
    }
    filtered.forEachIndexed { index, note ->
        item(key = "note-${note.id}") {
            val router = LocalRouter.current
            NoteCard(
                note,
                Modifier.animateItem().dealIn(minOf(index, 8), baseDelay = 120, step = 50).scrollSettle(),
            ) { router.push(Route.LessonNote(note.id)) }
        }
    }
}

@Composable
private fun NoteCard(note: LessonNote, modifier: Modifier = Modifier, onClick: () -> Unit) {
    CardSurface(modifier, padding = Space.m, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val style = TextStyles.caption
            Icon(symbol(note.kind.symbolName), null, tint = BookInk.rule, modifier = Modifier.size(14.dp))
            Text(note.kind.title, style = style, color = BookInk.rule)
            if (note.pinned) Icon(symbol("pin.fill"), "Pinned", tint = BookInk.rule, modifier = Modifier.size(13.dp))
            Spacer(Modifier.weight(1f))
            Text(note.date.formatted(FormatStyle.MEDIUM), style = style, color = BookInk.rule)
        }
        Spacer(Modifier.height(Space.xs))
        Text(note.displayTitle, style = TextStyles.headline, color = BookInk.ink)
        val meta = buildList {
            if (note.speaker.isNotEmpty()) add(note.speaker)
            if (note.minutes > 0) add("${note.minutes} min")
            if (note.verses.isNotEmpty()) add(if (note.verses.size == 1) "1 verse" else "${note.verses.size} verses")
        }.joinToString(" · ")
        if (meta.isNotEmpty()) {
            Spacer(Modifier.height(Space.xs))
            Text(meta, style = TextStyles.footnote, color = BookInk.caption)
        }
        if (note.takeaway.isNotEmpty()) {
            Spacer(Modifier.height(Space.xs))
            Text(
                note.takeaway,
                style = TextStyles.subheadline.copy(fontFamily = AppFont.serif),
                color = BookInk.caption,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// Recording

private enum class CaptureStage { ready, listening, arranging }

/** Recording or typing a new lesson note. */
@Composable
fun LessonCaptureScreen(record: Boolean) {
    if (!record) {
        val id = rememberSaveable { UUID.randomUUID().toString() }
        LessonEditScreen(id, isNew = true)
        return
    }
    // The screen stays awake while a talk is recorded.
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    LessonRecord()
}

/**
 * Recording a lesson. Each stage crossfades; while listening a ring pulses out from the mic,
 * the timer rolls its digits and the verse count ticks up as verses are heard.
 */
@Composable
private fun LessonRecord() {
    val graph = LocalGraph.current
    val quran = graph.quran
    val router = LocalRouter.current
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val reduce = LocalReduceMotion.current
    val scope = rememberCoroutineScope()
    val recorder = remember { NotesRecorder(context.applicationContext) }
    var stage by remember { mutableStateOf(CaptureStage.ready) }
    var kind by rememberSaveable { mutableStateOf(LessonNote.Kind.khutbah) }
    var speaker by rememberSaveable { mutableStateOf("") }

    DisposableEffect(Unit) {
        onDispose { if (recorder.state == NotesRecorder.State.recording) recorder.stop() }
    }
    LaunchedEffect(stage) { if (stage != CaptureStage.ready) haptics.soft() }

    fun begin() {
        recorder.start()
        if (recorder.state == NotesRecorder.State.recording) stage = CaptureStage.listening
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) begin() else recorder.markDenied()
    }

    fun finish() {
        val minutes = recorder.elapsedMinutes
        val transcript = recorder.stop()
        stage = CaptureStage.arranging
        scope.launch {
            delay(900)
            val (verses, arranged) = withContext(Dispatchers.Default) {
                val v = NotesVerseDetector.find(transcript, quran)
                v to NotesArranger.arrange(transcript, v) { quran.surah(it).name }
            }
            val note = LessonNote(
                kind = kind,
                title = arranged.title,
                speaker = speaker.trim(),
                date = System.currentTimeMillis(),
                verses = verses,
                points = arranged.points,
                quotes = arranged.quotes,
                takeaway = arranged.takeaway,
                transcript = transcript,
                minutes = minutes,
            )
            NotesDrafts.put(note)
            router.pop()
            router.push(Route.LessonEdit(note.id, isNew = true))
        }
    }

    JournalPrivacyCover(Modifier.background(BookInk.leaf)) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                title = "",
                backgroundAlpha = 0f,
                leading = {
                    TextAction(if (stage == CaptureStage.listening) "Discard" else "Cancel", {
                        recorder.stop()
                        router.pop()
                    })
                },
            )
            AnimatedContent(
                targetState = stage,
                transitionSpec = {
                    if (reduce) fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                    else fadeIn(Motion.smooth()) togetherWith fadeOut(Motion.smooth())
                },
                label = "stage",
                modifier = Modifier.weight(1f),
            ) { s ->
                when (s) {
                    CaptureStage.ready -> RecordReady(
                        kind = kind,
                        onKind = { kind = it },
                        speaker = speaker,
                        onSpeaker = { speaker = it },
                        state = recorder.state,
                    ) {
                        // Recording takes the microphone, so stop any recitation first.
                        graph.player.stop()
                        if (context.hasPermission(Manifest.permission.RECORD_AUDIO)) begin()
                        else permission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    CaptureStage.listening -> RecordListening(recorder, onStop = ::finish)
                    CaptureStage.arranging -> Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Space.m, Alignment.CenterVertically),
                    ) {
                        CircularProgressIndicator(color = BookInk.rule, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
                        Text("Arranging your notes", style = TextStyles.body, color = BookInk.caption)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordReady(
    kind: LessonNote.Kind,
    onKind: (LessonNote.Kind) -> Unit,
    speaker: String,
    onSpeaker: (String) -> Unit,
    state: NotesRecorder.State,
    onStart: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.xl),
    ) {
        Icon(
            symbol("waveform"), null, tint = BookInk.rule,
            modifier = Modifier.padding(top = Space.xl).focusIn(50).size(44.dp).journalBounceOnAppear(350),
        )
        Text(
            "Record a lesson",
            style = TextStyles.title.copy(fontFamily = AppFont.serif, fontWeight = FontWeight.SemiBold),
            color = BookInk.ink,
            modifier = Modifier.focusIn(100).semantics { heading() },
        )
        CardSurface(Modifier.dealIn(0, baseDelay = 200).scrollSettle(), padding = Space.m) {
            KindPicker(kind, onKind)
            Spacer(Modifier.height(Space.m))
            AppTextField(
                speaker, onSpeaker, "Speaker (optional)",
                filled = false,
                capitalization = KeyboardCapitalization.Words,
            )
        }
        when (state) {
            NotesRecorder.State.denied -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                Text("Microphone access is off.", style = TextStyles.footnote, color = BookInk.ribbon, textAlign = TextAlign.Center)
                TintedButton("Open Settings", { context.openAppSettings() }, fullWidth = false)
            }
            NotesRecorder.State.unavailable -> Text(
                "Speech recognition is unavailable.",
                style = TextStyles.footnote, color = BookInk.ribbon, textAlign = TextAlign.Center,
            )
            else -> Unit
        }
        PrimaryButton("Start Recording", onStart, icon = symbol("mic.fill"), modifier = Modifier.dealIn(1, baseDelay = 200))
    }
}

/** "Type" with the chosen kind, opening a menu of kinds. */
@Composable
private fun KindPicker(kind: LessonNote.Kind, onKind: (LessonNote.Kind) -> Unit, withIcons: Boolean = true) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).pressable { open = true }
                .semantics(mergeDescendants = true) { contentDescription = "Type, ${kind.title}" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Type", style = TextStyles.body, color = BookInk.ink, modifier = Modifier.weight(1f))
            if (withIcons) {
                Icon(symbol(kind.symbolName), null, tint = BookInk.rule, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(kind.title, style = TextStyles.body, color = BookInk.rule)
            Icon(symbol("chevron.up.chevron.down"), null, tint = BookInk.rule, modifier = Modifier.padding(start = 4.dp).size(16.dp))
        }
        JournalMenu(open, { open = false }) {
            LessonNote.Kind.entries.forEach { k ->
                JournalMenuItem(k.title, if (withIcons) symbol(k.symbolName) else null, checked = k == kind, onClick = {
                    onKind(k)
                    open = false
                })
            }
        }
    }
}

@Composable
private fun RecordListening(recorder: NotesRecorder, onStop: () -> Unit) {
    val quran = LocalGraph.current.quran
    val scroll = rememberScrollState()
    val transcript = recorder.transcript
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    LaunchedEffect(transcript) { scroll.animateScrollTo(scroll.maxValue, tween(200)) }
    val verses = remember(transcript) { NotesVerseDetector.find(transcript, quran).size }
    val seconds = recorder.startedAt?.let { ((now - it) / 1000).coerceAtLeast(0) } ?: 0L
    val elapsed = "%d:%02d".format(seconds / 60, seconds % 60)

    Column(Modifier.fillMaxSize()) {
        Text(
            if (transcript.isEmpty()) "Listening…" else transcript,
            style = TextStyles.title3.copy(fontFamily = AppFont.serif, fontWeight = FontWeight.Normal, lineHeight = 31.sp),
            color = if (transcript.isEmpty()) BookInk.caption else BookInk.ink,
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(Space.xl),
        )
        Column(Modifier.fillMaxWidth().background(BookInk.pageLit.copy(alpha = 0.96f))) {
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(BookInk.rule.copy(alpha = 0.25f)))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.xl, vertical = Space.m),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                RecordingPulse()
                AnimatedContent(
                    targetState = elapsed,
                    transitionSpec = {
                        (slideInVertically(spring(dampingRatio = 0.8f, stiffness = 500f)) { it } + fadeIn()) togetherWith
                            (slideOutVertically(spring(dampingRatio = 0.8f, stiffness = 500f)) { -it } + fadeOut())
                    },
                    label = "timer",
                ) { t -> Text(t, style = TextStyles.headline.copy(fontFeatureSettings = "tnum"), color = BookInk.ink) }
                LiveBars()
                AnimatedVisibility(verses > 0, enter = scaleIn(initialScale = 0.8f) + fadeIn(), exit = scaleOut(targetScale = 0.8f) + fadeOut()) {
                    AnimatedContent(verses, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "verses") { n ->
                        Text(if (n == 1) "· 1 verse" else "· $n verses", style = TextStyles.subheadline, color = BookInk.rule)
                    }
                }
                Spacer(Modifier.weight(1f))
                TintedButton("Stop", onStop, fullWidth = false)
            }
        }
    }
}

/** Gently moving bars while a lesson is being written out. */
@Composable
private fun LiveBars() {
    val reduce = LocalReduceMotion.current
    var t by remember { mutableStateOf(0.0) }
    LaunchedEffect(reduce) {
        if (reduce) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { frame -> t = (frame - start) / 1_000_000_000.0 }
            delay(50)
        }
    }
    Row(
        Modifier.height(20.dp).semantics { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        for (i in 0 until 5) {
            val h = 6 + 12 * abs(sin(t * 3.1 + i * 0.9))
            Box(Modifier.width(3.dp).height(h.dp).clip(CircleShape).background(BookInk.rule))
        }
    }
}

/** A red recording dot with a soft ring that keeps pulsing outward. Still with animations off. */
@Composable
private fun RecordingPulse() {
    val reduce = LocalReduceMotion.current
    val transition = rememberInfiniteTransition(label = "pulse")
    val p by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = androidx.compose.animation.core.LinearOutSlowInEasing), RepeatMode.Restart),
        label = "ring",
    )
    val red = Color(0xFFFF3B30)
    Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
        val scale = if (reduce) 1f else 1f + 1.2f * p
        val alpha = if (reduce) 0.35f else 0.8f * (1f - p)
        Box(
            Modifier
                .size(12.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                .border(2.dp, red.copy(alpha = 0.6f), CircleShape)
        )
        Box(Modifier.size(18.dp).clip(CircleShape).background(red), contentAlignment = Alignment.Center) {
            Icon(symbol("mic.fill"), null, tint = Color.White, modifier = Modifier.size(11.dp))
        }
    }
}

// Note view

/**
 * One lesson note. The heading comes into focus, verses, points and lines are dealt in, the
 * takeaway card rises last, and Save to Journal turns into a checkmark with a bounce.
 */
@Composable
fun LessonNoteScreen(id: String) {
    val graph = LocalGraph.current
    val journal = graph.journal
    val router = LocalRouter.current
    val context = LocalContext.current
    val haptics = rememberHaptics()
    var showTranscript by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var savedToJournal by rememberSaveable { mutableStateOf(false) }
    val note = journal.note(id)

    JournalPrivacyCover {
        AppScreen(
            title = "",
            largeTitle = false,
            book = true,
            spacing = Space.xl,
            actions = {
                // Nothing in the note can be shared or changed while the journal is locked.
                if (note != null && journal.isAccessible) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .pressable(scale = 0.9f) { journal.save(note.copy(pinned = !note.pinned)) }
                            .semantics { contentDescription = if (note.pinned) "Unpin" else "Pin" },
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedContent(
                            targetState = note.pinned,
                            transitionSpec = { (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut()) },
                            label = "pin",
                            modifier = Modifier.journalBounceOn(note.pinned),
                        ) { pinned -> Icon(journalIcon(if (pinned) "pin.fill" else "pin"), null, tint = Ink.accent, modifier = Modifier.size(22.dp)) }
                    }
                    BarIcon(symbol("square.and.arrow.up"), "Share") { context.shareText(note.plainText) }
                    var more by remember { mutableStateOf(false) }
                    Box {
                        BarIcon(symbol("ellipsis.circle"), "More") { more = true }
                        JournalMenu(more, { more = false }) {
                            JournalMenuItem("Edit", symbol("pencil"), onClick = {
                                more = false
                                router.push(Route.LessonEdit(note.id, isNew = false))
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
            if (note == null) {
                item(key = "missing") { SavedEmpty(symbol("doc"), "Note not found") }
                return@AppScreen
            }
            item(key = "head") {
                Column(Modifier.padding(top = Space.l).focusIn(50), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(symbol(note.kind.symbolName), null, tint = BookInk.rule, modifier = Modifier.size(15.dp))
                        Text(note.kind.title, style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold), color = BookInk.rule)
                    }
                    Text(
                        note.displayTitle,
                        style = TextStyles.title.copy(fontFamily = AppFont.serif, fontWeight = FontWeight.SemiBold),
                        color = BookInk.ink,
                        modifier = Modifier.semantics { heading() },
                    )
                    val meta = buildList {
                        if (note.speaker.isNotEmpty()) add(note.speaker)
                        add(note.date.formatted(FormatStyle.LONG))
                        if (note.minutes > 0) add("${note.minutes} min")
                    }.joinToString(" · ")
                    Text(meta, style = TextStyles.subheadline, color = BookInk.caption)
                }
            }
            if (note.verses.isNotEmpty()) {
                item(key = "verses-title") { NoteSectionTitle("Verses mentioned") }
                note.verses.forEachIndexed { index, ref ->
                    item(key = "verse-${ref.id}") {
                        NoteVerseCard(ref, Modifier.dealIn(minOf(index, 8), baseDelay = 150, step = 70).scrollSettle())
                    }
                }
            }
            if (note.points.isNotEmpty()) {
                item(key = "points") {
                    Column(Modifier.scrollSettle(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                        NoteSectionTitle("The main points")
                        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
                            note.points.forEachIndexed { index, point ->
                                Row(Modifier.dealIn(minOf(index, 8), baseDelay = 200, step = 50), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                                    Text("${index + 1}", style = TextStyles.headline.copy(fontFeatureSettings = "tnum"), color = JournalGold.number)
                                    Text(point, style = TextStyles.body.copy(fontFamily = AppFont.serif), color = BookInk.ink)
                                }
                            }
                        }
                    }
                }
            }
            if (note.quotes.isNotEmpty()) {
                item(key = "quotes") {
                    Column(Modifier.scrollSettle(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                        NoteSectionTitle("Lines worth keeping")
                        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
                            note.quotes.forEachIndexed { index, quote ->
                                Row(
                                    Modifier.dealIn(minOf(index, 8), baseDelay = 250, step = 50).height(androidx.compose.foundation.layout.IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                                ) {
                                    Box(Modifier.width(3.dp).fillMaxHeight().background(JournalGold.mark))
                                    Text(quote, style = TextStyles.body.copy(fontFamily = AppFont.serif, fontStyle = FontStyle.Italic), color = BookInk.ink)
                                }
                            }
                        }
                    }
                }
            }
            if (note.takeaway.isNotEmpty()) {
                item(key = "takeaway") {
                    Column(
                        Modifier
                            .dealIn(0, baseDelay = 300)
                            .scrollSettle()
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Radius.card))
                            .background(BookInk.rule)
                            .padding(Space.l),
                        verticalArrangement = Arrangement.spacedBy(Space.xs),
                    ) {
                        Text("Carry this into my week", style = TextStyles.subheadline.copy(fontWeight = FontWeight.SemiBold), color = BookInk.pageLit.copy(alpha = 0.85f))
                        Text(note.takeaway, style = TextStyles.title3.copy(fontFamily = AppFont.serif, fontWeight = FontWeight.Normal), color = BookInk.pageLit)
                    }
                }
            }
            if (note.notes.isNotEmpty()) {
                item(key = "notes") {
                    Column(Modifier.focusIn(350), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                        NoteSectionTitle("My notes")
                        JournalMarkdownBody(note.notes)
                    }
                }
            }
            if (note.transcript.isNotEmpty()) {
                item(key = "transcript") {
                    Column {
                        val turn by animateFloatAsState(if (showTranscript) 90f else 0f, Motion.gentle(), label = "chevron")
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 44.dp).pressable { showTranscript = !showTranscript }
                                .semantics(mergeDescendants = true) { },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Transcript", style = TextStyles.body, color = BookInk.ink, modifier = Modifier.weight(1f))
                            Icon(
                                journalIcon("chevron.right"), null, tint = BookInk.rule,
                                modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = turn },
                            )
                        }
                        AnimatedVisibility(showTranscript, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                            androidx.compose.foundation.text.selection.SelectionContainer {
                                Text(note.transcript, style = TextStyles.callout, color = BookInk.caption, modifier = Modifier.padding(top = Space.s))
                            }
                        }
                    }
                }
            }
            item(key = "save") {
                AnimatedContent(
                    targetState = savedToJournal,
                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.96f)) togetherWith fadeOut() },
                    label = "saved",
                    modifier = Modifier.journalBounceOn(savedToJournal),
                ) { saved ->
                    TintedButton(
                        if (saved) "Saved to Journal" else "Save to Journal",
                        {
                            saveToJournal(note, journal::save)
                            savedToJournal = true
                            haptics.success()
                        },
                        enabled = !saved,
                        icon = symbol(if (saved) "checkmark" else "book.closed"),
                    )
                }
            }
        }
    }

    if (note != null && confirmDelete) {
        AppAlert(
            title = "Delete this note?",
            confirmLabel = "Delete Note",
            destructive = true,
            onConfirm = {
                journal.delete(note)
                router.pop()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

private fun saveToJournal(note: LessonNote, save: (JournalEntry) -> Unit) {
    var body = ""
    if (note.points.isNotEmpty()) {
        body += "## Main points\n" + note.points.joinToString("\n") { "- $it" }
    }
    if (note.takeaway.isNotEmpty()) {
        body += (if (body.isEmpty()) "" else "\n\n") + "## Carry this into my week\n${note.takeaway}"
    }
    save(
        JournalEntry(
            date = note.date,
            title = note.displayTitle,
            body = body,
            verse = note.verses.firstOrNull(),
            origin = "From a ${note.kind.title.lowercase()}",
        )
    )
}

@Composable
private fun NoteSectionTitle(title: String) {
    Text(title, style = TextStyles.headline, color = BookInk.ink, modifier = Modifier.semantics { heading() })
}

@Composable
private fun NoteVerseCard(ref: VerseRef, modifier: Modifier = Modifier) {
    val graph = LocalGraph.current
    val quran = graph.quran
    val router = LocalRouter.current
    val saved = graph.library.isBookmarked(ref)
    CardSurface(modifier, onClick = { router.push(Route.Reader(ref.surah, ref.ayah)) }) {
        ArabicText("${quran.text(ref)} ﴿${ref.ayah.arabicIndic}﴾", size = 22f)
        Spacer(Modifier.height(Space.m))
        JournalTranslationBody(ref)
        Spacer(Modifier.height(Space.m))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(quran.displayName(ref), style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold), color = Ink.accent, modifier = Modifier.weight(1f))
            if (saved) Icon(symbol("bookmark.fill"), "Saved", tint = Ink.warm, modifier = Modifier.size(15.dp))
        }
    }
}

// Note editor

/**
 * Editing a lesson note. The form comes into focus, and each verse, whether found in the talk
 * or added by hand, pops in as a small chip with a book that bounces once.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LessonEditScreen(id: String, isNew: Boolean) {
    val graph = LocalGraph.current
    val journal = graph.journal
    val quran = graph.quran
    val router = LocalRouter.current
    val haptics = rememberHaptics()
    val initial = remember(id) { NotesDrafts.take(id) ?: journal.note(id) ?: LessonNote(id = id) }
    var note by rememberSaveable(saver = journalJsonSaver(LessonNote.serializer())) { mutableStateOf(initial) }
    var verseInput by rememberSaveable { mutableStateOf("") }
    var pickingDate by remember { mutableStateOf(false) }

    val parsed = VerseRef.parse(verseInput.trim())
    val isValidVerse = parsed != null && quran.contains(parsed)
    fun addVerse() {
        val ref = VerseRef.parse(verseInput.trim()) ?: return
        if (!quran.contains(ref)) return
        if (ref !in note.verses) {
            note = note.copy(verses = note.verses + ref)
            haptics.tick()
        }
        verseInput = ""
    }

    JournalEditorFrame(
        title = if (isNew) "New Note" else "Edit Note",
        background = BookInk.page,
        doneLabel = "Save",
        doneEnabled = journal.isAccessible,
        onCancel = { router.pop() },
        onDone = {
            journal.save(
                note.copy(
                    points = note.points.filter { it.isNotBlank() },
                    quotes = note.quotes.filter { it.isNotBlank() },
                )
            )
            router.pop()
        },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.page, vertical = Space.m)
                .focusIn(50),
            verticalArrangement = Arrangement.spacedBy(Space.l),
        ) {
            JournalFormSection(null) {
                Box(Modifier.padding(horizontal = Space.m)) { KindPicker(note.kind, { note = note.copy(kind = it) }, withIcons = false) }
                RowDivider(inset = Space.m)
                FormField(note.title, { note = note.copy(title = it) }, "Title")
                RowDivider(inset = Space.m)
                FormField(note.speaker, { note = note.copy(speaker = it) }, "Speaker", capitalization = KeyboardCapitalization.Words)
                RowDivider(inset = Space.m)
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).rowPress { pickingDate = true }.padding(horizontal = Space.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Date", style = TextStyles.body, color = BookInk.ink, modifier = Modifier.weight(1f))
                    Text(
                        note.date.formatted(FormatStyle.MEDIUM),
                        style = TextStyles.body,
                        color = BookInk.rule,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(BookInk.gild.copy(alpha = 0.2f)).padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }

            JournalFormSection("Verses mentioned") {
                if (note.verses.isNotEmpty()) {
                    FlowRow(
                        Modifier.fillMaxWidth().padding(Space.m),
                        horizontalArrangement = Arrangement.spacedBy(Space.xs),
                        verticalArrangement = Arrangement.spacedBy(Space.xs),
                    ) {
                        note.verses.forEachIndexed { index, ref ->
                            VerseChip(
                                "${quran.surah(ref.surah).name} · ${ref.id}",
                                index,
                            ) { note = note.copy(verses = note.verses.filter { it != ref }) }
                        }
                    }
                    RowDivider(inset = Space.m)
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = Space.m), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).padding(vertical = Space.m)) {
                        VerseInputField(verseInput, { verseInput = it }, onSubmit = ::addVerse)
                    }
                    TextAction("Add", ::addVerse, enabled = isValidVerse)
                }
            }

            EditableList("The main points", note.points, "A point") { note = note.copy(points = it) }
            EditableList("Lines worth keeping", note.quotes, "A line") { note = note.copy(quotes = it) }

            JournalFormSection("Carry this into my week") {
                FormField(note.takeaway, { note = note.copy(takeaway = it) }, "One thing to act on", multiline = true)
            }
            JournalFormSection("My notes") {
                FormField(note.notes, { note = note.copy(notes = it) }, "Anything else", multiline = true, minLines = 4)
            }
        }
    }

    if (pickingDate) {
        EntryDatePicker(note.date, onDismiss = { pickingDate = false }, upToToday = false) { note = note.copy(date = it) }
    }
}

@Composable
private fun FormField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    multiline: Boolean = false,
    minLines: Int = 1,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
) {
    AppTextField(
        value, onChange, placeholder,
        modifier = Modifier.padding(Space.m),
        filled = false,
        minLines = minLines,
        maxLines = if (multiline) Int.MAX_VALUE else 1,
        imeAction = if (multiline) ImeAction.Default else ImeAction.Done,
        capitalization = capitalization,
    )
}

@Composable
private fun VerseInputField(value: String, onChange: (String) -> Unit, onSubmit: () -> Unit) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = TextStyles.body.copy(color = BookInk.ink),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(BookInk.rule),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onSubmit() }),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text("Add a verse (2:255)", style = TextStyles.body, color = BookInk.caption.copy(alpha = 0.7f))
                inner()
            }
        },
    )
}

@Composable
private fun VerseChip(label: String, index: Int, onRemove: () -> Unit) {
    Row(
        Modifier
            .popIn(150 + minOf(index, 8) * 60)
            .clip(CircleShape)
            .background(BookInk.gild.copy(alpha = 0.2f))
            .padding(start = Space.s, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Icon(
            symbol("book.closed"), null, tint = BookInk.rule,
            modifier = Modifier.size(14.dp).journalBounceOnAppear(300 + minOf(index, 8) * 60),
        )
        Text(label, style = TextStyles.subheadline, color = BookInk.ink)
        Box(
            Modifier.size(32.dp).clip(CircleShape).pressable(scale = 0.9f, onClick = onRemove).semantics { contentDescription = "Remove $label" },
            contentAlignment = Alignment.Center,
        ) { Icon(symbol("xmark.circle.fill"), null, tint = BookInk.caption, modifier = Modifier.size(16.dp)) }
    }
}

/** A small chip pops in from a little smaller with a light overshoot. Only fades with animations off. */
private fun Modifier.popIn(delayMillis: Int): Modifier = composed {
    val reduce = LocalReduceMotion.current
    var shown by rememberSaveable { mutableStateOf(false) }
    val p = remember { Animatable(if (shown) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (p.value < 1f) {
            if (reduce) p.animateTo(1f, tween(200))
            else {
                delay(delayMillis.toLong())
                p.animateTo(1f, Motion.reward())
            }
        }
        shown = true
    }
    graphicsLayer {
        alpha = p.value.coerceIn(0f, 1f)
        val s = if (reduce) 1f else 0.6f + 0.4f * p.value
        scaleX = s; scaleY = s
        transformOrigin = TransformOrigin(0f, 0.5f)
    }
}

@Composable
private fun EditableList(title: String, items: List<String>, placeholder: String, onChange: (List<String>) -> Unit) {
    JournalFormSection(title) {
        items.forEachIndexed { index, value ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    FormField(value, { text -> onChange(items.toMutableList().also { it[index] = text }) }, placeholder, multiline = true)
                }
                Box(
                    Modifier.size(44.dp).clip(CircleShape)
                        .pressable(scale = 0.9f) { onChange(items.filterIndexed { i, _ -> i != index }) }
                        .semantics { contentDescription = "Remove" },
                    contentAlignment = Alignment.Center,
                ) { Icon(symbol("xmark.circle.fill"), null, tint = BookInk.caption.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)) }
            }
            RowDivider(inset = Space.m)
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).rowPress { onChange(items + "") }.padding(horizontal = Space.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            Icon(symbol("plus"), null, tint = BookInk.rule, modifier = Modifier.size(20.dp))
            Text("Add", style = TextStyles.body, color = BookInk.rule)
        }
    }
}
