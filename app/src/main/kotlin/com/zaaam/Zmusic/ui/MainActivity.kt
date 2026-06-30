package com.zaaam.Zmusic.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.ui.about.AboutScreen
import com.zaaam.Zmusic.ui.about.DevScreen
import com.zaaam.Zmusic.ui.artist.ArtistScreen
import com.zaaam.Zmusic.ui.artist.ArtistViewModel
import com.zaaam.Zmusic.ui.components.MiniPlayerBar
import com.zaaam.Zmusic.ui.equalizer.EqualizerScreen
import com.zaaam.Zmusic.ui.equalizer.EqualizerViewModel
import com.zaaam.Zmusic.ui.home.HomeNavActions
import com.zaaam.Zmusic.ui.home.HomeScreen
import com.zaaam.Zmusic.ui.home.HomeViewModel
import com.zaaam.Zmusic.ui.library.LibraryScreen
import com.zaaam.Zmusic.ui.library.LibraryViewModel
import com.zaaam.Zmusic.ui.library.PlaylistDetailScreen
import com.zaaam.Zmusic.ui.library.PlaylistDetailViewModel
import com.zaaam.Zmusic.ui.mood.MoodScreen
import com.zaaam.Zmusic.ui.mood.MoodViewModel
import com.zaaam.Zmusic.ui.player.PlayerScreen
import com.zaaam.Zmusic.ui.player.PlayerViewModel
import com.zaaam.Zmusic.ui.search.SearchScreen
import com.zaaam.Zmusic.ui.search.SearchViewModel
import com.zaaam.Zmusic.ui.settings.SettingsScreen
import com.zaaam.Zmusic.ui.settings.SettingsViewModel
import com.zaaam.Zmusic.ui.smartplaylist.SmartPlaylistScreen
import com.zaaam.Zmusic.ui.smartplaylist.SmartPlaylistViewModel
import com.zaaam.Zmusic.ui.stats.StatsScreen
import com.zaaam.Zmusic.ui.stats.StatsViewModel
import com.zaaam.Zmusic.ui.trending.TrendingScreen
import com.zaaam.Zmusic.ui.trending.TrendingViewModel
import com.zaaam.Zmusic.ui.recent.RecentlyPlayedScreen
import com.zaaam.Zmusic.ui.recent.RecentlyPlayedViewModel
import com.zaaam.Zmusic.ui.explore.ExploreScreen
import com.zaaam.Zmusic.ui.explore.ExploreViewModel
import com.zaaam.Zmusic.ui.tracking.ActivitiesScreen
import com.zaaam.Zmusic.ui.tracking.ActivityDetailScreen
import com.zaaam.Zmusic.ui.tracking.RecordScreen
import com.zaaam.Zmusic.ui.theme.FloatingNavBg
import com.zaaam.Zmusic.ui.theme.FloatingNavBorder
import com.zaaam.Zmusic.ui.theme.NavActiveGlow
import com.zaaam.Zmusic.ui.theme.ZmusicTheme
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen(val route: String, val label: String) {
    // ── FITUR STRAVA ──
    object Record        : Screen("record", "Rekam")
    object Activities    : Screen("activities", "Aktivitas")
    object ActivityDetail: Screen("activity/{activityId}", "Detail Aktivitas")
    // ── MUSIK ──
    object Home          : Screen("home", "Home")
    object Search        : Screen("search", "Cari")
    object Library       : Screen("library", "Koleksi")
    object Stats         : Screen("stats", "Statistik")
    object Mood          : Screen("mood", "Mood")
    object Player        : Screen("player", "Player")
    object PlaylistDetail: Screen("playlist/{playlistId}", "Playlist")
    object About         : Screen("about", "Tentang")
    object Dev           : Screen("dev", "Pengembang")
    object Settings      : Screen("settings", "Pengaturan")
    object Trending      : Screen("trending", "Trending")
    object RecentlyPlayed: Screen("recently_played", "Riwayat")
    object Explore       : Screen("explore", "Jelajahi")
    // ── NEW v2.0.0 ──
    object Equalizer     : Screen("equalizer", "Equalizer")
    object Artist        : Screen("artist/{artistName}", "Artis")
    object SmartPlaylist : Screen("smart_playlist", "Smart Playlist")
}

// Nav items with selected/unselected icon pairs
data class NavItem(
    val screen: Screen,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
    val showLabel: Boolean = false // only Koleksi shows label
)

