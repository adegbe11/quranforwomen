package com.quranforwomen.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.quranforwomen.app.AppGraph
import com.quranforwomen.app.data.Prayer

private class PrayerLine(val name: String, val time: String, val isNext: Boolean)

private class PrayerWidgetData(val name: String, val time: String, val place: String?, val day: List<PrayerLine>)

/** The next prayer, and the five times of its day on the wider size. Uses her saved location and settings. */
class PrayerWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prayer = AppGraph.get(context).prayer
        val next = prayer.nextPrayer()
        val data = next?.let { (p, time) ->
            val schedule = prayer.schedule(time.toLocalDate())
            PrayerWidgetData(
                name = p.title,
                time = widgetClock(context, time),
                place = prayer.placeName,
                day = Prayer.prayers.mapNotNull { each ->
                    schedule?.time(each)?.let { PrayerLine(each.title, widgetClock(context, it), each == p) }
                },
            )
        }
        val open = widgetIntent(context, "quranforwomen://prayer")
        WidgetTickReceiver.schedule(context)
        provideContent {
            GlanceTheme { Content(data, open) }
        }
    }

    @Composable
    private fun Content(data: PrayerWidgetData?, open: Intent) {
        val wide = LocalSize.current.width >= 250.dp
        Row(
            GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(WidgetColors.emerald)
                .padding(16.dp)
                .clickable(actionStartActivity(open))
                .semantics {
                    contentDescription = if (data == null) "Prayer times" else "Next prayer, ${data.name} at ${data.time}"
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(GlanceModifier.defaultWeight().fillMaxHeight()) {
                WidgetEyebrow("Next prayer")
                Spacer(GlanceModifier.defaultWeight())
                if (data == null) {
                    Text(
                        "Add your location",
                        style = TextStyle(color = WidgetColors.cream, fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Serif),
                    )
                } else {
                    Text(
                        data.name,
                        maxLines = 1,
                        style = TextStyle(color = WidgetColors.cream, fontSize = 26.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Serif),
                    )
                    Text(data.time, maxLines = 1, style = TextStyle(color = WidgetColors.gold, fontSize = 18.sp, fontWeight = FontWeight.Medium))
                    data.place?.let {
                        Spacer(GlanceModifier.height(2.dp))
                        Text(it, maxLines = 1, style = TextStyle(color = WidgetColors.creamSoft, fontSize = 11.sp))
                    }
                }
            }
            if (wide && data != null && data.day.isNotEmpty()) {
                Spacer(GlanceModifier.width(16.dp))
                Column(GlanceModifier.defaultWeight()) {
                    data.day.forEach { line ->
                        val color = if (line.isNext) WidgetColors.gold else WidgetColors.creamSoft
                        val weight = if (line.isNext) FontWeight.Bold else FontWeight.Normal
                        Row(GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(line.name, maxLines = 1, style = TextStyle(color = color, fontSize = 13.sp, fontWeight = weight), modifier = GlanceModifier.defaultWeight())
                            Text(line.time, maxLines = 1, style = TextStyle(color = color, fontSize = 13.sp, fontWeight = weight, textAlign = TextAlign.End))
                        }
                    }
                }
            }
        }
    }
}

class PrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerWidget()
}
