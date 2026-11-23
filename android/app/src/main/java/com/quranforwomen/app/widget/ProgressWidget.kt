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
import androidx.glance.appwidget.LinearProgressIndicator
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
import androidx.glance.text.TextStyle
import com.quranforwomen.app.AppGraph

private class PlanToday(val day: Int, val days: Int, val pages: IntRange, val finished: Int)

/** Surahs finished of 114, and the day of her reading plan when one is running. */
class ProgressWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val library = AppGraph.get(context).library
        val surahs = library.completedSurahs.size.coerceIn(0, 114)
        val plan = library.plan?.let { p ->
            val index = p.dayIndex()
            PlanToday(index + 1, p.days, p.pages(index), p.finishedDays.size)
        }
        val open = widgetIntent(context, if (plan == null) "quranforwomen://progress" else "quranforwomen://plan")
        WidgetTickReceiver.schedule(context)
        provideContent {
            GlanceTheme { Content(surahs, plan, open) }
        }
    }

    @Composable
    private fun Content(surahs: Int, plan: PlanToday?, open: Intent) {
        val wide = LocalSize.current.width >= 250.dp
        val label = buildString {
            append("$surahs of 114 surahs finished")
            if (plan != null) append(". Reading plan, day ${plan.day} of ${plan.days}")
        }
        Row(
            GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(WidgetColors.emerald)
                .padding(16.dp)
                .clickable(actionStartActivity(open))
                .semantics { contentDescription = label },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(GlanceModifier.defaultWeight().fillMaxHeight()) {
                WidgetEyebrow("Quran")
                Spacer(GlanceModifier.defaultWeight())
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "$surahs",
                        style = TextStyle(color = WidgetColors.cream, fontSize = 34.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Serif),
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    Text("of 114", style = TextStyle(color = WidgetColors.creamSoft, fontSize = 13.sp), modifier = GlanceModifier.padding(bottom = 6.dp))
                }
                Text("surahs finished", maxLines = 1, style = TextStyle(color = WidgetColors.creamSoft, fontSize = 13.sp))
                if (!wide && plan != null) {
                    Spacer(GlanceModifier.height(2.dp))
                    Text("Day ${plan.day} of ${plan.days}", maxLines = 1, style = TextStyle(color = WidgetColors.gold, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                }
            }
            if (wide) {
                Spacer(GlanceModifier.width(16.dp))
                Column(GlanceModifier.defaultWeight()) {
                    if (plan != null) {
                        Text(
                            "Day ${plan.day} of ${plan.days}",
                            maxLines = 1,
                            style = TextStyle(color = WidgetColors.cream, fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Serif),
                        )
                        val pages = if (plan.pages.first == plan.pages.last) "Page ${plan.pages.first}" else "Pages ${plan.pages.first} to ${plan.pages.last}"
                        Text(pages, maxLines = 1, style = TextStyle(color = WidgetColors.creamSoft, fontSize = 13.sp))
                        Spacer(GlanceModifier.height(8.dp))
                        Bar(plan.finished.toFloat() / plan.days)
                    } else {
                        Text("All 114 surahs", maxLines = 1, style = TextStyle(color = WidgetColors.creamSoft, fontSize = 13.sp))
                        Spacer(GlanceModifier.height(8.dp))
                        Bar(surahs / 114f)
                    }
                }
            }
        }
    }

    @Composable
    private fun Bar(fraction: Float) {
        LinearProgressIndicator(
            progress = fraction.coerceIn(0f, 1f),
            modifier = GlanceModifier.fillMaxWidth().height(6.dp).cornerRadius(3.dp),
            color = WidgetColors.gold,
            backgroundColor = WidgetColors.track,
        )
    }
}

class ProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ProgressWidget()
}
