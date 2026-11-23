package com.quranforwomen.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quranforwomen.app.nav.AppTab
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.ui.theme.BookInk
import com.quranforwomen.app.ui.theme.Ink
import com.quranforwomen.app.ui.theme.LocalInBook
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles

/** Space the tab bar and mini player take at the bottom. Scrolling content pads by this much. */
val LocalBottomInset = compositionLocalOf { 0.dp }

/** True for the first screen of a tab, which has no back button. */
val LocalIsRoot = compositionLocalOf { true }

/** The tab a screen lives in. */
val LocalTab = compositionLocalOf { AppTab.today }

val TopBarHeight = 52.dp

/**
 * A standard screen: a large title that scrolls away into a small centred title in the bar,
 * a back chevron on pushed screens, actions on the right, and a lazy column of content.
 * Pass book = true for reading screens set in PrayerKey's Bible colours.
 */
@Composable
fun AppScreen(
    title: String,
    modifier: Modifier = Modifier,
    largeTitle: Boolean = true,
    book: Boolean = false,
    listState: LazyListState = rememberLazyListState(),
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    horizontalPadding: Dp = Space.page,
    spacing: Dp = Space.xl,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val body: @Composable () -> Unit = {
        ScreenBody(title, largeTitle, listState, onBack, actions, horizontalPadding, spacing, header, content)
    }
    if (book) BookPage(modifier, body) else Box(modifier.fillMaxSize().background(Palette.background)) { body() }
}

@Composable
private fun ScreenBody(
    title: String,
    largeTitle: Boolean,
    listState: LazyListState,
    onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit,
    horizontalPadding: Dp,
    spacing: Dp,
    header: (@Composable ColumnScope.() -> Unit)?,
    content: LazyListScope.() -> Unit,
) {
    val router = LocalRouter.current
    val isRoot = LocalIsRoot.current
    val tab = LocalTab.current
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = LocalBottomInset.current + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Tapping the open tab again scrolls its first screen back to the top.
    LaunchedEffect(router.reselect) {
        if (isRoot && router.reselect.second == tab && router.reselect.first > 0) listState.animateScrollToItem(0)
    }

    val density = LocalDensity.current
    val collapse by remember(largeTitle) {
        derivedStateOf {
            if (!largeTitle) {
                if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 4) 1f else 0f
            } else if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / with(density) { 40.dp.toPx() }).coerceIn(0f, 1f)
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = horizontalPadding, end = horizontalPadding,
                top = top + TopBarHeight, bottom = bottom + Space.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            if (largeTitle || header != null) {
                item(key = "screen-title") {
                    Column {
                        if (largeTitle) {
                            Text(
                                title, style = TextStyles.largeTitle, color = Ink.text,
                                modifier = Modifier
                                    .graphicsLayer { alpha = 1f - collapse }
                                    .semantics { heading() },
                            )
                        }
                        header?.invoke(this)
                    }
                }
            }
            content()
        }
        TopBar(
            title = title,
            titleAlpha = if (largeTitle) ((collapse - 0.6f) / 0.4f).coerceIn(0f, 1f) else 1f,
            backgroundAlpha = collapse,
            onBack = if (isRoot && onBack == null) null else (onBack ?: { router.pop(); Unit }),
            actions = actions,
        )
    }
}

/** The bar at the top of a screen. Clear over the page until content scrolls under it. */
@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    titleAlpha: Float = 1f,
    backgroundAlpha: Float = 1f,
    onBack: (() -> Unit)? = null,
    leading: (@Composable RowScope.() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val inBook = LocalInBook.current
    val bg = if (inBook) BookInk.page else Palette.background
    val bgAlpha by animateFloatAsState(backgroundAlpha, label = "barBg")
    Box(
        modifier
            .fillMaxWidth()
            .graphicsLayer { }
            .background(bg.copy(alpha = 0.96f * bgAlpha))
            .padding(top = top)
            .height(TopBarHeight),
    ) {
        if (bgAlpha > 0.01f) {
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(0.5.dp)
                    .background(Ink.border.copy(alpha = bgAlpha))
            )
        }
        Row(Modifier.align(Alignment.CenterStart).padding(start = Space.xs), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) BackButton(onBack)
            leading?.invoke(this)
        }
        Text(
            title,
            style = TextStyles.headline,
            color = Ink.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 220.dp)
                .graphicsLayer { alpha = titleAlpha },
        )
        Row(
            Modifier.align(Alignment.CenterEnd).padding(end = Space.xs),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

@Composable
fun BackButton(onBack: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .pressable(scale = 0.9f, onClick = onBack)
            .semantics { contentDescription = "Back" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, null, tint = Ink.accent, modifier = Modifier.size(20.dp).padding(start = 4.dp))
    }
}

/** A round icon action for the top bar. */
@Composable
fun BarIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .pressable(scale = 0.9f, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Ink.accent, modifier = Modifier.size(22.dp))
    }
}

/**
 * A bottom sheet in the app's colours, like an iOS sheet: rounded top, a grabber, the page tone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    book: Boolean = false,
    skipPartiallyExpanded: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded),
    content: @Composable ColumnScope.() -> Unit,
) {
    val container = if (book) BookInk.page else Palette.background
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = container,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier.padding(top = 8.dp, bottom = 4.dp).size(width = 36.dp, height = 5.dp)
                    .clip(CircleShape).background(Ink.secondary.copy(alpha = 0.3f))
            )
        },
        modifier = modifier,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalInBook provides book) {
            // A tall sheet stops below the status bar instead of sliding under the clock.
            val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val maxHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp - top - 12.dp
            Column(Modifier.fillMaxWidth().heightIn(max = maxHeight).padding(bottom = Space.xl), content = content)
        }
    }
}

/** A soft fade at the bottom of a screen behind floating controls. */
@Composable
fun BottomFade(modifier: Modifier = Modifier, height: Dp = 120.dp) {
    val bg = if (LocalInBook.current) BookInk.page else Palette.background
    Spacer(
        modifier.fillMaxWidth().height(height)
            .background(Brush.verticalGradient(listOf(Color.Transparent, bg.copy(alpha = 0.92f), bg)))
    )
}
