package com.quranforwomen.app.feature.onboarding

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.ReminderKind
import com.quranforwomen.app.data.ReminderMoment
import com.quranforwomen.app.data.ReminderScheduler
import com.quranforwomen.app.data.TranslationOption
import com.quranforwomen.app.data.TranslationStore
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.feature.today.CelestialArc
import com.quranforwomen.app.feature.today.RollingText
import com.quranforwomen.app.feature.today.SkyBackground
import com.quranforwomen.app.feature.today.SkyPalette
import com.quranforwomen.app.feature.today.TodayStatusBarIcons
import com.quranforwomen.app.feature.today.todayShortTime
import com.quranforwomen.app.ui.components.ArabicText
import com.quranforwomen.app.ui.components.LotusMark
import com.quranforwomen.app.ui.components.LotusPart
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.glass
import com.quranforwomen.app.ui.components.inkReveal
import com.quranforwomen.app.ui.components.lotusPath
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.rememberNotificationRequest
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.LocalDark
import com.quranforwomen.app.ui.theme.LocalInBook
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Radius
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private enum class Step { Welcome, Language, Moment, Ready }

/** Welcome, language, daily moment, then a small celebration. No sign up, no survey. */
@Composable
fun OnboardingFlow(onDone: () -> Unit) {
    // The photograph and the warm cream pages are a light scene, so onboarding stays light.
    CompositionLocalProvider(LocalDark provides false, LocalInBook provides false) {
        TodayStatusBarIcons(light = false)
        OnboardingSteps(onDone)
    }
}

/** Each step leaves with a soft blur and the next one arrives with staggered springs. */
@Composable
private fun OnboardingSteps(onDone: () -> Unit) {
    val reduce = LocalReduceMotion.current
    var step by rememberSaveable { mutableStateOf(Step.Welcome) }
    var forward by remember { mutableStateOf(true) }
    // Welcome to Language is a crossfade, so the button that glided down stays put.
    var handoff by remember { mutableStateOf(false) }
    var moment by rememberSaveable { mutableStateOf<ReminderMoment?>(null) }
    var reminderOn by rememberSaveable { mutableStateOf(false) }

    fun go(next: Step) {
        // Set the direction first, so the leaving step also slides the right way.
        forward = next > step
        handoff = (step == Step.Welcome && next == Step.Language) || (step == Step.Language && next == Step.Welcome)
        step = next
    }

    BackHandler(enabled = step == Step.Language || step == Step.Moment) {
        go(if (step == Step.Moment) Step.Language else Step.Welcome)
    }

    Box(Modifier.fillMaxSize()) {
        AmbientBackground()

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                if (reduce) {
                    fadeIn(tween(250, easing = FastOutSlowInEasing)) togetherWith fadeOut(tween(250, easing = FastOutSlowInEasing))
                } else if (handoff) {
                    fadeIn(spring(dampingRatio = 0.86f, stiffness = 130f)) togetherWith fadeOut(spring(dampingRatio = 0.86f, stiffness = 130f))
                } else {
                    val slide = spring<IntOffset>(dampingRatio = 0.86f, stiffness = 130f)
                    val fade = spring<Float>(dampingRatio = 0.86f, stiffness = 130f)
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally(slide) { it * dir } + fadeIn(fade)) togetherWith
                        (slideOutHorizontally(slide) { -it * dir } + fadeOut(fade))
                } using SizeTransform(clip = false)
            },
            label = "onboardingStep",
        ) { current ->
            // Comes into focus on the way in, softens on the way out.
            val blur by transition.animateFloat(label = "stepBlur") { if (it == EnterExitState.Visible) 0f else 14f }
            val blurs = !reduce && !handoff
            Box(Modifier.fillMaxSize().softBlur { if (blurs) blur else 0f }) {
                when (current) {
                    Step.Welcome -> OnboardingWelcome(onGetStarted = { go(Step.Language) }, onContinue = onDone)
                    Step.Language -> StepScaffold(
                        step = 1,
                        onBack = { go(Step.Welcome) },
                        content = { LanguageStep() },
                        footer = { OnboardingButton("Continue", symbol("arrow.right")) { go(Step.Moment) } },
                    )
                    Step.Moment -> StepScaffold(
                        step = 2,
                        onBack = { go(Step.Language) },
                        content = { MomentStep(moment) { moment = it } },
                        footer = {
                            MomentButton(moment) { enabled ->
                                reminderOn = enabled
                                go(Step.Ready)
                            }
                        },
                    )
                    Step.Ready -> ReadyStep(moment, reminderOn, onBegin = onDone)
                }
            }
        }
    }
}

// Shared pieces

/** A real blur where the phone supports it (Android 12 and later); elsewhere the fade carries it. */
internal fun Modifier.softBlur(radius: () -> Float): Modifier = graphicsLayer {
    val r = radius()
    renderEffect = if (Build.VERSION.SDK_INT >= 31 && r > 0.1f) {
        android.graphics.RenderEffect.createBlurEffect(r.dp.toPx(), r.dp.toPx(), android.graphics.Shader.TileMode.DECAL)
            .asComposeRenderEffect()
    } else null
}

