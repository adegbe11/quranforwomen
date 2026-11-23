package com.quranforwomen.app.data

import kotlinx.serialization.Serializable
import java.util.UUID

/** A reference to one verse, for example 2:255. */
@Serializable
data class VerseRef(val surah: Int, val ayah: Int) : Comparable<VerseRef> {
    val id: String get() = "$surah:$ayah"

    override fun compareTo(other: VerseRef): Int =
        compareValuesBy(this, other, { it.surah }, { it.ayah })

    companion object {
        /** Parses "2:255". */
        fun parse(string: String): VerseRef? {
            val parts = string.split(":").mapNotNull { it.trim().toIntOrNull() }
            return if (parts.size == 2) VerseRef(parts[0], parts[1]) else null
        }
    }
}

@Serializable
data class Surah(
    val id: Int,
    val arabicName: String,
    /** Transliterated name, for example "Al-Baqarah". */
    val name: String,
    /** English meaning, for example "The Cow". */
    val meaning: String,
    val revelation: String,
    val sajdas: List<Int> = emptyList(),
    /** Madani mushaf page for each verse. */
    val pages: List<Int>,
    /** Verified Uthmani text, one entry per verse. Never edited. */
    val ayahs: List<String>,
) {
    val verseCount: Int get() = ayahs.size
    /** Al-Fatihah counts the Basmala as verse 1, and At-Tawbah has none. */
    val showsBasmala: Boolean get() = id != 1 && id != 9
    val revelationPlace: String get() = if (revelation == "Meccan") "Makkah" else "Madinah"

    override fun equals(other: Any?): Boolean = other is Surah && other.id == id
    override fun hashCode(): Int = id
}

/** A curated set of whole verses, for a feeling or a topic. */
@Serializable
data class VerseCollection(
    val id: String,
    val title: String,
    val symbol: String,
    val intro: String? = null,
    val verses: List<String>,
) {
    val refs: List<VerseRef> get() = verses.mapNotNull(VerseRef::parse)
}

@Serializable
enum class HighlightColor {
    emerald, blush, gold, sky;

    val title: String get() = name.replaceFirstChar { it.uppercase() }
}

@Serializable
data class VerseNote(val text: String, val updated: Long = System.currentTimeMillis())

@Serializable
data class DuaCategory(val id: String, val title: String, val symbol: String)

@Serializable
data class Dua(
    val id: String,
    val category: String,
    val title: String,
    val kind: Kind,
    /** "Quran 2:250" or a hadith collection and number. */
    val source: String,
    val arabic: String,
    val transliteration: String? = null,
    val translation: String,
    val note: String? = null,
    val verses: VerseRange? = null,
    val wholeVerses: Boolean? = null,
) {
    @Serializable
    enum class Kind { quran, hadith }

    @Serializable
    data class VerseRange(val surah: Int, val from: Int, val to: Int)

    /** Recitation is only offered when the dua is made of complete verses. */
    val playableRange: VerseRange? get() = if (wholeVerses == true) verses else null
    val sourceKind: SourceKind get() = if (kind == Kind.quran) SourceKind.Quran else SourceKind.Hadith
    val kindTitle: String get() = if (kind == Kind.quran) "From the Quran" else "From the Sunnah"
}

/** Where a text comes from. Quran and Sunnah look different on purpose. */
enum class SourceKind { Quran, Hadith, Personal }

@Serializable
data class DhikrPhrase(
    val id: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val target: Int,
    val source: String,
)

/** A prayer she writes in her own words. Kept apart from sourced duas. */
@Serializable
data class PersonalDua(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val created: Long = System.currentTimeMillis(),
)
