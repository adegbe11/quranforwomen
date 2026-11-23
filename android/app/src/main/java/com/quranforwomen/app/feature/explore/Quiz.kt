package com.quranforwomen.app.feature.explore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.ContentStore
import com.quranforwomen.app.data.QuranStore
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.ArabicText
import com.quranforwomen.app.ui.components.CardSurface
import com.quranforwomen.app.ui.components.PetalBurst
import com.quranforwomen.app.ui.components.PrimaryButton
import com.quranforwomen.app.ui.components.RollingNumber
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.inkReveal
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Radius
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max

private class QuizQuestion(
    val prompt: String,
    val arabic: String?,
    val options: List<String>,
    val answer: Int,
    val explanation: String,
) {
    val id: String = UUID.randomUUID().toString()
}

/**
 * Builds questions only from verified data: surah names, meanings, verse counts,
 * place of revelation, order, and the source of well-known verses.
 */
private object QuizMaker {
    fun make(quran: QuranStore, content: ContentStore, count: Int = 10): List<QuizQuestion> {
        val famous = (content.dailyVerses + content.feelings.flatMap { it.refs } + content.topics.flatMap { it.refs })
            .toSet().toList()
        val questions = mutableListOf<QuizQuestion>()
        for (i in 0 until count) {
            when (i % 5) {
                0 -> {
                    val ref = famous.randomOrNull() ?: continue
                    val surah = quran.surah(ref.surah)
                    val others = quran.surahs.filter { it.id != surah.id }.shuffled().take(3).map { it.name }
                    questions += build(
                        prompt = "Which surah is this verse from?",
                        arabic = quran.text(ref),
                        correct = surah.name, wrong = others,
                        explanation = "It is from ${surah.name}, verse ${ref.ayah}.",
                    )
                }
                1 -> {
                    val surah = quran.surahs.filter { it.verseCount >= 5 }.randomOrNull() ?: quran.surah(1)
                    val counts = quran.surahs.map { it.verseCount }.toSet() - surah.verseCount
                    val wrong = counts.sortedBy { abs(it - surah.verseCount) }.take(6).shuffled().take(3).map { it.toString() }
                    questions += build(
                        prompt = "How many verses are in ${surah.name}?",
                        arabic = null, correct = surah.verseCount.toString(), wrong = wrong,
                        explanation = "${surah.name} has ${surah.verseCount} verses.",
                    )
                }
                2 -> {
                    val surah = quran.surahs.randomOrNull() ?: quran.surah(1)
                    val other = if (surah.revelationPlace == "Makkah") "Madinah" else "Makkah"
                    questions += build(
                        prompt = "Where was ${surah.name} revealed?",
                        arabic = null, correct = surah.revelationPlace, wrong = listOf(other),
                        explanation = "${surah.name} is a ${if (surah.revelationPlace == "Makkah") "Makkan" else "Madinan"} surah.",
                    )
                }
                3 -> {
                    val surah = quran.surahs.randomOrNull() ?: quran.surah(1)
                    val wrong = quran.surahs.filter { it.meaning != surah.meaning }.shuffled().take(3).map { it.meaning }
                    questions += build(
                        prompt = "What does the name ${surah.name} mean?",
                        arabic = null, correct = surah.meaning, wrong = wrong,
                        explanation = "${surah.name} means ${surah.meaning}.",
                    )
                }
                else -> {
                    val surah = quran.surahs.filter { it.id < 114 }.randomOrNull() ?: quran.surah(1)
                    val next = quran.surah(surah.id + 1)
                    val wrong = quran.surahs.filter { it.id != next.id && it.id != surah.id }.shuffled().take(3).map { it.name }
                    questions += build(
                        prompt = "Which surah comes right after ${surah.name}?",
                        arabic = null, correct = next.name, wrong = wrong,
                        explanation = "${next.name} is surah ${next.id}, right after ${surah.name}.",
                    )
                }
            }
        }
        return questions.shuffled()
    }

    private fun build(prompt: String, arabic: String?, correct: String, wrong: List<String>, explanation: String): QuizQuestion {
        val options = (listOf(correct) + wrong).shuffled()
        return QuizQuestion(prompt, arabic, options, options.indexOf(correct).coerceAtLeast(0), explanation)
    }
}

