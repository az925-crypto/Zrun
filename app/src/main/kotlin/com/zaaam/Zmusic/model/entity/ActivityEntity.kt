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
    /** Waktu bergerak saja (ala Strava). 0 untuk data lama sebelum kolom ini ada. */
    val movingMillis: Long = 0L,
    val distanceMeters: Double,
    val avgSpeedKmh: Double,
    val route: List<GeoPoint>
) {
    /**
     * Pace rata-rata (detik per km) dari WAKTU BERGERAK — istirahat tidak merusak
     * pace. Fallback ke durasi total untuk aktivitas lama tanpa movingMillis.
     */
    val avgPaceSecPerKm: Long
        get() {
            val km = distanceMeters / 1000.0
            if (km <= 0) return 0L
            val baseMs = if (movingMillis > 0) movingMillis else durationMillis
            return (baseMs / 1000.0 / km).toLong()
        }
}