/** The emerald action with its arrow on the right, as on the iPhone. */
@Composable
internal fun OnboardingButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    working: Boolean = false,
    dimWhenDisabled: Boolean = true,
    onClick: () -> Unit,
) {
    val dim = !enabled && !working && dimWhenDisabled
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .graphicsLayer { alpha = if (dim) 0.4f else 1f }
            .clip(CircleShape)
            .background(Palette.accentFill)
            .pressable(scale = 0.98f, enabled = enabled && !working, onClick = onClick)
            .padding(horizontal = Space.l),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (working) {
            CircularProgressIndicator(Modifier.size(22.dp), color = Palette.onAccentFill, strokeWidth = 2.dp)
        } else {
            AnimatedContent(text, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }, label = "buttonText") {
                Text(it, style = TextStyles.headline, color = Palette.onAccentFill)
            }
            Spacer(Modifier.width(Space.s))
            AnimatedContent(
                icon,
                transitionSpec = { (scaleIn(initialScale = 0.5f) + fadeIn()) togetherWith fadeOut(tween(150)) },
                label = "buttonIcon",
            ) { Icon(it, null, tint = Palette.onAccentFill, modifier = Modifier.size(20.dp)) }
        }
    }
}

/** Seconds since this view appeared, ticking at a gentle frame rate. Stays at 0 while paused. */
@Composable
private fun rememberClock(running: Boolean, fps: Int): State<Float> {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        val base = time.floatValue
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> time.floatValue = base + (now - start) / 1_000_000_000f }
            delay(1000L / fps)
        }
    }
    return time
}

/** A warm cream page with a very slow blush and emerald light drifting across it. */
@Composable
private fun AmbientBackground() {
    val reduce = LocalReduceMotion.current
    val time = rememberClock(running = !reduce, fps = 20)
    val page = Palette.background
    Canvas(Modifier.fillMaxSize()) {
        drawRect(page)
        val t = time.value
        val c = Offset(size.width / 2, size.height / 2)
        blob(
            Color(0xFFF5DCE3).copy(alpha = 0.55f), radius = 300.dp.toPx(),
            center = c + Offset((120 + 30 * sin(t / 7)).dp.toPx(), (-260 + 25 * cos(t / 9)).dp.toPx()),
        )
        blob(
            Color(0xFFDCEDE5).copy(alpha = 0.6f), radius = 280.dp.toPx(),
            center = c + Offset((-150 + 25 * cos(t / 8)).dp.toPx(), (300 + 30 * sin(t / 6)).dp.toPx()),
        )
    }
}

/** A soft round light, the same as a heavily blurred circle. */
private fun DrawScope.blob(color: Color, radius: Float, center: Offset) {
    drawCircle(
        Brush.radialGradient(
            0f to color, 0.5f to color.copy(alpha = color.alpha * 0.75f), 1f to color.copy(alpha = 0f),
            center = center, radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/** Back button, progress, a scrolling body and a pinned footer. */
@Composable
private fun StepScaffold(
    step: Int,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
    footer: @Composable () -> Unit,
) {
    val bars = WindowInsets.systemBars.asPaddingValues()
    Column(Modifier.fillMaxSize().padding(top = bars.calculateTopPadding())) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.xl).padding(top = Space.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .pressable(scale = 0.92f, onClick = onBack)
                    .semantics { contentDescription = "Back" },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(40.dp).glass(CircleShape, tint = Color.White.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null, tint = Palette.accentFill, modifier = Modifier.size(26.dp))
                }
            }
            ProgressSegments(current = step, total = 2, modifier = Modifier.weight(1f))
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Space.xl),
            content = content,
        )

        Box(Modifier.padding(start = Space.xl, end = Space.xl, top = Space.s, bottom = Space.m + bars.calculateBottomPadding())) {
            footer()
        }
    }
}

@Composable
private fun ProgressSegments(current: Int, total: Int, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val fill = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { fill.animateTo(1f, tween(800, 250, Motion.entranceEasing)) }
    val track = Palette.accentFill.copy(alpha = 0.12f)
    val bar = Palette.accentFill
    Row(
        modifier.clearAndSetSemantics { contentDescription = "Step $current of $total" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (i in 1..total) {
            Canvas(Modifier.weight(1f).height(4.dp)) {
                val r = CornerRadius(size.height / 2, size.height / 2)
                drawRoundRect(track, cornerRadius = r)
                val f = when {
                    i < current -> 1f
                    i == current -> fill.value
                    else -> 0f
                }
                if (f > 0f) drawRoundRect(bar, size = Size(size.width * f, size.height), cornerRadius = r)
            }
        }
    }
}

/** Large serif title, lotus above. Comes into focus on arrival. */
@Composable
private fun StepHeader(title: String) {
    val reduce = LocalReduceMotion.current
    val shown = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) { shown.animateTo(1f, Motion.entrance(120)) }
    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = shown.value
                translationY = (1f - shown.value) * 10.dp.toPx()
            }
            .softBlur { (1f - shown.value) * 8f },
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        LotusMark(Modifier.size(34.dp, 24.dp), color = Palette.accentFill, strokeWidth = 1.5.dp)
        Text(
            title,
            style = TextStyle(fontFamily = AppFont.serif, fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.Medium),
            color = Palette.accentFill,
            modifier = Modifier.semantics { heading() },
        )
    }
}

