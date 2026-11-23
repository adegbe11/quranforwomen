package com.quranforwomen.app.feature.duas

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.quranforwomen.app.ui.theme.Motion
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.Dua
import com.quranforwomen.app.nav.AppTab
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.AppScreen
import com.quranforwomen.app.ui.components.LocalUndo
import com.quranforwomen.app.ui.components.RollingNumber
import com.quranforwomen.app.ui.components.SavedEmpty
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.rememberHaptics
import com.quranforwomen.app.ui.components.scrollSettle
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles

/** Duas she saved. They deal in, and a removed dua can be brought back with Undo. */
@Composable
internal fun SavedDuasContent() {
    val graph = LocalGraph.current
    val content = graph.content
    val library = graph.library
    val router = LocalRouter.current
    val undo = LocalUndo.current
    val haptics = rememberHaptics()
    val reduce = LocalReduceMotion.current

    // Newest first.
    val duas = library.savedDuaIDs.mapNotNull { content.dua(it) }

    fun remove(dua: Dua) {
        library.toggleSaved(dua)
        haptics.soft()
        undo.show("Removed ${dua.title}") {
            if (!library.isSaved(dua)) library.toggleSaved(dua)
        }
    }

    AppScreen(title = "Saved Duas", spacing = Space.s) {
        if (duas.isEmpty()) {
            item(key = "empty") {
                SavedEmpty(
                    icon = symbol("heart"),
                    title = "No saved duas",
                    actionLabel = "Browse Duas",
                    onAction = { router.tab = AppTab.duas },
                )
            }
        } else {
            item(key = "count") {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = Space.l - Space.s).focusIn(50),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RollingNumber(
                        value = duas.size,
                        style = TextStyles.largeTitle.copy(fontSize = 34.sp, fontWeight = FontWeight.Light),
                        color = Palette.textPrimary,
                    )
                    Text(
                        if (duas.size == 1) "dua saved" else "duas saved",
                        style = TextStyles.subheadline,
                        color = Palette.textSecondary,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
            itemsIndexed(duas, key = { _, d -> d.id }) { index, dua ->
                SavedDuaItem(
                    dua = dua,
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = if (reduce) null else tween(250),
                            placementSpec = if (reduce) null else spring(dampingRatio = 0.85f, stiffness = 300f),
                            fadeOutSpec = if (reduce) null else tween(250),
                        )
                        .dealIn(minOf(index, 8), baseDelay = 120, step = 50)
                        .scrollSettle(),
                    onOpen = { router.push(Route.DuaDetail(dua.id)) },
                    onRemove = { remove(dua) },
                )
            }
        }
    }
}

/** A saved dua card. A long press offers Remove from Saved, like the iPhone's context menu. */
@Composable
private fun SavedDuaItem(dua: Dua, modifier: Modifier, onOpen: () -> Unit, onRemove: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()
    Box(
        modifier
            .semantics {
                customActions = listOf(CustomAccessibilityAction("Remove from Saved") { onRemove(); true })
            }
    ) {
        DuaCard(
            dua = dua,
            onClick = null,
            modifier = Modifier.pressWithLongPress(onClick = onOpen) {
                haptics.tap()
                menu = true
            },
        )
        DropdownMenu(
            expanded = menu,
            onDismissRequest = { menu = false },
            shape = RoundedCornerShape(14.dp),
            containerColor = Palette.surface,
        ) {
            DropdownMenuItem(
                text = { Text("Remove from Saved", style = TextStyles.body, color = Palette.blushStrong) },
                leadingIcon = { Icon(symbol("bookmark.slash"), null, tint = Palette.blushStrong) },
                onClick = {
                    menu = false
                    onRemove()
                },
            )
        }
    }
}

/** Tap opens, long press offers the menu. Dips softly while pressed, like the other cards. */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.pressWithLongPress(onClick: () -> Unit, onLongClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.985f else 1f, Motion.press(), label = "press")
    val a by animateFloatAsState(if (pressed) 0.88f else 1f, Motion.press(), label = "pressAlpha")
    this
        .graphicsLayer { scaleX = s; scaleY = s; alpha = a }
        .combinedClickable(
            interactionSource = source,
            indication = null,
            onLongClickLabel = "Remove from Saved",
            onLongClick = onLongClick,
            onClick = onClick,
        )
}
