package com.quranforwomen.app.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.quranforwomen.app.AppGraph
import com.quranforwomen.app.MainActivity
import com.quranforwomen.app.R
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** The gentle times offered during onboarding. */
enum class ReminderMoment(val title: String, val detail: String, val minutes: Int?) {
    morning("Morning", "Around 7:00 AM", 7 * 60),
    afternoon("Afternoon", "Around 1:00 PM", 13 * 60),
    evening("Evening", "Around 8:00 PM", 20 * 60),
    later("I'll decide later", "No reminder for now", null),
}

/** Two optional gentle reminders: a Quran moment, and evening remembrance. No streaks, no guilt. */
enum class ReminderKind(val id: Int, val title: String, val body: String) {
    quran(101, "A moment with the Quran", "Your verse for today is here whenever you are ready."),
    evening(102, "Evening remembrance", "A few calm minutes of evening duas and dhikr."),
}

object Notifications {
    const val CHANNEL_REMINDERS = "reminders"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val bell = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.prayer_bell}")
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(PrayerSound.bell.channelId, context.getString(R.string.channel_prayer), NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(bell, attrs)
                },
                NotificationChannel(PrayerSound.system.channelId, "${context.getString(R.string.channel_prayer)} (phone sound)", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(CHANNEL_REMINDERS, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT),
            )
        )
    }

    fun allowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun openAppIntent(context: Context, requestCode: Int, uri: String? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            uri?.let { data = Uri.parse(it) }
        }
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}

private fun alarmManager(context: Context) = context.getSystemService(AlarmManager::class.java)

/** True when alarms can fire on the exact minute. On Android 12 and later she can switch this off. */
fun canScheduleExact(context: Context): Boolean =
    Build.VERSION.SDK_INT < 31 || alarmManager(context)?.canScheduleExactAlarms() == true

private fun setAlarm(context: Context, atMillis: Long, intent: PendingIntent) {
    val am = alarmManager(context) ?: return
    if (canScheduleExact(context)) {
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, intent)
    } else {
        // Without exact alarms, ask for the tightest window Android allows (about 10 minutes)
        // instead of the default, which can be an hour late.
        am.setWindow(AlarmManager.RTC_WAKEUP, atMillis, 10 * 60_000L, intent)
    }
}

object PrayerAlarms {
    private const val REQUEST = 200

    private fun pending(context: Context, prayer: Prayer?, at: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_PRAYER
            prayer?.let { putExtra(EXTRA_PRAYER, it.name) }
            putExtra(EXTRA_TIME, at)
        }
        return PendingIntent.getBroadcast(context, REQUEST, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun scheduleNext(context: Context, store: PrayerStore) {
        val am = alarmManager(context) ?: return
        am.cancel(pending(context, null, 0))
        // Keep the small copy used before unlock after a restart in step with her settings.
        PrayerMirror.save(context, store)
        val (prayer, time) = store.nextAlarm() ?: return
        val millis = time.toInstant().toEpochMilli()
        setAlarm(context, millis, pending(context, prayer, millis))
    }

    fun show(context: Context, prayer: Prayer, time: Long) {
        if (!Notifications.allowed(context)) return
        val graph = AppGraph.get(context)
        val channel = graph.prayer.sound.channelId
        val prayed = PendingIntent.getBroadcast(
            context, 300 + prayer.ordinal,
            Intent(context, PrayedActionReceiver::class.java).putExtra(EXTRA_PRAYER, prayer.name).putExtra(EXTRA_TIME, time),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF064E3B.toInt())
            .setContentTitle("Time for ${prayer.title}")
            .setContentText("Hayya 'ala as-salah. Come to prayer.")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setWhen(time)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(Notifications.openAppIntent(context, 400 + prayer.ordinal))
            .addAction(0, "I prayed", prayed)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFY_PRAYER_BASE + prayer.ordinal, notification) }
    }
}

object ReminderScheduler {
    private fun pending(context: Context, kind: ReminderKind): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_KIND, kind.name)
        }
        return PendingIntent.getBroadcast(context, kind.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Sets the next daily reminder. The caller asks for notification permission first. */
    fun enable(context: Context, minutes: Int, kind: ReminderKind) {
        val now = ZonedDateTime.now()
        var next = LocalDate.now().atTime(LocalTime.of(minutes / 60, minutes % 60)).atZone(now.zone)
        if (!next.isAfter(now)) next = next.plusDays(1)
        setAlarm(context, next.toInstant().toEpochMilli(), pending(context, kind))
    }

    fun disable(context: Context, kind: ReminderKind) {
        alarmManager(context)?.cancel(pending(context, kind))
    }

    /** Re-sets every reminder she has on, after a restart or an update. */
    fun restoreAll(context: Context) {
        val prefs = AppGraph.get(context).prefs
        if (prefs.reminderEnabled) enable(context, prefs.reminderMinutes, ReminderKind.quran)
        if (prefs.eveningReminderEnabled) enable(context, prefs.eveningReminderMinutes, ReminderKind.evening)
    }

    fun show(context: Context, kind: ReminderKind) {
        if (!Notifications.allowed(context)) return
        val target = if (kind == ReminderKind.quran) "quranforwomen://today" else "quranforwomen://dhikr"
        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF064E3B.toInt())
            .setContentTitle(kind.title)
            .setContentText(kind.body)
            .setAutoCancel(true)
            .setContentIntent(Notifications.openAppIntent(context, 500 + kind.id, target))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(kind.id, notification) }
    }
}

const val ACTION_PRAYER = "com.quranforwomen.app.PRAYER"
const val ACTION_REMINDER = "com.quranforwomen.app.REMINDER"
const val EXTRA_PRAYER = "prayer"
const val EXTRA_TIME = "time"
const val EXTRA_KIND = "kind"
private const val NOTIFY_PRAYER_BASE = 1000

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val graph = AppGraph.get(context)
        when (intent.action) {
            GentleReminders.ACTION -> GentleReminders.handle(context, intent)
            ACTION_PRAYER -> {
                val prayer = Prayer.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_PRAYER) }
                val time = intent.getLongExtra(EXTRA_TIME, System.currentTimeMillis())
                // A late delivery (phone off, deep sleep) more than half an hour after the time is dropped.
                if (prayer != null && !graph.library.isResting && System.currentTimeMillis() - time < 30 * 60_000) {
                    PrayerAlarms.show(context, prayer, time)
                }
                graph.prayer.refreshNotifications()
            }
            ACTION_REMINDER -> {
                val kind = ReminderKind.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_KIND) } ?: return
                ReminderScheduler.show(context, kind)
                val prefs = graph.prefs
                val minutes = if (kind == ReminderKind.quran) prefs.reminderMinutes else prefs.eveningReminderMinutes
                ReminderScheduler.enable(context, minutes, kind)
            }
        }
    }
}

/** The "Prayed" button on a prayer notification marks it in her prayer log. */
class PrayedActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayer = Prayer.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_PRAYER) } ?: return
        val time = intent.getLongExtra(EXTRA_TIME, System.currentTimeMillis())
        val day = java.time.Instant.ofEpochMilli(time).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        AppGraph.get(context).library.setPrayed(prayer.name, day)
        NotificationManagerCompat.from(context).cancel(NOTIFY_PRAYER_BASE + prayer.ordinal)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val graph = AppGraph.get(context)
        // Unlocked: the normal reminder takes over from the one set while locked.
        LockedPrayerAlarms.cancel(context)
        graph.prayer.refreshNotifications()
        ReminderScheduler.restoreAll(context)
        GentleReminders.restoreAll(context)
        graph.fasting.refreshReminders()
    }
}
