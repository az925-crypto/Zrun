package com.zaaam.Zmusic.ui.tracking

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.Zmusic.data.local.ActivityDao
import com.zaaam.Zmusic.model.entity.ActivityEntity
import com.zaaam.Zmusic.tracking.TrackingData
import com.zaaam.Zmusic.tracking.TrackingService
import com.zaaam.Zmusic.tracking.TrackingStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val app: Application,
    private val stateHolder: TrackingStateHolder,
    private val activityDao: ActivityDao
) : ViewModel() {

    val state: StateFlow<TrackingData> = stateHolder.data.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrackingData()
    )

    fun start() = sendCommand(TrackingService.ACTION_START)
    fun pause() = sendCommand(TrackingService.ACTION_PAUSE)
    fun resume() = sendCommand(TrackingService.ACTION_RESUME)
    fun stop() = sendCommand(TrackingService.ACTION_STOP)

    /** Simpan aktivitas yang baru selesai, lalu bersihkan state live. */
    fun saveActivity(title: String, type: String = "Run", onSaved: () -> Unit = {}) {
        val data = stateHolder.data.value
        viewModelScope.launch {
            activityDao.insert(
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
            stateHolder.reset()
            onSaved()
        }
    }

    /** Buang rekaman saat ini tanpa menyimpan. */
    fun discard() = stateHolder.reset()

    private fun defaultTitle(type: String): String {
        val time = SimpleDateFormat("EEE, d MMM HH:mm", Locale.getDefault()).format(Date())
        return "$type • $time"
    }

    private fun sendCommand(action: String) {
        val intent = Intent(app, TrackingService::class.java).apply { this.action = action }
        ContextCompat.startForegroundService(app, intent)
    }
}
