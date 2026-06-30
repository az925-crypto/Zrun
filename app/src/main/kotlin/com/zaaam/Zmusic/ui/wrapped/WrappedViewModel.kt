package com.zaaam.Zmusic.ui.wrapped

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.Zmusic.data.MusicRepository
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.model.entity.MoodStatResult
import com.zaaam.Zmusic.model.entity.TopArtistResult
import com.zaaam.Zmusic.model.entity.TopSongResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class WrappedData(
    val totalPlays: Int,
    val totalMinutes: Long,
    val topSongs: List<TopSongResult>,
    val topArtists: List<TopArtistResult>,
    val moodDistribution: List<MoodStatResult>,
    val dominantMood: String?,
    val listeningStreak: Int,
    val year: Int,
    val month: String?  // null = yearly wrapped
)

sealed class WrappedState {
    object Loading : WrappedState()
    data class Ready(val data: WrappedData) : WrappedState()
    object Empty : WrappedState()
}

@HiltViewModel
class WrappedViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _state = MutableStateFlow<WrappedState>(WrappedState.Loading)
    val state: StateFlow<WrappedState> = _state.asStateFlow()

    /** Mode: "monthly" atau "yearly" */
    private val _mode = MutableStateFlow("monthly")
    val mode: StateFlow<String> = _mode.asStateFlow()

    init { load("monthly") }

    fun load(mode: String) {
        _mode.value = mode
        _state.value = WrappedState.Loading
        viewModelScope.launch {
            try {
                val cal = Calendar.getInstance()
                val year = cal.get(Calendar.YEAR)

                // FIX POTENSI #2: Hitung rentang waktu yang benar berdasarkan mode.
                //
                // MASALAH LAMA: Semua query tidak difilter tanggal → monthly wrapped
                // dan yearly wrapped menampilkan data yang identik (seluruh histori).
                //
                // FIX: Hitung startMs dan endMs:
                //   - monthly → awal sampai akhir bulan berjalan
                //   - yearly  → 1 Januari sampai 31 Desember tahun berjalan
                val (startMs, endMs) = when (mode) {
                    "monthly" -> {
                        val start = Calendar.getInstance().apply {
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val end = Calendar.getInstance().apply {
                            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis
                        Pair(start, end)
                    }
                    else -> { // "yearly"
                        val start = Calendar.getInstance().apply {
                            set(Calendar.MONTH, Calendar.JANUARY)
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val end = Calendar.getInstance().apply {
                            set(Calendar.MONTH, Calendar.DECEMBER)
                            set(Calendar.DAY_OF_MONTH, 31)
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis
                        Pair(start, end)
                    }
                }

                // Semua query sekarang difilter berdasarkan rentang waktu yang tepat
                val totalPlays   = repository.getTotalPlaysBetween(startMs, endMs).first()
                val totalDurMs   = repository.getTotalDurationBetween(startMs, endMs).first() ?: 0L
                val topSongs     = repository.getTopSongsBetween(startMs, endMs, 5).first()
                val topArtists   = repository.getTopArtistsBetween(startMs, endMs, 3).first()
                val moodDist     = repository.getMoodDistributionBetween(startMs, endMs).first()
                val dominantMood = moodDist.maxByOrNull { it.playCount }?.mood

                if (totalPlays == 0) {
                    _state.value = WrappedState.Empty
                    return@launch
                }

                val month = if (mode == "monthly") {
                    arrayOf("Januari","Februari","Maret","April","Mei","Juni",
                        "Juli","Agustus","September","Oktober","November","Desember")
                        .getOrNull(cal.get(Calendar.MONTH))
                } else null

                _state.value = WrappedState.Ready(
                    WrappedData(
                        totalPlays       = totalPlays,
                        totalMinutes     = totalDurMs / 60_000L,
                        topSongs         = topSongs,
                        topArtists       = topArtists,
                        moodDistribution = moodDist,
                        dominantMood     = dominantMood,
                        listeningStreak  = 0,
                        year             = year,
                        month            = month
                    )
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("ZmusicWrapped", "agregasi data Wrapped gagal — tampil Empty", e)
                _state.value = WrappedState.Empty
            }
        }
    }
}
