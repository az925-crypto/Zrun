package com.stravamusic.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.stravamusic.app.ui.navigation.AppRoot
import com.stravamusic.app.ui.theme.StravaMusicTheme

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StravaMusicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val permissions = buildList {
                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    val permissionState = rememberMultiplePermissionsState(permissions)
                    val hasLocation = permissionState.permissions
                        .firstOrNull { it.permission == Manifest.permission.ACCESS_FINE_LOCATION }
                        ?.status?.isGranted == true

                    AppRoot(
                        hasLocationPermission = hasLocation,
                        onRequestPermission = { permissionState.launchMultiplePermissionRequest() }
                    )
                }
            }
        }
    }
}
