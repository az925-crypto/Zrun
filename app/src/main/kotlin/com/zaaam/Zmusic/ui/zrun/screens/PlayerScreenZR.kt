package com.zaaam.Zmusic.ui.zrun.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.ui.player.LyricsState
import com.zaaam.Zmusic.ui.player.PlayerViewModel
import com.zaaam.Zmusic.ui.zrun.SongRow
import com.zaaam.Zmusic.ui.zrun.ZR
import com.zaaam.Zmusic.ui.zrun.ZRChip
import com.zaaam.Zmusic.ui.zrun.ZRIconButton
import com.zaaam.Zmusic.ui.zrun.ZRIcons
import com.zaaam.Zmusic.ui.zrun.ZRRow
import com.zaaam.Zmusic.ui.zrun.ZRunSamples
import com.zaaam.Zmusic.ui.zrun.zStyle
import com.zaaam.Zmusic.util.DownloadState
import com.zaaam.Zmusic.util.RepeatMode
import com.zaaam.Zmusic.util.toTimerString

private enum class Sheet { None, Timer, Lyrics, Queue }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreenZR(player: PlayerViewModel, onClose: () -> Unit) {
    val queue by player.queueManager.queue.collectAsState()
    val idx by player.queueManager.currentIndex.collectAsState()
    val song = queue.getOrNull(idx)
    val isPlaying by player.isPlaying.collectAsState()
    val progress by player.progress.collectAsState()
    val position by player.currentPosition.collectAsState()

    var sheet by remember { mutableStateOf(Sheet.None) }

