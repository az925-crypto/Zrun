package com.zaaam.Zmusic.model

/**
 * FIX #9: Mood enum dipindahkan ke file terpisah Mood.kt.
 * Sebelumnya didefinisikan di dalam Lyrics.kt yang tidak relevan,
 * melanggar prinsip single responsibility dan membingungkan.
 */
enum class Mood(val emoji: String, val label: String, val queries: List<String>) {
    HAPPY("😊", "Happy", listOf("lagu happy vibes ceria", "happy songs upbeat")),
    SAD("😢", "Sad", listOf("lagu sedih galau terbaru", "sad song Indonesia")),
    ENERGETIC("⚡", "Energetic", listOf("lagu semangat workout hype", "energetic music pump up")),
    CHILL("😌", "Chill", listOf("lagu santai lofi indonesia", "chill vibes relaxing")),
    ROMANCE("❤️", "Romance", listOf("lagu romantis terbaik", "love song romantic")),
    HYPE("🔥", "Hype", listOf("lagu party hits indonesia", "hype music trending"))
}
