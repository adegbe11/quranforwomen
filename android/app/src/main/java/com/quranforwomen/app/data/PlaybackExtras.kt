package com.quranforwomen.app.data

/** Keys for the recitation extras. Same names as the iPhone app. */
object PlaybackPrefs {
    const val speed = "audio.speed"
    const val showQuranTransliteration = "reader.showQuranTransliteration"
}

/** Recitation speeds she can pick. */
object PlaybackSpeed {
    val choices = listOf(0.75f, 1f, 1.25f, 1.5f)

    fun label(value: Float): String = when (value) {
        0.75f -> "0.75x"
        1f -> "1x"
        1.25f -> "1.25x"
        1.5f -> "1.5x"
        else -> "${value}x"
    }
}

/** Pauses recitation after a while, or once the surah ends. */
sealed class SleepTimer {
    data class Minutes(val minutes: Int) : SleepTimer()
    data object EndOfSurah : SleepTimer()

    val title: String
        get() = when (this) {
            is Minutes -> if (minutes == 60) "1 hour" else "$minutes minutes"
            EndOfSurah -> "End of surah"
        }

    companion object {
        // A getter, not a stored list: a stored list would be built before EndOfSurah exists and hold null.
        val choices: List<SleepTimer> get() = listOf(Minutes(15), Minutes(30), Minutes(45), Minutes(60), EndOfSurah)
    }
}
