package com.zaaam.Zmusic.ui.zrun

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zaaam.Zmusic.ui.player.PlayerViewModel
import com.zaaam.Zmusic.ui.zrun.screens.DashboardScreen
import com.zaaam.Zmusic.ui.zrun.screens.FeedScreen
import com.zaaam.Zmusic.ui.zrun.screens.MusicHomeScreen
import com.zaaam.Zmusic.ui.zrun.screens.PlayerScreenZR
import com.zaaam.Zmusic.ui.zrun.screens.PlaylistScreenZR
import com.zaaam.Zmusic.ui.zrun.screens.ProfileScreen
import com.zaaam.Zmusic.ui.zrun.screens.RunScreen
import com.zaaam.Zmusic.ui.zrun.screens.SearchScreenZR

private object Routes {
    const val DASH = "dashboard"
    const val MUSIC = "music"
    const val RUN = "run"
    const val FEED = "feed"
    const val PROFILE = "profile"
    const val PLAYER = "player"
    const val SEARCH = "search"
    const val PLAYLIST = "playlist/{playlistId}"
}

private val barRoutes = setOf(Routes.DASH, Routes.MUSIC, Routes.FEED, Routes.PROFILE)

@Composable
fun ZRunApp() {
    val nav = rememberNavController()
    val player: PlayerViewModel = hiltViewModel()

    val queue by player.queueManager.queue.collectAsState()
    val idx by player.queueManager.currentIndex.collectAsState()
    val song = queue.getOrNull(idx)
    val isPlaying by player.isPlaying.collectAsState()
    val progress by player.progress.collectAsState()

    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBars = route in barRoutes

    Box(Modifier.fillMaxSize().background(ZR.Bg)) {
        NavHost(navController = nav, startDestination = Routes.DASH, modifier = Modifier.fillMaxSize()) {
            composable(Routes.DASH) {
                DashboardScreen(
                    player = player,
                    onStartRun = { nav.navigate(Routes.RUN) },
                    onOpenMusic = { nav.navigateTab(Routes.MUSIC) },
                    onOpenPlayer = { nav.navigate(Routes.PLAYER) }
                )
            }
            composable(Routes.MUSIC) {
                MusicHomeScreen(
                    player = player,
                    onOpenPlayer = { nav.navigate(Routes.PLAYER) },
                    onOpenPlaylist = { id -> nav.navigate("playlist/$id") },
                    onOpenSearch = { nav.navigate(Routes.SEARCH) }
                )
            }
            composable(Routes.FEED) { FeedScreen() }
            composable(Routes.PROFILE) { ProfileScreen() }
            composable(Routes.RUN) { RunScreen(onClose = { nav.popBackStack() }) }
            composable(Routes.PLAYER) { PlayerScreenZR(player = player, onClose = { nav.popBackStack() }) }
            composable(Routes.SEARCH) {
                SearchScreenZR(player = player, onBack = { nav.popBackStack() }, onOpenPlayer = { nav.navigate(Routes.PLAYER) })
            }
            composable(Routes.PLAYLIST, arguments = listOf(navArgument("playlistId") { type = NavType.LongType })) {
                PlaylistScreenZR(player = player, onBack = { nav.popBackStack() }, onOpenPlayer = { nav.navigate(Routes.PLAYER) })
            }
        }

        if (showBars) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                if (song != null) {
                    ZRMiniPlayer(
                        song = song, isPlaying = isPlaying, progress = progress,
                        onPlayPause = { player.togglePlayPause() },
                        onNext = { player.skipNext() },
                        onClick = { nav.navigate(Routes.PLAYER) },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                ZRBottomBar(
                    current = route,
                    onTab = { nav.navigateTab(it) },
                    onRun = { nav.navigate(Routes.RUN) }
                )
            }
        }
    }
}

private fun androidx.navigation.NavController.navigateTab(r: String) {
    navigate(r) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun ZRBottomBar(current: String?, onTab: (String) -> Unit, onRun: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(74.dp)
            .background(Color(0xCC101216))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTab("Home", Icons.Filled.Home, current == Routes.DASH) { onTab(Routes.DASH) }
            NavTab("Musik", Icons.Filled.LibraryMusic, current == Routes.MUSIC) { onTab(Routes.MUSIC) }
            RunFab(onRun)
            NavTab("Feed", Icons.AutoMirrored.Filled.List, current == Routes.FEED) { onTab(Routes.FEED) }
            NavTab("You", Icons.Filled.Person, current == Routes.PROFILE) { onTab(Routes.PROFILE) }
        }
    }
}

@Composable
private fun RunFab(onRun: () -> Unit) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .offset(y = (-18).dp)
            .clip(CircleShape)
            .background(ZR.Ember)
            .clickable(onClick = onRun),
        contentAlignment = Alignment.Center
    ) {
        Text("RUN", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
    }
}

@Composable
private fun NavTab(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = MutableInteractionSource(), indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = if (active) ZR.Ember2 else ZR.Faint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, color = if (active) ZR.Tx else ZR.Faint, fontSize = 10.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
    }
}
