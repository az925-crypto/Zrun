package com.zaaam.Zmusic.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zaaam.Zmusic.ui.components.GlassCard

// ── Fullscreen Lyrics Overlay (gaya mockup 02) ────────────────────────────

@Composable
internal fun FullLyricsOverlay(
    song: com.zaaam.Zmusic.model.Song,
    lyricsState: LyricsState,
    currentPosition: Long,
    accent: com.zaaam.Zmusic.ui.theme.AccentColors,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Dasar SOLID dulu (biar tidak tembus), baru glow accent di atasnya
            .background(Color(0xFF0C0A12))
            .background(
                Brush.radialGradient(
                    colors = listOf(accent.glow.copy(alpha = 0.22f), Color.Transparent),
                    radius = 1400f
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* blok klik tembus */ }
            .statusBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            // Top bar: chevron · judul/artis · spacer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.KeyboardArrowDown, "Tutup",
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(song.title, style = MaterialTheme.typography.titleSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.size(48.dp))
            }

            when (lyricsState) {
                is LyricsState.Success -> {
                    val lyrics = lyricsState.lyrics
                    if (lyrics.hasSynced) {
                        val activeIndex = lyrics.synced.indexOfLast { it.timeMs <= currentPosition }
                            .coerceAtLeast(0)
                        val listState = rememberLazyListState()
                        LaunchedEffect(activeIndex) {
                            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
                        }
                        LazyColumn(
                            state          = listState,
                            modifier       = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 30.dp),
                            contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            itemsIndexed(lyrics.synced) { index, line ->
                                val isActive = index == activeIndex
                                val isPast   = index < activeIndex
                                if (isActive) {
                                    Text(
                                        text  = line.text.ifBlank { "♪" },
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            brush = Brush.linearGradient(
                                                listOf(accent.secondary, accent.primary)
                                            )
                                        ),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Text(
                                        text  = line.text.ifBlank { "♪" },
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = if (isPast)
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else {
                        // Lirik plain — tampilkan scrollable besar
                        LazyColumn(
                            modifier       = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 30.dp),
                            contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp)
                        ) {
                            item {
                                Text(
                                    text       = lyrics.plain,
                                    style      = MaterialTheme.typography.headlineSmall,
                                    color      = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = MaterialTheme.typography.headlineSmall.lineHeight * 1.4
                                )
                            }
                        }
                    }
                }
                is LyricsState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = accent.primary)
                }
                else -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        "Lirik tidak tersedia untuk lagu ini",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Lyrics Section (tidak berubah) ───────────────────────────────────────

@Composable
internal fun LyricsSection(
    song: com.zaaam.Zmusic.model.Song,
    lyricsState: LyricsState,
    showTranslation: Boolean,
    isTranslating: Boolean,
    currentPosition: Long,
    onLoadLyrics: () -> Unit,
    onToggleTranslation: () -> Unit,
    onExpand: () -> Unit = {}
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.MusicNote, null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text     = "  Lirik · ketuk untuk layar penuh",
                    style    = MaterialTheme.typography.titleSmall,
                    color    = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onExpand() }
                )
                if (lyricsState is LyricsState.Success) {
                    if (isTranslating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(
                            onClick  = onToggleTranslation,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Translate, "Terjemahkan",
                                modifier = Modifier.size(20.dp),
                                tint     = if (showTranslation) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            when (lyricsState) {
                is LyricsState.Idle -> {
                    TextButton(
                        onClick  = onLoadLyrics,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) { Text("Tampilkan Lirik") }
                }
                is LyricsState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(32.dp)
                    )
                }
                is LyricsState.NotFound -> {
                    Text(
                        text      = "Lirik tidak ditemukan untuk lagu ini.",
                        style     = MaterialTheme.typography.bodySmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.fillMaxWidth()
                    )
                }
                is LyricsState.Error -> {
                    Text(
                        text  = lyricsState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                is LyricsState.Success -> {
                    val lyrics = lyricsState.lyrics

                    if (showTranslation && lyricsState.translated != null) {
                        Text(
                            text       = lyricsState.translated,
                            style      = MaterialTheme.typography.bodyMedium,
                            color      = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                        )
                    } else if (lyrics.hasSynced) {
                        val activeIndex = lyrics.synced.indexOfLast { it.timeMs <= currentPosition }
                            .coerceAtLeast(0)
                        val listState   = rememberLazyListState()

                        LaunchedEffect(activeIndex) {
                            if (activeIndex > 0) listState.animateScrollToItem(
                                (activeIndex - 1).coerceAtLeast(0)
                            )
                        }

                        LazyColumn(
                            state          = listState,
                            modifier       = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            itemsIndexed(lyrics.synced) { index, line ->
                                val isActive = index == activeIndex
                                val textColor by animateColorAsState(
                                    targetValue = if (isActive) MaterialTheme.colorScheme.primary
                                                  else MaterialTheme.colorScheme.onSurfaceVariant,
                                    label       = "lyric_color"
                                )
                                Text(
                                    text       = line.text.ifBlank { "♪" },
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color      = textColor,
                                    textAlign  = TextAlign.Center,
                                    modifier   = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text       = lyrics.plain,
                            style      = MaterialTheme.typography.bodyMedium,
                            color      = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                        )
                    }
                }
            }
        }
    }
}
