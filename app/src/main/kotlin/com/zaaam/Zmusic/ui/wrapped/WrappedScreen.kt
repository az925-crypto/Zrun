package com.zaaam.Zmusic.ui.wrapped

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zaaam.Zmusic.model.entity.MoodStatResult
import com.zaaam.Zmusic.model.entity.TopArtistResult
import com.zaaam.Zmusic.model.entity.TopSongResult
import com.zaaam.Zmusic.ui.components.GlassCard
import kotlinx.coroutines.delay

private val WrappedBg = Brush.verticalGradient(
    colorStops = arrayOf(
        0.0f to Color(0xFF0D0A1E),
        0.3f to Color(0xFF150B2E),
        0.6f to Color(0xFF1A0F38),
        1.0f to Color(0xFF0D0A1E)
    )
)

private val moodEmojis = mapOf(
    "HAPPY" to "😊", "SAD" to "😢", "ENERGETIC" to "⚡",
    "CHILL" to "😌", "ROMANCE" to "❤️", "HYPE" to "🔥"
)
private val moodColors = mapOf(
    "HAPPY"     to Color(0xFFFFD54F),
    "SAD"       to Color(0xFF5C6BC0),
    "ENERGETIC" to Color(0xFFE53935),
    "CHILL"     to Color(0xFF26A69A),
    "ROMANCE"   to Color(0xFFEC407A),
    "HYPE"      to Color(0xFF8E24AA)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WrappedScreen(
    viewModel: WrappedViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val mode  by viewModel.mode.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WrappedBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("🎵  Listening Wrapped", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                actions = {
                    Row(modifier = Modifier.padding(end = 8.dp)) {
                        FilterChip(
                            selected = mode == "monthly",
                            onClick  = { viewModel.load("monthly") },
                            label    = { Text("Bulan Ini", style = MaterialTheme.typography.labelSmall) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                        Spacer(Modifier.width(6.dp))
                        FilterChip(
                            selected = mode == "yearly",
                            onClick  = { viewModel.load("yearly") },
                            label    = { Text("Tahunan", style = MaterialTheme.typography.labelSmall) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            )

            when (val s = state) {
                is WrappedState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is WrappedState.Empty -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎵", fontSize = 64.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Belum cukup data",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Dengarkan lebih banyak musik\nuntuk melihat rekap kamu!",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
                is WrappedState.Ready  -> WrappedContent(data = s.data)
            }
        }
    }
}

@Composable
private fun WrappedContent(data: WrappedData) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); visible = true }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // ── Header Besar ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = visible,
            enter   = fadeIn(tween(600)) + slideInVertically(tween(600)) { -40 }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF7C4DFF), Color(0xFFE040FB))
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val label = if (data.month != null) data.month else data.year.toString()
                    Text(
                        "Rekap ${label}",
                        style     = MaterialTheme.typography.labelLarge,
                        color     = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${data.totalPlays}",
                        style      = MaterialTheme.typography.displayMedium,
                        color      = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "lagu diputar",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        StatPill("${data.totalMinutes}", "menit")
                        if (data.listeningStreak > 0) {
                            StatPill("${data.listeningStreak}", "hari streak")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Dominant Mood ────────────────────────────────────────────────
        data.dominantMood?.let { mood ->
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(800))) {
                val emoji = moodEmojis[mood.uppercase()] ?: "🎵"
                val color = moodColors[mood.uppercase()] ?: MaterialTheme.colorScheme.primary

                GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(color.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 28.sp)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "Mood Dominan",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                mood.lowercase().replaceFirstChar { it.uppercase() },
                                style      = MaterialTheme.typography.headlineSmall,
                                color      = color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Top Songs ────────────────────────────────────────────────────
        if (data.topSongs.isNotEmpty()) {
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(900))) {
                Column {
                    SectionHeader("🏆  Top Lagu")
                    Spacer(Modifier.height(8.dp))
                    data.topSongs.forEachIndexed { idx, song ->
                        TopSongRow(rank = idx + 1, song = song)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Top Artists ──────────────────────────────────────────────────
        if (data.topArtists.isNotEmpty()) {
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(1000))) {
                Column {
                    SectionHeader("⭐  Top Artis")
                    Spacer(Modifier.height(8.dp))
                    data.topArtists.forEachIndexed { idx, artist ->
                        TopArtistRow(rank = idx + 1, artist = artist)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Mood Distribution ────────────────────────────────────────────
        if (data.moodDistribution.isNotEmpty()) {
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(1100))) {
                Column {
                    SectionHeader("🎨  Distribusi Mood")
                    Spacer(Modifier.height(8.dp))
                    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val total = data.moodDistribution.sumOf { it.playCount }.toFloat()
                            data.moodDistribution.take(6).forEach { moodStat ->
                                MoodBar(moodStat = moodStat, total = total)
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // ── Footer ───────────────────────────────────────────────────────
        Text(
            "♪  Dibuat dengan Zmusic",
            style     = MaterialTheme.typography.labelMedium,
            color     = MaterialTheme.colorScheme.primary,
            modifier  = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatPill(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style      = MaterialTheme.typography.titleLarge,
            color      = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style      = MaterialTheme.typography.titleMedium,
        color      = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun TopSongRow(rank: Int, song: TopSongResult) {
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "#$rank",
                style      = MaterialTheme.typography.titleMedium,
                color      = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.width(36.dp)
            )
            AsyncImage(
                model        = song.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier     = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    style    = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${song.playCount}x",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TopArtistRow(rank: Int, artist: TopArtistResult) {
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "#$rank",
                style      = MaterialTheme.typography.titleMedium,
                color      = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.width(36.dp)
            )
            Icon(
                Icons.Default.Star,
                null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                artist.artist,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.weight(1f)
            )
            Text(
                "${artist.playCount}x",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MoodBar(moodStat: MoodStatResult, total: Float) {
    val mood    = moodStat.mood.uppercase()
    val emoji   = moodEmojis[mood] ?: "🎵"
    val color   = moodColors[mood] ?: Color(0xFF7C4DFF)
    val percent = if (total > 0) moodStat.playCount / total else 0f

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 18.sp, modifier = Modifier.width(28.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                mood.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = percent)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "${(percent * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
