package com.zaaam.Zmusic.ui.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.Zmusic.data.local.ActivityDao
import com.zaaam.Zmusic.model.entity.ActivityEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivitiesViewModel @Inject constructor(
    private val activityDao: ActivityDao
) : ViewModel() {

    val activities: StateFlow<List<ActivityEntity>> =
        activityDao.observeAll().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun delete(activity: ActivityEntity) {
        viewModelScope.launch { activityDao.delete(activity) }
    }
}
