package com.zaaam.Zmusic.ui.zrun

import com.zaaam.Zmusic.model.entity.ActivityEntity

/**
 * Definisi lencana tunggal — dipakai layar Home (lencana terdekat) dan You.
 * Semua angka/progres dihitung dari data aktivitas asli.
 */
data class ZBadge(
    val name: String,
    val syarat: String,
    val angka: String,
    val progress: String,
    val open: Boolean
)

fun zBadges(acts: List<ActivityEntity>): List<ZBadge> {
    val count = acts.size
    val totalKm = acts.sumOf { it.distanceMeters } / 1000.0
    val bestSingleKm = acts.maxOfOrNull { it.distanceMeters }?.div(1000.0) ?: 0.0
    val bestPace = acts.filter { it.distanceMeters > 300 }.minOfOrNull { it.avgPaceSecPerKm } ?: 0L
    return listOf(
        ZBadge(
            name = "Lari pertama",
            syarat = "Selesaikan 1 lari untuk membukanya",
            angka = "1",
            progress = "${fmtInt(count.coerceAtMost(1).toLong())}/1",
            open = count >= 1
        ),
        ZBadge(
            name = "5 aktivitas",
            syarat = "Selesaikan 5 lari untuk membukanya",
            angka = "5",
            progress = "${fmtInt(count.coerceAtMost(5).toLong())}/5",
            open = count >= 5
        ),
        ZBadge(
            name = "Sub-6 pace",
            syarat = "Lari 1 km di bawah 6 menit untuk membukanya",
            angka = "6'",
            progress = if (bestPace in 1..359) fmtPaceQuote(bestPace) else "belum ada",
            open = bestPace in 1..359
        ),
        ZBadge(
            name = "5 km pertama",
            syarat = "Selesaikan 1 lari 5 km untuk membukanya",
            angka = "5",
            progress = "${fmtKm1(bestSingleKm.coerceAtMost(5.0) * 1000)}/5 km",
            open = bestSingleKm >= 5.0
        ),
        ZBadge(
            name = "10 km",
            syarat = "Selesaikan 1 lari 10 km untuk membukanya",
            angka = "10",
            progress = "${fmtKm1(bestSingleKm.coerceAtMost(10.0) * 1000)}/10 km",
            open = bestSingleKm >= 10.0
        ),
        ZBadge(
            name = "100 km",
            syarat = "Kumpulkan 100 km total untuk membukanya",
            angka = "100",
            progress = "${fmtKm1(totalKm.coerceAtMost(100.0) * 1000)}/100 km",
            open = totalKm >= 100.0
        )
    )
}
