package com.zaaam.Zmusic.ui.artist

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

sealed class ArtistScreenState {
    object Idle    : ArtistScreenState()
    object Loading : ArtistScreenState()
    data class Success(val songs: List<Song>) : ArtistScreenState()
    data class Error(val message: String)     : ArtistScreenState()
}

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ArtistScreenState>(ArtistScreenState.Idle)
    val state: StateFlow<ArtistScreenState> = _state.asStateFlow()

    private val _artistName = MutableStateFlow("")
    val artistName: StateFlow<String> = _artistName.asStateFlow()

    fun loadArtist(artistName: String) {
        if (_state.value is ArtistScreenState.Loading) return
        _artistName.value = artistName
        _state.value = ArtistScreenState.Loading

        viewModelScope.launch {
            try {
                val songs = repository.searchByArtist(artistName)
                _state.value = if (songs.isNotEmpty())
                    ArtistScreenState.Success(songs)
                else
                    ArtistScreenState.Error("Tidak ada lagu ditemukan untuk \"$artistName\"")
            } catch (e: Exception) {
                _state.value = ArtistScreenState.Error(
                    "Gagal memuat lagu: ${e.message ?: "Coba lagi"}"
                )
            }
        }
    }

    fun retry() {
        val name = _artistName.value
        if (name.isNotBlank()) loadArtist(name)
    }
}
