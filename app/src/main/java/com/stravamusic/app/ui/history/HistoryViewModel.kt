package com.stravamusic.app.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stravamusic.app.StravaMusicApp
import com.stravamusic.app.data.local.ActivityEntity
import com.stravamusic.app.data.repository.ActivityRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: ActivityRepository = (app as StravaMusicApp).activityRepository

    val activities: StateFlow<List<ActivityEntity>> =
        repository.observeActivities().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun delete(activity: ActivityEntity) {
        viewModelScope.launch { repository.deleteActivity(activity) }
    }
}
