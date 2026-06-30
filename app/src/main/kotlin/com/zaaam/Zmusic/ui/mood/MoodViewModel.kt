package com.zaaam.Zmusic.ui.mood

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.Zmusic.data.MusicRepository
import com.zaaam.Zmusic.model.Mood
import com.zaaam.Zmusic.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MoodState {
    object Idle : MoodState()
    object Loading : MoodState()
    data class Success(val mood: Mood, val songs: List<Song>) : MoodState()
    data class Error(val message: String) : MoodState()
}

@HiltViewModel
class MoodViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _state = MutableStateFlow<MoodState>(MoodState.Idle)
    val state: StateFlow<MoodState> = _state.asStateFlow()

    private val _selectedMood = MutableStateFlow<Mood?>(null)
    val selectedMood: StateFlow<Mood?> = _selectedMood.asStateFlow()

    private var searchJob: Job? = null

    fun selectMood(mood: Mood) {
        if (_selectedMood.value == mood && _state.value is MoodState.Success) return
        _selectedMood.value = mood
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.value = MoodState.Loading
            try {
                val songs = repository.searchByMood(mood)
                _state.value = if (songs.isEmpty())
                    MoodState.Error("Tidak ada lagu ditemukan untuk mood ini")
                else
                    MoodState.Success(mood, songs)
            } catch (e: Exception) {
                _state.value = MoodState.Error("Gagal memuat: ${e.message?.take(60)}")
            }
        }
    }

    fun retry() {
        _selectedMood.value?.let { selectMood(it) }
    }
}
