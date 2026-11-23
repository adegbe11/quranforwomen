package com.quranforwomen.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/** One challenge. Every day is built from the bundled, verified content only. */
data class Challenge(
    val id: String,
    val title: String,
    val symbol: String,
    val days: Int,
    val kind: Kind,
) {
    sealed interface Kind {
        /** One bundled dhikr phrase a day, with its own target. */
        data object Dhikr : Kind
        /** A few pages of the mushaf a day, from where she is reading. */
        data class QuranPages(val perDay: Int) : Kind
        /** The bundled morning duas, one a day. */
        data object Morning : Kind
        /** The bundled evening duas, one a day. */
        data object Evening : Kind
        /** A gratitude verse and a journal prompt a day. */
        data object Gratitude : Kind
        /** The 30 day page plan. */
        data object QuranMonth : Kind
    }
}

object ChallengeCatalog {
    val all = listOf(
        Challenge("dhikr-7", "7 days of dhikr", "circle.dotted", 7, Challenge.Kind.Dhikr),
        Challenge("quran-7", "7 days of Quran", "book", 7, Challenge.Kind.QuranPages(perDay = 4)),
        Challenge("morning-7", "Morning adhkar for 7 days", "sunrise", 7, Challenge.Kind.Morning),
        Challenge("evening-7", "Evening adhkar for 7 days", "moon", 7, Challenge.Kind.Evening),
        Challenge("gratitude-7", "7 days of gratitude", "heart", 7, Challenge.Kind.Gratitude),
        Challenge("quran-30", "30 days with the Quran", "calendar", 30, Challenge.Kind.QuranMonth),
    )

    fun challenge(id: String): Challenge? = all.firstOrNull { it.id == id }
}

/** Her place in a challenge. */
@Serializable
data class ChallengeProgress(
    val started: Long,
    val done: Set<Int> = emptySet(),
    /** The mushaf page a page challenge began from. */
    val startPage: Int = 1,
)

/**
 * Challenges she has started. Missing a day never breaks anything: she simply continues
 * from the next day not yet done. Stored only on this phone.
 */
class ChallengeStore(context: Context) {
    private val sp = context.getSharedPreferences("challenges", Context.MODE_PRIVATE)
    private val serializer = MapSerializer(String.serializer(), ChallengeProgress.serializer())

    var progress by mutableStateOf<Map<String, ChallengeProgress>>(emptyMap()); private set

    init {
        sp.getString(KEY, null)?.let { json ->
            runCatching { AppJson.decodeFromString(serializer, json) }.getOrNull()?.let { progress = it }
        }
    }

    fun state(challenge: Challenge): ChallengeProgress? = progress[challenge.id]

    /** Challenges started and not yet finished. */
    val activeCount: Int
        get() = ChallengeCatalog.all.count { c -> progress[c.id]?.let { it.done.size < c.days } ?: false }

    fun start(challenge: Challenge, startPage: Int) {
        progress = progress + (challenge.id to ChallengeProgress(System.currentTimeMillis(), emptySet(), startPage.coerceAtLeast(1)))
        save()
    }

    fun isDone(day: Int, challenge: Challenge): Boolean = progress[challenge.id]?.done?.contains(day) == true

    fun setDone(done: Boolean, day: Int, challenge: Challenge) {
        val value = progress[challenge.id] ?: return
        if (day !in 0 until challenge.days) return
        val days = if (done) value.done + day else value.done - day
        progress = progress + (challenge.id to value.copy(done = days))
        save()
    }

    /** The first day not yet done, or the last day once all are done. */
    fun nextDay(challenge: Challenge): Int {
        val done = progress[challenge.id]?.done ?: emptySet()
        return (0 until challenge.days).firstOrNull { it !in done } ?: (challenge.days - 1)
    }

    fun isComplete(challenge: Challenge): Boolean = (progress[challenge.id]?.done?.size ?: 0) >= challenge.days

    fun leave(challenge: Challenge) {
        progress = progress - challenge.id
        save()
    }

    /** Erase My Data. */
    fun eraseAll() {
        progress = emptyMap()
        sp.edit().remove(KEY).apply()
    }

    private fun save() {
        sp.edit().putString(KEY, AppJson.encodeToString(serializer, progress)).apply()
    }

    private companion object {
        const val KEY = "progress"
    }
}
