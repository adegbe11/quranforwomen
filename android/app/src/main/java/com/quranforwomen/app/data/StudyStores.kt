package com.quranforwomen.app.data

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Study data comes from the Quran.com API, one surah per request, and is kept on the phone for offline reading. */
private const val API = "https://api.quran.com/api/v4"

private fun fetch(url: String): String {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 30_000
        readTimeout = 60_000
        setRequestProperty("Accept", "application/json")
    }
    try {
        if (connection.responseCode != 200) error("Bad response ${connection.responseCode}")
        return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

private fun writeAtomically(target: File, text: String) {
    target.parentFile?.mkdirs()
    val tmp = File(target.parentFile, target.name + ".tmp")
    tmp.writeText(text, Charsets.UTF_8)
    if (!tmp.renameTo(target)) { target.delete(); tmp.renameTo(target) }
}

enum class StudyState { Missing, Loading, Ready, Failed }

// Word by word

/** One Arabic word with its meaning, how it sounds, and a short recording of it. */
@Serializable
data class QuranWord(val arabic: String, val meaning: String, val sound: String, val audio: String)

enum class WordLanguage(val code: String, val title: String, val rightToLeft: Boolean) {
    English("en", "English", false), Urdu("ur", "Urdu", true);
}

/**
 * Word by word meanings. Under each Arabic word she sees what it means;
 * tapping a word says it aloud.
 */
class WordStore(private val context: Context, private val prefs: Prefs, private val scope: CoroutineScope) {
    private var showState by mutableStateOf(prefs.sp.getBoolean(KEY_SHOW, false))
    private var languageState by mutableStateOf(
        WordLanguage.entries.firstOrNull { it.code == prefs.sp.getString(KEY_LANGUAGE, "en") } ?: WordLanguage.English
    )

    var show: Boolean
        get() = showState
        set(value) {
            showState = value
            prefs.sp.edit().putBoolean(KEY_SHOW, value).apply()
        }

    var language: WordLanguage
        get() = languageState
        set(value) {
            languageState = value
            prefs.sp.edit().putString(KEY_LANGUAGE, value.code).apply()
        }

    private val states = mutableStateMapOf<String, StudyState>()
    private val cache = HashMap<String, List<List<QuranWord>>>()
    var loadedVersion by mutableIntStateOf(0); private set

    private fun key(surah: Int) = "${language.code}/$surah"

    fun state(surah: Int): StudyState = states[key(surah)] ?: if (file(key(surah)).exists()) StudyState.Ready else StudyState.Missing

    /** The words of every verse in a surah, or null while they load. */
    fun words(surah: Int): List<List<QuranWord>>? {
        loadedVersion // read so callers redraw when a surah loads
        val k = key(surah)
        cache[k]?.let { return it }
        if (states[k] != StudyState.Loading && states[k] != StudyState.Failed) scope.launch { load(surah) }
        return null
    }

    fun retry(surah: Int) {
        states.remove(key(surah))
        scope.launch { load(surah) }
    }

    suspend fun load(surah: Int) {
        val k = key(surah)
        if (cache.containsKey(k) || states[k] == StudyState.Loading) return
        states[k] = StudyState.Loading
        val lang = language.code
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val f = file(k)
                if (f.exists()) {
                    runCatching { AppJson.decodeFromString(Grid, f.readText(Charsets.UTF_8)) }.getOrNull()?.let { return@runCatching it }
                }
                val body = fetch("$API/verses/by_chapter/$surah?words=true&word_fields=text_uthmani&language=$lang&per_page=300&fields=verse_key")
                parse(body).also { writeAtomically(f, AppJson.encodeToString(Grid, it)) }
            }.getOrNull()
        }
        if (result != null) {
            cache[k] = result
            states[k] = StudyState.Ready
            loadedVersion++
        } else {
            states[k] = StudyState.Failed
        }
    }

    // A short player of its own, so saying one word never stops the recitation queue.
    private var wordPlayer: MediaPlayer? = null

    fun say(word: QuranWord) {
        if (word.audio.isBlank()) return
        runCatching {
            wordPlayer?.release()
            wordPlayer = MediaPlayer().apply {
                setDataSource("https://audio.qurancdn.com/${word.audio}")
                setOnPreparedListener { it.start() }
                setOnCompletionListener { it.release(); if (wordPlayer === it) wordPlayer = null }
                setOnErrorListener { mp, _, _ -> mp.release(); if (wordPlayer === mp) wordPlayer = null; true }
                prepareAsync()
            }
        }
    }

    fun sizeOnDisk(): Long = dir().listFiles()?.sumOf { d -> d.listFiles()?.sumOf { it.length() } ?: 0L } ?: 0L

    fun deleteAll() {
        dir().deleteRecursively()
        cache.clear()
        states.clear()
        loadedVersion++
    }

    private fun dir() = File(context.noBackupFilesDir, "words")
    private fun file(k: String) = File(dir(), "$k.json")

    companion object {
        private const val KEY_SHOW = "reader.wordByWord"
        private const val KEY_LANGUAGE = "reader.wordLanguage"
        private val Grid = ListSerializer(ListSerializer(QuranWord.serializer()))

        /** Verses in order, each a list of real words. The verse number marker at the end is left out. */
        internal fun parse(body: String): List<List<QuranWord>> {
            val root = AppJson.parseToJsonElement(body).jsonObject
            val verses = root["verses"]!!.jsonArray.map { it.jsonObject }.sortedBy { it["verse_number"]!!.jsonPrimitive.int }
            val grid = verses.map { v ->
                v["words"]!!.jsonArray.map { it.jsonObject }
                    .filter { it["char_type_name"]?.jsonPrimitive?.contentOrNull == "word" }
                    .map { w ->
                        QuranWord(
                            arabic = w["text_uthmani"]?.jsonPrimitive?.contentOrNull ?: w["text"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                            meaning = (w["translation"] as? JsonObject)?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty(),
                            sound = (w["transliteration"] as? JsonObject)?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty(),
                            audio = w["audio_url"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        )
                    }
            }
            require(grid.isNotEmpty() && grid.all { it.isNotEmpty() && it.all { w -> w.arabic.isNotBlank() } })
            return grid
        }
    }
}

// Tafsir

/** A tafsir that Quran.com publishes. Only well known, published works are offered. */
enum class TafsirEdition(val id: Int, val title: String, val author: String, val language: String, val rightToLeft: Boolean) {
    IbnKathir(169, "Ibn Kathir (Abridged)", "Hafiz Ibn Kathir", "English", false),
    MaarifulQuran(168, "Ma'arif al-Qur'an", "Mufti Muhammad Shafi", "English", false),
    IbnKathirUrdu(160, "Tafsir Ibn Kathir", "Hafiz Ibn Kathir", "Urdu", true),
    Muyassar(16, "Tafsir al-Muyassar", "King Fahd Complex", "Arabic", true),
    Saadi(91, "Tafsir al-Sa'di", "Abd al-Rahman al-Sa'di", "Arabic", true);
}

/** The explanation shown for a verse. Some tafsirs explain several verses together. */
data class TafsirEntry(val html: String, val firstAyah: Int, val lastAyah: Int)

class TafsirStore(private val context: Context, private val prefs: Prefs, private val scope: CoroutineScope) {
    private var editionState by mutableStateOf(
        TafsirEdition.entries.firstOrNull { it.id == prefs.sp.getInt(KEY_EDITION, TafsirEdition.IbnKathir.id) } ?: TafsirEdition.IbnKathir
    )

    var edition: TafsirEdition
        get() = editionState
        set(value) {
            editionState = value
            prefs.sp.edit().putInt(KEY_EDITION, value.id).apply()
        }

    private val states = mutableStateMapOf<String, StudyState>()
    private val cache = HashMap<String, List<String>>()
    var loadedVersion by mutableIntStateOf(0); private set

    private fun key(surah: Int) = "${edition.id}/$surah"

    fun state(surah: Int): StudyState = states[key(surah)] ?: StudyState.Missing

    /** The tafsir for one verse, or null while it loads. */
    fun entry(ref: VerseRef): TafsirEntry? {
        loadedVersion
        val k = key(ref.surah)
        val texts = cache[k]
        if (texts == null) {
            if (states[k] != StudyState.Loading && states[k] != StudyState.Failed) scope.launch { load(ref.surah) }
            return null
        }
        return group(texts, ref.ayah)
    }

    fun retry(surah: Int) {
        states.remove(key(surah))
        scope.launch { load(surah) }
    }

    suspend fun load(surah: Int) {
        val k = key(surah)
        if (cache.containsKey(k) || states[k] == StudyState.Loading) return
        states[k] = StudyState.Loading
        val id = edition.id
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val f = file(k)
                if (f.exists()) {
                    runCatching { AppJson.decodeFromString(Texts, f.readText(Charsets.UTF_8)) }.getOrNull()?.let { return@runCatching it }
                }
                val body = fetch("$API/tafsirs/$id/by_chapter/$surah?per_page=300")
                parse(body).also { writeAtomically(f, AppJson.encodeToString(Texts, it)) }
            }.getOrNull()
        }
        if (result != null) {
            cache[k] = result
            states[k] = StudyState.Ready
            loadedVersion++
        } else {
            states[k] = StudyState.Failed
        }
    }

    fun sizeOnDisk(): Long = dir().walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun deleteAll() {
        dir().deleteRecursively()
        cache.clear()
        states.clear()
        loadedVersion++
    }

    private fun dir() = File(context.noBackupFilesDir, "tafsir")
    private fun file(k: String) = File(dir(), "$k.json")

    companion object {
        private const val KEY_EDITION = "reader.tafsirEdition"
        private val Texts = ListSerializer(String.serializer())

        /** One text per verse, in order. A verse explained with the ones before it has an empty text. */
        internal fun parse(body: String): List<String> {
            val root = AppJson.parseToJsonElement(body).jsonObject
            val rows = (root["tafsirs"] as JsonArray).map { it.jsonObject }
            val byAyah = rows.associate { row ->
                val key = row["verse_key"]!!.jsonPrimitive.contentOrNull.orEmpty()
                key.substringAfter(':').toInt() to (row["text"]?.jsonPrimitive?.contentOrNull.orEmpty())
            }
            val count = byAyah.keys.maxOrNull() ?: 0
            require(count > 0 && byAyah.values.any { it.isNotBlank() })
            return (1..count).map { byAyah[it].orEmpty() }
        }

        /** Finds the text that covers this verse and the verses it covers. */
        internal fun group(texts: List<String>, ayah: Int): TafsirEntry? {
            if (texts.isEmpty()) return null
            var start = (ayah - 1).coerceIn(0, texts.lastIndex)
            while (start > 0 && texts[start].isBlank()) start--
            if (texts[start].isBlank()) return null
            var end = start
            while (end + 1 <= texts.lastIndex && texts[end + 1].isBlank()) end++
            return TafsirEntry(texts[start], start + 1, end + 1)
        }
    }
}

