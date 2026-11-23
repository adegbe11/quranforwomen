package com.quranforwomen.app.feature.quran

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.quranforwomen.app.data.QuranStore
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.data.arabicIndic
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** One page of the Madani mushaf: the verses printed on it. */
internal data class MushafPageData(val number: Int, val verses: List<VerseRef>)

/**
 * The 604 pages of the Madani mushaf, built from the bundled page number of every verse.
 * The text is the verified Uthmani text; only the grouping into pages comes from the mushaf.
 */
internal fun buildMushafPages(quran: QuranStore): List<MushafPageData> {
    val pages = sortedMapOf<Int, MutableList<VerseRef>>()
    for (surah in quran.surahs) {
        surah.pages.forEachIndexed { index, page ->
            pages.getOrPut(page) { mutableListOf() }.add(VerseRef(surah.id, index + 1))
        }
    }
    return pages.map { (number, verses) -> MushafPageData(number, verses) }
}

private sealed interface MushafBlock {
    data class Header(val surah: Int) : MushafBlock
    data object Basmala : MushafBlock
    data class Body(val text: String) : MushafBlock
}

/** Verses run together the way they are printed; a new surah starts with its band and the Basmala. */
private fun mushafBlocks(page: MushafPageData, quran: QuranStore): List<MushafBlock> {
    val result = mutableListOf<MushafBlock>()
    val running = StringBuilder()
    for (ref in page.verses) {
        if (ref.ayah == 1) {
            if (running.isNotEmpty()) { result += MushafBlock.Body(running.toString()); running.clear() }
            result += MushafBlock.Header(ref.surah)
            if (quran.surah(ref.surah).showsBasmala) result += MushafBlock.Basmala
        }
        running.append(quran.text(ref)).append(" ﴿").append(ref.ayah.arabicIndic).append("﴾ ")
    }
    if (running.isNotEmpty()) result += MushafBlock.Body(running.toString())
    return result
}

/** The type sizes tried in turn, largest first, until the page fits. */
private val MushafSizes = listOf(25f, 23f, 21f, 19.5f, 18f, 16.5f, 15f, 13.5f, 12f)

/**
 * One leaf: cream paper, a gold double frame with corner stars, the surah and juz at the top,
 * the verses justified right to left with their verse marks, and the page number in a medallion.
 */
@Composable
internal fun MushafPageView(
    page: MushafPageData,
    quran: QuranStore,
    hasRibbon: Boolean,
    visible: Boolean,
    sizeCache: MutableMap<String, Float>,
    modifier: Modifier = Modifier,
) {
    val first = page.verses.firstOrNull() ?: VerseRef(1, 1)
    val blocks = remember(page.number) { mushafBlocks(page, quran) }
    val shape = RoundedCornerShape(6.dp)
    val density = LocalDensity.current
    val caption = TextStyle(
        fontFamily = AppFont.arabic,
        fontSize = with(density) { 15.dp.toSp() },
        lineHeight = with(density) { 24.dp.toSp() },
        textDirection = TextDirection.Rtl,
    )
    Box(
        modifier
            .fillMaxSize()
            .clearAndSetSemantics {
                contentDescription = "Page ${page.number}, ${quran.surah(first.surah).name}, from verse ${first.ayah}"
            }
    ) {
        Box(
            Modifier
                .matchParentSize()
                .shadow(10.dp, shape, ambientColor = Color.Black, spotColor = Color.Black)
                .background(Brush.horizontalGradient(listOf(MushafInk.paperEdge, MushafInk.paper, MushafInk.paperLit)), shape)
        )
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(top = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("الجزء ${quran.juz(first).arabicIndic}", style = caption, color = MushafInk.caption)
                    Text(quran.surah(first.surah).arabicName, style = caption, color = MushafInk.caption)
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp)) {
                MushafFrame(Modifier.matchParentSize())
                MushafFitted(
                    cacheKey = page.number,
                    blocks = blocks,
                    quran = quran,
                    visible = visible,
                    cache = sizeCache,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 16.dp),
                )
            }
            Box(
                Modifier
                    .padding(bottom = 12.dp)
                    .defaultMinSize(minWidth = 34.dp, minHeight = 26.dp)
                    .border(1.dp, MushafInk.gild, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(page.number.arabicIndic, style = caption, color = MushafInk.caption)
            }
        }
        MushafRibbon(show = hasRibbon && visible)
    }
}