private enum class OptionState { Open, Right, Revealed, Wrong, Dimmed }

/**
 * A calm quiz for learning. No timers, no streaks, no leaderboard.
 * A right answer sweeps in emerald; a wrong one gives a small shake and the right answer is shown so she learns it.
 */
@Composable
fun QuizScreen() {
    val graph = LocalGraph.current
    val reduce = LocalReduceMotion.current
    val haptics = rememberHaptics()

    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf<Int?>(null) }
    var correct by remember { mutableIntStateOf(0) }
    val results = remember { mutableStateListOf<Boolean>() }
    var round by remember { mutableIntStateOf(0) }

    fun restart() {
        questions = QuizMaker.make(graph.quran, graph.content)
        index = 0
        chosen = null
        correct = 0
        results.clear()
        round++
    }

    LaunchedEffect(Unit) { if (questions.isEmpty()) restart() }

    AppScreen(title = "Quran Quiz", largeTitle = false) {
        if (questions.isEmpty()) {
            item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(top = Space.l), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Palette.accent)
                }
            }
        } else {
            item(key = "progress") {
                QuizProgress(questions.size, index, results, Modifier.padding(top = Space.l).focusIn(50))
            }
            item(key = "body") {
                AnimatedContent(
                    targetState = round to index,
                    transitionSpec = {
                        if (reduce) {
                            fadeIn() togetherWith fadeOut()
                        } else {
                            (slideInHorizontally(exploreSpring(0.5f, 0.86f)) { it } + fadeIn()) togetherWith
                                (slideOutHorizontally(exploreSpring(0.5f, 0.86f)) { -it } + fadeOut())
                        } using SizeTransform(clip = false)
                    },
                    contentAlignment = Alignment.TopStart,
                    label = "question",
                ) { (_, i) ->
                    if (i >= questions.size) {
                        QuizFinished(correct, questions.size) { restart() }
                    } else {
                        val q = questions[i]
                        val pick = if (i == index) chosen else null
                        QuestionView(
                            q = q,
                            chosen = pick,
                            isLast = i + 1 == questions.size,
                            onChoose = { option ->
                                if (chosen == null && i == index) {
                                    chosen = option
                                    if (option == q.answer) {
                                        correct++
                                        haptics.success()
                                    } else {
                                        haptics.soft()
                                    }
                                    results.add(option == q.answer)
                                }
                            },
                            onNext = {
                                index++
                                chosen = null
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionView(q: QuizQuestion, chosen: Int?, isLast: Boolean, onChoose: (Int) -> Unit, onNext: () -> Unit) {
    val reduce = LocalReduceMotion.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.l)) {
        Text(
            q.prompt,
            style = TextStyles.title2.copy(fontFamily = AppFont.serif, fontWeight = FontWeight.SemiBold),
            color = Palette.textPrimary,
            modifier = Modifier.focusIn(100),
        )
        if (q.arabic != null) {
            CardSurface(Modifier.focusIn(150)) {
                ArabicText(q.arabic, size = 24f, modifier = Modifier.inkReveal(250, 1000))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            q.options.forEachIndexed { i, option ->
                QuizOption(
                    text = option,
                    state = optionState(i, q.answer, chosen),
                    revealDelay = if (chosen != null && chosen != q.answer && i == q.answer) 250 else 0,
                    modifier = Modifier.dealIn(i, baseDelay = 250, step = 60),
                ) { onChoose(i) }
            }
        }
        AnimatedVisibility(
            visible = chosen != null,
            enter = if (reduce) fadeIn() else slideInVertically(exploreSpring(0.4f, 0.8f)) { it / 2 } + fadeIn(),
            exit = if (reduce) fadeOut() else slideOutVertically { it / 2 } + fadeOut(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
                val bulb = remember { Animatable(1f) }
                LaunchedEffect(Unit) {
                    if (!reduce) {
                        bulb.snapTo(0.7f)
                        bulb.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.tile))
                        .background(Palette.surface)
                        .padding(Space.m),
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    Icon(
                        symbol("lightbulb"), null, tint = Color(0xFFC9A24A),
                        modifier = Modifier.size(20.dp).graphicsLayer { scaleX = bulb.value; scaleY = bulb.value },
                    )
                    Text(q.explanation, style = TextStyles.subheadline, color = Palette.textPrimary)
                }
                PrimaryButton(if (isLast) "See How You Did" else "Next Question", onNext)
            }
        }
    }
}

private fun optionState(i: Int, answer: Int, chosen: Int?): OptionState {
    if (chosen == null) return OptionState.Open
    if (i == answer) return if (chosen == answer) OptionState.Right else OptionState.Revealed
    if (i == chosen) return OptionState.Wrong
    return OptionState.Dimmed
}

/** Ten small segments. The current one stretches wide; answered ones fill. */
@Composable
private fun QuizProgress(total: Int, index: Int, results: List<Boolean>, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    Column(modifier.semantics(mergeDescendants = true) { }, verticalArrangement = Arrangement.spacedBy(Space.s)) {
        AnimatedContent(
            targetState = index,
            transitionSpec = {
                if (reduce) fadeIn() togetherWith fadeOut()
                else (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            },
            label = "count",
        ) { i ->
            Text(
                if (i < total) "Question ${i + 1} of $total" else "All done",
                style = TextStyles.subheadline.copy(fontWeight = FontWeight.Medium),
                color = Palette.accent,
            )
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(6.dp)) {
            val gap = 5.dp
            val available = maxWidth - gap * (total - 1)
            val stretched = index < total && total > 1
            val other = if (stretched) (available - 28.dp) / (total - 1) else available / total
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (i in 0 until total) {
                    key(i) {
                        val target = when {
                            i < results.size -> if (results[i]) Palette.accentFill else Palette.blushStrong.copy(alpha = 0.55f)
                            i == index -> Palette.accent.copy(alpha = 0.45f)
                            else -> Palette.border
                        }
                        val w by animateDpAsState(
                            if (stretched && i == index) 28.dp else other,
                            if (reduce) snap() else exploreSpring(0.45f, 0.75f),
                            label = "segment",
                        )
                        val c by animateColorAsState(target, label = "segmentColor")
                        Box(Modifier.width(w).height(6.dp).clip(CircleShape).background(c))
                    }
                }
            }
        }
    }
}

/** One answer. It sweeps in emerald when right, shakes softly and turns blush when wrong. */
@Composable
private fun QuizOption(text: String, state: OptionState, revealDelay: Int, modifier: Modifier = Modifier, choose: () -> Unit) {
    val reduce = LocalReduceMotion.current
    val sweep = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(state) {
        val sweeps = state == OptionState.Right || state == OptionState.Revealed || state == OptionState.Wrong
        if (!sweeps) {
            sweep.snapTo(0f)
            return@LaunchedEffect
        }
        if (reduce) {
            sweep.snapTo(1f)
        } else {
            delay(revealDelay.toLong())
            sweep.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        }
    }
    LaunchedEffect(state) {
        if (reduce) return@LaunchedEffect
        when (state) {
            OptionState.Wrong -> launch {
                shake.animateTo(-7f, tween(60, easing = LinearEasing))
                shake.animateTo(6f, tween(80, easing = LinearEasing))
                shake.animateTo(-4f, tween(80, easing = LinearEasing))
                shake.animateTo(2f, tween(70, easing = LinearEasing))
                shake.animateTo(0f, tween(70, easing = LinearEasing))
            }
            OptionState.Right -> launch {
                pop.animateTo(1.03f, spring(dampingRatio = 0.5f, stiffness = 800f))
                pop.animateTo(1f, spring(dampingRatio = 1f, stiffness = 300f))
            }
            else -> Unit
        }
    }

    val shape = RoundedCornerShape(Radius.tile)
    val sweepColor = when (state) {
        OptionState.Right, OptionState.Revealed -> Palette.accentWash
        OptionState.Wrong -> Palette.blush
        else -> Color.Transparent
    }
    val shown = state == OptionState.Right || state == OptionState.Revealed
    val borderColor = if (shown) Palette.accent.copy(alpha = 0.5f) else Palette.border
    val dim by animateFloatAsState(if (state == OptionState.Dimmed) 0.5f else 1f, label = "dim")
    Row(
        modifier
            .graphicsLayer {
                translationX = shake.value * density
                scaleX = pop.value; scaleY = pop.value
                alpha = dim
            }
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(Palette.surface)
            .drawBehind { drawRect(sweepColor, size = Size(size.width * sweep.value, size.height)) }
            .border(1.dp, borderColor, shape)
            .pressable(enabled = state == OptionState.Open, onClick = choose)
            .semantics {
                stateDescription = when (state) {
                    OptionState.Right -> "Correct"
                    OptionState.Revealed -> "The right answer"
                    OptionState.Wrong -> "Not quite"
                    else -> ""
                }
            }
            .padding(Space.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = TextStyles.headline, color = Palette.textPrimary, modifier = Modifier.weight(1f))
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            androidx.compose.animation.AnimatedVisibility(shown, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(symbol("checkmark.circle.fill"), null, tint = Palette.accent, modifier = Modifier.size(24.dp))
                    PetalBurst(if (state == OptionState.Right) 1 else 0)
                }
            }
            androidx.compose.animation.AnimatedVisibility(state == OptionState.Wrong, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                Icon(symbol("xmark.circle"), null, tint = Palette.blushStrong, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/** The end of a round: a ring draws to her score, a lotus draws itself, a few sparks. */
@Composable
private fun QuizFinished(correct: Int, total: Int, again: () -> Unit) {
    val reduce = LocalReduceMotion.current
    var drawn by remember { mutableStateOf(false) }
    var fire by remember { mutableIntStateOf(0) }
    var shownCount by remember { mutableIntStateOf(if (reduce) correct else 0) }
    val fraction = if (total == 0) 0f else correct.toFloat() / total
    val ring by animateFloatAsState(
        if (drawn || reduce) max(fraction, 0.001f) else 0.001f,
        if (reduce) snap() else exploreSpring(1.2f, 0.85f),
        label = "score",
    )
    val lotus by animateFloatAsState(
        if (drawn || reduce) 1f else 0f,
        if (reduce) snap() else exploreSpring(1.2f, 0.85f),
        label = "lotus",
    )
    LaunchedEffect(Unit) {
        launch {
            delay(200)
            drawn = true
        }
        launch {
            delay(300)
            shownCount = correct
        }
        delay(1000)
        fire++
    }
    val headline = when {
        fraction >= 0.8f -> "Masha'Allah"
        fraction >= 0.5f -> "Well done"
        else -> "Good start"
    }
    val wash = Palette.accentWash
    val fill = Palette.accentFill

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.l),
    ) {
        Box(
            Modifier
                .padding(top = Space.l)
                .size(170.dp)
                .clearAndSetSemantics { contentDescription = "You answered $correct of $total" },
            contentAlignment = Alignment.Center,
        ) {
            ExploreSparks(fire, from = 70.dp, to = 120.dp)
            Canvas(Modifier.matchParentSize()) {
                val stroke = 12.dp.toPx()
                val inset = stroke / 2
                drawCircle(wash, radius = size.minDimension / 2 - inset, style = Stroke(stroke))
                drawArc(
                    fill, startAngle = -90f, sweepAngle = 360f * ring.coerceIn(0f, 1f), useCenter = false,
                    topLeft = Offset(inset, inset), size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    RollingNumber(
                        shownCount,
                        style = TextStyle(fontSize = 48.sp, lineHeight = 52.sp, fontWeight = FontWeight.Light),
                        color = Palette.textPrimary,
                    )
                    Text("/$total", style = TextStyles.title3, color = Palette.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
                }
                Text("answered", style = TextStyles.caption, color = Palette.textSecondary)
            }
        }
        ExploreDrawnLotus(lotus, Palette.accent, 1.6.dp, Modifier.size(44.dp, 32.dp))
        Text(
            headline,
            style = TextStyles.title2.copy(fontFamily = AppFont.serif, fontWeight = FontWeight.SemiBold),
            color = Palette.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.focusIn(600),
        )
        PrimaryButton("New Questions", again, Modifier.dealIn(0, baseDelay = 800))
    }
}