private fun Modifier.staggered(index: Int): Modifier = dealIn(index, baseDelay = 220, step = 45)

/** A checkmark that draws itself. */
@Composable
private fun CheckDraw(isOn: Boolean) {
    val p by animateFloatAsState(if (isOn) 1f else 0f, spring(dampingRatio = 0.65f, stiffness = 320f), label = "checkDraw")
    val on = Palette.accentFill
    val off = Palette.border
    Canvas(Modifier.size(26.dp)) {
        val sw = 1.5.dp.toPx()
        val radius = size.minDimension / 2
        drawCircle(if (isOn) on else off, radius = radius - sw / 2, style = Stroke(sw))
        val shown = p.coerceIn(0f, 1f)
        if (shown > 0f) drawCircle(on, radius = radius * (0.2f + 0.8f * p), alpha = shown)
        val pad = 7.dp.toPx()
        val w = size.width - 2 * pad
        val h = size.height - 2 * pad
        val check = Path().apply {
            moveTo(pad, pad + h / 2)
            lineTo(pad + w * 0.38f, pad + h - h * 0.1f)
            lineTo(pad + w, pad + h * 0.1f)
        }
        if (shown > 0f) {
            drawPath(
                trimmed(check, 0f, shown), Color.White,
                style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/**
 * Rows whose selection highlight glides from the previous choice to the new one.
 * Each row draws the part of the highlight that passes over it.
 */
@Composable
private fun <T> GlideList(
    items: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    label: @Composable (item: T, isSelected: Boolean) -> Unit,
) {
    val reduce = LocalReduceMotion.current
    val haptics = rememberHaptics()
    val bounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val selIndex = items.indexOf(selected)
    val y = remember { Animatable(0f) }
    val h = remember { Animatable(0f) }
    val shown = remember { Animatable(0f) }
    var placed by remember { mutableStateOf(false) }
    val selectedAtStart = remember { selected != null }
    val target = bounds[selIndex]

    LaunchedEffect(selIndex, target) {
        if (selIndex < 0 || target == null) return@LaunchedEffect
        if (!placed || reduce) {
            y.snapTo(target.first)
            h.snapTo(target.second)
            placed = true
            if (shown.value < 1f) {
                // Lands with its row when the list deals in.
                if (selectedAtStart && !reduce) delay(220L + 45L * (selIndex + 1))
                shown.animateTo(1f, tween(if (reduce) 0 else 250))
            }
        } else {
            val spec = spring<Float>(dampingRatio = 0.78f, stiffness = 224f)
            launch { h.animateTo(target.second, spec) }
            y.animateTo(target.first, spec)
        }
    }

    val wash = Palette.accentWash
    val edge = Palette.accentFill.copy(alpha = 0.35f)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selIndex
            Row(
                Modifier
                    .fillMaxWidth()
                    .onPlaced { c ->
                        val b = c.positionInParent().y to c.size.height.toFloat()
                        if (bounds[index] != b) bounds[index] = b
                    }
                    .staggered(index + 1)
                    .pressable(scale = 0.97f) {
                        if (!isSelected) haptics.tick()
                        onSelect(item)
                    }
                    .semantics(mergeDescendants = true) { this.selected = isSelected }
                    .drawBehind {
                        val r = CornerRadius(Radius.tile.toPx(), Radius.tile.toPx())
                        drawRoundRect(Color.White.copy(alpha = 0.7f), cornerRadius = r)
                        val a = shown.value
                        val top = bounds[index]?.first
                        if (a > 0f && placed && top != null) {
                            val clip = Path().apply { addRoundRect(RoundRect(size.toRect(), r)) }
                            clipPath(clip) {
                                val hy = y.value - top
                                val half = 0.5.dp.toPx()
                                drawRoundRect(wash, topLeft = Offset(0f, hy), size = Size(size.width, h.value), cornerRadius = r, alpha = a)
                                drawRoundRect(
                                    edge, topLeft = Offset(half, hy + half), size = Size(size.width - 2 * half, h.value - 2 * half),
                                    cornerRadius = r, style = Stroke(1.dp.toPx()), alpha = a,
                                )
                            }
                        }
                    }
                    .padding(horizontal = Space.m, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                Box(Modifier.weight(1f)) { label(item, isSelected) }
                CheckDraw(isSelected)
            }
        }
    }
}

// Language

@Composable
private fun LanguageStep() {
    val translations = LocalGraph.current.translations
    Column(verticalArrangement = Arrangement.spacedBy(Space.xl)) {
        StepHeader("Your Language")
        LanguagePreviewCard(Modifier.staggered(0))
        GlideList(
            items = TranslationOption.all,
            selected = TranslationOption.all.firstOrNull { it.id == translations.selectedID },
            onSelect = { translations.selectedID = it.id },
        ) { option, _ -> TranslationOptionLabel(option) }
    }
}

@Composable
private fun TranslationOptionLabel(option: TranslationOption) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(option.nativeName, style = TextStyles.headline, color = Palette.textPrimary)
        Text(
            if (option.language == option.nativeName) option.translator else "${option.language} · ${option.translator}",
            style = TextStyles.subheadline,
            color = Palette.textSecondary,
        )
    }
}

private data class PreviewMeaning(val optionId: String, val text: String?, val downloading: Boolean, val rtl: Boolean)

/**
 * Her choice made visible: 13:28 in Arabic, with its meaning in the language she picks.
 * The old meaning blurs away, soft lines hold the place while it downloads, then the new
 * meaning comes into focus. The text is the downloaded translation, never typed in.
 */
@Composable
private fun LanguagePreviewCard(modifier: Modifier = Modifier) {
    val graph = LocalGraph.current
    val translations = graph.translations
    val reduce = LocalReduceMotion.current
    val ref = VerseRef(13, 28)
    val option = translations.selected
    val meaning = PreviewMeaning(
        optionId = option.id,
        text = translations.text(ref),
        downloading = translations.selectedState == TranslationStore.State.Downloading,
        rtl = option.isRightToLeft,
    )
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(Color.White.copy(alpha = 0.7f))
            .padding(Space.l)
            .semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        ArabicText(graph.quran.text(ref), size = 22f, maxLines = 3, modifier = Modifier.inkReveal(delayMillis = 350, durationMillis = 1000))
        AnimatedContent(
            targetState = meaning,
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
            contentAlignment = if (meaning.rtl) Alignment.TopEnd else Alignment.TopStart,
            transitionSpec = {
                fadeIn(tween(450, easing = FastOutSlowInEasing)) togetherWith fadeOut(tween(450, easing = FastOutSlowInEasing)) using
                    SizeTransform(clip = false)
            },
            label = "meaning",
        ) { m ->
            val blur by transition.animateFloat(label = "meaningBlur") { if (it == EnterExitState.Visible) 0f else 14f }
            Box(Modifier.fillMaxWidth().softBlur { if (reduce) 0f else blur }) {
                when {
                    m.text != null -> Text(
                        m.text,
                        style = TextStyles.subheadline.copy(
                            fontFamily = AppFont.serif,
                            textAlign = if (m.rtl) TextAlign.End else TextAlign.Start,
                            textDirection = if (m.rtl) TextDirection.Rtl else TextDirection.Ltr,
                        ),
                        color = Palette.textSecondary,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    m.downloading -> ShimmerLines()
                    else -> Spacer(Modifier.fillMaxWidth())
                }
            }
        }
        Text("Ar-Ra'd 13:28", style = TextStyles.footnote.copy(fontWeight = FontWeight.Medium), color = Palette.accentFill)
    }
}

/** Three soft lines that breathe while a translation downloads. */
@Composable
private fun ShimmerLines() {
    val reduce = LocalReduceMotion.current
    val dim = rememberInfiniteTransition(label = "shimmer").animateFloat(
        1f, 0.45f, infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "shimmerAlpha",
    )
    val color = Palette.accentFill.copy(alpha = 0.1f)
    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (reduce) 1f else dim.value }
            .clearAndSetSemantics { contentDescription = "Loading" },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(1f, 0.85f, 0.55f).forEach { fraction ->
            Box(Modifier.fillMaxWidth(fraction).height(9.dp).clip(CircleShape).background(color))
        }
    }
}

