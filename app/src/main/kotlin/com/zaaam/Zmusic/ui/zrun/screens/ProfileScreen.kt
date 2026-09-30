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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.Zmusic.model.entity.ActivityEntity
import com.zaaam.Zmusic.ui.tracking.ActivitiesViewModel
import com.zaaam.Zmusic.ui.zrun.SectionHeader
import com.zaaam.Zmusic.ui.zrun.ZR
import com.zaaam.Zmusic.ui.zrun.ZRProgress
import com.zaaam.Zmusic.ui.zrun.ZRunSamples
import com.zaaam.Zmusic.ui.zrun.fmtInt
import com.zaaam.Zmusic.ui.zrun.fmtPaceQuote
import com.zaaam.Zmusic.ui.zrun.startOfMonthMs
import com.zaaam.Zmusic.ui.zrun.startOfYearMs
import com.zaaam.Zmusic.ui.zrun.zStyle

private const val YEAR_TARGET_KM = 1000.0

@Composable
fun ProfileScreen(vm: ActivitiesViewModel = hiltViewModel()) {
    val acts by vm.activities.collectAsState()
    val now = System.currentTimeMillis()
    val monthCount = acts.count { it.startTime >= startOfMonthMs(now) }
    val totalMeters = acts.sumOf { it.distanceMeters }
    val yearMeters = acts.filter { it.startTime >= startOfYearMs(now) }.sumOf { it.distanceMeters }
    val bestPace = acts.filter { it.distanceMeters > 300 }.minOfOrNull { it.avgPaceSecPerKm } ?: 0L

    ProfileContent(
        monthCount = monthCount,
        totalCount = acts.size,
        totalMeters = totalMeters,
        yearMeters = yearMeters,
        bestPace = bestPace,
        acts = acts
    )
}

@Composable
private fun ProfileContent(
    monthCount: Int,
    totalCount: Int,
    totalMeters: Double,
    yearMeters: Double,
    bestPace: Long,
    acts: List<ActivityEntity>
) {
    val badges = listOf(
        "Lari pertama" to acts.isNotEmpty(),
        "5 aktivitas" to (acts.size >= 5),
        "Sub-6 pace" to (bestPace in 1..359),
        "5 km pertama" to acts.any { it.distanceMeters >= 5_000 },
        "10 km" to acts.any { it.distanceMeters >= 10_000 },
        "100 km" to (totalMeters >= 100_000)
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(ZR.Bg)
            .statusBarsPadding()
            .padding(top = 8.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 150.dp)
    ) {
        // 1. Identitas rata tengah
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(90.dp).clip(CircleShape).background(ZR.Ember),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(84.dp).clip(CircleShape).background(ZR.Bg),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(78.dp).clip(CircleShape).background(ZR.Mint))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Pelari", style = zStyle(22.sp, FontWeight.Bold), color = ZR.Tx)
            Text(
                "${fmtInt(monthCount.toLong())} aktivitas bulan ini",
                style = zStyle(12.sp, FontWeight.Normal),
                color = ZR.Mut
            )
        }

        // 2. Tiga statistik (tanpa card)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            YouStat(fmtInt(totalCount.toLong()), "Lari")
            YouStat(fmtInt((totalMeters / 1000).toLong()), "Total km")
            YouStat(fmtPaceQuote(bestPace), "Pace terbaik")
        }

        // 3. Target tahun ini
        SectionHeader("Target tahun ini")
        Spacer(Modifier.height(4.dp))
        val yearKm = yearMeters / 1000.0
        ZRProgress(
            fraction = (yearKm / YEAR_TARGET_KM).toFloat(),
            height = 10.dp,
            fill = ZR.Ember
        )
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "${fmtInt(yearKm.toLong())} km",
                style = zStyle(12.sp, FontWeight.Normal),
                color = ZR.Mut
            )
            Text(
                "${fmtInt(YEAR_TARGET_KM.toLong())} km",
                style = zStyle(12.sp, FontWeight.Normal),
                color = ZR.Mut
            )
        }

        // 4. Lencana
        SectionHeader("Lencana")
        badges.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { (label, open) ->
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(if (open) ZR.Ember else ZR.S2)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            label,
                            style = zStyle(11.5.sp, FontWeight.Normal),
                            color = if (open) ZR.Tx else ZR.Mut,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YouStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = zStyle(22.sp, FontWeight.Bold), color = ZR.Tx)
        Spacer(Modifier.height(2.dp))
        Text(label, style = zStyle(12.sp, FontWeight.Normal), color = ZR.Mut)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0E, widthDp = 360, heightDp = 780)
@Composable
private fun ProfilePreview() {
    Box(Modifier.fillMaxSize().background(ZR.Bg)) {
        ProfileContent(
            monthCount = 14,
            totalCount = 38,
            totalMeters = 312_000.0,
            yearMeters = 312_000.0,
            bestPace = 288L,
            acts = ZRunSamples.activities
        )
    }
}
