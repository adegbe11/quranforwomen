package com.quranforwomen.app.feature.ramadan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.data.Hijri
import com.quranforwomen.app.data.Prayer
import com.quranforwomen.app.data.RamadanMoment
import com.quranforwomen.app.nav.LocalRouter
import com.quranforwomen.app.nav.Route
import com.quranforwomen.app.ui.components.CardSurface
import com.quranforwomen.app.ui.components.Eyebrow
import com.quranforwomen.app.ui.components.GroupedSection
import com.quranforwomen.app.ui.components.RowDivider
import com.quranforwomen.app.ui.components.SegmentedControl
import com.quranforwomen.app.ui.components.SwitchRow
import com.quranforwomen.app.ui.components.focusIn
import com.quranforwomen.app.ui.components.openAppSettings
import com.quranforwomen.app.ui.components.pressable
import com.quranforwomen.app.ui.components.rememberNotificationRequest
import com.quranforwomen.app.ui.components.symbol
import com.quranforwomen.app.ui.theme.AppFont
import com.quranforwomen.app.ui.theme.Palette
import com.quranforwomen.app.ui.theme.Space
import com.quranforwomen.app.ui.theme.TextStyles
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
private fun rememberRamadanTimeFormat(): DateTimeFormatter {
    val context = LocalContext.current
    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    return remember(is24) { DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm a", Locale.getDefault()) }
}

/** True on a day of Ramadan, for Today to decide whether to show the card. */
fun ramadanToday(date: LocalDate): Boolean = Hijri.isRamadan(date)

/** One quiet card on Today during Ramadan: the day, when suhoor ends and when iftar is. */
@Composable
fun RamadanTodayCard(today: LocalDate, modifier: Modifier = Modifier) {
    val graph = LocalGraph.current
    val router = LocalRouter.current
    val h = remember(today) { Hijri.of(today) }
    if (h.month != Hijri.RAMADAN) return
    val prayer = graph.prayer
    val schedule = remember(today, prayer.latitude, prayer.longitude, prayer.calculationMethod, prayer.asr) {
        prayer.schedule(today)
    }
    val format = rememberRamadanTimeFormat()
    val fajr = schedule?.time(Prayer.fajr)
    val maghrib = schedule?.time(Prayer.maghrib)

    CardSurface(modifier, padding = Space.xl, onClick = { router.push(Route.IslamicCalendar) }) {
        Eyebrow("Ramadan", Modifier.focusIn(150))
        Spacer(Modifier.height(Space.xs))
        Text(
            "Day ${h.day} of Ramadan",
            style = TextStyles.title2.copy(fontFamily = AppFont.serif),
            color = Palette.textPrimary,
            modifier = Modifier.focusIn(250),
        )
        if (fajr != null && maghrib != null) {
            Spacer(Modifier.height(Space.l))
            Row(Modifier.fillMaxWidth().focusIn(400)) {
                RamadanTime("Suhoor ends", fajr, format, Modifier.weight(1f))
                RamadanTime("Iftar", maghrib, format, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(Space.l))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).focusIn(500),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(symbol("calendar"), null, tint = Palette.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(Space.s))
            Text("Fasting tracker", style = TextStyles.headline, color = Palette.accent, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = Palette.accent, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun RamadanTime(label: String, time: ZonedDateTime, format: DateTimeFormatter, modifier: Modifier = Modifier) {
    val text = time.format(format)
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = "$label at $text" }) {
        Text(label, style = TextStyles.subheadline, color = Palette.textSecondary)
        Text(text, style = TextStyles.title3, color = Palette.textPrimary)
    }
}

/** Suhoor and iftar reminders, in Prayer Settings. */
@Composable
fun RamadanReminderSection(modifier: Modifier = Modifier) {
    val graph = LocalGraph.current
    val fasting = graph.fasting
    val prayer = graph.prayer
    val context = LocalContext.current

    var denied by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<RamadanMoment?>(null) }
    val ask = rememberNotificationRequest { granted ->
        val moment = pending
        pending = null
        denied = !granted
        if (granted && moment != null) fasting.setReminder(moment, true)
    }

    fun toggle(moment: RamadanMoment, on: Boolean) {
        if (on) {
            pending = moment
            ask()
        } else {
            fasting.setReminder(moment, false)
        }
    }

    // Prayer times move with her settings, so the reminders follow them.
    LaunchedEffect(prayer.calculationMethod, prayer.asr, prayer.latitude, prayer.longitude) {
        fasting.refreshReminders()
    }

    GroupedSection(modifier.animateContentSize(), title = "Ramadan") {
        SwitchRow(
            title = "Suhoor reminder",
            checked = fasting.suhoorReminder,
            onCheckedChange = { toggle(RamadanMoment.suhoor, it) },
            icon = symbol("moon.stars"),
        )
        AnimatedVisibility(
            visible = fasting.suhoorReminder,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(start = 58.dp, end = Space.m, top = Space.xxs, bottom = Space.s),
                verticalArrangement = Arrangement.spacedBy(Space.xs),
            ) {
                Text("Before Fajr", style = TextStyles.subheadline, color = Palette.textSecondary)
                SegmentedControl(
                    options = listOf(15, 30, 45, 60),
                    selected = fasting.suhoorMinutes,
                    label = { "$it min" },
                    onSelect = { fasting.updateSuhoorMinutes(it) },
                )
            }
        }
        RowDivider()
        SwitchRow(
            title = "Iftar reminder",
            checked = fasting.iftarReminder,
            onCheckedChange = { toggle(RamadanMoment.iftar, it) },
            icon = symbol("sun.horizon"),
        )
        AnimatedVisibility(visible = denied, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Text(
                "Notifications are off in Settings.",
                style = TextStyles.footnote, color = Palette.blushStrong,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .pressable(scale = 0.99f, onClickLabel = "Open Settings") { context.openAppSettings() }
                    .padding(horizontal = Space.m, vertical = Space.s),
            )
        }
    }
    LaunchedEffect(denied) {
        if (denied && com.quranforwomen.app.data.Notifications.allowed(context)) denied = false
    }
}
