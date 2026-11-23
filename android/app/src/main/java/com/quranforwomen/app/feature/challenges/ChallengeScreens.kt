package com.quranforwomen.app.feature.challenges

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.Challenge
import com.quranforwomen.app.data.ChallengeCatalog
import com.quranforwomen.app.data.ChallengeStore
import com.quranforwomen.app.data.ContentStore
import com.quranforwomen.app.data.DhikrPhrase
import com.quranforwomen.app.data.Dua
import com.quranforwomen.app.data.QuranStore
import com.quranforwomen.app.data.ReadingPlan
import com.quranforwomen.app.data.SourceKind
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.feature.explore.ExploreDrawnLotus
import com.quranforwomen.app.feature.explore.ExploreSparks
import com.quranforwomen.app.feature.explore.exploreSpring
import com.quranforwomen.app.feature.explore.rememberExploreIconLand
import com.quranforwomen.app.feature.guidance.GuidanceVerseCard
import com.quranforwomen.app.feature.journal.JournalContent
import com.quranforwomen.app.nav.JournalStart
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.AppAlert
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.ArabicText
import com.quranforwomen.app.ui.components.CardSurface
import com.quranforwomen.app.ui.components.Eyebrow
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.ListRow
import com.quranforwomen.app.ui.components.PetalBurst
import com.quranforwomen.app.ui.components.PrimaryButton
import com.quranforwomen.app.ui.components.RollingNumber
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.SectionHeader
import com.quranforwomen.app.ui.components.SourceLabel
import com.quranforwomen.app.ui.components.TextAction
import com.quranforwomen.app.ui.components.TintedButton
import com.quranforwomen.app.ui.components.TranslationText
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.inkReveal
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
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
import kotlinx.coroutines.launch

// What a day asks for

/** One day of a challenge, built only from the bundled, verified content. */
internal sealed interface ChallengeStep {
    data class DhikrDay(val phrase: DhikrPhrase) : ChallengeStep
    data class PagesDay(val pages: IntRange) : ChallengeStep
    data class DuaDay(val dua: Dua) : ChallengeStep
    data class GratitudeDay(val verse: VerseRef, val prompt: String) : ChallengeStep
}

/** What the day asks for, in plain words. */
internal val ChallengeStep.actionText: String
    get() = when (this) {
        is ChallengeStep.DhikrDay -> "Say this ${phrase.target} times"
        is ChallengeStep.PagesDay -> if (pages.first == pages.last) "Read page ${pages.first}" else "Read pages ${pages.first} to ${pages.last}"
        is ChallengeStep.DuaDay -> "Read this dua"
        is ChallengeStep.GratitudeDay -> "Read this verse, then write"
    }

/** The bundled morning and evening duas. Chosen by their own titles and notes, never by guesswork. */
private fun ContentStore.challengeAdhkar(morning: Boolean): List<Dua> {
    val word = if (morning) "morning" else "evening"
    return duas.filter { dua ->
        if (dua.category != "morning-evening") return@filter false
        val words = (dua.title + " " + (dua.note ?: "")).lowercase()
        word in words || (morning && "waking" in words)
    }
}

/** The step for a zero based day, or null when the bundled content for it is missing. */
internal fun challengeStep(challenge: Challenge, day: Int, content: ContentStore, startPage: Int): ChallengeStep? =
    when (val kind = challenge.kind) {
        Challenge.Kind.Dhikr ->
            content.dhikr.takeIf { it.isNotEmpty() }?.let { ChallengeStep.DhikrDay(it[day % it.size]) }
        is Challenge.Kind.QuranPages -> {
            val first = startPage.coerceIn(1, ReadingPlan.TOTAL_PAGES - kind.perDay * challenge.days + 1) + day * kind.perDay
            ChallengeStep.PagesDay(first..(first + kind.perDay - 1))
        }
        Challenge.Kind.Morning, Challenge.Kind.Evening ->
            content.challengeAdhkar(kind == Challenge.Kind.Morning).takeIf { it.isNotEmpty() }
                ?.let { ChallengeStep.DuaDay(it[day % it.size]) }
        Challenge.Kind.Gratitude -> {
            val verses = content.collection("grateful")?.refs.orEmpty()
            val prompts = JournalContent.pack("shukr")?.prompts.orEmpty()
            if (verses.isEmpty() || prompts.isEmpty()) null
            else ChallengeStep.GratitudeDay(verses[day % verses.size], prompts[day % prompts.size])
        }
        Challenge.Kind.QuranMonth -> ChallengeStep.PagesDay(ReadingPlan(challenge.days, 0L).pages(day))
    }

