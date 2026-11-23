package com.quranforwomen.app.feature.today

import androidx.compose.ui.semantics.heading
import com.quranforwomen.app.ui.components.ornateFrame
import com.quranforwomen.app.ui.components.Arabesque
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.TranslationStore
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.data.arabicIndic
import com.quranforwomen.app.nav.AppTab
import com.quranforwomen.app.nav.JournalStart
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.ArabicText
import com.quranforwomen.app.ui.components.CardSurface
import com.quranforwomen.app.ui.components.CircleIconButton
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.ListRow
import com.quranforwomen.app.ui.components.LocalBottomInset
import com.quranforwomen.app.ui.components.LocalIsRoot
import com.quranforwomen.app.ui.components.PrimaryButton
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.SaveButton
import com.quranforwomen.app.ui.components.TranslationText
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.inkReveal
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.scrollSettle
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.LocalDark
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Radius
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime

private val SheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)

/**
 * One quiet hero (the sky, with her five prayers), a verse, where she left off,
 * and one calm list for everything else.
 */
@Composable
fun TodayScreen() {
    val graph = LocalGraph.current
    val router = LocalRouter.current
    val isRoot = LocalIsRoot.current
    val density = LocalDensity.current
    val prayer = graph.prayer

    // The sky follows the clock, minute by minute, and catches up when she comes back.
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L - System.currentTimeMillis() % 60_000L + 50L)
            now = ZonedDateTime.now()
        }
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) now = ZonedDateTime.now()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    val today = now.toLocalDate()
    val schedule = remember(today, prayer.latitude, prayer.longitude, prayer.calculationMethod, prayer.asr) {
        prayer.schedule(today)
    }
    val sky = remember(now, schedule) { SkyState.at(now, schedule) }

    val scroll = rememberScrollState()
    LaunchedEffect(router.reselect) {
        if (isRoot && router.reselect.second == AppTab.today && router.reselect.first > 0) scroll.animateScrollTo(0)
    }

    // White status bar icons while the dark sky is behind them.
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val skyEndPx = with(density) { (SkyHeight - 26.dp - statusTop).toPx() }
    val overSky by remember(skyEndPx) { derivedStateOf { scroll.value < skyEndPx } }
    TodayStatusBarIcons(light = if (overSky) sky.isDark else LocalDark.current)

    val bottom = LocalBottomInset.current + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(Modifier.fillMaxSize().background(Palette.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
            // Drifts at half speed as she scrolls.
            PrayerSkyHeader(
                now = now,
                schedule = schedule,
                sky = sky,
                modifier = Modifier.graphicsLayer { translationY = scroll.value * 0.5f },
            )

            Column(
                Modifier
                    .offset(y = (-26).dp)
                    .fillMaxWidth()
                    .shadow(12.dp, SheetShape, clip = false, ambientColor = Color.Black.copy(alpha = 0.08f), spotColor = Color.Black.copy(alpha = 0.08f))
                    .background(Palette.background, SheetShape)
                    .padding(start = Space.page, end = Space.page, top = Space.xl, bottom = Space.xxl + bottom),
                verticalArrangement = Arrangement.spacedBy(Space.xxl),
            ) {
                // Dealt in after the sky wakes up, then each settles as it scrolls into view.
                // On a new day the old verse softens away and the new one comes into focus.
                if (com.quranforwomen.app.feature.ramadan.ramadanToday(today)) {
                    com.quranforwomen.app.feature.ramadan.RamadanTodayCard(today, Modifier.dealIn(0, baseDelay = 300, step = 80).scrollSettle())
                }
                Crossfade(
                    targetState = today,
                    animationSpec = tween(600),
                    modifier = Modifier.dealIn(0, baseDelay = 350, step = 80).scrollSettle(),
                    label = "dailyVerse",
                ) { day -> DailyVerseCard(graph.content.verseOfTheDay(day)) }
                ContinueReadingSection(Modifier.dealIn(1, baseDelay = 350, step = 80).scrollSettle())
                MoreForToday(today, Modifier.dealIn(2, baseDelay = 350, step = 80).scrollSettle())
            }
        }
        // Once the sky has scrolled away, a strip of page colour sits behind the status bar so the
        // clock and icons never sit on top of text.
        val statusInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val density = androidx.compose.ui.platform.LocalDensity.current
        val skyPx = with(density) { (SkyHeight - 60.dp).toPx() }
        Box(
            Modifier
                .fillMaxWidth()
                .height(statusInset)
                .graphicsLayer { alpha = ((scroll.value - skyPx) / (60f * density.density)).coerceIn(0f, 1f) }
                .background(Palette.background.copy(alpha = 0.96f))
        )
    }
}

