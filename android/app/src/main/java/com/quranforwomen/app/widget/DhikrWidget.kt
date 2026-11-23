package com.quranforwomen.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.quranforwomen.app.AppGraph

/** How much dhikr she counted today. Tapping opens the dhikr screen. */
class DhikrWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val count = AppGraph.get(context).dhikrExtras.total()
        val open = widgetIntent(context, "quranforwomen://dhikr")
        WidgetTickReceiver.schedule(context)
        provideContent {
            GlanceTheme { Content(count, open) }
        }
    }

    @Composable
    private fun Content(count: Int, open: Intent) {
        Column(
            GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(WidgetColors.emerald)
                .padding(16.dp)
                .clickable(actionStartActivity(open))
                .semantics { contentDescription = "Dhikr today, $count" },
        ) {
            WidgetEyebrow("Dhikr")
            Spacer(GlanceModifier.defaultWeight())
            Text(
                "$count",
                maxLines = 1,
                style = TextStyle(color = WidgetColors.cream, fontSize = 38.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Serif),
            )
            Text("today", style = TextStyle(color = WidgetColors.gold, fontSize = 13.sp))
        }
    }
}

class DhikrWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DhikrWidget()
}