/** Challenges whose bundled content is present. */
internal fun challengesAvailable(content: ContentStore): List<Challenge> =
    ChallengeCatalog.all.filter { challengeStep(it, 0, content, 1) != null }

/** First and last verse that fall on a range of mushaf pages. */
private fun QuranStore.challengeVersesOnPages(pages: IntRange): Pair<VerseRef, VerseRef>? {
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

private const val MushafPageKey = "mushaf.page"

// List

/** Every challenge in one quiet list. Rows deal in and each icon lands with a small bounce. */
@Composable
fun ChallengesScreen() {
    val graph = LocalGraph.current
    val router = LocalRouter.current
    val store = graph.challenges
    val list = remember { challengesAvailable(graph.content) }
    AppScreen(title = "Challenges") {
        item(key = "list") {
            GroupedSection(Modifier.focusIn(50)) {
                list.forEachIndexed { index, challenge ->
                    if (index > 0) RowDivider()
                    ListRow(
                        title = challenge.title,
                        icon = symbol(challenge.symbol),
                        value = challengeProgress(store, challenge),
                        landDelayMillis = 300 + minOf(index, 8) * 50,
                        modifier = Modifier.dealIn(minOf(index, 8), baseDelay = 100, step = 50),
                        onClick = { router.push(Route.Challenge(challenge.id)) },
                    )
                }
            }
        }
    }
}

/** Nothing until she starts; then where she is. The title already says how many days. */
private fun challengeProgress(store: ChallengeStore, challenge: Challenge): String? = when {
    store.state(challenge) == null -> null
    store.isComplete(challenge) -> "Done"
    else -> "Day ${store.nextDay(challenge) + 1}"
}

// One challenge

/**
 * One challenge, day by day. She can open any day; the next day not yet done comes first.
 * Marking a day done fills its tile with a spring and sends out a few petals, then the
 * next day slides in. Missing a day changes nothing: she continues where she is.
 */
@Composable
fun ChallengeScreen(id: String) {
    val graph = LocalGraph.current
    val router = LocalRouter.current
    val store = graph.challenges
    val reduce = LocalReduceMotion.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val challenge = remember(id) { ChallengeCatalog.challenge(id) }
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    var confirmLeave by remember { mutableStateOf(false) }
    var confirmRestart by remember { mutableStateOf(false) }

    if (challenge == null) {
        AppScreen(title = "", largeTitle = false) { }
        return
    }
    val savedPage = graph.prefs.sp.getInt(MushafPageKey, 1).coerceAtLeast(1)
    val state = store.state(challenge)

    fun mark(day: Int, done: Boolean) {
        // Keep this day on screen while it fills in.
        selected = day
        store.setDone(done, day, challenge)
        if (!done) {
            haptics.tick()
            return
        }
        haptics.success()
        if (store.isComplete(challenge)) return
        // Then the next day not yet done slides in.
        scope.launch {
            delay(if (reduce) 300L else 900L)
            if (selected == day) selected = null
        }
    }

    fun restart() {
        store.start(challenge, savedPage)
        selected = null
        haptics.tap()
    }

    AppScreen(title = challenge.title, largeTitle = false) {
        if (state == null) {
            item(key = "intro") {
                ChallengeIntro(challenge, challengeStep(challenge, 0, graph.content, savedPage), Modifier.dealIn(0, baseDelay = 100))
            }
            item(key = "start") {
                PrimaryButton(
                    "Start Challenge",
                    onClick = {
                        store.start(challenge, savedPage)
                        selected = null
                        haptics.tap()
                    },
                    modifier = Modifier.dealIn(1, baseDelay = 100),
                )
            }
        } else {
            val next = store.nextDay(challenge)
            val day = (selected ?: next).coerceIn(0, challenge.days - 1)
            val complete = state.done.size >= challenge.days
            val isDone = day in state.done

            item(key = "header") {
                ChallengeHeader(
                    done = state.done.size,
                    days = challenge.days,
                    day = day,
                    action = challengeStep(challenge, day, graph.content, state.startPage)?.actionText,
                    modifier = Modifier.dealIn(0, baseDelay = 100),
                )
            }
            item(key = "step") {
                AnimatedContent(
                    targetState = day,
                    transitionSpec = {
                        if (reduce) fadeIn() togetherWith fadeOut()
                        else {
                            val forward = targetState > initialState
                            (slideInHorizontally(Motion.smooth()) { if (forward) it / 3 else -it / 3 } + fadeIn()) togetherWith
                                (slideOutHorizontally(Motion.smooth()) { if (forward) -it / 3 else it / 3 } + fadeOut())
                        }
                    },
                    label = "day",
                    modifier = Modifier.dealIn(1, baseDelay = 100),
                ) { d ->
                    val step = challengeStep(challenge, d, graph.content, state.startPage)
                    if (step != null) {
                        ChallengeStepCard(
                            step = step,
                            onReached = { if (!store.isDone(d, challenge)) mark(d, true) },
                            openMushaf = { page -> router.push(Route.Mushaf(page)) },
                            write = { prompt -> router.push(Route.JournalEditor(JournalStart.Prompt(prompt))) },
                        )
                    }
                }
            }
            item(key = "mark") {
                ChallengeMarkButton(isDone, Modifier.dealIn(2, baseDelay = 100)) { mark(day, !isDone) }
            }
            item(key = "complete") {
                AnimatedVisibility(
                    visible = complete,
                    enter = if (reduce) fadeIn() else scaleIn(Motion.reward(), initialScale = 0.95f) + fadeIn(),
                    exit = if (reduce) fadeOut() else scaleOut(targetScale = 0.95f) + fadeOut(),
                ) {
                    ChallengeComplete(challenge.days) { confirmRestart = true }
                }
            }
            item(key = "days") {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
                    SectionHeader("All days", Modifier.focusIn(300))
                    (0 until challenge.days).chunked(7).forEach { week ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            week.forEach { index ->
                                ChallengeDayCell(
                                    day = index,
                                    isDone = index in state.done,
                                    isSelected = index == day,
                                    modifier = Modifier
                                        .weight(1f)
                                        .dealIn(minOf(index / 7 + index % 7, 14), baseDelay = 350, step = 30),
                                ) {
                                    haptics.tick()
                                    selected = index
                                }
                            }
                            repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            item(key = "actions") {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!complete) TextAction("Start Over", { confirmRestart = true })
                    ChallengeDangerAction("Leave Challenge") { confirmLeave = true }
                }
            }
        }
    }

    if (confirmLeave) {
        AppAlert(
            title = "Leave this challenge?",
            confirmLabel = "Leave Challenge",
            onConfirm = {
                store.leave(challenge)
                selected = null
            },
            onDismiss = { confirmLeave = false },
            destructive = true,
        )
    }
    if (confirmRestart) {
        AppAlert(
            title = "Start this challenge again?",
            confirmLabel = "Start Over",
            onConfirm = { restart() },
            onDismiss = { confirmRestart = false },
            destructive = true,
        )
    }
}

// Before starting

@Composable
private fun ChallengeIntro(challenge: Challenge, first: ChallengeStep?, modifier: Modifier = Modifier) {
    val land = rememberExploreIconLand(350)
    CardSurface(modifier.semantics(mergeDescendants = true) { }, padding = Space.xl) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Box(
                Modifier
                    .size(44.dp)
                    .graphicsLayer { scaleX = land.value; scaleY = land.value }
                    .clip(CircleShape)
                    .background(Palette.accentWash),
                contentAlignment = Alignment.Center,
            ) {
                Icon(symbol(challenge.symbol), null, tint = Palette.accent, modifier = Modifier.size(22.dp))
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                RollingNumber(
                    challenge.days,
                    style = TextStyle(fontSize = 56.sp, lineHeight = 60.sp, fontWeight = FontWeight.Light),
                    color = Palette.textPrimary,
                )
                Text("days", style = TextStyles.headline, color = Palette.textSecondary, modifier = Modifier.padding(bottom = 10.dp))
            }
            if (first != null) {
                Box(Modifier.padding(vertical = Space.xs)) { RowDivider(inset = 0.dp) }
                Text(
                    "Day 1 · ${first.actionText}",
                    style = TextStyles.subheadline.copy(fontWeight = FontWeight.Medium),
                    color = Palette.textPrimary,
                )
            }
        }
    }
}

