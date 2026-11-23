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
import com.quranforwomen.app.data.Dua
import java.time.LocalDate

/** Today's dua, the same one as the Today screen. Tapping opens it. */
class DuaWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dua = AppGraph.get(context).content.duaOfTheDay(LocalDate.now())
        val open = widgetIntent(context, "quranforwomen://dua/${dua.id}")
        WidgetTickReceiver.schedule(context)
        provideContent {
            GlanceTheme { Content(dua, open) }
        }
    }

    @Composable
    private fun Content(dua: Dua, open: Intent) {
        val size = LocalSize.current
        val wide = size.width >= 250.dp
        val tall = size.height >= 220.dp
        Column(
            GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(WidgetColors.emerald)
                .padding(16.dp)
                .clickable(actionStartActivity(open))
                .semantics { contentDescription = "Dua of the day. ${dua.title}" },
        ) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WidgetEyebrow("Dua of the day")
                if (wide) {
                    Spacer(GlanceModifier.width(8.dp))
                    Text(
                        dua.source,
                        maxLines = 1,
                        style = TextStyle(color = WidgetColors.creamSoft, fontSize = 10.sp, textAlign = TextAlign.End),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                }
            }
            Spacer(GlanceModifier.height(6.dp))
            Text(
                dua.title,
                maxLines = if (wide) 1 else 2,
                style = TextStyle(color = WidgetColors.cream, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Serif),
            )
            Spacer(GlanceModifier.height(6.dp))
            Text(
                dua.arabic,
                maxLines = if (tall) 5 else 2,
                style = TextStyle(color = WidgetColors.cream, fontSize = if (tall) 20.sp else 17.sp, textAlign = TextAlign.End),
                modifier = GlanceModifier.fillMaxWidth(),
            )
            if (wide) {
                Spacer(GlanceModifier.height(6.dp))
                Text(
                    dua.translation,
                    maxLines = if (tall) 7 else 2,
                    style = TextStyle(color = WidgetColors.creamSoft, fontSize = 13.sp, fontFamily = FontFamily.Serif),
                )
            } else {
                Spacer(GlanceModifier.height(4.dp))
                Text(dua.source, maxLines = 1, style = TextStyle(color = WidgetColors.creamSoft, fontSize = 10.sp))
            }
        }
    }
}

class DuaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DuaWidget()
}
