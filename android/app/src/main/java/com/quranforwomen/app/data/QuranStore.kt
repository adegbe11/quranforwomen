package com.quranforwomen.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

val AppJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
    encodeDefaults = true
}

/** The bundled, verified Quran text (Tanzil Uthmani). Read only. Works fully offline. */
class QuranStore(context: Context) {
    @Serializable
    private data class Payload(
        val basmala: String,
        val juzStarts: List<VerseRef>,
        val surahs: List<Surah>,
        val hizbQuarterStarts: List<VerseRef> = emptyList(),
    )

    val surahs: List<Surah>
    val basmala: String
    /** First verse of each of the 30 juz. */
    val juzStarts: List<VerseRef>
    /** First verse of each of the 240 hizb quarters (4 per hizb, 60 hizbs). */
    val hizbQuarterStarts: List<VerseRef>
    /** The 60 hizbs, each as its four quarter starts. Empty if the bundled data has no hizb index. */
    val hizbs: List<List<VerseRef>> by lazy {
        if (hizbQuarterStarts.size == 240) hizbQuarterStarts.chunked(4) else emptyList()
    }
    val totalVerses: Int
    private val offsets: IntArray

    @Volatile private var normalizedCache: List<List<String>>? = null

    init {
        val text = context.assets.open("quran-uthmani.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val payload = AppJson.decodeFromString(Payload.serializer(), text)
        check(payload.surahs.size == 114) { "The bundled Quran text is missing or damaged." }
        surahs = payload.surahs
        basmala = payload.basmala
        juzStarts = payload.juzStarts
        hizbQuarterStarts = payload.hizbQuarterStarts
        var running = 0
        offsets = IntArray(114)
        payload.surahs.forEachIndexed { i, s ->
            offsets[i] = running
            running += s.verseCount
        }
        totalVerses = running
    }

    fun surah(id: Int): Surah = surahs[id.coerceIn(1, 114) - 1]

    fun contains(ref: VerseRef): Boolean =
        ref.surah in 1..114 && ref.ayah in 1..surah(ref.surah).verseCount

    fun text(ref: VerseRef): String = if (contains(ref)) surah(ref.surah).ayahs[ref.ayah - 1] else ""

    /** Position of a verse in the whole Quran, 1 to 6236. Used for audio files. */
    fun globalNumber(ref: VerseRef): Int {
        val s = ref.surah.coerceIn(1, 114)
        return offsets[s - 1] + ref.ayah.coerceIn(1, surah(s).verseCount)
    }

    /** The verse at a position in the whole Quran, 1 to 6236. */
    fun refAtGlobal(number: Int): VerseRef {
        val n = number.coerceIn(1, totalVerses)
        var s = 1
        while (s < 114 && offsets[s] < n) s++
        return VerseRef(s, n - offsets[s - 1])
    }

    /** Juz number (1 to 30) that contains a verse. */
    fun juz(ref: VerseRef): Int = juzStarts.indexOfLast { it <= ref }.coerceAtLeast(0) + 1

    fun page(ref: VerseRef): Int = surah(ref.surah).pages[(ref.ayah - 1).coerceIn(0, surah(ref.surah).verseCount - 1)]

    fun displayName(ref: VerseRef): String = "${surah(ref.surah).name} · ${ref.surah}:${ref.ayah}"

    /** Diacritic free text for search. Built once, off the main thread. */
    suspend fun normalizedVerses(): List<List<String>> {
        normalizedCache?.let { return it }
        val result = withContext(Dispatchers.Default) {
            surahs.map { s -> s.ayahs.map(ArabicSearch::normalize) }
        }
        normalizedCache = result
        return result
    }
}

/** Duas, dhikr phrases and the daily verse list. All bundled and sourced. */
class ContentStore(context: Context) {
    @Serializable
    private data class Payload(
        val categories: List<DuaCategory>,
        val duas: List<Dua>,
        val dhikr: List<DhikrPhrase>,
        val dailyVerses: List<String>,
        val feelings: List<VerseCollection>,
        val topics: List<VerseCollection>,
    )

    val categories: List<DuaCategory>
    val duas: List<Dua>
    val dhikr: List<DhikrPhrase>
    val dailyVerses: List<VerseRef>
    val feelings: List<VerseCollection>
    val topics: List<VerseCollection>

    init {
        val text = context.assets.open("content.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val p = AppJson.decodeFromString(Payload.serializer(), text)
        categories = p.categories
        duas = p.duas
        dhikr = p.dhikr
        dailyVerses = p.dailyVerses.mapNotNull(VerseRef::parse)
        feelings = p.feelings
        topics = p.topics
    }

    fun collection(id: String): VerseCollection? = (feelings + topics).firstOrNull { it.id == id }
    fun duasIn(category: DuaCategory): List<Dua> = duas.filter { it.category == category.id }
    fun dua(id: String): Dua? = duas.firstOrNull { it.id == id }
    fun category(id: String): DuaCategory? = categories.firstOrNull { it.id == id }
    fun dhikr(id: String): DhikrPhrase? = dhikr.firstOrNull { it.id == id }

    fun verseOfTheDay(date: LocalDate = LocalDate.now()): VerseRef =
        dailyVerses[Math.floorMod(dayNumber(date), dailyVerses.size)]

    fun duaOfTheDay(date: LocalDate = LocalDate.now()): Dua =
        duas[Math.floorMod(dayNumber(date) * 7, duas.size)]

    companion object {
        /** Index used by both the app and the widget to pick today's verse. */
        fun dayNumber(date: LocalDate = LocalDate.now()): Int = date.toEpochDay().toInt()
    }
}

object ArabicSearch {
    /** Removes diacritics and Quranic marks and folds letter variants, so a plain
     *  search like "الرحمن" finds "ٱلرَّحْمَٰنِ". */
    fun normalize(text: String): String {
        val out = StringBuilder(text.length)
        for (ch in text) {
            val v = ch.code
            if (v in 0x0610..0x061A || v in 0x064B..0x065F || v == 0x0670 || v in 0x06D6..0x06ED || v == 0x0640) continue
            when (v) {
                0x0671, 0x0623, 0x0625, 0x0622 -> out.append('ا')
                0x0649 -> out.append('ي')
                0x0629 -> out.append('ه')
                else -> out.append(ch)
            }
        }
        return out.toString()
    }

    fun containsArabic(text: String): Boolean = text.any { it.code in 0x0600..0x06FF }
}

/** Arabic Indic digits, used for verse numbers inside Arabic text. */
val Int.arabicIndic: String
    get() = toString().map { c -> if (c.isDigit()) (0x0660 + (c - '0')).toChar() else c }.joinToString("")
