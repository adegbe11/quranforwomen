package com.quranforwomen.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class TranslationOption(
    /** Edition identifier on api.alquran.cloud. */
    val id: String,
    val language: String,
    val nativeName: String,
    val translator: String,
    val isRightToLeft: Boolean,
    /** ISO language code used to suggest a default. */
    val code: String,
) {
    companion object {
        val all = listOf(
            TranslationOption("en.sahih", "English", "English", "Saheeh International", false, "en"),
            TranslationOption("ur.junagarhi", "Urdu", "اردو", "Muhammad Junagarhi", true, "ur"),
            TranslationOption("id.indonesian", "Indonesian", "Bahasa Indonesia", "Indonesian Ministry of Religious Affairs", false, "id"),
            TranslationOption("ms.basmeih", "Malay", "Bahasa Melayu", "Abdullah Muhammad Basmeih", false, "ms"),
            TranslationOption("tr.diyanet", "Turkish", "Türkçe", "Diyanet İşleri", false, "tr"),
            TranslationOption("fr.hamidullah", "French", "Français", "Muhammad Hamidullah", false, "fr"),
            TranslationOption("bn.bengali", "Bengali", "বাংলা", "Muhiuddin Khan", false, "bn"),
            TranslationOption("es.cortes", "Spanish", "Español", "Julio Cortés", false, "es"),
            TranslationOption("de.bubenheim", "German", "Deutsch", "Bubenheim and Elyas", false, "de"),
            TranslationOption("ru.kuliev", "Russian", "Русский", "Elmir Kuliev", false, "ru"),
            TranslationOption("ha.gumi", "Hausa", "Hausa", "Abubakar Mahmoud Gumi", false, "ha"),
            TranslationOption("sw.barwani", "Swahili", "Kiswahili", "Ali Muhsin Al-Barwani", false, "sw"),
            TranslationOption("so.abduh", "Somali", "Soomaali", "Mahmud Muhammad Abduh", false, "so"),
        )

        fun suggested(locale: Locale = Locale.getDefault()): TranslationOption {
            // Java still reports Indonesian as "in" on some phones.
            val code = locale.language.let { if (it == "in") "id" else it }
            return all.firstOrNull { it.code == code } ?: all[0]
        }
    }
}

/** Downloads a full translation once, checks it, and keeps it for offline reading. */
class TranslationStore(private val context: Context, private val prefs: Prefs, private val scope: CoroutineScope) {
    enum class State { NotDownloaded, Downloading, Ready, Failed }

    val states = mutableStateMapOf<String, State>()
    private val cache = HashMap<String, List<List<String>>>()
    /** Bumped when a translation finishes loading, so screens showing text redraw. */
    var loadedVersion by mutableStateOf(0); private set

    private var selectedIdState by mutableStateOf(
        prefs.sp.getString(PrefKey.translation, null) ?: TranslationOption.suggested().id
    )

    var selectedID: String
        get() = selectedIdState
        set(value) {
            selectedIdState = value
            prefs.sp.edit().putString(PrefKey.translation, value).apply()
            scope.launch { prepareSelected() }
        }

    init {
        for (option in TranslationOption.all) {
            states[option.id] = if (fileFor(option.id).exists()) State.Ready else State.NotDownloaded
        }
    }

    val selected: TranslationOption get() = TranslationOption.all.firstOrNull { it.id == selectedID } ?: TranslationOption.all[0]
    val selectedState: State get() = state(selectedID)
    fun state(id: String): State = states[id] ?: State.NotDownloaded

    /** Translation of one verse in the selected language, if downloaded. */
    fun text(ref: VerseRef): String? {
        loadedVersion // read so callers redraw when a translation loads
        val verses = versesFor(selectedID) ?: return null
        return verses.getOrNull(ref.surah - 1)?.getOrNull(ref.ayah - 1)
    }

    /** The whole selected translation, used by search. */
    fun selectedVerses(): List<List<String>>? = versesFor(selectedID)

    /** Loads the selected translation into memory, downloading it if needed. */
    suspend fun prepareSelected() {
        val id = selectedID
        when (state(id)) {
            State.Ready -> if (cache[id] == null) {
                val loaded = withContext(Dispatchers.IO) { readFile(id) }
                if (loaded != null) {
                    cache[id] = loaded
                    loadedVersion++
                } else {
                    states[id] = State.NotDownloaded
                    download(id)
                }
            }
            State.NotDownloaded, State.Failed -> download(id)
            State.Downloading -> Unit
        }
    }

    fun downloadAsync(id: String) { scope.launch { download(id) } }

    suspend fun download(id: String) {
        if (state(id) == State.Downloading) return
        states[id] = State.Downloading
        try {
            val verses = withContext(Dispatchers.IO) {
                val connection = (URL("https://api.alquran.cloud/v1/quran/$id").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30_000
                    readTimeout = 60_000
                }
                try {
                    if (connection.responseCode != 200) error("Bad response")
                    val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    parse(body).also { file ->
                        val target = fileFor(id)
                        target.parentFile?.mkdirs()
                        val tmp = File(target.parentFile, "$id.tmp")
                        tmp.writeText(AppJson.encodeToString(Grid, file), Charsets.UTF_8)
                        if (!tmp.renameTo(target)) { target.delete(); tmp.renameTo(target) }
                    }
                } finally {
                    connection.disconnect()
                }
            }
            cache[id] = verses
            states[id] = State.Ready
            loadedVersion++
        } catch (e: Exception) {
            states[id] = State.Failed
        }
    }

    fun delete(id: String) {
        fileFor(id).delete()
        cache.remove(id)
        states[id] = State.NotDownloaded
        loadedVersion++
    }

    fun sizeOnDisk(id: String): Long? = fileFor(id).takeIf { it.exists() }?.length()

    private fun versesFor(id: String): List<List<String>>? {
        if (state(id) != State.Ready) return null
        cache[id]?.let { return it }
        val loaded = readFile(id) ?: return null
        cache[id] = loaded
        return loaded
    }

    private fun readFile(id: String): List<List<String>>? = runCatching {
        AppJson.decodeFromString(Grid, fileFor(id).readText(Charsets.UTF_8))
    }.getOrNull()

    /** Translations re-download, so they live in no-backup storage. */
    private fun fileFor(id: String) = File(File(context.noBackupFilesDir, "translations"), "$id.json")

    @Serializable
    private data class Response(val data: Body) {
        @Serializable data class Body(val surahs: List<SurahText>)
        @Serializable data class SurahText(val ayahs: List<Ayah>)
        @Serializable data class Ayah(val text: String)
    }

    companion object {
        private val Grid = ListSerializer(ListSerializer(String.serializer()))

        /** Accepts a download only if it has all 114 surahs and all 6236 verses. */
        fun parse(json: String): List<List<String>> {
            val response = AppJson.decodeFromString(Response.serializer(), json)
            val verses = response.data.surahs.map { s -> s.ayahs.map { it.text.trim() } }
            require(verses.size == 114 && verses.sumOf { it.size } == 6236) { "Incomplete translation" }
            return verses
        }
    }
}
