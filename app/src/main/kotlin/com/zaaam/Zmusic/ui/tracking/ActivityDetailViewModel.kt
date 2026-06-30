package com.zaaam.Zmusic.ui.tracking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.Zmusic.data.local.ActivityDao
import com.zaaam.Zmusic.model.entity.ActivityEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityDetailViewModel @Inject constructor(
    private val activityDao: ActivityDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val activityId: Long = savedStateHandle.get<String>("activityId")?.toLongOrNull() ?: 0L

    private val _activity = MutableStateFlow<ActivityEntity?>(null)
    val activity: StateFlow<ActivityEntity?> = _activity.asStateFlow()

    init {
        viewModelScope.launch { _activity.value = activityDao.getById(activityId) }
    }
}
