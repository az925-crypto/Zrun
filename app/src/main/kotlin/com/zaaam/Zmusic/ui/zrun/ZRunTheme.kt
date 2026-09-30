package com.zaaam.Zmusic.ui.zrun

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.zaaam.Zmusic.R
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.model.entity.ActivityEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Design tokens ZRun — SEMUA warna solid (tanpa gradien/glow/blur).
 * Warna, radius, dan skala tipografi mengikuti spesifikasi mockup.
 */
object ZR {
    // Permukaan
    val Bg = Color(0xFF0A0B0E)
    val S1 = Color(0xFF14161B)
    val S2 = Color(0xFF1D2027)

    // Teks
    val Tx = Color(0xFFFFFFFF)
    val Mut = Color(0xFF9298A4)
    val Faint = Color(0xFF5C616C)

    // Garis
    val Divider = Color(0x12FFFFFF) // putih alpha 7%
    val Stroke = Color(0x14FFFFFF)  // putih alpha 8%

    // Aksen (hanya 5 + turunan alpha untuk dot/pil)
    val Ember = Color(0xFFFF6A3D)  // lari / CTA / progres
    val Violet = Color(0xFF8B5CFF) // musik
    val Mint = Color(0xFF2DE0C0)   // GPS / avatar
    val Sky = Color(0xFF37B3FF)    // variasi cover
    val Stop = Color(0xFFFF2E63)   // stop / merekam

    /** Warna art solid deterministik dari seed ( Violet / Ember / Mint / Sky ). */
    fun artColor(seed: String): Color {
        val palette = listOf(Violet, Ember, Mint, Sky)
        val idx = (seed.hashCode() and Int.MAX_VALUE) % palette.size
        return palette[idx]
    }
}

/** Bricolage Grotesque (variable OFL, dibundel offline di res/font). */
@OptIn(ExperimentalTextApi::class)
private fun brico(weight: FontWeight, v: Int) = Font(
    R.font.bricolage_grotesque,
    weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(v))
)

val Bricolage = FontFamily(
    brico(FontWeight.Normal, 400),
    brico(FontWeight.Medium, 500),
    brico(FontWeight.SemiBold, 600),
    brico(FontWeight.Bold, 700)
)

/** Gaya teks standar: selalu Bricolage + angka tabular. */
fun zStyle(size: TextUnit, weight: FontWeight, ls: TextUnit = 0.sp): TextStyle = TextStyle(
    fontFamily = Bricolage,
    fontSize = size,
    fontWeight = weight,
    letterSpacing = ls,
    fontFeatureSettings = "tnum"
)

private val idLocale = Locale("id", "ID")

/** Kilometer 1 desimal gaya Indonesia: 5,2 — ribuan pakai titik. */
fun fmtKm1(meters: Double): String =
    (NumberFormat.getNumberInstance(idLocale).apply { maximumFractionDigits = 1; minimumFractionDigits = 1 })
        .format(meters / 1000.0)

/** Bilangan bulat gaya Indonesia: 1.000 */
fun fmtInt(n: Long): String = NumberFormat.getIntegerInstance(idLocale).format(n)

/** Pace detik/km jadi 5'21" (kutip ganda), "--" kalau kosong. */
fun fmtPaceQuote(secPerKm: Long): String {
    if (secPerKm <= 0) return "--"
    return "%d'%02d\"".format(secPerKm / 60, secPerKm % 60)
}

/** Durasi pendek: 7j 40m / 27m / 45d */
fun fmtDurShort(millis: Long): String {
    val totalMin = millis / 60_000
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h > 0 -> "${fmtInt(h)}j ${fmtInt(m)}m"
        m > 0 -> "${fmtInt(m)}m"
        else -> "${fmtInt(millis / 1000)}d"
    }
}

/** Nama hari gaya Indonesia sentence case: Selasa */
fun weekdayId(timeMs: Long): String =
    SimpleDateFormat("EEEE", idLocale).format(Date(timeMs))

/** Nama bulan berjalan: September */
fun monthNameId(timeMs: Long = System.currentTimeMillis()): String =
    SimpleDateFormat("MMMM", idLocale).format(Date(timeMs))

/** Sapaan waktu: pagi / siang / sore / malam */
fun greetingNow(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): String = when (hour) {
    in 0..10 -> "Selamat pagi"
    in 11..14 -> "Selamat siang"
    in 15..18 -> "Selamat sore"
    else -> "Selamat malam"
}

/** Awal minggu ini (Senin 00:00) dalam epoch millis. */
fun startOfWeekMs(now: Long = System.currentTimeMillis()): Long {
    val c = Calendar.getInstance().apply {
        timeInMillis = now
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return c.timeInMillis
}

/** Awal bulan ini (tanggal 1, 00:00). */
fun startOfMonthMs(now: Long = System.currentTimeMillis()): Long {
    val c = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return c.timeInMillis
}

/** Awal tahun ini (1 Jan 00:00). */
fun startOfYearMs(now: Long = System.currentTimeMillis()): Long {
    val c = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.MONTH, Calendar.JANUARY)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return c.timeInMillis
}

/** Data contoh untuk @Preview — jangan dipakai di UI produksi. */
object ZRunSamples {
    val song = Song(
        id = "preview1",
        title = "Tempo Fajar",
        artist = "Pelari Pagi",
        thumbnailUrl = "",
        duration = 214_000L
    )
    val songs = listOf(
        song,
        Song("preview2", "Lari Malam", "Kota Senja", "", 189_000L),
        Song("preview3", "Napas Panjang", "Arus Balik", "", 243_000L)
    )
    val activities = listOf(
        ActivityEntity(
            id = 1, title = "Lari pagi", type = "Run",
            startTime = System.currentTimeMillis() - 86_400_000L,
            durationMillis = 1_669_000L, movingMillis = 1_600_000L,
            distanceMeters = 5_200.0, avgSpeedKmh = 11.2,
            route = emptyList()
        ),
        ActivityEntity(
            id = 2, title = "Tempo Fajar", type = "Run",
            startTime = System.currentTimeMillis() - 2 * 86_400_000L,
            durationMillis = 2_340_000L, movingMillis = 2_300_000L,
            distanceMeters = 7_200.0, avgSpeedKmh = 11.3,
            route = emptyList()
        )
    )
}
