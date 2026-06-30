package com.stravamusic.app.tracking

import com.stravamusic.app.data.local.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Live state of the current (in-progress) recording. */
data class TrackingData(
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedMillis: Long = 0L,
    val distanceMeters: Double = 0.0,
    val currentSpeedKmh: Double = 0.0,
    val route: List<GeoPoint> = emptyList()
) {
    val avgSpeedKmh: Double
        get() {
            val hours = elapsedMillis / 3_600_000.0
            val km = distanceMeters / 1000.0
            return if (hours > 0) km / hours else 0.0
        }
}

/**
 * Shared bus between [TrackingService] (writer) and the UI/ViewModel (reader).
 * Keeping it as a process-wide singleton avoids binding to the service.
 */
object TrackingBus {
    private val _data = MutableStateFlow(TrackingData())
    val data: StateFlow<TrackingData> = _data.asStateFlow()

    fun update(transform: (TrackingData) -> TrackingData) {
        _data.value = transform(_data.value)
    }

    fun reset() {
        _data.value = TrackingData()
    }
}
