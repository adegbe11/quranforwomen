package com.quranforwomen.app.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.quranforwomen.app.AppGraph
import com.quranforwomen.app.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The kinds of fast she can mark on the calendar. */
enum class FastKind(val title: String) {
    ramadan("Ramadan fast"),
    voluntary("Voluntary fast"),
    makeUp("Making up a fast"),
}

/** The two Ramadan reminders. */
enum class RamadanMoment(val title: String) {
    suhoor("Suhoor"),
    iftar("Iftar"),
}

/**
 * Her fasts, the number of fasts she has to make up, and the suhoor and iftar reminders.
 * Kept only on this phone. Days she did not fast are simply left blank.
 */
class FastingStore(private val context: Context, private val prayer: PrayerStore) {
    private val sp = context.getSharedPreferences("fasting", Context.MODE_PRIVATE)

    /** "yyyy-MM-dd" to the kind of fast marked that day. */
    var fasts by mutableStateOf(load()); private set
    /** Fasts to make up in total, including the ones she has since made up. */
    private var makeUpOwed by mutableIntStateOf(sp.getInt(K_OWED, 0))

    var suhoorReminder by mutableStateOf(sp.getBoolean(K_SUHOOR, false)); private set
    var iftarReminder by mutableStateOf(sp.getBoolean(K_IFTAR, false)); private set
    /** How long before Fajr the suhoor reminder comes. */
    var suhoorMinutes by mutableIntStateOf(sp.getInt(K_SUHOOR_MINUTES, 30)); private set

    init {
        refreshReminders()
    }

    // Fasts

    fun kind(date: LocalDate): FastKind? = fasts[LibraryStore.dayKey(date)]

    fun setFast(date: LocalDate, kind: FastKind?) {
        val key = LibraryStore.dayKey(date)
        if (fasts[key] == kind) return
        fasts = if (kind == null) fasts - key else fasts + (key to kind)
        save()
    }

    private val madeUp: Int get() = fasts.values.count { it == FastKind.makeUp }

    /** Erase My Data: marked fasts and the make-up count. Reminder choices are settings and stay. */
    fun eraseAll() {
        fasts = emptyMap()
        makeUpOwed = 0
        sp.edit().remove(K_FASTS).remove(K_OWED).apply()
    }

    /** Counts down as she marks make-up fasts. */
    val makeUpLeft: Int get() = maxOf(0, makeUpOwed - madeUp)

    fun setMakeUpLeft(value: Int) {
        makeUpOwed = value.coerceIn(0, 9999) + madeUp
        sp.edit().putInt(K_OWED, makeUpOwed).apply()
    }

    /** Ramadan fasts in a Hijri year. */
    fun ramadanCount(hijriYear: Int): Int = fasts.count { (key, kind) ->
        kind == FastKind.ramadan && parse(key)?.let { Hijri.of(it).year } == hijriYear
    }

    /** Voluntary fasts in a Gregorian year. */
    fun voluntaryCount(year: Int): Int = fasts.count { (key, kind) ->
        kind == FastKind.voluntary && key.startsWith(String.format(Locale.US, "%04d-", year))
    }

    // Reminders

    fun setReminder(moment: RamadanMoment, on: Boolean) {
        when (moment) {
            RamadanMoment.suhoor -> { suhoorReminder = on; sp.edit().putBoolean(K_SUHOOR, on).apply() }
            RamadanMoment.iftar -> { iftarReminder = on; sp.edit().putBoolean(K_IFTAR, on).apply() }
        }
        refreshReminders()
    }

    fun updateSuhoorMinutes(minutes: Int) {
        suhoorMinutes = minutes
        sp.edit().putInt(K_SUHOOR_MINUTES, minutes).apply()
        refreshReminders()
    }

    /** Sets the next suhoor or iftar alarm. Each alarm sets the one after it. */
    fun refreshReminders() {
        RamadanAlarms.scheduleNext(context, this)
    }

    /** The next reminder, only on days of Ramadan. */
    fun nextReminder(after: ZonedDateTime = ZonedDateTime.now()): Pair<RamadanMoment, ZonedDateTime>? {
        if (!suhoorReminder && !iftarReminder) return null
        val today = after.toLocalDate()
        val h = Hijri.of(today)
        val nextStart = Hijri.start(if (h.month < Hijri.RAMADAN) h.year else h.year + 1, Hijri.RAMADAN)
        val days = (listOf(today, today.plusDays(1), today.plusDays(2), nextStart, nextStart.plusDays(1)))
            .distinct().sorted()
        for (day in days) {
            if (Hijri.of(day).month != Hijri.RAMADAN) continue
            val schedule = prayer.schedule(day) ?: return null
            val moments = buildList {
                if (suhoorReminder) schedule.time(Prayer.fajr)?.let { add(RamadanMoment.suhoor to it.minusMinutes(suhoorMinutes.toLong())) }
                if (iftarReminder) schedule.time(Prayer.maghrib)?.let { add(RamadanMoment.iftar to it) }
            }
            moments.sortedBy { it.second }.firstOrNull { it.second.isAfter(after) }?.let { return it }
        }
        return null
    }

