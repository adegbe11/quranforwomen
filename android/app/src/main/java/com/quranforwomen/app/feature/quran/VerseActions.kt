package com.quranforwomen.app.feature.quran

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.R
import com.quranforwomen.app.data.HighlightColor
import com.quranforwomen.app.data.RepeatMode
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.data.arabicIndic
import com.quranforwomen.app.nav.JournalStart
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.AppSheet
import com.quranforwomen.app.ui.components.AppTextField
import com.quranforwomen.app.ui.components.ArabicText
import com.quranforwomen.app.ui.components.CardSurface
import com.quranforwomen.app.ui.components.PetalBurst
import com.quranforwomen.app.ui.components.RollingNumber
import com.quranforwomen.app.ui.components.TextAction
import com.quranforwomen.app.ui.components.TintedButton
import com.quranforwomen.app.ui.components.copyText
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.inkReveal
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.scrollSettle
import com.quranforwomen.app.ui.components.shareFile
import com.quranforwomen.app.ui.components.shareText
import com.quranforwomen.app.ui.components.sharedCacheFile
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.LocalDark
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Motion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Radius
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Soft fills that keep text readable in both appearances. */
internal val HighlightColor.quranFill: Color
    @Composable @ReadOnlyComposable get() {
        val dark = LocalDark.current
        return when (this) {
            HighlightColor.emerald -> if (dark) Color(0xFF1C3329) else Color(0xFFDCEDE5)
            HighlightColor.blush -> if (dark) Color(0xFF3D2830) else Color(0xFFF7E1E8)
            HighlightColor.gold -> if (dark) Color(0xFF3A3220) else Color(0xFFF6ECCF)
            HighlightColor.sky -> if (dark) Color(0xFF1E2B38) else Color(0xFFDFEAF4)
        }
    }

internal val HighlightColor.quranSwatch: Color
    get() = when (this) {
        HighlightColor.emerald -> Color(0xFF3E9C7A)
        HighlightColor.blush -> Color(0xFFD98AA2)
        HighlightColor.gold -> Color(0xFFD3A94A)
        HighlightColor.sky -> Color(0xFF6E9CC6)
    }

