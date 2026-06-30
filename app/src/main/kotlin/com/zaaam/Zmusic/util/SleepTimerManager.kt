package com.zaaam.Zmusic.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SleepTimerManager — Countdown timer otomatis untuk menghentikan pemutaran musik.
 *
 * Cara kerja:
 * - Singleton, diakses PlayerViewModel (UI) dan MusicService (pause).
 * - [timerFinished] SharedFlow di-collect MusicService → player.pause().
 * - [remainingMs] StateFlow dipakai PlayerScreen untuk tampilkan sisa waktu.
 * - [isActive] StateFlow untuk state toggle button di UI.
 */
@Singleton
class SleepTimerManager @Inject constructor() {

    // ── State flows ───────────────────────────────────────────────────────

    private val _remainingMs = MutableStateFlow<Long?>(null)
    /** Sisa waktu dalam milidetik. null = timer tidak aktif. */
    val remainingMs: StateFlow<Long?> = _remainingMs.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    /** true jika timer sedang berjalan. */
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _timerFinished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emit saat timer habis — diobserve MusicService untuk pause player. */
    val timerFinished: SharedFlow<Unit> = _timerFinished.asSharedFlow()

    // ── Internal ──────────────────────────────────────────────────────────

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Mulai sleep timer.
     * @param minutes Durasi dalam menit (5, 10, 15, 20, 30, 45, 60).
     */
    fun start(minutes: Int) {
        cancel() // batalkan timer sebelumnya jika ada

        val totalMs = minutes * 60_000L
        _remainingMs.value = totalMs
        _isActive.value = true

        timerJob = scope.launch {
            var remaining = totalMs
            while (remaining > 0) {
                delay(1_000L)
                remaining -= 1_000L
                _remainingMs.value = remaining.coerceAtLeast(0L)
            }
            // Waktu habis → emit signal ke MusicService
            _isActive.value = false
            _remainingMs.value = null
            _timerFinished.tryEmit(Unit)
        }
    }

    /** Batalkan timer yang sedang berjalan. */
    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        _isActive.value = false
        _remainingMs.value = null
    }
}

// ── Extension ─────────────────────────────────────────────────────────────

/** Format sisa waktu menjadi "MM:SS" untuk ditampilkan di UI. */
fun Long.toTimerString(): String {
    val minutes = this / 60_000L
    val seconds = (this % 60_000L) / 1_000L
    return "%d:%02d".format(minutes, seconds)
}
