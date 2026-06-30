package com.zaaam.Zmusic.ui.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.Zmusic.data.MusicRepository
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.model.entity.RecentSongResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

// ══════════════════════════════════════════════════════════════════════════
// FIX #6: Repository pattern violation — DIPERBAIKI.
//
// Sekarang pakai method yang SUDAH ADA di MusicRepository:
//   - getRecentSongs(limit) → Flow<List<Song>> (pakai JOIN query, FIX #3)
//   - getRecentlyPlayedRaw(limit) → Flow<List<RecentSongResult>> (wrapper baru)
//
// PlayHistoryDao tidak lagi di-inject langsung ke ViewModel.
// ══════════════════════════════════════════════════════════════════════════

@HiltViewModel
class RecentlyPlayedViewModel @Inject constructor(
    private val repository: MusicRepository
) : ViewModel() {

    // Pakai getRecentSongs() yang sudah ada di MusicRepository (JOIN query)
    val recentSongs: Flow<List<Song>> = repository.getRecentSongs(50)

    /** Raw results with extra info (playCount, totalListened) */
    val recentResults: Flow<List<RecentSongResult>> = repository.getRecentlyPlayedRaw(50)

    fun deleteHistoryItem(songId: String) {
        viewModelScope.launch {
            repository.deleteHistoryBySongId(songId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }
}
