package com.zaaam.Zmusic.data.local

import androidx.room.TypeConverter
import com.zaaam.Zmusic.model.GeoPoint

/**
 * Serialisasi rute (polyline) ke/dari satu kolom String supaya muat di Room.
 * Format: "lat,lng,ts;lat,lng,ts;..." (string kosong = tanpa titik).
 */
class RouteConverters {

    @TypeConverter
    fun fromRoute(points: List<GeoPoint>): String =
        points.joinToString(separator = ";") { "${it.latitude},${it.longitude},${it.timestamp}" }

    @TypeConverter
    fun toRoute(encoded: String): List<GeoPoint> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split(";").mapNotNull { entry ->
            val parts = entry.split(",")
            if (parts.size != 3) return@mapNotNull null
            val lat = parts[0].toDoubleOrNull() ?: return@mapNotNull null
            val lng = parts[1].toDoubleOrNull() ?: return@mapNotNull null
            val ts = parts[2].toLongOrNull() ?: return@mapNotNull null
            GeoPoint(lat, lng, ts)
        }
    }
}
