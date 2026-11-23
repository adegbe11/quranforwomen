package com.quranforwomen.app

import com.quranforwomen.app.data.ArabicSearch
import com.quranforwomen.app.data.AsrMethod
import com.quranforwomen.app.data.CalculationMethod
import com.quranforwomen.app.data.LibraryStore
import com.quranforwomen.app.data.Prayer
import com.quranforwomen.app.data.PrayerCalculator
import com.quranforwomen.app.data.ReadingPlan
import com.quranforwomen.app.data.TranslationOption
import com.quranforwomen.app.data.VerseRef
import com.quranforwomen.app.data.arabicIndic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs

class VerseRefTests {
    @Test fun parsesReferences() {
        assertEquals(VerseRef(2, 255), VerseRef.parse("2:255"))
        assertNull(VerseRef.parse("2"))
        assertNull(VerseRef.parse("a:b"))
        assertTrue(VerseRef(2, 1) < VerseRef(2, 2))
        assertTrue(VerseRef(1, 7) < VerseRef(2, 1))
    }

    @Test fun arabicDigits() {
        assertEquals("٢٥٥", 255.arabicIndic)
    }
}

class SearchTests {
    @Test fun normalizeFoldsVariantsAndMarks() {
        assertEquals("الرحمن", ArabicSearch.normalize("ٱلرَّحْمَٰنِ"))
        assertEquals("اله", ArabicSearch.normalize("إِلَٰه"))
        assertTrue(ArabicSearch.containsArabic("Allah الله"))
    }
}

class PrayerTests {
    private val makkah = ZoneId.of("Asia/Riyadh")

    private fun minutes(t: java.time.ZonedDateTime) = t.withZoneSameInstant(makkah).let { it.hour * 60 + it.minute }

    /** Makkah, 1 June 2025, Umm al-Qura. Published times: Fajr 4:12, Dhuhr 12:19, Asr 15:37, Maghrib 19:02, Isha 20:32. */
    @Test fun makkahTimesMatchTimetable() {
        val s = PrayerCalculator.schedule(LocalDate.of(2025, 6, 1), 21.4225, 39.8262, CalculationMethod.ummAlQura, AsrMethod.standard, makkah)
        val expected = mapOf(
            Prayer.fajr to 4 * 60 + 12, Prayer.dhuhr to 12 * 60 + 19, Prayer.asr to 15 * 60 + 37,
            Prayer.maghrib to 19 * 60 + 2, Prayer.isha to 20 * 60 + 32,
        )
        for ((p, m) in expected) {
            val got = minutes(assertNotNullAndGet(s.time(p)))
            assertTrue("$p was $got, expected about $m", abs(got - m) <= 3)
        }
    }

    @Test fun timesAreInOrder() {
        val s = PrayerCalculator.schedule(LocalDate.of(2026, 1, 15), 51.5074, -0.1278, CalculationMethod.muslimWorldLeague, AsrMethod.hanafi, ZoneId.of("Europe/London"))
        val order = Prayer.entries.map { s.time(it)!! }
        assertEquals(order, order.sorted())
    }

    @Test fun highLatitudeSummerStillHasAllTimes() {
        val s = PrayerCalculator.schedule(LocalDate.of(2026, 6, 21), 59.33, 18.07, CalculationMethod.muslimWorldLeague, AsrMethod.standard, ZoneId.of("Europe/Stockholm"))
        Prayer.entries.forEach { assertNotNull(it.name, s.time(it)) }
    }

    @Test fun qiblaFromLondonPointsSouthEast() {
        val bearing = PrayerCalculator.qiblaBearing(51.5074, -0.1278)
        assertTrue("bearing $bearing", abs(bearing - 118.99) < 1.0)
    }

    @Test fun suggestedMethods() {
        assertEquals(CalculationMethod.isna, CalculationMethod.suggested("us"))
        assertEquals(CalculationMethod.karachi, CalculationMethod.suggested("PK"))
        assertEquals(CalculationMethod.muslimWorldLeague, CalculationMethod.suggested(null))
    }

    private fun <T> assertNotNullAndGet(v: T?): T { assertNotNull(v); return v!! }
}

class PlanTests {
    @Test fun planCoversEveryPageOnce() {
        for (days in listOf(30, 60, 120, 365)) {
            val plan = ReadingPlan(days, 0L)
            val pages = (0 until days).flatMap { plan.pages(it).toList() }
            assertEquals(1, pages.first())
            assertEquals(604, pages.last())
            assertTrue(pages.zipWithNext().all { (a, b) -> b == a + 1 || b == a })
        }
    }

    @Test fun dayKeyUsesWesternDigits() {
        val before = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("ar-EG"))
        try {
            assertEquals("2026-03-05", LibraryStore.dayKey(LocalDate.of(2026, 3, 5)))
        } finally {
            Locale.setDefault(before)
        }
    }
}

class TranslationTests {
    @Test fun suggestsByLanguage() {
        assertEquals("ur.junagarhi", TranslationOption.suggested(Locale.forLanguageTag("ur-PK")).id)
        assertEquals("id.indonesian", TranslationOption.suggested(Locale("in", "ID")).id)
        assertEquals("en.sahih", TranslationOption.suggested(Locale.JAPAN).id)
        assertEquals(13, TranslationOption.all.size)
    }

    @Test fun rejectsIncompleteDownload() {
        val json = """{"data":{"surahs":[{"ayahs":[{"text":"x"}]}]}}"""
        val result = runCatching { TranslationOption.all; com.quranforwomen.app.data.TranslationStore.parse(json) }
        assertTrue(result.isFailure)
    }
}

/** The bundled data the whole app stands on. Read straight from the assets folder. */
class ContentTests {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    private fun asset(name: String) = java.io.File("src/main/assets/$name").readText(Charsets.UTF_8)

    private val surahs by lazy {
        val root = json.parseToJsonElement(asset("quran-uthmani.json")).let { it as kotlinx.serialization.json.JsonObject }
        json.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(com.quranforwomen.app.data.Surah.serializer()), root["surahs"]!!)
    }

    @Test fun quranIsComplete() {
        assertEquals(114, surahs.size)
        assertEquals(6236, surahs.sumOf { it.verseCount })
        assertTrue(surahs.all { it.pages.size == it.verseCount })
        assertEquals(1, surahs.first().pages.first())
        assertEquals(604, surahs.last().pages.last())
    }

    @Test fun everyContentVerseExists() {
        val root = json.parseToJsonElement(asset("content.json")) as kotlinx.serialization.json.JsonObject
        fun exists(id: String): Boolean {
            val ref = VerseRef.parse(id) ?: return false
            return ref.surah in 1..114 && ref.ayah in 1..surahs[ref.surah - 1].verseCount
        }
        val daily = (root["dailyVerses"] as kotlinx.serialization.json.JsonArray).map { it.toString().trim('"') }
        assertTrue(daily.isNotEmpty())
        daily.forEach { assertTrue(it, exists(it)) }
        for (key in listOf("feelings", "topics")) {
            (root[key] as kotlinx.serialization.json.JsonArray).forEach { c ->
                val verses = (c as kotlinx.serialization.json.JsonObject)["verses"] as kotlinx.serialization.json.JsonArray
                verses.forEach { v -> assertTrue(v.toString(), exists(v.toString().trim('"'))) }
            }
        }
        assertTrue((root["duas"] as kotlinx.serialization.json.JsonArray).isNotEmpty())
    }
}
