package com.quranforwomen.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Keys for simple preferences. Same names as the iPhone app. */
object PrefKey {
    const val hasOnboarded = "hasOnboarded"
    const val arabicSize = "reader.arabicSize"
    const val translationSize = "reader.translationSize"
    const val showTranslation = "reader.showTranslation"
    const val showTransliteration = "duas.showTransliteration"
    const val appearance = "appearance"
    const val haptics = "haptics"
    const val reciter = "audio.reciter"
    const val translation = "translation.edition"
    const val reminderEnabled = "reminder.enabled"
    const val reminderMinutes = "reminder.minutes"
    const val eveningReminderEnabled = "reminder.evening.enabled"
    const val eveningReminderMinutes = "reminder.evening.minutes"
    const val dhikrSelection = "dhikr.selection"
    const val dhikrTarget = "dhikr.target"
    const val journalLock = "journal.lock"
}

object ReaderDefaults {
    const val arabicSize = 30f
    const val translationSize = 17f
    val arabicRange = 22f..48f
    val translationRange = 13f..26f
}

enum class Appearance(val title: String) {
    system("Automatic"), light("Light"), dark("Dark");
}

/**
 * App settings, saved in SharedPreferences and readable from Compose like state:
 * any screen that reads a value redraws when it changes.
 */
class Prefs(context: Context) {
    val sp: SharedPreferences = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    var hasOnboarded by bool(PrefKey.hasOnboarded, false)
    var arabicSize by float(PrefKey.arabicSize, ReaderDefaults.arabicSize)
    var translationSize by float(PrefKey.translationSize, ReaderDefaults.translationSize)
    var showTranslation by bool(PrefKey.showTranslation, true)
    var showTransliteration by bool(PrefKey.showTransliteration, true)
    var appearanceRaw by string(PrefKey.appearance, Appearance.system.name)
    var hapticsOn by bool(PrefKey.haptics, true)
    var reminderEnabled by bool(PrefKey.reminderEnabled, false)
    var reminderMinutes by int(PrefKey.reminderMinutes, 7 * 60)
    var eveningReminderEnabled by bool(PrefKey.eveningReminderEnabled, false)
    var eveningReminderMinutes by int(PrefKey.eveningReminderMinutes, 20 * 60)
    var dhikrSelection by string(PrefKey.dhikrSelection, "subhanallah")
    /** -1 is the phrase's suggested target, 0 is no target. */
    var dhikrTarget by int(PrefKey.dhikrTarget, -1)

    var appearance: Appearance
        get() = runCatching { Appearance.valueOf(appearanceRaw) }.getOrDefault(Appearance.system)
        set(value) { appearanceRaw = value.name }

    // Delegates. Each keeps a Compose state mirror so reads are observed.

    fun bool(key: String, default: Boolean) = Pref(key, sp.getBoolean(key, default)) { putBoolean(key, it) }
    fun int(key: String, default: Int) = Pref(key, sp.getInt(key, default)) { putInt(key, it) }
    fun float(key: String, default: Float) = Pref(key, sp.getFloat(key, default)) { putFloat(key, it) }
    fun string(key: String, default: String) = Pref(key, sp.getString(key, default) ?: default) { putString(key, it) }

    inner class Pref<T>(
        private val key: String,
        initial: T,
        private val write: SharedPreferences.Editor.(T) -> Unit,
    ) : ReadWriteProperty<Any?, T> {
        private val state: MutableState<T> = mutableStateOf(initial)
        override fun getValue(thisRef: Any?, property: KProperty<*>): T = state.value
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            if (state.value == value) return
            state.value = value
            sp.edit().apply { write(value) }.apply()
        }
    }
}
