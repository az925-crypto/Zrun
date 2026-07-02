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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.model.entity.PlaylistEntity
import com.zaaam.Zmusic.ui.home.HomeState
import com.zaaam.Zmusic.ui.home.HomeViewModel
import com.zaaam.Zmusic.ui.library.LibraryViewModel
import com.zaaam.Zmusic.ui.player.PlayerViewModel
import com.zaaam.Zmusic.ui.zrun.AddToPlaylistDialog
import com.zaaam.Zmusic.ui.zrun.CreatePlaylistDialog
import com.zaaam.Zmusic.ui.zrun.SectionHeader
import com.zaaam.Zmusic.ui.zrun.SongRow
import com.zaaam.Zmusic.ui.zrun.ZR

@Composable
fun MusicHomeScreen(
    player: PlayerViewModel,
    onOpenPlayer: () -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    homeVm: HomeViewModel = hiltViewModel(),
    libVm: LibraryViewModel = hiltViewModel()
) {
    val state by homeVm.state.collectAsState()
    val playlists by libVm.playlists.collectAsState()
    val queue by player.queueManager.queue.collectAsState()
    val idx by player.queueManager.currentIndex.collectAsState()
    val currentId = queue.getOrNull(idx)?.id

    var songToAdd by remember { mutableStateOf<Song?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { if (state !is HomeState.Success) homeVm.loadDiscovery() }

    fun play(song: Song, list: List<Song>) {
        player.playSong(song, list, autoShuffle = false)
        onOpenPlayer()
    }

    Column(
        Modifier.fillMaxSize().background(ZR.Bg).verticalScroll(rememberScrollState())
            .statusBarsPadding().padding(horizontal = 16.dp).padding(bottom = 150.dp)
    ) {
        Text("Musik", color = ZR.Tx, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp,
            modifier = Modifier.padding(top = 8.dp))

        // search bar
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp).height(44.dp)
                .clip(RoundedCornerShape(14.dp)).background(ZR.S1)
                .clickable(onClick = onOpenSearch).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, null, tint = ZR.Mut, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Cari lagu, artis, atau playlist", color = ZR.Mut, fontSize = 13.sp)
        }

        when (val s = state) {
            is HomeState.Loading -> {
                Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ZR.Ember2)
                }
            }
            is HomeState.Error -> {
                Text(s.message, color = ZR.Mut, fontSize = 13.sp, modifier = Modifier.padding(top = 40.dp))
            }
            is HomeState.Success -> {
                val c = s.content
                val running = c.trendingSongs.ifEmpty { c.allSongs }

                // hero "dibuat untuk lari"
                if (running.isNotEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 16.dp).height(150.dp)
                            .clip(RoundedCornerShape(22.dp)).background(ZR.Music)
                            .clickable { play(running.first(), running) }.padding(16.dp)
                    ) {
                        Column(Modifier.align(Alignment.BottomStart)) {
                            Text("DIBUAT UNTUK LARI", color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 1.sp)
                            Text("Power Mix", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                            Text("Tempo stabil buat lari", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                        }
                        Box(Modifier.align(Alignment.BottomEnd).size(46.dp)
                            .clip(RoundedCornerShape(23.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.PlayArrow, null, tint = Color(0xFF16121D), modifier = Modifier.size(26.dp))
                        }
                    }
                }

                // playlist kamu (+ tombol buat)
                SectionHeader("Playlist kamu", action = "+ Buat", onAction = { showCreate = true })
                if (playlists.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(playlists, key = { it.id }) { pl -> PlaylistCard(pl) { onOpenPlaylist(pl.id) } }
                    }
                } else {
                    Text("Belum ada playlist. Tekan “+ Buat”, atau tekan-lama lagu untuk menambah.",
                        color = ZR.Mut, fontSize = 12.5.sp)
                }

                // rekomendasi
                if (c.featuredSongs.isNotEmpty()) {
                    SectionHeader("Pilihan buatmu")
                    c.featuredSongs.take(6).forEach { song ->
                        SongRow(song = song, isPlaying = song.id == currentId,
                            onClick = { play(song, c.featuredSongs) }, onLongClick = { songToAdd = song })
                    }
                }

                if (running.isNotEmpty()) {
                    SectionHeader("Trending")
                    running.take(10).forEach { song ->
                        SongRow(song = song, isPlaying = song.id == currentId,
                            onClick = { play(song, running) }, onLongClick = { songToAdd = song })
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreatePlaylistDialog(
            onDismiss = { showCreate = false },
            onCreate = { name -> libVm.createPlaylist(name) }
        )
    }
    songToAdd?.let { s ->
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { songToAdd = null },
            onPick = { id -> libVm.addSongToPlaylist(id, s) },
            onCreateNew = { name -> libVm.createPlaylistAndAdd(name, s) }
        )
    }
}

@Composable
private fun PlaylistCard(pl: PlaylistEntity, onClick: () -> Unit) {
    Column(Modifier.width(120.dp).clickable(onClick = onClick)) {
        Box(Modifier.size(120.dp).clip(RoundedCornerShape(14.dp)).background(ZR.artBrush(pl.name)))
        Spacer(Modifier.height(7.dp))
        Text(pl.name, color = ZR.Tx, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("Playlist", color = ZR.Mut, fontSize = 11.sp)
    }
}
