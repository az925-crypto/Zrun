package com.zaaam.Zmusic.ui.zrun.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.model.entity.ActivityEntity
import com.zaaam.Zmusic.model.entity.PlaylistEntity
import com.zaaam.Zmusic.ui.home.HomeState
import com.zaaam.Zmusic.ui.home.HomeViewModel
import com.zaaam.Zmusic.ui.library.LibraryViewModel
import com.zaaam.Zmusic.ui.player.PlayerViewModel
import com.zaaam.Zmusic.ui.tracking.ActivitiesViewModel
import com.zaaam.Zmusic.ui.zrun.EmberButton
import com.zaaam.Zmusic.ui.zrun.SectionHeader
import com.zaaam.Zmusic.ui.zrun.ZBadge
import com.zaaam.Zmusic.ui.zrun.ZR
import com.zaaam.Zmusic.ui.zrun.ZRArt
import com.zaaam.Zmusic.ui.zrun.ZRBigNumber
import com.zaaam.Zmusic.ui.zrun.ZREqBars
import com.zaaam.Zmusic.ui.zrun.ZRIcons
import com.zaaam.Zmusic.ui.zrun.ZRRow
import com.zaaam.Zmusic.ui.zrun.ZRRouteThumb
import com.zaaam.Zmusic.ui.zrun.ZRunSamples
import com.zaaam.Zmusic.ui.zrun.dashedOutline
import com.zaaam.Zmusic.ui.zrun.fmtInt
import com.zaaam.Zmusic.ui.zrun.fmtKm1
import com.zaaam.Zmusic.ui.zrun.fmtPaceQuote
import com.zaaam.Zmusic.ui.zrun.greetingNow
import com.zaaam.Zmusic.ui.zrun.playlistSongCount
import com.zaaam.Zmusic.ui.zrun.startOfWeekMs
import com.zaaam.Zmusic.ui.zrun.weekdayId
import com.zaaam.Zmusic.ui.zrun.zBadges
import com.zaaam.Zmusic.ui.zrun.zStyle

private const val WEEK_TARGET_KM = 25.0
private val DAY_LABELS = listOf("S", "S", "R", "K", "J", "S", "M")

/** Rencana pemula: (selisih hari dari hari ini, jarak km). */
private val PLAN = listOf(0 to "2", 2 to "3", 4 to "3")

@Composable
fun DashboardScreen(
    player: PlayerViewModel,
    onStartRun: () -> Unit,
    onOpenMusic: () -> Unit,
    onOpenPlayer: () -> Unit,
    activitiesVm: ActivitiesViewModel = hiltViewModel(),
    libVm: LibraryViewModel = hiltViewModel(),
    homeVm: HomeViewModel = hiltViewModel()
) {
    val activities by activitiesVm.activities.collectAsState()
    val playlists by libVm.playlists.collectAsState()
    val queue by player.queueManager.queue.collectAsState()
    val idx by player.queueManager.currentIndex.collectAsState()
    val song = queue.getOrNull(idx)
    val isPlaying by player.isPlaying.collectAsState()

    val now = System.currentTimeMillis()
    val weekStart = startOfWeekMs(now)
    val weekMeters = activities.filter { it.startTime >= weekStart }.sumOf { it.distanceMeters }
    val dayKm = (0..6).map { d ->
        val s = weekStart + d * 86_400_000L
        activities.filter { it.startTime in s until s + 86_400_000L }.sumOf { it.distanceMeters } / 1000.0
    }
    val todayIdx = ((now - weekStart) / 86_400_000L).toInt().coerceIn(0, 6)

    if (activities.isEmpty()) {
        val homeState by homeVm.state.collectAsState()
        LaunchedEffect(Unit) {
            if (homeState !is HomeState.Success) homeVm.loadDiscovery()
        }
        val running = (homeState as? HomeState.Success)?.content
            ?.trendingSongs?.ifEmpty { (homeState as HomeState.Success).content.allSongs }
            ?: emptyList()
        DashboardEmpty(
            greeting = greetingNow(),
            todayIdx = todayIdx,
            badges = zBadges(activities),
            trending = running.take(4),
            hasTrending = homeState is HomeState.Success,
            onStartRun = onStartRun,
            onPlay = { s -> player.playSong(s, running, autoShuffle = false) }
        )
        return
    }

    DashboardContent(
        greeting = greetingNow(),
        weekKm = weekMeters / 1000.0,
        dayKm = dayKm,
        todayIdx = todayIdx,
        song = song,
        isPlaying = isPlaying,
        playlists = playlists,
        playlistCount = { id -> playlistSongCount(id, player.musicRepository) },
        last = activities.firstOrNull(),
        onStartRun = onStartRun,
        onOpenMusic = onOpenMusic,
        onOpenPlayer = onOpenPlayer
    )
}