// Started

@Composable
private fun ChallengeHeader(done: Int, days: Int, day: Int, action: String?, modifier: Modifier = Modifier) {
    CardSurface(modifier.semantics(mergeDescendants = true) { }, padding = Space.xl) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.l)) {
            ChallengeRing(done, days, Modifier.size(112.dp))
            Column(Modifier.weight(1f).focusIn(200), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Crossfade(day, label = "dayLabel") { Eyebrow("Day ${it + 1}") }
                if (action != null) {
                    Crossfade(action, label = "action") {
                        Text(
                            it,
                            style = TextStyles.headline.copy(fontFamily = AppFont.serif),
                            color = Palette.textPrimary,
                        )
                    }
                }
            }
        }
    }
}

/** The bundled text for one day, exactly as it is in the app's verified data. */
@Composable
private fun ChallengeStepCard(
    step: ChallengeStep,
    onReached: () -> Unit,
    openMushaf: (Int) -> Unit,
    write: (String) -> Unit,
) {
    val graph = LocalGraph.current
    when (step) {
        is ChallengeStep.DhikrDay -> CardSurface(padding = Space.xl) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Space.m)) {
                ArabicText(step.phrase.arabic, Modifier.inkReveal(200, 1000), size = 30f, center = true)
                Text(
                    step.phrase.transliteration,
                    style = TextStyles.subheadline.copy(fontStyle = FontStyle.Italic),
                    color = Palette.textSecondary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    step.phrase.translation,
                    style = TextStyles.body.copy(fontFamily = AppFont.serif),
                    color = Palette.textPrimary,
                    textAlign = TextAlign.Center,
                )
                SourceLabel(SourceKind.Hadith, step.phrase.source)
                ChallengeCounter(step.phrase.target, onReached, Modifier.padding(top = Space.xs))
            }
        }
        is ChallengeStep.DuaDay -> CardSurface(padding = Space.xl) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
                Text(step.dua.title, style = TextStyles.headline.copy(fontFamily = AppFont.serif), color = Palette.textPrimary)
                ArabicText(step.dua.arabic, Modifier.inkReveal(200, 1100), size = 26f)
                step.dua.transliteration?.let {
                    Text(it, style = TextStyles.subheadline.copy(fontStyle = FontStyle.Italic), color = Palette.textSecondary)
                }
                TranslationText(step.dua.translation)
                step.dua.note?.let { Text(it, style = TextStyles.footnote, color = Palette.textSecondary) }
                SourceLabel(step.dua.sourceKind, step.dua.source)
            }
        }
        is ChallengeStep.PagesDay -> {
            val quran = graph.quran
            val range = remember(step.pages) { quran.challengeVersesOnPages(step.pages) }
            CardSurface(padding = Space.xl) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
                    if (range != null) {
                        Text(
                            "${quran.surah(range.first.surah).name} ${range.first.id} to ${quran.surah(range.second.surah).name} ${range.second.id}",
                            style = TextStyles.title3.copy(fontFamily = AppFont.serif),
                            color = Palette.textPrimary,
                        )
                    }
                    Text(
                        if (step.pages.first == step.pages.last) "Page ${step.pages.first}" else "Pages ${step.pages.first} to ${step.pages.last}",
                        style = TextStyles.subheadline,
                        color = Palette.textSecondary,
                    )
                    PrimaryButton("Open the Mushaf", onClick = { openMushaf(step.pages.first) })
                }
            }
        }
        is ChallengeStep.GratitudeDay -> Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
            GuidanceVerseCard(step.verse, inkDelay = 250)
            CardSurface(padding = Space.xl) {
                Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
                    Eyebrow("Write")
                    Text(
                        step.prompt,
                        style = TextStyles.title3.copy(fontFamily = AppFont.serif),
                        color = Palette.textPrimary,
                    )
                    TintedButton("Write", onClick = { write(step.prompt) })
                }
            }
        }
    }
}

