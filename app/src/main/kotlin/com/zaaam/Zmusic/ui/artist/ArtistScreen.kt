package com.zaaam.Zmusic.ui.artist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
private val ArtistBgGradient = com.zaaam.Zmusic.ui.theme.PageBgPlum
private val BluePrimary   = com.zaaam.Zmusic.ui.theme.AccentMagenta
private val TextPrimary   = com.zaaam.Zmusic.ui.theme.TextPrimary
private val TextSecondary = com.zaaam.Zmusic.ui.theme.TextMuted
private val CardSurface   = com.zaaam.Zmusic.ui.theme.Bg2
private val CardBorder    = Color.White.copy(alpha = 0.09f)

@Composable
fun ArtistScreen(
    viewModel: ArtistViewModel,
    onSongClick: (Song, List<Song>) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state      by viewModel.state.collectAsState()
    val artistName by viewModel.artistName.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(ArtistBgGradient)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top Bar ───────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, "Kembali", tint = Color.White)
                }
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(
                        Brush.radialGradient(listOf(BluePrimary.copy(alpha = 0.5f), Color(0xFF1A2030)))
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = artistName.ifBlank { "Artis" },
                        style      = MaterialTheme.typography.titleLarge,
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    if (state is ArtistScreenState.Success) {
                        Text(
                            "${(state as ArtistScreenState.Success).songs.size} lagu",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // ── Content ───────────────────────────────────────────────────
            when (val s = state) {
                is ArtistScreenState.Idle    -> {}

                is ArtistScreenState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = BluePrimary)
                            Spacer(Modifier.height(16.dp))
                            Text("Mencari lagu dari \"$artistName\"...", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }

                is ArtistScreenState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Icon(Icons.Default.SearchOff, null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(s.message, color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(onClick = { viewModel.retry() }) {
                                Text("Coba Lagi", color = BluePrimary)
                            }
                        }
                    }
                }

                is ArtistScreenState.Success -> {
                    val songs = s.songs

                    // Play All / Shuffle bar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick  = { onSongClick(songs.first(), songs) },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Putar Semua", fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick  = { val s2 = songs.shuffled(); onSongClick(s2.first(), s2) },
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = BluePrimary)
                        ) {
                            Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Acak")
                        }
                    }

                    LazyColumn(
                        contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                            ArtistSongRow(
                                song        = song,
                                index       = index + 1,
                                onClick     = { onSongClick(song, songs) },
                                onLongClick = { onSongLongClick(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtistSongRow(
    song: Song,
    index: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text       = "$index",
            fontSize   = 12.sp,
            color      = TextSecondary,
            modifier   = Modifier.width(24.dp),
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.width(8.dp))
        AlbumArt(thumbnailUrl = song.thumbnailUrl, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = song.title,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
                color      = TextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            if (song.duration > 0L) {
                val mins = song.duration / 60000
                val secs = (song.duration % 60000) / 1000
                Text(
                    text     = "%d:%02d".format(mins, secs),
                    fontSize = 11.sp,
                    color    = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Icon(Icons.Default.PlayArrow, null, tint = TextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
    }
}
