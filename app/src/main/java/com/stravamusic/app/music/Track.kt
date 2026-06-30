package com.stravamusic.app.music

/** A playable song. `uri` can be a content://, file://, http(s):// or raw resource path. */
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val uri: String,
    val durationMs: Long = 0L
)

/** Snapshot of what the player is currently doing, observed by the UI. */
data class MusicState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val queue: List<Track> = emptyList()
)