// Daily moment

private fun momentSymbol(moment: ReminderMoment): String = when (moment) {
    ReminderMoment.morning -> "sunrise"
    ReminderMoment.afternoon -> "sun.max"
    ReminderMoment.evening -> "moon"
    ReminderMoment.later -> "clock"
}

@Composable
private fun MomentStep(choice: ReminderMoment?, onChoose: (ReminderMoment) -> Unit) {
    val wantsBanner = choice != null && choice != ReminderMoment.later
    var bannerShown by remember { mutableStateOf(wantsBanner) }
    var bannerDetail by remember { mutableStateOf(choice?.detail ?: "") }
    LaunchedEffect(choice) {
        if (wantsBanner) {
            bannerDetail = choice?.detail ?: bannerDetail
            if (!bannerShown) {
                delay(250)
                bannerShown = true
            }
        } else {
            bannerShown = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.xl)) {
        StepHeader("Your Daily Moment")

        Box(
            Modifier
                .staggered(0)
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(24.dp)),
        ) {
            MomentSky(choice, Modifier.matchParentSize())
            val bannerSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 158f)
            androidx.compose.animation.AnimatedVisibility(
                visible = bannerShown,
                modifier = Modifier.align(Alignment.TopCenter).padding(10.dp),
                enter = slideInVertically(spring(dampingRatio = 0.7f, stiffness = 158f)) { -it } +
                    fadeIn(bannerSpring) + scaleIn(bannerSpring, initialScale = 0.9f, transformOrigin = TransformOrigin(0.5f, 0f)),
                exit = slideOutVertically(tween(300)) { -it } + fadeOut(tween(300)),
            ) { NotificationPreview(bannerDetail) }
        }

        GlideList(
            items = ReminderMoment.entries,
            selected = choice,
            onSelect = onChoose,
        ) { item, isSelected -> MomentLabel(item, isSelected) }
    }
}