// Strava-first: Rekam jadi tab utama, Aktivitas (riwayat), lalu Musik (= Home).
val bottomNavItems = listOf(
    NavItem(Screen.Record,     Icons.Default.RadioButtonChecked, Icons.Outlined.RadioButtonChecked, "Rekam", showLabel = true),
    NavItem(Screen.Activities, Icons.Default.DirectionsRun, Icons.Default.DirectionsRun, "Aktivitas"),
    NavItem(Screen.Home,       Icons.Default.LibraryMusic, Icons.Default.LibraryMusic, "Musik")
)

private val hideBottomBarRoutes = setOf(
    Screen.Player.route,
    Screen.About.route,
    Screen.Dev.route,
    Screen.Settings.route,
    Screen.Equalizer.route,
    "artist/{artistName}",
    Screen.SmartPlaylist.route,
    Screen.ActivityDetail.route
)

// Routes that show bottom bar but are sub-pages (for back navigation)
private val subPageRoutes = setOf(
    Screen.Trending.route,
    Screen.RecentlyPlayed.route,
    Screen.Explore.route
)

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZmusicTheme {
                com.zaaam.Zmusic.ui.zrun.ZRunApp()
            }
        }
    }
}

@Composable
fun ZmusicApp() {
    val navController    = rememberNavController()
    val playerViewModel  : PlayerViewModel  = hiltViewModel()
    val libraryViewModel : LibraryViewModel = hiltViewModel()
    val context          = LocalContext.current

    val currentQueue by playerViewModel.queueManager.queue.collectAsState()
    val currentIndex by playerViewModel.queueManager.currentIndex.collectAsState()
    val song         =  currentQueue.getOrNull(currentIndex)
    val isPlaying    by playerViewModel.isPlaying.collectAsState()
    val progress     by playerViewModel.progress.collectAsState()

    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }
    val playlists           by libraryViewModel.playlists.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination =  navBackStackEntry?.destination
    val hideBottomBar      =  currentDestination?.route in hideBottomBarRoutes

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!hideBottomBar) {
                Column(
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    // MiniPlayerBar
                    if (song != null) {
                        MiniPlayerBar(
                            currentSong      = song,
                            isPlaying        = isPlaying,
                            progress         = progress,
                            onPlayPauseClick = { playerViewModel.togglePlayPause() },
                            onNextClick      = { playerViewModel.skipNext() },
                            onBarClick       = { navController.navigate(Screen.Player.route) }
                        )
                    }

                    // ── Floating Pill Navigation Bar ──────────────────────
                    FloatingBottomNav(
                        currentRoute = currentDestination?.route,
                        currentHierarchy = currentDestination?.hierarchy,
                        onItemClick = { screen ->
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Record.route,
            modifier         = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            // Transisi antar layar — bikin pindah tab/halaman terasa mulus,
            // bukan ganti seketika (snap). Fade + zoom halus, senada dgn animasi navbar.
            enterTransition    = { fadeIn(tween(280)) + scaleIn(initialScale = 0.96f, animationSpec = tween(280)) },
            exitTransition     = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(280)) + scaleIn(initialScale = 0.96f, animationSpec = tween(280)) },
            popExitTransition  = { fadeOut(tween(180)) }
        ) {
            // ── FITUR STRAVA: Rekam / Aktivitas / Detail ──────────────
            composable(Screen.Record.route) {
                RecordScreen()
            }

            composable(Screen.Activities.route) {
                ActivitiesScreen(
                    onOpenActivity = { id -> navController.navigate("activity/$id") }
                )
            }

            composable(
                route     = Screen.ActivityDetail.route,
                arguments = listOf(navArgument("activityId") { type = NavType.StringType })
            ) {
                ActivityDetailScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel       = homeViewModel,
                    onSongClick     = { clickedSong, queue ->
                        playerViewModel.playSong(clickedSong, queue, autoShuffle = true)
                        navController.navigate(Screen.Player.route)
                    },
                    onSongLongClick = { clickedSong -> songToAddToPlaylist = clickedSong },
                    navActions      = HomeNavActions(
                        onAboutClick    = { navController.navigate(Screen.About.route) },
                        onDevPageClick  = { navController.navigate(Screen.Dev.route) },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) },
                        // Quick Access navigation callbacks
                        onSearchClick   = {
                            navController.navigate(Screen.Search.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        onLibraryClick  = {
                            navController.navigate(Screen.Library.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        onMoodClick     = {
                            navController.navigate(Screen.Mood.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        onStatsClick    = {
                            navController.navigate(Screen.Stats.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        },
                        // View > dedicated page callbacks
                        onViewTrending  = { navController.navigate(Screen.Trending.route) },
                        onViewRecent    = { navController.navigate(Screen.RecentlyPlayed.route) },
                        onViewExplore   = { navController.navigate(Screen.Explore.route) }
                    )
                )
            }

            composable(Screen.Search.route) {
                val searchViewModel: SearchViewModel = hiltViewModel()
                SearchScreen(
                    viewModel       = searchViewModel,
                    onSongClick     = { clickedSong, queue ->
                        playerViewModel.playSong(clickedSong, queue, autoShuffle = true)
                        navController.navigate(Screen.Player.route)
                    },
                    onSongLongClick = { clickedSong -> songToAddToPlaylist = clickedSong }
                )
            }

            composable(Screen.Mood.route) {
                val moodViewModel: MoodViewModel = hiltViewModel()
                MoodScreen(
                    viewModel       = moodViewModel,
                    onSongClick     = { clickedSong, queue, moodName ->
                        playerViewModel.playSong(clickedSong, queue, moodHint = moodName, autoShuffle = true)
                        navController.navigate(Screen.Player.route)
                    },
                    onSongLongClick = { clickedSong -> songToAddToPlaylist = clickedSong }
                )
            }

            composable(Screen.Library.route) {
                LibraryScreen(
                    viewModel                  = libraryViewModel,
                    onPlaylistClick            = { playlistId ->
                        navController.navigate("playlist/$playlistId")
                    },
                    onSongClick                = { clickedSong, queue ->
                        playerViewModel.playSong(clickedSong, queue)
                        navController.navigate(Screen.Player.route)
                    },
                    onSongLongClick            = { clickedSong -> songToAddToPlaylist = clickedSong },
                    onNavigateToSmartPlaylist  = { navController.navigate(Screen.SmartPlaylist.route) }
                )
            }

            composable(
                route     = "playlist/{playlistId}",
                arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
            ) {
                val detailViewModel: PlaylistDetailViewModel = hiltViewModel()
                PlaylistDetailScreen(
                    viewModel      = detailViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSongClick    = { clickedSong, songs ->
                        playerViewModel.playPlaylistShuffled(songs, clickedSong)
                        navController.navigate(Screen.Player.route)
                    },
                    onPlayAll      = { songs ->
                        if (songs.isNotEmpty()) {
                            playerViewModel.playSong(songs.first(), songs)
                            navController.navigate(Screen.Player.route)
                        }
                    },
                    onShuffleAll   = { songs ->
                        if (songs.isNotEmpty()) {
                            playerViewModel.playPlaylistShuffled(songs)
                            navController.navigate(Screen.Player.route)
                        }
                    }
                )
            }

            composable(Screen.Stats.route) {
                val statsViewModel: StatsViewModel = hiltViewModel()
                StatsScreen(viewModel = statsViewModel)
            }

            composable(Screen.Player.route) {
                PlayerScreen(
                    viewModel             = playerViewModel,
                    onNavigateBack        = { navController.popBackStack() },
                    onNavigateToEqualizer = { navController.navigate(Screen.Equalizer.route) },
                    onNavigateToArtist    = { artistName ->
                        navController.navigate("artist/${java.net.URLEncoder.encode(artistName, "UTF-8")}")
                    }
                )
            }

            // ── NEW v2.0.0 Routes ──────────────────────────────────────
            composable(Screen.Equalizer.route) {
                val equalizerViewModel: EqualizerViewModel = hiltViewModel()
                EqualizerScreen(
                    viewModel      = equalizerViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route     = "artist/{artistName}",
                arguments = listOf(navArgument("artistName") { type = NavType.StringType })
            ) { backStackEntry ->
                val artistViewModel: ArtistViewModel = hiltViewModel()
                val rawName   = backStackEntry.arguments?.getString("artistName") ?: ""
                val artistName = try { java.net.URLDecoder.decode(rawName, "UTF-8") } catch (_: Exception) { rawName }
                LaunchedEffect(artistName) {
                    if (artistName.isNotBlank()) artistViewModel.loadArtist(artistName)
                }
                ArtistScreen(
                    viewModel       = artistViewModel,
                    onSongClick     = { clickedSong, queue ->
                        playerViewModel.playSong(clickedSong, queue)
                        navController.navigate(Screen.Player.route)
                    },
                    onSongLongClick = { clickedSong -> songToAddToPlaylist = clickedSong },
                    onNavigateBack  = { navController.popBackStack() }
                )
            }

            composable(Screen.SmartPlaylist.route) {
                val spViewModel: SmartPlaylistViewModel = hiltViewModel()
                SmartPlaylistScreen(
                    viewModel       = spViewModel,
                    onSongClick     = { clickedSong, queue ->
                        playerViewModel.playSong(clickedSong, queue)
                        navController.navigate(Screen.Player.route)
                    },
                    onSongLongClick = { clickedSong -> songToAddToPlaylist = clickedSong },
                    onNavigateBack  = { navController.popBackStack() }
                )
            }

            // ── NEW: Dedicated View pages ──────────────────────────────
            composable(Screen.Trending.route) {
                val trendingViewModel: TrendingViewModel = hiltViewModel()
                TrendingScreen(
                    viewModel       = trendingViewModel,
                    onSongClick     = { clickedSong, queue ->
                        playerViewModel.playSong(clickedSong, queue, autoShuffle = true)
                        navController.navigate(Screen.Player.route)
                    },
                    onSongLongClick = { clickedSong -> songToAddToPlaylist = clickedSong },
                    onBack          = { navController.popBackStack() }
                )
            }

            composable(Screen.RecentlyPlayed.route) {
                val recentViewModel: RecentlyPlayedViewModel = hiltViewModel()
                RecentlyPlayedScreen(
                    viewModel       = recentViewModel,
                    onSongClick     = { clickedSong: Song, queue: List<Song> ->
                        playerViewModel.playSong(clickedSong, queue, autoShuffle = true)
                        navController.navigate(Screen.Player.route)
                    },
                    onSongLongClick = { clickedSong: Song -> songToAddToPlaylist = clickedSong },
                    onBack          = { navController.popBackStack() }
                )
            }

            composable(Screen.Explore.route) {
                val exploreViewModel: ExploreViewModel = hiltViewModel()
                ExploreScreen(
                    viewModel       = exploreViewModel,
                    onSongClick     = { clickedSong, queue ->
                        playerViewModel.playSong(clickedSong, queue, autoShuffle = true)
                        navController.navigate(Screen.Player.route)
                    },
                    onSongLongClick = { clickedSong -> songToAddToPlaylist = clickedSong },
                    onBack          = { navController.popBackStack() }
                )
            }

            composable(Screen.About.route) {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onDevPageClick = { navController.navigate(Screen.Dev.route) }
                )
            }

            composable(Screen.Dev.route) {
                DevScreen(onNavigateBack = { navController.popBackStack() })
            }

            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel      = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }

    // Dialog: Tambah lagu ke playlist
    songToAddToPlaylist?.let { songToAdd ->
        if (playlists.isEmpty()) {
            AlertDialog(
                onDismissRequest = { songToAddToPlaylist = null },
                title   = { Text("Belum Ada Playlist") },
                text    = { Text("Buat playlist dulu di tab Perpustakaan.") },
                confirmButton = {
                    TextButton(onClick = {
                        songToAddToPlaylist = null
                        navController.navigate(Screen.Library.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }) { Text("Buat Playlist") }
                },
                dismissButton = {
                    TextButton(onClick = { songToAddToPlaylist = null }) { Text("Batal") }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { songToAddToPlaylist = null },
                title = { Text("Tambah ke Playlist") },
                text  = {
                    Column {
                        playlists.forEach { playlist ->
                            TextButton(
                                onClick = {
                                    libraryViewModel.addSongToPlaylist(playlist.id, songToAdd)
                                    songToAddToPlaylist = null
                                    Toast.makeText(
                                        context,
                                        "\"${songToAdd.title}\" ditambahkan ke ${playlist.name}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            ) {
                                Text(
                                    text  = playlist.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { songToAddToPlaylist = null }) { Text("Batal") }
                }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════
// ── Floating Pill Bottom Navigation ────────────────────────────────────
// ══════════════════════════════════════════════════════════════════════════

@Composable
private fun FloatingBottomNav(
    currentRoute: String?,
    currentHierarchy: Sequence<androidx.navigation.NavDestination>?,
    onItemClick: (Screen) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(FloatingNavBg)
                .border(1.dp, FloatingNavBorder, RoundedCornerShape(30.dp))
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = currentHierarchy?.any {
                    it.route == item.screen.route
                } == true

                FloatingNavItem(
                    item       = item,
                    isSelected = isSelected,
                    onClick    = { onItemClick(item.screen) }
                )
            }
        }
    }
}

@Composable
private fun FloatingNavItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "navIconColor"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) NavActiveGlow else Color.Transparent,
        animationSpec = tween(200),
        label = "navBgColor"
    )

    // Sedikit "pop" springy saat tab dipilih — bikin navbar terasa hidup, tak kaku.
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "navIconScale"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClick           = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (item.showLabel && isSelected) {
            // Active Koleksi tab — show icon + label
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = if (isSelected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                    tint               = iconColor,
                    modifier           = Modifier.size(22.dp).scale(iconScale)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text       = item.label,
                    style      = MaterialTheme.typography.labelMedium,
                    color      = iconColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 12.sp
                )
            }
        } else {
            Icon(
                imageVector        = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                tint               = iconColor,
                modifier           = Modifier.size(24.dp).scale(iconScale)
            )
        }
    }
}
