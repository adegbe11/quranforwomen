package com.quranforwomen.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** A plan to read the whole Quran over a number of days, split by mushaf pages. */
@Serializable
data class ReadingPlan(
    val days: Int,
    val start: Long,
    val finishedDays: Set<Int> = emptySet(),
    // Added later. Optional so older saved plans still decode. Null means a page plan.
    val firstSurah: Int? = null,
    val lastSurah: Int? = null,
) {
    /** True for plans that read one surah a day instead of a share of pages. */
    val isSurahPlan: Boolean get() = firstSurah != null

    /** The surah read on a day of a surah plan. */
    fun surah(day: Int): Int = (firstSurah ?: 1) + day

    /** True when finishing the plan means the whole Quran was read. */
    val coversWholeQuran: Boolean get() = !isSurahPlan || (firstSurah == 1 && (lastSurah ?: 114) >= 114)

    /** How much of the plan is read, from 0 to 1. */
    val progress: Double
        get() {
            if (days <= 0) return 0.0
            if (isSurahPlan || days > TOTAL_PAGES) return minOf(1.0, finishedDays.size.toDouble() / days)
            val pagesRead = finishedDays.sumOf { pages(it).count() }
            return minOf(1.0, pagesRead.toDouble() / TOTAL_PAGES)
        }

    /** Zero based day of the plan for a date, clamped to the plan length. */
    fun dayIndex(on: LocalDate = LocalDate.now()): Int {
        val startDay = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDate()
        val elapsed = ChronoUnit.DAYS.between(startDay, on).toInt()
        return elapsed.coerceIn(0, days - 1)
    }

    fun pages(day: Int): IntRange {
        val first = day * TOTAL_PAGES / days + 1
        val last = maxOf(first, (day + 1) * TOTAL_PAGES / days)
        return first..last
    }

    val isComplete: Boolean get() = finishedDays.size >= days

    companion object { const val TOTAL_PAGES = 604 }
}

/**
 * Everything personal: bookmarks, highlights, notes, saved duas, reading place,
 * prayer log and dhikr counts. Stored on this phone, and in her own encrypted
 * Google backup if she has backup on (the journal is never included).
 */
class LibraryStore(context: Context) {
    @Serializable
    private data class Snapshot(
        val bookmarks: List<VerseRef> = emptyList(),
        val savedDuaIDs: List<String> = emptyList(),
        val lastRead: VerseRef? = null,
        val completedSurahs: Set<Int> = emptySet(),
        val personalDuas: List<PersonalDua> = emptyList(),
        val dhikrCounts: Map<String, Int> = emptyMap(),
        val highlights: Map<String, HighlightColor> = emptyMap(),
        val notes: Map<String, VerseNote> = emptyMap(),
        val prayerLog: Map<String, List<String>> = emptyMap(),
        val restSince: Long? = null,
        val plan: ReadingPlan? = null,
        val updatedAt: Long? = null,
        val favouriteSurahs: List<Int> = emptyList(),
        val history: List<ReadingVisit> = emptyList(),
    )

    private val sp = context.getSharedPreferences("library", Context.MODE_PRIVATE)

    var bookmarks by mutableStateOf<List<VerseRef>>(emptyList()); private set
    var savedDuaIDs by mutableStateOf<List<String>>(emptyList()); private set
    var lastRead by mutableStateOf<VerseRef?>(null); private set
    var completedSurahs by mutableStateOf<Set<Int>>(emptySet()); private set
    var personalDuas by mutableStateOf<List<PersonalDua>>(emptyList()); private set
    var dhikrCounts by mutableStateOf<Map<String, Int>>(emptyMap()); private set
    var highlights by mutableStateOf<Map<String, HighlightColor>>(emptyMap()); private set
    var notes by mutableStateOf<Map<String, VerseNote>>(emptyMap()); private set
    /** "yyyy-MM-dd" to the prayers marked as prayed that day. */
    var prayerLog by mutableStateOf<Map<String, List<String>>>(emptyMap()); private set
    /** Set while she is on rest days (her period). Prayer reminders pause. */
    var restSince by mutableStateOf<Long?>(null); private set
    var plan by mutableStateOf<ReadingPlan?>(null); private set
    /** Surah numbers she marked with a heart, in Quran order. */
    var favouriteSurahs by mutableStateOf<List<Int>>(emptyList()); private set
    /** The last places opened in the reader, newest first. */
    var history by mutableStateOf<List<ReadingVisit>>(emptyList()); private set

