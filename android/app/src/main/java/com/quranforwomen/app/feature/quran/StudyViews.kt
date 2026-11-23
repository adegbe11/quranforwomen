package com.quranforwomen.app.feature.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.HideLevel
import com.quranforwomen.app.data.HifzStore
import com.quranforwomen.app.data.QuranWord
import com.quranforwomen.app.data.StudyState
import com.quranforwomen.app.data.TafsirEdition
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.data.WordLanguage
import com.quranforwomen.app.ui.components.AppSheet
import com.quranforwomen.app.ui.components.ArabicText
import com.quranforwomen.app.ui.components.ChoiceRow
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.SwitchRow
import com.quranforwomen.app.ui.components.TextAction
import com.quranforwomen.app.data.arabicIndic
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.BookInk
import com.quranforwomen.app.ui.theme.Ink
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles

// Verse words

/**
 * The Arabic of a verse laid out word by word, right to left.
 * With word by word on, each word carries its meaning; tapping a word says it.
 * In memorizing mode some words hide behind a soft bar until she taps them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VerseWords(ref: VerseRef, text: String, arabicSize: Float, translationSize: Float, modifier: Modifier = Modifier) {
    val graph = LocalGraph.current
    val wordStore = graph.words
    val level = graph.hifz.level
    val surahWords = if (wordStore.show) wordStore.words(ref.surah) else null
    val apiWords = surahWords?.getOrNull(ref.ayah - 1)
    val plain = remember(text) { HifzStore.splitWords(text) }
    val count = apiWords?.size ?: plain.size
    val revealed = remember(ref.id, level, count) { mutableStateListOf<Int>() }
    val haptics = rememberHaptics()

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (apiWords != null) 10.dp else 7.dp),
                verticalArrangement = Arrangement.spacedBy(if (apiWords != null) Space.s else 2.dp),
            ) {
                for (i in 0 until count) {
                    val hidden = level.hides(i) && i !in revealed
                    val word = apiWords?.get(i)
                    WordCell(
                        arabic = word?.arabic ?: plain[i],
                        meaning = word?.meaning,
                        sound = word?.sound,
                        meaningRtl = wordStore.language.rightToLeft,
                        hidden = hidden,
                        arabicSize = if (word != null) arabicSize * 0.86f else arabicSize,
                        meaningSize = (translationSize * 0.78f).coerceAtLeast(12f),
                        onTap = {
                            if (hidden) { haptics.tick(); revealed += i }
                            else if (word != null) wordStore.say(word)
                        },
                    )
                }
                // The verse number closes the verse, as in the printed Mushaf.
                Text(
                    "﴿${ref.ayah.arabicIndic}﴾",
                    style = arabicStyle(if (apiWords != null) arabicSize * 0.86f else arabicSize),
                    color = BookInk.ink,
                    modifier = Modifier.align(Alignment.Top),
                )
            }
        }
        if (wordStore.show && apiWords == null) WordsStatus(ref.surah)
        if (level != HideLevel.Off && revealed.size < (0 until count).count { level.hides(it) }) {
            TextAction(
                "Show all words",
                onClick = { revealed.clear(); revealed.addAll(0 until count) },
                modifier = Modifier.semantics { contentDescription = "Show all hidden words in this verse" },
            )
        }
    }
}

private fun arabicStyle(size: Float) = TextStyle(
    fontFamily = AppFont.arabic,
    fontSize = size.sp,
    lineHeight = (size * 1.7f).sp,
    textDirection = TextDirection.Rtl,
    localeList = LocaleList("ar"),
)

@Composable
private fun WordCell(
    arabic: String,
    meaning: String?,
    sound: String?,
    meaningRtl: Boolean,
    hidden: Boolean,
    arabicSize: Float,
    meaningSize: Float,
    onTap: () -> Unit,
) {
    val reduce = LocalReduceMotion.current
    val ink by animateColorAsState(if (hidden) Color.Transparent else BookInk.ink, if (reduce) snap() else tween(320, easing = FastOutSlowInEasing), label = "wordInk")
    val veil by animateFloatAsState(if (hidden) 1f else 0f, if (reduce) snap() else tween(320, easing = FastOutSlowInEasing), label = "wordVeil")
    val label = when {
        hidden -> "Hidden word. Tap to show."
        meaning != null -> "$arabic, $meaning"
        else -> arabic
    }
    Column(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BookInk.rule.copy(alpha = 0.32f * veil))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onTap,
            )
            .padding(horizontal = 3.dp)
            .semantics(mergeDescendants = true) { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(arabic, style = arabicStyle(arabicSize), color = ink)
        if (meaning != null) {
            val captionInk = if (hidden) Color.Transparent else BookInk.caption
            if (!sound.isNullOrBlank()) {
                Text(
                    sound,
                    style = TextStyles.caption.copy(fontStyle = FontStyle.Italic, fontSize = (meaningSize * 0.9f).sp),
                    color = captionInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 120.dp),
                )
            }
            CompositionLocalProvider(LocalLayoutDirection provides if (meaningRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                Text(
                    meaning,
                    style = TextStyle(fontFamily = AppFont.serif, fontSize = meaningSize.sp, lineHeight = (meaningSize * 1.25f).sp),
                    color = if (hidden) Color.Transparent else BookInk.ink.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 120.dp).padding(bottom = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun WordsStatus(surah: Int) {
    val words = LocalGraph.current.words
    when (words.state(surah)) {
        StudyState.Failed -> Row(
            Modifier.heightIn(min = 44.dp).pressable(scale = 0.96f) { words.retry(surah) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(symbol("wifi.exclamationmark"), null, tint = Ink.accent, modifier = Modifier.size(16.dp))
            Text("Word meanings need a connection. Try again", style = TextStyles.footnote.copy(fontWeight = FontWeight.Medium), color = Ink.accent)
        }
        else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            CircularProgressIndicator(color = Ink.secondary, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
            Text("Loading word meanings", style = TextStyles.footnote, color = Ink.secondary)
        }
    }
}

// Memorizing progress

/** Under the surah title while memorizing: how many verses she has learned here. */
@Composable
internal fun HifzProgress(surah: Int, verseCount: Int, modifier: Modifier = Modifier) {
    val hifz = LocalGraph.current.hifz
    AnimatedVisibility(
        visible = hifz.isOn,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        val learned = hifz.learnedCount(surah)
        val p by animateFloatAsState(if (verseCount == 0) 0f else learned.toFloat() / verseCount, tween(600), label = "hifzProgress")
        Column(Modifier.fillMaxWidth().padding(bottom = Space.l), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$learned of $verseCount verses learned",
                style = TextStyles.footnote.copy(fontWeight = FontWeight.Medium),
                color = BookInk.rubric,
            )
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth(0.5f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(BookInk.rule.copy(alpha = 0.3f))) {
                Box(Modifier.fillMaxWidth(p).height(4.dp).clip(RoundedCornerShape(2.dp)).background(BookInk.gild))
            }
        }
    }
}

