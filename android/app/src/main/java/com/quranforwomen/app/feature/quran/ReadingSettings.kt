package com.quranforwomen.app.feature.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.ReaderDefaults
import com.quranforwomen.app.data.Reciter
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.data.arabicIndic
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.ArabicText
import com.quranforwomen.app.ui.components.ListRow
import com.quranforwomen.app.ui.components.RollingNumber
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.SwitchRow
import com.quranforwomen.app.ui.components.inkReveal
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.rowPress
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Radius
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlin.math.roundToInt

/** The settings as a pushed screen in You. It carries the form's motion as is. */
@Composable
internal fun ReadingSettingsContent() {
    AppScreen(title = "Text Size & Recitation", largeTitle = false) {
        item(key = "form") {
            ReadingSettingsSections(showsLanguageLink = true, modifier = Modifier.padding(top = Space.s))
        }
    }
}

/** The form inside the reader's sheet. */
@Composable
internal fun ReadingSettingsScroll(showsLanguageLink: Boolean, surahID: Int? = null) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.page)
            .padding(top = Space.s, bottom = Space.l)
    ) {
        ReadingSettingsSections(showsLanguageLink, surahID = surahID)
    }
}

/**
 * Text size, translation visibility and reciter, with a live preview.
 * Motion: the preview verse inks in, then grows and shrinks smoothly as she moves a slider.
 * The translation fades in and out with its toggle, and the chosen reciter's checkmark pops into place.
 */
@Composable
private fun ReadingSettingsSections(showsLanguageLink: Boolean, modifier: Modifier = Modifier, surahID: Int? = null) {
    val graph = LocalGraph.current
    val prefs = graph.prefs
    val player = graph.player
    val translations = graph.translations
    val router = LocalRouter.current
    val reduce = LocalReduceMotion.current
    val haptics = rememberHaptics()
    val sample = remember { VerseRef(1, 2) }

    val arabicShown by animateFloatAsState(prefs.arabicSize, if (reduce) snap() else tween(300), label = "arabicSize")
    val translationShown by animateFloatAsState(prefs.translationSize, if (reduce) snap() else tween(300), label = "translationSize")

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.xl)) {
        FormSection("Preview") {
            Column(
                Modifier.padding(horizontal = Space.m, vertical = Space.m),
                verticalArrangement = Arrangement.spacedBy(Space.m),
            ) {
                ArabicText(
                    "${graph.quran.text(sample)} ﴿${sample.ayah.arabicIndic}﴾",
                    size = arabicShown,
                    color = Palette.textPrimary,
                    modifier = Modifier.inkReveal(200, 900),
                )
                TransliterationPreview(sample, translationShown)
                AnimatedVisibility(
                    visible = prefs.showTranslation,
                    enter = fadeIn(Motion.gentle()) + expandVertically(Motion.gentle()),
                    exit = fadeOut(Motion.gentle()) + shrinkVertically(Motion.gentle()),
                ) {
                    QuranTranslationBody(sample, translationShown)
                }
            }
        }

        FormSection("Arabic text size") {
            SizeSlider(prefs.arabicSize, ReaderDefaults.arabicRange, "Arabic text size") { prefs.arabicSize = it }
        }

        FormSection("Translation") {
            SwitchRow("Show translation", prefs.showTranslation, { prefs.showTranslation = it })
            AnimatedVisibility(
                visible = prefs.showTranslation,
                enter = fadeIn(Motion.gentle()) + expandVertically(Motion.gentle()),
                exit = fadeOut(Motion.gentle()) + shrinkVertically(Motion.gentle()),
            ) {
                Column {
                    RowDivider(Space.m)
                    SizeSlider(prefs.translationSize, ReaderDefaults.translationRange, "Translation text size") { prefs.translationSize = it }
                }
            }
            if (showsLanguageLink) {
                RowDivider(Space.m)
                ListRow("Language", value = translations.selected.nativeName, onClick = { router.push(Route.Language) })
            }
            RowDivider(Space.m)
            TransliterationSettingRows()
        }

        FormSection("Study") {
            StudySettingRows()
        }

        FormSection("Recitation") {
            Reciter.all.forEachIndexed { i, reciter ->
                if (i > 0) RowDivider(Space.m)
                ReciterRow(reciter.name, player.reciterID == reciter.id) {
                    if (player.reciterID != reciter.id) {
                        haptics.tick()
                        player.reciterID = reciter.id
                    }
                }
            }
            RowDivider(Space.m)
            com.quranforwomen.app.feature.audio.PlaybackSpeedRow()
            if (surahID != null) {
                RowDivider(Space.m)
                com.quranforwomen.app.feature.audio.SurahRecitationDownloadRow(surahID)
            }
        }

        FormSection(null) {
            val atDefaults = prefs.arabicSize == ReaderDefaults.arabicSize && prefs.translationSize == ReaderDefaults.translationSize
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .then(
                        if (atDefaults) Modifier
                        else Modifier.rowPress {
                            prefs.arabicSize = ReaderDefaults.arabicSize
                            prefs.translationSize = ReaderDefaults.translationSize
                        }
                    )
                    .padding(horizontal = Space.m, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    "Reset to default sizes",
                    style = TextStyles.body,
                    color = if (atDefaults) Palette.textSecondary.copy(alpha = 0.5f) else Palette.accent,
                )
            }
        }
    }
}

