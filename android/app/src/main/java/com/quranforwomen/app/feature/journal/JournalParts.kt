package com.quranforwomen.app.feature.journal

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.AppJson
import com.quranforwomen.app.data.JournalMood
import com.quranforwomen.app.data.TranslationStore
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.ui.components.TintedButton
import com.quranforwomen.app.ui.components.TranslationText
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.BookInk
import com.quranforwomen.app.ui.theme.Ink
import com.quranforwomen.app.ui.theme.LocalDark
import com.quranforwomen.app.ui.theme.LocalReduceMotion
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer

// Preferences kept by the journal only. Same keys as the iPhone app.

internal object JournalPrefKey {
    const val section = "journal.section"
    const val hidePreviews = "journal.hidePreviews"
}

// Icons the shared symbol map does not carry.

internal fun journalIcon(name: String): ImageVector = when (name) {
    "star" -> Icons.Outlined.StarOutline
    "lock.open" -> Icons.Outlined.LockOpen
    "hands.and.sparkles.fill" -> Icons.Filled.VolunteerActivism
    "ellipsis.circle.more" -> Icons.Outlined.Pending
    "ellipsis.circle.more.fill" -> Icons.Filled.Pending
    "chevron.left" -> Icons.AutoMirrored.Rounded.KeyboardArrowLeft
    "chevron.right" -> Icons.AutoMirrored.Rounded.KeyboardArrowRight
    "checkmark.square" -> Icons.Outlined.CheckBox
    "square" -> Icons.Outlined.CheckBoxOutlineBlank
    "pin" -> Icons.Outlined.PushPin
    else -> symbol(name)
}

internal val JournalMood.symbolName: String
    get() = when (this) {
        JournalMood.grateful -> "heart"
        JournalMood.peaceful -> "leaf"
        JournalMood.hopeful -> "sunrise"
        JournalMood.makingDua -> "hands.and.sparkles"
        JournalMood.heavy -> "cloud.rain"
        JournalMood.anxious -> "wind"
    }

/** The warm golds used for answered duas and favorites. */
internal object JournalGold {
    val mark = Color(0xFFD3A94A)
    val light = Color(0xFFF2C66D)
    val text = Color(0xFF9A7420)
    val wash = Color(0xFFF6ECCF)
    val number = Color(0xFFB8902F)
    val card: Color @Composable get() = if (LocalDark.current) Color(0xFF2A2519) else Color(0xFFFBF5E6)
}

/** Keeps a serializable value across process death, for drafts in progress. */
internal fun <T> journalJsonSaver(serializer: KSerializer<T>): Saver<MutableState<T>, String> = Saver(
    save = { AppJson.encodeToString(serializer, it.value) },
    restore = { mutableStateOf(AppJson.decodeFromString(serializer, it)) },
)

// The lock

private fun Context.findFragmentActivity(): FragmentActivity? {
    var c: Context? = this
    while (c != null) {
        if (c is FragmentActivity) return c
        c = (c as? ContextWrapper)?.baseContext
    }
    return null
}

/** When the last prompt was asked for. Only guards against a double tap; it can never get stuck
 *  the way a "showing" flag can when Android dismisses the prompt without telling the app. */
private var journalPromptAskedAt = 0L

/**
 * Fingerprint, face or screen lock. Calls back on the main thread.
 * With no screen lock set up on the phone there is nothing to check, so it opens, as on iPhone.
 */
internal fun journalAuthenticate(context: Context, title: String, onResult: (Boolean) -> Unit) {
    val activity = context.findFragmentActivity() ?: return onResult(false)
    val now = android.os.SystemClock.elapsedRealtime()
    if (now - journalPromptAskedAt < 800) return
    journalPromptAskedAt = now
    val authenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    if (BiometricManager.from(activity).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
        onResult(true)
        return
    }
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult(true)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onResult(false)
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setAllowedAuthenticators(authenticators)
        .build()
    runCatching { prompt.authenticate(info) }.onFailure { onResult(false) }
}

/**
 * Covers an open journal page or note while the journal is locked, without closing it,
 * so nothing she was writing is lost and nothing private shows.
 */
@Composable
internal fun JournalPrivacyCover(
    modifier: Modifier = Modifier,
    fill: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val journal = LocalGraph.current.journal
    val context = LocalContext.current
    Box(if (fill) modifier.fillMaxSize() else modifier) {
        content()
        AnimatedVisibility(
            visible = !journal.isAccessible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.matchParentSize(),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(BookInk.page)
                    .clickable(remember { MutableInteractionSource() }, indication = null) {},
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Space.m, Alignment.CenterVertically),
            ) {
                Icon(symbol("lock"), null, tint = BookInk.rule, modifier = Modifier.size(34.dp))
                Text(
                    "Your journal is private",
                    style = TextStyles.title3.copy(fontFamily = AppFont.serif),
                    color = Ink.text,
                )
                TintedButton("Unlock", onClick = {
                    journalAuthenticate(context, "Unlock your journal") { ok ->
                        if (ok) journal.isUnlocked = true
                    }
                }, fullWidth = false)
            }
        }
    }
}