/**
 * The small set of actions that appears when a verse is tapped.
 * Motion: the verse card comes into focus with its Arabic inked in, and tints live as
 * she picks a highlight. The action tiles are dealt in one by one, Save sends up a few
 * petals, and Share turns into a check once the verse is copied.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuranVerseActionsSheet(ref: VerseRef, onDismiss: () -> Unit) {
    val graph = LocalGraph.current
    val quran = graph.quran
    val library = graph.library
    val player = graph.player
    val translations = graph.translations
    val router = LocalRouter.current
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val reduce = LocalReduceMotion.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    var copied by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf(false) }
    var repeatOpen by remember { mutableStateOf(false) }
    var rangeOpen by remember { mutableStateOf(false) }
    var shareOpen by remember { mutableStateOf(false) }
    var burst by remember { mutableIntStateOf(0) }
    var tafsirOpen by remember { mutableStateOf(false) }
    var studioOpen by remember { mutableStateOf(false) }

    val close: (after: () -> Unit) -> Unit = { after ->
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            after()
        }
    }

    val surah = quran.surah(ref.surah)
    val translation = translations.text(ref)
    val shareBody = buildList {
        add("${quran.text(ref)} ﴿${ref.ayah.arabicIndic}﴾")
        translation?.let { add(it) }
        add("Quran ${ref.surah}:${ref.ayah} · ${surah.name}")
    }.joinToString("\n\n")

    val highlight = library.highlight(ref)
    val note = library.note(ref)
    val saved = library.isBookmarked(ref)
    val cardFill by animateColorAsState(
        highlight?.quranFill ?: Palette.surface,
        if (reduce) snap() else Motion.gentle(),
        label = "verseCard",
    )

    AppSheet(onDismiss = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.page)
                .padding(top = Space.xl, bottom = Space.l),
            verticalArrangement = Arrangement.spacedBy(Space.l),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .focusIn()
                    .scrollSettle()
                    .clip(RoundedCornerShape(Radius.tile))
                    .background(cardFill)
                    .padding(Space.m),
                verticalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                Column(Modifier.semantics(mergeDescendants = true) { }, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${surah.name} · Verse ${ref.ayah}", style = TextStyles.headline, color = Palette.textPrimary)
                    Text(
                        "${surah.meaning} · Juz ${quran.juz(ref)} · Page ${quran.page(ref)}",
                        style = TextStyles.subheadline,
                        color = Palette.textSecondary,
                    )
                }
                ArabicText(quran.text(ref), size = 22f, maxLines = 2, color = Palette.textPrimary, modifier = Modifier.inkReveal(200, 900))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalAlignment = Alignment.Top) {
                ActionTile(
                    "Play", symbol("play.fill"),
                    modifier = Modifier.weight(1f).dealIn(0, baseDelay = 80, step = 50),
                ) {
                    player.updateRepeatMode(RepeatMode.Off)
                    player.play(ref)
                    close {}
                }

                Box(Modifier.weight(1f).dealIn(1, baseDelay = 80, step = 50)) {
                    ActionTile("Repeat", symbol("repeat")) { repeatOpen = true }
                    SheetMenu(repeatOpen, { repeatOpen = false }) {
                        MenuHeader("Repeat this verse")
                        listOf(
                            "3 times" to RepeatMode.Times(3),
                            "5 times" to RepeatMode.Times(5),
                            "10 times" to RepeatMode.Times(10),
                            "Until I stop" to RepeatMode.Forever,
                        ).forEach { (label, mode) ->
                            MenuItem(label) {
                                repeatOpen = false
                                player.repeatVerse(ref, mode)
                                close {}
                            }
                        }
                        MenuHeader("Repeat a range")
                        MenuItem("Choose verses", symbol("repeat")) {
                            repeatOpen = false
                            rangeOpen = true
                        }
                    }
                }

                Box(Modifier.weight(1f).dealIn(2, baseDelay = 80, step = 50), contentAlignment = Alignment.Center) {
                    ActionTile(
                        if (saved) "Saved" else "Save",
                        symbol(if (saved) "bookmark.fill" else "bookmark"),
                        isActive = saved,
                        description = if (saved) "Saved. Removes this verse from Saved" else "Save. Keeps this verse in Saved",
                    ) {
                        haptics.tick()
                        if (!saved) burst++
                        library.toggleBookmark(ref)
                    }
                    PetalBurst(trigger = burst)
                }

                ActionTile(
                    if (note == null) "Note" else "Edit note",
                    symbol(if (note == null) "square.and.pencil" else "note.text"),
                    isActive = note != null,
                    modifier = Modifier.weight(1f).dealIn(3, baseDelay = 80, step = 50),
                ) { editingNote = true }

                Box(Modifier.weight(1f).dealIn(4, baseDelay = 80, step = 50)) {
                    ActionTile(
                        if (copied) "Copied" else "Share",
                        symbol(if (copied) "checkmark" else "square.and.arrow.up"),
                        isActive = copied,
                    ) { shareOpen = true }
                    SheetMenu(shareOpen, { shareOpen = false }) {
                        MenuItem("Share as text", symbol("text.alignleft")) {
                            shareOpen = false
                            context.shareText(shareBody)
                        }
                        MenuItem("Share as image", symbol("photo")) {
                            shareOpen = false
                            studioOpen = true
                        }
                        MenuItem("Copy", symbol("doc.on.doc")) {
                            shareOpen = false
                            context.copyText(shareBody)
                            if (!copied) haptics.success()
                            copied = true
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs), modifier = Modifier.dealIn(5, baseDelay = 80, step = 50)) {
                TintedButton(
                    "Read tafsir",
                    onClick = { tafsirOpen = true },
                    icon = symbol("book.pages"),
                    modifier = Modifier.weight(1f),
                )
                val learned = graph.hifz.isLearned(ref)
                TintedButton(
                    if (learned) "Learned" else "Mark learned",
                    onClick = { haptics.tick(); graph.hifz.toggleLearned(ref) },
                    icon = symbol(if (learned) "checkmark.circle.fill" else "checkmark"),
                    modifier = Modifier.weight(1f),
                )
            }

            TintedButton(
                "Reflect in my journal",
                onClick = { close { router.push(Route.JournalEditor(JournalStart.Verse(ref))) } },
                icon = symbol("square.and.pencil"),
                modifier = Modifier.dealIn(5, baseDelay = 80, step = 50),
            )

            HighlightPicker(
                selection = highlight,
                modifier = Modifier.dealIn(6, baseDelay = 80, step = 50),
            ) { color ->
                haptics.tick()
                library.setHighlight(color, ref)
            }

            AnimatedVisibility(
                visible = note != null,
                enter = if (reduce) fadeIn() else fadeIn() + scaleIn(initialScale = 0.96f) + expandVertically(),
                exit = if (reduce) fadeOut() else fadeOut() + scaleOut(targetScale = 0.96f) + shrinkVertically(),
            ) {
                val shownNote = remember { arrayOfNulls<String>(1) }
                note?.let { shownNote[0] = it.text }
                CardSurface(
                    modifier = Modifier.scrollSettle(),
                    padding = Space.m,
                    fill = Palette.surface,
                    onClick = { editingNote = true },
                ) {
                    Text("Your note", style = TextStyles.footnote.copy(fontWeight = FontWeight.SemiBold), color = Palette.textSecondary)
                    Spacer(Modifier.height(Space.xxs))
                    Text(shownNote[0] ?: "", style = TextStyles.body.copy(fontFamily = AppFont.serif), color = Palette.textPrimary)
                }
            }
        }
    }

    if (editingNote) VerseNoteEditor(ref) { editingNote = false }
    if (tafsirOpen) TafsirSheet(ref) { tafsirOpen = false }
    if (studioOpen) {
        ShareStudioSheet(
            arabic = "${quran.text(ref)} ﴿${ref.ayah.arabicIndic}﴾",
            translation = translation,
            reference = "${surah.name} · ${ref.surah}:${ref.ayah}",
            shareTitle = quran.displayName(ref),
            onDismiss = { studioOpen = false },
        )
    }
    if (rangeOpen) {
        com.quranforwomen.app.feature.audio.RangeRepeatSheet(
            ref.surah, ref.ayah,
            onDismiss = { rangeOpen = false },
            onStarted = { close {} },
        )
    }
}

@Composable
private fun SheetMenu(expanded: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = Palette.surface,
        shape = RoundedCornerShape(Radius.control),
    ) { content() }
}

@Composable
private fun MenuHeader(text: String) {
    Text(
        text,
        style = TextStyles.footnote,
        color = Palette.textSecondary,
        modifier = Modifier.padding(horizontal = Space.m, vertical = Space.xs).semantics { heading() },
    )
}

@Composable
private fun MenuItem(text: String, icon: ImageVector? = null, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text, style = TextStyles.body, color = Palette.textPrimary) },
        onClick = onClick,
        leadingIcon = icon?.let { { Icon(it, null, tint = Palette.textPrimary, modifier = Modifier.size(20.dp)) } },
    )
}

/**
 * A tappable action tile. Its symbol morphs and gives one small bounce when the tile
 * turns on or off, and its fill blushes softly.
 */
