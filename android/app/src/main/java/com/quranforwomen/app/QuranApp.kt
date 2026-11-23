package com.quranforwomen.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.quranforwomen.app.data.AudioPlayer
import com.quranforwomen.app.data.ContentStore
import com.quranforwomen.app.data.JournalStore
import com.quranforwomen.app.data.LibraryStore
import com.quranforwomen.app.data.Notifications
import com.quranforwomen.app.data.PrayerStore
import com.quranforwomen.app.data.Prefs
import com.quranforwomen.app.data.QuranStore
import com.quranforwomen.app.data.TranslationStore
import com.quranforwomen.app.nav.AppRouter
import kotlinx.coroutines.MainScope

/**
 * Every store, made once for the whole app (manual dependency injection).
 * Screens read it with LocalGraph.current; receivers and services with AppGraph.get(context).
 */
class AppGraph(context: Context) {
    val scope = MainScope()
    val prefs = Prefs(context)
    /** The Quran text is the largest file; it starts loading on a background thread at launch. */
    val quran by lazy { QuranStore(context) }
    val content by lazy { ContentStore(context) }
    val library = LibraryStore(context)
    val translations = TranslationStore(context, prefs, scope)
    val player by lazy { AudioPlayer(context, quran, prefs, scope) }
    val transliteration = com.quranforwomen.app.data.TransliterationStore(context, prefs, scope)
    val words = com.quranforwomen.app.data.WordStore(context, prefs, scope)
    val tafsir = com.quranforwomen.app.data.TafsirStore(context, prefs, scope)
    val hifz = com.quranforwomen.app.data.HifzStore(prefs)
    val recitations by lazy { com.quranforwomen.app.data.RecitationDownloads(context, quran, scope) }
    val prayer = PrayerStore(context, scope)
    val fasting = com.quranforwomen.app.data.FastingStore(context, prayer)
    val missedPrayers = com.quranforwomen.app.data.MissedPrayersStore(context)
    val journal = JournalStore(context, prefs, scope)
    val planHistory = com.quranforwomen.app.data.PlanHistoryStore(context)
    val gentleReminders = com.quranforwomen.app.data.GentleReminderPrefs(context)
    val dhikrExtras = com.quranforwomen.app.data.DhikrStore(context)
    val challenges = com.quranforwomen.app.data.ChallengeStore(context)
    val guidance by lazy { com.quranforwomen.app.data.GuidanceStore(context) }
    val router = AppRouter()
    /** Done once the Quran text and the duas are loaded. The launch screen waits for it. */
    val contentReady = kotlinx.coroutines.CompletableDeferred<Unit>()

    init {
        prayer.isResting = library.isResting
        library.onRestChanged = { resting -> prayer.isResting = resting }
    }

    companion object {
        @Volatile private var instance: AppGraph? = null

        fun get(context: Context): AppGraph =
            instance ?: synchronized(this) {
                instance ?: AppGraph(context.applicationContext).also { instance = it }
            }
    }
}

val LocalGraph = staticCompositionLocalOf<AppGraph> { error("No app graph") }

class QuranApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        // Before the first unlock after a restart only the small prayer copy can be read;
        // the rest of the app waits until she unlocks.
        if (getSystemService(android.os.UserManager::class.java)?.isUserUnlocked == false) return
        val graph = AppGraph.get(this)
        // Read the bundled Quran and duas while the splash plays, off the main thread.
        Thread({
            graph.quran
            graph.content
            graph.contentReady.complete(Unit)
        }, "content-preload").apply { priority = Thread.MAX_PRIORITY }.start()
    }
}
