package com.quranforwomen.app.feature.quran

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.TranslationStore
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.SwitchRow
import com.quranforwomen.app.ui.components.TextAction
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.Ink
import com.quranforwomen.app.ui.theme.Motion
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles

/** The verse in Latin letters, set a little smaller than the translation, in italic. */
@Composable
internal fun QuranTransliterationLine(ref: VerseRef, size: Float = 17f, modifier: Modifier = Modifier) {
    val text = LocalGraph.current.transliteration.text(ref) ?: return
    val s = size * 0.92f
    Text(
        text,
        style = TextStyles.body.copy(
            fontFamily = AppFont.serif,
            fontStyle = FontStyle.Italic,
            fontSize = s.sp,
            lineHeight = (s * 1.4f).sp,
        ),
        color = Ink.secondary,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * The Transliteration switch for the reading settings. While it downloads a small spinner
 * shows beside it, and if it could not download a Retry appears.
 */
@Composable
internal fun TransliterationSettingRows() {
    val store = LocalGraph.current.transliteration
    val state = store.state
    SwitchRow("Transliteration", store.show, { store.show = it })
    AnimatedVisibility(
        visible = store.show && (state == TranslationStore.State.Downloading || state == TranslationStore.State.Failed),
        enter = fadeIn(Motion.gentle()) + expandVertically(Motion.gentle()),
        exit = fadeOut(Motion.gentle()) + shrinkVertically(Motion.gentle()),
    ) {
        Column {
            RowDivider(Space.m)
            AnimatedContent(
                targetState = state == TranslationStore.State.Failed,
                transitionSpec = { fadeIn(Motion.gentle()) togetherWith fadeOut(Motion.gentle()) },
                label = "transliterationState",
            ) { failed ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(horizontal = Space.m, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.xs),
                ) {
                    if (failed) {
                        Text("Could not download", style = TextStyles.subheadline, color = Ink.secondary, modifier = Modifier.weight(1f))
                        TextAction("Retry", { store.downloadAsync() })
                    } else {
                        CircularProgressIndicator(
                            color = Ink.secondary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp).semantics { contentDescription = "Downloading" },
                        )
                        Text("Downloading", style = TextStyles.subheadline, color = Ink.secondary)
                    }
                }
            }
        }
    }
}

/** The preview's transliteration, fading in and out with the switch. */
@Composable
internal fun TransliterationPreview(ref: VerseRef, size: Float) {
    val store = LocalGraph.current.transliteration
    AnimatedVisibility(
        visible = store.show && store.text(ref) != null,
        enter = fadeIn(Motion.gentle()) + expandVertically(Motion.gentle()),
        exit = fadeOut(Motion.gentle()) + shrinkVertically(Motion.gentle()),
    ) {
        Box { QuranTransliterationLine(ref, size) }
    }
}
