package com.quranforwomen.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Prayers she wants to make up, set by her, counting down as she marks them. Kept only on this phone. */
class MissedPrayersStore(context: Context) {
    private val sp = context.getSharedPreferences("missedPrayers", Context.MODE_PRIVATE)

    var left by mutableStateOf(Prayer.prayers.associateWith { sp.getInt(key(it), 0) }); private set
    var madeUp by mutableIntStateOf(sp.getInt(K_MADE_UP, 0)); private set

    fun left(prayer: Prayer): Int = left[prayer] ?: 0

    val totalLeft: Int get() = left.values.sum()

    fun setLeft(prayer: Prayer, value: Int) {
        val v = value.coerceIn(0, MAX)
        left = left + (prayer to v)
        sp.edit().putInt(key(prayer), v).apply()
    }

    /** Marks one made up. Returns false when none are left. */
    fun markOne(prayer: Prayer): Boolean {
        val current = left(prayer)
        if (current <= 0) return false
        setLeft(prayer, current - 1)
        madeUp += 1
        sp.edit().putInt(K_MADE_UP, madeUp).apply()
        return true
    }

    fun undoOne(prayer: Prayer) {
        setLeft(prayer, left(prayer) + 1)
        madeUp = maxOf(0, madeUp - 1)
        sp.edit().putInt(K_MADE_UP, madeUp).apply()
    }

    /** Erase My Data. */
    fun eraseAll() {
        left = Prayer.prayers.associateWith { 0 }
        madeUp = 0
        sp.edit().clear().apply()
    }

    private fun key(prayer: Prayer) = "missed.${prayer.name}"

    companion object {
        const val MAX = 99999
        private const val K_MADE_UP = "missed.madeUp"
    }
}
