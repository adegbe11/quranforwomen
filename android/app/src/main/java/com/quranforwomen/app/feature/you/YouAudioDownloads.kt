package com.quranforwomen.app.feature.you

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.Reciter
import com.quranforwomen.app.data.RecitationDownloads
import com.quranforwomen.app.data.TranslationStore
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.PetalBurst
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.TextAction
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles

/** The Quran transliteration, kept like a translation. Swipe left to delete it. */
@Composable
internal fun YouTransliterationDownloads() {
    val context = LocalContext.current
    val store = LocalGraph.current.transliteration
    val haptics = rememberHaptics()
    val reduce = LocalReduceMotion.current
    val state = store.state

    var burst by remember { mutableIntStateOf(0) }
    val previous = remember { arrayOf(state) }
    LaunchedEffect(state) {
        val old = previous[0]
        previous[0] = state
        if (old == state) return@LaunchedEffect
        if (state == TranslationStore.State.Downloading) haptics.soft()
        if (old == TranslationStore.State.Downloading && state == TranslationStore.State.Ready) {
            haptics.success()
            burst++
        }
    }

    GroupedSection(title = "Transliteration", modifier = Modifier.dealIn(0, baseDelay = 400, step = 50)) {
        SwipeDeleteRow(
            deletable = state == TranslationStore.State.Ready,
            onDelete = { store.delete() },
        ) {
            Text("English transliteration", style = TextStyles.body, color = Palette.textPrimary, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(Space.s))
            Box(contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        if (reduce) fadeIn() togetherWith fadeOut()
                        else (scaleIn(Motion.reward(), initialScale = 0.5f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.5f) + fadeOut())
                    },
                    contentAlignment = Alignment.CenterEnd,
                    label = "transliterationDownload",
                ) { s ->
                    when (s) {
                        TranslationStore.State.Ready -> Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Icon(symbol("checkmark.circle.fill"), "Downloaded", tint = Palette.accent, modifier = Modifier.size(22.dp))
                            store.sizeOnDisk()?.let { size ->
                                Text(
                                    android.text.format.Formatter.formatShortFileSize(context, size),
                                    style = TextStyles.caption,
                                    color = Palette.textSecondary,
                                )
                            }
                        }
                        TranslationStore.State.Downloading -> CircularProgressIndicator(
                            Modifier.size(22.dp).semantics { contentDescription = "Downloading" },
                            color = Palette.accent,
                            strokeWidth = 2.dp,
                        )
                        TranslationStore.State.Failed -> TextAction("Retry", { store.downloadAsync() })
                        TranslationStore.State.NotDownloaded -> Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .pressable(scale = 0.9f) { store.downloadAsync() }
                                .semantics { contentDescription = "Download transliteration" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(symbol("arrow.down.circle"), null, tint = Palette.accent, modifier = Modifier.size(26.dp))
                        }
                    }
                }
                PetalBurst(trigger = burst)
            }
        }
    }
}

/** Surahs saved for offline listening, with their size. Swipe left to delete one. */
@Composable
internal fun YouRecitationDownloads() {
    val graph = LocalGraph.current
    val downloads = graph.recitations
    val quran = graph.quran
    val context = LocalContext.current
    val items = downloads.downloaded
    if (items.isEmpty()) return

    GroupedSection(title = "Recitations", modifier = Modifier.dealIn(1, baseDelay = 400, step = 50)) {
        items.forEachIndexed { index, item ->
            key(item.reciterID, item.surah) {
                if (index > 0) RowDivider(inset = Space.m)
                SwipeDeleteRow(
                    deletable = true,
                    onDelete = { downloads.delete(item.reciterID, item.surah) },
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(quran.surah(item.surah).name, style = TextStyles.body, color = Palette.textPrimary)
                        Text(Reciter.named(item.reciterID).name, style = TextStyles.subheadline, color = Palette.textSecondary)
                    }
                    Spacer(Modifier.width(Space.s))
                    if (downloads.state(item.reciterID, item.surah) == RecitationDownloads.State.Ready) {
                        downloads.sizeOnDisk(item.reciterID, item.surah)?.let { size ->
                            Text(
                                android.text.format.Formatter.formatShortFileSize(context, size),
                                style = TextStyles.subheadline,
                                color = Palette.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A row that reveals Delete when swiped left, with the same action for TalkBack. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeDeleteRow(
    deletable: Boolean,
    onDelete: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val canDelete by rememberUpdatedState(deletable)
    val delete by rememberUpdatedState(onDelete)
    val swipe = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && canDelete) delete()
            false
        },
    )
    val red = youDanger
    SwipeToDismissBox(
        state = swipe,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = deletable,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(red).padding(end = Space.l),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("Delete", style = TextStyles.headline, color = Color.White)
            }
        },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Palette.surface)
                .heightIn(min = 56.dp)
                .padding(horizontal = Space.m, vertical = 10.dp)
                .semantics {
                    if (deletable) {
                        customActions = listOf(CustomAccessibilityAction("Delete") { delete(); true })
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
