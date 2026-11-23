package com.quranforwomen.app.feature.duas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.ArabicSearch
import com.quranforwomen.app.data.ContentStore
import com.quranforwomen.app.data.Dua
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.ArabicText
import com.quranforwomen.app.ui.components.CardSurface
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.ListRow
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.SavedEmpty
import com.quranforwomen.app.ui.components.SearchField
import com.quranforwomen.app.ui.components.SourceLabel
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.inkReveal
import com.quranforwomen.app.ui.components.scrollSettle
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.BookInk
import com.quranforwomen.app.ui.theme.Ink
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import java.text.Normalizer
import kotlinx.coroutines.delay

/** The Duas tab: Dhikr, the categories, and her own words. Searching swaps in matching duas. */
@Composable
fun DuasScreen() {
    val graph = LocalGraph.current
    val content = graph.content
    val router = LocalRouter.current
    var query by rememberSaveable { mutableStateOf("") }
    val trimmed = query.trim()

    AppScreen(
        title = "Duas",
        book = true,
        spacing = if (trimmed.isEmpty()) Space.xl else Space.s,
        header = {
            Spacer(Modifier.height(Space.s))
            SearchField(query, { query = it }, "Search duas")
        },
    ) {
        if (trimmed.isEmpty()) {
            item(key = "dhikr") {
                GroupedSection(Modifier.dealIn(0)) {
                    ListRow(title = "Dhikr", icon = symbol("circle.dotted")) { router.push(Route.Dhikr) }
                }
            }
            val recent = graph.dhikrExtras.recentDuaIDs.mapNotNull { content.dua(it) }
            if (recent.isNotEmpty()) {
                item(key = "recent") {
                    RecentDuasRow(
                        duas = recent,
                        modifier = Modifier.animateItem().dealIn(1, baseDelay = 100),
                    ) { router.push(Route.DuaDetail(it.id)) }
                }
            }
            item(key = "categories") {
                GroupedSection(title = "Categories") {
                    content.categories.forEachIndexed { index, category ->
                        if (index > 0) RowDivider()
                        ListRow(
                            title = category.title,
                            icon = symbol(category.symbol),
                            value = "${content.duasIn(category).size}",
                            landDelayMillis = 250 + index * 50,
                            modifier = Modifier.dealIn(index, baseDelay = 200, step = 50),
                        ) { router.push(Route.DuaCategory(category.id)) }
                    }
                }
            }
            item(key = "personal") {
                GroupedSection(Modifier.dealIn(2, baseDelay = 500)) {
                    ListRow(title = "Your Own Words", icon = symbol("pencil")) { router.push(Route.PersonalDuas) }
                }
            }
        } else {
            val found = duaMatches(content, trimmed)
            if (found.isEmpty()) {
                item(key = "none") {
                    SavedEmpty(icon = symbol("magnifyingglass"), title = "No Results for “$trimmed”")
                }
            } else {
                items(found, key = { it.id }) { dua ->
                    DuaCard(
                        dua = dua,
                        modifier = Modifier.scrollSettle(),
                        onClick = { router.push(Route.DuaDetail(dua.id)) },
                    )
                }
            }
        }
    }
}

/** One category of duas. A small header comes into focus, then the cards are dealt in. */
@Composable
fun DuaCategoryScreen(id: String) {
    val content = LocalGraph.current.content
    val router = LocalRouter.current
    val category = content.category(id)
    AppScreen(title = category?.title ?: "Duas", book = true, spacing = Space.s) {
        if (category != null) {
            val duas = content.duasIn(category)
            item(key = "header") {
                CategoryHeader(symbolName = category.symbol, count = duas.size, modifier = Modifier.padding(bottom = Space.xs))
            }
            itemsIndexed(duas, key = { _, d -> d.id }) { index, dua ->
                val slot = minOf(index, 8)
                DuaCard(
                    dua = dua,
                    // The Arabic inks in just after this card has been dealt.
                    revealDelayMillis = 300 + slot * 60,
                    modifier = Modifier.dealIn(slot, baseDelay = 150, step = 60).scrollSettle(),
                    onClick = { router.push(Route.DuaDetail(dua.id)) },
                )
            }
        }
    }
}

@Composable
fun DuaDetailScreen(id: String) {
    DuaDetailContent(id)
}

@Composable
fun PersonalDuasScreen() {
    PersonalDuasContent()
}

@Composable
fun SavedDuasScreen() {
    SavedDuasContent()
}

// Parts

@Composable
private fun CategoryHeader(symbolName: String, count: Int, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (!reduce) {
            delay(200)
            bounce.snapTo(0.75f)
            bounce.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f))
        }
    }
    Row(
        modifier.fillMaxWidth().focusIn(50),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Box(
            Modifier
                .size(36.dp)
                .graphicsLayer { scaleX = bounce.value; scaleY = bounce.value }
                .clip(CircleShape)
                .background(BookInk.gild.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(symbol(symbolName), null, tint = BookInk.rule, modifier = Modifier.size(19.dp))
        }
        Text(if (count == 1) "1 dua" else "$count duas", style = TextStyles.subheadline, color = Ink.secondary)
    }
}

/**
 * A dua preview used in lists. Its Arabic line inks in right to left, a beat
 * after the card lands, so the card reads in the order the dua is said.
 */
@Composable
internal fun DuaCard(
    dua: Dua,
    modifier: Modifier = Modifier,
    revealDelayMillis: Int = 200,
    onClick: (() -> Unit)?,
) {
    CardSurface(modifier = modifier, onClick = onClick) {
        Text(dua.title, style = TextStyles.headline, color = Ink.text)
        Spacer(Modifier.height(Space.s))
        ArabicText(dua.arabic, size = 22f, maxLines = 2, modifier = Modifier.inkReveal(revealDelayMillis, 800))
        Spacer(Modifier.height(Space.s))
        Text(
            dua.translation,
            style = TextStyles.subheadline.copy(fontFamily = AppFont.serif),
            color = Ink.secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Space.s))
        SourceLabel(dua.sourceKind, dua.source)
    }
}

/** Folds case and accents, so "dua" finds "Duʿa" and "du'a". */
private fun fold(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

private fun duaMatches(content: ContentStore, trimmed: String): List<Dua> {
    val isArabic = ArabicSearch.containsArabic(trimmed)
    val needle = if (isArabic) ArabicSearch.normalize(trimmed) else fold(trimmed)
    return content.duas.filter { dua ->
        if (isArabic) return@filter ArabicSearch.normalize(dua.arabic).contains(needle)
        val fields = listOf(
            dua.title, dua.translation, dua.transliteration ?: "", dua.source,
            content.category(dua.category)?.title ?: "",
        )
        fields.any { fold(it).contains(needle) }
    }
}