@Composable
private fun MoreForToday(today: LocalDate, modifier: Modifier = Modifier) {
    val content = LocalGraph.current.content
    val router = LocalRouter.current
    val dua = content.duaOfTheDay(today)
    val question = todayQuestion(today)
    GroupedSection(modifier, title = "More for today") {
        ListRow("Today's dua", icon = symbol("hands.and.sparkles"), subtitle = dua.title, landDelayMillis = 750) {
            router.push(Route.DuaDetail(dua.id))
        }
        RowDivider()
        ListRow("A moment of dhikr", icon = symbol("circle.dotted"), landDelayMillis = 820) {
            router.push(Route.Dhikr)
        }
        RowDivider()
        ListRow("How is your heart?", icon = symbol("heart"), landDelayMillis = 890) {
            router.push(Route.Feelings)
        }
        RowDivider()
        // Opens over Today, as the iPhone's full screen cover does.
        ListRow("Today's question", icon = symbol("square.and.pencil"), subtitle = question, landDelayMillis = 960) {
            router.push(Route.JournalEditor(JournalStart.Prompt(question, isDua = false)))
        }
    }
}

// Verse of the day

/** The verse of the day. While it is recited, a soft emerald light breathes around the card. */
@Composable
private fun DailyVerseCard(ref: VerseRef) {
    val graph = LocalGraph.current
    val router = LocalRouter.current
    val quran = graph.quran
    val library = graph.library
    val player = graph.player
    val reduce = LocalReduceMotion.current
    val reciting = player.isCurrent(ref) && player.isPlaying
    val surah = quran.surah(ref.surah)

    Box {
        CardSurface(padding = Space.xl) {
            Column(verticalArrangement = Arrangement.spacedBy(Space.l)) {
                GoldHeading("Verse of the day")
                Column(
                    Modifier.pressable(onClickLabel = "Open this verse in the reader") {
                        router.push(Route.Reader(ref.surah, ref.ayah))
                    },
                    verticalArrangement = Arrangement.spacedBy(Space.l),
                ) {
                    ArabicText(
                        "${quran.text(ref)} ﴿${ref.ayah.arabicIndic}﴾",
                        size = 27f,
                        modifier = Modifier.inkReveal(delayMillis = 500),
                    )
                    TodayTranslationBody(ref, Modifier.focusIn(1300))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${surah.name} · ${ref.surah}:${ref.ayah}",
                        style = TextStyles.footnote,
                        color = Palette.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    CircleIconButton(
                        icon = symbol(if (reciting) "pause.fill" else "play.fill"),
                        label = if (reciting) "Pause" else "Listen",
                        isActive = reciting,
                    ) {
                        if (player.isCurrent(ref)) player.togglePlayPause() else player.play(ref, through = ref.ayah)
                    }
                    Spacer(Modifier.width(Space.s))
                    SaveButton(isSaved = library.isBookmarked(ref)) { library.toggleBookmark(ref) }
                }
            }
        }
        // A fine double gold rule with star corners, like the border of a Mushaf page.
        Canvas(Modifier.matchParentSize()) {
            ornateFrame(inset = 12.dp.toPx(), gap = 4.dp.toPx(), outer = 1.2.dp.toPx(), inner = 0.6.dp.toPx(), alpha = 0.8f)
        }
        AnimatedVisibility(
            visible = reciting && !reduce,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(400)),
            modifier = Modifier.matchParentSize(),
        ) { RecitingRing() }
    }
}

/** A title set between two gold rules with a small star, as in an illuminated manuscript. */
@Composable
private fun GoldHeading(text: String) {
    val gold = if (LocalDark.current) Arabesque.gold else Color(0xFF8A6A2B)
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Canvas(Modifier.weight(1f).height(10.dp)) {
            drawLine(Arabesque.goldBrush(size.width), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 0.9.dp.toPx())
        }
        Canvas(Modifier.size(10.dp)) { drawPath(Arabesque.starPath(center, size.minDimension / 2), Arabesque.goldBrush(size.width)) }
        Text(
            text,
            style = TextStyles.subheadline.copy(fontFamily = AppFont.serif, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp),
            color = gold,
        )
        Canvas(Modifier.size(10.dp)) { drawPath(Arabesque.starPath(center, size.minDimension / 2), Arabesque.goldBrush(size.width)) }
        Canvas(Modifier.weight(1f).height(10.dp)) {
            drawLine(Arabesque.goldBrush(size.width), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 0.9.dp.toPx())
        }
    }
}

@Composable
private fun RecitingRing() {
    val breathe = rememberInfiniteTransition(label = "recite").animateFloat(
        0.25f, 0.9f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ring",
    )
    val accent = Palette.accent
    Canvas(Modifier.fillMaxSize()) {
        val w = 1.5.dp.toPx()
        val r = Radius.card.toPx() - w / 2
        drawRoundRect(
            accent.copy(alpha = 0.5f * breathe.value),
            topLeft = Offset(w / 2, w / 2),
            size = Size(size.width - w, size.height - w),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(w),
        )
    }
}

