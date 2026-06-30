package com.zaaam.Zmusic.ui.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.zaaam.Zmusic.data.LyricsRepository
import com.zaaam.Zmusic.data.MusicRepository
import com.zaaam.Zmusic.model.Lyrics
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.model.entity.MoodStatResult
import com.zaaam.Zmusic.service.MusicService
import com.zaaam.Zmusic.util.AudioDownloadManager
import com.zaaam.Zmusic.util.AudioSessionHolder
import com.zaaam.Zmusic.util.DownloadState
import com.zaaam.Zmusic.util.QueueManager
import com.zaaam.Zmusic.util.SleepTimerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LyricsState {
    object Idle : LyricsState()
    object Loading : LyricsState()
    data class Success(val lyrics: Lyrics, val translated: String? = null) : LyricsState()
    object NotFound : LyricsState()
    data class Error(val message: String) : LyricsState()
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val queueManager: QueueManager,
    private val lyricsRepository: LyricsRepository,
    val musicRepository: MusicRepository,
    val downloadManager: AudioDownloadManager,
    val sleepTimerManager: SleepTimerManager,
    private val audioSessionHolder: AudioSessionHolder
) : ViewModel() {

    companion object {
        private const val TAG = "ZmusicPlayerVM"
    }

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _lyricsState = MutableStateFlow<LyricsState>(LyricsState.Idle)
    val lyricsState: StateFlow<LyricsState> = _lyricsState.asStateFlow()

    private val _showTranslation = MutableStateFlow(false)
    val showTranslation: StateFlow<Boolean> = _showTranslation.asStateFlow()

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    private val _isArtistLoading = MutableStateFlow(false)
    val isArtistLoading: StateFlow<Boolean> = _isArtistLoading.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var lastSongId: String? = null

    // FIX BUG #6: Flag untuk track apakah listener sudah di-attach ke controller.
    // Digunakan di onCleared() untuk menentukan cleanup path yang benar.
    private var listenerAttached = false

    private var cachedRecentPattern: List<MoodStatResult> = emptyList()
    private var loadingTimeoutJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                _isLoading.value = false
                loadingTimeoutJob?.cancel()
            }
        }
        override fun onPlaybackStateChanged(state: Int) {
            _isLoading.value = state == Player.STATE_BUFFERING
            // FIX STOP BUG: Jangan panggil startLoadingWatchdog() dari listener ini.
            // STATE_BUFFERING juga fire saat mid-song rebuffering DAN saat
            // MusicService auto-advance ke lagu berikutnya. Kalau watchdog
            // distart di sini → controller.pause() dipanggil paksa setelah 8 detik
            // kalau buffer belum selesai → lagu stop sendiri di tengah jalan /
            // lagu baru stop sebelum sempat main.
            // Watchdog cukup distart dari user-initiated actions (playSong,
            // skipNext, skipPrevious) yang ada di bawah.
            if (state != Player.STATE_BUFFERING) {
                loadingTimeoutJob?.cancel()
            }
            if (state == Player.STATE_ENDED) {
                _isPlaying.value = false
                _progress.value = 0f
            }
        }
        override fun onPlayerError(error: PlaybackException) {
            _errorMessage.value = "Gagal memutar lagu, coba lagi"
            _isLoading.value = false
            _isPlaying.value = false
            loadingTimeoutJob?.cancel()
        }
    }

    init {
        connectToService()
        startProgressUpdate()
        observeCurrentSong()
        fetchRecentMoodPattern()
    }

    private fun startLoadingWatchdog() {
        loadingTimeoutJob?.cancel()
        loadingTimeoutJob = viewModelScope.launch {
            delay(8_000L)
            if (_isLoading.value) {
                // FIX STOP BUG: Hanya bersihkan loading indicator — JANGAN pause.
                // Memanggil controller.pause() di sini menyebabkan lagu berhenti
                // paksa jika buffering (koneksi lambat, server lambat) > 8 detik.
                // Jika stream benar-benar gagal, ExoPlayer akan throw onPlayerError
                // yang sudah ditangani MusicService dengan retry + playNextAuto().
                _isLoading.value = false
            }
        }
    }

    private fun fetchRecentMoodPattern() {
        viewModelScope.launch {
            cachedRecentPattern = musicRepository.getRecentMoodPattern(20)
        }
    }

    // FIX BUG #6: Mencegah memory leak akibat race condition antara
    // connectToService() callback dan onCleared().
    //
    // Skenario leak lama:
    //   1. connectToService() → future.addListener(callback)
    //   2. ViewModel di-clear (rotasi layar cepat) → onCleared() tapi controller null
    //   3. Callback baru selesai → controller = future.get() → addListener(playerListener)
    //   4. PlayerViewModel tidak bisa di-GC karena playerListener masih terdaftar → LEAK
    //
    // Fix:
    //   - Flag `isViewModelCleared` di-set true di onCleared() sebelum cleanup lain.
    //   - Callback cek flag ini — jika true, langsung release controller tanpa attach.
    //   - Flag `listenerAttached` menentukan apakah perlu removeListener() di onCleared().
    private fun connectToService() {
        try {
            val sessionToken = SessionToken(
                context,
                ComponentName(context, MusicService::class.java)
            )
            controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

            val future = controllerFuture ?: return
            future.addListener({
                try {
                    // Guard: jika ViewModel sudah di-clear, jangan attach listener
                    if (isViewModelCleared) {
                        try {
                            future.get()?.release()
                        } catch (e: Exception) {
                            Log.w(TAG, "release controller (post-clear) gagal", e)
                        }
                        return@addListener
                    }
                    controller = future.get()
                    controller?.addListener(playerListener)
                    listenerAttached = true
                } catch (e: Exception) {
                    Log.w(TAG, "attach MediaController gagal — kontrol player UI tidak aktif", e)
                }
            }, MoreExecutors.directExecutor())
        } catch (e: Exception) {
            Log.w(TAG, "connectToService gagal — MediaController tidak terhubung", e)
        }
    }

    // FIX BUG #6: Flag deteksi ViewModel sudah di-clear.
    // Di-set di onCleared() sebelum operasi cleanup lainnya.
    private var isViewModelCleared = false

    private fun startProgressUpdate() {
        viewModelScope.launch {
            while (isActive) {
                delay(500)
                if (!_isPlaying.value) continue
                val ctrl = controller ?: continue
                val duration = ctrl.duration
                val position = ctrl.currentPosition
                if (duration > 0 && duration != C.TIME_UNSET) {
                    _progress.value = position.toFloat() / duration
                    _currentPosition.value = position
                }
            }
        }
    }

    private fun observeCurrentSong() {
        viewModelScope.launch {
            queueManager.currentIndex.collect {
                val song = queueManager.current() ?: return@collect
                if (song.id != lastSongId) {
                    lastSongId = song.id
                    _lyricsState.value = LyricsState.Idle
                    _showTranslation.value = false
                }
            }
        }
    }

    fun playSong(
        song: Song,
        queue: List<Song> = listOf(song),
        moodHint: String? = null,
        sourceQuery: String? = null,
        autoShuffle: Boolean = false
    ) {
        _errorMessage.value = null
        _isLoading.value = true
        _lyricsState.value = LyricsState.Idle
        _showTranslation.value = false
        startLoadingWatchdog()

        viewModelScope.launch {
            cachedRecentPattern = musicRepository.getRecentMoodPattern(20)

            val finalQueue = if (autoShuffle && queue.size > 1) {
                val targetMood = moodHint
                    ?: musicRepository.detectMoodByKeyword(song)

                if (targetMood != null) {
                    val compatibleSongs = musicRepository.filterMoodCompatible(queue, targetMood)
                    val withClickedSong = if (song in compatibleSongs) compatibleSongs
                    else listOf(song) + compatibleSongs

                    if (withClickedSong.size >= 3) withClickedSong else queue
                } else {
                    queue
                }
            } else {
                queue
            }

            queueManager.setQueue(
                songs         = finalQueue,
                startIndex    = finalQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0),
                moodHint      = moodHint,
                sourceQuery   = sourceQuery,
                autoShuffle   = autoShuffle,
                recentPattern = cachedRecentPattern
            )
            queueManager.requestPlay(
                song          = song,
                userInitiated = true,
                mood          = moodHint,
                sourceQuery   = sourceQuery
            )
        }
    }

    // ── Playlist Shuffle — bypass mood filter, shuffle semua lagu ─────────
    fun playPlaylistShuffled(songs: List<Song>, startSong: Song? = null) {
        if (songs.isEmpty()) return
        val song       = startSong ?: songs.first()
        val startIndex = songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        _errorMessage.value  = null
        _isLoading.value     = true
        _lyricsState.value   = LyricsState.Idle
        _showTranslation.value = false
        startLoadingWatchdog()

        viewModelScope.launch {
            cachedRecentPattern = musicRepository.getRecentMoodPattern(20)
            queueManager.setQueue(
                songs         = songs,          // semua lagu, tanpa mood filter
                startIndex    = startIndex,
                autoShuffle   = true,
                recentPattern = cachedRecentPattern
            )
            queueManager.requestPlay(song, userInitiated = true)
        }
    }

    fun playArtist(artistName: String) {
        if (_isArtistLoading.value) return
        viewModelScope.launch {
            _isArtistLoading.value = true
            try {
                val artistSongs = musicRepository.searchByArtist(artistName)
                if (artistSongs.isEmpty()) {
                    _errorMessage.value = "Tidak ada lagu dari \"$artistName\" yang ditemukan"
                    return@launch
                }

                val firstSong = artistSongs.first()
                _isLoading.value = true
                startLoadingWatchdog()
                _lyricsState.value = LyricsState.Idle
                _showTranslation.value = false

                queueManager.setQueue(
                    songs         = artistSongs,
                    startIndex    = 0,
                    moodHint      = null,
                    sourceQuery   = "$artistName songs",
                    autoShuffle   = false,
                    recentPattern = cachedRecentPattern
                )
                queueManager.requestPlay(
                    song          = firstSong,
                    userInitiated = true,
                    mood          = null,
                    sourceQuery   = "$artistName songs"
                )
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat lagu artis: ${e.message ?: "Coba lagi"}"
                _isLoading.value = false
            } finally {
                _isArtistLoading.value = false
            }
        }
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun seekTo(fraction: Float) {
        val ctrl = controller ?: return
        val duration = ctrl.duration
        if (duration > 0 && duration != C.TIME_UNSET) {
            ctrl.seekTo((fraction * duration).toLong())
        }
    }

    fun seekForward10() {
        val ctrl = controller ?: return
        val duration = ctrl.duration
        if (duration <= 0 || duration == C.TIME_UNSET) return
        ctrl.seekTo((ctrl.currentPosition + 10_000).coerceAtMost(duration))
    }

    fun seekBackward10() {
        val ctrl = controller ?: return
        ctrl.seekTo((ctrl.currentPosition - 10_000).coerceAtLeast(0))
    }

    fun skipNext() {
        val next = queueManager.next() ?: return
        _isLoading.value = true
        startLoadingWatchdog()
        queueManager.requestPlay(next, userInitiated = true)
    }

    fun skipPrevious() {
        val ctrl = controller
        if (ctrl != null && ctrl.currentPosition > 3000) {
            ctrl.seekTo(0)
        } else {
            val prev = queueManager.previous() ?: return
            _isLoading.value = true
            startLoadingWatchdog()
            queueManager.requestPlay(prev, userInitiated = true)
        }
    }

    fun toggleShuffle() {
        viewModelScope.launch {
            val recentPattern = musicRepository.getRecentMoodPattern(20)
            queueManager.toggleSmartShuffle(recentPattern)
        }
    }

    fun toggleRepeat() = queueManager.toggleRepeat()

    fun setSpeed(speed: Float) = queueManager.setSpeed(speed)

    fun setPitch(pitch: Float) = queueManager.setPitch(pitch)

    fun removeFromQueue(index: Int) = queueManager.removeAt(index)

    fun jumpToQueueIndex(index: Int) = queueManager.jumpTo(index)

    fun moveInQueue(from: Int, to: Int) = queueManager.moveItem(from, to)

    fun startSleepTimer(minutes: Int) = sleepTimerManager.start(minutes)

    fun cancelSleepTimer() = sleepTimerManager.cancel()

    // Equalizer sekarang punya dedicated screen — navigasi ditangani MainActivity.
    // Fungsi ini dipertahankan sebagai fallback ke system equalizer jika dibutuhkan.
    fun openSystemEqualizer(context: Context) {
        val sessionId = audioSessionHolder.audioSessionId.value
        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "openSystemEqualizer gagal (tidak ada panel EQ sistem)", e)
            _errorMessage.value = "Equalizer tidak tersedia di perangkat ini"
        }
    }

    fun loadLyrics(song: Song) {
        if (_lyricsState.value is LyricsState.Success || _lyricsState.value is LyricsState.Loading) return
        viewModelScope.launch {
            _lyricsState.value = LyricsState.Loading
            val lyrics = lyricsRepository.getLyrics(song.title, song.artist)
            _lyricsState.value = if (lyrics != null) LyricsState.Success(lyrics)
                                  else LyricsState.NotFound
        }
    }

    fun toggleTranslation(song: Song) {
        val state = _lyricsState.value as? LyricsState.Success ?: return
        if (state.translated != null) {
            _showTranslation.value = !_showTranslation.value
            return
        }
        viewModelScope.launch {
            _isTranslating.value = true
            val textToTranslate = if (state.lyrics.hasSynced)
                state.lyrics.synced.joinToString("\n") { it.text }
            else
                state.lyrics.plain

            val translated = lyricsRepository.translateLyrics(textToTranslate)
            _isTranslating.value = false
            _lyricsState.value = state.copy(translated = translated ?: "Terjemahan tidak tersedia")
            _showTranslation.value = true
        }
    }

    fun downloadSong(song: Song) {
        val currentState = downloadManager.getState(song.id)
        if (currentState is DownloadState.Downloading || currentState is DownloadState.Done) return
        viewModelScope.launch {
            downloadManager.download(song) { id -> musicRepository.getStreamUrl(id) }
        }
    }

    fun deleteDownload(song: Song) {
        viewModelScope.launch {
            downloadManager.deleteDownload(song.id)
            musicRepository.clearLocalPath(song.id)
        }
    }

    fun clearError() { _errorMessage.value = null }

    override fun onCleared() {
        // FIX BUG #6: Set flag PERTAMA — callback connectToService() yang sedang
        // pending akan deteksi ini dan tidak akan attach listener ke controller.
        isViewModelCleared = true

        loadingTimeoutJob?.cancel()

        // FIX BUG #6: Pisahkan cleanup berdasarkan status listenerAttached:
        //   true  → listener sudah attached → wajib removeListener() sebelum release
        //   false → future belum complete → isViewModelCleared sudah guard callback
        if (listenerAttached) {
            controller?.removeListener(playerListener)
        }
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null

        super.onCleared()
    }
}
