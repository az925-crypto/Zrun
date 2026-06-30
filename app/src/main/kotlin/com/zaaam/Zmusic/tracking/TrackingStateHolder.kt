package com.zaaam.Zmusic.tracking

import com.zaaam.Zmusic.model.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** State aktivitas yang sedang berjalan (live recording). */
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
 * Jembatan state antara [TrackingService] (penulis) dan UI/ViewModel (pembaca).
 * Singleton Hilt → satu instance dipakai bersama tanpa perlu bind ke service.
 */
@Singleton
class TrackingStateHolder @Inject constructor() {

    private val _data = MutableStateFlow(TrackingData())
    val data: StateFlow<TrackingData> = _data.asStateFlow()

    fun update(transform: (TrackingData) -> TrackingData) {
        _data.value = transform(_data.value)
    }

    fun reset() {
        _data.value = TrackingData()
    }
}
