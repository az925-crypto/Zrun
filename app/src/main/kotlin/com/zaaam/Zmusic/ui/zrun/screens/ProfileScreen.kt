package com.zaaam.Zmusic.ui.zrun.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.Zmusic.ui.tracking.ActivitiesViewModel
import com.zaaam.Zmusic.ui.zrun.StatTile
import com.zaaam.Zmusic.ui.zrun.ZR
import com.zaaam.Zmusic.ui.zrun.ZRCard
import com.zaaam.Zmusic.util.LocationUtils

@Composable
fun ProfileScreen(vm: ActivitiesViewModel = hiltViewModel()) {
    val acts by vm.activities.collectAsState()
    val totalKm = acts.sumOf { it.distanceMeters }
    val bestPace = acts.filter { it.distanceMeters > 300 }.minOfOrNull { it.avgPaceSecPerKm } ?: 0L
    val yearTarget = 1000.0

    Column(
        Modifier.fillMaxSize().background(ZR.Bg).verticalScroll(rememberScrollState())
            .statusBarsPadding().padding(horizontal = 16.dp).padding(bottom = 150.dp)
    ) {
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(ZR.MintG))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Pelari", color = ZR.Tx, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp)
                Text("ZRun · ${acts.size} aktivitas", color = ZR.Mut, fontSize = 12.5.sp)
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("${acts.size}", "TOTAL LARI", Modifier.weight(1f))
            StatTile(LocationUtils.formatDistanceKm(totalKm), "TOTAL KM", Modifier.weight(1f))
            StatTile(if (bestPace > 0) "${LocationUtils.formatPace(bestPace)}" else "--:--", "PACE TERBAIK", Modifier.weight(1f), ZR.Mint)
        }

        Text("Target tahunan", color = ZR.Tx, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
            modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
        ZRCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Progress", color = ZR.Mut, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("${LocationUtils.formatDistanceKm(totalKm)} / ${yearTarget.toInt()} km", color = ZR.Tx, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(9.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(5.dp)).background(ZR.S3)) {
                    Box(Modifier.fillMaxWidth((totalKm / 1000.0 / yearTarget).coerceIn(0.0, 1.0).toFloat())
                        .height(8.dp).background(ZR.Ember))
                }
            }
        }

        Text("Lencana", color = ZR.Tx, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp,
            modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Badge("🏅", "Lari pertama", acts.isNotEmpty(), Modifier.weight(1f))
            Badge("🔥", "5 aktivitas", acts.size >= 5, Modifier.weight(1f))
            Badge("⚡", "Sub-6 pace", bestPace in 1..359, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Badge(emoji: String, label: String, unlocked: Boolean, modifier: Modifier = Modifier) {
    ZRCard(modifier) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 26.sp, modifier = Modifier.then(if (unlocked) Modifier else Modifier))
            Spacer(Modifier.height(8.dp))
            Text(label, color = if (unlocked) ZR.Tx else ZR.Faint, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