/** An inset grouped section with a small header, like an iOS form section. */
@Composable
private fun FormSection(header: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (header != null) {
            Text(
                header.uppercase(),
                style = TextStyles.footnote,
                color = Palette.textSecondary,
                modifier = Modifier.padding(horizontal = Space.m).semantics { heading() },
            )
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Radius.tile)).background(Palette.surface),
            content = content,
        )
    }
}

/** One reciter. The checkmark pops in with a small bounce when she picks this voice. */
@Composable
private fun ReciterRow(name: String, isSelected: Boolean, onClick: () -> Unit) {
    val reduce = LocalReduceMotion.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .rowPress(onClick)
            .padding(horizontal = Space.m, vertical = 12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = name
                selected = isSelected
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = TextStyles.body, color = Palette.textPrimary, modifier = Modifier.weight(1f))
        AnimatedVisibility(
            visible = isSelected,
            enter = if (reduce) fadeIn() else scaleIn(Motion.reward(), initialScale = 0.3f) + fadeIn(),
            exit = if (reduce) fadeOut() else scaleOut(targetScale = 0.3f) + fadeOut(),
        ) {
            Icon(symbol("checkmark"), null, tint = Palette.accent, modifier = Modifier.size(20.dp))
        }
    }
}

/** A size slider with the current size beside it. The number rolls as she slides. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SizeSlider(value: Float, range: ClosedFloatingPointRange<Float>, label: String, onChange: (Float) -> Unit) {
    val haptics = rememberHaptics()
    val fill = Palette.accentFill
    val trackColor = Palette.border
    val steps = (range.endInclusive - range.start).roundToInt() - 1
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Space.m, vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Icon(symbol("textformat.size.smaller"), null, tint = Palette.textSecondary, modifier = Modifier.size(15.dp))
        Slider(
            value = value,
            onValueChange = { v ->
                val rounded = v.roundToInt().toFloat()
                if (rounded != value) {
                    haptics.tick()
                    onChange(rounded)
                }
            },
            valueRange = range,
            steps = steps,
            modifier = Modifier
                .weight(1f)
                .semantics {
                    contentDescription = label
                    stateDescription = "${value.roundToInt()} points"
                },
            thumb = {
                Box(
                    Modifier
                        .size(26.dp)
                        .shadow(3.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            },
            track = { state ->
                val span = state.valueRange.endInclusive - state.valueRange.start
                val frac = if (span > 0f) ((state.value - state.valueRange.start) / span).coerceIn(0f, 1f) else 0f
                Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(trackColor)) {
                    Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(fill))
                }
            },
        )
        Icon(symbol("textformat.size.larger"), null, tint = Palette.textSecondary, modifier = Modifier.size(22.dp))
        Box(Modifier.widthIn(min = 24.dp), contentAlignment = Alignment.CenterEnd) {
            RollingNumber(
                value.roundToInt(),
                TextStyles.subheadline.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"),
                Palette.accent,
            )
        }
    }
}

