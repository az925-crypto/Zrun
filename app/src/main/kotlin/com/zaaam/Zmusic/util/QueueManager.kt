package com.zaaam.Zmusic.util

import com.zaaam.Zmusic.data.MusicRepository
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.model.entity.MoodStatResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PlayRequest(
    val song: Song,
    val userInitiated: Boolean = true,
    val mood: String? = null,
    val sourceQuery: String? = null
)

enum class RepeatMode { OFF, ONE, ALL }

@Singleton
class QueueManager @Inject constructor(
    private val repository: MusicRepository
) {

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isShuffled = MutableStateFlow(false)
    val isShuffled: StateFlow<Boolean> = _isShuffled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    // FIX BUG #4: extraBufferCapacity dinaikkan dari 1 ke 8.
    // tryEmit() return false (silent drop) jika buffer penuh sebelum MusicService
    // sempat consume. Buffer 1 terlalu kecil — user yang skip cepat-cepat bisa
    // menyebabkan _currentIndex sudah bergerak tapi play command di-drop → lagu
    // tidak main tapi index queue sudah geser → inkonsistensi antrian.
    // Buffer 8 memberi ruang cukup untuk rapid skip tanpa drop.
    private val _playCommand = MutableSharedFlow<PlayRequest>(extraBufferCapacity = 8)
    val playCommand: SharedFlow<PlayRequest> = _playCommand.asSharedFlow()

    private val _speedCommand = MutableSharedFlow<Float>(extraBufferCapacity = 1)
    val speedCommand: SharedFlow<Float> = _speedCommand.asSharedFlow()

    private val _currentSpeed = MutableStateFlow(1f)
    val currentSpeed: StateFlow<Float> = _currentSpeed.asStateFlow()

    // ── Pitch Control ─────────────────────────────────────────────────────
    private val _pitchCommand = MutableSharedFlow<Float>(extraBufferCapacity = 1)
    val pitchCommand: SharedFlow<Float> = _pitchCommand.asSharedFlow()

    private val _currentPitch = MutableStateFlow(1f)
    val currentPitch: StateFlow<Float> = _currentPitch.asStateFlow()

    private val _currentMoodHint = MutableStateFlow<String?>(null)
    val currentMoodHint: StateFlow<String?> = _currentMoodHint.asStateFlow()

    private val _currentSourceQuery = MutableStateFlow<String?>(null)
    val currentSourceQuery: StateFlow<String?> = _currentSourceQuery.asStateFlow()

    val currentMood: StateFlow<String?> get() = _currentMoodHint

    private var originalQueue: List<Song> = emptyList()
    private var queueMoodCache: Map<String, String> = emptyMap()
    private var recentMoodPattern: List<MoodStatResult> = emptyList()

    // ══════════════════════════════════════════════════════════════════════════
    // Lock untuk thread-safety semua operasi baca+tulis queue/index.
    // Menggunakan synchronized{} (bukan Mutex) agar bisa dipanggil dari
    // non-suspend context (next()/previous() di MusicService playNextAuto).
    // ══════════════════════════════════════════════════════════════════════════
    private val lock = Any()

    fun setQueue(
        songs: List<Song>,
        startIndex: Int = 0,
        moodHint: String? = null,
        sourceQuery: String? = null,
        autoShuffle: Boolean = false,
        recentPattern: List<MoodStatResult> = emptyList()
    ) {
        synchronized(lock) {
            originalQueue = songs
            _currentMoodHint.value = moodHint
            _currentSourceQuery.value = sourceQuery

            queueMoodCache = repository.detectMoodBatch(songs)

            if (autoShuffle && songs.size > 1) {
                recentMoodPattern = recentPattern
                val chosen = songs[startIndex.coerceIn(0, songs.size - 1)]
                val rest = songs.filterIndexed { i, _ -> i != startIndex.coerceIn(0, songs.size - 1) }

                val shuffled = trueShuffleWithAntiCluster(rest)

                _queue.value = listOf(chosen) + shuffled
                _currentIndex.value = 0
                _isShuffled.value = true
            } else {
                _queue.value = songs.toList()
                _currentIndex.value = startIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0))
                _isShuffled.value = false
            }
        }
    }

    fun requestPlay(
        song: Song,
        userInitiated: Boolean = true,
        mood: String? = null,
        sourceQuery: String? = null
    ) {
        _playCommand.tryEmit(
            PlayRequest(
                song = song,
                userInitiated = userInitiated,
                mood = mood ?: _currentMoodHint.value,
                sourceQuery = sourceQuery ?: _currentSourceQuery.value
            )
        )
    }

    fun setSpeed(speed: Float) {
        _currentSpeed.value = speed
        _speedCommand.tryEmit(speed)
    }

    fun setPitch(pitch: Float) {
        _currentPitch.value = pitch
        _pitchCommand.tryEmit(pitch)
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    // ── TRUE SHUFFLE — Fisher-Yates + Anti-Cluster ────────────────────────

    private fun trueShuffleWithAntiCluster(songs: List<Song>): List<Song> {
        if (songs.size <= 2) return songs.shuffled()

        val shuffled = songs.toMutableList().apply { shuffle() }

        for (i in 2 until shuffled.size) {
            val moodI = queueMoodCache[shuffled[i].id]
            val moodPrev1 = queueMoodCache[shuffled[i - 1].id]
            val moodPrev2 = queueMoodCache[shuffled[i - 2].id]

            if (moodI != null && moodI == moodPrev1 && moodI == moodPrev2) {
                val swapTarget = ((i + 1) until shuffled.size).firstOrNull { idx ->
                    queueMoodCache[shuffled[idx].id] != moodI
                } ?: ((i + 1) until shuffled.size).randomOrNull()

                if (swapTarget != null) {
                    val temp = shuffled[i]
                    shuffled[i] = shuffled[swapTarget]
                    shuffled[swapTarget] = temp
                }
            }
        }

        return shuffled
    }

    fun toggleSmartShuffle(recentPattern: List<MoodStatResult> = emptyList()) {
        synchronized(lock) {
            val currentSong = current()
            if (_isShuffled.value) {
                _queue.value = originalQueue
                _currentIndex.value = originalQueue.indexOfFirst { it.id == currentSong?.id }
                    .coerceAtLeast(0)
            } else {
                recentMoodPattern = recentPattern
                queueMoodCache = repository.detectMoodBatch(_queue.value)
                val rest = _queue.value.filter { it.id != currentSong?.id }
                val shuffled = trueShuffleWithAntiCluster(rest)
                _queue.value = listOfNotNull(currentSong) + shuffled
                _currentIndex.value = 0
            }
            _isShuffled.value = !_isShuffled.value
        }
    }

    fun toggleShuffle() {
        toggleSmartShuffle(recentMoodPattern)
    }

    // ── Navigation ─────────────────────────────────────────────────────────

    fun next(): Song? {
        synchronized(lock) {
            val q = _queue.value
            if (q.isEmpty()) return null

            return when (_repeatMode.value) {
                RepeatMode.ONE -> current()
                RepeatMode.ALL -> {
                    _currentIndex.value = (_currentIndex.value + 1) % q.size
                    current()
                }
                RepeatMode.OFF -> {
                    if (_currentIndex.value < q.size - 1) {
                        _currentIndex.value++
                        current()
                    } else null
                }
            }
        }
    }

    fun previous(): Song? {
        synchronized(lock) {
            val q = _queue.value
            if (q.isEmpty()) return null

            return when (_repeatMode.value) {
                RepeatMode.ALL -> {
                    _currentIndex.value = if (_currentIndex.value > 0)
                        _currentIndex.value - 1
                    else q.size - 1
                    current()
                }
                else -> {
                    if (_currentIndex.value > 0) _currentIndex.value--
                    current()
                }
            }
        }
    }

    // FIX BUG #5: current() dibungkus synchronized(lock).
    //
    // TOCTOU race condition lama:
    //   Thread A baca _queue.value → List[10 lagu]
    //   Thread B panggil setQueue() → ganti _queue.value jadi List[3 lagu]
    //   Thread A baca _currentIndex.value → 7
    //   Thread A: getOrNull(7) pada List[3] → NULL
    //   → MusicService.playNextAuto() terima null → stopSelf() → musik berhenti!
    //
    // Fix: satu synchronized(lock) membungkus kedua operasi baca sekaligus.
    fun current(): Song? {
        synchronized(lock) {
            return _queue.value.getOrNull(_currentIndex.value)
        }
    }

    // FIX TAMBAHAN: hasNext() dan hasPrevious() juga dibungkus synchronized(lock)
    // untuk konsistensi — mencegah TOCTOU yang sama dengan current().
    fun hasNext(): Boolean {
        synchronized(lock) {
            return _repeatMode.value != RepeatMode.OFF ||
                    _currentIndex.value < _queue.value.size - 1
        }
    }

    fun hasPrevious(): Boolean {
        synchronized(lock) {
            return _repeatMode.value == RepeatMode.ALL || _currentIndex.value > 0
        }
    }

    // ── Queue Management ───────────────────────────────────────────────────

    fun removeAt(index: Int) {
        synchronized(lock) {
            val q = _queue.value.toMutableList()
            if (index < 0 || index >= q.size) return

            val wasPlayingThisIndex = index == _currentIndex.value
            q.removeAt(index)

            if (q.isEmpty()) {
                _queue.value = emptyList()
                _currentIndex.value = 0
                return
            }

            val newIndex = when {
                index < _currentIndex.value -> (_currentIndex.value - 1).coerceAtLeast(0)
                wasPlayingThisIndex         -> _currentIndex.value.coerceAtMost(q.size - 1)
                else                        -> _currentIndex.value
            }

            _queue.value = q
            _currentIndex.value = newIndex

            if (wasPlayingThisIndex && q.isNotEmpty()) {
                requestPlay(q[newIndex], userInitiated = true)
            }
        }
    }

    /** Pindahkan lagu dalam antrean (drag-reorder). currentIndex ikut bergeser. */
    fun moveItem(from: Int, to: Int) {
        synchronized(lock) {
            val q = _queue.value.toMutableList()
            if (from == to || from !in q.indices || to !in q.indices) return

            val item = q.removeAt(from)
            q.add(to, item)

            val cur = _currentIndex.value
            val newIndex = when {
                cur == from            -> to
                from < cur && to >= cur -> cur - 1
                from > cur && to <= cur -> cur + 1
                else                   -> cur
            }
            _queue.value = q
            _currentIndex.value = newIndex
        }
    }

    fun jumpTo(index: Int) {
        synchronized(lock) {
            val q = _queue.value
            if (index < 0 || index >= q.size) return
            _currentIndex.value = index
            requestPlay(q[index], userInitiated = true)
        }
    }
}
