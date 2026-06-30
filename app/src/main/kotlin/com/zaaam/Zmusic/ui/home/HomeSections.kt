package com.zaaam.Zmusic.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.ui.components.GlassCard
import com.zaaam.Zmusic.ui.components.SongItem
import com.zaaam.Zmusic.ui.theme.GradientBaru
import com.zaaam.Zmusic.ui.theme.GradientAccent
import com.zaaam.Zmusic.ui.theme.GradientAccentFull
import com.zaaam.Zmusic.ui.theme.GradientFavorit
import com.zaaam.Zmusic.ui.theme.GradientKoleksi
import com.zaaam.Zmusic.ui.theme.GradientMusik
import com.zaaam.Zmusic.util.toTimeString

// ── Accent colors for rank badges & highlights ────────────────────────────
// Gold/Silver/Bronze TETAP — warna ranking, bukan bagian tema lama
private val AccentGold   = Color(0xFFFFD700)
private val AccentSilver = Color(0xFFB0BEC5)
private val AccentBronze = Color(0xFFCD7F32)
private val CardSurface  = com.zaaam.Zmusic.ui.theme.Bg2
private val CardBorder   = Color.White.copy(alpha = 0.10f)

@Composable
internal fun HomeContentLayout(
    content: HomeContent,
    onSongClick: (Song, List<Song>) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    navActions: HomeNavActions
) {
    var showWarning by rememberSaveable { mutableStateOf(true) }
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleIndex >= totalItems - 4 && !content.isLoadingMore && content.hasMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    // Derive top artists from featuredSongs (unique by artist name)
    val topArtists = remember(content.featuredSongs) {
        content.featuredSongs
            .distinctBy { it.artist.trim().lowercase() }
            .take(6)
    }

    // Pilihan Cepat — first 5 from allSongs beyond the featured/trending block
    val pilihanCepat = remember(content.allSongs) {
        content.allSongs.drop(14).take(5)
    }

    LazyColumn(
        state          = listState,
        modifier       = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ══════════════════════════════════════════════════════════════════
        // ── 1. HERO SECTION ────────────────────────────────────────────
        // ══════════════════════════════════════════════════════════════════
        item(key = "hero") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                // ── Top bar ────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person, "Profile",
                            tint     = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { navActions.onSearchClick?.invoke() }) {
                        Icon(Icons.Default.Search, "Search",
                            tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = { navActions.onSettingsClick?.invoke() }) {
                        Icon(Icons.Default.Settings, "Pengaturan",
                            tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = { navActions.onAboutClick?.invoke() }) {
                        Box {
                            Icon(Icons.Default.Notifications, "Notifikasi",
                                tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF4757))
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Sapaan + headline gradient (gaya mockup 01) ────────────
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text  = content.greeting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Row {
                        Text(
                            text  = "Mau dengerin ",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text  = "apa?",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                brush = GradientAccentFull
                            )
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                // ── Kartu "Lanjut dengerin" (gaya mockup 01) ───────────────
                content.heroSong?.let { hero ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF3A1F3D), Color(0xFF241A30), Color(0xFF1E1828))
                                )
                            )
                            .border(
                                0.8.dp, Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(28.dp)
                            )
                            .clickable { onSongClick(hero, content.featuredSongs.ifEmpty { listOf(hero) }) }
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text          = "LANJUT DENGERIN",
                                style         = MaterialTheme.typography.labelSmall,
                                color         = Color(0xFFFF9D5C)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text     = hero.artist,
                                style    = MaterialTheme.typography.headlineSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text     = "${hero.title} · Song",
                                style    = MaterialTheme.typography.bodyMedium,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model              = hero.thumbnailUrl,
                                    contentDescription = null,
                                    contentScale       = ContentScale.Crop,
                                    modifier           = Modifier
                                        .size(58.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                                Spacer(Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF3EEFB)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow, "Putar",
                                        tint     = Color(0xFF15111D),
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
            }
        }

        // ── 2. QUICK ACCESS ────────────────────────────────────────────
        // ══════════════════════════════════════════════════════════════════
        item(key = "quickAccess") {
            SectionHeader(title = "Quick Access", showViewAll = true, onViewAll = navActions.onLibraryClick)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickAccessIcon(Icons.Default.Apps, "Favorit", GradientFavorit) { navActions.onLibraryClick?.invoke() }
                QuickAccessIcon(Icons.Default.FavoriteBorder, "Baru", GradientBaru, onRefresh)
                QuickAccessIcon(Icons.Default.MusicNote, "Musik", GradientMusik) { navActions.onMoodClick?.invoke() }
                QuickAccessIcon(Icons.Default.LibraryMusic, "Koleksi", GradientKoleksi) { navActions.onLibraryClick?.invoke() }
            }
        }

        // ══════════════════════════════════════════════════════════════════
        // ── 3. WARNING BANNER ──────────────────────────────────────────
        // ══════════════════════════════════════════════════════════════════
        item(key = "warning") {
            AnimatedVisibility(visible = showWarning, exit = shrinkVertically() + fadeOut()) {
                GlassCard(
                    modifier    = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape       = RoundedCornerShape(14.dp),
                    borderColor = Color.White.copy(alpha = 0.12f)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFFFC107), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Aplikasi ini GRATIS dan tidak dijual.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(2.dp))
                            Text("Bug atau saran? Hubungi pengembang langsung.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (navActions.onAboutClick != null) {
                                Spacer(Modifier.height(6.dp))
                                Text("Hubungi Pengembang →",
                                    style    = MaterialTheme.typography.labelLarge,
                                    color    = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { navActions.onAboutClick?.invoke() })
                            }
                        }
                        IconButton(onClick = { showWarning = false }, Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, "Tutup", Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════
        // ── 4. FEATURED GRID — "Speed Dial" style ──────────────────────
        //    (featuredSongs — sebelumnya tidak ditampilkan sama sekali!)
        // ══════════════════════════════════════════════════════════════════
        if (content.featuredSongs.isNotEmpty()) {
            item(key = "featuredGrid") {
                SectionHeader(title = "Pilihan Unggulan", showViewAll = false)
                val cols = 3
                val rows = (content.featuredSongs.size + cols - 1) / cols
                Column(
                    modifier              = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement   = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(rows) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            repeat(cols) { col ->
                                val idx  = row * cols + col
                                val song = content.featuredSongs.getOrNull(idx)
                                if (song != null) {
                                    FeaturedGridCard(
                                        song        = song,
                                        modifier    = Modifier.weight(1f),
                                        onClick     = { onSongClick(song, content.allSongs) },
                                        onLongClick = { onSongLongClick(song) }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════
        // ── 5. ARTIS TERPOPULER — Artist circle row ─────────────────────
        // ══════════════════════════════════════════════════════════════════
        if (topArtists.isNotEmpty()) {
            item(key = "artistCircles") {
                SectionHeader(title = "Artis Terpopuler", showViewAll = false)
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(topArtists, key = { it.id }) { song ->
                        ArtistCircleCard(
                            song    = song,
                            onClick = { onSongClick(song, content.allSongs) }
                        )
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════
        // ── 6. TRENDING SEKARANG — with rank badge ─────────────────────
        // ══════════════════════════════════════════════════════════════════
        if (content.trendingSongs.isNotEmpty()) {
            item(key = "trending") {
                SectionHeader(title = "Trending Sekarang", showViewAll = true, onViewAll = navActions.onViewTrending)
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(content.trendingSongs, key = { _, song -> "tr_${song.id}" }) { idx, song ->
                        TrendingFeaturedCard(
                            song        = song,
                            rank        = idx + 1,
                            onClick     = { onSongClick(song, content.allSongs) },
                            onLongClick = { onSongLongClick(song) }
                        )
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════
        // ── 7. LANJUTKAN MENDENGARKAN ──────────────────────────────────
        // ══════════════════════════════════════════════════════════════════
        if (content.recentSongs.isNotEmpty()) {
            item(key = "recent") {
                SectionHeader(title = "Lanjutkan Mendengarkan", showViewAll = true, onViewAll = navActions.onViewRecent)
                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(content.recentSongs, key = { it.id }) { song ->
                        ContinueListeningCard(
                            song        = song,
                            onClick     = { onSongClick(song, content.allSongs) },
                            onLongClick = { onSongLongClick(song) }
                        )
                    }
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════
        // ── 8. PILIHAN CEPAT — List with "Putar Semua" button ──────────
        // ══════════════════════════════════════════════════════════════════
        // FIX SCROLL LAG: pisahkan header & items jadi lazy items sendiri
        // (sebelumnya semua forEach dalam satu item{} → render sekaligus, tidak lazy)
        if (pilihanCepat.isNotEmpty()) {
            item(key = "pilihanCepatHeader") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text  = "Pilihan Cepat",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { onSongClick(pilihanCepat.first(), pilihanCepat) },
                        shape   = RoundedCornerShape(20.dp),
                        colors  = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor   = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(GradientAccent)
                            .padding(horizontal = 14.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Putar Semua", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            // Setiap lagu jadi item lazy tersendiri → hanya yang visible yang di-compose
            items(pilihanCepat, key = { "pc_${it.id}" }) { song ->
                Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                    PilihanCepatItem(
                        song        = song,
                        onClick     = { onSongClick(song, pilihanCepat) },
                        onLongClick = { onSongLongClick(song) }
                    )
                }
            }
            item(key = "pilihanCepatBottom") { Spacer(Modifier.height(4.dp)) }
        }

        // ══════════════════════════════════════════════════════════════════
        // ── 9. JELAJAHI SEMUA ──────────────────────────────────────────
        // ══════════════════════════════════════════════════════════════════
        item(key = "exploreHeader") {
            SectionHeader(title = "Jelajahi Semua", showViewAll = true, onViewAll = navActions.onViewExplore)
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(content.allSongs.take(12), key = { "exp_${it.id}" }) { song ->
                    ExploreCard(
                        song        = song,
                        onClick     = { onSongClick(song, content.allSongs) },
                        onLongClick = { onSongLongClick(song) }
                    )
                }
            }
        }

        // ── Vertical song list (infinite scroll) ──
        if (content.allSongs.size > 19) {
            item(key = "verticalDivider") {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f).height(0.5.dp).background(Color.White.copy(alpha = 0.08f)))
                    Text(
                        text  = "  Semua Musik  ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Box(modifier = Modifier.weight(1f).height(0.5.dp).background(Color.White.copy(alpha = 0.08f)))
                }
            }

            itemsIndexed(
                content.allSongs.drop(19),
                key = { _, song -> song.id }
            ) { _, song ->
                SongItem(
                    song        = song,
                    onClick     = { onSongClick(song, content.allSongs) },
                    onLongClick = { onSongLongClick(song) }
                )
            }
        }

        if (content.isLoadingMore) {
            item(key = "loadingMore") {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                }
            }
        }

        if (!content.hasMore && content.allSongs.size > 19) {
            item(key = "endIndicator") {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text  = "— Sudah semua —",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// ── COMPONENTS ─────────────────────────────────────────────────────────
// ══════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionHeader(
    title: String,
    showViewAll: Boolean = false,
    onViewAll: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text       = title,
                style      = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color      = MaterialTheme.colorScheme.onBackground,
            )
            // Accent underline
            Spacer(Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            )
        }
        if (showViewAll && onViewAll != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.clickable { onViewAll() }
            ) {
                Text("View", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Default.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Quick Access icon tile — gradient + glass border overlay
 */
@Composable
private fun QuickAccessIcon(
    icon: ImageVector,
    label: String,
    gradient: Brush,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(gradient)
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.05f))
                    ),
                    shape = RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)),
                        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                    )
            )
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

/**
 * Featured Grid Card — "Speed Dial" style 3-column grid
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeaturedGridCard(
    song: Song,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(0.85f)
            .clip(RoundedCornerShape(14.dp))
            .border(0.5.dp, CardBorder, RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        AsyncImage(
            model = song.thumbnailUrl, contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Gradient overlay bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                    )
                )
        )
        // Title
        Text(
            text     = song.title,
            style    = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color    = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        )
        // Play button top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * Artist circle card — circular avatar + name below
 */
@Composable
private fun ArtistCircleCard(song: Song, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(82.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    ),
                    shape = CircleShape
                )
        ) {
            AsyncImage(
                model = song.thumbnailUrl, contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
            // Subtle inner shadow at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.35f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f))
                        )
                    )
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text      = song.artist,
            style     = MaterialTheme.typography.labelSmall,
            color     = MaterialTheme.colorScheme.onSurface,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth()
        )
        Text(
            text     = "Artis",
            style    = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            maxLines = 1
        )
    }
}

/**
 * Trending Featured Card — with rank badge + play overlay
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrendingFeaturedCard(
    song: Song,
    rank: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val rankColor = when (rank) {
        1    -> AccentGold
        2    -> AccentSilver
        3    -> AccentBronze
        else -> Color.White.copy(alpha = 0.55f)
    }

    GlassCard(
        modifier    = Modifier
            .width(260.dp)
            .height(175.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape       = RoundedCornerShape(18.dp),
        borderColor = Color.White.copy(alpha = 0.14f),
        borderWidth = 0.8.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = song.thumbnailUrl, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.60f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            // Rank badge — top left
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text  = "#$rank",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = rankColor
                )
            }

            // Play button — center left
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, "Play", tint = Color.Black, modifier = Modifier.size(26.dp))
            }

            // Title + artist bottom
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium,
                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold)
                Text("${song.artist} • Song", style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f), maxLines = 1)
            }
        }
    }
}

/**
 * Continue Listening card
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContinueListeningCard(song: Song, onClick: () -> Unit, onLongClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .width(148.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape       = RoundedCornerShape(16.dp),
        borderColor = Color.White.copy(alpha = 0.12f),
        borderWidth = 0.8.dp
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                AsyncImage(
                    model = song.thumbnailUrl, contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                            )
                        )
                )
                // Small play overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(song.title, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Text(song.duration.toTimeString(), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Pilihan Cepat row item — compact card with album art, title, artist, duration
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PilihanCepatItem(song: Song, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
            .border(0.5.dp, CardBorder, RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                AsyncImage(
                    model = song.thumbnailUrl, contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(song.title, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Text(song.artist, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Text(song.duration.toTimeString(), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

/**
 * Explore card — square image + title + artist below
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExploreCard(song: Song, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(128.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
        ) {
            AsyncImage(
                model = song.thumbnailUrl, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
            // Bottom gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                        )
                    )
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(song.title, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
            overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
        Text(song.artist, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}
