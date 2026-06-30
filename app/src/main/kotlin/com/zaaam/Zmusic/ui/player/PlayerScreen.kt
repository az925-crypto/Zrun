package com.zaaam.Zmusic.ui.player

import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.BedtimeOff
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
// FIX #15: import toArgb dihapus — tidak dipakai di file ini
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.zaaam.Zmusic.ui.components.AlbumArt
import com.zaaam.Zmusic.ui.components.GradientPlayButton
import com.zaaam.Zmusic.ui.theme.rememberAccentColors
import com.zaaam.Zmusic.util.DownloadState
import com.zaaam.Zmusic.util.NowPlayingCardGenerator
import com.zaaam.Zmusic.util.RepeatMode
import com.zaaam.Zmusic.util.toTimeString
import com.zaaam.Zmusic.util.toTimerString

// Warna default jika Palette gagal — BATCH 5: navy lama diganti plum (Bg1/Bg0)
private val DefaultBgStart = com.zaaam.Zmusic.ui.theme.Bg1
private val DefaultBgEnd   = com.zaaam.Zmusic.ui.theme.Bg0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEqualizer: () -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val shareScope = rememberCoroutineScope()

    val queueList    by viewModel.queueManager.queue.collectAsState()
    val currentIndex by viewModel.queueManager.currentIndex.collectAsState()
    val song          = queueList.getOrNull(currentIndex)

    val isPlaying       by viewModel.isPlaying.collectAsState()
    val progress        by viewModel.progress.collectAsState()

    // ── Aksen dinamis dari cover lagu (menyatukan tombol, slider, glow) ────
    val accent = rememberAccentColors(song?.thumbnailUrl)
    val currentPosition by viewModel.currentPosition.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
    val errorMessage    by viewModel.errorMessage.collectAsState()
    val isShuffled      by viewModel.queueManager.isShuffled.collectAsState()
    val currentSpeed    by viewModel.queueManager.currentSpeed.collectAsState()
    val currentPitch    by viewModel.queueManager.currentPitch.collectAsState()
    val repeatMode      by viewModel.queueManager.repeatMode.collectAsState()

    val lyricsState    by viewModel.lyricsState.collectAsState()
    val showTranslation by viewModel.showTranslation.collectAsState()
    val isTranslating  by viewModel.isTranslating.collectAsState()

    val downloadStates by viewModel.downloadManager.states.collectAsState()
    val downloadState   = song?.let { downloadStates[it.id] ?: viewModel.downloadManager.getState(it.id) }

    // ── Sleep Timer state ─────────────────────────────────────────────────
    val sleepTimerActive    by viewModel.sleepTimerManager.isActive.collectAsState()
    val sleepTimerRemaining by viewModel.sleepTimerManager.remainingMs.collectAsState()

    // ── Bottom sheet visibility ───────────────────────────────────────────
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showQueueSheet      by remember { mutableStateOf(false) }
    var showSpeedPitchSheet by remember { mutableStateOf(false) }
    var showOverflowMenu    by remember { mutableStateOf(false) }
    var showFullLyrics      by remember { mutableStateOf(false) }

    val sleepSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // ── Dynamic Color dari album art (Palette API) ────────────────────────
    var dynamicBgColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(song?.id) {
        dynamicBgColor = null
        song?.let { s ->
            try {
                val request = ImageRequest.Builder(context)
                    .data(s.thumbnailUrl)
                    .allowHardware(false) // Palette butuh software bitmap
                    .build()
                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    bitmap?.let { bmp ->
                        // FIX #16: Palette.generate() CPU-intensive —
                        // pindah ke Dispatchers.Default agar tidak block
                        // main thread (bisa 10-50ms untuk bitmap besar).
                        val extractedColor = withContext(Dispatchers.Default) {
                            val palette = Palette.from(bmp).generate()
                            val swatch = palette.darkVibrantSwatch
                                ?: palette.vibrantSwatch
                                ?: palette.dominantSwatch
                            swatch?.let { sw ->
                                var color = Color(sw.rgb)
                                if (color.luminance() > 0.15f) {
                                    color = color.copy(
                                        red   = color.red   * 0.4f,
                                        green = color.green * 0.4f,
                                        blue  = color.blue  * 0.4f
                                    )
                                }
                                color
                            }
                        }
                        extractedColor?.let { dynamicBgColor = it }
                    }
                }
            } catch (e: Exception) {
                // Ekstraksi warna Palette gagal — fallback ke warna default
                Log.w("ZmusicPlayer", "ekstraksi warna background gagal", e)
            }
        }
    }

    // Animasikan transisi warna background
    val bgTopColor by animateColorAsState(
        targetValue   = dynamicBgColor ?: DefaultBgStart,
        animationSpec = tween(durationMillis = 800),
        label         = "bgTopColor"
    )
    val bgBottomColor by animateColorAsState(
        targetValue   = DefaultBgEnd,
        animationSpec = tween(durationMillis = 800),
        label         = "bgBottomColor"
    )

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // ── Sleep Timer Bottom Sheet ──────────────────────────────────────────
    if (showSleepTimerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSleepTimerSheet = false },
            sheetState       = sleepSheetState,
            containerColor   = Color(0xFF141822)
        ) {
            SleepTimerSheetContent(
                isActive        = sleepTimerActive,
                remainingMs     = sleepTimerRemaining,
                onSelectMinutes = { minutes ->
                    viewModel.startSleepTimer(minutes)
                    showSleepTimerSheet = false
                },
                onCancel = {
                    viewModel.cancelSleepTimer()
                    showSleepTimerSheet = false
                }
            )
        }
    }

    // ── Queue Bottom Sheet ────────────────────────────────────────────────
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            sheetState       = queueSheetState,
            containerColor   = Color(0xFF141822)
        ) {
            QueueSheetContent(
                queue        = queueList,
                currentIndex = currentIndex,
                onJumpTo     = { idx ->
                    viewModel.jumpToQueueIndex(idx)
                    showQueueSheet = false
                },
                onRemove = { idx -> viewModel.removeFromQueue(idx) },
                onMove   = { from, to -> viewModel.moveInQueue(from, to) }
            )
        }
    }

    // ── Speed & Pitch Bottom Sheet (pindahan dari chips di layar utama) ───
    if (showSpeedPitchSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSpeedPitchSheet = false },
            containerColor   = Color(0xFF1E1828)
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                Text("Kecepatan", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                ) {
                    speedOptions.forEach { speed ->
                        FilterChip(
                            selected = currentSpeed == speed,
                            onClick  = { viewModel.setSpeed(speed) },
                            label    = {
                                Text(
                                    text  = "${if (speed == speed.toInt().toFloat()) speed.toInt() else speed}x",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent.primary.copy(alpha = 0.22f),
                                selectedLabelColor     = accent.primary
                            )
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("Pitch", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                ) {
                    pitchOptions.forEachIndexed { idx, pitch ->
                        FilterChip(
                            selected = currentPitch == pitch,
                            onClick  = { viewModel.setPitch(pitch) },
                            label    = {
                                Text(
                                    text  = pitchLabels.getOrElse(idx) { "0" },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent.primary.copy(alpha = 0.22f),
                                selectedLabelColor     = accent.primary
                            )
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }

    // ── Main Content ──────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f  to bgTopColor.copy(alpha = 0.90f),
                        0.38f to bgTopColor.copy(alpha = 0.20f),
                        1.0f  to bgBottomColor
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // ── Top bar gaya mockup: chevron · konteks · menu ⋯ ────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.KeyboardArrowDown, "Kembali",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text  = "DARI ANTREAN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text     = song?.artist ?: "Zmusic",
                            style    = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert, "Menu",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded         = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            containerColor   = Color(0xFF1E1828)
                        ) {
                            DropdownMenuItem(
                                text        = { Text("Antrean") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null) },
                                onClick     = { showOverflowMenu = false; showQueueSheet = true }
                            )
                            DropdownMenuItem(
                                text = { Text(if (sleepTimerActive) "Sleep timer · aktif" else "Sleep timer") },
                                leadingIcon = {
                                    Icon(
                                        if (sleepTimerActive) Icons.Default.Bedtime else Icons.Default.BedtimeOff,
                                        null,
                                        tint = if (sleepTimerActive) accent.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = { showOverflowMenu = false; showSleepTimerSheet = true }
                            )
                            DropdownMenuItem(
                                text        = { Text("Kecepatan & pitch") },
                                leadingIcon = { Icon(Icons.Default.Speed, null) },
                                onClick     = { showOverflowMenu = false; showSpeedPitchSheet = true }
                            )
                            DropdownMenuItem(
                                text        = { Text("Equalizer") },
                                leadingIcon = { Icon(Icons.Default.Equalizer, null) },
                                onClick     = { showOverflowMenu = false; onNavigateToEqualizer() }
                            )
                            if (song != null) {
                                DropdownMenuItem(
                                    text        = { Text("Bagikan kartu") },
                                    leadingIcon = { Icon(Icons.Default.Share, null) },
                                    onClick     = {
                                        showOverflowMenu = false
                                        shareScope.launch {
                                            NowPlayingCardGenerator.shareNowPlayingCard(
                                                context, song.title, song.artist, song.thumbnailUrl
                                            )
                                        }
                                    }
                                )
                            }
                            if (song != null) {
                                when (val ds = downloadState) {
                                    is DownloadState.Downloading -> DropdownMenuItem(
                                        text        = { Text("Mengunduh… ${(ds.progress * 100).toInt()}%") },
                                        leadingIcon = {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = accent.primary
                                            )
                                        },
                                        onClick = { }
                                    )
                                    is DownloadState.Done -> DropdownMenuItem(
                                        text        = { Text("Hapus unduhan") },
                                        leadingIcon = { Icon(Icons.Default.DownloadDone, null, tint = accent.primary) },
                                        onClick     = { showOverflowMenu = false; viewModel.deleteDownload(song) }
                                    )
                                    else -> DropdownMenuItem(
                                        text        = { Text("Unduh lagu") },
                                        leadingIcon = { Icon(Icons.Default.Download, null) },
                                        onClick     = { showOverflowMenu = false; viewModel.downloadSong(song) }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(Modifier.height(28.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(272.dp)
                            .shadow(
                                elevation = 48.dp,
                                shape = RoundedCornerShape(20.dp),
                                spotColor = accent.glow,
                                ambientColor = accent.glow
                            )
                    ) {
                        AlbumArt(
                            thumbnailUrl = song?.thumbnailUrl ?: "",
                            size         = 272.dp,
                            cornerRadius = 20.dp
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    // ── Visualizer (gaya mockup) — bergerak saat lagu diputar ──
                    PlayerVisualizer(accent = accent, animating = isPlaying)

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text      = song?.title ?: "Tidak ada lagu",
                        style     = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        maxLines  = 2,
                        overflow  = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))

                    // ── Nama artis — clickable untuk Play Artist ───────────
                    // Tap nama artis → app fetch semua lagu artis itu dari YouTube
                    // lalu replace queue dan langsung putar.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = song != null
                            ) {
                                song?.artist?.let { onNavigateToArtist(it) }
                            }
                    ) {
                        Text(
                            text      = song?.artist ?: "",
                            style     = MaterialTheme.typography.bodyLarge,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (song != null) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.PersonSearch,
                                contentDescription = "Lihat semua lagu artis",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // ── Sleep Timer countdown badge ────────────────────────
                    if (sleepTimerActive && sleepTimerRemaining != null) {
                        Spacer(Modifier.height(8.dp))
                        SleepTimerBadge(
                            remainingMs = sleepTimerRemaining!!,
                            onCancel    = { viewModel.cancelSleepTimer() }
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    Slider(
                        value         = progress,
                        onValueChange = { viewModel.seekTo(it) },
                        modifier      = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor      = accent.primary,
                            activeTrackColor = accent.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text  = currentPosition.toTimeString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text  = (song?.duration ?: 0L).toTimeString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // ── Main Controls ─────────────────────────────────────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.toggleShuffle() }) {
                            Icon(
                                Icons.Default.Shuffle, "Shuffle",
                                tint = if (isShuffled) accent.primary
                                       else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { viewModel.skipPrevious() }) {
                            Icon(Icons.Default.SkipPrevious, "Previous", modifier = Modifier.size(32.dp))
                        }
                        Box(contentAlignment = Alignment.Center) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(72.dp),
                                    color = accent.primary
                                )
                            } else {
                                GradientPlayButton(
                                    isPlaying = isPlaying,
                                    onClick   = { viewModel.togglePlayPause() },
                                    size      = 72.dp,
                                    accent    = accent
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.skipNext() }) {
                            Icon(Icons.Default.SkipNext, "Next", modifier = Modifier.size(32.dp))
                        }
                        IconButton(onClick = { viewModel.toggleRepeat() }) {
                            Icon(
                                imageVector = when (repeatMode) {
                                    RepeatMode.ONE -> Icons.Default.RepeatOne
                                    else -> Icons.Default.Repeat
                                },
                                contentDescription = "Repeat",
                                tint = when (repeatMode) {
                                    RepeatMode.OFF -> MaterialTheme.colorScheme.onSurface
                                    else -> accent.primary
                                }
                            )
                        }
                    }

                    // ── 10 Sec Skip ───────────────────────────────────────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.seekBackward10() }) {
                            Icon(
                                Icons.Default.FastRewind, "-10s",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text     = "10 detik",
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        IconButton(onClick = { viewModel.seekForward10() }) {
                            Icon(
                                Icons.Default.FastForward, "+10s",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (song != null) {
                        LyricsSection(
                            song                = song,
                            lyricsState         = lyricsState,
                            showTranslation     = showTranslation,
                            isTranslating       = isTranslating,
                            currentPosition     = currentPosition,
                            onLoadLyrics        = { viewModel.loadLyrics(song) },
                            onToggleTranslation = { viewModel.toggleTranslation(song) },
                            onExpand            = {
                                viewModel.loadLyrics(song)
                                showFullLyrics = true
                            }
                        )
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        // ── Fullscreen Lyrics (gaya mockup 02) ─────────────────────────────
        if (showFullLyrics && song != null) {
            FullLyricsOverlay(
                song            = song,
                lyricsState     = lyricsState,
                currentPosition = currentPosition,
                accent          = accent,
                onDismiss       = { showFullLyrics = false }
            )
        }
    }
}

// ── Visualizer 7 batang gaya mockup ──────────────────────────────────────

@Composable
private fun PlayerVisualizer(
    accent: com.zaaam.Zmusic.ui.theme.AccentColors,
    animating: Boolean
) {
    val transition = rememberInfiniteTransition(label = "playerViz")
    val delays  = listOf(0, 150, 300, 100, 250, 400, 50)
    val heights = listOf(0.40f, 0.80f, 0.55f, 1f, 0.65f, 0.35f, 0.75f)

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment     = Alignment.Bottom,
        modifier              = Modifier.height(28.dp)
    ) {
        delays.forEachIndexed { i, delay ->
            val scale by transition.animateFloat(
                initialValue  = 0.45f,
                targetValue   = 1f,
                animationSpec = infiniteRepeatable(
                    animation  = tween(520, delayMillis = delay, easing = LinearEasing),
                    repeatMode = AnimRepeatMode.Reverse
                ),
                label = "bar$i"
            )
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight(heights[i])
                    .graphicsLayer {
                        scaleY = if (animating) scale else 0.45f
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                    }
                    .background(
                        Brush.verticalGradient(listOf(accent.secondary, accent.primary)),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

// ── Sleep Timer Badge — ditampilkan di bawah judul lagu ──────────────────

@Composable
private fun SleepTimerBadge(remainingMs: Long, onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Bedtime, null,
            tint     = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text  = "Berhenti dalam ${remainingMs.toTimerString()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Default.Close, "Batal",
            tint     = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier
                .size(14.dp)
                .clickable { onCancel() }
        )
    }
}