/** Sets the page at the largest size that fits, like print. The size found is kept for next time. */
@Composable
private fun MushafFitted(
    cacheKey: Int,
    blocks: List<MushafBlock>,
    quran: QuranStore,
    visible: Boolean,
    cache: MutableMap<String, Float>,
    modifier: Modifier,
) {
    SubcomposeLayout(modifier) { constraints ->
        if (!constraints.hasBoundedHeight || !constraints.hasBoundedWidth) {
            return@SubcomposeLayout layout(0, 0) {}
        }
        val loose = Constraints(
            minWidth = constraints.maxWidth, maxWidth = constraints.maxWidth,
            minHeight = 0, maxHeight = Constraints.Infinity,
        )
        val key = "$cacheKey/${constraints.maxWidth}x${constraints.maxHeight}"
        val cached = cache[key]
        val order = if (cached != null) listOf(cached) + MushafSizes.filter { it < cached } else MushafSizes
        var chosen: Placeable? = null
        for (size in order) {
            val placeable = subcompose(size) { MushafBlocks(blocks, size, quran, visible) }
                .firstOrNull()?.measure(loose) ?: continue
            chosen = placeable
            if (placeable.height <= constraints.maxHeight) {
                cache[key] = size
                break
            }
        }
        layout(constraints.maxWidth, constraints.maxHeight) { chosen?.place(0, 0) }
    }
}

@Composable
private fun MushafBlocks(blocks: List<MushafBlock>, size: Float, quran: QuranStore, visible: Boolean) {
    val density = LocalDensity.current
    // Fixed sizes on the page, so it stays in proportion like print.
    val style = TextStyle(
        fontFamily = AppFont.arabic,
        fontSize = with(density) { size.dp.toSp() },
        lineHeight = with(density) { (size * 1.9f).dp.toSp() },
        textDirection = TextDirection.Rtl,
        localeList = LocaleList("ar"),
        color = MushafInk.ink,
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy((size * 0.45f).dp)) {
            for (block in blocks) {
                when (block) {
                    is MushafBlock.Header -> MushafSurahBand(quran.surah(block.surah).arabicName, size, visible)
                    MushafBlock.Basmala -> Text(
                        quran.basmala,
                        style = style.copy(textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    is MushafBlock.Body -> Text(
                        block.text,
                        style = style.copy(textAlign = TextAlign.Justify),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** The band that opens a surah, in gold. */
@Composable
private fun MushafSurahBand(name: String, size: Float, visible: Boolean) {
    val reduce = LocalReduceMotion.current
    val lit = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible && !reduce && lit.value == 0f) {
            delay(350)
            lit.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
        }
    }
    val density = LocalDensity.current
    Box(
        Modifier
            .fillMaxWidth()
            .height((size * 1.9f).dp)
            .drawBehind {
                val r = CornerRadius(4.dp.toPx())
                drawRoundRect(MushafInk.goldLight.copy(alpha = 0.4f), cornerRadius = r)
                val outer = 1.2.dp.toPx()
                drawRoundRect(
                    MushafInk.gild, topLeft = Offset(outer / 2, outer / 2),
                    size = Size(this.size.width - outer, this.size.height - outer),
                    cornerRadius = r, style = Stroke(outer),
                )
                val inset = 3.dp.toPx()
                val inner = 0.6.dp.toPx()
                drawRoundRect(
                    MushafInk.rule.copy(alpha = 0.6f), topLeft = Offset(inset + inner / 2, inset + inner / 2),
                    size = Size(this.size.width - 2 * inset - inner, this.size.height - 2 * inset - inner),
                    cornerRadius = CornerRadius(3.dp.toPx()), style = Stroke(inner),
                )
            }
            .drawWithContent {
                drawContent()
                val p = lit.value
                if (p <= 0f || p >= 1f) return@drawWithContent
                val w = this.size.width
                val band = w * 0.3f
                val x = -band + (w + band) * p
                clipPath(Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, w, this@drawWithContent.size.height, CornerRadius(4.dp.toPx())))
                }) {
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.55f), Color.Transparent),
                            startX = x, endX = x + band,
                        ),
                        topLeft = Offset(x, 0f), size = Size(band, this.size.height),
                        blendMode = BlendMode.Plus,
                    )
                }
            }
            .clearAndSetSemantics { contentDescription = "Surah $name" },
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            MushafStar(Modifier.size((size * 0.55f).dp))
            Spacer(Modifier.weight(1f))
            Text(
                "سورة $name",
                style = TextStyle(
                    fontFamily = AppFont.arabic,
                    fontSize = with(density) { (size * 0.9f).dp.toSp() },
                    textDirection = TextDirection.Rtl,
                ),
                color = MushafInk.caption,
                maxLines = 1,
                softWrap = false,
            )
            Spacer(Modifier.weight(1f))
            MushafStar(Modifier.size((size * 0.55f).dp))
        }
    }
}