/** A tap counter for the day's dhikr. Each tap ticks; reaching the target marks the day done. */
@Composable
private fun ChallengeCounter(target: Int, reached: () -> Unit, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    var count by remember { mutableIntStateOf(0) }
    val fraction = if (target <= 0) 0f else (count.toFloat() / target).coerceAtMost(1f)
    val sweep by animateFloatAsState(fraction, if (reduce) snap() else exploreSpring(0.4f, 0.8f), label = "count")
    val pulse = remember { Animatable(1f) }
    val wash = Palette.accentWash
    val fill = Palette.accentFill
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(140.dp)
                .graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }
                .clip(CircleShape)
                .pressable(scale = 0.97f, onClickLabel = "Count") {
                    count++
                    if (count == target) reached() else haptics.tick()
                    if (!reduce) scope.launch {
                        pulse.snapTo(0.95f)
                        pulse.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f))
                    }
                }
                .semantics {
                    contentDescription = "Count"
                    stateDescription = "$count of $target"
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(140.dp)) {
                val stroke = 8.dp.toPx()
                val inset = stroke / 2
                drawCircle(wash, radius = size.minDimension / 2 - inset, style = Stroke(stroke))
                drawArc(
                    fill, startAngle = -90f, sweepAngle = 360f * sweep.coerceIn(0f, 1f), useCenter = false,
                    topLeft = Offset(inset, inset), size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RollingNumber(
                    count,
                    style = TextStyle(fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Light),
                    color = Palette.textPrimary,
                )
                Text("of $target", style = TextStyles.caption, color = Palette.textSecondary)
            }
        }
        AnimatedVisibility(count > 0) {
            TextAction("Reset", { count = 0 })
        }
    }
}

