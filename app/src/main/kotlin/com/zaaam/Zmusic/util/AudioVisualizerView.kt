package com.zaaam.Zmusic.util

import android.media.audiofx.Visualizer
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * AudioVisualizerView — Real-time audio visualizer menggunakan Android Visualizer API.
 *
 * Menampilkan bar equalizer yang bergerak sesuai audio yang sedang diputar.
 * Otomatis fallback ke animasi idle jika Visualizer tidak tersedia / tidak ada izin.
 *
 * @param audioSessionId  Session ID dari ExoPlayer (via AudioSessionHolder)
 * @param isPlaying       True jika musik sedang diputar
 * @param barCount        Jumlah bar visualizer (default 32)
 * @param height          Tinggi komponen
 * @param activeColor     Warna bar saat aktif
 * @param idleColor       Warna bar saat idle
 */
@Composable
fun AudioVisualizerView(
    audioSessionId: Int,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 32,
    height: Dp = 64.dp,
    activeColor: Color = Color(0xFF7C4DFF),
    idleColor: Color = Color(0xFF7C4DFF).copy(alpha = 0.3f)
) {
    var waveform by remember { mutableStateOf(FloatArray(barCount) { 0f }) }
    var visualizer by remember { mutableStateOf<Visualizer?>(null) }
    var hasVisualizer by remember { mutableStateOf(false) }

    // Setup Visualizer
    DisposableEffect(audioSessionId) {
        try {
            val vis = Visualizer(audioSessionId).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer, bytes: ByteArray, sr: Int) {
                        // Downsample bytes ke barCount bars
                        val step = bytes.size / barCount
                        val newWave = FloatArray(barCount)
                        for (i in 0 until barCount) {
                            val start = i * step
                            val end = minOf(start + step, bytes.size)
                            var sum = 0f
                            for (j in start until end) {
                                sum += (bytes[j].toInt() and 0xFF) / 255f
                            }
                            newWave[i] = (sum / (end - start)).coerceIn(0f, 1f)
                        }
                        waveform = newWave
                    }
                    override fun onFftDataCapture(v: Visualizer, fft: ByteArray, sr: Int) {}
                }, Visualizer.getMaxCaptureRate() / 2, true, false)
                enabled = true
            }
            visualizer = vis
            hasVisualizer = true
        } catch (e: Exception) {
            // Visualizer butuh izin RECORD_AUDIO + audio session valid — kalau gagal,
            // fallback ke animasi fake. Log supaya alasannya kelihatan di logcat.
            Log.w("ZmusicVisualizer", "init Visualizer gagal — pakai fake bars", e)
            hasVisualizer = false
        }

        onDispose {
            visualizer?.enabled = false
            visualizer?.release()
            visualizer = null
        }
    }

    // Idle animation saat tidak ada visualizer atau tidak playing
    LaunchedEffect(isPlaying, hasVisualizer) {
        if (!isPlaying || !hasVisualizer) {
            var tick = 0
            while (true) {
                if (!isPlaying) {
                    // Slow idle pulse
                    val idleWave = FloatArray(barCount) { i ->
                        val phase = (i.toFloat() / barCount + tick * 0.03f) % 1f
                        (0.05f + 0.08f * kotlin.math.sin(phase * 2 * Math.PI.toFloat()).coerceAtLeast(0f))
                    }
                    waveform = idleWave
                }
                tick++
                delay(80)
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val totalWidth  = size.width
        val totalHeight = size.height
        val barWidth    = totalWidth / (barCount * 2f)
        val gap         = barWidth
        val cornerRad   = barWidth / 2f

        for (i in 0 until barCount) {
            val x    = i * (barWidth + gap) + barWidth / 2f
            val barH = (waveform[i] * totalHeight).coerceAtLeast(4f)
            val top  = totalHeight - barH
            val color = if (isPlaying && waveform[i] > 0.05f) activeColor else idleColor

            drawLine(
                brush     = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.9f),
                        color.copy(alpha = 0.4f)
                    ),
                    startY = top,
                    endY   = totalHeight
                ),
                start     = Offset(x, totalHeight),
                end       = Offset(x, top),
                strokeWidth = barWidth,
                cap       = StrokeCap.Round
            )
        }
    }
}

/**
 * Versi simplified tanpa Visualizer API — menggunakan animasi sinusoidal sebagai fallback.
 * Digunakan ketika audioSessionId tidak tersedia atau permission RECORD_AUDIO tidak ada.
 */
@Composable
fun FakeVisualizerView(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 32,
    height: Dp = 64.dp,
    color: Color = Color(0xFF7C4DFF)
) {
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(isPlaying) {
        while (true) {
            if (isPlaying) tick++
            delay(if (isPlaying) 60L else 150L)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val totalWidth  = size.width
        val totalHeight = size.height
        val barWidth    = totalWidth / (barCount * 2f)
        val gap         = barWidth

        for (i in 0 until barCount) {
            val x = i * (barWidth + gap) + barWidth / 2f

            val amplitude = if (isPlaying) {
                val phase = (i.toFloat() / barCount * 4f + tick * 0.15f) % (2 * Math.PI.toFloat())
                val base  = kotlin.math.sin(phase.toDouble()).toFloat()
                val wave2 = kotlin.math.sin((phase * 1.7f).toDouble()).toFloat()
                ((base + wave2 * 0.5f + 1.5f) / 3f).coerceIn(0.05f, 1f)
            } else {
                val phase = (i.toFloat() / barCount * 2f + tick * 0.02f) % (2 * Math.PI.toFloat())
                (0.05f + 0.06f * kotlin.math.sin(phase.toDouble()).toFloat().coerceAtLeast(0f))
            }

            val barH  = (amplitude * totalHeight).coerceAtLeast(4f)
            val top   = totalHeight - barH
            val alpha = if (isPlaying) 0.85f else 0.3f

            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.4f)),
                    startY = top,
                    endY   = totalHeight
                ),
                start       = Offset(x, totalHeight),
                end         = Offset(x, top),
                strokeWidth = barWidth,
                cap         = StrokeCap.Round
            )
        }
    }
}
