package com.zaaam.Zmusic.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zaaam.Zmusic.model.GeoPoint

/** Aktivitas olahraga yang sudah selesai & disimpan (riwayat). */
@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String,            // MVP: "Run"
    val startTime: Long,         // epoch millis
    val durationMillis: Long,
    val distanceMeters: Double,
    val avgSpeedKmh: Double,
    val route: List<GeoPoint>
) {
    /** Pace rata-rata dalam detik per kilometer (0 kalau tak ada jarak). */
    val avgPaceSecPerKm: Long
        get() {
            val km = distanceMeters / 1000.0
            return if (km > 0) (durationMillis / 1000.0 / km).toLong() else 0L
        }
}