@Composable
private fun DashboardEmpty(
    greeting: String,
    todayIdx: Int,
    badges: List<ZBadge>,
    trending: List<Song>,
    hasTrending: Boolean,
    onStartRun: () -> Unit,
    onPlay: (Song) -> Unit
) {
    val nearest = badges.firstOrNull { !it.open }
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
        Row(
            Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(greeting, style = zStyle(12.sp, FontWeight.Normal), color = ZR.Mut)
                Text("Pelari", style = zStyle(26.sp, FontWeight.Bold, (-0.52).sp), color = ZR.Tx)
            }
            Box(Modifier.size(38.dp).clip(CircleShape).background(ZR.Mint))
        }

        ZRBigNumber(fmtKm1(0.0), "km")
        Spacer(Modifier.height(6.dp))
        Text(
            "Target ${fmtInt(WEEK_TARGET_KM.toLong())} km · rencana pemula: 3 lari santai",
            style = zStyle(12.sp, FontWeight.Normal),
            color = ZR.Mut
        )
        PlanStrip(todayIdx = todayIdx)

        EmberButton(
            text = "Mulai lari",
            icon = ZRIcons.Flame,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            onClick = onStartRun
        )

        if (nearest != null) {
            SectionHeader("Lencana terdekat")
            ZRRow {
                Box(
                    Modifier
                        .size(52.dp)
                        .dashedOutline(ZR.Ember, radius = 26.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(ZRIcons.Flame, contentDescription = null, tint = ZR.Ember, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        nearest.name,
                        style = zStyle(14.sp, FontWeight.SemiBold),
                        color = ZR.Tx, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        nearest.syarat,
                        style = zStyle(12.sp, FontWeight.Normal),
                        color = ZR.Mut
                    )
                }
                Text(
                    nearest.progress,
                    style = zStyle(14.sp, FontWeight.SemiBold),
                    color = ZR.Mut
                )
            }
        }

        SectionHeader("Soundtrack lari")
        if (hasTrending && trending.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(trending, key = { it.id }) { song ->
                    Column(
                        Modifier
                            .width(64.dp)
                            .clickable { onPlay(song) }
                    ) {
                        ZRArt(seed = song.id, thumbnailUrl = song.thumbnailUrl, size = 64.dp, radius = 16.dp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            song.title,
                            style = zStyle(12.sp, FontWeight.SemiBold),
                            color = ZR.Tx, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(ZR.S2)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanStrip(todayIdx: Int) {
    val planByDay = PLAN.mapNotNull { (off, km) ->
        val d = todayIdx + off
        if (d <= 6) d to km else null
    }.toMap()
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (0..6).forEach { i ->
            val planKm = planByDay[i]
            val isToday = i == todayIdx
            Column(
                Modifier.weight(1f).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Arrangement.Bottom
            ) {
                when {
                    planKm != null && isToday -> Box(
                        Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.5.dp, ZR.Ember, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(planKm, style = zStyle(10.sp, FontWeight.SemiBold), color = ZR.Ember)
                    }
                    planKm != null -> Box(
                        Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .dashedOutline(ZR.Ember, radius = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(planKm, style = zStyle(10.sp, FontWeight.SemiBold), color = ZR.Ember)
                    }
                    else -> Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ZR.S2)
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    DAY_LABELS[i],
                    style = zStyle(11.sp, if (isToday) FontWeight.Bold else FontWeight.Normal),
                    color = if (isToday) ZR.Tx else ZR.Mut
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    greeting: String,
    weekKm: Double,
    dayKm: List<Double>,
    todayIdx: Int,
    song: Song?,
    isPlaying: Boolean,
    playlists: List<PlaylistEntity>,
    playlistCount: @Composable (Long) -> Int,
    last: ActivityEntity?,
    onStartRun: () -> Unit,
    onOpenMusic: () -> Unit,
    onOpenPlayer: () -> Unit
) {
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
        // 1. Header
        Row(
            Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(greeting, style = zStyle(12.sp, FontWeight.Normal), color = ZR.Mut)
                Text("Pelari", style = zStyle(26.sp, FontWeight.Bold, (-0.52).sp), color = ZR.Tx)
            }
            Box(Modifier.size(38.dp).clip(CircleShape).background(ZR.Mint))
        }

        // 2. Progres minggu
        val weekMeters = weekKm * 1000.0
        ZRBigNumber(fmtKm1(weekMeters), "km")
        Spacer(Modifier.height(6.dp))
        val sisa = (WEEK_TARGET_KM - weekKm).coerceAtLeast(0.0)
        Text(
            "dari target ${fmtInt(WEEK_TARGET_KM.toLong())} km · sisa ${fmtKm1(sisa * 1000)} km",
            style = zStyle(12.sp, FontWeight.Normal),
            color = ZR.Mut
        )
        WeekStrip(dayKm = dayKm, todayIdx = todayIdx)

        // 3. CTA
        EmberButton(
            text = "Mulai lari",
            icon = ZRIcons.Flame,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            onClick = onStartRun
        )

        // 4. Soundtrack lari
        SectionHeader("Soundtrack lari")
        val pl = playlists.lastOrNull()
        if (pl != null) {
            val n = playlistCount(pl.id)
            ZRRow(onClick = onOpenMusic) {
                ZRArt(seed = pl.name, size = 48.dp, radius = 14.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        pl.name,
                        style = zStyle(14.sp, FontWeight.SemiBold),
                        color = ZR.Tx, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Playlist · ${fmtInt(n.toLong())} lagu",
                        style = zStyle(12.sp, FontWeight.Normal),
                        color = ZR.Mut
                    )
                }
                if (isPlaying) ZREqBars()
            }
        } else if (song != null) {
            ZRRow(onClick = onOpenPlayer) {
                ZRArt(seed = song.id, thumbnailUrl = song.thumbnailUrl, size = 48.dp, radius = 14.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        song.title,
                        style = zStyle(14.sp, FontWeight.SemiBold),
                        color = ZR.Tx, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(song.artist, style = zStyle(12.sp, FontWeight.Normal), color = ZR.Mut,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (isPlaying) ZREqBars()
            }
        } else {
            ZRRow(onClick = onOpenMusic) {
                ZRArt(seed = "kosong", size = 48.dp, radius = 14.dp)
                Column(Modifier.weight(1f)) {
                    Text("Pilih soundtrack lari", style = zStyle(14.sp, FontWeight.SemiBold), color = ZR.Tx)
                    Text("Cari lagu dari YouTube", style = zStyle(12.sp, FontWeight.Normal), color = ZR.Mut)
                }
            }
        }

        // 5. Terakhir
        if (last != null) {
            SectionHeader("Terakhir")
            ZRRow {
                ZRRouteThumb(route = last.route)
                Column(Modifier.weight(1f)) {
                    Text(
                        last.title,
                        style = zStyle(14.sp, FontWeight.SemiBold),
                        color = ZR.Tx, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${weekdayId(last.startTime)} · ${fmtKm1(last.distanceMeters)} km",
                        style = zStyle(12.sp, FontWeight.Normal),
                        color = ZR.Mut
                    )
                }
                Text(
                    fmtPaceQuote(last.avgPaceSecPerKm),
                    style = zStyle(14.sp, FontWeight.SemiBold),
                    color = ZR.Tx
                )
            }
        }
    }
}

@Composable
private fun WeekStrip(dayKm: List<Double>, todayIdx: Int) {
    val maxKm = dayKm.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(top = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dayKm.forEachIndexed { i, km ->
            val isToday = i == todayIdx
            val isFuture = i > todayIdx
            Column(
                Modifier.weight(1f).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Arrangement.Bottom
            ) {
                when {
                    km > 0 -> Box(
                        Modifier
                            .fillMaxWidth()
                            .height(((km / maxKm * 44).toFloat()).coerceAtLeast(6f).dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ZR.Ember)
                    )
                    isToday -> Box(
                        Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.5.dp, ZR.Ember, RoundedCornerShape(8.dp))
                    )
                    else -> Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ZR.S2)
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    DAY_LABELS[i],
                    style = zStyle(
                        11.sp,
                        if (isToday) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isToday || km > 0 && !isFuture) ZR.Tx else ZR.Mut
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0E, widthDp = 360, heightDp = 780)
@Composable
private fun DashboardPreview() {
    Box(Modifier.fillMaxSize().background(ZR.Bg)) {
        DashboardEmpty(
            greeting = "Selamat pagi",
            todayIdx = 2,
            badges = zBadges(emptyList()),
            trending = ZRunSamples.songs,
            hasTrending = true,
            onStartRun = {},
            onPlay = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0E, widthDp = 360, heightDp = 780)
@Composable
private fun DashboardFilledPreview() {
    val acts = ZRunSamples.activities
    val now = System.currentTimeMillis()
    val weekStart = startOfWeekMs(now)
    val dayKm = (0..6).map { d ->
        val s = weekStart + d * 86_400_000L
        acts.filter { it.startTime in s until s + 86_400_000L }.sumOf { it.distanceMeters } / 1000.0
    }
    Box(Modifier.fillMaxSize().background(ZR.Bg)) {
        DashboardContent(
            greeting = "Selamat pagi",
            weekKm = 12.4,
            dayKm = dayKm,
            todayIdx = 2,
            song = ZRunSamples.song,
            isPlaying = true,
            playlists = emptyList(),
            playlistCount = { 0 },
            last = acts.first(),
            onStartRun = {}, onOpenMusic = {}, onOpenPlayer = {}
        )
    }
}
