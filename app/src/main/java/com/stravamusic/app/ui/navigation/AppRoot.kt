package com.stravamusic.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.stravamusic.app.music.MusicViewModel
import com.stravamusic.app.ui.components.MusicBar
import com.stravamusic.app.ui.screens.ActivityDetailScreen
import com.stravamusic.app.ui.screens.HistoryScreen
import com.stravamusic.app.ui.screens.RecordScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Record : Screen("record", "Record", Icons.Filled.RadioButtonChecked)
    data object History : Screen("history", "History", Icons.Filled.History)
}

private val bottomNavItems = listOf(Screen.Record, Screen.History)

@Composable
fun AppRoot(
    hasLocationPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val navController = rememberNavController()
    val musicViewModel: MusicViewModel = viewModel()
    val musicState by musicViewModel.state.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBars = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBars) {
                Column {
                    MusicBar(
                        state = musicState,
                        onPlayPause = { musicViewModel.togglePlayPause() },
                        onNext = { musicViewModel.next() },
                        onPrevious = { musicViewModel.previous() }
                    )
                    NavigationBar {
                        bottomNavItems.forEach { screen ->
                            val selected = backStackEntry?.destination?.hierarchy?.any {
                                it.route == screen.route
                            } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(screen.icon, contentDescription = screen.label) },
                                label = { Text(screen.label) }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Record.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Record.route) {
                RecordScreen(
                    hasLocationPermission = hasLocationPermission,
                    onRequestPermission = onRequestPermission
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    onOpenActivity = { id -> navController.navigate("detail/$id") }
                )
            }
            composable("detail/{id}") { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                ActivityDetailScreen(
                    activityId = id,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