    Box(
        Modifier.fillMaxSize().background(ZR.Bg)
            .statusBarsPadding().padding(top = 10.dp)
    ) {
        if (song == null) {
            Text(
                "Belum ada lagu",
                style = zStyle(14.sp, FontWeight.Normal),
                color = ZR.Mut,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }
        PlayerContent(
            song = song,
            isPlaying = isPlaying,
            progress = progress,
            position = position,
            onClose = onClose,
            onSeek = { player.seekTo(it) },
            onPlayPause = { player.togglePlayPause() },
            onNext = { player.skipNext() },
            onPrev = { player.skipPrevious() },
            onTimer = { sheet = Sheet.Timer },
            onLyrics = { sheet = Sheet.Lyrics },
            onDownload = {
                val st = player.downloadManager.getState(song.id)
                if (st is DownloadState.Done) player.deleteDownload(song)
                else player.downloadSong(song)
            },
            onQueue = { sheet = Sheet.Queue }
        )
    }

    if (sheet != Sheet.None && song != null) {
        ModalBottomSheet(
            onDismissRequest = { sheet = Sheet.None },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = ZR.S1,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
        ) {
            when (sheet) {
                Sheet.Timer -> TimerSheet(player = player, onDone = { sheet = Sheet.None })
                Sheet.Lyrics -> LyricsSheet(player = player, song = song)
                Sheet.Queue -> QueueSheet(player = player)
                Sheet.None -> Unit
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerContent(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    position: Long,
    onClose: () -> Unit,
    onSeek: (Float) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onTimer: () -> Unit,
    onLyrics: () -> Unit,
    onDownload: () -> Unit,
    onQueue: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
    ) {
        // Bar atas: tombol kembali + judul rata tengah
        Box(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Text(
                "Tutup",
                style = zStyle(13.sp, FontWeight.SemiBold),
                color = ZR.Mut,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable(onClick = onClose)
                    .padding(12.dp)
            )
            Text(
                "Sedang diputar",
                style = zStyle(12.sp, FontWeight.Normal),
                color = ZR.Mut,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Cover
        Box(
            Modifier
                .size(236.dp)
                .align(Alignment.CenterHorizontally)
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(34.dp))
                .background(ZR.Violet),
            contentAlignment = Alignment.Center
        ) {
            if (song.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(236.dp).clip(RoundedCornerShape(34.dp))
                )
            } else {
                // Fallback: cincin outline + lingkaran di atas solid Violet
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(44.dp)
                        .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(84.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                )
            }
        }
        Spacer(Modifier.height(22.dp))

        Text(
            song.title,
            style = zStyle(22.sp, FontWeight.Bold),
            color = ZR.Tx, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
        Text(
            song.artist,
            style = zStyle(14.sp, FontWeight.Normal),
            color = ZR.Mut, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )

        // Progress + seek
        Slider(
            value = progress.coerceIn(0f, 1f),
            onValueChange = onSeek,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            thumb = {
                Box(Modifier.size(13.dp).clip(CircleShape).background(Color.White))
            },
            track = { st ->
                val range = st.valueRange.endInclusive - st.valueRange.start
                val frac = if (range > 0) ((st.value - st.valueRange.start) / range) else 0f
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(ZR.S2)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(frac.coerceIn(0f, 1f))
                            .height(5.dp)
                            .background(Color.White)
                    )
                }
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(fmtMs(position), style = zStyle(12.sp, FontWeight.Normal), color = ZR.Mut)
            Text(fmtMs(song.duration), style = zStyle(12.sp, FontWeight.Normal), color = ZR.Mut)
        }

        // Kontrol
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ZRIconButton(icon = ZRIcons.Prev, desc = "Sebelumnya", onClick = onPrev, iconSize = 26.dp)
            Spacer(Modifier.width(30.dp))
            Box(
                Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) ZRIcons.Pause else ZRIcons.Play,
                    contentDescription = if (isPlaying) "Jeda" else "Putar",
                    tint = Color(0xFF14161B),
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.width(30.dp))
            ZRIconButton(icon = ZRIcons.Next, desc = "Berikutnya", onClick = onNext, iconSize = 26.dp)
        }

        // Chips fitur
        ChipsRow(onTimer = onTimer, onLyrics = onLyrics, onDownload = onDownload, onQueue = onQueue)
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipsRow(onTimer: () -> Unit, onLyrics: () -> Unit, onDownload: () -> Unit, onQueue: () -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = 4
    ) {
        listOf(
            "Timer tidur" to onTimer,
            "Lirik" to onLyrics,
            "Unduh" to onDownload,
            "Antrean" to onQueue
        ).forEach { (label, action) ->
            Box(Modifier.padding(horizontal = 4.dp)) {
                ZRChip(label, onClick = action)
            }
        }
    }
}

@Composable
private fun TimerSheet(player: PlayerViewModel, onDone: () -> Unit) {
    val remaining by player.sleepTimerManager.remainingMs.collectAsState()
    val active by player.sleepTimerManager.isActive.collectAsState()
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text("Timer tidur", style = zStyle(15.sp, androidx.compose.ui.text.font.FontWeight.SemiBold), color = ZR.Tx)
        Spacer(Modifier.height(4.dp))
        Text(
            if (active && remaining != null) "Berhenti dalam ${remaining!!.toTimerString()}" else "Musik berhenti otomatis",
            style = zStyle(12.sp, FontWeight.Normal), color = ZR.Mut
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 30, 45, 60).forEach { m ->
                ZRChip("$m mnt", onClick = { player.startSleepTimer(m); onDone() })
            }
        }
        if (active) {
            Spacer(Modifier.height(8.dp))
            ZRChip("Matikan timer", onClick = { player.cancelSleepTimer(); onDone() })
        }
    }
}

@Composable
private fun LyricsSheet(player: PlayerViewModel, song: Song) {
    val state by player.lyricsState.collectAsState()
    val showTr by player.showTranslation.collectAsState()
    val translating by player.isTranslating.collectAsState()
    LaunchedEffect(song.id) { player.loadLyrics(song) }
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Text("Lirik", style = zStyle(15.sp, FontWeight.SemiBold), color = ZR.Tx)
        Spacer(Modifier.height(4.dp))
        Text("${song.title} · ${song.artist}", style = zStyle(12.sp, FontWeight.Normal), color = ZR.Mut)
        Spacer(Modifier.height(12.dp))
        when (val s = state) {
            is LyricsState.Loading -> Text("Memuat lirik…", style = zStyle(13.sp, FontWeight.Normal), color = ZR.Mut)
            is LyricsState.NotFound -> Text("Lirik tidak ditemukan.", style = zStyle(13.sp, FontWeight.Normal), color = ZR.Mut)
            is LyricsState.Error -> Text(s.message, style = zStyle(13.sp, FontWeight.Normal), color = ZR.Mut)
            is LyricsState.Idle -> Text("Memuat lirik…", style = zStyle(13.sp, FontWeight.Normal), color = ZR.Mut)
            is LyricsState.Success -> {
                val body = if (showTr) (s.translated ?: "Terjemahan tidak tersedia") else s.lyrics.plain
                Text(body, style = zStyle(13.sp, FontWeight.Normal), color = ZR.Tx)
                Spacer(Modifier.height(12.dp))
                ZRChip(
                    if (translating) "Menerjemahkan…" else if (showTr) "Lihat asli" else "Terjemahkan",
                    onClick = { player.toggleTranslation(song) }
                )
            }
        }
    }
}