@Composable
private fun MomentLabel(item: ReminderMoment, isSelected: Boolean) {
    val reduce = LocalReduceMotion.current
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(isSelected) {
        if (isSelected && !reduce) {
            bounce.snapTo(0.75f)
            bounce.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f))
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
        Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
            Icon(
                symbol(momentSymbol(item)), null, tint = Palette.accentFill,
                modifier = Modifier.size(22.dp).graphicsLayer { scaleX = bounce.value; scaleY = bounce.value },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.title, style = TextStyles.headline, color = Palette.textPrimary)
            if (item != ReminderMoment.later) {
                Text(item.detail, style = TextStyles.subheadline, color = Palette.textSecondary)
            }
        }
    }
}

/** One full turn of the day. 0 is dawn; it comes round again at 1.4. */
private const val DayCycle = 1.4f

/** Position in the day: 0.14 morning, 0.5 midday, 0.9 sunset, 1.2 night. */
private fun momentPosition(choice: ReminderMoment?): Float = when (choice) {
    ReminderMoment.morning -> 0.14f
    ReminderMoment.afternoon -> 0.5f
    ReminderMoment.evening -> 0.9f
    ReminderMoment.later, null -> 1.2f
}

private val MomentStops = listOf(
    0f to SkyPalette.Fajr, 0.14f to SkyPalette.Sunrise, 0.5f to SkyPalette.Dhuhr, 0.72f to SkyPalette.Asr,
    0.9f to SkyPalette.Maghrib, 1.2f to SkyPalette.Night, 1.4f to SkyPalette.Fajr,
)

private fun momentPalette(time: Float): SkyPalette {
    if (time <= MomentStops[0].first) return MomentStops[0].second
    for (i in 0 until MomentStops.size - 1) {
        if (time <= MomentStops[i + 1].first) {
            val t = (time - MomentStops[i].first) / (MomentStops[i + 1].first - MomentStops[i].first)
            return MomentStops[i].second.mixed(MomentStops[i + 1].second, t)
        }
    }
    return MomentStops.last().second
}

/**
 * The sky at the time she picks. The day moves to it, always forward: the light changes
 * through the hours in between, the sun travels its arc, at night the moon rises and the
 * stars come out, and dawn breaks again. The chosen time rolls in the corner.
 */
@Composable
private fun MomentSky(choice: ReminderMoment?, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    // Grows without wrapping, so the day only ever turns forward.
    val dayTime = remember { Animatable(momentPosition(choice)) }
    LaunchedEffect(choice) {
        val current = dayTime.value
        var next = momentPosition(choice)
        while (next < current - 0.0001f) next += DayCycle
        val distance = next - current
        if (distance <= 0.0001f) return@LaunchedEffect
        val millis = if (reduce) 300 else (min(1.7f, 0.8f + distance * 0.6f) * 1000).toInt()
        dayTime.animateTo(next, tween(millis, easing = FastOutSlowInEasing))
    }

    val time = ((dayTime.value % DayCycle) + DayCycle) % DayCycle
    val sky = momentPalette(time)
    val night = time > 1f
    val ink = if (sky.isDark) Color.White else Palette.ink
    // The sun crosses by day; by night the moon crosses, rising after sunset and setting before dawn.
    val arc = if (night) (time - 1f) / (DayCycle - 1f) else time.coerceIn(0f, 1f)
    val minutes = choice?.minutes
    // Keeps the last time while the clock fades out for "I'll decide later".
    val held = remember { intArrayOf(minutes ?: 7 * 60) }
    if (minutes != null) held[0] = minutes
    val lastMinutes = held[0]
    val context = LocalContext.current

    Box(modifier.clearAndSetSemantics { }) {
        SkyBackground(sky, Modifier.matchParentSize())
        CelestialArc({ arc }, night, ink, Modifier.fillMaxSize().padding(top = 40.dp))
        AnimatedVisibility(
            visible = minutes != null,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = Space.l, bottom = Space.m),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            val clock = todayShortTime(context, LocalDate.now().atTime(LocalTime.of(lastMinutes / 60, lastMinutes % 60)).atZone(java.time.ZoneId.systemDefault()))
            RollingText(
                clock,
                TextStyle(fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.Light, fontFeatureSettings = "tnum"),
                ink,
            )
        }
    }
}

