package com.zaaam.Zmusic.ui.theme

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ── AKSEN DINAMIS DARI ARTWORK ───────────────────────────────────────────
 *
 * Seluruh UI "nyerap" warna dari cover lagu yang sedang diputar:
 * tombol play, progress bar, equalizer, glow — semua ikut berubah
 * dengan transisi halus tiap ganti lagu.
 *
 * Pemakaian di screen:
 * ```
 * val accent = rememberAccentColors(currentSong?.thumbnailUrl)
 * Box(Modifier.background(accent.gradient())) { ... }
 * Text(..., color = accent.primary)
 * ```
 *
 * Polanya diangkat dari ekstraksi Palette yang sudah ada di PlayerScreen:
 * - allowHardware(false)  → Palette butuh software bitmap
 * - generate() jalan di Dispatchers.Default (CPU-intensive)
 * - fallback swatch: vibrant → lightVibrant → dominant
 */

/** Tiga warna aksen turunan dari artwork (atau default Dark Plum). */
data class AccentColors(
    val primary: Color,    // warna paling "hidup" dari cover
    val secondary: Color,  // pendamping (lebih gelap/muted)
    val glow: Color        // versi transparan buat shadow/glow
) {
    /** Gradient utama: secondary → primary (dipakai tombol, fill, dsb.) */
    fun gradient(): Brush = Brush.linearGradient(listOf(secondary, primary))
}

/** Aksen default saat belum ada cover / ekstraksi gagal. */
val DefaultAccent = AccentColors(
    primary   = AccentMagenta,
    secondary = AccentAmber,
    glow      = AccentMagenta.copy(alpha = 0.35f)
)

/**
 * Ekstrak + animasikan warna aksen dari URL artwork.
 * Aman dipanggil dengan url null/kosong → balik ke [DefaultAccent].
 */
@Composable
fun rememberAccentColors(artworkUrl: String?): AccentColors {
    val context = LocalContext.current

    // Ekstraksi: jalan ulang hanya saat URL berubah
    val raw by produceState(initialValue = DefaultAccent, key1 = artworkUrl) {
        if (artworkUrl.isNullOrEmpty()) {
            value = DefaultAccent
            return@produceState
        }
        val extracted = withContext(Dispatchers.Default) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .allowHardware(false)
                    .size(128) // bitmap kecil cukup buat Palette, hemat CPU
                    .build()
                val result = ImageLoader(context).execute(request)
                val bmp = ((result as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
                    ?: return@runCatching null
                val palette = Palette.from(bmp).generate()
                val vibrant = palette.vibrantSwatch
                    ?: palette.lightVibrantSwatch
                    ?: palette.dominantSwatch
                val dark = palette.darkVibrantSwatch
                    ?: palette.mutedSwatch
                    ?: vibrant
                if (vibrant == null) null
                else AccentColors(
                    primary   = Color(vibrant.rgb),
                    secondary = Color((dark ?: vibrant).rgb),
                    glow      = Color(vibrant.rgb).copy(alpha = 0.35f)
                )
            }.getOrNull()
        }
        value = extracted ?: DefaultAccent
    }

    // Transisi halus antar lagu (600ms, senada sama crossfade artwork)
    val p by animateColorAsState(raw.primary, tween(600), label = "accentPrimary")
    val s by animateColorAsState(raw.secondary, tween(600), label = "accentSecondary")
    val g by animateColorAsState(raw.glow, tween(600), label = "accentGlow")
    return AccentColors(p, s, g)
}
