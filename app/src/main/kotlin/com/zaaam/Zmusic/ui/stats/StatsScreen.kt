package com.zaaam.Zmusic.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.Zmusic.data.Milestone
import com.zaaam.Zmusic.model.Mood
import com.zaaam.Zmusic.model.entity.MoodStatResult
import com.zaaam.Zmusic.ui.components.AlbumArt
import com.zaaam.Zmusic.ui.components.GlassCard
import com.zaaam.Zmusic.ui.components.StateDisplay
import com.zaaam.Zmusic.ui.theme.MoodGradientColors
import com.zaaam.Zmusic.util.toHoursMinutesString

private fun getMoodEnum(moodName: String): Mood? =
    Mood.values().firstOrNull { it.name.equals(moodName, ignoreCase = true) }

// Warna mood terpusat di ui/theme/MoodColors.kt (dipakai juga oleh MoodScreen)
private val moodStatsColors = MoodGradientColors

private val moodBarColors = mapOf(
    Mood.HAPPY     to Color(0xFFFFD54F),
    Mood.SAD       to Color(0xFF5C6BC0),
    Mood.ENERGETIC to Color(0xFFE53935),
    Mood.CHILL     to Color(0xFF26A69A),
    Mood.ROMANCE   to Color(0xFFEC407A),
    Mood.HYPE      to Color(0xFF8E24AA)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val totalPlays       by viewModel.totalPlays.collectAsState()
    val totalDuration    by viewModel.totalDuration.collectAsState()
    val topSongs         by viewModel.topSongs.collectAsState()
    val topArtists       by viewModel.topArtists.collectAsState()
    val moodDistribution by viewModel.moodDistribution.collectAsState()
    val recentMood       by viewModel.recentMood.collectAsState()
    // ── NEW ──────────────────────────────────────────────────────────────
    val listeningStreak  by viewModel.listeningStreak.collectAsState()
    val milestones       by viewModel.milestones.collectAsState()

    // ── Empty state: belum ada satu pun play yang terecord ─────────────────
    if (totalPlays == 0) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(title = { Text("Statistik") })
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                StateDisplay(
                    icon    = Icons.Default.MusicNote,
                    title   = "Belum ada statistik",
                    message = "Putar beberapa lagu dulu — statistik mendengarkanmu bakal muncul di sini."
                )
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Statistik") })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Summary Cards ──────────────────────────────────────────────
            Row {
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text  = "$totalPlays",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text  = "Lagu diputar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                GlassCard(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text  = totalDuration.toHoursMinutesString(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text  = "Didengarkan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── NEW: Listening Streak Card ─────────────────────────────────
            Spacer(Modifier.height(16.dp))
            StreakCard(streak = listeningStreak)

            // ── Mood Detector ──────────────────────────────────────────────
            Spacer(Modifier.height(24.dp))
            MoodDetectorSection(
                moodDistribution = moodDistribution,
                recentMood       = recentMood
            )

            // ── Top Songs ──────────────────────────────────────────────────
            if (topSongs.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Lagu Favoritmu",
                    style    = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                topSongs.forEach { song ->
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlbumArt(thumbnailUrl = song.thumbnailUrl, size = 48.dp)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(song.title, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "${song.playCount}x",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Top Artists ────────────────────────────────────────────────
            if (topArtists.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Artis Favoritmu",
                    style    = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                topArtists.forEach { artist ->
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.MusicNote, null,
                            modifier = Modifier
                                .size(48.dp)
                                .padding(8.dp),
                            tint     = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            artist.artist,
                            modifier = Modifier.weight(1f),
                            style    = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "${artist.playCount}x",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── NEW: Milestones ────────────────────────────────────────────
            if (milestones.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                MilestonesSection(milestones = milestones)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── NEW: Streak Card ──────────────────────────────────────────────────────

@Composable
private fun StreakCard(streak: Int) {
    val streakColors = when {
        streak >= 30 -> listOf(Color(0xFFFFD700), Color(0xFFFF8C00)) // gold
        streak >= 7  -> listOf(Color(0xFFFF6B35), Color(0xFFFF3E00)) // orange
        streak >= 3  -> listOf(Color(0xFFFF5252), Color(0xFFC62828)) // red
        else         -> listOf(Color(0xFFB66BFF), Color(0xFF8E4DE8)) // violet (on-palette Dark Plum)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flame icon dengan gradient container
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(streakColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = "Streak Mendengarkan",
                    style      = MaterialTheme.typography.labelMedium,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text       = "$streak",
                        style      = MaterialTheme.typography.headlineLarge,
                        color      = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 40.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text     = "hari berturut-turut",
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                if (streak == 0) {
                    Text(
                        text  = "Dengarkan musik hari ini untuk mulai streak!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val nextMilestone = when {
                        streak < 3  -> "3 hari"
                        streak < 7  -> "7 hari"
                        streak < 30 -> "30 hari"
                        else        -> null
                    }
                    nextMilestone?.let {
                        Text(
                            text  = "🎯 Target berikutnya: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } ?: Text(
                        text  = "🏆 Luar biasa! Kamu sudah 30+ hari!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFD700)
                    )
                }
            }
        }
    }
}

// ── NEW: Milestones Section ───────────────────────────────────────────────

@Composable
private fun MilestonesSection(milestones: List<Milestone>) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.EmojiEvents, null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text       = "Pencapaian",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        // Grid 2 kolom
        val rows = milestones.chunked(2)
        rows.forEach { rowItems ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { milestone ->
                    MilestoneCard(
                        milestone = milestone,
                        modifier  = Modifier.weight(1f)
                    )
                }
                // Isi kolom kosong jika ganjil
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MilestoneCard(milestone: Milestone, modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier,
        shape    = RoundedCornerShape(16.dp),
        borderColor = if (milestone.isUnlocked)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        else
            Color.White.copy(alpha = 0.08f)
    ) {
        Column(
            modifier            = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Emoji atau lock icon
            if (milestone.isUnlocked) {
                Text(
                    text     = milestone.emoji,
                    fontSize = 32.sp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock, null,
                        tint     = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text       = milestone.title,
                style      = MaterialTheme.typography.labelLarge,
                color      = if (milestone.isUnlocked) MaterialTheme.colorScheme.onSurface
                             else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontWeight = FontWeight.SemiBold,
                textAlign  = TextAlign.Center,
                maxLines   = 2
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text      = milestone.description,
                style     = MaterialTheme.typography.bodySmall,
                color     = if (milestone.isUnlocked) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                textAlign = TextAlign.Center,
                maxLines  = 2
            )

            // Glow badge untuk yang sudah unlock
            if (milestone.isUnlocked) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text  = "✓ Diraih",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ── Existing: Mood Detector Section (tidak berubah) ───────────────────────

@Composable
private fun MoodDetectorSection(
    moodDistribution: List<MoodStatResult>,
    recentMood: MoodStatResult?
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Mood, null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = "Mood Detector",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(16.dp))

            if (moodDistribution.isEmpty()) {
                Text(
                    text      = "Belum ada data mood.\nDengarkan beberapa lagu untuk mulai tracking!",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            } else {
                if (recentMood != null) {
                    val mood = getMoodEnum(recentMood.mood)
                    if (mood != null) {
                        RecentMoodCard(mood = mood)
                        Spacer(Modifier.height(16.dp))
                    }
                }

                val topMood = moodDistribution.firstOrNull()
                if (topMood != null) {
                    val mood = getMoodEnum(topMood.mood)
                    if (mood != null) {
                        Text(
                            text       = "Mood Paling Sering",
                            style      = MaterialTheme.typography.labelMedium,
                            color      = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = mood.emoji, fontSize = 28.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text       = mood.label,
                                    style      = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text  = "${topMood.playCount} lagu diputar",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text       = "Distribusi Mood",
                    style      = MaterialTheme.typography.labelMedium,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))

                val maxCount = moodDistribution.maxOf { it.playCount }

                moodDistribution.forEach { stat ->
                    val mood     = getMoodEnum(stat.mood)
                    val fraction = stat.playCount.toFloat() / maxCount
                    val barColor = mood?.let { moodBarColors[it] }
                        ?: MaterialTheme.colorScheme.primary

                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text     = mood?.emoji ?: "?",
                            fontSize = 18.sp,
                            modifier = Modifier.width(28.dp)
                        )
                        Text(
                            text     = mood?.label ?: stat.mood,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(70.dp)
                        )
                        LinearProgressIndicator(
                            progress   = { fraction },
                            modifier   = Modifier
                                .weight(1f)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color      = barColor,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            strokeCap  = StrokeCap.Round
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text      = "${stat.playCount}",
                            style     = MaterialTheme.typography.bodySmall,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier  = Modifier.width(28.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentMoodCard(mood: Mood) {
    val colors = moodStatsColors[mood] ?: listOf(Color.Gray, Color.DarkGray)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(colors))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text       = "Mood Terkini",
                style      = MaterialTheme.typography.labelMedium,
                color      = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = mood.emoji, fontSize = 36.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text       = mood.label,
                        style      = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                    Text(
                        text  = "Berdasarkan lagu terakhir kamu",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }
            }
        }
    }
}