/** Marks a day done. The ring fills with a spring, the check pops in, and a few petals drift out. */
@Composable
private fun ChallengeMarkButton(done: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val p by animateFloatAsState(if (done) 1f else 0f, if (reduce) snap() else exploreSpring(0.35f, 0.6f), label = "mark")
    var burst by remember { mutableIntStateOf(0) }
    var previous by remember { mutableStateOf(done) }
    LaunchedEffect(done) {
        if (done && !previous) burst++
        previous = done
    }
    val accent = Palette.accent
    val fill = Palette.accentFill
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clip(CircleShape)
            .background(Palette.accentWash)
            .pressable(scale = 0.98f, onClick = onClick)
            .semantics(mergeDescendants = true) { stateDescription = if (done) "Done" else "Not done" }
            .padding(horizontal = Space.l),
        horizontalArrangement = Arrangement.spacedBy(Space.s, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(22.dp)) {
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
        Crossfade(done, label = "markText") {
            Text(if (it) "Done" else "Mark as done", style = TextStyles.headline, color = Palette.accent)
        }
    }
}

/** How many days are done, drawn as a ring that fills from the top. */
@Composable
private fun ChallengeRing(done: Int, days: Int, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    var drawn by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!drawn) {
            delay(250)
            drawn = true
        }
    }
    val fraction = if (days == 0) 0f else (done.toFloat() / days).coerceAtMost(1f)
    val target = if (drawn || reduce) maxOf(fraction, 0.001f) else 0.001f
    val sweep by animateFloatAsState(target, if (reduce) snap() else exploreSpring(1.0f, 0.82f), label = "ring")
    val wash = Palette.accentWash
    val fill = Palette.accentFill
    Box(
        modifier.clearAndSetSemantics { contentDescription = "$done of $days days done" },
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
            RollingNumber(
                done,
                style = TextStyles.title2.copy(fontFamily = AppFont.serif, fontWeight = FontWeight.SemiBold),
                color = Palette.textPrimary,
            )
            Text("of $days done", style = TextStyles.caption, color = Palette.textSecondary)
        }
    }
}

