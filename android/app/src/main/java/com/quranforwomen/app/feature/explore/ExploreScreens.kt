package com.quranforwomen.app.feature.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.components.arabesque
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.HighlightColor
import com.quranforwomen.app.data.TranslationStore
import com.quranforwomen.app.data.VerseCollection
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.data.arabicIndic
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.ArabicText
import com.quranforwomen.app.ui.components.CardSurface
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.ListRow
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.TranslationText
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.inkReveal
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.scrollSettle
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.Ink
import com.quranforwomen.app.ui.theme.LocalDark
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Radius
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.pow

/** A spring described the iOS way: a response time in seconds and a damping fraction. */
internal fun <T> exploreSpring(response: Float, damping: Float) =
    spring<T>(dampingRatio = damping, stiffness = (2 * PI / response).pow(2).toFloat())

// Explore

/**
 * Topics, the reading plan and the quiz, in one quiet list.
 * Rows deal in one after another and each icon lands with a small bounce.
 */
@Composable
fun ExploreScreen() {
    val graph = LocalGraph.current
    val router = LocalRouter.current
    val topics = graph.content.topics
    val plan = graph.library.plan

    AppScreen(title = "Explore") {
        item(key = "topics") {
            GroupedSection(Modifier.focusIn(50), title = "Topics") {
                topics.forEachIndexed { index, topic ->
                    if (index > 0) RowDivider()
                    ListRow(
                        title = topic.title,
                        icon = symbol(topic.symbol),
                        subtitle = "${topic.verses.size} verses",
                        landDelayMillis = 300 + minOf(index, 8) * 50,
                        modifier = Modifier.dealIn(minOf(index, 8), baseDelay = 100, step = 50),
                        onClick = { router.push(Route.Collection(topic.id)) },
                    )
                }
            }
        }
        item(key = "practice") {
            GroupedSection(Modifier.focusIn(300), title = "Practice") {
                ListRow(
                    title = "Read the whole Quran",
                    icon = symbol("calendar"),
                    subtitle = plan?.let { "Day ${it.dayIndex() + 1} of ${it.days}" },
                    landDelayMillis = 550,
                    modifier = Modifier.dealIn(0, baseDelay = 350, step = 60),
                    onClick = { router.push(Route.ReadingPlan) },
                )
                RowDivider()
                ListRow(
                    title = "Quran quiz",
                    icon = symbol("questionmark.circle"),
                    landDelayMillis = 610,
                    modifier = Modifier.dealIn(1, baseDelay = 350, step = 60),
                    onClick = { router.push(Route.Quiz) },
                )
                RowDivider()
                val active = graph.challenges.activeCount
                ListRow(
                    title = "Challenges",
                    icon = symbol("target"),
                    subtitle = if (active > 0) "$active in progress" else null,
                    landDelayMillis = 670,
                    modifier = Modifier.dealIn(2, baseDelay = 350, step = 60),
                    onClick = { router.push(Route.Challenges) },
                )
            }
        }
        // Shown only once reviewed articles are bundled.
        val guidanceSections = graph.guidance.sections
        if (guidanceSections.isNotEmpty()) {
            item(key = "guidance") {
                GroupedSection(Modifier.focusIn(400), title = "Guidance") {
                    guidanceSections.forEachIndexed { index, section ->
                        if (index > 0) RowDivider()
                        ListRow(
                            title = section,
                            icon = symbol("book.pages"),
                            landDelayMillis = 750 + minOf(index, 8) * 50,
                            modifier = Modifier.dealIn(minOf(index, 8), baseDelay = 450, step = 50),
                            onClick = { router.push(Route.GuidanceSection(section)) },
                        )
                    }
                }
            }
        }
    }
}

// Feelings

/**
 * "How is your heart?" as soft tiles. The tiles deal in and each icon lands with a small bounce.
 * When she picks one, a gentle blush wash spreads across it with a light tick, and its verses open.
 * The wash fades away when she comes back.
 */
