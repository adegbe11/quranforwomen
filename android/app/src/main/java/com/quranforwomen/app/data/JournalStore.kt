package com.quranforwomen.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID

@Serializable
enum class JournalMood(val title: String) {
    grateful("Grateful"), peaceful("Peaceful"), hopeful("Hopeful"), makingDua("Making dua"), heavy("Heavy"), anxious("Anxious");

    /** The editor softens to a deeper leaf on heavy days. */
    val isHeavy: Boolean get() = this == heavy || this == anxious
}

/** Where a dua is now. Every sincere dua is heard. */
@Serializable
enum class DuaStage(val title: String) {
    asking("Making dua"), waiting("Waiting with patience"), takingAStep("Taking a step"),
    answered("Answered"), answeredDifferently("Answered differently");

    val isAnswered: Boolean get() = this == answered || this == answeredDifferently
}

/** Every field has a default, so a page saved by any version always opens. */
@Serializable
data class JournalEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: Long = System.currentTimeMillis(),
    val updated: Long = System.currentTimeMillis(),
    val title: String = "",
    /** Light markdown: "## Heading" lines, "- " bullets, "- [ ]" checklists. */
    val body: String = "",
    val mood: JournalMood? = null,
    val gratitude: String = "",
    val verse: VerseRef? = null,
    val tags: List<String> = emptyList(),
    val favorite: Boolean = false,
    /** The template or prompt that started this page, shown as a small label. */
    val origin: String? = null,
    // Dua journey
    val isDua: Boolean = false,
    val duaStage: DuaStage = DuaStage.asking,
    val nextStep: String = "",
    val answeredNote: String = "",
    val answeredAt: Long? = null,
) {
    val isEmpty: Boolean get() = title.isBlank() && body.isBlank() && gratitude.isBlank()

    val localDate: LocalDate get() = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate()

    val displayTitle: String
        get() {
            if (title.isNotEmpty()) return title
            body.split("\n").firstOrNull { !it.startsWith("##") && it.isNotEmpty() }?.let { return it.take(60) }
            return mood?.title ?: "Journal page"
        }

    /** Plain text for sharing and export. */
    val plainText: String
        get() {
            val parts = mutableListOf(localDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)))
            if (title.isNotEmpty()) parts += title
            if (body.isNotEmpty()) parts += body.replace("## ", "")
            if (gratitude.isNotEmpty()) parts += "Grateful for: $gratitude"
            verse?.let { parts += "Verse: Quran ${it.id}" }
            if (isDua && duaStage.isAnswered && answeredNote.isNotEmpty()) parts += "How Allah answered: $answeredNote"
            return parts.joinToString("\n\n")
        }
}

/** Notes from a khutbah, halaqa, class or lecture. */
@Serializable
data class LessonNote(
    val id: String = UUID.randomUUID().toString(),
    val kind: Kind = Kind.khutbah,
    val title: String = "",
    val speaker: String = "",
    val date: Long = System.currentTimeMillis(),
    val verses: List<VerseRef> = emptyList(),
    val points: List<String> = emptyList(),
    val quotes: List<String> = emptyList(),
    val takeaway: String = "",
    val notes: String = "",
    val transcript: String = "",
    val minutes: Int = 0,
    val pinned: Boolean = false,
    val tags: List<String> = emptyList(),
) {
    @Serializable
    enum class Kind(val title: String) {
        khutbah("Khutbah"), halaqa("Halaqa"), classNote("Class"), lecture("Lecture"), study("Personal study");
    }

    val displayTitle: String get() = title.ifEmpty { "${kind.title} notes" }

    val plainText: String
        get() {
            val parts = mutableListOf(displayTitle)
            val day = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate()
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            parts += listOfNotNull(kind.title, speaker.ifEmpty { null }, day).joinToString(" · ")
            if (verses.isNotEmpty()) parts += "Verses: " + verses.joinToString(", ") { "Quran ${it.id}" }
            if (points.isNotEmpty()) parts += "Main points:\n" + points.mapIndexed { i, p -> "${i + 1}. $p" }.joinToString("\n")
            if (quotes.isNotEmpty()) parts += "Lines worth keeping:\n" + quotes.joinToString("\n") { "\"$it\"" }
            if (takeaway.isNotEmpty()) parts += "Carry this into my week: $takeaway"
            if (notes.isNotEmpty()) parts += notes
            return parts.joinToString("\n\n")
        }
}

/** The journal and lesson notes. Saved as a file on this phone only, never backed up or uploaded. */
class JournalStore(private val context: Context, private val prefs: Prefs, private val scope: CoroutineScope) {
    @Serializable
    private data class Snapshot(
        val entries: List<JournalEntry> = emptyList(),
        val notes: List<LessonNote> = emptyList(),
        val recentTemplates: List<String> = emptyList(),
    )

