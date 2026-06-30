package com.zaaam.Zmusic.ui.equalizer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.Zmusic.util.EqPreset

// BATCH 5: palet biru-navy tema lama diganti token Dark Plum (ZmusicTheme)
private val EqBgGradient  = com.zaaam.Zmusic.ui.theme.PageBgPlum
private val BluePrimary   = com.zaaam.Zmusic.ui.theme.AccentMagenta
private val TextPrimary   = com.zaaam.Zmusic.ui.theme.TextPrimary
private val TextSecondary = com.zaaam.Zmusic.ui.theme.TextMuted
private val CardSurface   = com.zaaam.Zmusic.ui.theme.Bg2
private val CardBorder    = Color.White.copy(alpha = 0.09f)

private fun formatFreq(hz: Int): String = if (hz < 1000) "$hz Hz" else "${String.format("%.1f", hz / 1000f).trimEnd('0').trimEnd('.')} kHz"

@Composable
fun EqualizerScreen(
    viewModel: EqualizerViewModel,
    onNavigateBack: () -> Unit
) {
    val isEnabled    by viewModel.isEnabled.collectAsState()
    val bandLevels   by viewModel.bandLevels.collectAsState()
    val bandFreqs    by viewModel.bandFreqs.collectAsState()
    val levelRange   by viewModel.levelRange.collectAsState()
    val numBands     by viewModel.numberOfBands.collectAsState()
    val activePreset by viewModel.activePreset.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(EqBgGradient)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top Bar ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Kembali", tint = Color.White) }
                Icon(Icons.Default.Equalizer, null, tint = BluePrimary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Equalizer", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Sesuaikan suara sesuai seleramu", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                }
                IconButton(onClick = { viewModel.resetToFlat() }) { Icon(Icons.Default.RestartAlt, "Reset", tint = TextSecondary) }
                Switch(
                    checked         = isEnabled,
                    onCheckedChange = { viewModel.setEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor   = Color.White, checkedTrackColor   = BluePrimary,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.6f), uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                    )
                )
            }

            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Spacer(Modifier.height(8.dp))

                // ── EQ Sliders ────────────────────────────────────────────
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CardSurface).border(1.dp, CardBorder, RoundedCornerShape(20.dp)).padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("+${(levelRange.second) / 100} dB", fontSize = 10.sp, color = TextSecondary)
                            Text("0 dB", fontSize = 10.sp, color = TextSecondary)
                            Text("${(levelRange.first) / 100} dB", fontSize = 10.sp, color = TextSecondary)
                        }
                        Spacer(Modifier.height(12.dp))

                        if (bandLevels.size == numBands && numBands > 0) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                repeat(numBands) { band ->
                                    val level = bandLevels.getOrElse(band) { 0 }
                                    EqBandSlider(
                                        band      = band,
                                        level     = level,
                                        minLevel  = levelRange.first,
                                        maxLevel  = levelRange.second,
                                        freqLabel = formatFreq(bandFreqs.getOrElse(band) { 0 }),
                                        enabled   = isEnabled,
                                        onLevelChange = { viewModel.setBandLevel(band, it) }
                                    )
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                                Text("Equalizer tidak tersedia di perangkat ini", color = TextSecondary, textAlign = TextAlign.Center, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Presets ────────────────────────────────────────────────
                Text("PRESET", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BluePrimary.copy(alpha = 0.8f), letterSpacing = 1.5.sp, modifier = Modifier.padding(horizontal = 4.dp))
                Spacer(Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding        = PaddingValues(horizontal = 4.dp)
                ) {
                    items(EqPreset.entries) { preset ->
                        val isActive = activePreset == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isActive) BluePrimary.copy(alpha = 0.18f) else CardSurface)
                                .border(1.dp, if (isActive) BluePrimary.copy(alpha = 0.7f) else CardBorder, RoundedCornerShape(12.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication        = null,
                                    enabled           = isEnabled
                                ) { viewModel.applyPreset(preset) }
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = preset.label,
                                fontSize   = 13.sp,
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                color      = if (isActive) BluePrimary else TextSecondary
                            )
                        }
                    }
                }

                if (!isEnabled) {
                    Spacer(Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.04f)).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                        Text("Aktifkan toggle di atas untuk menggunakan equalizer.", fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                }

                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun EqBandSlider(
    band: Int,
    level: Int,
    minLevel: Int,
    maxLevel: Int,
    freqLabel: String,
    enabled: Boolean,
    onLevelChange: (Int) -> Unit
) {
    val range            = (maxLevel - minLevel).toFloat().coerceAtLeast(1f)
    val normalizedValue  = (level - minLevel).toFloat() / range
    val animatedValue    by animateFloatAsState(targetValue = normalizedValue, animationSpec = tween(150), label = "eqBand$band")

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(48.dp)) {
        Text(
            text       = "${if (level >= 0) "+" else ""}${level / 100}",
            fontSize   = 9.sp,
            color      = if (enabled) BluePrimary else TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(6.dp))

        Box(modifier = Modifier.height(140.dp), contentAlignment = Alignment.Center) {
            Slider(
                value         = normalizedValue,
                onValueChange = { v -> onLevelChange((minLevel + v * range).toInt()) },
                enabled       = enabled,
                modifier      = Modifier
                    .width(140.dp)
                    .height(48.dp)
                    .graphicsLayer { rotationZ = 270f },
                colors = SliderDefaults.colors(
                    thumbColor         = if (enabled) BluePrimary else TextSecondary,
                    activeTrackColor   = if (enabled) BluePrimary else TextSecondary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                )
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(text = freqLabel, fontSize = 9.sp, color = TextSecondary, textAlign = TextAlign.Center, maxLines = 2)
    }
}
