package com.quranforwomen.app.feature.duas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quranforwomen.app.data.Dua
import com.quranforwomen.app.ui.components.CardSurface
import com.quranforwomen.app.ui.components.dealIn
import com.quranforwomen.app.ui.theme.Ink
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles

/**
 * The duas she opened last, newest first, as a row of small leaves she can scroll through.
 * Each leaf is dealt in and opens the dua.
 */
@Composable
internal fun RecentDuasRow(duas: List<Dua>, modifier: Modifier = Modifier, onOpen: (Dua) -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Text(
            "Recent",
            style = TextStyles.title3,
            color = Ink.text,
            modifier = Modifier.padding(bottom = Space.xxs).semantics { heading() },
        )
        LazyRow(
            // The row runs to the screen edges, while its first leaf lines up with the page.
            modifier = Modifier.bleed(Space.page.value),
            contentPadding = PaddingValues(horizontal = Space.page, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            itemsIndexed(duas, key = { _, d -> d.id }) { index, dua ->
                RecentDuaLeaf(
                    dua = dua,
                    modifier = Modifier.dealIn(minOf(index, 6), baseDelay = 150, step = 50),
                    onClick = { onOpen(dua) },
                )
            }
        }
    }
}

@Composable
private fun RecentDuaLeaf(dua: Dua, modifier: Modifier = Modifier, onClick: () -> Unit) {
    CardSurface(
        modifier = modifier.size(width = 150.dp, height = 92.dp).semantics(mergeDescendants = true) { },
        padding = Space.s,
        onClick = onClick,
    ) {
        Text(
            dua.title,
            style = TextStyles.subheadline.copy(fontWeight = FontWeight.SemiBold),
            color = Ink.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.weight(1f))
        Text(
            dua.source,
            style = TextStyles.caption,
            color = Ink.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Widens a child past the page padding on both sides, so a horizontal row can scroll edge to edge. */
private fun Modifier.bleed(paddingDp: Float): Modifier = layout { measurable, constraints ->
    if (!constraints.hasBoundedWidth) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
    val extra = (paddingDp * density).toInt() * 2
    val wide = constraints.maxWidth + extra
    val placeable = measurable.measure(constraints.copy(minWidth = wide, maxWidth = wide))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}
