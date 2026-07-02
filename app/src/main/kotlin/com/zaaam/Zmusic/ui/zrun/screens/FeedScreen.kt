package com.zaaam.Zmusic.ui.zrun.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
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
import com.zaaam.Zmusic.model.entity.ActivityEntity
import com.zaaam.Zmusic.ui.tracking.ActivitiesViewModel
import com.zaaam.Zmusic.ui.zrun.ZR
import com.zaaam.Zmusic.ui.zrun.ZRCard
import com.zaaam.Zmusic.util.LocationUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FeedScreen(vm: ActivitiesViewModel = hiltViewModel()) {
    val acts by vm.activities.collectAsState()
    val totalKm = acts.sumOf { it.distanceMeters }
    val totalMs = acts.sumOf { it.durationMillis }

    Box(Modifier.fillMaxSize().background(ZR.Bg)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 150.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Aktivitas", color = ZR.Tx, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp)
            }
            item {
                ZRCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceAround) {
                        MiniStat("${acts.size}", "LARI")
                        MiniStat(LocationUtils.formatDistanceKm(totalKm), "KM")
                        MiniStat(LocationUtils.formatDuration(totalMs), "WAKTU")
                    }
                }
            }
            if (acts.isEmpty()) {
                item {
                    Text("Belum ada aktivitas.\nTekan tombol RUN buat mulai lari pertamamu!",
                        color = ZR.Mut, fontSize = 14.sp, modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            items(acts, key = { it.id }) { a -> FeedCard(a) }
        }
    }
}

@Composable
private fun MiniStat(v: String, l: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(v, color = ZR.Tx, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        Text(l, color = ZR.Mut, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.6.sp)
    }
}

@Composable
private fun FeedCard(a: ActivityEntity) {
    ZRCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)).background(Color(0x24FF2E63)),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.DirectionsRun, null, tint = ZR.Ember2, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(a.title, color = ZR.Tx, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                Text(fmt.format(Date(a.startTime)), color = ZR.Mut, fontSize = 11.sp)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("${LocationUtils.formatDistanceKm(a.distanceMeters)} km", color = ZR.Tx, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(LocationUtils.formatDuration(a.durationMillis), color = ZR.Tx, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${LocationUtils.formatPace(a.avgPaceSecPerKm)}/km", color = ZR.Mint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private val fmt = SimpleDateFormat("EEE, d MMM yyyy • HH:mm", Locale.getDefault())
