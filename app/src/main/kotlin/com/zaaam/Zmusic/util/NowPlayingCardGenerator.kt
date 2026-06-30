package com.zaaam.Zmusic.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * NowPlayingCardGenerator — Membuat bitmap "Now Playing" card untuk dishare.
 *
 * Menghasilkan gambar 1080x1080px bergaya dark premium dengan:
 * - Album art blur sebagai background
 * - Gradient overlay
 * - Judul + artis
 * - Logo/nama app
 */
object NowPlayingCardGenerator {

    private const val TAG = "ZmusicShareCard"
    private const val CARD_SIZE = 1080
    private const val CORNER_RADIUS = 48f
    private const val MAX_CACHE_AGE_MS = 24 * 60 * 60 * 1000L  // 24 jam

    /**
     * Generate bitmap card dan simpan ke cache, kemudian share via intent.
     * @param context     Android context
     * @param title       Judul lagu
     * @param artist      Nama artis
     * @param thumbnailUrl URL thumbnail album art
     */
    suspend fun shareNowPlayingCard(
        context: Context,
        title: String,
        artist: String,
        thumbnailUrl: String
    ) {
        // Guard OOM: bitmap 1080x1080 ARGB_8888 = ~4.5MB per panggilan.
        // Di HP low-RAM, alokasi bisa gagal — jangan crash, cukup batal share + log.
        try {
            val bitmap = generateCard(context, title, artist, thumbnailUrl)
            val file = saveBitmapToCache(context, bitmap)
            val uri  = getUriForFile(context, file)
            launchShareIntent(context, uri)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "shareNowPlayingCard OOM — share dibatalkan", e)
        } catch (e: Exception) {
            Log.e(TAG, "shareNowPlayingCard gagal", e)
        }
    }

    /**
     * Generate bitmap card.
     */
    suspend fun generateCard(
        context: Context,
        title: String,
        artist: String,
        thumbnailUrl: String
    ): Bitmap = withContext(Dispatchers.IO) {
        val size   = CARD_SIZE
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // ── 1. Background gelap ──────────────────────────────────────────
        val bgPaint = Paint().apply {
            color = 0xFF0D1020.toInt()
            isAntiAlias = true
        }
        val roundedRect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        canvas.drawRoundRect(roundedRect, CORNER_RADIUS, CORNER_RADIUS, bgPaint)

        // ── 2. Load album art ────────────────────────────────────────────
        val albumBitmap = loadBitmapFromUrl(context, thumbnailUrl)

        if (albumBitmap != null) {
            // Album art besar di tengah-atas
            val artSize   = (size * 0.65f).toInt()
            val artLeft   = (size - artSize) / 2f
            val artTop    = size * 0.08f
            val artRect   = RectF(artLeft, artTop, artLeft + artSize, artTop + artSize)

            val scaledArt = Bitmap.createScaledBitmap(albumBitmap, artSize, artSize, true)
            val artPaint  = Paint().apply { isAntiAlias = true }

            // Gambar art dengan rounded corners
            val artCanvas = Canvas(bitmap)
            val clipPath  = android.graphics.Path().apply {
                addRoundRect(artRect, 32f, 32f, android.graphics.Path.Direction.CW)
            }
            artCanvas.clipPath(clipPath)
            artCanvas.drawBitmap(scaledArt, artLeft, artTop, artPaint)

            // Reset clip
            val newCanvas = Canvas(bitmap)

            // ── 3. Gradient overlay bawah ────────────────────────────────
            val gradStartY = size * 0.55f
            val gradPaint  = Paint().apply {
                shader = LinearGradient(
                    0f, gradStartY, 0f, size.toFloat(),
                    intArrayOf(0x00000000, 0xFF0D1020.toInt(), 0xFF0D1020.toInt()),
                    floatArrayOf(0f, 0.6f, 1f),
                    Shader.TileMode.CLAMP
                )
                isAntiAlias = true
            }
            newCanvas.drawRect(0f, gradStartY, size.toFloat(), size.toFloat(), gradPaint)
        } else {
            // Fallback: gradient background
            val gradPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, size.toFloat(),
                    intArrayOf(0xFF1A1040.toInt(), 0xFF0D1020.toInt()),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), gradPaint)
        }

        val drawCanvas = Canvas(bitmap)

        // ── 4. Teks judul ────────────────────────────────────────────────
        val titlePaint = Paint().apply {
            color       = 0xFFFFFFFF.toInt()
            textSize    = 72f
            typeface    = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign   = Paint.Align.CENTER
        }
        // Wrap title jika terlalu panjang
        val maxTitleWidth = size * 0.85f
        val displayTitle  = if (titlePaint.measureText(title) > maxTitleWidth) {
            title.take(28) + "…"
        } else title
        drawCanvas.drawText(displayTitle, size / 2f, size * 0.80f, titlePaint)

        // ── 5. Teks artis ────────────────────────────────────────────────
        val artistPaint = Paint().apply {
            color       = 0xFFBBBBBB.toInt()
            textSize    = 48f
            typeface    = Typeface.DEFAULT
            isAntiAlias = true
            textAlign   = Paint.Align.CENTER
        }
        val displayArtist = if (artistPaint.measureText(artist) > maxTitleWidth) {
            artist.take(36) + "…"
        } else artist
        drawCanvas.drawText(displayArtist, size / 2f, size * 0.87f, artistPaint)

        // ── 6. Branding ──────────────────────────────────────────────────
        val brandPaint = Paint().apply {
            color       = 0xFF7C4DFF.toInt()
            textSize    = 36f
            typeface    = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textAlign   = Paint.Align.CENTER
        }
        drawCanvas.drawText("♪  Zmusic", size / 2f, size * 0.95f, brandPaint)

        // ── 7. Border tipis ──────────────────────────────────────────────
        val borderPaint = Paint().apply {
            color   = 0x337C4DFF.toInt()
            style   = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        drawCanvas.drawRoundRect(
            RectF(2f, 2f, (size - 2).toFloat(), (size - 2).toFloat()),
            CORNER_RADIUS, CORNER_RADIUS, borderPaint
        )

        bitmap
    }

    private suspend fun loadBitmapFromUrl(context: Context, url: String): Bitmap? {
        return try {
            // FIX POTENSI #5: Pakai Coil singleton (context.imageLoader) bukan ImageLoader(context).
            // ImageLoader(context) membuat instance baru setiap kali generateCard() dipanggil
            // → OkHttpClient baru, cache baru, tidak bisa reuse koneksi/cache dari komponen lain.
            // context.imageLoader mengembalikan singleton yang di-setup di ZmusicApp.
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .build()
            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "load album art gagal untuk card — pakai fallback gradient", e)
            null
        }
    }

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap): File {
        val cacheDir = File(context.cacheDir, "shares").apply { mkdirs() }

        // Bersihkan PNG share lama (>24 jam) — sebelumnya file menumpuk selamanya
        // sampai OS sendiri yang membersihkan cache.
        val now = System.currentTimeMillis()
        cacheDir.listFiles { f -> f.name.startsWith("nowplaying_") }
            ?.filter { now - it.lastModified() > MAX_CACHE_AGE_MS }
            ?.forEach { old ->
                if (!old.delete()) Log.w(TAG, "gagal hapus cache lama: ${old.name}")
            }

        val file = File(cacheDir, "nowplaying_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 95, it) }
        return file
    }

    private fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun launchShareIntent(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type  = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan ke").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
