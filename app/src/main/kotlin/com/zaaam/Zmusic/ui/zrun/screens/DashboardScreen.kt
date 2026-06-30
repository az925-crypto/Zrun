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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.Zmusic.ui.player.PlayerViewModel
import com.zaaam.Zmusic.ui.tracking.ActivitiesViewModel
import com.zaaam.Zmusic.ui.zrun.ArtBox
import com.zaaam.Zmusic.ui.zrun.EmberButton
import com.zaaam.Zmusic.ui.zrun.ProgressRing
import com.zaaam.Zmusic.ui.zrun.ZR
import com.zaaam.Zmusic.ui.zrun.ZRCard
import com.zaaam.Zmusic.util.LocationUtils

@Composable
fun DashboardScreen(
    player: PlayerViewModel,
    onStartRun: () -> Unit,
    onOpenMusic: () -> Unit,
    onOpenPlayer: () -> Unit,
    activitiesVm: ActivitiesViewModel = hiltViewModel()
) {
    val activities by activitiesVm.activities.collectAsState()
    val queue by player.queueManager.queue.collectAsState()
    val idx by player.queueManager.currentIndex.collectAsState()
    val song = queue.getOrNull(idx)

    val weekTarget = 25.0
    val nowMs = System.currentTimeMillis()
    val weekMeters = activities.filter { nowMs - it.startTime < 7L * 24 * 3600 * 1000 }
        .sumOf { it.distanceMeters }
    val weekKm = weekMeters / 1000.0

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 150.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Selamat datang 👋", color = ZR.Mut, fontSize = 12.5.sp)
                Text("Pelari", color = ZR.Tx, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp)
            }
            Box(Modifier.size(40.dp).clip(CircleShape).background(ZR.MintG))
        }

        // Ring target mingguan
        ZRCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    ProgressRing(progress = (weekKm / weekTarget).toFloat(), size = 84)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Target mingguan", color = ZR.Mut, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(LocationUtils.formatDistanceKm(weekMeters), color = ZR.Tx,
                            fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
                        Text(" / ${weekTarget.toInt()} km", color = ZR.Mut, fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 3.dp))
                    }
                    Text("🔥 ${activities.size} aktivitas total", color = ZR.Mut, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        EmberButton(
            text = "MULAI LARI",
            icon = Icons.Filled.DirectionsRun,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            onClick = onStartRun
        )

        // Lanjut dengerin
        if (song != null) {
            Text("Lagi diputar", color = ZR.Tx, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
                modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
            ZRCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(10.dp).clickable(onClick = onOpenPlayer), verticalAlignment = Alignment.CenterVertically) {
                    ArtBox(seed = song.id, thumbnailUrl = song.thumbnailUrl, size = 46)
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, color = ZR.Tx, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                        Text(song.artist, color = ZR.Mut, fontSize = 12.sp, maxLines = 1)
                    }
                    Box(Modifier.size(38.dp).clip(CircleShape).background(ZR.Ember), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
        } else {
            Text("Musik", color = ZR.Tx, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
                modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
            ZRCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp).fillMaxWidth().clickable(onClick = onOpenMusic),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(ZR.Music))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Pilih soundtrack lari", color = ZR.Tx, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Cari & putar lagu dari YouTube", color = ZR.Mut, fontSize = 12.sp)
                    }
                }
            }
        }

        // Lari terakhir
        val last = activities.firstOrNull()
        if (last != null) {
            Text("Lari terakhir", color = ZR.Tx, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
                modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
            ZRCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(Color(0x24FF2E63)),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.DirectionsRun, null, tint = ZR.Ember2, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(last.title, color = ZR.Tx, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                        Text("${LocationUtils.formatDistanceKm(last.distanceMeters)} km · ${LocationUtils.formatDuration(last.durationMillis)}",
                            color = ZR.Mut, fontSize = 12.sp)
                    }
                    Text("${LocationUtils.formatPace(last.avgPaceSecPerKm)}/km", color = ZR.Mint,
                        fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }
        }
    }
}