// Reading

/** Renders the light markdown used by templates: headings, bullets, checklists. */
@Composable
internal fun JournalMarkdownBody(text: String, modifier: Modifier = Modifier) {
    val body = TextStyles.body.copy(fontFamily = AppFont.serif)
    SelectionContainer(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            text.split("\n").forEach { raw ->
                val line = raw.trim()
                when {
                    line.startsWith("## ") -> Text(
                        line.drop(3).uppercase(),
                        style = TextStyles.caption.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp),
                        color = BookInk.rule,
                        modifier = Modifier.padding(top = Space.xs),
                    )
                    line.startsWith("- [x] ") || line.startsWith("- [ ] ") -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            journalIcon(if (line.startsWith("- [x]")) "checkmark.square" else "square"), null,
                            tint = BookInk.ink, modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(Space.xs))
                        Text(line.drop(6), style = body, color = BookInk.ink)
                    }
                    line.startsWith("- ") -> Row {
                        Text("•", style = body, color = BookInk.rule)
                        Spacer(Modifier.width(Space.xs))
                        Text(line.drop(2), style = body, color = BookInk.ink)
                    }
                    line.isNotEmpty() -> Text(line, style = body.copy(lineHeight = 27.sp), color = BookInk.ink)
                }
            }
        }
    }
}

/** Translation for a verse, or a calm note while it downloads. */
@Composable
internal fun JournalTranslationBody(ref: VerseRef, size: Float = 17f) {
    val translations = LocalGraph.current.translations
    val scope = rememberCoroutineScope()
    @Suppress("UNUSED_VARIABLE") val version = translations.loadedVersion
    val text = translations.text(ref)
    if (text != null) {
        TranslationText(text, size = size, rightToLeft = translations.selected.isRightToLeft)
        return
    }
    when (translations.selectedState) {
        TranslationStore.State.Downloading -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            CircularProgressIndicator(Modifier.size(14.dp), color = Ink.secondary, strokeWidth = 2.dp)
            Text("Preparing the translation", style = TextStyles.footnote, color = Ink.secondary)
        }
        TranslationStore.State.Failed, TranslationStore.State.NotDownloaded -> Row(
            Modifier.pressable { scope.launch { translations.prepareSelected() } }.padding(vertical = Space.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(symbol("arrow.down.circle"), null, tint = Ink.accent, modifier = Modifier.size(16.dp))
            Text("Download translation", style = TextStyles.footnote.copy(fontWeight = FontWeight.Medium), color = Ink.accent)
        }
        TranslationStore.State.Ready -> Unit
    }
}

/** "No results" for a search, like iOS. */
@Composable
internal fun JournalSearchEmpty(query: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = Space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Icon(symbol("magnifyingglass"), null, tint = Ink.secondary, modifier = Modifier.size(36.dp))
        Text("No Results for “$query”", style = TextStyles.title3, color = Ink.text, textAlign = TextAlign.Center)
    }
}

// Menus

/** A menu item with an icon, for the + and view menus. */
@Composable
internal fun JournalMenuItem(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    destructive: Boolean = false,
    checked: Boolean = false,
) {
    val danger = if (LocalDark.current) Color(0xFFFF6B6B) else Color(0xFFC62828)
    val color = if (destructive) danger else Ink.text
    DropdownMenuItem(
        text = { Text(text, style = TextStyles.body, color = color) },
        onClick = onClick,
        leadingIcon = if (checked) ({ Icon(symbol("checkmark"), null, tint = Ink.accent, modifier = Modifier.size(20.dp)) }) else null,
        trailingIcon = icon?.let { { Icon(it, null, tint = color, modifier = Modifier.size(20.dp)) } },
    )
}

@Composable
internal fun JournalMenu(expanded: Boolean, onDismiss: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = Ink.surface,
        content = content,
    )
}

@Composable
internal fun JournalMenuDivider() {
    HorizontalDivider(color = Ink.border, modifier = Modifier.padding(vertical = Space.xxs))
}

// Motion

/** One small bounce of an icon shortly after it appears. Still with animations off. */
internal fun Modifier.journalBounceOnAppear(delayMillis: Int = 300): Modifier = composed {
    val reduce = LocalReduceMotion.current
    val scale = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        delay(delayMillis.toLong())
        scale.animateTo(1.25f, spring(dampingRatio = 1f, stiffness = 1400f))
        scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
    }
    graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}

/** One bounce whenever the key changes (not when first shown), like an SF Symbol bounce. */
internal fun Modifier.journalBounceOn(key: Any?): Modifier = composed {
    val reduce = LocalReduceMotion.current
    val scale = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(key) {
        if (first) { first = false; return@LaunchedEffect }
        if (reduce) return@LaunchedEffect
        scale.snapTo(0.8f)
        scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
    }
    graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}
