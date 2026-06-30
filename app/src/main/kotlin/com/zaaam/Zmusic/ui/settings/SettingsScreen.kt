package com.zaaam.Zmusic.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.Zmusic.data.SettingsRepository

private val SettingsBgGradient = Brush.verticalGradient(
    colorStops = arrayOf(
        0.0f to Color(0xFF0C0A12), 0.3f to Color(0xFF130F1C),
        0.6f to Color(0xFF1A1226), 1.0f to Color(0xFF0C0A12)
    )
)
// Token di bawah alias ke ZmusicTheme (nilai tidak berubah).
// CardBorder tetap lokal: alpha 0.09 sedikit beda dari GlassBorderColor (0.10).
private val BluePrimary   = com.zaaam.Zmusic.ui.theme.AccentMagenta
private val TextPrimary   = com.zaaam.Zmusic.ui.theme.TextPrimary
private val TextSecondary = com.zaaam.Zmusic.ui.theme.TextMuted
private val CardSurface   = com.zaaam.Zmusic.ui.theme.Bg2
private val CardBorder    = Color.White.copy(alpha = 0.09f)

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigateBack: () -> Unit) {
    val context          = LocalContext.current
    val bubbleEnabled    by viewModel.floatingBubbleEnabled.collectAsState()
    val maxCacheMb       by viewModel.maxCacheMb.collectAsState()
    val storageUsedLabel by viewModel.storageUsedLabel.collectAsState()
    val analyticsEnabled by viewModel.analyticsEnabled.collectAsState()

    var showPermissionDialog by remember { mutableStateOf(false) }
    var showCacheDialog      by remember { mutableStateOf(false) }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Izin Diperlukan") },
            text  = { Text("Floating Bubble membutuhkan izin \"Tampil di atas aplikasi lain\".\n\nKamu akan diarahkan ke pengaturan HP.") },
            confirmButton = { TextButton(onClick = { viewModel.requestOverlayPermission(context); showPermissionDialog = false }) { Text("Buka Pengaturan") } },
            dismissButton = { TextButton(onClick = { showPermissionDialog = false }) { Text("Batal") } }
        )
    }

    if (showCacheDialog) {
        AlertDialog(
            onDismissRequest = { showCacheDialog = false },
            title = { Text("Batas Penyimpanan Unduhan") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Digunakan: $storageUsedLabel", style = MaterialTheme.typography.bodySmall, color = BluePrimary)
                    Spacer(Modifier.height(8.dp))
                    SettingsRepository.CACHE_SIZE_OPTIONS.forEach { mb ->
                        val isSelected = maxCacheMb == mb
                        TextButton(onClick = { viewModel.setMaxCacheMb(mb); showCacheDialog = false }, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(if (mb < 1024) "$mb MB" else "${mb / 1024} GB", color = if (isSelected) BluePrimary else TextPrimary, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                                if (isSelected) Icon(Icons.Default.Check, null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCacheDialog = false }) { Text("Tutup") } }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(SettingsBgGradient)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Kembali", tint = Color.White) }
                Icon(Icons.Default.Settings, null, tint = BluePrimary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Pengaturan", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Kustomisasi pengalaman Zmusic", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f))
                }
            }

            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(8.dp))

                SettingsSectionHeader("Pemutaran")

                SettingsToggleItem(
                    icon = Icons.Default.BubbleChart, iconTint = BluePrimary,
                    title = "Floating Bubble", subtitle = "Kontrol pemutar mengambang saat aplikasi di-minimize",
                    checked = bubbleEnabled,
                    onCheckedChange = { want ->
                        if (want && !viewModel.hasOverlayPermission(context)) showPermissionDialog = true
                        else viewModel.setFloatingBubble(want, context)
                    }
                )

                if (bubbleEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BluePrimary.copy(alpha = 0.08f)).border(1.dp, BluePrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, null, tint = BluePrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Bubble akan muncul saat lagu diputar dan aplikasi di-minimize. Tap icon untuk memperluas kontrol.", style = MaterialTheme.typography.bodySmall, color = BluePrimary.copy(alpha = 0.8f))
                    }
                }

                Spacer(Modifier.height(20.dp))
                SettingsSectionHeader("Penyimpanan")

                // Storage info card
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardSurface).border(1.dp, CardBorder, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(BluePrimary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Storage, null, tint = BluePrimary, modifier = Modifier.size(22.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Penggunaan Penyimpanan", style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(2.dp))
                        Text("Digunakan: $storageUsedLabel  ·  Batas: ${if (maxCacheMb < 1024) "$maxCacheMb MB" else "${maxCacheMb / 1024} GB"}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.45f))
                    }
                }

                Spacer(Modifier.height(8.dp))

                SettingsClickItem(
                    icon = Icons.Default.FolderOpen, iconTint = Color(0xFF82B1FF),
                    title = "Batas Unduhan",
                    subtitle = "Maks ${if (maxCacheMb < 1024) "$maxCacheMb MB" else "${maxCacheMb / 1024} GB"} untuk lagu yang diunduh",
                    onClick = { viewModel.refreshStorageInfo(); showCacheDialog = true }
                )

                Spacer(Modifier.height(20.dp))
                SettingsSectionHeader("Privasi")

                SettingsToggleItem(
                    icon = Icons.Default.Analytics, iconTint = BluePrimary,
                    title = "Kirim statistik anonim",
                    subtitle = "Bantu pengembang lihat jumlah pengguna aktif. Nggak ada data pribadi yang dikirim.",
                    checked = analyticsEnabled,
                    onCheckedChange = { viewModel.setAnalyticsEnabled(it) }
                )

                Spacer(Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Zmusic v${com.zaaam.Zmusic.BuildConfig.VERSION_NAME} · by Zaaam", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.25f))
                }
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}

@Composable private fun SettingsSectionHeader(title: String) {
    Text(text = title.uppercase(), style = MaterialTheme.typography.labelSmall, color = BluePrimary.copy(alpha = 0.7f), letterSpacing = 1.5.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp))
}

@Composable private fun SettingsToggleItem(icon: ImageVector, iconTint: Color, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.04f)).border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(iconTint, iconTint.copy(red = (iconTint.red * 0.7f), green = (iconTint.green * 0.55f), blue = minOf(1f, iconTint.blue * 1.3f + 0.15f))))), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.45f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.primary, uncheckedThumbColor = Color.White.copy(alpha = 0.6f), uncheckedTrackColor = Color.White.copy(alpha = 0.15f)))
    }
}

@Composable private fun SettingsClickItem(icon: ImageVector, iconTint: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.04f)).border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(iconTint, iconTint.copy(red = (iconTint.red * 0.7f), green = (iconTint.green * 0.55f), blue = minOf(1f, iconTint.blue * 1.3f + 0.15f))))), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.45f))
        }
        Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
    }
}
