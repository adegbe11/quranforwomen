package com.quranforwomen.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlin.math.abs

/** A reading plan she finished, kept so she can look back on it. */
@Serializable
data class FinishedPlan(val title: String, val start: Long, val finish: Long)

/** A short name for the plan, used on the plan screen and in her finished plans. */
val ReadingPlan.title: String
    get() {
        val first = firstSurah
        if (first != null) {
            if (first == 1 && (lastSurah ?: 114) >= 114) return "One surah at a time"
            if (first == 78) return "Short surahs"
            return "Surahs $first to ${lastSurah ?: 114}"
        }
        return when (days) {
            120 -> "4 months"
            365 -> "One year"
            else -> "$days days"
        }
    }

/** Finished reading plans, newest first. Stored on this phone in its own preferences file. */
class PlanHistoryStore(context: Context) {
    private val sp = context.getSharedPreferences("plan_history", Context.MODE_PRIVATE)
    private val serializer = ListSerializer(FinishedPlan.serializer())

    var finished by mutableStateOf<List<FinishedPlan>>(emptyList()); private set

    init {
        sp.getString(KEY, null)?.let { json ->
            runCatching { AppJson.decodeFromString(serializer, json) }.getOrNull()?.let { finished = it }
        }
    }

    /**
     * Keeps the list in step with the current plan: a complete plan is listed,
     * and a plan that is no longer complete (a day unmarked) is taken off again.
     */
    fun sync(plan: ReadingPlan?) {
        plan ?: return
        val existing = finished.firstOrNull { abs(it.start - plan.start) < 1000 }
        if (plan.isComplete) {
            if (existing != null) return
            finished = listOf(FinishedPlan(plan.title, plan.start, System.currentTimeMillis())) + finished
            save()
        } else if (existing != null) {
            finished = finished - existing
            save()
        }
    }

    fun eraseAll() {
        finished = emptyList()
        sp.edit().remove(KEY).apply()
    }

    private fun save() {
        sp.edit().putString(KEY, AppJson.encodeToString(serializer, finished)).apply()
    }

    private companion object {
        const val KEY = "planHistory.v1"
    }
}
