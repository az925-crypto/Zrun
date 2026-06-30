package com.zaaam.Zmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zaaam.Zmusic.ui.theme.AccentColors
import com.zaaam.Zmusic.ui.theme.Bg0
import com.zaaam.Zmusic.ui.theme.DefaultAccent
import com.zaaam.Zmusic.ui.theme.ZmusicTheme

/**
 * Tombol play/pause bulat dengan gradient aksen + glow.
 * Elemen signature redesign — dipakai di NowPlaying, detail playlist,
 * kartu "Lanjut dengerin", dan hasil pencarian.
 *
 * @param accent warna dari [rememberAccentColors] — otomatis ikut cover lagu
 */
@Composable
fun GradientPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    accent: AccentColors = DefaultAccent
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = size / 4,
                shape = CircleShape,
                spotColor = accent.glow,
                ambientColor = accent.glow
            )
            .clip(CircleShape)
            .background(accent.gradient())
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Jeda" else "Putar",
            tint = Bg0,
            modifier = Modifier.size(size * 0.46f)
        )
    }
}

@Preview
@Composable
private fun GradientPlayButtonPreview() {
    ZmusicTheme {
        GradientPlayButton(isPlaying = false, onClick = {})
    }
}
