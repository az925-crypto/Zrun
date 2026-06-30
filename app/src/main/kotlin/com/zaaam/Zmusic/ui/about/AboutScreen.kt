package com.zaaam.Zmusic.ui.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.Zmusic.BuildConfig
import com.zaaam.Zmusic.ui.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    onDevPageClick: (() -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title          = { Text("Tentang Aplikasi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Icon(
                imageVector        = Icons.Default.MusicNote,
                contentDescription = null,
                modifier           = Modifier.size(72.dp),
                tint               = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(
                        color      = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )) { append("Z") }
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("music") }
                },
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Versi diambil dari BuildConfig.VERSION_NAME (sumber tunggal: versionName
            // di build.gradle.kts) — otomatis ikut tiap rilis, tak perlu edit manual.
            Text(
                text  = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text  = "Dibuat oleh Zaaam",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (onDevPageClick != null) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onDevPageClick) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Halaman Pengembang")
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Disclaimer (GlassCard) ─────────────────────────────────────
            SectionCard(title = "Disclaimer") {
                Text(
                    text = "Zmusic adalah aplikasi pemutar musik open-source yang bersifat " +
                            "independen dan TIDAK berafiliasi, didukung, disponsori, atau " +
                            "disetujui oleh YouTube™, Google LLC, Spotify™, Spotify AB, " +
                            "atau entitas lain manapun.\n\n" +
                            "Semua merek dagang, nama layanan, dan logo yang disebutkan " +
                            "adalah milik dari pemiliknya masing-masing.\n\n" +
                            "Aplikasi ini menggunakan NewPipeExtractor sebagai library " +
                            "pihak ketiga untuk mengakses konten yang tersedia secara " +
                            "publik. Pengembang tidak bertanggung jawab atas penggunaan " +
                            "yang melanggar ketentuan layanan pihak ketiga.\n\n" +
                            "Aplikasi ini GRATIS dan tidak diperjualbelikan dalam bentuk apapun.",
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Lisensi (GlassCard) ────────────────────────────────────────
            SectionCard(title = "Lisensi") {
                Text(
                    text       = "GNU General Public License v3.0",
                    style      = MaterialTheme.typography.titleMedium,
                    color      = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Copyright © 2025 Zaaam\n\n" +
                            "Zmusic adalah perangkat lunak bebas: Anda dapat mendistribusikan " +
                            "ulang dan/atau memodifikasinya berdasarkan ketentuan GNU General " +
                            "Public License sebagaimana diterbitkan oleh Free Software " +
                            "Foundation, baik versi 3 dari Lisensi, atau (sesuai pilihan " +
                            "Anda) versi yang lebih baru.\n\n" +
                            "Program ini didistribusikan dengan harapan bahwa program ini " +
                            "akan berguna, tetapi TANPA JAMINAN APAPUN. Lihat GNU General " +
                            "Public License untuk detail lebih lanjut.\n\n" +
                            "Lihat: https://www.gnu.org/licenses/",
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Library Pihak Ketiga (GlassCard) ──────────────────────────
            SectionCard(title = "Library Pihak Ketiga") {
                // FIX #14: "Haze" dihapus dari daftar library karena sudah
                // tidak dipakai di project (semua referensi Haze sudah REMOVED).
                val libraries = listOf(
                    "NewPipeExtractor"   to "GPL-3.0",
                    "ExoPlayer (Media3)" to "Apache-2.0",
                    "Jetpack Compose"    to "Apache-2.0",
                    "Hilt (Dagger)"      to "Apache-2.0",
                    "Room Database"      to "Apache-2.0",
                    "OkHttp"             to "Apache-2.0",
                    "Coil"               to "Apache-2.0",
                    "Kotlin Coroutines"  to "Apache-2.0"
                )
                libraries.forEachIndexed { index, (name, license) ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                    Text(
                        text  = "$name — $license",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text      = "Made with ♪ in Indonesia",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── SectionCard: wrapper GlassCard ────────────────────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text       = title,
                style      = MaterialTheme.typography.titleMedium,
                color      = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