/** Translation for a verse, or a calm note while it downloads. */
@Composable
private fun TodayTranslationBody(ref: VerseRef, modifier: Modifier = Modifier) {
    val translations = LocalGraph.current.translations
    val scope = rememberCoroutineScope()
    val text = translations.text(ref)
    Box(modifier) {
        if (text != null) {
            TranslationText(text, rightToLeft = translations.selected.isRightToLeft)
        } else {
            when (translations.selectedState) {
                TranslationStore.State.Downloading -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.xs),
                ) {
                    CircularProgressIndicator(Modifier.size(15.dp), color = Palette.textSecondary, strokeWidth = 2.dp)
                    Text("Preparing the translation", style = TextStyles.footnote, color = Palette.textSecondary)
                }
                TranslationStore.State.Failed, TranslationStore.State.NotDownloaded -> Row(
                    Modifier
                        .heightIn(min = 44.dp)
                        .pressable { scope.launch { translations.prepareSelected() } },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(symbol("arrow.down.circle"), null, tint = Palette.accent, modifier = Modifier.size(16.dp))
                    Text(
                        "Download translation",
                        style = TextStyles.footnote.copy(fontWeight = FontWeight.Medium),
                        color = Palette.accent,
                    )
                }
                TranslationStore.State.Ready -> Unit
            }
        }
    }
}

// Continue reading

@Composable
private fun ContinueReadingSection(modifier: Modifier = Modifier) {
    val graph = LocalGraph.current
    val router = LocalRouter.current
    val library = graph.library
    val last = library.lastRead
    val ref = last ?: VerseRef(1, 1)
    val surah = graph.quran.surah(ref.surah)

    CardSurface(modifier, padding = Space.xl) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.l)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        if (last == null) "Begin reading" else "Continue reading",
                        style = TextStyles.subheadline,
                        color = Palette.textSecondary,
                    )
                    Text(surah.name, style = TextStyles.title3, color = Palette.textPrimary)
                    RollingText(
                        if (last == null) "The Opening" else "Verse ${ref.ayah} of ${surah.verseCount}",
                        TextStyles.subheadline.copy(fontFeatureSettings = "tnum"),
                        Palette.textSecondary,
                    )
                }
                Text(
                    surah.arabicName,
                    style = TextStyle(fontFamily = AppFont.arabic, fontSize = 24.sp),
                    color = Palette.accent,
                    modifier = Modifier.inkReveal(delayMillis = 600, durationMillis = 800).clearAndSetSemantics { },
                )
            }

            if (last != null) {
                FillBar(
                    value = ref.ayah.toFloat() / maxOf(surah.verseCount, 1),
                    modifier = Modifier.semantics {
                        contentDescription = "Position in surah"
                        stateDescription = "Verse ${ref.ayah} of ${surah.verseCount}"
                    },
                )
            }

            PrimaryButton(
                text = if (last == null) "Start Reading" else "Continue",
                onClick = { router.push(Route.Reader(ref.surah, ref.ayah)) },
            )
        }
    }
}

/** A slim progress bar that fills to its value when it appears. */
@Composable
private fun FillBar(value: Float, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val target = value.coerceIn(0.02f, 1f)
    var shown by rememberSaveable { mutableStateOf(false) }
    val fill = remember { Animatable(if (shown || reduce) target else 0f) }
    LaunchedEffect(target) {
        if (reduce) {
            fill.snapTo(target)
        } else {
            if (!shown) delay(700)
            fill.animateTo(target, spring(dampingRatio = 0.85f, stiffness = 48f))
        }
        shown = true
    }
    val wash = Palette.accentWash
    val from = Palette.accentFill
    val to = Palette.accent
    Canvas(modifier.fillMaxWidth().height(6.dp)) {
        val r = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(wash, cornerRadius = r)
        val w = size.width * fill.value
        if (w > 0f) {
            drawRoundRect(
                Brush.horizontalGradient(listOf(from, to), startX = 0f, endX = size.width),
                size = Size(w, size.height),
                cornerRadius = r,
            )
        }
    }
}

// Today's question

/** One rotating question, by day. The same list as the iPhone's journal. */
private val DailyQuestions = listOf(
    "What is on your heart today?",
    "Where did you notice Allah's mercy this week?",
    "What are you asking Allah for right now?",
    "What blessing have you not thanked Allah for yet?",
    "What do you want to remember about today?",
    "What are you carrying that you can leave with Allah?",
    "Which verse has stayed with you lately, and why?",
)

/** Days since 1 January of year 1, like the iPhone's day count, so both phones ask the same question. */
private fun todayQuestion(date: LocalDate): String {
    val day = date.toEpochDay() + 719_163L
    return DailyQuestions[Math.floorMod(day, DailyQuestions.size.toLong()).toInt()]
}
