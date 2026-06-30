package com.zaaam.Zmusic.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AudioSessionHolder — Singleton sederhana untuk berbagi audioSessionId ExoPlayer
 * antara MusicService (yang membuat player) dan PlayerViewModel (yang butuh ID
 * untuk membuka system equalizer via Intent).
 *
 * MusicService set nilai ini setelah ExoPlayer dibuat.
 * PlayerViewModel membaca nilai ini saat user tap tombol Equalizer.
 */
@Singleton
class AudioSessionHolder @Inject constructor() {

    private val _audioSessionId = MutableStateFlow(0)
    /** audioSessionId dari ExoPlayer. 0 jika belum diinisialisasi. */
    val audioSessionId: StateFlow<Int> = _audioSessionId.asStateFlow()

    fun setSessionId(id: Int) {
        _audioSessionId.value = id
    }
}
