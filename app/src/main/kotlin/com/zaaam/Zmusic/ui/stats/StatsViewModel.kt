package com.zaaam.Zmusic.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.Zmusic.data.Milestone
import com.zaaam.Zmusic.data.MilestoneRepository
import com.zaaam.Zmusic.data.MusicRepository
import com.zaaam.Zmusic.model.entity.MoodStatResult
import com.zaaam.Zmusic.model.entity.TopArtistResult
import com.zaaam.Zmusic.model.entity.TopSongResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    repository: MusicRepository,
    // ── NEW: Milestone + Streak ─────────────────────────────────────────
    milestoneRepository: MilestoneRepository
) : ViewModel() {

    val totalPlays: StateFlow<Int> = repository.getTotalPlays()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // FIX: getTotalDuration() return Flow<Long?> karena SUM() bisa NULL
    val totalDuration: StateFlow<Long> = repository.getTotalDuration()
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val topSongs: StateFlow<List<TopSongResult>> = repository.getTopSongs()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val topArtists: StateFlow<List<TopArtistResult>> = repository.getTopArtists()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val moodDistribution: StateFlow<List<MoodStatResult>> = repository.getMoodDistribution()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentMood: StateFlow<MoodStatResult?> = repository.getRecentMood()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // ── NEW: Listening Streak ──────────────────────────────────────────────
    val listeningStreak: StateFlow<Int> = milestoneRepository.listeningStreak
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // ── NEW: Milestones ────────────────────────────────────────────────────
    val milestones: StateFlow<List<Milestone>> = milestoneRepository.milestones
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}
