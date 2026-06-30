package com.zaaam.Zmusic.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.zaaam.Zmusic.R
import com.zaaam.Zmusic.data.MusicRepository
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.util.AudioSessionHolder
import com.zaaam.Zmusic.util.QueueManager
import com.zaaam.Zmusic.util.RepeatMode
import com.zaaam.Zmusic.util.SleepTimerManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import com.zaaam.Zmusic.model.entity.PlaylistWithSongs
import com.zaaam.Zmusic.model.entity.toSong
import com.zaaam.Zmusic.widget.ZmusicWidgetProvider
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject lateinit var queueManager: QueueManager
    @Inject lateinit var repository: MusicRepository
    @Inject lateinit var sleepTimerManager: SleepTimerManager
    @Inject lateinit var audioSessionHolder: AudioSessionHolder
    @Inject lateinit var equalizerManager: com.zaaam.Zmusic.util.EqualizerManager

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    // ── Coroutine Scopes ────────────────────────────────────────────────────
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // FIX BUG #3: recordingScope terpisah dari serviceScope.
    // Coroutine recordPlay() TIDAK ikut ter-cancel saat serviceScope.cancel()
    // dipanggil di onDestroy() — mencegah data loss play history saat lagu terakhir
    // di queue selesai dan service langsung di-destroy.
    private val recordingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var currentPlayJob: Job? = null
    private var errorRetryJob: Job? = null

    // ── FIX BUG #1: WiFi Lock dikelola manual ─────────────────────────────
    private var wifiLock: WifiManager.WifiLock? = null

    // ── FIX BUG #2: Custom AudioFocusChangeListener ────────────────────────
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wasPlayingBeforeFocusLoss = false
    private var isAudioFocusHeld = false

    private var isIntendingToPlay = false

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "AudioFocus: GAIN — wasPlayingBefore=$wasPlayingBeforeFocusLoss")
                isAudioFocusHeld = true
                player.volume = 1f
                if (wasPlayingBeforeFocusLoss) {
                    player.play()
                    wasPlayingBeforeFocusLoss = false
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "AudioFocus: LOSS permanen — isIntendingToPlay=$isIntendingToPlay")
                isAudioFocusHeld = false
                // BUG FIX: Jangan reset wasPlayingBeforeFocusLoss kalau kita sedang
                // dalam proses load lagu berikutnya (isIntendingToPlay=true).
                // Tanpa ini: OPPO kadang kirim AUDIOFOCUS_LOSS saat lagu habis →
                // wasPlayingBeforeFocusLoss=false → lagu berikutnya tidak auto-play.
                if (!isIntendingToPlay) {
                    wasPlayingBeforeFocusLoss = false
                }
                isIntendingToPlay = false
                if (player.isPlaying || player.playWhenReady) player.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "AudioFocus: LOSS_TRANSIENT — isPlaying=${player.isPlaying}")
                wasPlayingBeforeFocusLoss = player.isPlaying || player.playWhenReady || isIntendingToPlay
                isIntendingToPlay = false
                if (player.isPlaying || player.playWhenReady) player.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "AudioFocus: LOSS_TRANSIENT_CAN_DUCK — ducking volume")
                player.volume = 0.3f
            }
        }
    }

    // ── Thread-Safety Flags ─────────────────────────────────────────────────
    private val isHandlingError = AtomicBoolean(false)

    // ── Playback State ──────────────────────────────────────────────────────
    private var currentSong: Song? = null
    private var songStartTime = 0L
    private var accumulatedDuration = 0L
    private var hasRecordedForCurrentSong = false

    private var currentMoodHint: String? = null
    private var currentSourceQuery: String? = null

    // ── Error Retry ─────────────────────────────────────────────────────────
    private var consecutiveErrorCount = 0
    private val MAX_CONSECUTIVE_ERRORS = 3

    // ── URL Cache ────────────────────────────────────────────────────────────
    // Tetap digunakan untuk cache URL agar getCachedUrlOrFetch() bisa skip
    // network call saat URL untuk lagu ini sudah fresh di cache.
    private val prefetchCache = ConcurrentHashMap<String, Pair<String, Long>>()
    private val PREFETCH_TTL_MS = 5 * 60 * 1000L
    private val MAX_PREFETCH_CACHE_SIZE = 10
    private val MAX_FETCH_ATTEMPTS = 3

    // ── GAPLESS PLAYBACK — Spotify Approach ─────────────────────────────────
    //
    // ROOT CAUSE GAP: pendekatan lama (single MediaItem) menyebabkan:
    //   STATE_ENDED → fetch URL (network) → setMediaItem() → prepare() → buffer audio → play()
    //   Ini ciptakan gap 2-5 detik (jaringan bagus) atau 10-15 detik (jaringan lambat).
    //
    // FIX — Spotify Approach:
    //   1. Saat current song mulai play, langsung jadwalkan add MediaItem lagu berikutnya
    //      ke ExoPlayer via player.addMediaItem().
    //   2. ExoPlayer otomatis pre-buffer audio data lagu berikutnya DI BACKGROUND selagi
    //      current song masih berjalan.
    //   3. Saat current song habis, ExoPlayer auto-advance ke lagu berikutnya secara
    //      gapless — TANPA network call saat transisi.
    //   4. onMediaItemTransition(AUTO) menggantikan STATE_ENDED untuk auto-advance:
    //      update currentSong, advance QueueManager index, jadwalkan add lagu berikutnya.
    //   5. STATE_ENDED sekarang hanya berarti SEMUA item di queue ExoPlayer habis.
    //
    // playerQueue: tracks lagu-lagu yang saat ini ada di ExoPlayer's internal queue.
    //   Index 0 = lagu yang sedang dimainkan.
    //   Ini harus diupdate sync dengan operasi player.setMediaItem/addMediaItem/clearMediaItems.
    //   HANYA diakses dari Main thread (serviceScope = Dispatchers.Main).
    private val playerQueue = ArrayDeque<Song>()
    private var addNextJob: Job? = null

    // ── Notification ────────────────────────────────────────────────────────
    companion object {
        private const val TAG = "ZmusicService"
        private const val FOREGROUND_NOTIF_ID  = 1
        private const val PLAYBACK_CHANNEL_ID  = "zmusic_playback"

        // Widget actions
        const val ACTION_PLAY_PLAYLIST      = "com.zaaam.Zmusic.ACTION_PLAY_PLAYLIST"
        const val ACTION_TOGGLE_PLAY_PAUSE  = "com.zaaam.Zmusic.ACTION_TOGGLE_PLAY_PAUSE"
        const val ACTION_NEXT               = "com.zaaam.Zmusic.ACTION_NEXT"
        const val EXTRA_PLAYLIST_ID         = "playlist_id"

        // Widget now playing broadcast
        const val ACTION_UPDATE_WIDGET  = "com.zaaam.Zmusic.ACTION_UPDATE_WIDGET"
        const val EXTRA_SONG_TITLE      = "song_title"
        const val EXTRA_SONG_ARTIST     = "song_artist"
        const val EXTRA_IS_PLAYING      = "is_playing"
    }

    // ════════════════════════════════════════════════════════════════════════
    // broadcastWidgetUpdate — kirim state Now Playing ke widget
    // ════════════════════════════════════════════════════════════════════════

    private fun broadcastWidgetUpdate(song: Song?, isPlaying: Boolean) {
        val intent = Intent(this, ZmusicWidgetProvider::class.java).apply {
            action = ACTION_UPDATE_WIDGET
            putExtra(EXTRA_SONG_TITLE,  song?.title  ?: "")
            putExtra(EXTRA_SONG_ARTIST, song?.artist ?: "")
            putExtra(EXTRA_IS_PLAYING,  isPlaying)
        }
        sendBroadcast(intent)
    }

    // ════════════════════════════════════════════════════════════════════════
    // onStartCommand — handle widget actions
    // ════════════════════════════════════════════════════════════════════════

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)

        when (intent?.action) {

            ACTION_PLAY_PLAYLIST -> {
                val playlistId = intent.getLongExtra(EXTRA_PLAYLIST_ID, -1L)
                if (playlistId == -1L) return result

                serviceScope.launch {
                    try {
                        val playlistWithSongs = withContext(Dispatchers.IO) {
                            repository.getPlaylistWithSongs(playlistId).first()
                        }
                        val songs = playlistWithSongs.songs.map { it.toSong() }
                        if (songs.isEmpty()) return@launch

                        queueManager.setQueue(
                            songs       = songs,
                            startIndex  = 0,
                            sourceQuery = "widget_playlist"
                        )
                        queueManager.requestPlay(songs[0], userInitiated = true)
                    } catch (e: Exception) {
                        // Playlist kosong atau DB error — tap widget jadi no-op,
                        // minimal tinggalkan jejak di logcat (aturan emas: jangan telan error)
                        Log.e(TAG, "ACTION_PLAY_PLAYLIST gagal untuk playlistId=$playlistId", e)
                    }
                }
            }

            ACTION_TOGGLE_PLAY_PAUSE -> {
                if (player.isPlaying) player.pause()
                else if (requestAudioFocus()) player.play()
            }

            ACTION_NEXT -> {
                val nextSong = queueManager.next()
                if (nextSong != null) playSong(nextSong)
            }
        }

        return result
    }

    // ════════════════════════════════════════════════════════════════════════
    // onCreate
    // ════════════════════════════════════════════════════════════════════════

    override fun onCreate() {
        super.onCreate()

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // ── Setup Notification Channel ──────────────────────────────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PLAYBACK_CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Kontrol pemutaran musik Zmusic"
                setShowBadge(false)
            }
            val notifManager = getSystemService(NotificationManager::class.java)
            notifManager.createNotificationChannel(channel)
        }

        // ── FIX BUG #8: startForeground() SEGERA di onCreate() ─────────────
        val placeholderNotif = NotificationCompat.Builder(this, PLAYBACK_CHANNEL_ID)
            .setContentTitle("Zmusic")
            .setContentText("Mempersiapkan pemutaran...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOngoing(true)
            .build()
        startForeground(FOREGROUND_NOTIF_ID, placeholderNotif)

        // ── Build ExoPlayer ─────────────────────────────────────────────────
        player = ExoPlayer.Builder(this)
            // FIX BUG LAG: Custom DefaultLoadControl dengan nilai eksplisit.
            // Sebelumnya mengandalkan default Media3 yang berbeda tiap versi.
            // Dengan explicit config:
            //   minBufferMs = 30s  → ExoPlayer selalu jaga minimal 30s audio ter-buffer
            //   maxBufferMs = 60s  → batas atas buffer agar tidak makan terlalu banyak RAM
            //   bufferForPlaybackMs = 2.5s → mulai play setelah 2.5s data tersedia
            //   bufferForPlaybackAfterRebufferMs = 5s → setelah rebuffer (koneksi putus),
            //     tunggu 5s buffer dulu baru resume — mencegah langsung rebuffer lagi
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        30_000,  // minBufferMs
                        60_000,  // maxBufferMs
                        2_500,   // bufferForPlaybackMs
                        5_000    // bufferForPlaybackAfterRebufferMs
                    )
                    .build()
            )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                // FIX BUG #2: false → kita tangani audio focus sendiri
                false
            )
            .setHandleAudioBecomingNoisy(true)
            // FIX BUG #1: Aktifkan PARTIAL_WAKE_LOCK agar CPU tidak sleep saat layar mati
            .setWakeMode(PowerManager.PARTIAL_WAKE_LOCK)
            .build()

        // ── HEMAT BATERAI (mode muxed fallback) ─────────────────────────────
        // Sejak fallback ke muxed stream (video+audio itag 18), ExoPlayer ikut
        // men-decode track video 360p yang tidak pernah ditonton. Matikan
        // track video sepenuhnya: hanya audio yang di-decode. Tidak berdampak
        // pada stream audio-only (yang memang tidak punya track video).
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            .build()

        // FIX BUG #1: Akuisisi WifiLock secara manual
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(
            WifiManager.WIFI_MODE_FULL_HIGH_PERF,
            "Zmusic::WifiLock"
        ).also { it.acquire() }

        audioSessionHolder.setSessionId(player.audioSessionId)
        // Attach built-in equalizer ke session ExoPlayer yang baru dibuat
        equalizerManager.ensureAttached()
        mediaSession = MediaSession.Builder(this, player).build()

        // ── Player Listener ─────────────────────────────────────────────────
        player.addListener(object : Player.Listener {

            // ── GAPLESS: onMediaItemTransition ─────────────────────────────
            // Menggantikan STATE_ENDED untuk auto-advance lagu.
            //
            // REASON_AUTO   : ExoPlayer selesai play item saat ini dan auto-advance
            //                 ke item berikutnya yang sudah di-buffer (gapless).
            //                 → Update state, advance QueueManager, jadwalkan next.
            // REASON_REPEAT : ExoPlayer repeat item yang sama (RepeatMode.ONE aktif).
            //                 → Hanya reset timing untuk record durasi per play.
            // REASON_PLAYLIST_CHANGED / REASON_SEEK: user action — diabaikan di sini
            //                 karena playSong() sudah handle update state sendiri.
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                when (reason) {
                    Player.MEDIA_ITEM_TRANSITION_REASON_AUTO -> {
                        Log.d(TAG, "MediaItemTransition: REASON_AUTO — gapless berhasil, currentSong=${currentSong?.title}")
                        // Gapless auto-advance berhasil!
                        // Record play dari lagu yang baru selesai (masih currentSong lama).
                        recordCurrentPlay()
                        // BUG FIX: Reset error counter — transisi berhasil berarti
                        // tidak ada error beruntun. Counter yang tidak direset bisa
                        // membuat lagu berikutnya langsung di-skip saat ada error
                        // pertama karena counter sudah "bocor" dari sesi sebelumnya.
                        consecutiveErrorCount = 0
                        isHandlingError.set(false)

                        // Pop lagu yang baru selesai dari playerQueue
                        if (playerQueue.isNotEmpty()) playerQueue.removeFirst()

                        // Advance QueueManager index agar sync dengan ExoPlayer
                        val nextSong = queueManager.next()
                        Log.d(TAG, "MediaItemTransition: REASON_AUTO — nextSong=${nextSong?.title}, index=${queueManager.currentIndex.value}")
                        if (nextSong != null) {
                            currentSong = nextSong
                            currentMoodHint = queueManager.currentMoodHint.value
                            currentSourceQuery = queueManager.currentSourceQuery.value
                            songStartTime = 0L
                            accumulatedDuration = 0L
                            hasRecordedForCurrentSong = false
                            broadcastWidgetUpdate(nextSong, player.isPlaying)

                            // Jadwalkan add lagu berikutnya ke ExoPlayer untuk
                            // gapless transition berikutnya
                            scheduleAddNextToPlayer()
                        }
                        // Jika nextSong null: STATE_ENDED akan segera menyusul
                        // (ExoPlayer exhausted) — ditangani di onPlaybackStateChanged
                    }

                    Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> {
                        // RepeatMode.ONE: lagu yang sama diulang oleh ExoPlayer.
                        // Record durasi play sebelumnya lalu reset untuk play berikutnya.
                        recordCurrentPlay()
                        hasRecordedForCurrentSong = false
                        songStartTime = 0L
                        accumulatedDuration = 0L
                    }
                    // PLAYLIST_CHANGED dan SEEK diabaikan — ditangani oleh playSong()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    Log.d(TAG, "STATE_ENDED — currentSong=${currentSong?.title}, queueSize=${queueManager.queue.value.size}, currentIndex=${queueManager.currentIndex.value}")
                    // STATE_ENDED sekarang hanya berarti seluruh queue ExoPlayer habis.
                    //
                    // Dengan gapless aktif:
                    //   - RepeatMode.OFF di lagu terakhir: scheduleAddNextToPlayer()
                    //     tidak add item baru (peekNextSong() return null) → ExoPlayer
                    //     exhausted → STATE_ENDED fires → stopSelf().
                    //   - RepeatMode.ALL: scheduleAddNextToPlayer() selalu add item berikutnya
                    //     (wrap-around) → STATE_ENDED tidak pernah fires dalam kondisi normal.
                    //     Kalau gagal add (network error), STATE_ENDED fires sebagai fallback
                    //     → queueManager.next() wrap-around → playSong() restart.
                    //   - RepeatMode.ONE: ditangani ExoPlayer native REPEAT_MODE_ONE →
                    //     REASON_REPEAT fires di onMediaItemTransition, bukan STATE_ENDED.
                    recordCurrentPlay()
                    consecutiveErrorCount = 0

                    val next = queueManager.next()
                    Log.d(TAG, "STATE_ENDED — next=${next?.title}, akan ${if (next != null) "playSong" else "stopSelf"}")
                    if (next != null) {
                        // Fallback: gapless gagal add item, tapi masih ada lagu →
                        // restart dengan playSong() (ada gap, tapi playback lanjut)
                        playSong(next, userInitiated = false)
                    } else {
                        abandonAudioFocus()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "onPlayerError — ${error.errorCodeName}: ${error.message}, currentSong=${currentSong?.title}, isHandling=${isHandlingError.get()}")
                if (!isHandlingError.compareAndSet(false, true)) return

                // ── BUG FIX: Identifikasi lagu yang BENAR-BENAR error ──────────────
                //
                // Masalah lama: kode selalu memakai `currentSong` sebagai lagu yang
                // error. Padahal error bisa berasal dari lagu BERIKUTNYA yang sedang
                // di-pre-buffer di background untuk gapless playback.
                //
                // Akibatnya: lagu A main dengan baik → lagu B gagal di-buffer →
                // onPlayerError fires → kode kira A yang error → force-restart A
                // dari posisi tengah → lagu "hiccup" → kalau retry juga gagal →
                // skipNextAuto() → A berhenti sebelum waktunya!
                //
                // Fix: gunakan player.currentMediaItem?.mediaId untuk tau lagu mana
                // yang sedang aktif di ExoPlayer, lalu cocokkan dengan playerQueue.
                val failingMediaId = player.currentMediaItem?.mediaId
                val failingSong = when {
                    failingMediaId == null               -> currentSong
                    failingMediaId == currentSong?.id    -> currentSong
                    // Error berasal dari pre-queued next song
                    else -> playerQueue.firstOrNull { it.id == failingMediaId } ?: currentSong
                }
                val song = failingSong ?: run { isHandlingError.set(false); return }
                val isCurrentSongFailing = (song.id == currentSong?.id)

                consecutiveErrorCount++

                if (consecutiveErrorCount > MAX_CONSECUTIVE_ERRORS) {
                    isHandlingError.set(false)
                    consecutiveErrorCount = 0
                    if (isCurrentSongFailing) {
                        // Current song benar-benar tidak bisa diputar → skip
                        playNextAuto()
                    } else {
                        // Pre-queued next song yang gagal → buang dari queue ExoPlayer,
                        // biarkan current song terus main. STATE_ENDED akan handle fallback.
                        val failIdx = (0 until player.mediaItemCount)
                            .firstOrNull { player.getMediaItemAt(it).mediaId == song.id }
                        if (failIdx != null) {
                            player.removeMediaItem(failIdx)
                            playerQueue.removeAll { it.id == song.id }
                        }
                    }
                    return
                }

                // Posisi hanya relevan untuk current song retry
                val position = if (isCurrentSongFailing) player.currentPosition else 0L

                errorRetryJob = serviceScope.launch {
                    try {
                        val backoffMs = (1000L * (1 shl (consecutiveErrorCount - 1)))
                            .coerceAtMost(5000L)
                        delay(backoffMs)

                        // Invalidate cached URL untuk lagu yang error agar re-fetch fresh
                        prefetchCache.remove(song.id)
                        val freshUrl = repository.getStreamUrl(song.id)

                        if (isCurrentSongFailing) {
                            // ── CURRENT SONG ERROR: retry dengan URL baru ──────────────
                            playerQueue.clear()
                            playerQueue.addLast(song)

                            isIntendingToPlay = true
                            player.setMediaItem(buildMediaItem(song, freshUrl))
                            player.prepare()
                            player.seekTo(position)

                            if (requestAudioFocus()) {
                                isIntendingToPlay = false
                                player.play()
                                Log.d(TAG, "ErrorRetry: berhasil, play lanjut dari pos=$position")
                            } else {
                                isIntendingToPlay = false
                                // BUG FIX: Set wasPlayingBeforeFocusLoss=true agar musik
                                // auto-resume saat audio focus dikembalikan.
                                // Tanpa ini: setelah retry berhasil tapi focus belum
                                // dikembalikan, music tidak akan pernah resume otomatis.
                                wasPlayingBeforeFocusLoss = true
                                Log.w(TAG, "ErrorRetry: audio focus gagal — akan resume saat focus kembali")
                            }

                            consecutiveErrorCount = 0
                            isHandlingError.set(false)

                            // Re-schedule gapless setelah retry berhasil
                            scheduleAddNextToPlayer()
                        } else {
                            // ── PRE-QUEUED NEXT SONG ERROR: replace item di ExoPlayer ─
                            // Lagu yang sedang main TIDAK terganggu sama sekali.
                            val failIdx = (0 until player.mediaItemCount)
                                .firstOrNull { player.getMediaItemAt(it).mediaId == song.id }
                            if (failIdx != null) {
                                player.removeMediaItem(failIdx)
                                playerQueue.removeAll { it.id == song.id }
                                // Re-add dengan URL yang fresh
                                val newItem = buildMediaItem(song, freshUrl)
                                player.addMediaItem(newItem)
                                playerQueue.addLast(song)
                            }
                            consecutiveErrorCount = 0
                            isHandlingError.set(false)
                        }

                    } catch (e: CancellationException) {
                        isIntendingToPlay = false
                        isHandlingError.set(false)
                        throw e
                    } catch (_: Exception) {
                        isIntendingToPlay = false
                        isHandlingError.set(false)
                        if (isCurrentSongFailing) {
                            // Current song benar-benar gagal di-retry → skip
                            playNextAuto()
                        } else {
                            // Pre-queued next song tetap gagal → buang aja.
                            // STATE_ENDED jadi fallback saat current song habis.
                            val failIdx = (0 until player.mediaItemCount)
                                .firstOrNull { player.getMediaItemAt(it).mediaId == song.id }
                            if (failIdx != null) {
                                player.removeMediaItem(failIdx)
                                playerQueue.removeAll { it.id == song.id }
                            }
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    if (songStartTime == 0L) {
                        songStartTime = System.currentTimeMillis()
                    }
                } else if (songStartTime > 0L) {
                    accumulatedDuration += System.currentTimeMillis() - songStartTime
                    songStartTime = 0L
                }
                broadcastWidgetUpdate(currentSong, isPlaying)
            }
        })

        // ── Observe play commands ───────────────────────────────────────────
        serviceScope.launch {
            queueManager.playCommand.collect { request ->
                playSong(request.song, request.userInitiated, request.mood, request.sourceQuery)
            }
        }

        serviceScope.launch {
            queueManager.speedCommand.collect { speed ->
                val currentPitch = player.playbackParameters.pitch
                player.playbackParameters = PlaybackParameters(speed, currentPitch)
            }
        }

        // ── Pitch Control ───────────────────────────────────────────────────
        serviceScope.launch {
            queueManager.pitchCommand.collect { pitch ->
                val currentSpeed = player.playbackParameters.speed
                player.playbackParameters = PlaybackParameters(currentSpeed, pitch)
            }
        }

        serviceScope.launch {
            sleepTimerManager.timerFinished.collect {
                if (player.isPlaying) {
                    player.pause()
                }
            }
        }

        // ── GAPLESS: Sync RepeatMode ke ExoPlayer ──────────────────────────
        // Untuk RepeatMode.ONE, pakai ExoPlayer's native REPEAT_MODE_ONE agar:
        //   1. ExoPlayer handle loop sendiri → true gapless loop tanpa re-prepare
        //   2. onMediaItemTransition(REASON_REPEAT) fires untuk record play history
        //   3. STATE_ENDED tidak pernah fires → tidak ada restart dengan gap
        //
        // Untuk RepeatMode.OFF dan ALL, kita handle sendiri via onMediaItemTransition(AUTO).
        serviceScope.launch {
            queueManager.repeatMode.collect { mode ->
                player.repeatMode = when (mode) {
                    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
                // Saat switch dari ONE ke ALL/OFF, re-schedule agar next song
                // segera di-add ke ExoPlayer (sebelumnya tidak di-add karena ONE)
                if (mode != RepeatMode.ONE) {
                    scheduleAddNextToPlayer()
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // FIX BUG #2: Audio Focus Management
    // ════════════════════════════════════════════════════════════════════════

    private fun requestAudioFocus(): Boolean {
        if (isAudioFocusHeld) return true

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(audioFocusListener)
                .build()
            audioFocusRequest = request
            val granted = audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (granted) isAudioFocusHeld = true
            granted
        } else {
            @Suppress("DEPRECATION")
            val granted = audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (granted) isAudioFocusHeld = true
            granted
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusListener)
        }
        isAudioFocusHeld = false
        wasPlayingBeforeFocusLoss = false
    }

    // ════════════════════════════════════════════════════════════════════════
    // GAPLESS HELPERS
    // ════════════════════════════════════════════════════════════════════════

    // Build MediaItem dengan MediaId = song.id.
    // MediaId penting untuk identifikasi di onMediaItemTransition.
    private fun buildMediaItem(song: Song, url: String): MediaItem =
        MediaItem.Builder()
            .setUri(url)
            .setMediaId(song.id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setArtworkUri(Uri.parse(song.thumbnailUrl))
                    .build()
            )
            .build()

    // Fetch URL dengan retry + gunakan cache kalau tersedia.
    // PENTING: Tidak pakai prefetchCache.remove() agar URL bisa tetap di-cache
    // untuk penggunaan berikutnya (misal saat error retry).
    // Cache hanya di-invalidate secara eksplisit di onPlayerError.
    private suspend fun getCachedUrlOrFetch(songId: String): String {
        val cached = prefetchCache[songId]
        if (cached != null && System.currentTimeMillis() - cached.second < PREFETCH_TTL_MS) {
            return cached.first
        }
        var lastError: Exception? = null
        for (attempt in 0 until MAX_FETCH_ATTEMPTS) {
            try {
                val url = repository.getStreamUrl(songId)
                // Simpan ke cache untuk penggunaan berikutnya
                manageCacheSize()
                prefetchCache[songId] = url to System.currentTimeMillis()
                return url
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "getCachedUrlOrFetch: attempt ${attempt + 1}/$MAX_FETCH_ATTEMPTS gagal untuk songId=$songId — ${e.javaClass.simpleName}: ${e.message}")
                if (attempt < MAX_FETCH_ATTEMPTS - 1) {
                    delay(1_000L * (attempt + 1)) // backoff: 1s, 2s
                }
            }
        }
        throw lastError!!
    }

    // Bersihkan cache yang sudah expired atau kalau sudah terlalu banyak
    private fun manageCacheSize() {
        if (prefetchCache.size >= MAX_PREFETCH_CACHE_SIZE) {
            val now = System.currentTimeMillis()
            val expiredKeys = prefetchCache.entries
                .filter { (_, v) -> now - v.second >= PREFETCH_TTL_MS }
                .map { it.key }
            expiredKeys.forEach { prefetchCache.remove(it) }
            if (prefetchCache.size >= MAX_PREFETCH_CACHE_SIZE) {
                prefetchCache.clear()
            }
        }
    }

    // Peek lagu berikutnya dari QueueManager TANPA advance index.
    // Digunakan untuk gapless pre-queue — hanya melihat, tidak mengubah state.
    private fun peekNextSong(): Song? {
        val q = queueManager.queue.value
        val idx = queueManager.currentIndex.value
        return when (queueManager.repeatMode.value) {
            RepeatMode.ONE  -> null // ExoPlayer handle loop sendiri via REPEAT_MODE_ONE
            RepeatMode.ALL  -> q.getOrNull((idx + 1) % q.size.coerceAtLeast(1))
            RepeatMode.OFF  -> q.getOrNull(idx + 1)
        }
    }

    // Jadwalkan add lagu berikutnya ke ExoPlayer untuk gapless playback.
    //
    // Delay 8 detik sebelum fetch URL agar:
    //   1. ExoPlayer punya cukup waktu buffer current song (terutama koneksi lambat)
    //   2. Tidak bersaing bandwidth dengan buffering audio current song
    //   3. Fetch URL next song (NewPipeExtractor) yang berat tidak rebutan network
    //      dengan ExoPlayer yang lagi agresif buffer di awal playback
    //
    // Sebelumnya delay 3s — terlalu singkat, NewPipeExtractor.getStreamUrl() parse
    // full YouTube page dan bisa rebutan bandwidth → choppy audio di awal lagu.
    private fun scheduleAddNextToPlayer() {
        addNextJob?.cancel()

        // Jangan add next kalau RepeatMode.ONE — ExoPlayer handle sendiri
        if (queueManager.repeatMode.value == RepeatMode.ONE) return

        addNextJob = serviceScope.launch {
            delay(8_000) // FIX BUG LAG: naikkan dari 3s ke 8s

            try {
                val nextSong = peekNextSong() ?: return@launch

                // Cek apakah lagu ini sudah ada di playerQueue (hindari double-add)
                if (playerQueue.any { it.id == nextSong.id }) return@launch

                // Cek lagi repeatMode karena bisa berubah selama delay
                if (queueManager.repeatMode.value == RepeatMode.ONE) return@launch

                // Fetch URL — pakai cache kalau ada, fallback ke network
                // Hanya 2 attempt untuk prefetch (lebih ringan dari main fetch)
                val url = try {
                    withContext(Dispatchers.IO) {
                        val cached = prefetchCache[nextSong.id]
                        if (cached != null && System.currentTimeMillis() - cached.second < PREFETCH_TTL_MS) {
                            cached.first
                        } else {
                            val fetched = repository.getStreamUrl(nextSong.id)
                            manageCacheSize()
                            prefetchCache[nextSong.id] = fetched to System.currentTimeMillis()
                            fetched
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Gagal fetch URL untuk next song — tidak masalah.
                    // ExoPlayer akan fallback ke STATE_ENDED → playNextAuto()
                    return@launch
                }

                // Verifikasi masih relevan: current song belum berubah
                // (user tidak skip selama kita fetch URL)
                val currentPlayerSong = playerQueue.firstOrNull()
                if (currentPlayerSong?.id != currentSong?.id) return@launch

                // Add ke ExoPlayer — ini yang membuat ExoPlayer mulai buffer audio
                // data lagu berikutnya di background!
                val mediaItem = buildMediaItem(nextSong, url)
                player.addMediaItem(mediaItem)
                playerQueue.addLast(nextSong)

            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Ignore — playback tidak terganggu, STATE_ENDED jadi fallback
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // playSong
    // ════════════════════════════════════════════════════════════════════════

    fun playSong(
        song: Song,
        userInitiated: Boolean = false,
        moodHint: String? = null,
        sourceQuery: String? = null
    ) {
        Log.d(TAG, "playSong: ${song.title} | userInitiated=$userInitiated | prevSong=${currentSong?.title}")
        isHandlingError.set(false)
        consecutiveErrorCount = 0

        errorRetryJob?.cancel()
        currentPlayJob?.cancel()
        // Cancel job pre-queue lagu berikutnya — akan dijadwalkan ulang setelah
        // lagu baru mulai play
        addNextJob?.cancel()

        currentPlayJob = serviceScope.launch {
            try {
                if (currentSong != null && currentSong?.id != song.id) {
                    recordCurrentPlay()
                }

                currentSong = song
                currentMoodHint = moodHint ?: queueManager.currentMoodHint.value
                currentSourceQuery = sourceQuery ?: queueManager.currentSourceQuery.value
                songStartTime = 0L
                accumulatedDuration = 0L
                hasRecordedForCurrentSong = false
                broadcastWidgetUpdate(song, false) // isPlaying akan update via onIsPlayingChanged

                // Reset playerQueue — kita mulai fresh dengan lagu ini
                playerQueue.clear()
                playerQueue.addLast(song)

                // Fetch URL dengan retry + cache
                val streamUrl = getCachedUrlOrFetch(song.id)

                val mediaItem = buildMediaItem(song, streamUrl)

                // player.setMediaItem() otomatis:
                //   1. Clear semua MediaItem yang ada (termasuk pre-queued next song)
                //   2. Reset player ke STATE_IDLE
                //   3. Set item baru sebagai satu-satunya item
                // Ini yang benar — tidak perlu clearMediaItems() terpisah.
                isIntendingToPlay = true
                player.setMediaItem(mediaItem)
                player.prepare()

                if (requestAudioFocus()) {
                    isIntendingToPlay = false
                    player.play()
                    Log.d(TAG, "playSong: audio focus granted, playing ${song.title}")
                } else {
                    isIntendingToPlay = false
                    wasPlayingBeforeFocusLoss = true
                    Log.w(TAG, "playSong: audio focus DITOLAK untuk ${song.title} — set wasPlayingBeforeFocusLoss=true")
                }

                // Jadwalkan pre-queue lagu berikutnya untuk gapless
                scheduleAddNextToPlayer()

            } catch (e: CancellationException) {
                isIntendingToPlay = false
                playerQueue.clear() // cleanup agar state konsisten
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "playSong GAGAL untuk ${song.title} (id=${song.id}) — skip ke lagu berikutnya", e)
                isIntendingToPlay = false
                playerQueue.clear()
                playNextAuto()
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Navigation helpers
    // ════════════════════════════════════════════════════════════════════════

    private fun playNextAuto() {
        val next = queueManager.next()
        Log.d(TAG, "playNextAuto — next=${next?.title}, index=${queueManager.currentIndex.value}")
        if (next != null) {
            playSong(next, userInitiated = false)
        } else {
            Log.d(TAG, "playNextAuto — queue habis, stopSelf()")
            abandonAudioFocus()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // FIX BUG #3: recordCurrentPlay menggunakan recordingScope
    // ════════════════════════════════════════════════════════════════════════

    private fun recordCurrentPlay() {
        if (hasRecordedForCurrentSong) return
        val song = currentSong ?: return

        // FIX: Snapshot durasi SEBELUM reset apapun.
        // Sebelumnya totalDuration dihitung lalu di-check, tapi ada window race
        // di mana onIsPlayingChanged bisa reset songStartTime = 0L di antara
        // perhitungan dan reset manual — menyebabkan durasi hilang & play tidak
        // terecord. Dengan snapshot atomik ini, nilai terkunci dulu baru direset.
        val snapshot = accumulatedDuration +
            if (songStartTime > 0L) System.currentTimeMillis() - songStartTime else 0L

        if (snapshot < 10_000) return

        // Tandai sudah direcord dan reset state SETELAH snapshot diambil.
        hasRecordedForCurrentSong = true
        songStartTime = 0L
        accumulatedDuration = 0L

        val moodHint = currentMoodHint
        val sourceQuery = currentSourceQuery

        recordingScope.launch {
            repository.recordPlay(
                song = song,
                durationListened = snapshot,
                moodScreenHint = moodHint,
                sourceQuery = sourceQuery
            )
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // FIX BUG LAG: onTaskRemoved — cegah service di-kill saat swipe dari Recents
    // ════════════════════════════════════════════════════════════════════════
    //
    // Tanpa override ini, beberapa ROM (Xiaomi, Samsung, Oppo, Vivo — populer di
    // Indonesia) akan stop MusicService saat user swipe app dari Recent Apps,
    // bahkan jika musik sedang dimainkan. Akibatnya:
    //   - Audio glitch / patah sesaat sebelum service di-kill
    //   - Musik berhenti mendadak
    //   - Notifikasi media hilang
    //
    // Fix: hanya stop service kalau memang tidak sedang playing atau queue kosong.
    // Kalau masih playing → biarkan service hidup sebagai foreground service.
    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        // Jika masih playing → tidak melakukan apapun.
        // Service tetap hidup karena sudah startForeground() di onCreate().
    }

    // ════════════════════════════════════════════════════════════════════════
    // MediaSessionService
    // ════════════════════════════════════════════════════════════════════════

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    // ════════════════════════════════════════════════════════════════════════
    // onDestroy
    // ════════════════════════════════════════════════════════════════════════

    override fun onDestroy() {
        // Safety net: record lagu yang sedang berjalan sebelum service mati
        val song = currentSong
        if (song != null && !hasRecordedForCurrentSong) {
            // FIX: Konsisten dengan recordCurrentPlay() — snapshot atomik
            val snapshot = accumulatedDuration +
                if (songStartTime > 0L) System.currentTimeMillis() - songStartTime else 0L
            if (snapshot >= 10_000) {
                hasRecordedForCurrentSong = true
                val moodHint = currentMoodHint
                val sourceQuery = currentSourceQuery
                try {
                    runBlocking {
                        withTimeout(3000L) {
                            repository.recordPlay(
                                song = song,
                                durationListened = snapshot,
                                moodScreenHint = moodHint,
                                sourceQuery = sourceQuery
                            )
                        }
                    }
                } catch (_: Exception) { /* timeout atau error — skip daripada ANR */ }
            }
        }

        // ── Cleanup ─────────────────────────────────────────────────────────
        sleepTimerManager.cancel()
        equalizerManager.release()
        addNextJob?.cancel()       // GAPLESS: cancel pre-queue job
        errorRetryJob?.cancel()
        prefetchCache.clear()
        playerQueue.clear()        // GAPLESS: cleanup playerQueue

        // FIX BUG #2: Abandon audio focus saat service mati
        abandonAudioFocus()

        // FIX BUG #1: Release WifiLock — wajib dilepas agar tidak leak
        if (wifiLock?.isHeld == true) {
            wifiLock?.release()
        }
        wifiLock = null

        serviceScope.cancel()

        // FIX BUG #3: recordingScope di-cancel TERAKHIR
        recordingScope.cancel()

        mediaSession.release()
        player.release()
        super.onDestroy()
    }
}
