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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.Zmusic.model.entity.ActivityEntity
import com.zaaam.Zmusic.ui.tracking.ActivitiesViewModel
import com.zaaam.Zmusic.ui.zrun.ZR
import com.zaaam.Zmusic.ui.zrun.ZRBigNumber
import com.zaaam.Zmusic.ui.zrun.ZRRow
import com.zaaam.Zmusic.ui.zrun.ZRRouteThumb
import com.zaaam.Zmusic.ui.zrun.ZRunSamples
import com.zaaam.Zmusic.ui.zrun.fmtDurShort
import com.zaaam.Zmusic.ui.zrun.fmtInt
import com.zaaam.Zmusic.ui.zrun.fmtKm1
import com.zaaam.Zmusic.ui.zrun.fmtPaceQuote
import com.zaaam.Zmusic.ui.zrun.monthNameId
import com.zaaam.Zmusic.ui.zrun.startOfMonthMs
import com.zaaam.Zmusic.ui.zrun.startOfWeekMs
import com.zaaam.Zmusic.ui.zrun.weekdayId
import com.zaaam.Zmusic.ui.zrun.zStyle
import com.zaaam.Zmusic.util.LocationUtils

@Composable
fun FeedScreen(vm: ActivitiesViewModel = hiltViewModel()) {
    val acts by vm.activities.collectAsState()
    val now = System.currentTimeMillis()
    val monthActs = acts.filter { it.startTime >= startOfMonthMs(now) }
    val monthMeters = monthActs.sumOf { it.distanceMeters }
    val monthMs = monthActs.sumOf { it.durationMillis }

    val weekStart = startOfWeekMs(now)
    val weekKm = (0..3).map { w ->
        val s = weekStart - (3 - w) * 7 * 86_400_000L
        acts.filter { it.startTime in s until s + 7 * 86_400_000L }.sumOf { it.distanceMeters } / 1000.0
    }

    FeedContent(
        monthName = monthNameId(now),
        monthKm = monthMeters / 1000.0,
        monthCount = monthActs.size,
        monthMs = monthMs,
        weekKm = weekKm,
        acts = acts
    )
}

@Composable
private fun FeedContent(
    monthName: String,
    monthKm: Double,
    monthCount: Int,
    monthMs: Long,
    weekKm: List<Double>,
    acts: List<ActivityEntity>
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(ZR.Bg)
            .statusBarsPadding()
            .padding(top = 6.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 150.dp)
    ) {
        // 1. Ringkasan bulan
        Text(monthName, style = zStyle(12.sp, FontWeight.Normal), color = ZR.Mut)
        ZRBigNumber(fmtKm1(monthKm * 1000), "km")
        Spacer(Modifier.height(6.dp))
        Text(
            "${fmtInt(monthCount.toLong())} lari · ${fmtDurShort(monthMs)}",
            style = zStyle(12.sp, FontWeight.Normal),
            color = ZR.Mut
        )

        // 2. Strip mingguan (4 minggu terakhir, paling kanan = berjalan)
        val maxKm = weekKm.maxOrNull()?.takeIf { it > 0 } ?: 1.0
        Row(
            Modifier
                .fillMaxWidth()
                .height(74.dp)
                .padding(top = 14.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            weekKm.forEachIndexed { i, km ->
                val current = i == weekKm.size - 1
                val h = if (km > 0) ((km / maxKm * 0.9 * 74).toFloat()).coerceAtLeast(6f) else 6f
                Box(
                    Modifier
                        .weight(1f)
                        .height(h.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (current) ZR.Ember else ZR.S2)
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            weekKm.forEachIndexed { i, km ->
                val current = i == weekKm.size - 1
                Text(
                    fmtKm1(km * 1000),
                    style = zStyle(12.sp, FontWeight.Normal),
                    color = if (current) ZR.Tx else ZR.Mut,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Daftar aktivitas (terbaru di atas)
        if (acts.isEmpty()) {
            Text(
                "Belum ada aktivitas. Tekan Run untuk mulai lari pertamamu.",
                style = zStyle(14.sp, FontWeight.Normal),
                color = ZR.Mut,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp)
            )
        } else {
            acts.forEachIndexed { i, a ->
                ZRRow(showDivider = i < acts.size - 1) {
                    ZRRouteThumb(route = a.route)
                    Column(Modifier.weight(1f)) {
                        Text(
                            a.title,
                            style = zStyle(14.sp, FontWeight.SemiBold),
                            color = ZR.Tx, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${weekdayId(a.startTime)} · ${LocationUtils.formatDuration(a.durationMillis)}",
                            style = zStyle(12.sp, FontWeight.Normal),
                            color = ZR.Mut
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${fmtKm1(a.distanceMeters)} km",
                            style = zStyle(14.sp, FontWeight.SemiBold),
                            color = ZR.Tx
                        )
                        Text(
                            fmtPaceQuote(a.avgPaceSecPerKm),
                            style = zStyle(12.sp, FontWeight.Normal),
                            color = ZR.Mut
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0E, widthDp = 360, heightDp = 780)
@Composable
private fun FeedPreview() {
    Box(Modifier.fillMaxSize().background(ZR.Bg)) {
        FeedContent(
            monthName = "September",
            monthKm = 12.4,
            monthCount = 14,
            monthMs = 7 * 3_600_000L + 40 * 60_000L,
            weekKm = listOf(8.1, 10.5, 6.3, 12.4),
            acts = ZRunSamples.activities
        )
    }
}
