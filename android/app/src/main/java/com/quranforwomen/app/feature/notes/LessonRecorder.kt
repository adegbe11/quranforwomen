package com.quranforwomen.app.feature.notes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quranforwomen.app.data.QuranStore
import com.quranforwomen.app.data.VerseRef
import java.text.BreakIterator
import java.text.Normalizer
import java.util.Locale

// Verse detection

/**
 * Finds Quran references in free text: "2:255", "Surah Al-Baqarah verse 255",
 * "Baqarah ayah 255", "chapter 2 verse 255". Only returns verses that exist.
 */
internal object NotesVerseDetector {
    private val colon = Regex("""\b(\d{1,3})\s*:\s*(\d{1,3})\b(?!\s*(?:am|pm|a\.m|p\.m))""", RegexOption.IGNORE_CASE)
    private val chapter = Regex("""(?:chapter|surah|sura)\s+(\d{1,3}),?\s+(?:verse|ayah|ayat|aya)\s+(\d{1,3})""", RegexOption.IGNORE_CASE)
    private val named = Regex("""([a-z]+(?: [a-z]+)?) (?:verse|ayah|ayat|aya) (\d{1,3})""")

    @Volatile private var nameKeys: List<Pair<Int, String>>? = null

    fun find(text: String, quran: QuranStore, limit: Int = 12): List<VerseRef> {
        if (text.isBlank()) return emptyList()
        val found = mutableListOf<VerseRef>()
        fun add(ref: VerseRef) {
            if (quran.contains(ref) && ref !in found) found += ref
        }

        // 2:255
        for (m in colon.findAll(text)) {
            val s = m.groupValues[1].toIntOrNull()
            val a = m.groupValues[2].toIntOrNull()
            if (s != null && a != null) add(VerseRef(s, a))
        }

        // chapter 2 verse 255
        for (m in chapter.findAll(text)) {
            val s = m.groupValues[1].toIntOrNull()
            val a = m.groupValues[2].toIntOrNull()
            if (s != null && a != null) add(VerseRef(s, a))
        }

        // Surah Al-Baqarah verse 255, Baqarah ayah 255
        val folded = fold(text)
        val names = nameKeys ?: quran.surahs.map { s ->
            val key = fold(s.name).let { if (it.startsWith("al")) it.substring(2) else it }
            s.id to key
        }.also { nameKeys = it }
        for (m in named.findAll(folded)) {
            val words = m.groupValues[1].replace(" ", "")
            val a = m.groupValues[2].toIntOrNull() ?: continue
            val candidate = if (words.startsWith("al")) words.substring(2) else words
            val surah = names.firstOrNull { (_, key) -> key.isNotEmpty() && (candidate.endsWith(key) || key == candidate) }
            if (surah != null) add(VerseRef(surah.first, a))
        }
        return found.take(limit)
    }

    /** Lowercase letters, digits and single spaces only. */
    private fun fold(text: String): String {
        val lowered = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
        val out = StringBuilder(lowered.length)
        var lastWasSpace = false
        for (ch in lowered) {
            if (ch.isLetterOrDigit()) {
                out.append(ch)
                lastWasSpace = false
            } else if (ch == '-' || ch == '\'' || ch == '’') {
                continue
            } else if (!lastWasSpace) {
                out.append(' ')
                lastWasSpace = true
            }
        }
        return out.toString()
    }
}

// Arranging a talk into notes

/** Picks the speaker's own sentences. It never rewrites or summarizes in new words. */
internal object NotesArranger {
    data class Result(val title: String, val points: List<String>, val quotes: List<String>, val takeaway: String)

    private val stopWords = setOf(
        "the", "and", "that", "this", "with", "for", "you", "your", "are", "was", "were", "have", "has", "had",
        "but", "not", "they", "them", "their", "what", "when", "will", "would", "there", "from", "about",
        "which", "who", "his", "her", "him", "she", "our", "out", "can", "all", "just", "like", "one", "because",
        "into", "then", "than", "also", "some", "very", "more", "know", "said", "says", "say", "going", "really",
        "people", "thing", "things", "even", "only", "want", "get", "how", "why", "any",
    )

    private val pointCues = listOf(
        "the first", "the second", "the third", "number one", "number two", "the point is", "the lesson",
        "remember", "never forget", "the key", "most important", "allah tells us", "allah says",
        "the prophet", "we learn", "this teaches",
    )

    private val quoteCues = listOf("allah says", "allah tells us", "the prophet said", "he said", "she said", "narrated")

    private val actionCues = listOf(
        "this week", "go home", "tonight", "tomorrow", "starting today", "try to", "make sure", "let us",
        "let's", "i want you to", "make dua", "ask yourselves", "ask yourself", "start",
    )