@Composable
private fun ActionTile(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    description: String? = null,
    onClick: () -> Unit,
) {
    val reduce = LocalReduceMotion.current
    val fill by animateColorAsState(if (isActive) Palette.blush else Palette.surface, if (reduce) snap() else Motion.gentle(), label = "tileFill")
    val bounce = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(isActive) {
        if (first) { first = false; return@LaunchedEffect }
        if (!reduce) {
            bounce.snapTo(0.75f)
            bounce.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
        }
    }
    val shape = RoundedCornerShape(Radius.control)
    Column(
        modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .pressable(onClick = onClick)
            .clip(shape)
            .background(fill)
            .border(1.dp, Palette.border, shape)
            .padding(horizontal = 4.dp, vertical = Space.s)
            .then(if (description != null) Modifier.semantics(mergeDescendants = true) { contentDescription = description } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.xs, Alignment.CenterVertically),
    ) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = {
                if (reduce) fadeIn() togetherWith fadeOut()
                else (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut())
            },
            label = "tileIcon",
            modifier = Modifier.height(24.dp).graphicsLayer { scaleX = bounce.value; scaleY = bounce.value },
        ) { Icon(it, null, tint = Palette.accent, modifier = Modifier.size(22.dp)) }
        Text(
            title,
            style = TextStyles.caption.copy(fontWeight = FontWeight.Medium),
            color = Palette.accent,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            softWrap = false,
        )
    }
}

