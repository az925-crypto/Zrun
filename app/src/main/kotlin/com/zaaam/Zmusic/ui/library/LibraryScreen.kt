package com.zaaam.Zmusic.ui.library

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.model.entity.PlaylistEntity
import com.zaaam.Zmusic.ui.components.AlbumArt
import com.zaaam.Zmusic.ui.components.GlassCard

// ── Design tokens — alias ke ZmusicTheme (sumber kebenaran tunggal) ────────────
// Nilai TIDAK berubah; sebelumnya hardcode duplikat dari token tema.
private val ScreenBg      = com.zaaam.Zmusic.ui.theme.Bg0
private val CardSurface   = com.zaaam.Zmusic.ui.theme.Bg2
private val CardBorder    = com.zaaam.Zmusic.ui.theme.GlassBorderColor
private val TextPrimary   = com.zaaam.Zmusic.ui.theme.TextPrimary
private val TextSecondary = com.zaaam.Zmusic.ui.theme.TextMuted
private val BluePrimary   = com.zaaam.Zmusic.ui.theme.AccentMagenta

// Gradien variatif senada palet aksen (amber→magenta→violet→cyan→green)
private val coverGradients = listOf(
    Brush.linearGradient(listOf(Color(0xFFFF5F6D), Color(0xFFFFC371))),
    Brush.linearGradient(listOf(Color(0xFF7F5AF0), Color(0xFFFF4D8D))),
    Brush.linearGradient(listOf(Color(0xFF2CCCFF), Color(0xFFB66BFF))),
    Brush.linearGradient(listOf(Color(0xFF43E97B), Color(0xFF38F9D7))),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onPlaylistClick: (Long) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onNavigateToSmartPlaylist: () -> Unit = {}
) {
    val context         = LocalContext.current
    val playlists       by viewModel.playlists.collectAsState()
    val downloadedSongs by viewModel.downloadedSongs.collectAsState()
    var showCreateDialog    by remember { mutableStateOf(false) }
    var newPlaylistName     by remember { mutableStateOf("") }
    var selectedTab         by remember { mutableIntStateOf(0) }
    var showIoMenu          by remember { mutableStateOf(false) }   // dropdown export/import
    var showExportPicker    by remember { mutableStateOf(false) }   // dialog pilih playlist

    // ── File picker untuk Import ───────────────────────────────────────────
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importPlaylist(context, it) } }

    // ── Collect events dari ViewModel ──────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.exportIntent.collect { intent -> context.startActivity(intent) }
    }
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ─────────────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(
                    start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp
                )
            ) {
                Text(
                    text       = "Koleksi",
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary,
                    letterSpacing = (-0.3).sp
                )
                val subtitle = buildString {
                    if (playlists.isNotEmpty()) append("${playlists.size} playlist")
                    if (playlists.isNotEmpty() && downloadedSongs.isNotEmpty()) append(" · ")
                    if (downloadedSongs.isNotEmpty()) append("${downloadedSongs.size} tersimpan")
                    if (isEmpty()) append("Koleksi musikmu")
                }
                Text(
                    text     = subtitle,
                    fontSize = 13.sp,
                    color    = TextSecondary,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            // ── Pill Tab ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(CardSurface)
                    .border(0.8.dp, CardBorder, RoundedCornerShape(13.dp))
                    .padding(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("Playlist", "Tersimpan", "Smart").forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected)
                                        Brush.linearGradient(listOf(TextPrimary, TextPrimary))
                                    else
                                        Brush.linearGradient(
                                            listOf(Color.Transparent, Color.Transparent)
                                        )
                                )
                                .clickable { selectedTab = index }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment    = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text       = title,
                                    fontSize   = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color      = if (isSelected) ScreenBg else TextSecondary
                                )
                                if (index == 1 && downloadedSongs.isNotEmpty()) {
                                    Spacer(Modifier.width(5.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(BluePrimary.copy(alpha = 0.18f))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text       = "${downloadedSongs.size}",
                                            fontSize   = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color      = BluePrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Tab Content ────────────────────────────────────────────────
            when (selectedTab) {
                0 -> Column {
                    if (playlists.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            playlists.take(2).forEachIndexed { i, pinned ->
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CardSurface)
                                        .border(0.8.dp, CardBorder, RoundedCornerShape(12.dp))
                                        .clickable { onPlaylistClick(pinned.id) }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(coverGradients[i % coverGradients.size])
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text       = pinned.name,
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color      = TextPrimary,
                                        maxLines   = 1,
                                        overflow   = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (playlists.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    PlaylistTab(
                    playlists        = playlists,
                    onPlaylistClick  = onPlaylistClick,
                    onDeletePlaylist = { viewModel.deletePlaylist(it) }
                )
                }
                1 -> DownloadedTab(
                    songs            = downloadedSongs,
                    onSongClick      = { song -> onSongClick(song, downloadedSongs) },
                    onSongLongClick  = onSongLongClick,
                    onDeleteDownload = { viewModel.deleteDownload(it) }
                )
                2 -> SmartPlaylistEntryTab(
                    onNavigate = onNavigateToSmartPlaylist
                )
            }
        }

        // ── FABs: Import/Export di atas, + Buat Playlist di bawah ────────
        if (selectedTab == 0) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 28.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                // FAB Import/Export
                Box {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardSurface)
                            .border(0.8.dp, CardBorder, RoundedCornerShape(16.dp))
                            .clickable { showIoMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ImportExport,
                            contentDescription = "Export / Import",
                            tint     = BluePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    DropdownMenu(
                        expanded         = showIoMenu,
                        onDismissRequest = { showIoMenu = false },
                        containerColor   = Color(0xFF1A2030)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export JSON", color = TextPrimary, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Upload, null, tint = BluePrimary, modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                showIoMenu = false
                                showExportPicker = true   // tampilkan dialog pilih playlist
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Import JSON", color = TextPrimary, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Download, null, tint = BluePrimary, modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                showIoMenu = false
                                importLauncher.launch(arrayOf("application/json", "*/*"))
                            }
                        )
                    }
                }

                // FAB + Buat Playlist
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFFF9D5C), Color(0xFFFF4D8D))))
                        .clickable { showCreateDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Buat Playlist",
                        tint     = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // ── Dialog Pilih Playlist untuk Export ────────────────────────────
        if (showExportPicker) {
            if (playlists.isEmpty()) {
                // Tidak ada playlist — langsung toast
                LaunchedEffect(Unit) {
                    Toast.makeText(context, "Belum ada playlist untuk di-export", Toast.LENGTH_SHORT).show()
                    showExportPicker = false
                }
            } else {
                AlertDialog(
                    onDismissRequest = { showExportPicker = false },
                    containerColor   = Color(0xFF1E1828),
                    title = {
                        Text("Pilih Playlist", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Playlist mana yang ingin di-export?",
                                fontSize = 13.sp,
                                color    = TextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            playlists.forEach { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(CardSurface)
                                        .border(0.8.dp, CardBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            showExportPicker = false
                                            viewModel.exportPlaylist(context, playlist.id)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.MusicNote, null,
                                        tint     = BluePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text      = playlist.name,
                                        fontSize  = 14.sp,
                                        color     = TextPrimary,
                                        maxLines  = 1,
                                        overflow  = TextOverflow.Ellipsis,
                                        modifier  = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showExportPicker = false }) {
                            Text("Batal", color = TextSecondary)
                        }
                    }
                )
            }
        }
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false; newPlaylistName = "" },
                containerColor   = Color(0xFF1E1828),
                title = {
                    Text(
                        "Buat Playlist Baru",
                        color      = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    OutlinedTextField(
                        value         = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label         = { Text("Nama Playlist") },
                        singleLine    = true,
                        shape         = RoundedCornerShape(12.dp),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = BluePrimary,
                            unfocusedBorderColor = CardBorder,
                            focusedLabelColor    = BluePrimary,
                            cursorColor          = BluePrimary,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.createPlaylist(newPlaylistName)
                        showCreateDialog = false
                        newPlaylistName  = ""
                    }) {
                        Text("Buat", color = BluePrimary, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showCreateDialog = false
                        newPlaylistName  = ""
                    }) {
                        Text("Batal", color = TextSecondary)
                    }
                }
            )
        }
    }
}

