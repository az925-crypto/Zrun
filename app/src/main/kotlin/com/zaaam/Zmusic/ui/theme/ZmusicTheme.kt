package com.zaaam.Zmusic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Kompatibilitas: beberapa file lama masih refer ke PoppinsFamily
val PoppinsFamily: FontFamily get() = SoraFamily

// ═══════════════════════════════════════════════════════════════════════
//  ZMUSIC DESIGN TOKENS — "Dark Plum"
//  Gelap berlapis (bukan hitam flat) + aksen amber→magenta→violet
//  yang nantinya bisa di-override dinamis dari artwork (lihat Accent.kt)
// ═══════════════════════════════════════════════════════════════════════

// ── Lapisan gelap ────────────────────────────────────────────────────────
val Bg0 = Color(0xFF0C0A12)   // paling dalam — background utama
val Bg1 = Color(0xFF15111D)   // surface
val Bg2 = Color(0xFF1E1828)   // card
val Bg3 = Color(0xFF2A2238)   // elevated / slider track

// ── Teks ─────────────────────────────────────────────────────────────────
val TextPrimary = Color(0xFFF3EEFB)
val TextMuted   = Color(0xFF9A90AD)
val TextFaint   = Color(0xFF645A78)

// ── Aksen default (sebelum dinamis dari cover masuk) ─────────────────────
val AccentAmber   = Color(0xFFFF9D5C)
val AccentMagenta = Color(0xFFFF4D8D)
val AccentViolet  = Color(0xFFB66BFF)

val LineColor = Color.White.copy(alpha = 0.07f)

// ── Glass Design Tokens (nama lama dipertahankan, warna baru) ────────────
val GlassCardColor      = Bg2.copy(alpha = 0.60f)
val GlassBorderColor    = Color.White.copy(alpha = 0.10f)
val GlassOverlayColor   = Color.White.copy(alpha = 0.05f)
val GlassPlayerBarColor = Bg3.copy(alpha = 0.90f)
val GlassNavBarColor    = Bg0.copy(alpha = 0.92f)

// ── Gradient Tokens (nama lama dipertahankan, dicat ulang se-keluarga) ───
val GradientFavorit = Brush.linearGradient(listOf(Color(0xFFFF5F6D), Color(0xFFFFC371)))
val GradientBaru    = Brush.linearGradient(listOf(Color(0xFF7F5AF0), Color(0xFFFF4D8D)))
val GradientMusik   = Brush.linearGradient(listOf(Color(0xFF2CCCFF), Color(0xFFB66BFF)))
val GradientKoleksi = Brush.linearGradient(listOf(Color(0xFFC471F5), Color(0xFFFA71CD)))

/** Gradient aksen utama — dipakai tombol play, progress fill, dsb. */
val GradientAccent = Brush.linearGradient(listOf(AccentAmber, AccentMagenta))
val GradientAccentFull = Brush.linearGradient(listOf(AccentAmber, AccentMagenta, AccentViolet))

val HeroFadeGradient = Brush.verticalGradient(
    colorStops = arrayOf(
        0.0f to Color.Transparent,
        0.45f to Color.Transparent,
        0.80f to Bg0.copy(alpha = 0.80f),
        1.0f to Bg0
    )
)

val AccentGlow = AccentMagenta.copy(alpha = 0.18f)

/**
 * Gradient background halaman — plum gelap berlapis.
 * Dipakai layar non-mockup (Artist/Equalizer/Trending/Explore/Recent/SmartPlaylist/Home)
 * menggantikan gradient navy tema lama. Nilai senada SettingsScreen.
 */
val PageBgPlum = Brush.verticalGradient(
    colorStops = arrayOf(
        0.0f to Bg0, 0.3f to Color(0xFF130F1C),
        0.6f to Color(0xFF1A1226), 1.0f to Bg0
    )
)

val FloatingNavBg     = Bg0.copy(alpha = 0.92f)
val FloatingNavBorder = Color.White.copy(alpha = 0.10f)
val NavActiveGlow     = AccentMagenta.copy(alpha = 0.22f)

// ── Color Schemes ────────────────────────────────────────────────────────
private val ZmusicDarkColors = darkColorScheme(
    primary              = AccentMagenta,
    onPrimary            = Bg0,
    primaryContainer     = Color(0xFF4A1B33),
    onPrimaryContainer   = Color(0xFFFFB1CC),
    secondary            = AccentAmber,
    onSecondary          = Bg0,
    tertiary             = AccentViolet,
    onTertiary           = Bg0,
    background           = Bg0,
    onBackground         = TextPrimary,
    surface              = Bg1,
    onSurface            = TextPrimary,
    surfaceVariant       = Bg2,
    onSurfaceVariant     = TextMuted,
    surfaceContainer     = Bg3,
    outline              = Color(0xFF3A3048)
)

private val ZmusicLightColors = lightColorScheme(
    primary          = Color(0xFFD81B60),
    onPrimary        = Color.White,
    background       = Color(0xFFFBF7FF),
    onBackground     = Color(0xFF1D1822),
    surface          = Color.White,
    onSurface        = Color(0xFF1D1822),
    surfaceVariant   = Color(0xFFF0EAF6),
    onSurfaceVariant = Color(0xFF564E63)
)

// ── Typography — Sora (display/body) + Grotesk (utility) ────────────────
private val ZmusicTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily    = SoraFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 23.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 18.sp
    ),
    titleLarge = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 16.sp
    ),
    titleSmall = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.Medium,
        fontSize   = 14.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 13.sp
    ),
    bodySmall = TextStyle(
        fontFamily = SoraFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 11.sp
    ),
    // label* = utility → Grotesk (durasi, eyebrow, chip, nav)
    labelLarge = TextStyle(
        fontFamily = GroteskFamily,
        fontWeight = FontWeight.Medium,
        fontSize   = 13.sp
    ),
    labelMedium = TextStyle(
        fontFamily    = GroteskFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 11.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily    = GroteskFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 10.sp,
        letterSpacing = 1.5.sp   // buat eyebrow uppercase
    ),
)

@Composable
fun ZmusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ZmusicDarkColors else ZmusicLightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = ZmusicTypography,
        content     = content
    )
}
