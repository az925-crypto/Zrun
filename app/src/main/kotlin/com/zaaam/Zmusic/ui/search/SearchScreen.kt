package com.zaaam.Zmusic.ui.search

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.model.entity.SearchHistoryEntity
import com.zaaam.Zmusic.ui.components.SongItem

// ── Design tokens — alias ke ZmusicTheme (sumber kebenaran tunggal) ────────────
// Nilai TIDAK berubah; sebelumnya hardcode duplikat dari token tema.
private val ScreenBg      = com.zaaam.Zmusic.ui.theme.Bg0
private val CardSurface   = com.zaaam.Zmusic.ui.theme.Bg2
private val CardBorder    = com.zaaam.Zmusic.ui.theme.GlassBorderColor
private val TextPrimary   = com.zaaam.Zmusic.ui.theme.TextPrimary
private val TextSecondary = com.zaaam.Zmusic.ui.theme.TextMuted
private val BluePrimary   = com.zaaam.Zmusic.ui.theme.AccentMagenta

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onSongClick: (Song, List<Song>) -> Unit,
    onSongLongClick: (Song) -> Unit
) {
    val state   by viewModel.state.collectAsState()
    val query   by viewModel.query.collectAsState()
    val history by viewModel.searchHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {

        // ── Header ─────────────────────────────────────────────────────────
        Column(
            modifier = Modifier.padding(
                start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp
            )
        ) {
            Text(
                text          = "Cari",
                fontSize      = 24.sp,
                fontWeight    = FontWeight.Bold,
                color         = TextPrimary,
                letterSpacing = (-0.3).sp
            )
        }

        // ── Glass Search Bar ───────────────────────────────────────────────
        BasicTextField(
            value          = query,
            onValueChange  = { viewModel.onQueryChange(it) },
            singleLine     = true,
            textStyle      = TextStyle(color = TextPrimary, fontSize = 14.sp),
            cursorBrush    = SolidColor(BluePrimary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.search(query) }),
            modifier       = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            decorationBox  = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardSurface)
                        .border(0.8.dp, BluePrimary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search, null,
                        tint     = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                "Judul, artis, mood...",
                                fontSize = 14.sp,
                                color    = TextSecondary.copy(alpha = 0.55f)
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick  = { viewModel.clearSearch() },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear, "Bersihkan",
                                tint     = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        )

        // ── State Content ──────────────────────────────────────────────────
        // Crossfade: perpindahan loading → hasil/empty/error nggak lagi nyentak.
        Crossfade(
            targetState   = state,
            animationSpec = tween(250),
            modifier      = Modifier.weight(1f).fillMaxWidth(),
            label         = "searchState"
        ) { s ->
            when (s) {

            is SearchState.Idle -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (history.isNotEmpty()) {
                        SearchHistorySection(
                            history     = history,
                            onItemClick = { viewModel.searchFromHistory(it.query) },
                            onClearAll  = { viewModel.clearAllHistory() }
                        )
                    }
                    // ── Grid "Jelajahi" (gaya mockup) — tap = cari kategori ─
                    JelajahiGrid(onCategoryClick = { keyword -> viewModel.search(keyword) })
                }
            }

            is SearchState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color            = BluePrimary,
                        strokeWidth      = 2.dp,
                        modifier         = Modifier.size(36.dp)
                    )
                }
            }

            is SearchState.Success -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Section label
                    Text(
                        text          = "HASIL PENCARIAN",
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                        color         = TextSecondary,
                        modifier      = Modifier.padding(start = 20.dp, bottom = 6.dp)
                    )
                    LazyColumn {
                        items(s.results, key = { it.id }) { song ->
                            SongItem(
                                song        = song,
                                onClick     = { onSongClick(song, s.results) },
                                onLongClick = { onSongLongClick(song) },
                                modifier    = Modifier.animateItem()
                            )
                        }
                    }
                }
            }

            is SearchState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    com.zaaam.Zmusic.ui.components.StateDisplay(
                        icon    = Icons.Default.Search,
                        title   = "Tidak ada hasil",
                        message = "Coba kata kunci lain, atau cek ejaan judul lagunya."
                    )
                }
            }

            is SearchState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    com.zaaam.Zmusic.ui.components.StateDisplay(
                        icon        = Icons.Default.Warning,
                        title       = "Pencarian gagal",
                        message     = "Koneksi terputus saat memuat. Cek internetmu lalu coba lagi.",
                        isError     = true,
                        actionLabel = "Coba lagi",
                        onAction    = { viewModel.search(query) }
                    )
                }
            }
            }
        }
    }
}

// ── Grid Jelajahi gaya mockup ─────────────────────────────────────────────────

@Composable
private fun JelajahiGrid(onCategoryClick: (String) -> Unit) {
    data class Cat(val label: String, val query: String, val bg: Color, val blob: List<Color>)
    val cats = listOf(
        Cat("Galau",    "lagu galau indonesia",  Color(0xFF3A1F3D), listOf(Color(0xFFFF5F6D), Color(0xFFFFC371))),
        Cat("Fokus",    "musik fokus belajar",   Color(0xFF1F2B3D), listOf(Color(0xFF2CCCFF), Color(0xFFB66BFF))),
        Cat("Indo Pop", "lagu pop indonesia",    Color(0xFF3D2E1F), listOf(Color(0xFFF7B733), Color(0xFFFC4A1A))),
        Cat("Santai",   "lagu santai akustik",   Color(0xFF1F3D2E), listOf(Color(0xFF43E97B), Color(0xFF38F9D7)))
    )
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text       = "Jelajahi",
            fontSize   = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary,
            modifier   = Modifier.padding(vertical = 12.dp)
        )
        cats.chunked(2).forEach { rowCats ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCats.forEach { cat ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(92.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(cat.bg)
                            .clickable { onCategoryClick(cat.query) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .align(Alignment.BottomEnd)
                                .offset(x = 18.dp, y = 18.dp)
                                .rotate(25f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(cat.blob))
                        )
                        Text(
                            text       = cat.label,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextPrimary,
                            modifier   = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 14.dp, bottom = 12.dp)
                        )
                    }
                }
                if (rowCats.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ── Riwayat Pencarian — chip style ────────────────────────────────────────────

@Composable
private fun SearchHistorySection(
    history: List<SearchHistoryEntity>,
    onItemClick: (SearchHistoryEntity) -> Unit,
    onClearAll: () -> Unit
) {
    Column {
        // Header section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text          = "TERAKHIR DICARI",
                fontSize      = 10.sp,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                color         = TextSecondary,
                modifier      = Modifier.weight(1f)
            )
            Text(
                text     = "Hapus Semua",
                fontSize = 12.sp,
                color    = BluePrimary.copy(alpha = 0.80f),
                modifier = Modifier
                    .clickable(onClick = onClearAll)
                    .padding(4.dp)
            )
        }

        // Chip scroll horizontal
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier              = Modifier.padding(bottom = 8.dp)
        ) {
            items(history, key = { it.id }) { item ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardSurface)
                        .border(0.8.dp, CardBorder, RoundedCornerShape(20.dp))
                        .clickable { onItemClick(item) }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.History, null,
                        tint     = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text     = item.query,
                        fontSize = 12.sp,
                        color    = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
