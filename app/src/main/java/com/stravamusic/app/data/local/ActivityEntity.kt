package com.stravamusic.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A completed, saved workout. */
@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String,            // "Run", "Ride", "Walk"
    val startTime: Long,         // epoch millis
    val durationMillis: Long,
    val distanceMeters: Double,
    val avgSpeedKmh: Double,
    val route: List<GeoPoint>
) {
    /** Average pace in seconds per kilometer (0 if no distance). */
    val avgPaceSecPerKm: Long
        get() {
            val km = distanceMeters / 1000.0
            return if (km > 0) (durationMillis / 1000.0 / km).toLong() else 0L
        }
}
