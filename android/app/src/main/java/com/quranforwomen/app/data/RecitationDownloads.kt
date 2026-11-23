package com.quranforwomen.app.data

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

/**
 * Keeps a surah's recitation on the phone for offline listening, one reciter at a time.
 * Files come from the same audio CDN the player streams from and live in no-backup storage.
 */
class RecitationDownloads(
    private val context: Context,
    private val quran: QuranStore,
    private val scope: CoroutineScope,
) {
    enum class State { NotDownloaded, Downloading, Ready, Failed }

    data class Item(val reciterID: String, val surah: Int)

    private val states = mutableStateMapOf<String, State>()
    private val progresses = mutableStateMapOf<String, Float>()
    private val sizes = mutableStateMapOf<String, Long>()
    private val jobs = HashMap<String, Job>()

    init {
        // A surah counts as downloaded once every verse arrived and the marker was written.
        root(context).listFiles()?.forEach { reciterDir ->
            reciterDir.listFiles()?.forEach { surahDir ->
                val surah = surahDir.name.toIntOrNull()
                if (surah != null && File(surahDir, MARKER).exists()) {
                    states[key(reciterDir.name, surah)] = State.Ready
                }
            }
        }
    }

    fun state(reciterID: String, surah: Int): State = states[key(reciterID, surah)] ?: State.NotDownloaded

    /** How much of a download in progress has arrived, 0 to 1. */
    fun progress(reciterID: String, surah: Int): Float = progresses[key(reciterID, surah)] ?: 0f

    /** Every downloaded surah, grouped by reciter in the app's reciter order. */
    val downloaded: List<Item>
        get() = states.filterValues { it == State.Ready }.keys
            .mapNotNull { k ->
                val parts = k.split("/")
                parts.getOrNull(1)?.toIntOrNull()?.let { Item(parts[0], it) }
            }
            .sortedWith(compareBy({ item -> Reciter.all.indexOfFirst { it.id == item.reciterID } }, { it.surah }))

    fun download(reciterID: String, surah: Int) {
        val k = key(reciterID, surah)
        if (state(reciterID, surah) == State.Downloading || state(reciterID, surah) == State.Ready) return
        states[k] = State.Downloading
        progresses[k] = 0f
        jobs[k] = scope.launch {
            try {
                val total = quran.surah(surah).verseCount
                val first = quran.globalNumber(VerseRef(surah, 1))
                val dir = surahDir(context, reciterID, surah)
                withContext(Dispatchers.IO) {
                    dir.mkdirs()
                    val done = AtomicInteger(0)
                    val gate = Semaphore(4)
                    coroutineScope {
                        (1..total).map { ayah ->
                            async {
                                gate.withPermit {
                                    val target = File(dir, "$ayah.mp3")
                                    if (!target.exists() || target.length() == 0L) {
                                        fetch(
                                            "https://cdn.islamic.network/quran/audio/128/$reciterID/${first + ayah - 1}.mp3",
                                            target,
                                        )
                                    }
                                    val n = done.incrementAndGet()
                                    withContext(Dispatchers.Main) { progresses[k] = n.toFloat() / total }
                                }
                            }
                        }.awaitAll()
                    }
                    File(dir, MARKER).writeText("ok")
                }
                sizes[k] = withContext(Dispatchers.IO) { folderSize(dir) }
                states[k] = State.Ready
            } catch (e: CancellationException) {
                withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) {
                    surahDir(context, reciterID, surah).deleteRecursively()
                }
                states[k] = State.NotDownloaded
                throw e
            } catch (e: Exception) {
                states[k] = State.Failed
            } finally {
                progresses.remove(k)
                jobs.remove(k)
            }
        }
    }

    fun cancel(reciterID: String, surah: Int) {
        jobs[key(reciterID, surah)]?.cancel()
    }

    fun delete(reciterID: String, surah: Int) {
        val k = key(reciterID, surah)
        jobs[k]?.cancel()
        surahDir(context, reciterID, surah).deleteRecursively()
        states[k] = State.NotDownloaded
        sizes.remove(k)
    }

    fun sizeOnDisk(reciterID: String, surah: Int): Long? {
        val k = key(reciterID, surah)
        if (state(reciterID, surah) != State.Ready) return null
        sizes[k]?.let { return it }
        val size = folderSize(surahDir(context, reciterID, surah))
        sizes[k] = size
        return size
    }

    private fun fetch(url: String, target: File) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
        }
        try {
            if (connection.responseCode != 200) error("Bad response")
            val tmp = File(target.parentFile, "${target.name}.tmp")
            connection.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            if (tmp.length() == 0L) { tmp.delete(); error("Empty file") }
            if (!tmp.renameTo(target)) { target.delete(); tmp.renameTo(target) }
        } finally {
            connection.disconnect()
        }
    }

    private fun folderSize(dir: File): Long = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    private fun key(reciterID: String, surah: Int) = "$reciterID/$surah"

    companion object {
        private const val MARKER = ".complete"

        /** Recitations re-download, so they live in no-backup storage. */
        private fun root(context: Context) = File(context.noBackupFilesDir, "recitations")

        private fun surahDir(context: Context, reciterID: String, surah: Int) = File(File(root(context), reciterID), "$surah")

        /** The saved recitation of one verse. It may not exist. */
        fun localFile(context: Context, reciterID: String, surah: Int, ayah: Int): File =
            File(surahDir(context, reciterID, surah), "$ayah.mp3")
    }
}
