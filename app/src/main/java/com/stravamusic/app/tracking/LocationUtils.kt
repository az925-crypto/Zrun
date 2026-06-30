package com.stravamusic.app.tracking

import com.stravamusic.app.data.local.GeoPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object LocationUtils {

    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Great-circle distance between two points in meters (Haversine). */
    fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)

        val h = sin(dLat / 2) * sin(dLat / 2) +
            sin(dLng / 2) * sin(dLng / 2) * cos(lat1) * cos(lat2)
        return 2 * EARTH_RADIUS_M * atan2(sqrt(h), sqrt(1 - h))
    }

    /** Format milliseconds as H:MM:SS (or M:SS under an hour). */
    fun formatDuration(millis: Long): String {
        val totalSec = millis / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /** Pace (sec/km) as M:SS, "--:--" when undefined. */
    fun formatPace(secPerKm: Long): String {
        if (secPerKm <= 0) return "--:--"
        val m = secPerKm / 60
        val s = secPerKm % 60
        return "%d:%02d".format(m, s)
    }

    /** Distance in km, one decimal. */
    fun formatDistanceKm(meters: Double): String = "%.2f".format(meters / 1000.0)
}
