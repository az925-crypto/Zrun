package com.zaaam.Zmusic.model

// FIX #9: Mood enum dipindahkan ke Mood.kt — file ini sekarang hanya berisi
// model data untuk lirik lagu sesuai tanggung jawabnya.

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class Lyrics(
    val plain: String,
    val synced: List<LyricLine> = emptyList()
) {
    val hasSynced: Boolean get() = synced.isNotEmpty()
}
