package com.zaaam.Zmusic.ui.explore

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.Zmusic.data.MusicRepository
import com.zaaam.Zmusic.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.Calendar
import javax.inject.Inject

data class GenreSection(
    val name: String,
    val emoji: String,
    val queries: List<String>,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true
)

data class ExploreState(
    val genres: List<GenreSection> = defaultGenres(),
    val isInitialLoad: Boolean = true
)

fun defaultGenres(): List<GenreSection> {
    val year = Calendar.getInstance().get(Calendar.YEAR).toString()
    return listOf(
        GenreSection("Pop Indonesia", "🎤", listOf("pop indonesia terbaru $year", "lagu pop indonesia hits")),
        GenreSection("Dangdut", "🥁", listOf("dangdut populer terbaru", "dangdut koplo hits $year")),
        GenreSection("Rock & Metal", "🎸", listOf("rock indonesia terbaik", "rock alternative indonesia")),
        GenreSection("K-Pop", "🇰🇷", listOf("kpop populer $year", "kpop hits terbaru")),
        GenreSection("Hip Hop & Rap", "🎧", listOf("hip hop indonesia terbaru", "rap indonesia $year")),
        GenreSection("R&B & Soul", "🎷", listOf("rnb indonesia", "soul music indonesia")),
        GenreSection("Acoustic & Chill", "🌿", listOf("acoustic cover indonesia", "musik santai lofi")),
        GenreSection("EDM & Remix", "⚡", listOf("edm remix indonesia", "dj remix terbaru $year")),
        GenreSection("Barat Populer", "🌍", listOf("western pop hits $year", "billboard hot 100 songs")),
        GenreSection("Nostalgia", "💫", listOf("lagu nostalgia indonesia", "tembang kenangan populer"))
    )
}

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ExploreState())
    val state: StateFlow<ExploreState> = _state.asStateFlow()

    // ══════════════════════════════════════════════════════════════════════════
    // FIX #13: Semaphore membatasi request concurrent ke max 3.
    //
    // MASALAH LAMA:
    //   loadAllGenres() launch 10 coroutine bersamaan → 10 genre × 2 query
    //   = 20 concurrent network request ke NewPipeExtractor. Ini bisa:
    //   1. Trigger rate limiting dari YouTube → request gagal → genre kosong
    //   2. Overwhelm OkHttp connection pool → timeout cascade
    //   3. Spike memory karena 20 response di-buffer bersamaan
    //
    // SOLUSI:
    //   Semaphore(3) → max 3 genre di-fetch bersamaan. Masih cukup cepat
    //   (10 genre selesai dalam ~4 batch), tapi tidak membanjiri server.
    // ══════════════════════════════════════════════════════════════════════════
    private val fetchSemaphore = Semaphore(3)

    init { loadAllGenres() }

    fun loadAllGenres() {
        val genres = defaultGenres()
        _state.value = ExploreState(genres = genres, isInitialLoad = true)

        genres.forEachIndexed { index, genre ->
            viewModelScope.launch {
                fetchSemaphore.withPermit {
                    try {
                        val songs = repository.searchByGenre(genre.queries)
                        updateGenre(index, songs)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w("ZmusicExplore", "load genre \"${genre.name}\" gagal", e)
                        updateGenre(index, emptyList())
                    }
                }
            }
        }
    }

    fun loadGenre(index: Int) {
        val genre = _state.value.genres.getOrNull(index) ?: return
        viewModelScope.launch {
            // FIX #10: Pakai _state.update{} yang atomic
            _state.update { current ->
                val updated = current.genres.toMutableList()
                updated[index] = genre.copy(isLoading = true)
                current.copy(genres = updated)
            }

            fetchSemaphore.withPermit {
                try {
                    val songs = repository.searchByGenre(genre.queries)
                    updateGenre(index, songs)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w("ZmusicExplore", "reload genre index=$index gagal", e)
                    updateGenre(index, emptyList())
                }
            }
        }
    }

    // FIX #10: Pakai _state.update{} yang atomic.
    private fun updateGenre(index: Int, songs: List<Song>) {
        _state.update { current ->
            val updated = current.genres.toMutableList()
            if (index < updated.size) {
                updated[index] = updated[index].copy(songs = songs, isLoading = false)
            }
            val allLoaded = updated.none { it.isLoading }
            current.copy(genres = updated, isInitialLoad = !allLoaded)
        }
    }
}
