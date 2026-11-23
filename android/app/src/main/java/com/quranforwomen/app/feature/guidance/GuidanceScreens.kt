package com.quranforwomen.app.feature.guidance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.GuidanceArticle
import com.quranforwomen.app.data.SourceKind
import com.quranforwomen.app.data.TranslationStore
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.data.arabicIndic
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.ArabicText
import com.quranforwomen.app.ui.components.CardSurface
import com.quranforwomen.app.ui.components.Eyebrow
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.ListRow
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.SectionHeader
import com.quranforwomen.app.ui.components.SourceLabel
import com.quranforwomen.app.ui.components.TranslationText
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.inkReveal
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.scrollSettle
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.Ink
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Guidance and devotionals. Every word here comes from guidance.json, written by a named
// author and approved by a named reviewer. While there are none, these screens are never reached.

/** The articles in one section, in one quiet list. */
@Composable
fun GuidanceSectionScreen(section: String) {
    val graph = LocalGraph.current
    val router = LocalRouter.current
    val articles = remember(section) { graph.guidance.articles(section) }
    AppScreen(title = section) {
        item(key = "articles") {
            GroupedSection(Modifier.focusIn(50)) {
                articles.forEachIndexed { index, article ->
                    if (index > 0) RowDivider(inset = Space.m)
                    ListRow(
                        title = article.title,
                        modifier = Modifier.dealIn(minOf(index, 8), baseDelay = 100, step = 50),
                        onClick = { router.push(Route.GuidanceArticle(article.id)) },
                    )
                }
            }
        }
    }
}

/** One article, set like a book. Its verses open the reader. */
@Composable
fun GuidanceArticleScreen(id: String) {
    val graph = LocalGraph.current
    val article = remember(id) { graph.guidance.article(id) }
    AppScreen(title = "", largeTitle = false, book = true, spacing = Space.l) {
        if (article == null) return@AppScreen
        item(key = "title") {
            Column(Modifier.focusIn(50), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Eyebrow(article.section)
                Text(
                    article.title,
                    style = TextStyles.serifTitle,
                    color = Ink.text,
                    modifier = Modifier.semantics { heading() },
                )
            }
        }
        itemsIndexed(article.body, key = { i, _ -> "p$i" }) { index, paragraph ->
            TranslationText(paragraph, Modifier.dealIn(minOf(index, 8), baseDelay = 150, step = 60))
        }
        val verses = article.verses.filter { graph.quran.contains(it) }
        if (verses.isNotEmpty()) {
            item(key = "quranHeader") {
                SectionHeader("From the Quran", Modifier.padding(top = Space.s).focusIn(300))
            }
            itemsIndexed(verses, key = { i, ref -> "v$i-${ref.id}" }) { index, ref ->
                GuidanceVerseCard(
                    ref = ref,
                    inkDelay = 350 + minOf(index, 6) * 80,
                    modifier = Modifier.dealIn(minOf(index, 8), baseDelay = 300, step = 70).scrollSettle(),
                )
            }
        }
        if (article.hadith.isNotEmpty()) {
            item(key = "hadith") {
                Column(Modifier.padding(top = Space.s), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    SectionHeader("From the Sunnah")
                    article.hadith.forEachIndexed { index, citation ->
                        SourceLabel(SourceKind.Hadith, citation, Modifier.dealIn(minOf(index, 8), baseDelay = 350, step = 50))
                    }
                }
            }
        }
        item(key = "credits") { GuidanceCredits(article, Modifier.padding(top = Space.s)) }
    }
}

/** Who wrote it and who reviewed it. */
@Composable
private fun GuidanceCredits(article: GuidanceArticle, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) { }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        val author = article.author?.trim().orEmpty()
        if (author.isNotEmpty()) Text("Written by $author", style = TextStyles.footnote, color = Ink.secondary)
        val reviewer = article.reviewer?.trim().orEmpty()
        if (reviewer.isNotEmpty()) {
            val date = article.reviewedOn?.takeIf { it.isNotBlank() }?.let { raw ->
                runCatching { LocalDate.parse(raw).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) }.getOrDefault(raw)
            }
            Text(
                if (date != null) "Reviewed by $reviewer · $date" else "Reviewed by $reviewer",
                style = TextStyles.footnote,
                color = Ink.secondary,
            )
        }
    }
}

/** A verse with its translation, opening the reader. The Arabic inks in right to left. */
@Composable
internal fun GuidanceVerseCard(ref: VerseRef, modifier: Modifier = Modifier, inkDelay: Int = 300) {
    val graph = LocalGraph.current
    val router = LocalRouter.current
    val translations = graph.translations
    val scope = rememberCoroutineScope()
    CardSurface(modifier, onClick = { router.push(Route.Reader(ref.surah, ref.ayah)) }) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            ArabicText("${graph.quran.text(ref)} ﴿${ref.ayah.arabicIndic}﴾", Modifier.inkReveal(inkDelay, 1000), size = 24f)
            val text = translations.text(ref)
            if (text != null) {
                TranslationText(text, rightToLeft = translations.selected.isRightToLeft)
            } else when (translations.selectedState) {
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
            Text(
                graph.quran.displayName(ref),
                style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold),
                color = Ink.accent,
            )
        }
    }
}
