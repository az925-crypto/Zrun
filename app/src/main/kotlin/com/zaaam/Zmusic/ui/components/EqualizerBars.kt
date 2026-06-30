package com.zaaam.Zmusic.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zaaam.Zmusic.ui.theme.AccentColors
import com.zaaam.Zmusic.ui.theme.DefaultAccent
import com.zaaam.Zmusic.ui.theme.ZmusicTheme

/**
 * Indikator "sedang diputar" — 3 batang equalizer yang naik-turun.
 * Dipakai di list lagu (pengganti nomor track), kartu antrean, mini player.
 *
 * @param animating false = batang diam rendah (lagu di-pause)
 */
@Composable
fun EqualizerBars(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    accent: AccentColors = DefaultAccent,
    animating: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "eq")

    // Tiga batang dengan fase beda biar gerakannya organik
    val scales = listOf(0, 160, 320).map { delay ->
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(440, delayMillis = delay, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar$delay"
        )
    }

    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        scales.forEach { scale ->
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .graphicsLayer {
                        scaleY = if (animating) scale.value else 0.35f
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                    }
                    .background(accent.primary, RoundedCornerShape(1.5.dp))
            )
        }
    }
}

@Preview
@Composable
private fun EqualizerBarsPreview() {
    ZmusicTheme {
        EqualizerBars()
    }
}
