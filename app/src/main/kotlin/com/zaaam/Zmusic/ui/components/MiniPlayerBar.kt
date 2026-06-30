package com.zaaam.Zmusic.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.ui.theme.GlassBorderColor
import com.zaaam.Zmusic.ui.theme.GlassPlayerBarColor
import com.zaaam.Zmusic.ui.theme.rememberAccentColors
// REMOVED: import dev.chrisbanes.haze.* — Haze dihapus, glass via transparansi + border

private val miniPlayerShape = RoundedCornerShape(14.dp)

/**
 * MiniPlayerBar dengan efek glass murni Compose.
 *
 * Haze (real blur) dihapus karena rememberHazeState tidak bisa di-resolve
 * di environment build ini. Glass effect tetap ada melalui:
 *   • GlassPlayerBarColor — surface semi-transparan (#181C22 @ 80%)
 *   • GlassBorderColor    — border putih tipis (White @ 8%)
 *
 * REMOVED parameter: hazeState: HazeState
 */
@Composable
fun MiniPlayerBar(
    currentSong: Song?,
    isPlaying: Boolean,
    progress: Float,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onBarClick: () -> Unit,
    modifier: Modifier = Modifier
    // REMOVED: hazeState: HazeState
) {
    if (currentSong == null) return

    val accent = rememberAccentColors(currentSong.thumbnailUrl)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .clip(miniPlayerShape),
            // REMOVED: .hazeChild(state = hazeState, style = ...)
        shape          = miniPlayerShape,
        color          = GlassPlayerBarColor,
        border         = BorderStroke(1.dp, GlassBorderColor),
        tonalElevation = 0.dp,
        shadowElevation= 0.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBarClick() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArt(
                    thumbnailUrl = currentSong.thumbnailUrl,
                    size         = 44.dp,
                    cornerRadius = 8.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text     = currentSong.title,
                        style    = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color    = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text     = currentSong.artist,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onPlayPauseClick) {
                    Icon(
                        imageVector        = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier           = Modifier.size(28.dp),
                        tint               = accent.primary
                    )
                }

                IconButton(onClick = onNextClick) {
                    Icon(
                        imageVector        = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier           = Modifier.size(28.dp),
                        tint               = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Progress bar tipis di bawah
            LinearProgressIndicator(
                progress     = { progress },
                modifier     = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)),
                color        = accent.primary,
                trackColor   = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                strokeCap    = StrokeCap.Round
            )

            Spacer(Modifier.height(4.dp))
        }
    }
}
