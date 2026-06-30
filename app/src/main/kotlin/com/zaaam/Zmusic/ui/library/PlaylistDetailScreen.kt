package com.zaaam.Zmusic.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.zaaam.Zmusic.model.entity.toSong
import com.zaaam.Zmusic.ui.components.AlbumArt

// ── Design tokens ──────────────────────────────────────────────────────────────
private val ScreenBg      = Color(0xFF0C0A12)   // Bg0
private val CardSurface   = Color(0xFF1E1828)   // Bg2
private val CardBorder    = Color.White.copy(alpha = 0.10f)
private val TextPrimary   = Color(0xFFF3EEFB)
private val TextSecondary = Color(0xFF9A90AD)
private val AccentA = Color(0xFFFF9D5C)
private val AccentB = Color(0xFFFF4D8D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    viewModel: PlaylistDetailViewModel,
    onNavigateBack: () -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffleAll: (List<Song>) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit = { _, queue -> onPlayAll(queue) }
) {
    val playlistWithSongs by viewModel.playlistWithSongs.collectAsState()
    val songs        = playlistWithSongs?.songs?.map { it.toSong() } ?: emptyList()
    val playlistName = playlistWithSongs?.playlist?.name ?: ""

    val totalMs  = songs.sumOf { it.duration }
    val totalMin = totalMs / 60_000L
    val durationText = when {
        totalMin == 0L -> ""
        totalMin < 60L -> " · ${totalMin} menit"
        else           -> " · ${totalMin / 60}j ${totalMin % 60}m"
    }
    val subtitleText = "${songs.size} lagu$durationText"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        // ── Hero Header ────────────────────────────────────────────────────
        // FIX: 190 → 220dp agar subtitle tidak terpotong
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.linearGradient(
                        colorStops = arrayOf(
                            0f   to Color(0xFF2B1530),
                            0.6f to Color(0xFF160F1E),
                            1f   to ScreenBg
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end   = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.TopStart)
                    .background(
                        Brush.radialGradient(
                            listOf(AccentB.copy(alpha = 0.14f), Color.Transparent)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // Back button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(0.8.dp, CardBorder, RoundedCornerShape(11.dp))
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, "Kembali",
                        tint     = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(Modifier.weight(1f))

                // ── Cover + meta TENGAH (gaya mockup) ─────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(AccentA, AccentB))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote, null,
                            tint     = Color.White.copy(alpha = 0.92f),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text          = "PLAYLIST",
                        fontSize      = 11.sp,
                        letterSpacing = 2.2.sp,
                        fontWeight    = FontWeight.Medium,
                        color         = AccentA
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text          = playlistName,
                        fontSize      = 23.sp,
                        fontWeight    = FontWeight.SemiBold,
                        color         = TextPrimary,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text     = subtitleText,
                        fontSize = 12.sp,
                        color    = TextSecondary,
                        modifier = Modifier.padding(top = 3.dp, bottom = 14.dp)
                    )
                }
            }
        }

        if (songs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text      = "Playlist ini masih kosong.\nTambah lagu dari Home atau Search.",
                    color     = TextSecondary,
                    textAlign = TextAlign.Center,
                    fontSize  = 14.sp
                )
            }
            return@Column
        }

        // ── Action Row gaya mockup: shuffle · play bulat gradient ─────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable { onShuffleAll(songs) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Shuffle, "Acak", tint = TextSecondary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(26.dp))
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.linearGradient(listOf(AccentA, AccentB)))
                    .clickable { onPlayAll(songs) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, "Putar semua", tint = ScreenBg, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(26.dp))
            Spacer(Modifier.size(44.dp)) // penyeimbang visual
        }

        // ── Song List ──────────────────────────────────────────────────────
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                PlaylistSongRow(
                    song     = song,
                    number   = index + 1,
                    onClick  = { onSongClick(song, songs) },
                    onRemove = { viewModel.removeSong(song.id) }
                )
            }
        }
    }
}

@Composable
private fun PlaylistSongRow(
    song: Song,
    number: Int,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = number.toString(),
            fontSize = 13.sp,
            color = TextSecondary.copy(alpha = 0.7f),
            modifier = Modifier.width(22.dp)
        )
        AlbumArt(thumbnailUrl = song.thumbnailUrl, size = 48.dp, cornerRadius = 10.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist, fontSize = 12.sp, color = TextSecondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.MoreVert, "Menu", tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, containerColor = Color(0xFF1E1828)) {
                DropdownMenuItem(
                    text = { Text("Hapus dari Playlist", color = MaterialTheme.colorScheme.error, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) },
                    onClick = { showMenu = false; onRemove() }
                )
            }
        }
    }
}
