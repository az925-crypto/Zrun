package com.zaaam.Zmusic.ui.theme

import androidx.compose.ui.graphics.Color
import com.zaaam.Zmusic.model.Mood

// ═══════════════════════════════════════════════════════════════════════
//  WARNA MOOD — sumber kebenaran tunggal.
//  Sebelumnya map ini diduplikat persis di MoodScreen (moodColors) dan
//  StatsScreen (moodStatsColors). Ubah warna mood cukup di sini.
// ═══════════════════════════════════════════════════════════════════════

/** Pasangan warna gradient per mood (terang → gelap). */
val MoodGradientColors: Map<Mood, List<Color>> = mapOf(
    Mood.HAPPY     to listOf(Color(0xFFFFF176), Color(0xFFFFD54F)),
    Mood.SAD       to listOf(Color(0xFF90CAF9), Color(0xFF5C6BC0)),
    Mood.ENERGETIC to listOf(Color(0xFFFF8A65), Color(0xFFE53935)),
    Mood.CHILL     to listOf(Color(0xFFA5D6A7), Color(0xFF26A69A)),
    Mood.ROMANCE   to listOf(Color(0xFFF48FB1), Color(0xFFEC407A)),
    Mood.HYPE      to listOf(Color(0xFFCE93D8), Color(0xFF8E24AA))
)
