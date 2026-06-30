package com.zaaam.Zmusic.ui.trending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.Zmusic.data.MusicRepository
import com.zaaam.Zmusic.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrendingState(
    val isLoading: Boolean = true,
    val songs: List<Song> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class TrendingViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TrendingState())
    val state: StateFlow<TrendingState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = TrendingState(isLoading = true)
            try {
                val songs = repository.getTrending()
                _state.value = TrendingState(isLoading = false, songs = songs)
            } catch (e: Exception) {
                _state.value = TrendingState(
                    isLoading = false,
                    error = "Gagal memuat trending: ${e.message?.take(80)}"
                )
            }
        }
    }
}
