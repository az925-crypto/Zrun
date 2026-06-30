package com.zaaam.Zmusic.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaaam.Zmusic.model.Song

// ── Background gradient — BATCH 5: navy tema lama diganti token Dark Plum ──
private val HomeBgGradient = com.zaaam.Zmusic.ui.theme.PageBgPlum

// Catatan refactor (pure extraction, nol perubahan perilaku):
// - Callback navigasi dibundel di HomeNavActions.kt
// - HomeContentLayout + section/kartu pindah ke HomeSections.kt

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSongClick: (Song, List<Song>) -> Unit,
    onSongLongClick: (Song) -> Unit,
    navActions: HomeNavActions = HomeNavActions()
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeBgGradient)
    ) {
        when (val s = state) {
            is HomeState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text  = "Memuat musik...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is HomeState.Success -> {
                HomeContentLayout(
                    content         = s.content,
                    onSongClick     = onSongClick,
                    onSongLongClick = onSongLongClick,
                    onRefresh       = { viewModel.loadDiscovery() },
                    onLoadMore      = { viewModel.loadMore() },
                    navActions      = navActions
                )
            }
            is HomeState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text  = s.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadDiscovery() }) { Text("Coba Lagi") }
                    }
                }
            }
        }
    }
}
