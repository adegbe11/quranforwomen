package com.quranforwomen.app.data

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quranforwomen.app.R
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

/**
 * A dhikr phrase she writes in her own words, with her own target.
 * Always shown as personal, never as Quran or Sunnah, and kept apart from the sourced list.
 */
@Serializable
data class CustomDhikr(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    /** 0 means no target. */
    val target: Int,
    val created: Long = System.currentTimeMillis(),
) {
    /** The key her count is kept under, apart from the sourced phrase ids. */
    val countID: String get() = PREFIX + id

    companion object { const val PREFIX = "custom:" }
}

/**
 * Dhikr extras and recently opened duas: her own phrases, how much she counted each day,
 * an optional daily goal, the counting sound, and the last duas she opened.
 * Kept only on this phone.
 */
class DhikrStore(context: Context) {
    @Serializable
    private data class Snapshot(
        val custom: List<CustomDhikr> = emptyList(),
        /** "yyyy-MM-dd" to the total she counted that day. */
        val daily: Map<String, Int> = emptyMap(),
        val goal: Int? = null,
        val soundOn: Boolean = false,
        val recentDuaIDs: List<String> = emptyList(),
    )

    private val app = context.applicationContext
    private val sp = app.getSharedPreferences("dhikr_extras", Context.MODE_PRIVATE)

    var custom by mutableStateOf<List<CustomDhikr>>(emptyList()); private set
    var daily by mutableStateOf<Map<String, Int>>(emptyMap()); private set
    var goal by mutableStateOf<Int?>(null); private set
    var soundOn by mutableStateOf(false); private set
    var recentDuaIDs by mutableStateOf<List<String>>(emptyList()); private set

    private var pool: SoundPool? = null
    private var clickId = 0
    private var clickReady = false

    init {
        sp.getString(KEY, null)?.let { json ->
            runCatching { AppJson.decodeFromString(Snapshot.serializer(), json) }.getOrNull()?.let { s ->
                custom = s.custom
                daily = s.daily
                goal = s.goal
                soundOn = s.soundOn
                recentDuaIDs = s.recentDuaIDs
            }
        }
    }

    // Her own dhikr

    fun customDhikr(countID: String): CustomDhikr? = custom.firstOrNull { it.countID == countID }

    fun addCustom(text: String, target: Int): CustomDhikr? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val item = CustomDhikr(text = trimmed, target = maxOf(0, target))
        custom = listOf(item) + custom
        save()
        return item
    }

    fun updateCustom(item: CustomDhikr, text: String, target: Int) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        custom = custom.map { if (it.id == item.id) it.copy(text = trimmed, target = maxOf(0, target)) else it }
        save()
    }

    fun removeCustom(item: CustomDhikr) {
        custom = custom.filter { it.id != item.id }
        save()
    }

    fun restoreCustom(item: CustomDhikr, at: Int) {
        if (custom.any { it.id == item.id }) return
        custom = custom.toMutableList().apply { add(at.coerceIn(0, size), item) }
        save()
    }

    // Daily totals

    /** Adds one count to today. Days beyond the history window are dropped. */
    fun recordCount(date: LocalDate = LocalDate.now()) {
        val key = LibraryStore.dayKey(date)
        var next = daily + (key to ((daily[key] ?: 0) + 1))
        if (next.size > HISTORY_DAYS + 5) {
            val oldest = LibraryStore.dayKey(date.minusDays(HISTORY_DAYS.toLong()))
            next = next.filterKeys { it >= oldest }
        }
        daily = next
        save()
    }

    fun total(date: LocalDate = LocalDate.now()): Int = daily[LibraryStore.dayKey(date)] ?: 0

    /** The last 30 days, oldest first, ending today. */
    fun history(endingOn: LocalDate = LocalDate.now()): List<Pair<LocalDate, Int>> =
        (HISTORY_DAYS - 1 downTo 0).map { offset ->
            val day = endingOn.minusDays(offset.toLong())
            day to total(day)
        }

    fun updateGoal(value: Int?) {
        goal = value?.takeIf { it > 0 }
        save()
    }

    // Counting sound

    fun updateSoundOn(on: Boolean) {
        soundOn = on
        save()
        if (on) prepareClick()
    }

    /** A soft click, only when she turned the sound on. */
    fun playClick() {
        if (!soundOn) return
        prepareClick()
        if (clickReady) pool?.play(clickId, 0.7f, 0.7f, 1, 0, 1f)
    }

    private fun prepareClick() {
        if (pool != null) return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val created = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(attributes).build()
        created.setOnLoadCompleteListener { _, _, status -> clickReady = status == 0 }
        clickId = created.load(app, R.raw.dhikr_click, 1)
        pool = created
    }

    // Recently opened duas

    fun noteOpened(duaID: String) {
        if (recentDuaIDs.firstOrNull() == duaID) return
        recentDuaIDs = (listOf(duaID) + recentDuaIDs.filter { it != duaID }).take(RECENT_LIMIT)
        save()
    }

    // Data controls

    fun eraseAll() {
        custom = emptyList()
        daily = emptyMap()
        goal = null
        soundOn = false
        recentDuaIDs = emptyList()
        sp.edit().remove(KEY).apply()
    }

    private fun save() {
        val snapshot = Snapshot(custom, daily, goal, soundOn, recentDuaIDs)
        sp.edit().putString(KEY, AppJson.encodeToString(Snapshot.serializer(), snapshot)).apply()
    }

    companion object {
        private const val KEY = "dhikr.extras.v1"
        const val RECENT_LIMIT = 10
        const val HISTORY_DAYS = 30
    }
}
