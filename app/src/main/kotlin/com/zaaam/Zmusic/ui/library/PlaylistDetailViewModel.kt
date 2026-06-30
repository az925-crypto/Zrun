package com.zaaam.Zmusic.ui.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.Zmusic.data.MusicRepository
import com.zaaam.Zmusic.model.entity.PlaylistWithSongs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val repository: MusicRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val playlistId: Long = checkNotNull(savedStateHandle["playlistId"])

    val playlistWithSongs: StateFlow<PlaylistWithSongs?> =
        repository.getPlaylistWithSongs(playlistId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun removeSong(songId: String) {
        viewModelScope.launch {
            repository.removeFromPlaylist(playlistId, songId)
        }
    }
}
