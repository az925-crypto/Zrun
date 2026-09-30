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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.Zmusic.model.entity.toPlaylist
import com.zaaam.Zmusic.ui.library.PlaylistDetailViewModel
import com.zaaam.Zmusic.ui.player.PlayerViewModel
import com.zaaam.Zmusic.ui.zrun.SongRow
import com.zaaam.Zmusic.ui.zrun.ZR
import com.zaaam.Zmusic.ui.zrun.ZRArt
import com.zaaam.Zmusic.ui.zrun.ZRIcons
import com.zaaam.Zmusic.ui.zrun.ZRunSamples
import com.zaaam.Zmusic.ui.zrun.fmtInt
import com.zaaam.Zmusic.ui.zrun.zStyle

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

    Box(
        Modifier
            .fillMaxSize()
            .background(ZR.Bg)
            .statusBarsPadding()
            .padding(top = 10.dp)
    ) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(ZR.Violet)
                        .padding(16.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali",
                        tint = Color.White,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onBack)
                            .padding(12.dp)
                    )
                    Row(Modifier.padding(top = 4.dp)) {
                        ZRArt(seed = name, size = 96.dp, radius = 14.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.align(Alignment.Bottom).weight(1f)) {
                            Text(
                                "Playlist",
                                style = zStyle(11.sp, FontWeight.Bold),
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                name,
                                style = zStyle(24.sp, FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                "${fmtInt(songs.size.toLong())} lagu",
                                style = zStyle(12.sp, FontWeight.Normal),
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable {
                                    if (songs.isNotEmpty()) {
                                        player.playPlaylistShuffled(songs); onOpenPlayer()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(ZRIcons.Next, contentDescription = "Acak", tint = Color.White,
                                modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Box(
                            Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    if (songs.isNotEmpty()) {
                                        player.playSong(songs.first(), songs); onOpenPlayer()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(ZRIcons.Play, contentDescription = "Putar", tint = ZR.Violet,
                                modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
            itemsIndexed(songs, key = { _, s -> s.id }) { i, song ->
                Box(Modifier.padding(horizontal = 16.dp)) {
                    SongRow(
                        song = song, isPlaying = song.id == currentId, index = i + 1,
                        onClick = { player.playSong(song, songs); onOpenPlayer() },
                        onLongClick = { vm.removeSong(song.id) }
                    )
                }
            }
            item { Spacer(Modifier.size(40.dp)) }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0E, widthDp = 360, heightDp = 780)
@Composable
private fun PlaylistPreview() {
    Column(
        Modifier
            .fillMaxSize()
            .background(ZR.Bg)
            .padding(16.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(ZR.Violet)
                .padding(16.dp)
        ) {
            Row {
                ZRArt(seed = "Lari malam", size = 96.dp, radius = 14.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.align(Alignment.Bottom)) {
                    Text("Playlist", style = zStyle(11.sp, FontWeight.Bold),
                        color = Color.White.copy(alpha = 0.8f))
                    Text("Lari malam", style = zStyle(24.sp, FontWeight.Bold), color = Color.White)
                    Text("30 lagu", style = zStyle(12.sp, FontWeight.Normal),
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
        ZRunSamples.songs.forEachIndexed { i, song ->
            Box(Modifier.padding(horizontal = 0.dp)) {
                SongRow(song = song, isPlaying = i == 0, index = i + 1, onClick = {})
            }
        }
    }
}