@Composable
fun FeelingsScreen() {
    val graph = LocalGraph.current
    val router = LocalRouter.current
    val haptics = rememberHaptics()
    val reduce = LocalReduceMotion.current
    val feelings = graph.content.feelings
    var picked by rememberSaveable { mutableStateOf<String?>(null) }

    // Back from the verses: let the wash fade.
    val onTop = router.current?.route == Route.Feelings
    LaunchedEffect(onTop) {
        if (onTop && picked != null) {
            if (!reduce) delay(350)
            picked = null
        }
    }

    val rows = remember(feelings) { feelings.withIndex().toList().chunked(2) }
    AppScreen(title = "How is your heart?", spacing = Space.s) {
        itemsIndexed(rows, key = { i, _ -> "row$i" }) { _, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.s)) {
                row.forEach { (index, feeling) ->
                    FeelingTile(
                        feeling = feeling,
                        jewel = com.quranforwomen.app.ui.components.Arabesque.jewels[index % com.quranforwomen.app.ui.components.Arabesque.jewels.size],
                        picked = picked == feeling.id,
                        landDelay = 300 + minOf(index, 8) * 50,
                        modifier = Modifier
                            .weight(1f)
                            .dealIn(minOf(index, 8), baseDelay = 120, step = 50)
                            .scrollSettle(),
                    ) {
                        picked = feeling.id
                        haptics.tick()
                        router.push(Route.Collection(feeling.id))
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** An icon that gives one small bounce as its row or tile lands. Plays once per visit. */
@Composable
internal fun rememberExploreIconLand(delayMillis: Int): Animatable<Float, *> {
    val reduce = LocalReduceMotion.current
    val land = remember { Animatable(1f) }
    var landed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (reduce || landed) return@LaunchedEffect
        delay(delayMillis.toLong())
        landed = true
        land.snapTo(0.75f)
        land.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f))
    }
    return land
}

/**
 * A jewel toned tile: deep colour, a faint gold star lattice, a gold medallion and an ivory title.
 * Picking it sends a sheen of gold across the tile before its verses open.
 */
@Composable
private fun FeelingTile(
    feeling: VerseCollection,
    jewel: com.quranforwomen.app.ui.components.Arabesque.Jewel,
    picked: Boolean,
    landDelay: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val reduce = LocalReduceMotion.current
    val sheen by animateFloatAsState(
        if (picked) 1f else 0f,
        if (reduce) snap() else if (picked) tween(520, easing = FastOutSlowInEasing) else Motion.smooth(),
        label = "sheen",
    )
    val land = rememberExploreIconLand(landDelay)
    val scope = rememberCoroutineScope()
    LaunchedEffect(picked) {
        if (picked && !reduce) {
            scope.launch {
                land.snapTo(0.8f)
                land.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 450f))
            }
        }
    }
    val gold = com.quranforwomen.app.ui.components.Arabesque.gold
    val ivory = com.quranforwomen.app.ui.components.Arabesque.ivory
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .fillMaxWidth()
            .heightIn(min = 136.dp)
            .pressable(onClickLabel = "Opens verses for this feeling", onClick = onClick)
            .clip(shape)
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(jewel.top, jewel.bottom)))
            .arabesque(gold, alpha = 0.13f, cell = 40.dp)
            .drawBehind {
                // Light falling on the top corner, like polished stone.
                drawCircle(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.15f, 0f), radius = size.width * 0.9f,
                    ),
                    radius = size.width * 0.9f, center = Offset(size.width * 0.15f, 0f),
                )
                val inset = 5.dp.toPx()
                drawRoundRect(
                    com.quranforwomen.app.ui.components.Arabesque.goldBrush(size.width),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - 2 * inset, size.height - 2 * inset),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(17.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()),
                    alpha = 0.55f,
                )
                if (sheen > 0f) {
                    // A band of gold light sweeps across.
                    val x = -size.width * 0.6f + size.width * 2.2f * sheen
                    drawRect(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(Color.Transparent, gold.copy(alpha = 0.35f * (1f - sheen * 0.5f)), Color.Transparent),
                            start = Offset(x - size.width * 0.4f, 0f), end = Offset(x + size.width * 0.4f, size.height),
                        )
                    )
                }
            }
            .semantics(mergeDescendants = true) { contentDescription = feeling.title }
            .padding(horizontal = Space.m, vertical = 18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .graphicsLayer { scaleX = land.value; scaleY = land.value }
                .drawBehind {
                    val c = Offset(size.width / 2, size.height / 2)
                    drawPath(
                        com.quranforwomen.app.ui.components.Arabesque.starPath(c, size.minDimension / 2, inner = 0.8f),
                        com.quranforwomen.app.ui.components.Arabesque.goldBrush(size.width),
                    )
                    drawCircle(jewel.bottom, radius = size.minDimension * 0.34f, center = c)
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(symbol(feeling.symbol), null, tint = gold, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.heightIn(min = Space.m))
        Text(
            feeling.title,
            style = TextStyles.headline.copy(fontFamily = AppFont.serif, fontSize = 20.sp, fontWeight = FontWeight.Medium),
            color = ivory,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// Collection

/** A feeling or topic's verses. They deal in one after another, ink in right to left, and settle as she scrolls. */
@Composable
fun CollectionScreen(id: String) {
    val graph = LocalGraph.current
    val collection = remember(id) { graph.content.collection(id) }
    AppScreen(title = collection?.title ?: "", book = true, spacing = Space.s) {
        if (collection != null) {
            itemsIndexed(collection.refs, key = { i, ref -> "$i-${ref.id}" }) { index, ref ->
                // Cards further down ink in soon after they scroll into view.
                ExploreVerseCard(
                    ref = ref,
                    inkDelay = if (index < 8) 300 + index * 80 else 200,
                    modifier = Modifier.dealIn(minOf(index, 8), baseDelay = 150, step = 70).scrollSettle(),
                )
            }
        }
    }
}

/** Soft highlight fills that keep text readable in both appearances. */
@Composable
@ReadOnlyComposable
private fun highlightFill(color: HighlightColor): Color {
    val dark = LocalDark.current
    return when (color) {
        HighlightColor.emerald -> if (dark) Color(0xFF1C3329) else Color(0xFFDCEDE5)
        HighlightColor.blush -> if (dark) Color(0xFF3D2830) else Color(0xFFF7E1E8)
        HighlightColor.gold -> if (dark) Color(0xFF3A3220) else Color(0xFFF6ECCF)
        HighlightColor.sky -> if (dark) Color(0xFF1E2B38) else Color(0xFFDFEAF4)
    }
}

/**
 * A verse with its translation, opening the reader. When asked, the Arabic inks in right to left.
 * A saved verse's bookmark gives one small bounce as the card lands, and a highlight colour washes in softly.
 */
@Composable
private fun ExploreVerseCard(ref: VerseRef, modifier: Modifier = Modifier, arabicSize: Float = 24f, inkDelay: Int? = null) {
    val graph = LocalGraph.current
    val router = LocalRouter.current
    val reduce = LocalReduceMotion.current
    val library = graph.library
    val saved = library.isBookmarked(ref)
    val highlight = library.highlight(ref)
    val fill by animateColorAsState(
        highlight?.let { highlightFill(it) } ?: Ink.surface,
        if (reduce) snap() else Motion.gentle(),
        label = "highlight",
    )
    val bounce = remember { Animatable(1f) }
    var landed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (reduce || landed || !saved) return@LaunchedEffect
        delay(450L + (inkDelay ?: 0))
        landed = true
        bounce.snapTo(0.7f)
        bounce.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
    }

    CardSurface(modifier, fill = fill, onClick = { router.push(Route.Reader(ref.surah, ref.ayah)) }) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            ArabicText(
                "${graph.quran.text(ref)} ﴿${ref.ayah.arabicIndic}﴾",
                size = arabicSize,
                modifier = if (inkDelay != null) Modifier.inkReveal(inkDelay, 1000) else Modifier,
            )
            ExploreTranslationBody(ref)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    graph.quran.displayName(ref),
                    style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold),
                    color = Ink.accent,
                    modifier = Modifier.weight(1f),
                )
                AnimatedVisibility(
                    visible = saved,
                    enter = if (reduce) fadeIn() else scaleIn(Motion.reward(), initialScale = 0.4f) + fadeIn(),
                    exit = if (reduce) fadeOut() else scaleOut(targetScale = 0.4f) + fadeOut(),
                ) {
                    Icon(
                        symbol("bookmark.fill"),
                        contentDescription = "Saved",
                        tint = Ink.warm,
                        modifier = Modifier.size(15.dp).graphicsLayer { scaleX = bounce.value; scaleY = bounce.value },
                    )
                }
            }
        }
    }
}

/** Translation for a verse, or a calm note while it downloads. */
@Composable
private fun ExploreTranslationBody(ref: VerseRef) {
    val translations = LocalGraph.current.translations
    val scope = rememberCoroutineScope()
    val text = translations.text(ref)
    if (text != null) {
        TranslationText(text, rightToLeft = translations.selected.isRightToLeft)
        return
    }
    when (translations.selectedState) {
        TranslationStore.State.Downloading -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            CircularProgressIndicator(Modifier.size(14.dp), color = Ink.secondary, strokeWidth = 2.dp)
            Text("Preparing the translation", style = TextStyles.footnote, color = Ink.secondary)
        }
        TranslationStore.State.Failed, TranslationStore.State.NotDownloaded -> Row(
            Modifier
                .heightIn(min = 44.dp)
                .pressable { scope.launch { translations.prepareSelected() } },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(symbol("arrow.down.circle"), null, tint = Ink.accent, modifier = Modifier.size(16.dp))
            Text("Download translation", style = TextStyles.footnote.copy(fontWeight = FontWeight.Medium), color = Ink.accent)
        }
        TranslationStore.State.Ready -> Unit
    }
}
