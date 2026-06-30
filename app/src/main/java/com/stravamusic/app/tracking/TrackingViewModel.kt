package com.stravamusic.app.tracking

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stravamusic.app.StravaMusicApp
import com.stravamusic.app.data.local.ActivityEntity
import com.stravamusic.app.data.repository.ActivityRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrackingViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: ActivityRepository = (app as StravaMusicApp).activityRepository

    val state: StateFlow<TrackingData> = TrackingBus.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrackingData()
    )

    fun start() = sendCommand(TrackingService.ACTION_START)
    fun pause() = sendCommand(TrackingService.ACTION_PAUSE)
    fun resume() = sendCommand(TrackingService.ACTION_RESUME)
    fun stop() = sendCommand(TrackingService.ACTION_STOP)

    /** Persist the just-finished activity, then clear the live state. */
    fun saveActivity(title: String, type: String, onSaved: () -> Unit = {}) {
        val data = TrackingBus.data.value
        viewModelScope.launch {
            repository.saveActivity(
                ActivityEntity(
                    title = title.ifBlank { defaultTitle(type) },
                    type = type,
                    startTime = data.route.firstOrNull()?.timestamp ?: System.currentTimeMillis(),
                    durationMillis = data.elapsedMillis,
                    distanceMeters = data.distanceMeters,
                    avgSpeedKmh = data.avgSpeedKmh,
                    route = data.route
                )
            )
            TrackingBus.reset()
            onSaved()
        }
    }

    /** Throw away the current recording without saving. */
    fun discard() = TrackingBus.reset()

    private fun defaultTitle(type: String): String = "$type • ${java.text.SimpleDateFormat(
        "EEE, d MMM HH:mm", java.util.Locale.getDefault()
    ).format(java.util.Date())}"

    private fun sendCommand(action: String) {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, TrackingService::class.java).apply { this.action = action }
        ContextCompat.startForegroundService(ctx, intent)
    }
}
