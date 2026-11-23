package com.quranforwomen.app.feature.quran

import androidx.compose.runtime.Composable

/** The Quran tab: the Mushaf, continue reading, and the 114 surahs with search. */
@Composable
fun QuranLibraryScreen() {
    QuranLibraryContent()
}

/** One surah, set like a book, opening at a verse when given one. */
@Composable
fun SurahReaderScreen(surah: Int, verse: Int?) {
    SurahReaderContent(surah, verse)
}

/** Text size, translation and reciter, pushed from You. */
@Composable
fun ReadingSettingsScreen() {
    ReadingSettingsContent()
}