    fun arrange(transcript: String, verses: List<VerseRef>, surahName: (Int) -> String): Result {
        val sentences = split(transcript)
        if (sentences.isEmpty()) return Result("", emptyList(), emptyList(), "")

        // How often each meaningful word appears across the whole talk.
        val frequency = HashMap<String, Int>()
        for (word in sentences.flatMap(::words)) {
            if (word.length > 3 && word !in stopWords) frequency[word] = (frequency[word] ?: 0) + 1
        }

        fun score(sentence: String): Double {
            val lower = sentence.lowercase()
            val ws = words(sentence)
            val themeScore = ws.sumOf { (frequency[it] ?: 0).toDouble() } / maxOf(ws.size, 1)
            val cueScore = if (pointCues.any { lower.contains(it) }) 3.0 else 0.0
            return themeScore + cueScore
        }

        val usable = sentences.withIndex().filter { words(it.value).size in 8..45 }
        val ranked = usable.sortedByDescending { score(it.value) }

        val pointIndexes = ranked.take(4).map { it.index }.toSet()
        val points = sentences.withIndex().filter { it.index in pointIndexes }.map { it.value }

        var quotes = usable.filter { item -> quoteCues.any { item.value.lowercase().contains(it) } }
            .map { it.value }.filter { it !in points }
        if (quotes.size < 2) {
            val more = ranked.drop(4).map { it.value }.filter { it !in quotes && it !in points }
            quotes = quotes + more
        }
        quotes = quotes.take(2)

        val lastFifth = sentences.size * 4 / 5
        val takeaway = sentences.asReversed().firstOrNull { s ->
            actionCues.any { s.lowercase().contains(it) } && words(s).size >= 5
        } ?: sentences.drop(lastFifth).maxByOrNull { score(it) } ?: ""

        val themes = frequency.entries.sortedByDescending { it.value }.take(2).map { e -> e.key.replaceFirstChar { it.uppercase() } }
        val title = when {
            verses.isNotEmpty() -> "Reflections on ${surahName(verses.first().surah)}"
            themes.size == 2 -> "${themes[0]} and ${themes[1]}"
            else -> ""
        }
        return Result(title, points, quotes, takeaway)
    }

    private fun split(text: String): List<String> {
        val result = mutableListOf<String>()
        val it = BreakIterator.getSentenceInstance(Locale.getDefault())
        it.setText(text)
        var start = it.first()
        var end = it.next()
        while (end != BreakIterator.DONE) {
            val s = text.substring(start, end).trim()
            if (s.isNotEmpty()) result += s
            start = end
            end = it.next()
        }
        return result
    }

    private fun words(sentence: String): List<String> =
        sentence.lowercase().split { !it.isLetter() && it != '\'' }.filter { it.isNotEmpty() }

    private fun String.split(isSeparator: (Char) -> Boolean): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        for (c in this) {
            if (isSeparator(c)) { out += sb.toString(); sb.clear() } else sb.append(c)
        }
        out += sb.toString()
        return out
    }
}

// Recorder

/**
 * Live transcription of a khutbah or class, on the phone where it can be (EXTRA_PREFER_OFFLINE).
 * The audio is never saved. Android ends a recognition session at each pause, so it quietly
 * starts a fresh one and keeps the words heard so far.
 */
internal class NotesRecorder(private val context: Context) {
    enum class State { idle, denied, recording, unavailable }

    var state by mutableStateOf(State.idle); private set
    var transcript by mutableStateOf(""); private set
    var startedAt by mutableStateOf<Long?>(null); private set

    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    /** Text from earlier recognition sessions. */
    private var committed = ""
    /** Sessions in a row that failed outright. Stops a broken restart loop. */
    private var failures = 0

    val elapsedMinutes: Int
        get() {
            val start = startedAt ?: return 0
            return maxOf(1, ((System.currentTimeMillis() - start) / 60_000).toInt())
        }

    fun markDenied() { state = State.denied }

    fun start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            state = State.unavailable
            return
        }
        val r = runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
        if (r == null) {
            state = State.unavailable
            return
        }
        r.setRecognitionListener(listener)
        recognizer = r
        transcript = ""
        committed = ""
        failures = 0
        startedAt = System.currentTimeMillis()
        state = State.recording
        listen()
    }

    /** Stops and returns the full transcript. */
    fun stop(): String {
        release()
        state = State.idle
        return transcript
    }

    private fun release() {
        handler.removeCallbacksAndMessages(null)
        recognizer?.let { r ->
            runCatching { r.cancel() }
            runCatching { r.destroy() }
        }
        recognizer = null
    }

    private fun listen() {
        if (state != State.recording) return
        runCatching { recognizer?.startListening(intent()) }.onFailure { fail() }
    }

    private fun intent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
    }

    private fun joined(text: String): String = if (committed.isEmpty()) text else "$committed $text"

    /** Each session ends at a pause, which is where a sentence ends. */
    private fun asSentence(text: String): String {
        val t = text.trim().replaceFirstChar { it.uppercase() }
        if (t.isEmpty()) return t
        return if (t.last() in ".!?") t else "$t."
    }

    private fun restart(delayMillis: Long = 60) {
        handler.postDelayed({ listen() }, delayMillis)
    }

    private fun fail() {
        failures++
        if (failures >= 3) {
            // Recognition keeps failing straight away. Stop, and keep what we have.
            release()
            state = State.unavailable
        } else {
            runCatching { recognizer?.cancel() }
            restart(300)
        }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onPartialResults(partialResults: Bundle?) {
            if (state != State.recording) return
            val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!text.isNullOrBlank()) transcript = joined(text.trim())
        }

        override fun onResults(results: Bundle?) {
            if (state != State.recording) return
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!text.isNullOrBlank()) {
                transcript = joined(asSentence(text))
                failures = 0
            } else if (transcript != committed) {
                // Keep the words shown from the last partial result.
                transcript = joined(asSentence(transcript.removePrefix(committed)))
            }
            committed = transcript
            // Keep listening with a fresh session.
            restart()
        }

        override fun onError(error: Int) {
            if (state != State.recording) return
            when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    release()
                    state = State.denied
                }
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    // A quiet moment in the talk. Keep anything shown, then listen again.
                    if (transcript != committed) {
                        transcript = joined(asSentence(transcript.removePrefix(committed)))
                        committed = transcript
                    }
                    restart()
                }
                else -> {
                    if (transcript != committed) {
                        transcript = joined(asSentence(transcript.removePrefix(committed)))
                        committed = transcript
                    }
                    fail()
                }
            }
        }
    }
}