@Composable
private fun QueueSheet(player: PlayerViewModel) {
    val context = LocalContext.current
    val queue by player.queueManager.queue.collectAsState()
    val idx by player.queueManager.currentIndex.collectAsState()
    val shuffled by player.queueManager.isShuffled.collectAsState()
    val repeat by player.queueManager.repeatMode.collectAsState()
    val speed by player.queueManager.currentSpeed.collectAsState()
    val pitch by player.queueManager.currentPitch.collectAsState()

    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp).padding(horizontal = 20.dp)) {
        item {
            Text("Antrean", style = zStyle(15.sp, FontWeight.SemiBold), color = ZR.Tx)
        }
        item {
            ZRRow {
                Text("Acak", style = zStyle(14.sp, FontWeight.SemiBold), color = ZR.Tx, modifier = Modifier.weight(1f))
                Text(
                    if (shuffled) "Aktif" else "Nonaktif",
                    style = zStyle(13.sp, FontWeight.SemiBold),
                    color = if (shuffled) ZR.Ember else ZR.Mut,
                    modifier = Modifier.clickable { player.toggleShuffle() }.padding(8.dp)
                )
            }
        }
        item {
            ZRRow {
                Text("Ulangi", style = zStyle(14.sp, FontWeight.SemiBold), color = ZR.Tx, modifier = Modifier.weight(1f))
                val label = when (repeat) {
                    RepeatMode.OFF -> "Mati"
                    RepeatMode.ALL -> "Semua"
                    RepeatMode.ONE -> "Satu"
                }
                Text(
                    label,
                    style = zStyle(13.sp, FontWeight.SemiBold),
                    color = if (repeat != RepeatMode.OFF) ZR.Ember else ZR.Mut,
                    modifier = Modifier.clickable { player.toggleRepeat() }.padding(8.dp)
                )
            }
        }
        item {
            Text("Kecepatan", style = zStyle(15.sp, FontWeight.SemiBold), color = ZR.Tx,
                modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
        }
        item {
            SpeedRow(
                options = listOf(0.75f, 1f, 1.25f, 1.5f, 2f),
                current = speed,
                format = { if (it == 1f) "Normal" else "${it}×".replace('.', ',') },
                onPick = { player.setSpeed(it) }
            )
        }
        item {
            Text("Nada", style = zStyle(15.sp, FontWeight.SemiBold), color = ZR.Tx,
                modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
        }
        item {
            SpeedRow(
                options = listOf(0.75f, 1f, 1.25f),
                current = pitch,
                format = { if (it == 1f) "Normal" else "${it}×".replace('.', ',') },
                onPick = { player.setPitch(it) }
            )
        }
        item {
            ZRChip(
                "Buka equalizer",
                modifier = Modifier.padding(top = 14.dp),
                onClick = { player.openSystemEqualizer(context) }
            )
        }
        item {
            Text("Daftar putar", style = zStyle(15.sp, FontWeight.SemiBold), color = ZR.Tx,
                modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
        }
        itemsIndexed(queue, key = { _, s -> s.id }) { i, s ->
            SongRow(
                song = s,
                isPlaying = i == idx,
                onClick = { player.jumpToQueueIndex(i) },
                onLongClick = { player.removeFromQueue(i) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpeedRow(
    options: List<Float>,
    current: Float,
    format: (Float) -> String,
    onPick: (Float) -> Unit
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { v ->
            val sel = v == current
            Box(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (sel) ZR.Ember else ZR.S2)
                    .clickable { onPick(v) }
                    .padding(horizontal = 13.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    format(v),
                    style = zStyle(12.sp, FontWeight.Medium),
                    color = Color.White
                )
            }
        }
    }
}

private fun fmtMs(ms: Long): String {
    if (ms <= 0) return "0:00"
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0E, widthDp = 360, heightDp = 780)
@Composable
private fun PlayerPreview() {
    Box(Modifier.fillMaxSize().background(ZR.Bg)) {
        PlayerContent(
            song = ZRunSamples.song,
            isPlaying = true,
            progress = 0.35f,
            position = 74_000L,
            onClose = {}, onSeek = {}, onPlayPause = {}, onNext = {}, onPrev = {},
            onTimer = {}, onLyrics = {}, onDownload = {}, onQueue = {}
        )
    }
}