    var entries by mutableStateOf<List<JournalEntry>>(emptyList()); private set
    var notes by mutableStateOf<List<LessonNote>>(emptyList()); private set
    var recentTemplates by mutableStateOf<List<String>>(emptyList()); private set
    /** The page saved a moment ago. The list gives it a soft arrival glow. */
    var justSaved by mutableStateOf<String?>(null); private set
    /** True after fingerprint, face or screen lock in this session. Never saved. */
    var isUnlocked by mutableStateOf(false)

    private var lockState by mutableStateOf(prefs.sp.getBoolean(PrefKey.journalLock, false))
    /** Whether she turned the journal lock on. Saved. */
    var lockEnabled: Boolean
        get() = lockState
        set(value) { lockState = value; prefs.sp.edit().putBoolean(PrefKey.journalLock, value).apply() }

    /** Whether journal content may be shown right now. */
    val isAccessible: Boolean get() = isUnlocked || !lockEnabled

    fun relock() { if (lockEnabled) isUnlocked = false }

    /** Internal app storage: private to the app and encrypted by Android. Excluded from backup. */
    private val file = File(File(context.filesDir, "journal"), "journal.json")

    init { load() }

    private fun load() {
        if (!file.exists()) return
        val text = runCatching { file.readText(Charsets.UTF_8) }.getOrNull() ?: return
        val snapshot = runCatching { AppJson.decodeFromString(Snapshot.serializer(), text) }.getOrNull()
        if (snapshot != null) {
            entries = snapshot.entries
            notes = snapshot.notes
            recentTemplates = snapshot.recentTemplates
        } else {
            // Unreadable format: keep a copy instead of overwriting it.
            file.renameTo(File(file.parentFile, "journal-backup-${System.currentTimeMillis() / 1000}.json"))
        }
    }

    // Entries

    /** Newest first. */
    val sortedEntries: List<JournalEntry> get() = entries.sortedByDescending { it.date }

    fun entry(id: String): JournalEntry? = entries.firstOrNull { it.id == id }

    fun save(entry: JournalEntry) {
        var e = entry.copy(updated = System.currentTimeMillis())
        e = if (e.isDua && e.duaStage.isAnswered) {
            if (e.answeredAt == null) e.copy(answeredAt = System.currentTimeMillis()) else e
        } else e.copy(answeredAt = null)
        entries = if (entries.any { it.id == e.id }) entries.map { if (it.id == e.id) e else it } else entries + e
        persist()
        justSaved = e.id
        val saved = e.id
        scope.launch {
            delay(2500)
            if (justSaved == saved) justSaved = null
        }
    }

    fun delete(entry: JournalEntry) { entries = entries.filter { it.id != entry.id }; persist() }

    fun toggleFavorite(entry: JournalEntry) {
        val current = entry(entry.id) ?: return
        save(current.copy(favorite = !current.favorite))
    }

    val lastEdited: JournalEntry? get() = entries.maxByOrNull { it.updated }

    /** A page from a year ago, or else a month ago. */
    val onThisDay: JournalEntry?
        get() {
            val today = LocalDate.now()
            for (months in listOf(12L, 1L)) {
                val past = today.minusMonths(months)
                sortedEntries.firstOrNull { it.localDate == past }?.let { return it }
            }
            return null
        }

    val allTags: List<String>
        get() = entries.flatMap { it.tags }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.map { it.key }

    fun markTemplateUsed(id: String) {
        recentTemplates = (listOf(id) + recentTemplates.filter { it != id }).take(6)
        persist()
    }

    // Notes

    fun note(id: String): LessonNote? = notes.firstOrNull { it.id == id }

    fun save(note: LessonNote) {
        notes = if (notes.any { it.id == note.id }) notes.map { if (it.id == note.id) note else it } else notes + note
        persist()
    }

    fun delete(note: LessonNote) { notes = notes.filter { it.id != note.id }; persist() }

    /** Pinned first, then newest. */
    val sortedNotes: List<LessonNote>
        get() = notes.sortedWith(compareByDescending<LessonNote> { it.pinned }.thenByDescending { it.date })

    // Export and erase

    /** The whole journal and notes as one plain text file in the share cache. */
    fun exportFile(): File? = runCatching {
        val sb = StringBuilder("Quran for Women · Journal\nExported ${LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))}\n\n")
        for (e in sortedEntries) sb.append(e.plainText).append("\n\n----------\n\n")
        if (notes.isNotEmpty()) {
            sb.append("LESSON NOTES\n\n")
            for (n in sortedNotes) sb.append(n.plainText).append("\n\n----------\n\n")
        }
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        File(dir, "My Journal.txt").apply { writeText(sb.toString(), Charsets.UTF_8) }
    }.getOrNull()

    fun eraseAll() {
        entries = emptyList()
        notes = emptyList()
        recentTemplates = emptyList()
        file.delete()
    }

    private fun persist() {
        val snapshot = Snapshot(entries, notes, recentTemplates)
        val text = AppJson.encodeToString(Snapshot.serializer(), snapshot)
        runCatching {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, "journal.tmp")
            tmp.writeText(text, Charsets.UTF_8)
            if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
        }
    }
}
