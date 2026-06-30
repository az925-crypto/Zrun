package com.zaaam.Zmusic.model

data class Song(
    val id: String,              // videoId YouTube
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val duration: Long,          // milidetik
    val streamUrl: String? = null  // tidak disimpan ke DB — selalu fetch ulang
)