// Tafsir

/** The explanation of a verse from a published tafsir, in the book's page tone. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun TafsirSheet(ref: VerseRef, onDismiss: () -> Unit) {
    val graph = LocalGraph.current
    val tafsir = graph.tafsir
    val surah = graph.quran.surah(ref.surah)
    val entry = tafsir.entry(ref)
    val edition = tafsir.edition
    var pickerOpen by remember { mutableStateOf(false) }

    AppSheet(onDismiss = onDismiss, book = true, skipPartiallyExpanded = false) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Space.page).padding(top = Space.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }) {
                Text("Tafsir", style = TextStyles.title3.copy(fontFamily = AppFont.serif), color = BookInk.ink)
                Text("${surah.name} · Verse ${ref.ayah}", style = TextStyles.subheadline, color = BookInk.caption)
            }
            Box {
                Row(
                    Modifier.heightIn(min = 44.dp).pressable(scale = 0.96f) { pickerOpen = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(edition.title, style = TextStyles.footnote.copy(fontWeight = FontWeight.Medium), color = BookInk.rubric)
                    Icon(symbol("chevron.up.chevron.down"), null, tint = BookInk.rubric, modifier = Modifier.size(14.dp))
                }
                DropdownMenu(expanded = pickerOpen, onDismissRequest = { pickerOpen = false }) {
                    TafsirEdition.entries.forEach { e ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(e.title, style = TextStyles.body)
                                    Text(e.language, style = TextStyles.caption, color = Ink.secondary)
                                }
                            },
                            trailingIcon = if (e == edition) ({ Icon(symbol("checkmark"), null, tint = Ink.accent) }) else null,
                            onClick = { pickerOpen = false; tafsir.edition = e },
                        )
                    }
                }
            }
        }

        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.page)
                .padding(top = Space.l),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            ArabicText("${graph.quran.text(ref)} ﴿${ref.ayah.arabicIndic}﴾", size = 24f, color = BookInk.ink)
            when {
                entry != null -> {
                    if (entry.firstAyah != entry.lastAyah) {
                        Text(
                            "Explains verses ${entry.firstAyah} to ${entry.lastAyah} together",
                            style = TextStyles.footnote.copy(fontWeight = FontWeight.Medium),
                            color = BookInk.rubric,
                        )
                    }
                    val body = remember(entry.html) {
                        // Headings read as bold lines of the same size, so the page stays calm.
                        val calm = entry.html.replace(Regex("<h[1-6][^>]*>"), "<p><b>").replace(Regex("</h[1-6]>"), "</b></p>")
                        AnnotatedString.fromHtml(calm)
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides if (edition.rightToLeft) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                        Text(
                            body,
                            style = TextStyle(
                                fontFamily = if (edition.rightToLeft) AppFont.arabic else AppFont.serif,
                                fontSize = if (edition.rightToLeft) 20.sp else 17.sp,
                                lineHeight = if (edition.rightToLeft) 36.sp else 26.sp,
                                textDirection = if (edition.rightToLeft) TextDirection.Rtl else TextDirection.Ltr,
                            ),
                            color = BookInk.ink,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Text(
                        "${edition.title} · ${edition.author} · Quran.com",
                        style = TextStyles.caption,
                        color = BookInk.caption,
                        modifier = Modifier.padding(top = Space.s),
                    )
                }
                tafsir.state(ref.surah) == StudyState.Failed -> Row(
                    Modifier.heightIn(min = 44.dp).pressable(scale = 0.96f) { tafsir.retry(ref.surah) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(symbol("wifi.exclamationmark"), null, tint = BookInk.rubric, modifier = Modifier.size(16.dp))
                    Text("Tafsir needs a connection. Try again", style = TextStyles.footnote.copy(fontWeight = FontWeight.Medium), color = BookInk.rubric)
                }
                else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                    CircularProgressIndicator(color = BookInk.caption, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                    Text("Loading tafsir", style = TextStyles.footnote, color = BookInk.caption)
                }
            }
        }
    }
}

// Settings

/** Word by word and memorizing, inside the reading settings. */
@Composable
internal fun StudySettingRows() {
    val graph = LocalGraph.current
    val words = graph.words
    val hifz = graph.hifz
    SwitchRow("Word by word", words.show, { words.show = it })
    AnimatedVisibility(
        visible = words.show,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Column {
            WordLanguage.entries.forEach { lang ->
                RowDivider(Space.m)
                ChoiceRow("Meanings in ${lang.title}", words.language == lang, { words.language = lang })
            }
        }
    }
    RowDivider(Space.m)
    var open by remember { mutableStateOf(false) }
    Box {
        com.quranforwomen.app.ui.components.ListRow(
            "Memorize",
            value = hifz.level.title,
            onClick = { open = true },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            HideLevel.entries.forEach { l ->
                DropdownMenuItem(
                    text = { Text(l.title, style = TextStyles.body) },
                    trailingIcon = if (l == hifz.level) ({ Icon(symbol("checkmark"), null, tint = Ink.accent) }) else null,
                    onClick = { open = false; hifz.level = l },
                )
            }
        }
    }
}