/** A preview of the real notification she will get. */
@Composable
private fun NotificationPreview(detail: String) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.12f), spotColor = Color.Black.copy(alpha = 0.12f))
            .clip(shape)
            .background(Color.White.copy(alpha = 0.93f))
            .padding(10.dp)
            .clearAndSetSemantics { contentDescription = "Preview of the reminder notification" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(Palette.accentFill).padding(7.dp),
        ) { LotusMark(Modifier.fillMaxSize(), color = Color.White, strokeWidth = 1.2.dp) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    ReminderKind.quran.title,
                    style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold),
                    color = Palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(detail.replace("Around ", ""), style = TextStyle(fontSize = 11.sp), color = Palette.textSecondary)
            }
            Text(
                ReminderKind.quran.body,
                style = TextStyles.footnote,
                color = Palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The footer button for the moment step. Asks permission only here, after she chose. */
@Composable
private fun MomentButton(choice: ReminderMoment?, onDone: (Boolean) -> Unit) {
    val graph = LocalGraph.current
    val prefs = graph.prefs
    val context = LocalContext.current
    val reduce = LocalReduceMotion.current
    var working by remember { mutableStateOf(false) }
    val minutes = choice?.minutes
    val request = rememberNotificationRequest { granted ->
        if (minutes != null) {
            if (granted) ReminderScheduler.enable(context, minutes, ReminderKind.quran)
            prefs.reminderEnabled = granted
            prefs.reminderMinutes = minutes
        }
        working = false
        onDone(granted && minutes != null)
    }
    val quiet = choice == null || choice == ReminderMoment.later

    // A small spring the first time she chooses, as the button comes awake.
    val wake = remember { Animatable(1f) }
    var wasEmpty by remember { mutableStateOf(choice == null) }
    LaunchedEffect(choice == null) {
        if (wasEmpty == (choice == null)) return@LaunchedEffect
        wasEmpty = choice == null
        if (!reduce) {
            wake.animateTo(1.04f, spring(dampingRatio = 0.5f, stiffness = 600f))
            wake.animateTo(1f, spring(dampingRatio = 1f, stiffness = 300f))
        }
    }

    OnboardingButton(
        text = if (quiet) "Continue" else "Turn On Reminder",
        icon = if (quiet) symbol("arrow.right") else Icons.Filled.Notifications,
        enabled = choice != null && !working,
        working = working,
        modifier = Modifier.graphicsLayer { scaleX = wake.value; scaleY = wake.value },
    ) {
        if (minutes != null) {
            working = true
            request()
        } else {
            onDone(false)
        }
    }
}

// Ready

/**
 * The one celebratory moment, in the same language as the splash: the lotus is drawn petal
 * by petal, fills with light and a gold shimmer passes once, the Bismillah writes itself in,
 * her two choices land, and Begin dissolves the lotus as the app opens.
 */
@Composable
private fun ReadyStep(moment: ReminderMoment?, reminderOn: Boolean, onBegin: () -> Unit) {
    val graph = LocalGraph.current
    val reduce = LocalReduceMotion.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()

    val center = remember { Animatable(0f) }
    val sides = remember { Animatable(0f) }
    val water = remember { Animatable(0f) }
    val shimmer = remember { Animatable(-1f) }
    var bloomed by remember { mutableStateOf(false) }
    var showText by remember { mutableStateOf(false) }
    var showChoices by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (reduce) {
            center.snapTo(1f); sides.snapTo(1f); water.snapTo(1f)
            bloomed = true; showText = true; showChoices = true; showButton = true
            haptics.success()
            return@LaunchedEffect
        }
        launch { delay(200); center.animateTo(1f, tween(750, easing = FastOutSlowInEasing)) }
        launch { delay(400); sides.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        launch { delay(600); water.animateTo(1f, tween(600, easing = LinearOutSlowInEasing)) }
        launch { delay(1150); bloomed = true; haptics.success() }
        launch { delay(1200); shimmer.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        launch { delay(1450); showText = true }
        launch { delay(2000); showChoices = true }
        launch { delay(2400); showButton = true }
    }

    val bloom by animateFloatAsState(if (bloomed) 1f else 0f, if (reduce) snap() else spring(dampingRatio = 0.6f, stiffness = 80f), label = "bloom")
    val textIn by animateFloatAsState(if (showText) 1f else 0f, if (reduce) snap() else Motion.entrance(), label = "textIn")
    val buttonIn by animateFloatAsState(if (showButton) 1f else 0f, if (reduce) snap() else Motion.entrance(), label = "buttonIn")
    val leave by animateFloatAsState(if (leaving) 1f else 0f, tween(if (reduce) 200 else 450, easing = FastOutLinearInEasing), label = "leave")

    val languageName = TranslationOption.all.firstOrNull { it.id == graph.translations.selectedID }?.nativeName ?: "English"

    /** The lotus grows and dissolves, the rest falls away, then the app opens. */
    fun begin() {
        if (leaving) return
        leaving = true
        scope.launch {
            delay(if (reduce) 200 else 450)
            onBegin()
        }
    }

    val bars = WindowInsets.systemBars.asPaddingValues()
    Box(Modifier.fillMaxSize()) {
        if (bloomed && !reduce && !leaving) RisingLight(Modifier.fillMaxSize())

        Column(
            Modifier.fillMaxSize().padding(top = bars.calculateTopPadding(), bottom = bars.calculateBottomPadding()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            ReadyLotus(
                center = { center.value }, sides = { sides.value }, water = { water.value },
                bloom = { bloom }, shimmer = { shimmer.value },
                modifier = Modifier
                    .graphicsLayer {
                        val s = 1f + 0.4f * leave
                        scaleX = s; scaleY = s
                        alpha = 1f - leave
                    }
                    .softBlur { 8f * leave }
                    .clearAndSetSemantics { },
            )

            Spacer(Modifier.height(Space.xl))

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Space.xl)
                    .graphicsLayer {
                        alpha = textIn.coerceIn(0f, 1f) * (1f - leave)
                        translationY = if (reduce) 0f else (1f - textIn) * 10.dp.toPx()
                    }
                    .softBlur { if (reduce) 0f else (1f - textIn).coerceAtLeast(0f) * 10f },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                // Keyed, so the Bismillah writes itself in when it appears.
                key(showText) {
                    ArabicText(
                        graph.quran.basmala, size = 26f, center = true, color = Palette.accentFill,
                        modifier = Modifier.inkReveal(delayMillis = 100, durationMillis = 1000),
                    )
                }
                Text(
                    "Bismillah",
                    style = TextStyle(fontFamily = AppFont.serif, fontSize = 40.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
                    color = Palette.accentFill,
                )
            }

            Spacer(Modifier.height(Space.xl))

            Column(
                Modifier.graphicsLayer { alpha = 1f - leave },
                verticalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                ChoiceLine(symbol("globe"), languageName, showChoices, 0)
                ChoiceLine(
                    if (reminderOn) Icons.Filled.Notifications else Icons.Outlined.NotificationsOff,
                    if (reminderOn) moment?.detail ?: "" else "No reminder",
                    showChoices, 1,
                )
            }

            Spacer(Modifier.weight(1f))

            OnboardingButton(
                text = "Begin",
                icon = symbol("arrow.right"),
                enabled = showButton && !leaving,
                dimWhenDisabled = false,
                modifier = Modifier
                    .padding(horizontal = Space.xl)
                    .padding(bottom = Space.m)
                    .graphicsLayer {
                        alpha = buttonIn.coerceIn(0f, 1f) * (1f - leave)
                        translationY = if (reduce) 0f else (1f - buttonIn) * 14.dp.toPx()
                    },
            ) { begin() }
        }
    }
}

private val Gold = Color(0xFFD3A94A)

@Composable
private fun ReadyLotus(
    center: () -> Float,
    sides: () -> Float,
    water: () -> Float,
    bloom: () -> Float,
    shimmer: () -> Float,
    modifier: Modifier = Modifier,
) {
    val accent = Palette.accentFill
    val glow = Color(0xFFF6ECCF)
    Box(modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(260.dp)) {
            val b = bloom()
            val r = 130.dp.toPx() * (0.3f + 0.7f * b)
            if (b > 0f) {
                drawCircle(
                    Brush.radialGradient(listOf(glow, glow.copy(alpha = 0f)), center = this.center, radius = r.coerceAtLeast(1f)),
                    radius = r.coerceAtLeast(1f),
                    alpha = b.coerceIn(0f, 1f),
                )
            }
        }
        Canvas(
            Modifier
                .size(120.dp, 86.dp)
                .graphicsLayer {
                    val s = 1f + 0.06f * bloom()
                    scaleX = s; scaleY = s
                },
        ) {
            val rect = Rect(Offset.Zero, size)
            val stroke = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val all = lotusPath(LotusPart.All, rect)
            drawPath(all, accent.copy(alpha = 0.08f * bloom().coerceIn(0f, 1f)))
            drawPath(trimmed(lotusPath(LotusPart.Center, rect), 0f, center()), accent, style = stroke)
            drawPath(trimmed(lotusPath(LotusPart.Left, rect), 0f, sides()), accent, style = stroke)
            drawPath(trimmed(lotusPath(LotusPart.Right, rect), 0f, sides()), accent, style = stroke)
            val w = water()
            drawPath(trimmed(lotusPath(LotusPart.Water, rect), 0.5f - w / 2, 0.5f + w / 2), accent, style = stroke)
            // One pass of gold light across the lotus, only where the lines are.
            val sh = shimmer()
            if (sh > -1f && sh < 1f) {
                val x = size.width / 2 + sh * 100.dp.toPx()
                drawPath(
                    all,
                    Brush.horizontalGradient(listOf(Color.Transparent, Gold, Color.Transparent), startX = x - 23.dp.toPx(), endX = x + 23.dp.toPx()),
                    style = stroke,
                )
            }
        }
    }
}

/** One of her choices, landing with a small check. */
@Composable
private fun ChoiceLine(icon: ImageVector, text: String, shown: Boolean, index: Int) {
    val reduce = LocalReduceMotion.current
    val row = remember { Animatable(if (shown) 1f else 0f) }
    val check = remember { Animatable(if (shown) 1f else 0f) }
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(shown) {
        val target = if (shown) 1f else 0f
        if (reduce) {
            row.snapTo(target); check.snapTo(target)
            return@LaunchedEffect
        }
        launch {
            delay(index * 120L)
            row.animateTo(target, spring(dampingRatio = 0.78f, stiffness = 158f))
        }
        launch {
            delay(150L + index * 120L)
            check.animateTo(target, spring(dampingRatio = 0.55f, stiffness = 247f))
        }
        if (shown) {
            bounce.snapTo(0.7f)
            bounce.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f))
        }
    }
    val style = TextStyles.subheadline.copy(fontWeight = FontWeight.Medium)
    Row(
        Modifier
            .width(260.dp)
            .height(44.dp)
            .graphicsLayer {
                alpha = row.value.coerceIn(0f, 1f)
                translationY = (1f - row.value) * 12.dp.toPx()
            }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.7f))
            .padding(horizontal = Space.m)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(22.dp), contentAlignment = Alignment.Center) {
            Icon(
                icon, null, tint = Palette.accentFill,
                modifier = Modifier.size(18.dp).graphicsLayer { scaleX = bounce.value; scaleY = bounce.value },
            )
        }
        Spacer(Modifier.width(Space.s))
        Text(text, style = style, color = Palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(Space.m))
        Icon(
            symbol("checkmark.circle.fill"), null, tint = Palette.accentFill,
            modifier = Modifier.size(20.dp).graphicsLayer {
                val s = 0.3f + 0.7f * check.value
                scaleX = s; scaleY = s
                alpha = check.value.coerceIn(0f, 1f)
            },
        )
    }
}

