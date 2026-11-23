package com.quranforwomen.app.ui.components

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.quranforwomen.app.ui.theme.BookInk
import com.quranforwomen.app.ui.theme.Ink
import com.quranforwomen.app.ui.theme.LocalDark
import com.quranforwomen.app.ui.theme.LocalInBook
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Radius
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import java.io.File

/** An on/off switch in the app's colours. Ticks under her finger. */
@Composable
fun AppSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val haptics = rememberHaptics()
    Switch(
        checked = checked,
        onCheckedChange = { haptics.tick(); onCheckedChange(it) },
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Ink.fill,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Ink.secondary.copy(alpha = 0.25f),
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

/** A row with a title and a switch, for grouped lists. The whole row toggles. */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    ListRow(
        title = title,
        icon = icon,
        subtitle = subtitle,
        showsChevron = false,
        modifier = modifier.semantics { role = Role.Switch },
        trailing = { AppSwitch(checked, onCheckedChange, enabled = enabled) },
        onClick = if (enabled) ({ onCheckedChange(!checked) }) else null,
    )
}

/** A row with a checkmark when chosen, for picking one option from a grouped list. */
@Composable
fun ChoiceRow(title: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null, icon: ImageVector? = null) {
    val haptics = rememberHaptics()
    ListRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        showsChevron = false,
        modifier = modifier.semantics { this.selected = selected },
        trailing = {
            if (selected) Icon(symbol("checkmark"), null, tint = Ink.accent, modifier = Modifier.size(22.dp))
        },
        onClick = { if (!selected) haptics.tick(); onClick() },
    )
}

/** An iOS style segmented control: a sliding thumb over equal segments. */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val dark = LocalDark.current
    val track = if (LocalInBook.current) BookInk.gild.copy(alpha = 0.2f) else if (dark) Color(0xFF1C2420) else Color(0xFFEDE9DF)
    val thumb = if (LocalInBook.current) BookInk.pageLit else if (dark) Color(0xFF2E3833) else Color.White
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(track)
            .padding(2.dp)
    ) {
        val w = maxWidth / options.size
        val index = options.indexOf(selected).coerceAtLeast(0)
        val x by animateDpAsState(w * index, spring(dampingRatio = 0.8f, stiffness = 500f), label = "segment")
        Box(
            Modifier.offset(x = x).width(w).fillMaxHeight()
                .shadow(2.dp, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)).background(thumb)
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEach { option ->
                val isSel = option == selected
                Box(
                    Modifier.weight(1f).fillMaxHeight()
                        .pressable(scale = 0.96f) { if (!isSel) { haptics.tick(); onSelect(option) } }
                        .semantics { this.selected = isSel; role = Role.Tab },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label(option),
                        style = TextStyles.footnote.copy(fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium),
                        color = Ink.text,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** A small pill, for filters and tags. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val bg = if (selected) Ink.fill else Ink.wash
    val fg = if (selected) Ink.onFill else Ink.accent
    Row(
        modifier
            .heightIn(min = 36.dp)
            .clip(CircleShape)
            .background(bg)
            .pressable(scale = 0.95f, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .semantics { this.selected = selected },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
        Text(text, style = TextStyles.subheadline.copy(fontWeight = FontWeight.Medium), color = fg, maxLines = 1)
    }
}

/** A calm text field on a card surface. Multi line when minLines is above 1. */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    maxLines: Int = if (minLines > 1) Int.MAX_VALUE else 1,
    style: TextStyle = TextStyles.body,
    filled: Boolean = true,
    imeAction: ImeAction = if (minLines > 1) ImeAction.Default else ImeAction.Done,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
) {
    val color = Ink.text
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .then(if (filled) Modifier.clip(RoundedCornerShape(Radius.control)).background(Ink.surface) else Modifier)
            .padding(if (filled) Space.m else 0.dp),
        textStyle = style.copy(color = color),
        minLines = minLines,
        maxLines = maxLines,
        cursorBrush = SolidColor(Ink.accent),
        keyboardOptions = KeyboardOptions(capitalization = capitalization, imeAction = imeAction),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, style = style, color = Ink.secondary.copy(alpha = 0.7f))
                inner()
            }
        },
    )
}

/** A search field with a magnifier, like the iOS search bar. */
@Composable
fun SearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val dark = LocalDark.current
    Row(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (LocalInBook.current) BookInk.gild.copy(alpha = 0.18f) else if (dark) Color(0xFF1C2420) else Color(0xFFEDE9DF))
            .padding(horizontal = Space.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Icon(symbol("magnifyingglass"), null, tint = Ink.secondary, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f)) {
            AppTextField(value, onValueChange, placeholder, filled = false, imeAction = ImeAction.Search, capitalization = KeyboardCapitalization.None)
        }
        if (value.isNotEmpty()) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).pressable { onValueChange("") }.semantics { contentDescription = "Clear" },
                contentAlignment = Alignment.Center,
            ) { Icon(symbol("xmark.circle.fill"), null, tint = Ink.secondary.copy(alpha = 0.6f), modifier = Modifier.size(18.dp)) }
        }
    }
}

