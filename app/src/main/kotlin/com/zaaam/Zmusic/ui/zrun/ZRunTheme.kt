package com.zaaam.Zmusic.ui.zrun

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Design system "Ember" untuk ZRun — UI baru dari nol (di atas mesin Zmusic).
 * Dipakai langsung di composable (ZR.xxx), tidak lewat MaterialTheme.colorScheme,
 * supaya independen dari tema lama Zmusic.
 */
object ZR {
    // Base gelap berlapis
    val Bg = Color(0xFF0A0B0E)
    val S1 = Color(0xFF15171C)
    val S2 = Color(0xFF1E2128)
    val S3 = Color(0xFF272B33)

    val Stroke = Color(0x14FFFFFF)   // white .08
    val Stroke2 = Color(0x24FFFFFF)  // white .14

    val Tx = Color(0xFFFFFFFF)
    val Mut = Color(0xFF9298A4)
    val Faint = Color(0xFF5C616C)

    // Aksen
    val Ember1 = Color(0xFFFF7A3D)
    val Ember2 = Color(0xFFFF2E63)
    val Violet = Color(0xFF8B5CFF)
    val Mint = Color(0xFF2DE0C0)
    val Sky = Color(0xFF37B3FF)
    val Stop = Color(0xFFFF3B5C)

    val Ember = Brush.linearGradient(listOf(Ember1, Ember2))
    val Music = Brush.linearGradient(listOf(Violet, Ember2))
    val MintG = Brush.linearGradient(listOf(Mint, Sky))

    /** Gradient art deterministik dari id (biar tiap lagu/playlist beda warna). */
    fun artBrush(seed: String): Brush {
        val palettes = listOf(
            listOf(Ember1, Ember2),
            listOf(Violet, Ember2),
            listOf(Mint, Sky),
            listOf(Ember1, Violet),
            listOf(Sky, Violet),
            listOf(Mint, Violet)
        )
        val idx = (seed.hashCode().ushr(1)) % palettes.size
        return Brush.linearGradient(palettes[idx])
    }
}
