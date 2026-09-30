package com.zaaam.Zmusic.ui.zrun

import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

    CompositionLocalProvider(LocalContentColor provides ZR.Tx) {
        Box(Modifier.fillMaxSize().background(ZR.Bg)) {
            NavHost(
                navController = nav,
                startDestination = Routes.DASH,
                modifier = Modifier.fillMaxSize(),
                // Transisi berenergi: layar meluncur cepat dengan pegas, tanpa fade
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(stiffness = 1000f, dampingRatio = 0.85f)
                    )
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it / 3 },
                        animationSpec = spring(stiffness = 1000f, dampingRatio = 0.85f)
                    )
                },
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = spring(stiffness = 1000f, dampingRatio = 0.85f)
                    )
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = spring(stiffness = 1000f, dampingRatio = 0.85f)
                    )
                }
            ) {
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
                composable(Routes.FEED) { FeedScreen(onOpenRun = { nav.navigate(Routes.RUN) }) }
                composable(Routes.PROFILE) { ProfileScreen() }
                composable(Routes.RUN) {
                    RunScreen(
                        player = player,
                        onClose = { nav.popBackStack() },
                        onOpenMusic = { nav.navigate(Routes.MUSIC) },
                        onOpenPlayer = { nav.navigate(Routes.PLAYER) }
                    )
                }
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
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    if (song != null) {
                        ZRMiniPlayer(
                            song = song, isPlaying = isPlaying, progress = progress,
                            onPlayPause = { player.togglePlayPause() },
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
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xF00A0B0E))
            .navigationBarsPadding()
    ) {
        Divider(color = ZR.Stroke, thickness = 1.dp)
        Row(
            Modifier
                .fillMaxWidth()
                .height(78.dp)
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            NavTab("Home", ZRIcons.Home, current == Routes.DASH, Modifier.weight(1f)) { onTab(Routes.DASH) }
            NavTab("Musik", ZRIcons.Music, current == Routes.MUSIC, Modifier.weight(1f)) { onTab(Routes.MUSIC) }
            RunTab(onRun, Modifier.weight(1f))
            NavTab("Feed", ZRIcons.Feed, current == Routes.FEED, Modifier.weight(1f)) { onTab(Routes.FEED) }
            NavTab("You", ZRIcons.User, current == Routes.PROFILE, Modifier.weight(1f)) { onTab(Routes.PROFILE) }
        }
    }
}

@Composable
private fun RunTab(onRun: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.height(36.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .offset(y = (-20).dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(ZR.Ember)
                    .border(4.dp, ZR.Bg, CircleShape)
                    .clickable(onClick = onRun),
                contentAlignment = Alignment.Center
            ) {
                Icon(ZRIcons.Flame, contentDescription = "Run", tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Run", style = zStyle(10.5.sp, FontWeight.Medium), color = ZR.Mut)
    }
}

@Composable
private fun NavTab(
    label: String,
    icon: ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.height(36.dp), contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (active) ZR.Ember else ZR.Faint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = zStyle(10.5.sp, if (active) FontWeight.Bold else FontWeight.Medium),
            color = if (active) ZR.Tx else ZR.Faint
        )
    }
}