// ── Tab Playlist ───────────────────────────────────────────────────────────────

@Composable
private fun PlaylistTab(
    playlists: List<PlaylistEntity>,
    onPlaylistClick: (Long) -> Unit,
    onDeletePlaylist: (Long) -> Unit
) {
    if (playlists.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            com.zaaam.Zmusic.ui.components.StateDisplay(
                icon    = Icons.Default.PlaylistPlay,
                title   = "Belum ada playlist",
                message = "Kumpulkan lagu favoritmu — tap + di kanan bawah untuk membuat playlist pertamamu."
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(
                start  = 16.dp,
                end    = 16.dp,
                top    = 4.dp,
                bottom = 96.dp   // ruang untuk FAB
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(playlists, key = { _, pl -> pl.id }) { index, playlist ->
                var showMenu by remember { mutableStateOf(false) }

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlaylistClick(playlist.id) },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier          = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cover gradient
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(coverGradients[index % coverGradients.size]),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MusicNote, null,
                                tint     = Color.White.copy(alpha = 0.90f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text      = playlist.name,
                                fontSize  = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color     = TextPrimary,
                                maxLines  = 1,
                                overflow  = TextOverflow.Ellipsis
                            )
                            Text(
                                text     = "Playlist",
                                fontSize = 11.sp,
                                color    = TextSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // 3-dot menu
                        Box {
                            IconButton(
                                onClick  = { showMenu = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert, "Menu",
                                    tint     = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded          = showMenu,
                                onDismissRequest  = { showMenu = false },
                                containerColor    = Color(0xFF1A2030)
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Hapus Playlist",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete, null,
                                            tint     = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDeletePlaylist(playlist.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Tab Tersimpan ─────────────────────────────────────────────────────────────

@Composable
private fun DownloadedTab(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onDeleteDownload: (Song) -> Unit
) {
    if (songs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            com.zaaam.Zmusic.ui.components.StateDisplay(
                icon    = Icons.Default.DownloadDone,
                title   = "Belum ada lagu tersimpan",
                message = "Unduh lagu dari halaman Player untuk didengarkan secara offline."
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical   = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSongClick(song) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumArt(thumbnailUrl = song.thumbnailUrl, size = 46.dp)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text       = song.title,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color      = TextPrimary,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Text(
                            text     = song.artist,
                            fontSize = 11.sp,
                            color    = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Badge offline
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BluePrimary.copy(alpha = 0.12f))
                            .border(0.5.dp, BluePrimary.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text          = "OFFLINE",
                            fontSize      = 9.sp,
                            fontWeight    = FontWeight.Bold,
                            color         = BluePrimary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    IconButton(
                        onClick  = { onDeleteDownload(song) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete, "Hapus Download",
                            tint     = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Tab Smart Playlist entry ──────────────────────────────────────────────────

@Composable
private fun SmartPlaylistEntryTab(onNavigate: () -> Unit) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp))
                    .background(BluePrimary.copy(alpha = 0.12f))
                    .border(1.dp, BluePrimary.copy(alpha = 0.25f), androidx.compose.foundation.shape.RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("✨", fontSize = 36.sp)
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "Smart Playlist",
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Playlist yang dibuat otomatis dari histori dengaran kamu — Top Picks, Mood Mix, dan Recent Faves.",
                fontSize  = 13.sp,
                color     = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onNavigate,
                shape   = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                colors  = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Lihat Smart Playlist", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
