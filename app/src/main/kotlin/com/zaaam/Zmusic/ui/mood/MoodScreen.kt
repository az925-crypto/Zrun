package com.zaaam.Zmusic.ui.mood

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.Zmusic.model.Mood
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.ui.components.SongItem
import com.zaaam.Zmusic.ui.theme.MoodGradientColors

// Warna mood terpusat di ui/theme/MoodColors.kt (dipakai juga oleh StatsScreen)
private val moodColors = MoodGradientColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodScreen(
    viewModel: MoodViewModel,
    onSongClick: (Song, List<Song>, String) -> Unit,
    onSongLongClick: (Song) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val selectedMood by viewModel.selectedMood.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Pilih Mood") }) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Mood Grid
            item {
                Text(
                    text = "Hari ini kamu mau dengerin yang gimana?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // FIX #10: Ganti Mood.values() yang deprecated dengan Mood.entries
                    // (Kotlin 1.9+ / akan error di Kotlin 2.x)
                    items(Mood.entries) { mood ->
                        MoodCard(
                            mood = mood,
                            isSelected = selectedMood == mood,
                            onClick = { viewModel.selectMood(mood) }
                        )
                    }
                }
            }

            // Result Section
            when (val s = state) {
                is MoodState.Idle -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Pilih mood di atas untuk mulai\n🎵",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                is MoodState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Text(
                                    "Nyari lagu yang pas...",
                                    modifier = Modifier.padding(top = 12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                is MoodState.Success -> {
                    item {
                        Text(
                            text = "${s.mood.emoji} Lagu ${s.mood.label}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                start = 16.dp, top = 20.dp, bottom = 8.dp
                            )
                        )
                    }
                    items(s.songs, key = { it.id }) { song ->
                        SongItem(
                            song = song,
                            onClick = { onSongClick(song, s.songs, s.mood.name) },
                            onLongClick = { onSongLongClick(song) }
                        )
                    }
                }

                is MoodState.Error -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(s.message, color = MaterialTheme.colorScheme.error)
                            Button(
                                onClick = { viewModel.retry() },
                                modifier = Modifier.padding(top = 12.dp)
                            ) { Text("Coba Lagi") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodCard(
    mood: Mood,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = moodColors[mood] ?: listOf(Color.Gray, Color.DarkGray)

    Card(
        modifier = Modifier
            .height(64.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        ),
        border = if (isSelected) BorderStroke(
            2.dp, MaterialTheme.colorScheme.primary
        ) else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(colors)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(mood.emoji, fontSize = 22.sp)
                Text(
                    text = mood.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