    // Storage

    private fun load(): Map<String, FastKind> =
        (sp.getStringSet(K_FASTS, null) ?: emptySet()).mapNotNull { entry ->
            val parts = entry.split("=")
            if (parts.size != 2) return@mapNotNull null
            val kind = FastKind.entries.firstOrNull { it.name == parts[1] } ?: return@mapNotNull null
            parts[0] to kind
        }.toMap()

    private fun save() {
        sp.edit().putStringSet(K_FASTS, fasts.map { "${it.key}=${it.value.name}" }.toSet()).apply()
    }

    private fun parse(key: String): LocalDate? = runCatching { LocalDate.parse(key) }.getOrNull()

    companion object {
        private const val K_FASTS = "fasting.days"
        private const val K_OWED = "fasting.makeUpOwed"
        private const val K_SUHOOR = "fasting.suhoorReminder"
        private const val K_IFTAR = "fasting.iftarReminder"
        private const val K_SUHOOR_MINUTES = "fasting.suhoorMinutes"
    }
}

/** Suhoor and iftar alarms, set one at a time like the prayer alarms, with their own request codes. */
object RamadanAlarms {
    const val ACTION = "com.quranforwomen.app.RAMADAN"
    private const val REQUEST = 640
    private const val EXTRA_MOMENT = "ramadan.moment"
    private const val EXTRA_AT = "ramadan.at"
    private const val EXTRA_FAJR = "ramadan.fajr"
    private const val NOTIFY_BASE = 1640

    private fun pending(context: Context, moment: RamadanMoment?, at: Long, fajr: Long): PendingIntent {
        val intent = Intent(context, RamadanReceiver::class.java).apply {
            action = ACTION
            moment?.let { putExtra(EXTRA_MOMENT, it.name) }
            putExtra(EXTRA_AT, at)
            putExtra(EXTRA_FAJR, fajr)
        }
        return PendingIntent.getBroadcast(context, REQUEST, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun scheduleNext(context: Context, store: FastingStore) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pending(context, null, 0, 0))
        val (moment, time) = store.nextReminder() ?: return
        val millis = time.toInstant().toEpochMilli()
        val fajr = if (moment == RamadanMoment.suhoor) millis + store.suhoorMinutes * 60_000L else 0L
        val intent = pending(context, moment, millis, fajr)
        runCatching {
            if (canScheduleExact(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intent)
            } else {
                am.setWindow(AlarmManager.RTC_WAKEUP, millis, 10 * 60_000L, intent)
            }
        }
    }

    fun handle(context: Context, intent: Intent) {
        val graph = AppGraph.get(context)
        if (intent.action == ACTION) {
            val moment = RamadanMoment.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_MOMENT) }
            val at = intent.getLongExtra(EXTRA_AT, System.currentTimeMillis())
            val fajr = intent.getLongExtra(EXTRA_FAJR, 0L)
            // Dropped on rest days, and when it arrives more than half an hour late.
            if (moment != null && !graph.library.isResting && System.currentTimeMillis() - at < 30 * 60_000) {
                show(context, moment, at, fajr)
            }
        }
        graph.fasting.refreshReminders()
    }

    private fun show(context: Context, moment: RamadanMoment, at: Long, fajr: Long) {
        if (!Notifications.allowed(context)) return
        val is24 = android.text.format.DateFormat.is24HourFormat(context)
        val format = DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm a", Locale.getDefault())
        val body = when (moment) {
            RamadanMoment.suhoor ->
                if (fajr > 0) "Fajr is at ${Instant.ofEpochMilli(fajr).atZone(ZoneId.systemDefault()).format(format)}."
                else "Fajr is coming soon."
            RamadanMoment.iftar -> "It is Maghrib time."
        }
        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF064E3B.toInt())
            .setContentTitle(moment.title)
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setWhen(at)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(Notifications.openAppIntent(context, 650 + moment.ordinal, "quranforwomen://today"))
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFY_BASE + moment.ordinal, notification) }
    }
}

/** Fires the suhoor and iftar reminders, and sets them again after a restart, an update or a time zone change. */
class RamadanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        RamadanAlarms.handle(context, intent)
    }
}
