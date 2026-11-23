package com.quranforwomen.app.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.quranforwomen.app.R
import java.time.ZonedDateTime

/**
 * Prayer reminders before the phone is unlocked.
 *
 * After a restart (for example an overnight update), Android keeps the app's normal storage sealed
 * until she unlocks the phone, and only then tells apps the phone has started. Fajr could pass first.
 * So a tiny copy of just what the prayer reminder needs (place, method, which prayers, sound) is kept
 * in the storage Android opens at start, and the next reminder is set from that while still locked.
 * Nothing private is in it: no journal, no notes, no saved verses.
 */
object PrayerMirror {
    private fun sp(context: Context) =
        context.createDeviceProtectedStorageContext().getSharedPreferences("prayer_mirror", Context.MODE_PRIVATE)

    /** Called whenever the normal prayer alarm is set, so the copy always matches. */
    fun save(context: Context, store: PrayerStore) {
        val lat = store.latitude
        val lon = store.longitude
        val on = store.notifyAtPrayerTimes && !store.isResting && lat != null && lon != null
        sp(context).edit()
            .putBoolean("on", on)
            .putFloat("lat", (lat ?: 0.0).toFloat())
            .putFloat("lon", (lon ?: 0.0).toFloat())
            .putString("method", store.calculationMethod.name)
            .putString("asr", store.asr.name)
            .putStringSet("enabled", store.enabledPrayers.map { it.name }.toSet())
            .putString("channel", store.sound.channelId)
            .apply()
    }

    fun channel(context: Context): String = sp(context).getString("channel", null) ?: PrayerSound.bell.channelId

    /** The next chosen prayer from the copy, or null when reminders are off. */
    fun next(context: Context, after: ZonedDateTime = ZonedDateTime.now()): Pair<Prayer, ZonedDateTime>? {
        val p = sp(context)
        if (!p.getBoolean("on", false)) return null
        val lat = p.getFloat("lat", 0f).toDouble()
        val lon = p.getFloat("lon", 0f).toDouble()
        val method = CalculationMethod.entries.firstOrNull { it.name == p.getString("method", null) } ?: CalculationMethod.muslimWorldLeague
        val asr = AsrMethod.entries.firstOrNull { it.name == p.getString("asr", null) } ?: AsrMethod.standard
        val enabled = p.getStringSet("enabled", null)?.mapNotNull { n -> Prayer.entries.firstOrNull { it.name == n } }?.toSet()
            ?: Prayer.prayers.toSet()
        for (offset in 0L..2L) {
            val schedule = PrayerCalculator.schedule(after.toLocalDate().plusDays(offset), lat, lon, method, asr)
            Prayer.prayers.filter { it in enabled }.forEach { prayer ->
                val t = schedule.time(prayer) ?: return@forEach
                if (t.isAfter(after)) return prayer to t
            }
        }
        return null
    }
}

/** Sets and shows prayer reminders while the phone is still locked after a restart. */
object LockedPrayerAlarms {
    private const val REQUEST = 210
    const val ACTION = "com.quranforwomen.app.LOCKED_PRAYER"

    private fun pending(context: Context, prayer: Prayer?, at: Long): PendingIntent {
        val intent = Intent(context, LockedBootReceiver::class.java).apply {
            action = ACTION
            prayer?.let { putExtra(EXTRA_PRAYER, it.name) }
            putExtra(EXTRA_TIME, at)
        }
        return PendingIntent.getBroadcast(context, REQUEST, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun scheduleNext(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pending(context, null, 0))
        val (prayer, time) = PrayerMirror.next(context) ?: return
        val millis = time.toInstant().toEpochMilli()
        val intent = pending(context, prayer, millis)
        if (canScheduleExact(context)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intent)
        else am.setWindow(AlarmManager.RTC_WAKEUP, millis, 10 * 60_000L, intent)
    }

    /** Once she unlocks, the normal reminders take over and this one steps aside. */
    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pending(context, null, 0))
    }

    fun show(context: Context, prayer: Prayer, time: Long) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        // No "I prayed" button here: her prayer log is sealed until she unlocks the phone.
        val notification = NotificationCompat.Builder(context, PrayerMirror.channel(context))
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF064E3B.toInt())
            .setContentTitle("Time for ${prayer.title}")
            .setContentText("Hayya 'ala as-salah. Come to prayer.")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setWhen(time)
            .setShowWhen(true)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(1000 + prayer.ordinal, notification) }
    }
}

/** Runs before the first unlock (direct boot aware). Touches only the small prayer copy. */
class LockedBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> LockedPrayerAlarms.scheduleNext(context)
            LockedPrayerAlarms.ACTION -> {
                val prayer = Prayer.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_PRAYER) }
                val time = intent.getLongExtra(EXTRA_TIME, System.currentTimeMillis())
                if (prayer != null && System.currentTimeMillis() - time < 30 * 60_000) LockedPrayerAlarms.show(context, prayer, time)
                LockedPrayerAlarms.scheduleNext(context)
            }
        }
    }
}
