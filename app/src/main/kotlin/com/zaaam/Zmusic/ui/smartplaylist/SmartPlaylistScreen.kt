package com.zaaam.Zmusic.ui.smartplaylist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.ui.components.AlbumArt

// BATCH 5: palet biru-navy tema lama diganti token Dark Plum (ZmusicTheme)
private val SpBgGradient = com.zaaam.Zmusic.ui.theme.PageBgPlum
private val BluePrimary   = com.zaaam.Zmusic.ui.theme.AccentMagenta
private val TextPrimary   = com.zaaam.Zmusic.ui.theme.TextPrimary
private val TextSecondary = com.zaaam.Zmusic.ui.theme.TextMuted
private val CardSurface   = com.zaaam.Zmusic.ui.theme.Bg2
private val CardBorder    = Color.White.copy(alpha = 0.09f)

@Composable
fun SmartPlaylistScreen(
    viewModel: SmartPlaylistViewModel,
    onSongClick: (Song, List<Song>) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onNavigateBack: () -> Unit
) {
    val playlists by viewModel.playlists.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Which playlist is expanded (null = none)
    var expandedId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBgGradient)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top Bar ───────────────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, "Kembali", tint = Color.White)
                }
                Icon(
                    Icons.Default.AutoAwesome, null,
                    tint     = BluePrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Smart Playlist",
                        style      = MaterialTheme.typography.titleLarge,
                        color      = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Dibuat otomatis dari histori dengaran kamu",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                // Refresh
                IconButton(onClick = { viewModel.loadAll() }) {
                    Icon(Icons.Default.Refresh, "Refresh", tint = TextSecondary)
                }
            }

            // ── Content ───────────────────────────────────────────────────
            when {
                isLoading -> {
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = BluePrimary)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Menganalisis histori dengaran...",
                                color    = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                playlists.isEmpty() -> {
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text("🎵", fontSize = 48.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Belum Ada Data",
                                color      = TextPrimary,
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Putar lebih banyak lagu agar Zmusic bisa membuat playlist otomatis untukmu.",
                                color     = TextSecondary,
                                fontSize  = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding        = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 120.dp),
                        verticalArrangement   = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        playlists.forEach { playlist ->
                            item(key = playlist.id) {
                                val isExpanded = expandedId == playlist.id

                                SmartPlaylistCard(
                                    playlist    = playlist,
                                    isExpanded  = isExpanded,
                                    onToggleExpand = {
                                        expandedId = if (isExpanded) null else playlist.id
                                    },
                                    onPlayAll   = {
                                        if (playlist.songs.isNotEmpty()) {
                                            onSongClick(playlist.songs.first(), playlist.songs)
                                        }
                                    },
                                    onShuffle   = {
                                        val shuffled = playlist.songs.shuffled()
                                        if (shuffled.isNotEmpty()) {
                                            onSongClick(shuffled.first(), shuffled)
                                        }
                                    },
                                    onSongClick = { song -> onSongClick(song, playlist.songs) },
                                    onSongLongClick = onSongLongClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartPlaylistCard(
    playlist: SmartPlaylist,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardSurface)
            .border(1.dp, if (isExpanded) BluePrimary.copy(alpha = 0.3f) else CardBorder, RoundedCornerShape(18.dp))
    ) {
        // ── Header row ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null
                ) { onToggleExpand() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BluePrimary.copy(alpha = 0.12f))
                    .border(1.dp, BluePrimary.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(playlist.emoji, fontSize = 24.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = playlist.name,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = playlist.description,
                    fontSize = 12.sp,
                    color    = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = "${playlist.songs.size} lagu",
                    fontSize = 11.sp,
                    color    = BluePrimary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint     = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        // ── Action buttons (always visible) ────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick  = onPlayAll,
                modifier = Modifier.weight(1f),
                shape    = RoundedCornerShape(10.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Putar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick  = onShuffle,
                modifier = Modifier.weight(1f),
                shape    = RoundedCornerShape(10.dp),
                border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = BluePrimary),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Acak", fontSize = 13.sp)
            }
        }

        // ── Expanded song list ──────────────────────────────────────────
        if (isExpanded) {
            HorizontalDivider(
                color = CardBorder,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            playlist.songs.take(20).forEachIndexed { idx, song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null
                        ) { onSongClick(song) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text     = "${idx + 1}",
                        fontSize = 11.sp,
                        color    = TextSecondary,
                        modifier = Modifier.width(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    AlbumArt(thumbnailUrl = song.thumbnailUrl, size = 40.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = song.title,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color      = TextPrimary,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Text(
                            text     = song.artist,
                            fontSize = 11.sp,
                            color    = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (playlist.songs.size > 20) {
                Text(
                    text     = "+${playlist.songs.size - 20} lagu lainnya",
                    fontSize = 11.sp,
                    color    = TextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
