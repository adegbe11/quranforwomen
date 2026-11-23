package com.quranforwomen.app.feature.duas

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.Dua
import com.quranforwomen.app.feature.quran.renderVerseShareImage
import com.quranforwomen.app.ui.components.copyText
import com.quranforwomen.app.ui.components.shareFile
import com.quranforwomen.app.ui.theme.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.ArabicText
import com.quranforwomen.app.ui.components.BarIcon
import com.quranforwomen.app.ui.components.Eyebrow
import com.quranforwomen.app.ui.components.PetalBurst
import com.quranforwomen.app.ui.components.SavedEmpty
import com.quranforwomen.app.ui.components.SourceLabel
import com.quranforwomen.app.ui.components.TranslationText
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.inkReveal
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.shareText
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.BookInk
import com.quranforwomen.app.ui.theme.Ink
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Radius
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles

/**
 * One dua, unfolding in reading order: the title comes into focus, the Arabic inks in
 * right to left, then the transliteration and meaning follow. Saving pops the bookmark
 * and lets a few petals fly. Listen and Pause morph into each other.
 */
@Composable
internal fun DuaDetailContent(id: String) {
    val graph = LocalGraph.current
    val dua = graph.content.dua(id)

    if (dua == null) {
        AppScreen(title = "", largeTitle = false, book = true) {
            item { SavedEmpty(icon = symbol("questionmark"), title = "Dua not found") }
        }
        return
    }

    val prefs = graph.prefs
    val player = graph.player
    val library = graph.library
    val reduce = LocalReduceMotion.current
    val haptics = rememberHaptics()

    LaunchedEffect(dua.id) { graph.dhikrExtras.noteOpened(dua.id) }

    AppScreen(
        title = "",
        largeTitle = false,
        book = true,
        spacing = Space.xl,
        actions = {
            DuaShareMenu(dua, shareTextFor(dua))
        },
    ) {
        item(key = "title") {
            Column(
                Modifier.fillMaxWidth().padding(top = Space.xs).focusIn(50),
                verticalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                Eyebrow(dua.kindTitle)
                Text(
                    dua.title,
                    style = TextStyles.serifTitle,
                    color = BookInk.ink,
                    modifier = Modifier.semantics { heading() },
                )
            }
        }

        item(key = "arabic") {
            val playing = isPlayingDua(dua)
            Column(Modifier.fillMaxWidth().padding(vertical = Space.xs), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                ArabicText(dua.arabic, size = maxOf(prefs.arabicSize, 28f), modifier = Modifier.inkReveal(300, 1300))
                AnimatedVisibility(visible = playing, enter = fadeIn(), exit = fadeOut()) {
                    RecitationLine(player.itemProgress)
                }
            }
        }

        val transliteration = dua.transliteration
        if (prefs.showTransliteration && transliteration != null) {
            item(key = "transliteration") {
                Text(
                    transliteration,
                    style = TextStyles.body.copy(fontStyle = FontStyle.Italic),
                    color = BookInk.caption,
                    modifier = Modifier.fillMaxWidth().focusIn(1200),
                )
            }
        }

        item(key = "meaning") {
            Column(Modifier.fillMaxWidth().focusIn(1400), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text("Meaning", style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold), color = BookInk.caption)
                TranslationText(dua.translation, size = 20f)
            }
        }

        val note = dua.note
        if (note != null) {
            item(key = "note") {
                Text(
                    note,
                    style = TextStyles.subheadline,
                    color = BookInk.caption,
                    modifier = Modifier
                        .dealIn(0, baseDelay = 1550)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Radius.control))
                        .background(BookInk.pageEdge.copy(alpha = 0.7f))
                        .padding(Space.m),
                )
            }
        }

        item(key = "source") {
            Box(Modifier.dealIn(1, baseDelay = 1550, step = 80)) {
                SourceLabel(dua.sourceKind, dua.source)
            }
        }

        item(key = "actions") {
            Row(
                Modifier.dealIn(2, baseDelay = 1550, step = 80),
                horizontalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                val range = dua.playableRange
                if (range != null) {
                    val current = player.current
                    val isThis = current != null && current.surah == range.surah && current.ayah in range.from..range.to
                    val pausing = isThis && player.isPlaying
                    val first = VerseRef(range.surah, range.from)
                    PillButton(
                        text = if (pausing) "Pause" else "Listen",
                        icon = symbol(if (pausing) "pause.fill" else "play.fill"),
                        description = if (pausing) "Pause" else "Listen. Plays the recitation of ${first.id}",
                    ) {
                        if (isThis) player.togglePlayPause() else player.playRange(range)
                    }
                }
                SavePill(
                    saved = library.isSaved(dua),
                    reduce = reduce,
                    onSave = { haptics.soft() },
                ) { library.toggleSaved(dua) }
            }
        }
    }
}

