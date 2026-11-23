package com.quranforwomen.app.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.quranforwomen.app.AppGraph
import com.quranforwomen.app.MainActivity
import java.time.LocalDate

/** Today's verse on the home screen: the Arabic, her translation, and the reference. Tapping opens it. */
class VerseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val graph = AppGraph.get(context)
        val ref = graph.content.verseOfTheDay(LocalDate.now())
        val surah = graph.quran.surah(ref.surah)
        val arabic = graph.quran.text(ref)
        val translation = graph.translations.text(ref)
        val open = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("quranforwomen://verse/${ref.id}")
        }
        provideContent {
            GlanceTheme {
                Content(arabic, translation, "${surah.name} ${ref.id}", open)
            }
        }
    }

    @Composable
    private fun Content(arabic: String, translation: String?, reference: String, open: Intent) {
        val cream = ColorProvider(Color(0xFFFAF7F0))
        Column(
            GlanceModifier
                .fillMaxSize()
                .cornerRadius(24.dp)
                .background(Color(0xFF064E3B))
                .padding(16.dp)
                .clickable(actionStartActivity(open)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                arabic,
                maxLines = 3,
                style = TextStyle(color = cream, fontSize = 20.sp, textAlign = TextAlign.End),
                modifier = GlanceModifier.fillMaxWidth(),
            )
            if (translation != null) {
                Spacer(GlanceModifier.height(6.dp))
                Text(
                    translation,
                    maxLines = 3,
                    style = TextStyle(color = ColorProvider(Color(0xD9FAF7F0)), fontSize = 13.sp, fontFamily = FontFamily.Serif),
                    modifier = GlanceModifier.fillMaxWidth(),
                )
            }
            Spacer(GlanceModifier.height(6.dp))
            Text(
                reference,
                style = TextStyle(color = ColorProvider(Color(0xFFE9CF8F)), fontSize = 11.sp, fontWeight = FontWeight.Medium),
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
    }

    companion object {
        suspend fun refresh(context: Context) {
            runCatching {
                if (GlanceAppWidgetManager(context).getGlanceIds(VerseWidget::class.java).isNotEmpty()) {
                    VerseWidget().updateAll(context)
                }
            }
        }
    }
}

class VerseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VerseWidget()
}