// Memorizing

/** How much of each verse is hidden while she tests herself. */
enum class HideLevel(val title: String) {
    Off("Show all"), Some("Hide some words"), FirstWord("Show first word only"), All("Hide all");

    /** Whether the word at this position starts hidden. */
    fun hides(index: Int): Boolean = when (this) {
        Off -> false
        Some -> index % 2 == 1
        FirstWord -> index > 0
        All -> true
    }
}

/**
 * Memorizing mode: words hide so she can recite from memory and tap to check herself,
 * and the verses she has learned are kept.
 */
class HifzStore(private val prefs: Prefs) {
    private var levelState by mutableStateOf(
        HideLevel.entries.firstOrNull { it.name == prefs.sp.getString(KEY_LEVEL, HideLevel.Off.name) } ?: HideLevel.Off
    )
    private var learnedState by mutableStateOf(prefs.sp.getStringSet(KEY_LEARNED, emptySet())!!.toSet())

    var level: HideLevel
        get() = levelState
        set(value) {
            levelState = value
            prefs.sp.edit().putString(KEY_LEVEL, value.name).apply()
        }

    val isOn: Boolean get() = levelState != HideLevel.Off

    fun isLearned(ref: VerseRef): Boolean = ref.id in learnedState

    fun learnedCount(surah: Int): Int = learnedState.count { it.substringBefore(':') == surah.toString() }

    fun totalLearned(): Int = learnedState.size

    fun toggleLearned(ref: VerseRef) {
        val next = if (ref.id in learnedState) learnedState - ref.id else learnedState + ref.id
        learnedState = next
        prefs.sp.edit().putStringSet(KEY_LEARNED, next).apply()
    }

    companion object {
        private const val KEY_LEVEL = "hifz.level"
        private const val KEY_LEARNED = "hifz.learned"

        private fun isMarkOnly(token: String) = token.isNotEmpty() && token.all { it.code in 0x06D6..0x06ED || it.code == 0x06DE || it.code == 0x06E9 }

        /**
         * Splits a verse into the words she recites. The small pause marks belong to the word before them;
         * a mark at the very start (the rub el hizb star) goes with the first word.
         */
        fun splitWords(text: String): List<String> {
            val out = mutableListOf<String>()
            var pending = ""
            for (token in text.split(' ').filter { it.isNotBlank() }) {
                if (isMarkOnly(token)) {
                    if (out.isEmpty()) pending += "$token " else out[out.lastIndex] = out.last() + " " + token
                } else {
                    out += pending + token
                    pending = ""
                }
            }
            if (pending.isNotBlank()) out += pending.trim()
            return out
        }
    }
}
