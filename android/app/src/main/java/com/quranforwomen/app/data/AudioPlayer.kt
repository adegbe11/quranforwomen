package com.quranforwomen.app.data

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class Reciter(
    /** Edition identifier on the Islamic Network audio CDN. */
    val id: String,
    val name: String,
) {
    companion object {
        val all = listOf(
            Reciter("ar.alafasy", "Mishary Rashid Alafasy"),
            Reciter("ar.husary", "Mahmoud Khalil Al-Husary"),
            Reciter("ar.minshawi", "Mohamed Siddiq Al-Minshawi"),
        )

        fun named(id: String): Reciter = all.firstOrNull { it.id == id } ?: all[0]
    }
}

sealed class RepeatMode {
    data object Off : RepeatMode()
    data class Times(val n: Int) : RepeatMode()
    data object Forever : RepeatMode()

    val title: String
        get() = when (this) {
            Off -> "Off"
            is Times -> "$n times"
            Forever -> "Until I stop"
        }
}

/** Verse by verse recitation with repeat, follow along, and lock screen controls. */
class AudioPlayer(
    private val context: Context,
    private val quran: QuranStore,
    private val prefs: Prefs,
    private val scope: CoroutineScope,
) {
    var current by mutableStateOf<VerseRef?>(null); private set
    var isPlaying by mutableStateOf(false); private set
    var isLoading by mutableStateOf(false); private set
    var didFail by mutableStateOf(false); private set
    /** How far through the current verse the recitation is, 0 to 1. */
    var itemProgress by mutableFloatStateOf(0f); private set
    /** Last verse to play. Null means the end of the surah. */
    var stopAfter by mutableStateOf<Int?>(null); private set

    var repeatMode by mutableStateOf<RepeatMode>(RepeatMode.Off)
        private set

    fun updateRepeatMode(mode: RepeatMode) {
        repeatMode = mode
        playsRemaining = plays(mode)
    }

    private var reciterIdState by mutableStateOf(prefs.sp.getString(PrefKey.reciter, null) ?: Reciter.all[0].id)
    var reciterID: String
        get() = reciterIdState
        set(value) {
            reciterIdState = value
            prefs.sp.edit().putString(PrefKey.reciter, value).apply()
            current?.let { load(it, autoplay = isPlaying) }
        }
    val reciter: Reciter get() = Reciter.named(reciterID)

    /** Recitation speed, remembered. */
    var speed by mutableFloatStateOf(prefs.sp.getFloat(PlaybackPrefs.speed, 1f).takeIf { it in PlaybackSpeed.choices } ?: 1f)
        private set

    fun updateSpeed(value: Float) {
        speed = value
        prefs.sp.edit().putFloat(PlaybackPrefs.speed, value).apply()
        player.setPlaybackSpeed(value)
    }

    /** The sleep timer, if one is set. */
    var sleepTimer by mutableStateOf<SleepTimer?>(null); private set
    /** When a timed sleep timer ends, in elapsed realtime milliseconds. */
    var sleepEndsAt by mutableStateOf<Long?>(null); private set
    private var sleepJob: Job? = null

    fun setSleep(timer: SleepTimer?) {
        sleepJob?.cancel()
        player.volume = 1f
        sleepTimer = timer
        sleepEndsAt = null
        if (timer is SleepTimer.Minutes) {
            val ends = android.os.SystemClock.elapsedRealtime() + timer.minutes * 60_000L
            sleepEndsAt = ends
            sleepJob = scope.launch {
                while (isActive) {
                    val left = ends - android.os.SystemClock.elapsedRealtime()
                    if (left <= 0) break
                    delay(minOf(left, 1_000L))
                }
                // Pause gently: the voice fades over three seconds.
                for (step in 1..30) {
                    player.volume = 1f - step / 30f
                    delay(100)
                }
                pause()
                player.volume = 1f
                sleepTimer = null
                sleepEndsAt = null
            }
        }
    }

    /** A range of verses that plays again from its start, loopsRemaining more times. */
    var loopRange by mutableStateOf<IntRange?>(null); private set
    var loopMode by mutableStateOf<RepeatMode>(RepeatMode.Off); private set
    private var loopsRemaining = 1

    /** Plays verses from one to another within a surah, then the whole range again. */
    fun repeatRange(surah: Int, from: Int, to: Int, mode: RepeatMode) {
        val start = minOf(from, to)
        val end = maxOf(from, to)
        updateRepeatMode(RepeatMode.Off)
        play(VerseRef(surah, start), through = end)
        if (current != VerseRef(surah, start)) return
        loopRange = start..end
        loopMode = mode
        loopsRemaining = plays(mode)
    }

    /** Stops repeating the range and carries on to the end of the surah. */
    fun clearLoop() {
        loopRange = null
        stopAfter = null
    }

    private var playsRemaining = 1
    private var progressJob: Job? = null

    /** The one ExoPlayer, shared with PlaybackService for the lock screen and headphones. */
    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()
        .apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    this@AudioPlayer.isPlaying = playing
                    if (playing) startProgress() else progressJob?.cancel()
                }

                override fun onPlaybackStateChanged(state: Int) {
                    this@AudioPlayer.isLoading = state == Player.STATE_BUFFERING
                    if (state == Player.STATE_ENDED) itemFinished()
                }

                override fun onPlayerError(error: PlaybackException) {
                    this@AudioPlayer.didFail = true
                    this@AudioPlayer.isLoading = false
                }
            })
        }

    /** Plays a verse. By default continues through the rest of the surah. */
    fun play(ref: VerseRef, through: Int? = null) {
        if (!quran.contains(ref)) return
        loopRange = null
        stopAfter = through
        playsRemaining = plays(repeatMode)
        load(ref, autoplay = true)
    }

    /** Plays a range of whole verses, for example a dua, then stops. */
    fun playRange(range: Dua.VerseRange) {
        updateRepeatMode(RepeatMode.Off)
        play(VerseRef(range.surah, range.from), through = range.to)
    }

    /** Plays a single verse a number of times. */
    fun repeatVerse(ref: VerseRef, mode: RepeatMode) {
        updateRepeatMode(mode)
        play(ref, through = ref.ayah)
    }

    fun togglePlayPause() { if (isPlaying) pause() else resume() }

    fun pause() { player.pause() }

    fun resume() {
        val ref = current ?: return
        if (player.currentMediaItem == null || didFail) load(ref, autoplay = true) else player.play()
    }

    fun next() {
        val cur = current ?: return
        val next = VerseRef(cur.surah, cur.ayah + 1)
        if (!quran.contains(next)) return
        playsRemaining = plays(repeatMode)
        stopAfter?.let { if (next.ayah > it) stopAfter = null }
        loopRange?.let { if (next.ayah > it.last) loopRange = null }
        load(next, autoplay = true)
    }

    fun previous() {
        val cur = current ?: return
        val prev = VerseRef(cur.surah, cur.ayah - 1)
        if (!quran.contains(prev)) { player.seekTo(0); return }
        playsRemaining = plays(repeatMode)
        load(prev, autoplay = true)
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        current = null
        stopAfter = null
        didFail = false
        itemProgress = 0f
        loopRange = null
        sleepJob?.cancel()
        sleepTimer = null
        sleepEndsAt = null
        player.volume = 1f
    }

    fun isCurrent(ref: VerseRef) = current == ref

    private fun plays(mode: RepeatMode): Int = when (mode) {
        RepeatMode.Off -> 1
        is RepeatMode.Times -> maxOf(1, mode.n)
        RepeatMode.Forever -> Int.MAX_VALUE
    }

    private fun load(ref: VerseRef, autoplay: Boolean) {
        current = ref
        didFail = false
        itemProgress = 0f
        ensureService()
        val surah = quran.surah(ref.surah)
        // A downloaded surah plays from the phone.
        val local = RecitationDownloads.localFile(context, reciterID, ref.surah, ref.ayah)
        val uri = if (local.exists() && local.length() > 0) android.net.Uri.fromFile(local).toString()
        else "https://cdn.islamic.network/quran/audio/128/$reciterID/${quran.globalNumber(ref)}.mp3"
        player.setPlaybackSpeed(speed)
        val item = MediaItem.Builder()
            .setUri(uri)
            .setMediaId(ref.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("${surah.name} · Verse ${ref.ayah}")
                    .setArtist(reciter.name)
                    .setAlbumTitle("Quran for Women")
                    .build()
            )
            .build()
        player.setMediaItem(item)
        player.prepare()
        player.playWhenReady = autoplay
    }

    private fun itemFinished() {
        val cur = current ?: return
        val last = stopAfter ?: quran.surah(cur.surah).verseCount
        // The sleep timer set to the end of the surah stops once it has played through.
        if (sleepTimer == SleepTimer.EndOfSurah && cur.ayah >= last) {
            stop()
            return
        }
        if (playsRemaining > 1) {
            if (playsRemaining != Int.MAX_VALUE) playsRemaining--
            player.seekTo(0)
            player.play()
            return
        }
        val loop = loopRange
        if (cur.ayah < last) {
            playsRemaining = plays(repeatMode)
            load(VerseRef(cur.surah, cur.ayah + 1), autoplay = true)
        } else if (loop != null && loopsRemaining > 1) {
            if (loopsRemaining != Int.MAX_VALUE) loopsRemaining--
            playsRemaining = plays(repeatMode)
            load(VerseRef(cur.surah, loop.first), autoplay = true)
        } else {
            stop()
        }
    }

    private fun startProgress() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val duration = player.duration
                if (duration > 0) itemProgress = (player.currentPosition.toFloat() / duration).coerceIn(0f, 1f)
                delay(100)
            }
        }
    }

    private var serviceStarted = false

    /** Connecting a controller starts PlaybackService, which shows the lock screen controls. */
    private fun ensureService() {
        if (serviceStarted) return
        serviceStarted = true
        val token = androidx.media3.session.SessionToken(context, ComponentName(context, PlaybackService::class.java))
        androidx.media3.session.MediaController.Builder(context, token).buildAsync()
    }
}

/** Hosts the media session so recitation keeps going with the screen off, with lock screen controls. */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val audio = com.quranforwomen.app.AppGraph.get(this).player
        // The player holds one verse at a time, so the lock screen and headphone buttons for
        // next and previous are routed to the app's own verse by verse steps.
        val forwarding = object : androidx.media3.common.ForwardingPlayer(audio.player) {
            override fun getAvailableCommands(): Player.Commands =
                super.getAvailableCommands().buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT).add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS).add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .build()

            override fun isCommandAvailable(command: Int): Boolean =
                command == Player.COMMAND_SEEK_TO_NEXT || command == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM ||
                    command == Player.COMMAND_SEEK_TO_PREVIOUS || command == Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM ||
                    super.isCommandAvailable(command)

            override fun seekToNext() = audio.next()
            override fun seekToNextMediaItem() = audio.next()
            override fun seekToPrevious() = audio.previous()
            override fun seekToPreviousMediaItem() = audio.previous()
        }
        session = MediaSession.Builder(this, forwarding).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.run {
            // The player belongs to the app, so only the session is released here.
            release()
        }
        session = null
        super.onDestroy()
    }
}