/** Four soft colours and a clear option. The selection ring glides from one swatch to the next. */
@Composable
private fun HighlightPicker(selection: HighlightColor?, modifier: Modifier = Modifier, onSelect: (HighlightColor?) -> Unit) {
    val reduce = LocalReduceMotion.current
    val colors = HighlightColor.entries
    val cell = 44.dp
    val gap = Space.s
    val index = selection?.let { colors.indexOf(it) }
    var lastIndex by remember { mutableIntStateOf(index ?: 0) }
    if (index != null) lastIndex = index
    val ringX by animateDpAsState((cell + gap) * lastIndex, if (reduce) snap() else Motion.gentle(), label = "ringX")
    val ringAlpha by animateFloatAsState(if (index != null) 1f else 0f, if (reduce) snap() else Motion.gentle(), label = "ringAlpha")
    val ringColor = Palette.textPrimary

    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Highlight", style = TextStyles.subheadline.copy(fontWeight = FontWeight.Medium), color = Palette.textSecondary)
        Spacer(Modifier.weight(1f))
        Box {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                colors.forEach { color ->
                    val isSel = selection == color
                    val swell by animateFloatAsState(if (isSel) 1.08f else 1f, if (reduce) snap() else Motion.gentle(), label = "swell")
                    Box(
                        Modifier
                            .size(cell)
                            .pressable(scale = 0.9f) { onSelect(if (isSel) null else color) }
                            .semantics {
                                contentDescription = "${color.title} highlight"
                                selected = isSel
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .graphicsLayer { scaleX = swell; scaleY = swell }
                                .clip(CircleShape)
                                .background(color.quranSwatch)
                        )
                    }
                }
            }
            Box(
                Modifier
                    .offset(x = ringX + 3.dp, y = 3.dp)
                    .size(38.dp)
                    .graphicsLayer { alpha = ringAlpha }
                    .border(2.dp, ringColor, CircleShape)
            )
        }
    }
}

/**
 * Writing a note on a verse. The Arabic inks in, the writing box comes into focus and its
 * border warms to emerald while she types, and the character count rolls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerseNoteEditor(ref: VerseRef, onDismiss: () -> Unit) {
    val graph = LocalGraph.current
    val reduce = LocalReduceMotion.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf(graph.library.note(ref)?.text ?: "") }
    var focused by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val close: () -> Unit = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } }

    val borderColor by animateColorAsState(
        if (focused) Palette.accent.copy(alpha = 0.55f) else Palette.border,
        if (reduce) snap() else Motion.gentle(), label = "noteBorder",
    )
    val borderWidth by animateDpAsState(if (focused) 1.5.dp else 1.dp, if (reduce) snap() else Motion.gentle(), label = "noteBorderWidth")

    LaunchedEffect(Unit) {
        delay(250)
        runCatching { focus.requestFocus() }
    }

    AppSheet(onDismiss = onDismiss, sheetState = sheetState) {
        Box(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = Space.xs)) {
            TextAction("Cancel", onClick = close, modifier = Modifier.align(Alignment.CenterStart))
            Text(
                "Note on ${ref.id}", style = TextStyles.headline, color = Palette.textPrimary,
                modifier = Modifier.align(Alignment.Center).semantics { heading() },
            )
            TextAction(
                "Save",
                bold = true,
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = {
                    graph.library.setNote(text, ref)
                    close()
                },
            )
        }
        Column(
            Modifier.padding(Space.page).imePadding(),
            verticalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            ArabicText(graph.quran.text(ref), size = 22f, maxLines = 3, color = Palette.textPrimary, modifier = Modifier.inkReveal(100, 900))
            Box(
                Modifier
                    .focusIn(120)
                    .clip(RoundedCornerShape(Radius.control))
                    .border(borderWidth, borderColor, RoundedCornerShape(Radius.control))
            ) {
                AppTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = "",
                    minLines = 6,
                    style = TextStyles.body.copy(fontFamily = AppFont.serif),
                    modifier = Modifier
                        .focusRequester(focus)
                        .onFocusChanged { focused = it.isFocused },
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                RollingNumber(text.length, TextStyles.caption, Palette.textSecondary)
                Text(if (text.length == 1) " character" else " characters", style = TextStyles.caption, color = Palette.textSecondary)
            }
        }
    }
}

/** A verse or dua as a shareable image, in the Emerald style. */
internal fun renderVerseShareImage(context: Context, arabic: String, translation: String?, reference: String): File =
    saveShareCard(context, renderShareCard(context, arabic, translation, reference, ShareStyle.Emerald))
