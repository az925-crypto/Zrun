package com.zaaam.Zmusic.ui.zrun.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.zaaam.Zmusic.ui.tracking.RecordViewModel
import com.zaaam.Zmusic.ui.zrun.EmberButton
import com.zaaam.Zmusic.ui.zrun.StatTile
import com.zaaam.Zmusic.ui.zrun.ZR
import com.zaaam.Zmusic.util.LocationUtils

@Composable
fun RunScreen(onClose: () -> Unit, vm: RecordViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val data by vm.state.collectAsState()

    var hasPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        hasPerm = res[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            res[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    val requestPerm = {
        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        launcher.launch(perms.toTypedArray())
    }

    val routeLatLng = remember(data.route.size) { data.route.map { LatLng(it.latitude, it.longitude) } }
    val cam = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-6.2088, 106.8456), 15f)
    }
    androidx.compose.runtime.LaunchedEffect(routeLatLng.lastOrNull()) {
        routeLatLng.lastOrNull()?.let { cam.position = CameraPosition.fromLatLngZoom(it, 16f) }
    }

    val finished = !data.isTracking && (data.elapsedMillis > 0 || data.route.isNotEmpty())
    var showSave by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(ZR.Bg)) {
        if (hasPerm) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cam,
                properties = MapProperties(isMyLocationEnabled = true)
            ) {
                if (routeLatLng.size >= 2) Polyline(points = routeLatLng, color = ZR.Ember2, width = 16f)
            }
        }

        // tombol close
        Box(
            Modifier.statusBarsPadding().padding(14.dp).size(40.dp).clip(CircleShape)
                .background(Color(0xAA15171C)).clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Close, "tutup", tint = ZR.Tx) }

        if (!hasPerm) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Butuh izin lokasi", color = ZR.Tx, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                Text("Lokasi dipakai buat rekam rute, jarak & pace lari kamu.",
                    color = ZR.Mut, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                EmberButton("Izinkan lokasi", onClick = requestPerm)
            }
            return@Box
        }

        // metric besar di atas
        Column(
            Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(LocationUtils.formatDistanceKm(data.distanceMeters), color = ZR.Tx,
                fontWeight = FontWeight.Black, fontSize = 72.sp, letterSpacing = (-2).sp)
            Text("KILOMETER", color = ZR.Mut, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 2.sp)
        }

        // sheet bawah
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(ZR.S1).padding(16.dp).padding(bottom = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(LocationUtils.formatDuration(data.elapsedMillis), "WAKTU", Modifier.weight(1f))
                StatTile("${LocationUtils.formatPace(pace(data.distanceMeters, data.elapsedMillis))}", "PACE /KM", Modifier.weight(1f), ZR.Mint)
                StatTile("%.1f".format(data.currentSpeedKmh), "KM/J", Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            when {
                finished -> {
                    Text("Lari selesai 🎉 simpan?", color = ZR.Tx, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(16.dp)).background(ZR.S2)
                            .clickable { vm.discard(); onClose() }, contentAlignment = Alignment.Center) {
                            Text("Buang", color = ZR.Tx, fontWeight = FontWeight.Bold)
                        }
                        EmberButton("Simpan", Modifier.weight(1f)) { showSave = true }
                    }
                }
                !data.isTracking -> {
                    EmberButton("MULAI", Modifier.fillMaxWidth().height(58.dp), Icons.Filled.PlayArrow) { vm.start() }
                }
                else -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(16.dp)).background(ZR.S2)
                            .clickable { if (data.isPaused) vm.resume() else vm.pause() }, contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (data.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause, null, tint = ZR.Tx)
                                Spacer(Modifier.width(6.dp))
                                Text(if (data.isPaused) "LANJUT" else "JEDA", color = ZR.Tx, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(16.dp))
                            .background(Color(0x29FF3B5C)).clickable { vm.stop() }, contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Stop, null, tint = ZR.Stop)
                                Spacer(Modifier.width(6.dp))
                                Text("SELESAI", color = ZR.Stop, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (showSave && finished) {
            SaveDialog(onSave = { title -> vm.saveActivity(title) { showSave = false; onClose() } },
                onCancel = { showSave = false })
        }
    }
}

private fun pace(meters: Double, ms: Long): Long {
    val km = meters / 1000.0
    return if (km > 0) (ms / 1000.0 / km).toLong() else 0L
}

@Composable
private fun SaveDialog(onSave: (String) -> Unit, onCancel: () -> Unit) {
    var title by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onCancel,
        containerColor = ZR.S2,
        title = { Text("Simpan aktivitas", color = ZR.Tx) },
        text = {
            OutlinedTextField(value = title, onValueChange = { title = it },
                label = { Text("Judul (opsional)") }, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(ZR.Ember).clickable { onSave(title) }
                .padding(horizontal = 18.dp, vertical = 10.dp)) { Text("Simpan", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { Text("Batal", color = ZR.Mut, modifier = Modifier.clickable(onClick = onCancel).padding(10.dp)) }
    )
}
