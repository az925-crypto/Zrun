package com.zaaam.Zmusic.ui.zrun.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.ui.home.HomeState
import com.zaaam.Zmusic.ui.home.HomeViewModel
import com.zaaam.Zmusic.ui.library.LibraryViewModel
import com.zaaam.Zmusic.ui.player.PlayerViewModel
import com.zaaam.Zmusic.ui.zrun.AddToPlaylistDialog
import com.zaaam.Zmusic.ui.zrun.CreatePlaylistDialog
import com.zaaam.Zmusic.ui.zrun.SectionHeader
import com.zaaam.Zmusic.ui.zrun.SongRow
import com.zaaam.Zmusic.ui.zrun.ZR
import com.zaaam.Zmusic.ui.zrun.ZRArt
import com.zaaam.Zmusic.ui.zrun.ZRIcons
import com.zaaam.Zmusic.ui.zrun.ZRRow
import com.zaaam.Zmusic.ui.zrun.ZRunSamples
import com.zaaam.Zmusic.ui.zrun.dashedOutline
import com.zaaam.Zmusic.ui.zrun.fmtInt
import com.zaaam.Zmusic.ui.zrun.playlistSongCount
import com.zaaam.Zmusic.ui.zrun.zStyle

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
        Modifier
            .fillMaxSize()
            .background(ZR.Bg)
            .statusBarsPadding()
            .padding(top = 10.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 150.dp)
    ) {
        Text("Musik", style = zStyle(26.sp, FontWeight.Bold, (-0.52).sp), color = ZR.Tx)

        // 1. Search pill
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(ZR.S1)
                .clickable(onClick = onOpenSearch)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(ZRIcons.Search, contentDescription = "Cari", tint = ZR.Mut, modifier = Modifier.size(20.dp))
            Text("Cari lagu, artis, playlist", style = zStyle(13.sp, FontWeight.Normal), color = ZR.Mut)
        }

        when (val s = state) {
            is HomeState.Loading -> {
                Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ZR.Ember)
                }
            }
            is HomeState.Error -> {
                Text(s.message, style = zStyle(13.sp, FontWeight.Normal), color = ZR.Mut,
                    modifier = Modifier.padding(top = 40.dp))
            }
            is HomeState.Success -> {
                val c = s.content
                val running = c.trendingSongs.ifEmpty { c.allSongs }

                // 2. Hero
                if (running.isNotEmpty()) {
                    val first = running.first()
                    val heroTitle = c.heroSong?.title ?: first.title
                    val totalMs = running.sumOf { it.duration }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .height(148.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(ZR.Violet)
                            .clickable { play(first, running) }
                            .padding(16.dp)
                    ) {
                        Row(
                            Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Dibuat untuk lari",
                                    style = zStyle(11.sp, FontWeight.Bold, 1.sp),
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    heroTitle,
                                    style = zStyle(24.sp, FontWeight.Bold),
                                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${fmtInt(running.size.toLong())} lagu · ${fmtTotal(totalMs)}",
                                    style = zStyle(12.sp, FontWeight.Normal),
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Box {
                                ZRArt(seed = first.id, thumbnailUrl = first.thumbnailUrl, size = 96.dp, radius = 18.dp)
                                Box(
                                    Modifier
                                        .align(Alignment.BottomStart)
                                        .offset(x = (-12).dp, y = 12.dp)
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        ZRIcons.Play, contentDescription = "Putar",
                                        tint = Color(0xFF14161B), modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Trending
                if (running.isNotEmpty()) {
                    SectionHeader("Trending")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        itemsIndexed(running.take(10), key = { _, song -> song.id }) { i, song ->
                            @OptIn(ExperimentalFoundationApi::class)
                            Column(
                                Modifier
                                    .width(100.dp)
                                    .combinedClickable(
                                        onClick = { play(song, running) },
                                        onLongClick = { songToAdd = song }
                                    )
                            ) {
                                Box {
                                    ZRArt(seed = song.id, thumbnailUrl = song.thumbnailUrl, size = 100.dp, radius = 20.dp)
                                    Box(
                                        Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(ZR.S2),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            fmtInt((i + 1).toLong()),
                                            style = zStyle(13.sp, FontWeight.Bold),
                                            color = ZR.Tx
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    song.title,
                                    style = zStyle(12.5.sp, FontWeight.SemiBold),
                                    color = ZR.Tx, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    song.artist,
                                    style = zStyle(12.sp, FontWeight.Normal),
                                    color = ZR.Mut, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 4. Terakhir diputar
                if (c.recentSongs.isNotEmpty()) {
                    SectionHeader("Terakhir diputar")
                    c.recentSongs.take(4).forEach { song ->
                        SongRow(
                            song = song,
                            isPlaying = song.id == currentId,
                            onClick = { play(song, c.recentSongs) },
                            onLongClick = { songToAdd = song }
                        )
                    }
                }

                // 5. Pilihan buatmu
                if (c.featuredSongs.isNotEmpty()) {
                    SectionHeader("Pilihan buatmu")
                    c.featuredSongs.take(5).forEach { song ->
                        SongRow(
                            song = song,
                            isPlaying = song.id == currentId,
                            onClick = { play(song, c.featuredSongs) },
                            onLongClick = { songToAdd = song }
                        )
                    }
                }

                // 6. Playlist kamu
                SectionHeader("Playlist kamu")
                if (playlists.isEmpty()) {
                    ZRRow(onClick = { showCreate = true }) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .dashedOutline(Color(0xFF3A3F4A), radius = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", style = zStyle(24.sp, FontWeight.Normal), color = ZR.Mut)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Buat playlist pertamamu",
                                style = zStyle(14.sp, FontWeight.SemiBold),
                                color = ZR.Tx, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Simpan lagu favorit untuk lari",
                                style = zStyle(12.sp, FontWeight.Normal),
                                color = ZR.Mut
                            )
                        }
                    }
                } else {
                    playlists.forEachIndexed { i, pl ->
                        val n = playlistSongCount(pl.id, player.musicRepository)
                        ZRRow(
                            onClick = { onOpenPlaylist(pl.id) },
                            showDivider = i < playlists.size - 1
                        ) {
                            ZRArt(seed = pl.name, size = 48.dp, radius = 14.dp)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    pl.name,
                                    style = zStyle(14.sp, FontWeight.SemiBold),
                                    color = ZR.Tx, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${fmtInt(n.toLong())} lagu",
                                    style = zStyle(12.sp, FontWeight.Normal),
                                    color = ZR.Mut
                                )
                            }
                        }
                    }
                    Text(
                        "Buat playlist",
                        style = zStyle(13.sp, FontWeight.SemiBold),
                        color = ZR.Ember,
                        modifier = Modifier
                            .clickable { showCreate = true }
                            .padding(vertical = 9.dp)
                    )
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

private fun fmtTotal(ms: Long): String {
    val m = (ms / 60_000L).toInt()
    return if (m >= 60) "${fmtInt((m / 60).toLong())} j ${fmtInt((m % 60).toLong())} mnt"
    else "${fmtInt(m.toLong())} mnt"
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0E, widthDp = 360, heightDp = 780)
@Composable
private fun MusicPreview() {
    Column(
        Modifier.fillMaxSize().background(ZR.Bg).padding(horizontal = 16.dp)
    ) {
        Text("Musik", style = zStyle(26.sp, FontWeight.Bold, (-0.52).sp), color = ZR.Tx)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(ZR.S1)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(ZRIcons.Search, contentDescription = "Cari", tint = ZR.Mut, modifier = Modifier.size(20.dp))
            Text("Cari lagu, artis, playlist", style = zStyle(13.sp, FontWeight.Normal), color = ZR.Mut)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .height(148.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(ZR.Violet)
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Dibuat untuk lari", style = zStyle(11.sp, FontWeight.Bold, 1.sp),
                        color = Color.White.copy(alpha = 0.8f))
                    Spacer(Modifier.height(4.dp))
                    Text("Lari malam", style = zStyle(24.sp, FontWeight.Bold), color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("30 lagu · 1 j 42 mnt", style = zStyle(12.sp, FontWeight.Normal),
                        color = Color.White.copy(alpha = 0.8f))
                }
                Spacer(Modifier.width(12.dp))
                Box {
                    ZRArt(seed = "Lari malam", size = 96.dp, radius = 18.dp)
                    Box(
                        Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-12).dp, y = 12.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(ZRIcons.Play, contentDescription = "Putar", tint = Color(0xFF14161B),
                            modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        SectionHeader("Trending")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ZRunSamples.songs.forEachIndexed { i, song ->
                Column(Modifier.width(100.dp)) {
                    Box {
                        ZRArt(seed = song.id, size = 100.dp, radius = 20.dp)
                        Box(
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ZR.S2),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(fmtInt((i + 1).toLong()), style = zStyle(13.sp, FontWeight.Bold), color = ZR.Tx)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(song.title, style = zStyle(12.5.sp, FontWeight.SemiBold), color = ZR.Tx,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, style = zStyle(12.sp, FontWeight.Normal), color = ZR.Mut,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