/** A short alert, like an iOS confirmation. A destructive action shows in red. */
@Composable
fun AppAlert(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    message: String? = null,
    destructive: Boolean = false,
    dismissLabel: String = "Cancel",
) {
    val danger = if (LocalDark.current) Color(0xFFFF6B6B) else Color(0xFFC62828)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.surface,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, style = TextStyles.headline, color = Palette.textPrimary, textAlign = TextAlign.Start) },
        text = message?.let { { Text(it, style = TextStyles.subheadline, color = Palette.textSecondary) } },
        confirmButton = {
            TextActionColored(confirmLabel, if (destructive) danger else Palette.accent, bold = true) { onConfirm(); onDismiss() }
        },
        dismissButton = { TextActionColored(dismissLabel, Palette.accent) { onDismiss() } },
    )
}

@Composable
private fun TextActionColored(text: String, color: Color, bold: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.heightIn(min = 44.dp).pressable(onClick = onClick).padding(horizontal = Space.s), contentAlignment = Alignment.Center) {
        Text(text, style = if (bold) TextStyles.headline else TextStyles.body, color = color)
    }
}

/** A labelled value stepper, minus and plus around a number. */
@Composable
fun Stepper(value: Int, onValueChange: (Int) -> Unit, range: IntRange, modifier: Modifier = Modifier, step: Int = 1) {
    val haptics = rememberHaptics()
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
        StepButton("-", enabled = value - step >= range.first) { haptics.tick(); onValueChange((value - step).coerceIn(range)) }
        Text(value.toString(), style = TextStyles.headline, color = Ink.text, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
        StepButton("+", enabled = value + step <= range.last) { haptics.tick(); onValueChange((value + step).coerceIn(range)) }
    }
}

@Composable
private fun StepButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(Ink.wash)
            .pressable(scale = 0.9f, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = TextStyles.title3, color = if (enabled) Ink.accent else Ink.secondary.copy(alpha = 0.4f)) }
}

// Permissions

/** True if a runtime permission is granted. */
fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/**
 * Asks for city level location, then finds her location. Returns a function to call from a button.
 * If she says no, PrayerStore.locationDenied turns true and screens can offer the phone's settings.
 */
@Composable
fun rememberLocationRequest(): () -> Unit {
    val graph = com.quranforwomen.app.LocalGraph.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) graph.prayer.requestLocation() else graph.prayer.markDenied()
    }
    val context = LocalContext.current
    return {
        if (context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) graph.prayer.requestLocation()
        else launcher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}

/**
 * Asks to send notifications (Android 13 and later), then calls back with the answer.
 * Earlier Android versions allow notifications by default.
 */
@Composable
fun rememberNotificationRequest(onResult: (Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> onResult(granted) }
    return {
        if (Build.VERSION.SDK_INT >= 33 && !context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onResult(com.quranforwomen.app.data.Notifications.allowed(context))
        }
    }
}

/** Opens this app's page in the phone's Settings, for permissions she turned off. */
fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
}

/** Opens the phone's setting for on-time alarms (Android 12 and later). */
fun Context.openExactAlarmSettings() {
    if (Build.VERSION.SDK_INT < 31) return
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try { startActivity(intent) } catch (e: ActivityNotFoundException) { openAppSettings() }
}

// Sharing

/** Opens the share sheet with plain text. */
fun Context.shareText(text: String, title: String? = null) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        title?.let { putExtra(Intent.EXTRA_TITLE, it) }
    }
    runCatching { startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** Opens the share sheet with a file from cacheDir/shared (an image or the journal export). */
fun Context.shareFile(file: File, mimeType: String, title: String? = null) {
    val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** A file in the share cache, the only folder FileProvider exposes. */
fun Context.sharedCacheFile(name: String): File = File(File(cacheDir, "shared").apply { mkdirs() }, name)

/** Copies text to the clipboard. Android 13 and later show their own confirmation. */
fun Context.copyText(text: String, label: String = "Quran for Women") {
    val cm = getSystemService(android.content.ClipboardManager::class.java) ?: return
    cm.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
}

/**
 * Android 12 and later can hold prayer reminders back by many minutes unless she allows exact alarms.
 * Call the returned function right after reminders are switched on; it asks once, only when needed.
 */
@Composable
fun rememberExactAlarmPrompt(): () -> Unit {
    val context = LocalContext.current
    var show by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    if (show) {
        AppAlert(
            title = "Reminders on time",
            message = "Allow exact alarms so each reminder comes at the prayer time.",
            confirmLabel = "Allow",
            onConfirm = { context.openExactAlarmSettings() },
            onDismiss = { show = false },
            dismissLabel = "Not now",
        )
    }
    return { if (!com.quranforwomen.app.data.canScheduleExact(context)) show = true }
}