@Composable
private fun isPlayingDua(dua: Dua): Boolean {
    val player = LocalGraph.current.player
    val range = dua.playableRange ?: return false
    val current = player.current ?: return false
    return current.surah == range.surah && current.ayah in range.from..range.to
}

private fun shareTextFor(dua: Dua): String =
    listOfNotNull(dua.arabic, dua.transliteration, dua.translation, dua.source).joinToString("\n\n")

/**
 * Share as text or as an image card, and copy each part on its own.
 * The share icon turns into a check for a moment after a copy.
 */
@Composable
private fun DuaShareMenu(dua: Dua, shareBody: String) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    var studioOpen by remember { mutableStateOf(false) }
    var copyToken by remember { mutableIntStateOf(0) }
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copyToken) {
        if (copyToken > 0) {
            copied = true
            delay(1600)
            copied = false
        }
    }
    fun copy(text: String) {
        open = false
        context.copyText(text)
        haptics.success()
        copyToken++
    }
    Box {
        AnimatedContent(
            targetState = copied,
            transitionSpec = { (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut()) },
            label = "shareIcon",
        ) { isCopied ->
            BarIcon(symbol(if (isCopied) "checkmark" else "square.and.arrow.up"), if (isCopied) "Copied" else "Share") { open = true }
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(Radius.control),
            containerColor = Palette.surface,
        ) {
            ShareMenuItem("Share as text", symbol("text.alignleft")) {
                open = false
                context.shareText(shareBody)
            }
            ShareMenuItem("Share as image", symbol("photo")) {
                open = false
                studioOpen = true
            }
            HorizontalDivider(color = Palette.border)
            ShareMenuItem("Copy Arabic", symbol("doc.on.doc")) { copy(dua.arabic) }
            ShareMenuItem("Copy translation", symbol("doc.on.doc")) { copy(dua.translation) }
            val transliteration = dua.transliteration
            if (transliteration != null) {
                ShareMenuItem("Copy transliteration", symbol("doc.on.doc")) { copy(transliteration) }
            }
        }
    }
    if (studioOpen) {
        com.quranforwomen.app.feature.quran.ShareStudioSheet(
            arabic = dua.arabic,
            translation = dua.translation,
            reference = dua.source,
            shareTitle = dua.title,
            onDismiss = { studioOpen = false },
        )
    }
}

@Composable
private fun ShareMenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, style = TextStyles.body, color = Palette.textPrimary) },
        onClick = onClick,
        leadingIcon = { Icon(icon, null, tint = Palette.textPrimary, modifier = Modifier.size(20.dp)) },
    )
}

/** A gilt line that fills right to left with the recitation of the current verse. */
@Composable
private fun RecitationLine(progress: Float) {
    val shown by animateFloatAsState(progress, tween(100, easing = LinearEasing), label = "recitation")
    androidx.compose.runtime.CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        BoxWithConstraints(
            Modifier.fillMaxWidth().height(2.dp).clip(CircleShape).background(BookInk.gild.copy(alpha = 0.2f))
        ) {
            Box(Modifier.width(maxWidth * shown).fillMaxHeight().clip(CircleShape).background(BookInk.gild))
        }
    }
}

/** A tinted pill with an icon and a label, like the iPhone's tinted buttons. The icon swaps with a small morph. */
@Composable
private fun PillButton(
    text: String,
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    iconScale: Float = 1f,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .heightIn(min = 50.dp)
            .clip(CircleShape)
            .background(Ink.wash)
            .pressable(scale = 0.97f, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = Space.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = { (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut()) },
            label = "pillIcon",
        ) {
            Icon(
                it, null, tint = BookInk.caption,
                modifier = Modifier.size(20.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale },
            )
        }
        Spacer(Modifier.width(Space.xs))
        AnimatedContent(
            targetState = text,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "pillText",
        ) { Text(it, style = TextStyles.headline, color = BookInk.caption) }
    }
}

/** Save and Saved. One small pop and a few petals when she saves. Nothing when she removes it. */
@Composable
private fun SavePill(saved: Boolean, reduce: Boolean, onSave: () -> Unit, toggle: () -> Unit) {
    var pops by remember { mutableIntStateOf(0) }
    val bounce = remember { Animatable(1f) }
    LaunchedEffect(pops) {
        if (pops > 0 && !reduce) {
            bounce.snapTo(0.7f)
            bounce.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
        }
    }
    Box(contentAlignment = Alignment.Center) {
        PillButton(
            text = if (saved) "Saved" else "Save",
            icon = symbol(if (saved) "bookmark.fill" else "bookmark"),
            description = if (saved) "Saved. Removes this dua from Saved" else "Save. Keeps this dua in Saved",
            iconScale = bounce.value,
        ) {
            if (!saved) {
                onSave()
                if (!reduce) pops++
            }
            toggle()
        }
        // In a book the petals are gilt and red silk.
        PetalBurst(trigger = pops, count = 4, color = BookInk.gild)
        PetalBurst(trigger = pops, count = 4, color = BookInk.ribbon)
    }
}
