package com.zaaam.Zmusic.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.zaaam.Zmusic.R

/**
 * ── FONT SETUP ───────────────────────────────────────────────────────────
 *
 * Butuh 7 file font di `app/src/main/res/font/` (huruf kecil semua):
 *
 *   sora_regular.ttf          (Sora 400)
 *   sora_medium.ttf           (Sora 500)
 *   sora_semibold.ttf         (Sora 600)
 *   sora_bold.ttf             (Sora 700)
 *   space_grotesk_regular.ttf (Space Grotesk 400)
 *   space_grotesk_medium.ttf  (Space Grotesk 500)
 *   space_grotesk_semibold.ttf(Space Grotesk 600)
 *
 * Cara dapat:
 *   1. Buka fonts.google.com → cari "Sora" → Download family (zip)
 *   2. Dari zip, ambil file static: Sora-Regular.ttf, Sora-Medium.ttf,
 *      Sora-SemiBold.ttf, Sora-Bold.ttf
 *   3. Rename jadi huruf kecil + underscore seperti daftar di atas
 *   4. Ulangi untuk "Space Grotesk"
 *   5. Taruh semua di app/src/main/res/font/ (buat foldernya kalau belum ada)
 *
 * DARURAT: kalau mau build duluan tanpa font, ganti dua val di bawah jadi:
 *   val SoraFamily: FontFamily = FontFamily.Default
 *   val GroteskFamily: FontFamily = FontFamily.Default
 */

/** Display & body — kepribadian utama UI */
val SoraFamily = FontFamily(
    Font(R.font.sora_regular, FontWeight.Normal),
    Font(R.font.sora_medium, FontWeight.Medium),
    Font(R.font.sora_semibold, FontWeight.SemiBold),
    Font(R.font.sora_bold, FontWeight.Bold),
)

/** Utility — angka, durasi, label kecil, eyebrow */
val GroteskFamily = FontFamily(
    Font(R.font.space_grotesk_regular, FontWeight.Normal),
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
)
