package com.stravamusic.app.data.local

/** A single recorded GPS sample along a route. */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)
