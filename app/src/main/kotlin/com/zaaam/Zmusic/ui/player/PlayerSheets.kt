package com.zaaam.Zmusic.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BedtimeOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zaaam.Zmusic.util.toTimerString

internal val speedOptions = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
internal val pitchOptions = listOf(0.75f, 0.875f, 1f, 1.125f, 1.25f)
internal val pitchLabels  = listOf("-4", "-2", "0", "+2", "+4") // semitone labels
private val sleepTimerOptions = listOf(5, 10, 15, 20, 30, 45, 60)

// ── Sleep Timer Bottom Sheet ──────────────────────────────────────────────

@Composable
internal fun SleepTimerSheetContent(
    isActive: Boolean,
    remainingMs: Long?,
    onSelectMinutes: (Int) -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        // Handle bar
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.2f))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text       = "⏰  Sleep Timer",
            style      = MaterialTheme.typography.titleLarge,
            color      = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text  = "Musik akan otomatis berhenti setelah waktu habis",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        // Tampilkan sisa waktu jika timer aktif
        if (isActive && remainingMs != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text  = "Timer Aktif",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text       = remainingMs.toTimerString(),
                        style      = MaterialTheme.typography.headlineLarge,
                        color      = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = "tersisa",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Text(
            text  = if (isActive) "Ganti durasi:" else "Pilih durasi:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        // Grid pilihan menit
        val chunked = sleepTimerOptions.chunked(4)
        chunked.forEach { row ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { minutes ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isActive) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectMinutes(minutes) }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text       = "$minutes",
                                style      = MaterialTheme.typography.titleMedium,
                                color      = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text  = "menit",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                // Isi sisa kolom jika row tidak penuh
                repeat(4 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (isActive) {
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick  = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.BedtimeOff, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Batalkan Timer", color = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Queue Bottom Sheet ────────────────────────────────────────────────────

@Composable
internal fun QueueSheetContent(
    queue: List<com.zaaam.Zmusic.model.Song>,
    currentIndex: Int,
    onJumpTo: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit = { _, _ -> }
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .align(Alignment.CenterVertically)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text       = "Antrean",
                style      = MaterialTheme.typography.titleMedium,
                color      = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Text(
                text  = "${queue.size} lagu",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        // ── Kartu "Sedang Diputar" (gaya mockup 06) ────────────────────────
        queue.getOrNull(currentIndex)?.let { nowSong ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF3A1F3D), Color(0xFF241A30))
                        )
                    )
                    .border(0.8.dp, Color(0xFFFF4D8D).copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text          = "SEDANG DIPUTAR",
                    style         = MaterialTheme.typography.labelSmall,
                    color         = Color(0xFFFF9D5C)
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    coil.compose.AsyncImage(
                        model              = nowSong.thumbnailUrl,
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(nowSong.title, style = MaterialTheme.typography.titleSmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(nowSong.artist, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    com.zaaam.Zmusic.ui.components.EqualizerBars(height = 20.dp)
                }
            }
        }

        // List antrian
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = (currentIndex - 1).coerceAtLeast(0)
        )

        LazyColumn(
            state          = listState,
            modifier       = Modifier
                .fillMaxWidth()
                .height(480.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(queue, key = { _, song -> song.id }) { index, queueSong ->
                val isCurrentSong = index == currentIndex

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isCurrentSong) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else Color.Transparent
                        )
                        .clickable { onJumpTo(index) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Indikator kiri: equalizer (aktif) / grip drag-reorder
                    Box(
                        modifier         = Modifier.width(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCurrentSong) {
                            com.zaaam.Zmusic.ui.components.EqualizerBars()
                        } else {
                            val dragAccum = remember { floatArrayOf(0f) }
                            val rowHeightPx = with(LocalDensity.current) { 64.dp.toPx() }
                            Icon(
                                Icons.Default.DragHandle, "Geser untuk urut",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .pointerInput(index, queue.size) {
                                        detectVerticalDragGestures(
                                            onDragStart = { dragAccum[0] = 0f },
                                            onVerticalDrag = { change, delta ->
                                                change.consume()
                                                dragAccum[0] += delta
                                                if (dragAccum[0] > rowHeightPx && index < queue.size - 1) {
                                                    onMove(index, index + 1)
                                                    dragAccum[0] = 0f
                                                }
                                                if (dragAccum[0] < -rowHeightPx && index > 0) {
                                                    onMove(index, index - 1)
                                                    dragAccum[0] = 0f
                                                }
                                            }
                                        )
                                    }
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    // Thumbnail
                    coil.compose.AsyncImage(
                        model              = queueSong.thumbnailUrl,
                        contentDescription = null,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(Modifier.width(12.dp))

                    // Info
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = queueSong.title,
                            style      = MaterialTheme.typography.bodyMedium,
                            color      = if (isCurrentSong) MaterialTheme.colorScheme.primary
                                         else MaterialTheme.colorScheme.onSurface,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                            fontWeight = if (isCurrentSong) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(
                            text     = queueSong.artist,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Hapus dari antrian (tidak bisa hapus lagu yang sedang main)
                    if (!isCurrentSong) {
                        IconButton(
                            onClick  = { onRemove(index) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Close, "Hapus dari antrian",
                                tint     = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Spacer(Modifier.width(36.dp))
                    }
                }

                if (index < queue.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 52.dp),
                        color    = Color.White.copy(alpha = 0.05f)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}
