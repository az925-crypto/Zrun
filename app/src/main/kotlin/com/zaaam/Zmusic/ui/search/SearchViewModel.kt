package com.zaaam.Zmusic.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.Zmusic.data.MusicRepository
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.model.entity.SearchHistoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    object Empty : SearchState()
    data class Success(val results: List<Song>) : SearchState()
    data class Error(val message: String) : SearchState()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SearchState>(SearchState.Idle)
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // Search history dari Room - auto-update saat ada perubahan
    val searchHistory: StateFlow<List<SearchHistoryEntity>> = repository
        .getSearchHistory()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _state.value = SearchState.Idle
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.value = SearchState.Loading
            try {
                val results = repository.search(query)
                _state.value = if (results.isEmpty()) SearchState.Empty
                               else SearchState.Success(results)

                // Simpan ke search history kalau berhasil
                if (results.isNotEmpty()) {
                    repository.saveSearchQuery(query)
                }
            } catch (e: Exception) {
                _state.value = SearchState.Error("Gagal mencari lagu")
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _query.value = ""
        _state.value = SearchState.Idle
    }

    fun searchFromHistory(query: String) {
        _query.value = query
        search(query)
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteSearchHistoryById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearSearchHistory()
        }
    }
}
