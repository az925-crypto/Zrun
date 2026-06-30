package com.zaaam.Zmusic.ui.zrun.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.Zmusic.ui.search.SearchState
import com.zaaam.Zmusic.ui.search.SearchViewModel
import com.zaaam.Zmusic.ui.player.PlayerViewModel
import com.zaaam.Zmusic.ui.zrun.SongRow
import com.zaaam.Zmusic.ui.zrun.ZR

@Composable
fun SearchScreenZR(
    player: PlayerViewModel,
    onBack: () -> Unit,
    onOpenPlayer: () -> Unit,
    vm: SearchViewModel = hiltViewModel()
) {
    val query by vm.query.collectAsState()
    val state by vm.state.collectAsState()

    Column(Modifier.fillMaxSize().background(ZR.Bg).statusBarsPadding().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "kembali", tint = ZR.Tx,
                modifier = Modifier.size(26.dp).clickable(onClick = onBack))
            Spacer(Modifier.width(10.dp))
            TextField(
                value = query,
                onValueChange = { vm.onQueryChange(it) },
                placeholder = { Text("Cari lagu, artis…", color = ZR.Mut) },
                singleLine = true,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.search(query) }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = ZR.S1, unfocusedContainerColor = ZR.S1,
                    focusedTextColor = ZR.Tx, unfocusedTextColor = ZR.Tx,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = ZR.Ember2
                )
            )
        }

        when (val s = state) {
            is SearchState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ZR.Ember2)
            }
            is SearchState.Empty -> Center("Nggak ada hasil")
            is SearchState.Idle -> Center("Cari lagu favoritmu buat lari 🎧")
            is SearchState.Error -> Center(s.message)
            is SearchState.Success -> {
                LazyColumn(Modifier.fillMaxSize().padding(top = 12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 40.dp)) {
                    items(s.results, key = { it.id }) { song ->
                        SongRow(song = song, isPlaying = false,
                            onClick = { player.playSong(song, s.results); onOpenPlayer() })
                    }
                }
            }
        }
    }
}

@Composable
private fun Center(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = ZR.Mut, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