    /** Called after any change that prayer reminders depend on. */
    var onRestChanged: ((Boolean) -> Unit)? = null

    init {
        sp.getString(KEY, null)?.let { json ->
            runCatching { AppJson.decodeFromString(Snapshot.serializer(), json) }.getOrNull()?.let(::apply)
        }
    }

    // Verses

    fun isBookmarked(ref: VerseRef) = ref in bookmarks

    fun toggleBookmark(ref: VerseRef) {
        bookmarks = if (ref in bookmarks) bookmarks - ref else listOf(ref) + bookmarks
        save()
    }

    fun removeBookmark(ref: VerseRef) { bookmarks = bookmarks - ref; save() }

    fun restoreBookmark(ref: VerseRef, at: Int) {
        if (ref in bookmarks) return
        bookmarks = bookmarks.toMutableList().apply { add(at.coerceIn(0, size), ref) }
        save()
    }

    fun highlight(ref: VerseRef): HighlightColor? = highlights[ref.id]

    fun setHighlight(color: HighlightColor?, ref: VerseRef) {
        highlights = if (color == null) highlights - ref.id else highlights + (ref.id to color)
        save()
    }

    fun note(ref: VerseRef): VerseNote? = notes[ref.id]

    fun setNote(text: String, ref: VerseRef) {
        val trimmed = text.trim()
        notes = if (trimmed.isEmpty()) notes - ref.id else notes + (ref.id to VerseNote(trimmed))
        save()
    }

    /** Verses with a highlight or a note, newest note first, then in Quran order. */
    val annotatedVerses: List<VerseRef>
        get() = (highlights.keys + notes.keys).mapNotNull(VerseRef::parse).sortedWith { a, b ->
            val da = notes[a.id]?.updated ?: 0L
            val db = notes[b.id]?.updated ?: 0L
            if (da == db) a.compareTo(b) else db.compareTo(da)
        }

    fun markLastRead(ref: VerseRef) {
        if (ref == lastRead) return
        lastRead = ref
        // The newest history entry follows her place in the surah she has open.
        val first = history.firstOrNull()
        if (first != null && first.ref.surah == ref.surah) history = listOf(first.copy(ref = ref)) + history.drop(1)
        save()
    }

    // Favourite surahs

    fun isFavourite(surah: Int) = surah in favouriteSurahs

    fun toggleFavourite(surah: Int) {
        favouriteSurahs = if (surah in favouriteSurahs) favouriteSurahs - surah else (favouriteSurahs + surah).sorted()
        save()
    }

    // Reading history

    /** Called when the reader opens. One entry per surah, newest first. */
    fun recordVisit(ref: VerseRef) {
        history = (listOf(ReadingVisit(ref, System.currentTimeMillis())) + history.filter { it.ref.surah != ref.surah })
            .take(HISTORY_LIMIT)
        save()
    }

    fun clearHistory() { history = emptyList(); save() }

    fun markCompleted(surah: Int) {
        if (surah in completedSurahs) return
        completedSurahs = completedSurahs + surah
        save()
    }

    // Duas

    fun isSaved(dua: Dua) = dua.id in savedDuaIDs

    fun toggleSaved(dua: Dua) {
        savedDuaIDs = if (dua.id in savedDuaIDs) savedDuaIDs - dua.id else listOf(dua.id) + savedDuaIDs
        save()
    }

    fun addPersonalDua(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        personalDuas = listOf(PersonalDua(text = trimmed)) + personalDuas
        save()
    }

