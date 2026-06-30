package com.zaaam.Zmusic.ui.zrun.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.Zmusic.model.entity.toPlaylist
import com.zaaam.Zmusic.ui.library.PlaylistDetailViewModel
import com.zaaam.Zmusic.ui.player.PlayerViewModel
import com.zaaam.Zmusic.ui.zrun.SongRow
import com.zaaam.Zmusic.ui.zrun.ZR

@Composable
fun PlaylistScreenZR(
    player: PlayerViewModel,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
    vm: PlaylistDetailViewModel = hiltViewModel()
) {
    val pws by vm.playlistWithSongs.collectAsState()
    val queue by player.queueManager.queue.collectAsState()
    val idx by player.queueManager.currentIndex.collectAsState()
    val currentId = queue.getOrNull(idx)?.id

    val playlist = pws?.toPlaylist()
    val songs = playlist?.songs ?: emptyList()
    val name = playlist?.name ?: "Playlist"

    Box(Modifier.fillMaxSize().background(ZR.Bg)) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(
                    Modifier.fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0x528B5CFF), Color(0x00000000))))
                        .statusBarsPadding().padding(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "kembali", tint = ZR.Tx,
                        modifier = Modifier.size(26.dp).clickable(onClick = onBack))
                    Row(Modifier.padding(top = 12.dp)) {
                        Box(Modifier.size(96.dp).clip(RoundedCornerShape(14.dp)).background(ZR.artBrush(name)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.align(Alignment.Bottom)) {
                            Text("PLAYLIST", color = ZR.Ember2, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 1.sp)
                            Text(name, color = ZR.Tx, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                            Text("${songs.size} lagu", color = ZR.Mut, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(46.dp).clip(CircleShape).background(ZR.S2)
                            .clickable { if (songs.isNotEmpty()) { player.playPlaylistShuffled(songs); onOpenPlayer() } },
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Shuffle, "acak", tint = ZR.Tx, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Box(Modifier.size(54.dp).clip(CircleShape).background(ZR.Ember)
                            .clickable { if (songs.isNotEmpty()) { player.playSong(songs.first(), songs); onOpenPlayer() } },
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.PlayArrow, "putar", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
            itemsIndexed(songs, key = { _, s -> s.id }) { i, song ->
                Box(Modifier.padding(horizontal = 16.dp)) {
                    SongRow(song = song, isPlaying = song.id == currentId, index = i + 1,
                        onClick = { player.playSong(song, songs); onOpenPlayer() })
                }
            }
            item { Spacer(Modifier.size(40.dp)) }
        }
    }
}
