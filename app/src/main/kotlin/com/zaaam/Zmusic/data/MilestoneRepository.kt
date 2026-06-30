package com.zaaam.Zmusic.data

import android.util.Log
import com.zaaam.Zmusic.data.local.PlayHistoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

// ── Milestone data model ──────────────────────────────────────────────────

data class Milestone(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean
)

// ── Repository ────────────────────────────────────────────────────────────

@Singleton
class MilestoneRepository @Inject constructor(
    private val playHistoryDao: PlayHistoryDao
) {

    // ══════════════════════════════════════════════════════════════════════════
    // FIX: SimpleDateFormat diganti dengan java.time.LocalDate.
    //
    // MASALAH LAMA:
    //   computeStreak() pakai SimpleDateFormat yang TIDAK thread-safe.
    //   Dipanggil dari flowOn(Dispatchers.Default) → multiple threads bisa
    //   akses instance sdf yang sama secara bersamaan → hasil parse KORUP
    //   (tanggal salah, NumberFormatException, ArrayIndexOutOfBoundsException).
    //
    // SOLUSI:
    //   java.time.LocalDate thread-safe by design (immutable).
    //   Desugaring sudah enabled di project (coreLibraryDesugaring 2.0.4)
    //   jadi aman dipakai di minSdk 26+.
    // ══════════════════════════════════════════════════════════════════════════
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // ── Listening Streak ─────────────────────────────────────────────────

    val listeningStreak: Flow<Int> = playHistoryDao.getDistinctListeningDays()
        .map { days -> computeStreak(days) }
        .flowOn(Dispatchers.Default)

    /**
     * Dari list tanggal "YYYY-MM-DD" terurut DESC, hitung berapa hari berturut-turut.
     * Streak valid jika hari terakhir adalah hari ini atau kemarin
     * (supaya streak tidak langsung putus jam 00:00).
     */
    private fun computeStreak(days: List<String>): Int {
        if (days.isEmpty()) return 0

        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val firstDay = try {
            LocalDate.parse(days.first(), dateFormatter)
        } catch (e: Exception) {
            Log.w("ZmusicMilestone", "parse tanggal streak gagal: '${days.first()}'", e)
            return 0
        }

        // Streak harus mulai dari hari ini atau kemarin
        if (firstDay != today && firstDay != yesterday) return 0

        var streak = 1
        for (i in 1 until days.size) {
            // Parse gagal = data korup; hentikan hitung streak di titik itu + log sekali
            val prev = try { LocalDate.parse(days[i - 1], dateFormatter) } catch (e: Exception) {
                Log.w("ZmusicMilestone", "parse tanggal '${days[i - 1]}' gagal — streak dipotong", e); break
            }
            val curr = try { LocalDate.parse(days[i], dateFormatter) } catch (e: Exception) {
                Log.w("ZmusicMilestone", "parse tanggal '${days[i]}' gagal — streak dipotong", e); break
            }
            val diffDays = ChronoUnit.DAYS.between(curr, prev)
            if (diffDays == 1L) streak++ else break
        }
        return streak
    }

    // ── Milestones ────────────────────────────────────────────────────────

    val milestones: Flow<List<Milestone>> = combine(
        playHistoryDao.getTotalPlays(),
        playHistoryDao.getDistinctListeningDays().map { computeStreak(it) }
    ) { totalPlays, streak ->
        buildMilestones(totalPlays, streak)
    }.flowOn(Dispatchers.Default)

    private fun buildMilestones(totalPlays: Int, streak: Int): List<Milestone> = listOf(

        // ── Play count milestones ──────────────────────────────────────────
        Milestone(
            id          = "first_play",
            emoji       = "🎵",
            title       = "Mulai Berdendang",
            description = "Putar lagu pertamamu",
            isUnlocked  = totalPlays >= 1
        ),
        Milestone(
            id          = "plays_10",
            emoji       = "🎶",
            title       = "Penikmat Musik",
            description = "Putar 10 lagu",
            isUnlocked  = totalPlays >= 10
        ),
        Milestone(
            id          = "plays_50",
            emoji       = "🎸",
            title       = "Pendengar Setia",
            description = "Putar 50 lagu",
            isUnlocked  = totalPlays >= 50
        ),
        Milestone(
            id          = "plays_100",
            emoji       = "🏆",
            title       = "Musisi Sejati",
            description = "Putar 100 lagu",
            isUnlocked  = totalPlays >= 100
        ),
        Milestone(
            id          = "plays_500",
            emoji       = "💿",
            title       = "Legenda Musik",
            description = "Putar 500 lagu",
            isUnlocked  = totalPlays >= 500
        ),

        // ── Streak milestones ─────────────────────────────────────────────
        Milestone(
            id          = "streak_3",
            emoji       = "🔥",
            title       = "On Fire",
            description = "3 hari berturut-turut mendengarkan",
            isUnlocked  = streak >= 3
        ),
        Milestone(
            id          = "streak_7",
            emoji       = "⚡",
            title       = "Seminggu Nonstop",
            description = "7 hari streak mendengarkan",
            isUnlocked  = streak >= 7
        ),
        Milestone(
            id          = "streak_30",
            emoji       = "🌟",
            title       = "Bulan Penuh Musik",
            description = "30 hari streak mendengarkan",
            isUnlocked  = streak >= 30
        )
    )
}
