package com.zaaam.Zmusic.ui.zrun.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zaaam.Zmusic.ui.player.PlayerViewModel
import com.zaaam.Zmusic.ui.zrun.ZR

@Composable
fun PlayerScreenZR(player: PlayerViewModel, onClose: () -> Unit) {
    val queue by player.queueManager.queue.collectAsState()
    val idx by player.queueManager.currentIndex.collectAsState()
    val song = queue.getOrNull(idx)
    val isPlaying by player.isPlaying.collectAsState()
    val progress by player.progress.collectAsState()
    val position by player.currentPosition.collectAsState()
    val isShuffled by player.queueManager.isShuffled.collectAsState()

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF2A1740), ZR.Bg, ZR.Bg))
        )
    ) {
        if (song == null) {
            Text("Belum ada lagu", color = ZR.Mut, modifier = Modifier.align(Alignment.Center))
            return@Box
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 22.dp)) {
            // top bar
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.KeyboardArrowDown, "tutup", tint = ZR.Tx,
                    modifier = Modifier.size(30.dp).clip(CircleShape).clickable(onClick = onClose))
                Text("SEDANG DIPUTAR", color = ZR.Mut, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp)
                Spacer(Modifier.size(30.dp))
            }

            // art besar
            Box(
                Modifier.fillMaxWidth().padding(top = 18.dp).aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp)).background(ZR.artBrush(song.id)),
                contentAlignment = Alignment.Center
            ) {
                if (song.thumbnailUrl.isNotBlank()) {
                    AsyncImage(model = song.thumbnailUrl, contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)))
                }
            }

            // judul
            Text(song.title, color = ZR.Tx, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 22.dp))
            Text(song.artist, color = ZR.Mut, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)

            // scrubber
            Slider(
                value = progress.coerceIn(0f, 1f),
                onValueChange = { player.seekTo(it) },
                modifier = Modifier.padding(top = 10.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White, activeTrackColor = ZR.Ember2, inactiveTrackColor = ZR.S3
                )
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fmtMs(position), color = ZR.Mut, fontSize = 11.sp)
                Text(fmtMs(song.duration), color = ZR.Mut, fontSize = 11.sp)
            }

            // controls
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Shuffle, "shuffle", tint = if (isShuffled) ZR.Mint else ZR.Mut,
                    modifier = Modifier.size(24.dp).clickable { player.toggleShuffle() })
                Icon(Icons.Filled.SkipPrevious, "prev", tint = ZR.Tx,
                    modifier = Modifier.size(40.dp).clickable { player.skipPrevious() })
                Box(
                    Modifier.size(70.dp).clip(CircleShape).background(Color.White)
                        .clickable { player.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "play",
                        tint = Color(0xFF16121D), modifier = Modifier.size(34.dp))
                }
                Icon(Icons.Filled.SkipNext, "next", tint = ZR.Tx,
                    modifier = Modifier.size(40.dp).clickable { player.skipNext() })
                Icon(Icons.Filled.Repeat, "repeat", tint = ZR.Mut,
                    modifier = Modifier.size(24.dp).clickable { player.toggleRepeat() })
            }
        }
    }
}

private fun fmtMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
