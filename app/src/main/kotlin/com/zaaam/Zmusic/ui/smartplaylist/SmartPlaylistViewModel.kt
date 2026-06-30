package com.zaaam.Zmusic.ui.smartplaylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.Zmusic.data.MusicRepository
import com.zaaam.Zmusic.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmartPlaylist(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    val songs: List<Song>,
    val isLoading: Boolean = false
)

@HiltViewModel
class SmartPlaylistViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _playlists = MutableStateFlow<List<SmartPlaylist>>(emptyList())
    val playlists: StateFlow<List<SmartPlaylist>> = _playlists.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        _isLoading.value = true
        viewModelScope.launch {
            val topPicksDeferred    = async { repository.generateTopPicksPlaylist() }
            val moodMixDeferred     = async { repository.generateMoodMixPlaylist() }
            val recentFavesDeferred = async { repository.generateRecentFavesPlaylist() }

            val topPicks    = topPicksDeferred.await()
            val moodMix     = moodMixDeferred.await()
            val recentFaves = recentFavesDeferred.await()

            _playlists.value = buildList {
                if (topPicks.isNotEmpty()) add(SmartPlaylist(
                    id          = "top_picks",
                    name        = "Top Picks",
                    description = "Lagu-lagu yang paling sering kamu putar",
                    emoji       = "🔥",
                    songs       = topPicks
                ))
                if (moodMix.isNotEmpty()) add(SmartPlaylist(
                    id          = "mood_mix",
                    name        = "Mood Mix",
                    description = "Sesuai mood pendengaranmu belakangan ini",
                    emoji       = "✨",
                    songs       = moodMix
                ))
                if (recentFaves.isNotEmpty()) add(SmartPlaylist(
                    id          = "recent_faves",
                    name        = "Recent Faves",
                    description = "Lagu baru yang sudah kamu putar berkali-kali",
                    emoji       = "💙",
                    songs       = recentFaves
                ))
            }
            _isLoading.value = false
        }
    }
}
