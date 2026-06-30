package com.zaaam.Zmusic.ui.tracking

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.zaaam.Zmusic.tracking.TrackingData
import com.zaaam.Zmusic.util.LocationUtils

@Composable
fun RecordScreen(viewModel: RecordViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val data by viewModel.state.collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasPermission = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    val requestPermissions = {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    if (!hasPermission) {
        PermissionPrompt(onRequest = requestPermissions)
        return
    }

    RecordContent(
        data = data,
        onStart = viewModel::start,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onStop = viewModel::stop,
        onSave = { title -> viewModel.saveActivity(title) },
        onDiscard = viewModel::discard
    )
}

@Composable
private fun RecordContent(
    data: TrackingData,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSave: (String) -> Unit,
    onDiscard: () -> Unit
) {
    val routeLatLng = remember(data.route.size) {
        data.route.map { LatLng(it.latitude, it.longitude) }
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-6.2088, 106.8456), 15f) // default: Jakarta
    }
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
            properties = MapProperties(isMyLocationEnabled = true)
        ) {
            if (routeLatLng.size >= 2) {
                Polyline(
                    points = routeLatLng,
                    color = MaterialTheme.colorScheme.primary,
                    width = 14f
                )
            }
        }

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
                Text(
                    text = LocationUtils.formatDistanceKm(data.distanceMeters),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("KILOMETER", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SmallStat("WAKTU", LocationUtils.formatDuration(data.elapsedMillis))
                    SmallStat("PACE", "${LocationUtils.formatPace(currentPace(data.distanceMeters, data.elapsedMillis))} /km")
                    SmallStat("KECEPATAN", "%.1f km/j".format(data.currentSpeedKmh))
                }
                Spacer(Modifier.height(20.dp))
                Controls(
                    isTracking = data.isTracking,
                    isPaused = data.isPaused,
                    finished = finished,
                    onStart = onStart,
                    onPause = onPause,
                    onResume = onResume,
                    onStop = {
                        onStop()
                        showSaveDialog = true
                    }
                )
            }
        }
    }

    if (showSaveDialog && finished) {
        SaveActivityDialog(
            onSave = { title ->
                onSave(title)
                showSaveDialog = false
            },
            onDiscard = {
                onDiscard()
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
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelMedium)
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
            Text(
                "Aktivitas selesai — simpan di bawah.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        !isTracking -> {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text("  MULAI", fontWeight = FontWeight.Bold)
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
                    Text(if (isPaused) "  LANJUT" else "  JEDA")
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
    onSave: (String) -> Unit,
    onDiscard: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { /* paksa pilih */ },
        title = { Text("Simpan aktivitas") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Judul (opsional)") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { Button(onClick = { onSave(title) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDiscard) { Text("Buang") } }
    )
}

@Composable
private fun PermissionPrompt(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Butuh izin lokasi", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Lokasi dipakai untuk merekam rute, jarak, dan pace lari kamu.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest) { Text("Izinkan lokasi") }
    }
}
