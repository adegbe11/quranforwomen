package com.quranforwomen.app.data

import kotlinx.serialization.Serializable

/** One place opened in the reader, and when (epoch millis). Kept in LibraryStore's reading history. */
@Serializable
data class ReadingVisit(val ref: VerseRef, val date: Long)
