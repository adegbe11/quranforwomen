package com.quranforwomen.app.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.quranforwomen.app.AppGraph
import com.quranforwomen.app.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Daily reminders added after the first two: one for the reading plan, one to write in the journal.
 * Each has its own request code and notification id, so turning one off never touches another.
 */
enum class GentleReminder(val requestCode: Int, val title: String, val body: String, val defaultMinutes: Int) {
    plan(140, "Reading plan", "Today's reading is ready whenever you are.", 7 * 60),
    journal(141, "Journal", "A few minutes to write, whenever you are ready.", 21 * 60),
}

/** Whether each of these reminders is on, and its time, in their own preferences file. */
class GentleReminderPrefs(context: Context) {
    private val sp = context.getSharedPreferences("gentle_reminders", Context.MODE_PRIVATE)
    private val enabled: Map<GentleReminder, MutableState<Boolean>> =
        GentleReminder.entries.associateWith { mutableStateOf(sp.getBoolean("${it.name}.enabled", false)) }
    private val minutes: Map<GentleReminder, MutableIntState> =
        GentleReminder.entries.associateWith { mutableIntStateOf(sp.getInt("${it.name}.minutes", it.defaultMinutes)) }

    fun isOn(reminder: GentleReminder): Boolean = enabled.getValue(reminder).value

    fun setOn(reminder: GentleReminder, on: Boolean) {
        enabled.getValue(reminder).value = on
        sp.edit().putBoolean("${reminder.name}.enabled", on).apply()
    }

    fun minutes(reminder: GentleReminder): Int = minutes.getValue(reminder).intValue

    fun setMinutes(reminder: GentleReminder, value: Int) {
        minutes.getValue(reminder).intValue = value
        sp.edit().putInt("${reminder.name}.minutes", value).apply()
    }
}

object GentleReminders {
    const val ACTION = "com.quranforwomen.app.GENTLE_REMINDER"
    private const val EXTRA = "gentle"

    private fun pending(context: Context, reminder: GentleReminder): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION
            putExtra(EXTRA, reminder.name)
        }
        return PendingIntent.getBroadcast(
            context, reminder.requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Sets the next daily reminder. The caller asks for notification permission first. */
    fun enable(context: Context, reminder: GentleReminder, minutes: Int) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val now = ZonedDateTime.now()
        var next = LocalDate.now().atTime(LocalTime.of(minutes / 60, minutes % 60)).atZone(now.zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val at = next.toInstant().toEpochMilli()
        val intent = pending(context, reminder)
        if (canScheduleExact(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        } else {
            // Without exact alarms the phone may deliver it a few minutes late.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }

    fun disable(context: Context, reminder: GentleReminder) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pending(context, reminder))
    }

    /** Cancels the reminder and switches its setting off, for example when the plan ends. */
    fun turnOff(context: Context, reminder: GentleReminder) {
        disable(context, reminder)
        AppGraph.get(context).gentleReminders.setOn(reminder, false)
    }

    /** Re-sets every one of these reminders she has on, after a restart or an update. */
    fun restoreAll(context: Context) {
        val prefs = AppGraph.get(context).gentleReminders
        GentleReminder.entries.forEach { if (prefs.isOn(it)) enable(context, it, prefs.minutes(it)) }
    }

    /** Called by the reminder receiver: shows the reminder and sets tomorrow's. */
    fun handle(context: Context, intent: Intent) {
        val reminder = GentleReminder.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA) } ?: return
        val prefs = AppGraph.get(context).gentleReminders
        if (!prefs.isOn(reminder)) return
        show(context, reminder)
        enable(context, reminder, prefs.minutes(reminder))
    }

    private fun show(context: Context, reminder: GentleReminder) {
        if (!Notifications.allowed(context)) return
        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF064E3B.toInt())
            .setContentTitle(reminder.title)
            .setContentText(reminder.body)
            .setAutoCancel(true)
            .setContentIntent(Notifications.openAppIntent(context, 500 + reminder.requestCode, "quranforwomen://today"))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(reminder.requestCode, notification) }
    }
}