private class Speck(val x: Float, val speed: Float, val size: Float, val phase: Float)

private val Specks: List<Speck> = List(36) { i ->
    val a = i.toDouble()
    Speck(
        x = ((a * 0.618) % 1).toFloat(),
        speed = (0.05 + ((a * 0.37) % 1) * 0.08).toFloat(),
        size = (2 + ((a * 0.71) % 1) * 3).toFloat(),
        phase = ((a * 0.29) % 1).toFloat(),
    )
}

/** Soft specks of light that drift upward and fade. Drawn in one Canvas. */
@Composable
private fun RisingLight(modifier: Modifier = Modifier) {
    val time = rememberClock(running = true, fps = 30)
    Canvas(modifier) {
        val t = time.value
        for (s in Specks) {
            val progress = (t * s.speed + s.phase) % 1f
            val y = size.height * (1f - progress)
            val x = size.width * s.x + 12.dp.toPx() * sin(t * 0.8f + s.phase * 6f)
            val alpha = (sin(progress * PI.toFloat()) * 0.7f).coerceIn(0f, 1f)
            val d = s.size.dp.toPx()
            drawOval(Gold.copy(alpha = alpha), topLeft = Offset(x, y), size = Size(d, d))
        }
    }
}

/** The part of a path from one fraction of its length to another, like SwiftUI's trim. */
private fun trimmed(path: Path, from: Float, to: Float): Path {
    val out = android.graphics.Path()
    val start = from.coerceIn(0f, 1f)
    val end = to.coerceIn(0f, 1f)
    if (end <= start) return out.asComposePath()
    val src = path.asAndroidPath()
    val measure = android.graphics.PathMeasure(src, false)
    val lengths = mutableListOf<Float>()
    do { lengths += measure.length } while (measure.nextContour())
    val total = lengths.sum()
    val walk = android.graphics.PathMeasure(src, false)
    var walked = 0f
    for (len in lengths) {
        val s = (start * total - walked).coerceIn(0f, len)
        val e = (end * total - walked).coerceIn(0f, len)
        if (e > s) walk.getSegment(s, e, out, true)
        walked += len
        if (!walk.nextContour()) break
    }
    return out.asComposePath()
}