/** One day. Done days fill; the open day wears a soft ring. */
@Composable
private fun ChallengeDayCell(
    day: Int,
    isDone: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    select: () -> Unit,
) {
    val reduce = LocalReduceMotion.current
    val fillP by animateFloatAsState(if (isDone) 1f else 0f, if (reduce) snap() else exploreSpring(0.35f, 0.65f), label = "dayFill")
    val ringP by animateFloatAsState(if (isSelected) 1f else 0f, if (reduce) snap() else exploreSpring(0.3f, 0.8f), label = "dayRing")
    val pop = remember { Animatable(1f) }
    var previous by remember { mutableStateOf(isDone) }
    LaunchedEffect(isDone) {
        if (previous == isDone) return@LaunchedEffect
        previous = isDone
        if (reduce) return@LaunchedEffect
        pop.animateTo(0.86f, tween(100))
        pop.animateTo(1.1f, tween(180, easing = FastOutSlowInEasing))
        pop.animateTo(1f, spring(dampingRatio = 1f, stiffness = 300f))
    }
    val shape = RoundedCornerShape(9.dp)
    val fill = Palette.accentFill
    val ring = Palette.blushStrong
    Box(
        modifier
            .height(44.dp)
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            .clip(shape)
            .background(Palette.surface)
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
            .then(if (ringP > 0.01f) Modifier.border(1.5.dp, ring.copy(alpha = ringP.coerceIn(0f, 1f)), shape) else Modifier)
            .pressable(scale = 0.97f, onClick = select)
            .semantics {
                contentDescription = "Day ${day + 1}"
                stateDescription = if (isDone) "Done" else "Not done"
                selected = isSelected
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

/** All days done: a lotus draws itself in, with a few gold sparks. */
@Composable
private fun ChallengeComplete(days: Int, again: () -> Unit) {
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
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(Brush.linearGradient(listOf(Color(0xFFC9A24A).copy(alpha = 0.16f), Palette.accentWash.copy(alpha = 0.6f))))
            .padding(Space.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Box(Modifier.height(70.dp).fillMaxWidth().clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
            ExploreSparks(fire, from = 10.dp, to = 64.dp)
            ExploreDrawnLotus(draw, Palette.accent, 1.8.dp, Modifier.size(64.dp, 46.dp))
        }
        Text(
            "You finished all $days days.",
            style = TextStyles.body.copy(fontFamily = AppFont.serif, fontWeight = FontWeight.SemiBold),
            color = Palette.accent,
            textAlign = TextAlign.Center,
        )
        TintedButton("Start Again", onClick = again, fullWidth = false)
    }
}

/** A destructive text action, in red. */
@Composable
private fun ChallengeDangerAction(text: String, onClick: () -> Unit) {
    val danger = if (LocalDark.current) Color(0xFFFF6B6B) else Color(0xFFC62828)
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .pressable(onClick = onClick)
            .padding(horizontal = Space.s),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = TextStyles.body, color = danger)
    }
}
