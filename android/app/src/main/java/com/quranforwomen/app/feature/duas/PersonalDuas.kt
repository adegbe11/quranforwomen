package com.quranforwomen.app.feature.duas

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.PersonalDua
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.AppSheet
import com.quranforwomen.app.ui.components.AppTextField
import com.quranforwomen.app.ui.components.BarIcon
import com.quranforwomen.app.ui.components.LocalUndo
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.SavedEmpty
import com.quranforwomen.app.ui.components.TextAction
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.rowPress
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.BookInk
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Radius
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay

/**
 * Personal prayers in her own words. Clearly marked as personal, never as Quran or Hadith.
 * Her duas are dealt in. A new one slides in at the top with a soft glow, an edited one
 * glows gently, and a deleted one folds away with a short Undo.
 */
@Composable
internal fun PersonalDuasContent() {
    val library = LocalGraph.current.library
    val undo = LocalUndo.current
    val haptics = rememberHaptics()
    val reduce = LocalReduceMotion.current

    var isAdding by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    /** The dua that should glow for a moment, after she adds, edits or restores it. */
    var glowing by remember { mutableStateOf<String?>(null) }
    var glowToken by remember { mutableIntStateOf(0) }
    var addedToken by remember { mutableIntStateOf(0) }

    fun glow(id: String) {
        glowing = id
        glowToken++
    }

    LaunchedEffect(glowToken) {
        if (glowToken > 0) {
            delay(1600)
            glowing = null
        }
    }

    val plusBounce = remember { Animatable(1f) }
    LaunchedEffect(addedToken) {
        if (addedToken > 0 && !reduce) {
            plusBounce.snapTo(0.7f)
            plusBounce.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
        }
    }

    fun delete(dua: PersonalDua) {
        val index = library.personalDuas.indexOfFirst { it.id == dua.id }
        if (index < 0) return
        library.removePersonalDua(dua)
        haptics.soft()
        undo.show("Dua deleted") {
            library.restorePersonalDua(dua, index)
            glow(dua.id)
        }
    }

    val duas = library.personalDuas

    AppScreen(
        title = "Your Own Words",
        largeTitle = false,
        book = true,
        spacing = 0.dp,
        actions = {
            Box(Modifier.graphicsLayer { scaleX = plusBounce.value; scaleY = plusBounce.value }) {
                BarIcon(symbol("plus"), "Write a Dua") { isAdding = true }
            }
        },
    ) {
        if (duas.isEmpty()) {
            item(key = "empty") {
                SavedEmpty(
                    icon = symbol("pencil.and.scribble"),
                    title = "No personal duas yet",
                    actionLabel = "Write a Dua",
                    onAction = { isAdding = true },
                )
            }
        } else {
            item(key = "top") { Spacer(Modifier.padding(top = Space.s)) }
            itemsIndexed(duas, key = { _, d -> d.id }) { index, dua ->
                PersonalDuaRow(
                    dua = dua,
                    first = index == 0,
                    last = index == duas.lastIndex,
                    glowing = glowing == dua.id,
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = if (reduce) null else tween(300),
                            placementSpec = if (reduce) null else spring(dampingRatio = 0.85f, stiffness = 300f),
                            fadeOutSpec = if (reduce) null else tween(250),
                        )
                        .dealIn(minOf(index, 8), baseDelay = 150, step = 50),
                    onOpen = { editingId = dua.id },
                    onDelete = { delete(dua) },
                )
            }
        }
    }

    if (isAdding) {
        PersonalDuaEditor(initialText = "", onDismiss = { isAdding = false }) { text ->
            val before = library.personalDuas.firstOrNull()?.id
            library.addPersonalDua(text)
            // Only celebrate when a dua was really added (empty text is ignored by the store).
            val added = library.personalDuas.firstOrNull()?.id
            if (added != null && added != before) {
                addedToken++
                haptics.success()
                glow(added)
            }
        }
    }

    val editing = editingId?.let { id -> library.personalDuas.firstOrNull { it.id == id } }
    if (editing != null) {
        PersonalDuaEditor(initialText = editing.text, onDismiss = { editingId = null }) { text ->
            library.updatePersonalDua(editing, text)
            if (library.personalDuas.any { it.id == editing.id }) glow(editing.id)
        }
    }
}

@Composable
private fun PersonalDuaRow(
    dua: PersonalDua,
    first: Boolean,
    last: Boolean,
    glowing: Boolean,
    modifier: Modifier,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val radius = Radius.tile
    val shape = RoundedCornerShape(
        topStart = if (first) radius else 0.dp, topEnd = if (first) radius else 0.dp,
        bottomStart = if (last) radius else 0.dp, bottomEnd = if (last) radius else 0.dp,
    )
    // The row warms to a soft blush when the dua is new or just edited, then fades back.
    val glow by animateFloatAsState(
        if (glowing) 1f else 0f,
        if (glowing) tween(350, delayMillis = 300, easing = LinearOutSlowInEasing) else tween(1000, easing = FastOutSlowInEasing),
        label = "glow",
    )
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        },
    )
    val date = remember(dua.created) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .format(Instant.ofEpochMilli(dua.created).atZone(ZoneId.systemDefault()).toLocalDate())
    }
    val gild = BookInk.gild
    Column(modifier.fillMaxWidth()) {
        SwipeToDismissBox(
            state = state,
            enableDismissFromStartToEnd = false,
            modifier = Modifier.clip(shape),
            backgroundContent = {
                Box(
                    Modifier.fillMaxSize().background(Color(0xFFC62828)).padding(horizontal = Space.l),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(symbol("trash"), null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(BookInk.pageLit)
                    .background(gild.copy(alpha = 0.28f * glow))
                    .heightIn(min = 48.dp)
                    .rowPress(onOpen)
                    .semantics(mergeDescendants = true) {
                        onClick(label = "Edit") { onOpen(); true }
                        customActions = listOf(CustomAccessibilityAction("Delete") { onDelete(); true })
                    }
                    .padding(horizontal = Space.m, vertical = Space.s),
                verticalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                Text(
                    dua.text,
                    style = TextStyles.body.copy(fontFamily = AppFont.serif),
                    color = BookInk.ink,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(date, style = TextStyles.caption, color = BookInk.caption)
            }
        }
        if (!last) Box(Modifier.background(BookInk.pageLit)) { RowDivider(inset = Space.m) }
    }
}

/** Writing or editing a personal dua. The words come softly into focus as the sheet opens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalDuaEditor(initialText: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initialText) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { focus.requestFocus() }
    }
    val canSave = !(text.isBlank() && initialText.isEmpty())
    AppSheet(onDismiss = onDismiss, book = true) {
        Box(Modifier.fillMaxWidth().padding(horizontal = Space.xs)) {
            TextAction("Cancel", onDismiss, modifier = Modifier.align(Alignment.CenterStart))
            Text(
                if (initialText.isEmpty()) "New Dua" else "Edit Dua",
                style = TextStyles.headline,
                color = BookInk.ink,
                modifier = Modifier.align(Alignment.Center),
            )
            TextAction(
                "Save",
                {
                    onSave(text)
                    onDismiss()
                },
                enabled = canSave,
                bold = true,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
        Row(Modifier.fillMaxWidth().padding(Space.m).focusIn(100)) {
            AppTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = "",
                minLines = 10,
                filled = false,
                style = TextStyles.body.copy(fontFamily = AppFont.serif),
                modifier = Modifier.focusRequester(focus),
            )
        }
    }
}
