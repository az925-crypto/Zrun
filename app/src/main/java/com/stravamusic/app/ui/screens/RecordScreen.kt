package com.stravamusic.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.stravamusic.app.tracking.LocationUtils
import com.stravamusic.app.tracking.TrackingViewModel
import com.stravamusic.app.ui.components.StatBlock

@Composable
fun RecordScreen(
    hasLocationPermission: Boolean,
    onRequestPermission: () -> Unit,
    trackingViewModel: TrackingViewModel = viewModel()
) {
    val data by trackingViewModel.state.collectAsState()

    if (!hasLocationPermission) {
        PermissionPrompt(onRequestPermission)
        return
    }

    val routeLatLng = remember(data.route.size) {
        data.route.map { LatLng(it.latitude, it.longitude) }
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-6.2088, 106.8456), 15f) // default: Jakarta
    }

    // Keep the camera centered on the latest point.
    androidx.compose.runtime.LaunchedEffect(routeLatLng.lastOrNull()) {
        routeLatLng.lastOrNull()?.let {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(it, 16f)
        }
    }

    var showSaveDialog by remember { mutableStateOf(false) }
    val finished = !data.isTracking && (data.elapsedMillis > 0 || data.route.isNotEmpty())

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission)
        ) {
            if (routeLatLng.size >= 2) {
                Polyline(
                    points = routeLatLng,
                    color = MaterialTheme.colorScheme.primary,
                    width = 14f
                )
            }
        }

        // Stats + controls panel.
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatBlock(
                    label = "DISTANCE",
                    value = LocationUtils.formatDistanceKm(data.distanceMeters),
                    unit = "km"
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SmallStat("TIME", LocationUtils.formatDuration(data.elapsedMillis))
                    SmallStat("PACE", LocationUtils.formatPace(currentPace(data.distanceMeters, data.elapsedMillis)))
                    SmallStat("SPEED", "%.1f km/h".format(data.currentSpeedKmh))
                }
                Spacer(Modifier.height(20.dp))
                Controls(
                    isTracking = data.isTracking,
                    isPaused = data.isPaused,
                    finished = finished,
                    onStart = { trackingViewModel.start() },
                    onPause = { trackingViewModel.pause() },
                    onResume = { trackingViewModel.resume() },
                    onStop = {
                        trackingViewModel.stop()
                        showSaveDialog = true
                    }
                )
            }
        }
    }

    if (showSaveDialog && finished) {
        SaveActivityDialog(
            onSave = { title, type ->
                trackingViewModel.saveActivity(title, type)
                showSaveDialog = false
            },
            onDiscard = {
                trackingViewModel.discard()
                showSaveDialog = false
            }
        )
    }
}

private fun currentPace(distanceMeters: Double, elapsedMillis: Long): Long {
    val km = distanceMeters / 1000.0
    return if (km > 0) (elapsedMillis / 1000.0 / km).toLong() else 0L
}

@Composable
private fun SmallStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun Controls(
    isTracking: Boolean,
    isPaused: Boolean,
    finished: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    when {
        finished -> {
            Text("Activity finished — save it below.", style = MaterialTheme.typography.bodyMedium)
        }
        !isTracking -> {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text("  START", fontWeight = FontWeight.Bold)
            }
        }
        else -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = if (isPaused) onResume else onPause,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Icon(
                        if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = null
                    )
                    Text(if (isPaused) "  RESUME" else "  PAUSE")
                }
                Button(
                    onClick = onStop,
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020))
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Text("  STOP")
                }
            }
        }
    }
}

@Composable
private fun SaveActivityDialog(
    onSave: (title: String, type: String) -> Unit,
    onDiscard: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Run") }
    val types = listOf("Run", "Ride", "Walk")

    AlertDialog(
        onDismissRequest = { /* force a choice */ },
        title = { Text("Save activity") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("Type", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { t ->
                        TextButton(onClick = { type = t }) {
                            Text(
                                t,
                                fontWeight = if (t == type) FontWeight.Bold else FontWeight.Normal,
                                color = if (t == type) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, type) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDiscard) { Text("Discard") }
        }
    )
}

@Composable
private fun PermissionPrompt(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Location needed",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "We use your location to record the route, distance and pace of your activity.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequestPermission) { Text("Grant location") }
    }
}
