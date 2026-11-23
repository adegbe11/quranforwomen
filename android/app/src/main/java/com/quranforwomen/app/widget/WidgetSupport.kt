package com.quranforwomen.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.quranforwomen.app.AppGraph
import com.quranforwomen.app.LocalGraph
import com.quranforwomen.app.MainActivity
import com.quranforwomen.app.data.canScheduleExact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Brand colours for the home screen widgets, matching the verse widget. */
internal object WidgetColors {
    val emerald = Color(0xFF064E3B)
    val cream = ColorProvider(Color(0xFFFAF7F0))
    val creamSoft = ColorProvider(Color(0xB3FAF7F0))
    val gold = ColorProvider(Color(0xFFE9CF8F))
    val track = ColorProvider(Color(0x33FAF7F0))
}

/** The small capitals label at the top of each widget. */
@Composable
internal fun WidgetEyebrow(text: String) {
    Text(
        text.uppercase(Locale.getDefault()),
        maxLines = 1,
        style = TextStyle(color = WidgetColors.gold, fontSize = 10.sp, fontWeight = FontWeight.Medium),
    )
}

internal fun widgetIntent(context: Context, link: String): Intent =
    Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse(link)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

/** Short clock time in her phone's 12 or 24 hour style. */
internal fun widgetClock(context: Context, time: ZonedDateTime): String {
    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    return time.format(DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm a", Locale.getDefault()))
}

/** Redraws the widgets she has placed. Safe to call often. */
object WidgetUpdates {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    internal suspend fun update(context: Context, widget: GlanceAppWidget) {
        runCatching {
            if (GlanceAppWidgetManager(context).getGlanceIds(widget.javaClass).isNotEmpty()) widget.updateAll(context)
        }
    }

    suspend fun prayer(context: Context) = update(context, PrayerWidget())
    suspend fun progress(context: Context) = update(context, ProgressWidget())
    suspend fun dhikr(context: Context) = update(context, DhikrWidget())

    /** The daily widgets, after midnight or a prayer time. */
    fun all(context: Context) {
        val app = context.applicationContext
        scope.launch {
            update(app, DuaWidget())
            update(app, PrayerWidget())
            update(app, ProgressWidget())
            update(app, DhikrWidget())
        }
    }
}

/** Keeps the widgets in step with the app while it is open. Placed once in the app root. */
@Composable
fun WidgetSync() {
    val graph = LocalGraph.current
    val context = LocalContext.current.applicationContext
    val prayer = graph.prayer
    val library = graph.library
    LaunchedEffect(prayer.latitude, prayer.longitude, prayer.calculationMethod, prayer.asr, prayer.placeName) {
        WidgetUpdates.prayer(context)
    }
    LaunchedEffect(library.completedSurahs.size, library.plan) {
        WidgetUpdates.progress(context)
    }
    LaunchedEffect(graph.dhikrExtras.total()) {
        // Wait for a pause in counting before redrawing.
        delay(2000)
        WidgetUpdates.dhikr(context)
    }
}

/**
 * Wakes the widgets at the next prayer time and at midnight, so the next prayer,
 * today's dua and today's dhikr move on without the app open.
 */
class WidgetTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WidgetUpdates.all(context)
    }

    companion object {
        /** Sets the next wake up. Called whenever a widget draws. */
        fun schedule(context: Context) {
            val app = context.applicationContext
            val am = app.getSystemService(AlarmManager::class.java) ?: return
            val now = ZonedDateTime.now()
            val midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone).plusMinutes(1)
            val prayer = runCatching { AppGraph.get(app).prayer.nextPrayer(now)?.second }.getOrNull()
            val at = listOfNotNull(prayer?.plusSeconds(5), midnight).minOrNull() ?: midnight
            val pending = PendingIntent.getBroadcast(
                app, 4107, Intent(app, WidgetTickReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val millis = at.toInstant().toEpochMilli()
            runCatching {
                if (canScheduleExact(app)) am.setExactAndAllowWhileIdle(AlarmManager.RTC, millis, pending)
                else am.setAndAllowWhileIdle(AlarmManager.RTC, millis, pending)
            }
        }
    }
}