    fun updatePersonalDua(dua: PersonalDua, text: String) {
        val trimmed = text.trim()
        personalDuas = if (trimmed.isEmpty()) personalDuas.filter { it.id != dua.id }
        else personalDuas.map { if (it.id == dua.id) it.copy(text = trimmed) else it }
        save()
    }

    fun removePersonalDua(dua: PersonalDua) { personalDuas = personalDuas.filter { it.id != dua.id }; save() }

    fun restorePersonalDua(dua: PersonalDua, at: Int) {
        if (personalDuas.any { it.id == dua.id }) return
        personalDuas = personalDuas.toMutableList().apply { add(at.coerceIn(0, size), dua) }
        save()
    }

    // Dhikr

    fun dhikrCount(id: String): Int = dhikrCounts[id] ?: 0

    fun setDhikrCount(id: String, value: Int) {
        dhikrCounts = dhikrCounts + (id to maxOf(0, value))
        save()
    }

    // Prayer

    fun prayed(prayer: String, date: LocalDate = LocalDate.now()): Boolean =
        prayerLog[dayKey(date)]?.contains(prayer) == true

    fun togglePrayed(prayer: String, date: LocalDate = LocalDate.now()) {
        val key = dayKey(date)
        val done = (prayerLog[key] ?: emptyList()).toMutableList()
        if (prayer in done) done.remove(prayer) else done.add(prayer)
        prayerLog = if (done.isEmpty()) prayerLog - key else prayerLog + (key to done)
        save()
    }

    fun setPrayed(prayer: String, date: LocalDate = LocalDate.now()) {
        if (!prayed(prayer, date)) togglePrayed(prayer, date)
    }

    val isResting: Boolean get() = restSince != null

    fun setResting(on: Boolean) {
        restSince = if (on) (restSince ?: System.currentTimeMillis()) else null
        save()
        onRestChanged?.invoke(on)
    }

    // Reading plan

    fun startPlan(days: Int) { plan = ReadingPlan(days, System.currentTimeMillis()); save() }

    /** One surah a day, from the first surah to the last, in order. */
    fun startSurahPlan(first: Int, last: Int) {
        val low = first.coerceIn(1, 114)
        val high = last.coerceIn(low, 114)
        plan = ReadingPlan(high - low + 1, System.currentTimeMillis(), firstSurah = low, lastSurah = high)
        save()
    }

    fun toggleFinished(day: Int) {
        val current = plan ?: return
        plan = current.copy(finishedDays = if (day in current.finishedDays) current.finishedDays - day else current.finishedDays + day)
        save()
    }

    fun endPlan() { plan = null; save() }

    // Data controls

    fun eraseAll() {
        apply(Snapshot())
        sp.edit().remove(KEY).apply()
        onRestChanged?.invoke(false)
    }

    private fun apply(s: Snapshot) {
        bookmarks = s.bookmarks
        savedDuaIDs = s.savedDuaIDs
        lastRead = s.lastRead
        completedSurahs = s.completedSurahs
        personalDuas = s.personalDuas
        dhikrCounts = s.dhikrCounts
        highlights = s.highlights
        notes = s.notes
        prayerLog = s.prayerLog
        restSince = s.restSince
        plan = s.plan
        favouriteSurahs = s.favouriteSurahs
        history = s.history
    }

    private fun save() {
        val snapshot = Snapshot(
            bookmarks, savedDuaIDs, lastRead, completedSurahs, personalDuas, dhikrCounts,
            highlights, notes, prayerLog, restSince, plan, System.currentTimeMillis(),
            favouriteSurahs = favouriteSurahs, history = history,
        )
        sp.edit().putString(KEY, AppJson.encodeToString(Snapshot.serializer(), snapshot)).apply()
    }

    companion object {
        private const val KEY = "library.v1"
        const val HISTORY_LIMIT = 20

        /** Always Western digits, whatever the phone's language, so saved keys never change. */
        fun dayKey(date: LocalDate): String =
            String.format(java.util.Locale.US, "%04d-%02d-%02d", date.year, date.monthValue, date.dayOfMonth)
    }
}
