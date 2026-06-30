package com.zaaam.Zmusic.util

import android.content.Context
import android.media.audiofx.Equalizer
import android.util.Log
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// ── EQ Presets ──────────────────────────────────────────────────────────────
// Band levels dalam millibels (mB). Range device umumnya -1500 sampai +1500 mB.
// Nilai akan di-clamp ke range aktual perangkat saat diterapkan.

enum class EqPreset(
    val label: String,
    /** 5 band levels: [Bass, Low-Mid, Mid, High-Mid, Treble] dalam mB */
    val bands: List<Int>
) {
    NORMAL("Normal",        listOf(   0,   0,   0,   0,   0)),
    POP("Pop",              listOf( 200, 400, 600, 400, 200)),
    ROCK("Rock",            listOf( 500, 300,-200, 300, 500)),
    JAZZ("Jazz",            listOf( 400, 200, 100, 200,-200)),
    ELECTRONIC("Electronic",listOf( 400, 300,   0, 300, 600)),
    CLASSICAL("Klasik",     listOf( 500, 300,-200, 300, 400)),
    HIPHOP("Hip-Hop",       listOf( 600, 300,   0,-200, 100)),
    BASS_BOOST("Bass Boost",listOf(1000, 600, 200,-100,-200))
}

@Singleton
class EqualizerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioSessionHolder: AudioSessionHolder
) {
    private val prefs = context.getSharedPreferences("zmusic_eq", Context.MODE_PRIVATE)

    private var equalizer: Equalizer? = null
    private var lastSessionId: Int = -1

    // ── Public State ─────────────────────────────────────────────────────────

    private val _isEnabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    /** Current band levels (mB). Size = number of bands (usually 5). */
    private val _bandLevels = MutableStateFlow<List<Int>>(loadSavedLevels())
    val bandLevels: StateFlow<List<Int>> = _bandLevels.asStateFlow()

    /** Band frequency centers in Hz (loaded from device). */
    private val _bandFrequencies = MutableStateFlow<List<Int>>(listOf(60, 230, 910, 3600, 14000))
    val bandFrequencies: StateFlow<List<Int>> = _bandFrequencies.asStateFlow()

    /** Level range [min, max] in mB. */
    private val _levelRange = MutableStateFlow(Pair(-1500, 1500))
    val levelRange: StateFlow<Pair<Int, Int>> = _levelRange.asStateFlow()

    private val _numberOfBands = MutableStateFlow(5)
    val numberOfBands: StateFlow<Int> = _numberOfBands.asStateFlow()

    private val _activePreset = MutableStateFlow<EqPreset?>(
        prefs.getString(KEY_PRESET, null)?.let { name ->
            EqPreset.entries.firstOrNull { it.name == name }
        }
    )
    val activePreset: StateFlow<EqPreset?> = _activePreset.asStateFlow()

    // ── Attach ───────────────────────────────────────────────────────────────

    /**
     * Attach equalizer ke ExoPlayer audio session.
     * Dipanggil dari EqualizerViewModel saat screen dibuka.
     * Juga bisa dipanggil dari MusicService setelah player dibuat.
     */
    fun ensureAttached() {
        val sessionId = audioSessionHolder.audioSessionId.value
        if (sessionId == 0) return
        if (equalizer != null && lastSessionId == sessionId) {
            // Sudah attach ke session yang sama — sync state saja
            applyCurrentLevels()
            return
        }
        attachToSession(sessionId)
    }

    private fun attachToSession(sessionId: Int) {
        try {
            equalizer?.release()
            equalizer = null

            val eq = Equalizer(0, sessionId)
            lastSessionId = sessionId

            // Baca metadata dari device
            val range = eq.bandLevelRange
            if (range != null && range.size >= 2) {
                _levelRange.value = Pair(range[0].toInt(), range[1].toInt())
            }

            val numBands = eq.numberOfBands.toInt()
            _numberOfBands.value = numBands

            // Baca frekuensi center tiap band (dalam mHz → ubah ke Hz)
            val freqs = (0 until numBands).map { band ->
                (eq.getCenterFreq(band.toShort()) / 1000)  // mHz → Hz
            }
            _bandFrequencies.value = freqs

            // Pastikan kita punya level untuk semua band
            val savedLevels = _bandLevels.value
            val levels = if (savedLevels.size == numBands) savedLevels
                         else List(numBands) { 0 }
            _bandLevels.value = levels

            // Terapkan levels ke EQ hardware
            val (minLevel, maxLevel) = _levelRange.value
            levels.forEachIndexed { band, level ->
                if (band < numBands) {
                    val clamped = level.coerceIn(minLevel, maxLevel)
                    eq.setBandLevel(band.toShort(), clamped.toShort())
                }
            }

            eq.enabled = _isEnabled.value
            equalizer = eq

        } catch (e: Exception) {
            // EQ tidak tersedia di perangkat ini — biarkan null.
            // Aturan emas: JANGAN telan error tanpa log (pelajaran bug bisu 2-3 bulan)
            Log.w(TAG, "attachToSession gagal — EQ tidak tersedia di session $sessionId", e)
            equalizer = null
        }
    }

    // ── Controls ─────────────────────────────────────────────────────────────

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        equalizer?.enabled = enabled
        prefs.edit { putBoolean(KEY_ENABLED, enabled) }
        if (enabled) ensureAttached()
    }

    fun setBandLevel(band: Int, levelMb: Int) {
        val (minLevel, maxLevel) = _levelRange.value
        val clamped = levelMb.coerceIn(minLevel, maxLevel)

        val current = _bandLevels.value.toMutableList()
        if (band < current.size) {
            current[band] = clamped
            _bandLevels.value = current
        }

        try {
            equalizer?.setBandLevel(band.toShort(), clamped.toShort())
        } catch (e: Exception) {
            Log.w(TAG, "setBandLevel($band, $clamped) gagal", e)
        }

        _activePreset.value = null  // custom — tidak ada preset
        saveLevels(current)
    }

    fun applyPreset(preset: EqPreset) {
        _activePreset.value = preset
        prefs.edit { putString(KEY_PRESET, preset.name) }

        val numBands = _numberOfBands.value
        val (minLevel, maxLevel) = _levelRange.value

        // Pastikan preset punya cukup bands — pad dengan 0 kalau kurang
        val presetLevels = preset.bands.let { bands ->
            if (bands.size >= numBands) bands.take(numBands)
            else bands + List(numBands - bands.size) { 0 }
        }

        val clamped = presetLevels.map { it.coerceIn(minLevel, maxLevel) }
        _bandLevels.value = clamped
        saveLevels(clamped)

        clamped.forEachIndexed { band, level ->
            try {
                equalizer?.setBandLevel(band.toShort(), level.toShort())
            } catch (e: Exception) {
                Log.w(TAG, "applyPreset: setBandLevel($band) gagal", e)
            }
        }
    }

    fun resetToFlat() {
        applyPreset(EqPreset.NORMAL)
        _activePreset.value = EqPreset.NORMAL
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun applyCurrentLevels() {
        val (minLevel, maxLevel) = _levelRange.value
        _bandLevels.value.forEachIndexed { band, level ->
            try {
                val clamped = level.coerceIn(minLevel, maxLevel)
                equalizer?.setBandLevel(band.toShort(), clamped.toShort())
            } catch (e: Exception) {
                Log.w(TAG, "applyCurrentLevels: setBandLevel($band) gagal", e)
            }
        }
        equalizer?.enabled = _isEnabled.value
    }

    private fun loadSavedLevels(): List<Int> {
        val saved = prefs.getString(KEY_LEVELS, null) ?: return List(5) { 0 }
        return try {
            saved.split(",").map { it.trim().toInt() }
        } catch (e: Exception) {
            Log.w(TAG, "loadSavedLevels: data prefs korup ('$saved') — reset ke flat", e)
            List(5) { 0 }
        }
    }

    private fun saveLevels(levels: List<Int>) {
        prefs.edit { putString(KEY_LEVELS, levels.joinToString(",")) }
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "release equalizer gagal", e)
        }
        equalizer = null
    }

    companion object {
        private const val TAG = "ZmusicEq"
        private const val KEY_ENABLED = "eq_enabled"
        private const val KEY_LEVELS  = "eq_band_levels"
        private const val KEY_PRESET  = "eq_preset"
    }
}
