package com.stravamusic.app.data.local

import androidx.room.TypeConverter

/**
 * Serializes the route polyline to/from a compact string so it can live in a
 * single Room column. Format: "lat,lng,ts;lat,lng,ts;..." (empty string = no points).
 */
class Converters {

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
