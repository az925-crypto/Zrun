package com.zaaam.Zmusic.model

data class PlayHistory(
    val songId: String,
    val title: String,
    val artist: String,
    val playedAt: Long,
    val durationListened: Long
)