@Composable
private fun MushafStar(modifier: Modifier, color: Color = MushafInk.gild) {
    Canvas(modifier) { drawPath(mushafStarPath(center, this.size.minDimension / 2), color) }
}

/** The gold double rule around the text, with a star in each corner. */
@Composable
private fun MushafFrame(modifier: Modifier) {
    Canvas(modifier) {
        val outer = 1.4.dp.toPx()
        drawRect(
            MushafInk.gild, topLeft = Offset(outer / 2, outer / 2),
            size = Size(size.width - outer, size.height - outer), style = Stroke(outer),
        )
        val inset = 4.dp.toPx()
        val inner = 0.7.dp.toPx()
        drawRect(
            MushafInk.rule.copy(alpha = 0.6f), topLeft = Offset(inset + inner / 2, inset + inner / 2),
            size = Size(size.width - 2 * inset - inner, size.height - 2 * inset - inner), style = Stroke(inner),
        )
        val e = 2.dp.toPx()
        val r = 6.dp.toPx()
        for (corner in 0 until 4) {
            val x = if (corner % 2 == 0) e else size.width - e
            val y = if (corner < 2) e else size.height - e
            drawPath(mushafStarPath(Offset(x, y), r), MushafInk.gild)
        }
    }
}

/** An eight pointed star, the common ornament of mushaf borders. */
internal fun mushafStarPath(center: Offset, outer: Float): Path {
    val inner = outer * 0.48f
    return Path().apply {
        for (i in 0 until 16) {
            val angle = i * PI / 8 - PI / 2
            val r = if (i % 2 == 0) outer else inner
            val x = center.x + (cos(angle) * r).toFloat()
            val y = center.y + (sin(angle) * r).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

/** The silk ribbon that keeps her place. It drops in from the top edge. */
@Composable
private fun BoxScope.MushafRibbon(show: Boolean) {
    val reduce = LocalReduceMotion.current
    val drop by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = if (reduce) snap() else spring(dampingRatio = 0.55f, stiffness = 130f),
        label = "ribbon",
    )
    if (!show && drop <= 0.001f) return
    Canvas(
        Modifier
            .align(Alignment.TopStart)
            .padding(start = 34.dp)
            .size(18.dp, 90.dp)
            .graphicsLayer {
                translationY = (1f - drop) * -96.dp.toPx()
                rotationZ = (1f - drop) * -6f
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
    ) { drawRibbon() }
}

private fun DrawScope.drawRibbon() {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(0f, 0f)
        lineTo(w, 0f)
        lineTo(w, h)
        lineTo(w / 2, h * 80f / 90f)
        lineTo(0f, h)
        close()
    }
    translate(top = 1.dp.toPx()) { drawPath(path, Color.Black.copy(alpha = 0.25f)) }
    drawPath(path, Brush.horizontalGradient(listOf(MushafInk.ribbon, MushafInk.ribbonDeep)))
}
