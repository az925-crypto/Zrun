package com.zaaam.Zmusic.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.Zmusic.data.MusicRepository
import com.zaaam.Zmusic.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeContent(
    val greeting: String,
    val heroSong: Song?,
    val featuredSongs: List<Song>,
    val trendingSongs: List<Song>,
    val recentSongs: List<Song>,
    val allSongs: List<Song>,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true
)

sealed class HomeState {
    object Loading : HomeState()
    data class Success(val content: HomeContent) : HomeState()
    data class Error(val message: String) : HomeState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _state = MutableStateFlow<HomeState>(HomeState.Loading)
    val state: StateFlow<HomeState> = _state.asStateFlow()

    // FIX #8: Dynamic year — tidak hardcode "2025" lagi
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()

    // Query pool untuk infinite scroll — rotate setiap kali loadMore
    private val moreQueries = listOf(
        "lagu indonesia terbaru $currentYear",
        "pop indonesia populer",
        "lagu viral tiktok indonesia",
        "top hits asia",
        "lagu romantis indonesia",
        "rock indonesia terbaik",
        "lagu galau terbaru",
        "musik dangdut populer",
        "lagu barat terbaru $currentYear",
        "acoustic cover indonesia",
        "lagu nostalgia indonesia",
        "hip hop indonesia terbaru",
        "k-pop populer $currentYear",
        "lagu senja santai",
        "edm remix indonesia"
    )
    private var queryIndex = 0
    private var isLoadingMore = false

    // Simpan semua ID yang sudah diload untuk deduplikasi
    private val loadedIds = mutableSetOf<String>()

    init {
        loadDiscovery()
        observeRecentSongs()
    }

    // FIX #1: Observe riwayat NYATA dari database play_history,
    // bukan fake slice dari discovery result.
    private fun observeRecentSongs() {
        viewModelScope.launch {
            repository.getRecentSongs(6).collect { recentFromDb ->
                val currentState = _state.value
                if (currentState is HomeState.Success) {
                    _state.value = HomeState.Success(
                        currentState.content.copy(recentSongs = recentFromDb)
                    )
                }
            }
        }
    }

    fun loadDiscovery() {
        viewModelScope.launch {
            _state.value = HomeState.Loading
            queryIndex = 0
            loadedIds.clear()
            try {
                val songs = repository.getDiscovery()
                if (songs.isEmpty()) {
                    _state.value = HomeState.Error("Tidak ada konten ditemukan. Coba lagi.")
                } else {
                    loadedIds.addAll(songs.map { it.id })

                    // FIX #4: featuredSongs dan trendingSongs TIDAK overlap lagi.
                    // Featured ambil 6 pertama, Trending ambil 8 SETELAHNYA (drop 6).
                    // FIX #1: recentSongs = emptyList(), diisi dari observeRecentSongs() via DB.
                    val content = HomeContent(
                        greeting      = getGreeting(),
                        heroSong      = songs.firstOrNull(),
                        featuredSongs = songs.take(6),
                        trendingSongs = songs.drop(6).take(8),
                        recentSongs   = emptyList(),
                        allSongs      = songs,
                        isLoadingMore = false,
                        hasMore       = true
                    )
                    _state.value = HomeState.Success(content)
                }
            } catch (e: Exception) {
                val detail = e.message?.take(100) ?: "Unknown error"
                _state.value = HomeState.Error("Gagal memuat: $detail")
            }
        }
    }

    /**
     * Load more songs — dipanggil saat scroll mendekati bawah.
     * Cari dengan query berbeda setiap kali, deduplikasi dengan yang sudah ada.
     *
     * FIX #4: Diganti dari recursive call ke iterative loop dalam satu coroutine.
     * Sebelumnya: kalau query kosong → recursive loadMore() → potensi loop 15x
     * dengan setiap recursive call launch coroutine baru.
     * Sekarang: satu coroutine yang iterate max 3 query berturut-turut.
     */
    fun loadMore() {
        if (isLoadingMore) return
        val currentState = _state.value
        if (currentState !is HomeState.Success) return
        if (!currentState.content.hasMore) return

        isLoadingMore = true
        val content = currentState.content

        // Set loading indicator
        _state.value = HomeState.Success(content.copy(isLoadingMore = true))

        viewModelScope.launch {
            try {
                var newSongs: List<Song> = emptyList()
                // FIX #4: Iterative — coba max 3 query berturut-turut dalam satu coroutine
                val maxRetries = 3
                var retryCount = 0

                while (newSongs.isEmpty() && retryCount < maxRetries && queryIndex < moreQueries.size) {
                    val query = moreQueries[queryIndex % moreQueries.size]
                    queryIndex++
                    retryCount++

                    newSongs = repository.search(query)
                        .filter { it.id !in loadedIds }
                }

                loadedIds.addAll(newSongs.map { it.id })

                val updatedAll = content.allSongs + newSongs
                val hasMore = queryIndex < moreQueries.size

                _state.value = HomeState.Success(
                    content.copy(
                        allSongs      = updatedAll,
                        isLoadingMore = false,
                        hasMore       = hasMore
                    )
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Gagal load more — tetap tampilkan yang sudah ada, tapi tinggalkan jejak
                Log.w("ZmusicHomeVM", "loadMore gagal — hasil sebelumnya dipertahankan", e)
                _state.value = HomeState.Success(
                    content.copy(isLoadingMore = false)
                )
            } finally {
                isLoadingMore = false
            }
        }
    }

    private fun getGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            hour < 11 -> "Selamat Pagi"
            hour < 15 -> "Selamat Siang"
            hour < 18 -> "Selamat Sore"
            else      -> "Selamat Malam"
        }
    }
}
