package com.quranforwomen.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * The Quran in Latin letters, shown under the Arabic when she turns it on.
 * Downloaded once from AlQuran Cloud, checked like a translation, and kept for offline reading.
 */
class TransliterationStore(private val context: Context, private val prefs: Prefs, private val scope: CoroutineScope) {
    var state by mutableStateOf(
        if (file().exists()) TranslationStore.State.Ready else TranslationStore.State.NotDownloaded
    ); private set

    /** Bumped when the text finishes loading, so screens showing it redraw. */
    var loadedVersion by mutableIntStateOf(0); private set

    private var cache: List<List<String>>? = null
    private var loading = false

    private var showState by mutableStateOf(prefs.sp.getBoolean(PlaybackPrefs.showQuranTransliteration, false))

    /** Off by default. Turning it on fetches the text the first time. */
    var show: Boolean
        get() = showState
        set(value) {
            showState = value
            prefs.sp.edit().putBoolean(PlaybackPrefs.showQuranTransliteration, value).apply()
            if (value) scope.launch { prepare() }
        }

    /** Transliteration of one verse, if downloaded and loaded. */
    fun text(ref: VerseRef): String? {
        loadedVersion // read so callers redraw when the text loads
        if (state != TranslationStore.State.Ready) return null
        val verses = cache
        if (verses == null) {
            scope.launch { prepare() }
            return null
        }
        return verses.getOrNull(ref.surah - 1)?.getOrNull(ref.ayah - 1)
    }

    /** Loads the text into memory, downloading it if needed. */
    suspend fun prepare() {
        when (state) {
            TranslationStore.State.Ready -> {
                if (cache != null || loading) return
                loading = true
                val loaded = withContext(Dispatchers.IO) { readFile() }
                loading = false
                if (loaded != null) {
                    cache = loaded
                    loadedVersion++
                } else {
                    state = TranslationStore.State.NotDownloaded
                    download()
                }
            }
            TranslationStore.State.NotDownloaded, TranslationStore.State.Failed -> download()
            TranslationStore.State.Downloading -> Unit
        }
    }

    fun downloadAsync() { scope.launch { download() } }

    suspend fun download() {
        if (state == TranslationStore.State.Downloading) return
        state = TranslationStore.State.Downloading
        try {
            val verses = withContext(Dispatchers.IO) {
                val connection = (URL("https://api.alquran.cloud/v1/quran/$EDITION").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30_000
                    readTimeout = 60_000
                }
                try {
                    if (connection.responseCode != 200) error("Bad response")
                    val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    // The same check as translations: all 114 surahs and all 6236 verses.
                    TranslationStore.parse(body).also { grid ->
                        val target = file()
                        target.parentFile?.mkdirs()
                        val tmp = File(target.parentFile, "$EDITION.tmp")
                        tmp.writeText(AppJson.encodeToString(Grid, grid), Charsets.UTF_8)
                        if (!tmp.renameTo(target)) { target.delete(); tmp.renameTo(target) }
                    }
                } finally {
                    connection.disconnect()
                }
            }
            cache = verses
            state = TranslationStore.State.Ready
            loadedVersion++
        } catch (e: Exception) {
            state = TranslationStore.State.Failed
        }
    }

    fun delete() {
        file().delete()
        cache = null
        state = TranslationStore.State.NotDownloaded
        loadedVersion++
    }

    fun sizeOnDisk(): Long? = file().takeIf { it.exists() }?.length()

    private fun readFile(): List<List<String>>? = runCatching {
        AppJson.decodeFromString(Grid, file().readText(Charsets.UTF_8))
    }.getOrNull()

    /** Stored beside the translations, in no-backup storage. */
    private fun file() = File(File(context.noBackupFilesDir, "translations"), "$EDITION.json")

    companion object {
        const val EDITION = "en.transliteration"
        private val Grid = ListSerializer(ListSerializer(String.serializer()))
    }
}
