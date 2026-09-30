package com.zaaam.Zmusic.ui.zrun.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.zaaam.Zmusic.model.GeoPoint
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.tracking.TrackingData
import com.zaaam.Zmusic.ui.player.PlayerViewModel
import com.zaaam.Zmusic.ui.tracking.RecordViewModel
import com.zaaam.Zmusic.ui.zrun.EmberButton
import com.zaaam.Zmusic.ui.zrun.ZR
import com.zaaam.Zmusic.ui.zrun.ZRArt
import com.zaaam.Zmusic.ui.zrun.ZRBigNumber
import com.zaaam.Zmusic.ui.zrun.ZREqBars
import com.zaaam.Zmusic.ui.zrun.ZRIcons
import com.zaaam.Zmusic.ui.zrun.ZRRow
import com.zaaam.Zmusic.ui.zrun.fmtKm1
import com.zaaam.Zmusic.ui.zrun.fmtPaceQuote
import com.zaaam.Zmusic.ui.zrun.zStyle
import com.zaaam.Zmusic.util.LocationUtils
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

private val MAP_STYLE = """
[
  {"elementType":"geometry","stylers":[{"color":"#0f1217"}]},
  {"elementType":"labels","stylers":[{"visibility":"off"}]},
  {"featureType":"road","elementType":"geometry","stylers":[{"color":"#1d222b"}]},
  {"featureType":"water","elementType":"geometry","stylers":[{"color":"#0b0e13"}]},
  {"featureType":"poi","stylers":[{"visibility":"off"}]},
  {"featureType":"transit","stylers":[{"visibility":"off"}]}
]
""".trimIndent()

private val PEEK_H = 104.dp

@Composable
fun RunScreen(
    player: PlayerViewModel,
    onClose: () -> Unit,
    onOpenMusic: () -> Unit = {},
    onOpenPlayer: () -> Unit = {},
    vm: RecordViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val data by vm.state.collectAsState()
    val queue by player.queueManager.queue.collectAsState()
    val idx by player.queueManager.currentIndex.collectAsState()
    val song = queue.getOrNull(idx)
    val isPlaying by player.isPlaying.collectAsState()

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

    if (!hasPerm) {
        PermContent(onGrant = {
            val perms = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            launcher.launch(perms.toTypedArray())
        })
        return
    }

    val finished = !data.isTracking && (data.elapsedMillis > 0 || data.route.isNotEmpty())
    var showSave by remember { mutableStateOf(false) }

    RunContent(
        data = data,
        song = song,
        isPlaying = isPlaying,
        onMusic = { if (song != null) onOpenPlayer() else onOpenMusic() },
        onMain = {
            when {
                finished -> { /* menunggu simpan */ }
                !data.isTracking -> vm.start()
                data.isPaused -> vm.resume()
                else -> vm.pause()
            }
        },
        onStop = { vm.stop(); showSave = true }
    )

    if (showSave && finished) {
        SaveDialog(
            onSave = { title -> vm.saveActivity(title) { showSave = false; onClose() } },
            onDiscard = { vm.discard(); showSave = false; onClose() },
            onCancel = { showSave = false }
        )
    }
}

@Composable
private fun RunContent(
    data: TrackingData,
    song: Song?,
    isPlaying: Boolean,
    onMusic: () -> Unit,
    onMain: () -> Unit,
    onStop: () -> Unit
) {
    val routeLatLng = remember(data.route.size) {
        data.route.map { LatLng(it.latitude, it.longitude) }
    }
    val cam = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-6.2088, 106.8456), 15f)
    }
    androidx.compose.runtime.LaunchedEffect(routeLatLng.lastOrNull()) {
        routeLatLng.lastOrNull()?.let { cam.position = CameraPosition.fromLatLngZoom(it, 16f) }
    }
    val density = LocalDensity.current
    val polyWidth = remember { with(density) { 5.dp.toPx() } }
    // Ikon titik posisi: lingkaran putih solid diameter 14dp (ukuran tetap di semua zoom)
    val dotPx = remember(density) { with(density) { 14.dp.roundToPx() } }
    val dotIcon = remember(dotPx) {
        val bmp = Bitmap.createBitmap(dotPx, dotPx, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        val paint = Paint().apply { isAntiAlias = true; color = android.graphics.Color.WHITE }
        val r = dotPx / 2f
        canvas.drawCircle(r, r, r, paint)
        BitmapDescriptorFactory.fromBitmap(bmp)
    }
    val markerState = remember { MarkerState(position = LatLng(-6.2088, 106.8456)) }
    val lastPoint = routeLatLng.lastOrNull()
    androidx.compose.runtime.LaunchedEffect(lastPoint) {
        if (lastPoint != null) markerState.position = lastPoint
    }

    val seeking = data.route.isEmpty()
    val gpsText = if (seeking) "Mencari GPS" else "GPS aktif"
    val statusText = when {
        data.isTracking && !data.isPaused -> "Merekam"
        data.isPaused -> "Dijeda"
        data.elapsedMillis > 0 -> "Selesai"
        else -> "Siap"
    }
    val moving = if (data.movingMillis > 0) data.movingMillis else data.elapsedMillis
    val paceQ = fmtPaceQuote(pace(data.distanceMeters, moving))

    BoxWithConstraints(Modifier.fillMaxSize().background(ZR.Bg)) {
        val maxSheetH = maxHeight * 0.55f
        val scope = rememberCoroutineScope()
        var expanded by rememberSaveable { mutableStateOf(false) }
        val anim = remember { Animatable(PEEK_H, Dp.VectorConverter) }
        val sheetH = anim.value

        fun snapTo(target: Boolean) {
            expanded = target
            scope.launch {
                anim.animateTo(
                    if (target) maxSheetH else PEEK_H,
                    spring(stiffness = Spring.StiffnessMedium)
                )
            }
        }
        val drag = rememberDraggableState { delta ->
            scope.launch {
                anim.snapTo((anim.value + with(density) { delta.toDp() }).coerceIn(PEEK_H, maxSheetH))
            }
        }

        // Peta mengisi seluruh layar di belakang sheet
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cam,
            properties = MapProperties(
                isMyLocationEnabled = false,
                mapStyleOptions = MapStyleOptions(MAP_STYLE)
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false
            ),
            contentPadding = PaddingValues(bottom = sheetH)
        ) {
            if (routeLatLng.size >= 2) {
                Polyline(
                    points = routeLatLng,
                    color = ZR.Ember,
                    width = polyWidth,
                    jointType = JointType.ROUND,
                    startCap = RoundCap(),
                    endCap = RoundCap()
                )
            }
            routeLatLng.lastOrNull()?.let {
                Marker(
                    state = markerState,
                    icon = dotIcon,
                    anchor = Offset(0.5f, 0.5f),
                    flat = false,
                    zIndex = 2f
                )
            }
        }

        // Dua pil melayang
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 10.dp, start = 14.dp, end = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatusPill(text = gpsText, dot = if (seeking) ZR.Mut else ZR.Mint)
            StatusPill(text = statusText, dot = ZR.Stop)
        }

        // Sheet bawah: collapsed 104dp, expanded sampai 55% layar
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(sheetH)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(ZR.S1)
                .navigationBarsPadding()
        ) {
            // Header = area drag + tap (handle + zona 32dp / baris ringkas)
            Column(
                Modifier
                    .fillMaxWidth()
                    .draggable(
                        state = drag,
                        orientation = Orientation.Vertical,
                        onDragStopped = { v ->
                            snapTo(
                                when {
                                    v < -500f -> true
                                    v > 500f -> false
                                    else -> anim.value > (PEEK_H + maxSheetH) / 2
                                }
                            )
                        }
                    )
                    .clickable { snapTo(!expanded) }
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp, bottom = 10.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF3A3F4A))
                )
                if (!expanded) {
                    CollapsedRow(
                        data = data,
                        paceQ = paceQ,
                        onMain = onMain,
                        onStop = onStop
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text("Jarak", style = zStyle(12.sp, FontWeight.Normal), color = ZR.Mut)
                    }
                }
            }
            if (expanded) {
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 16.dp)
                ) {
                    ZRBigNumber(fmtKm1(data.distanceMeters), "km", size = 66.sp)
                    if (seeking) {
                        Text(
                            "Mencari sinyal GPS. Tunggu sampai status berubah jadi GPS aktif.",
                            style = zStyle(12.sp, FontWeight.Normal),
                            color = ZR.Mut,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RunStat(
                            LocationUtils.formatDuration(data.elapsedMillis), "Durasi",
                            Modifier.weight(1f)
                        )
                        RunStat(paceQ, "Pace", Modifier.weight(1f))
                        RunStat(
                            NumberFormat.getNumberInstance(Locale("id", "ID")).apply {
                                maximumFractionDigits = 1; minimumFractionDigits = 1
                            }.format(data.currentSpeedKmh),
                            "km/jam",
                            Modifier.weight(1f)
                        )
                    }
                    if (song != null) {
                        ZRRow {
                            ZRArt(seed = song.id, thumbnailUrl = song.thumbnailUrl, size = 40.dp, radius = 12.dp)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    song.title,
                                    style = zStyle(14.sp, FontWeight.SemiBold),
                                    color = ZR.Tx, maxLines = 1
                                )
                                Text(
                                    song.artist,
                                    style = zStyle(12.sp, FontWeight.Normal),
                                    color = ZR.Mut, maxLines = 1
                                )
                            }
                            if (isPlaying) ZREqBars()
                        }
                    } else {
                        ZRRow(onClick = onMusic) {
                            ZRArt(seed = "kosong", size = 40.dp, radius = 12.dp)
                            Text(
                                "Pilih soundtrack lari",
                                style = zStyle(14.sp, FontWeight.SemiBold),
                                color = ZR.Tx,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(ZRIcons.Music, contentDescription = "Musik", tint = ZR.Mut, modifier = Modifier.size(20.dp))
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp)
                            .padding(top = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(ZR.S2)
                                .clickable(onClick = onMusic),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(ZRIcons.Music, contentDescription = "Musik", tint = ZR.Tx, modifier = Modifier.size(20.dp))
                        }
                        val recording = data.isTracking && !data.isPaused
                        Box(
                            Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable(onClick = onMain),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (recording) ZRIcons.Pause else ZRIcons.Play,
                                contentDescription = if (recording) "Jeda" else "Mulai",
                                tint = Color(0xFF14161B),
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .border(2.dp, ZR.Stop, CircleShape)
                                .clickable(onClick = onStop),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ZR.Stop)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsedRow(
    data: TrackingData,
    paceQ: String,
    onMain: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row {
                Text(
                    fmtKm1(data.distanceMeters),
                    style = zStyle(28.sp, FontWeight.Bold),
                    color = ZR.Tx,
                    modifier = Modifier.alignByBaseline()
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "km",
                    style = zStyle(14.sp, FontWeight.Medium),
                    color = ZR.Mut,
                    modifier = Modifier.alignByBaseline()
                )
            }
            Text(
                "${LocationUtils.formatDuration(data.elapsedMillis)} · $paceQ",
                style = zStyle(12.sp, FontWeight.Normal),
                color = ZR.Mut
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val recording = data.isTracking && !data.isPaused
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onMain),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (recording) ZRIcons.Pause else ZRIcons.Play,
                    contentDescription = if (recording) "Jeda" else "Mulai",
                    tint = Color(0xFF14161B),
                    modifier = Modifier.size(22.dp)
                )
            }
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(2.dp, ZR.Stop, CircleShape)
                    .clickable(onClick = onStop),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(ZR.Stop)
                )
            }
        }
    }
}

@Composable
private fun RunStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = zStyle(20.sp, FontWeight.Bold), color = ZR.Tx)
        Spacer(Modifier.height(2.dp))
        Text(label, style = zStyle(12.sp, FontWeight.Normal), color = ZR.Mut)
    }
}

@Composable
private fun StatusPill(text: String, dot: Color) {
    Row(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xB80A0B0E))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
        Text(text, style = zStyle(11.5.sp, FontWeight.SemiBold), color = ZR.Tx)
    }
}

@Composable
private fun PermContent(onGrant: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(ZR.Bg)
            .statusBarsPadding()
            .padding(top = 10.dp)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Butuh izin lokasi", style = zStyle(26.sp, FontWeight.Bold), color = ZR.Tx)
        Spacer(Modifier.height(8.dp))
        Text(
            "Lokasi dipakai buat rekam rute, jarak dan pace larimu.",
            style = zStyle(13.sp, FontWeight.Normal),
            color = ZR.Mut, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        EmberButton("Izinkan lokasi", onClick = onGrant)
    }
}

@Composable
private fun SaveDialog(onSave: (String) -> Unit, onDiscard: () -> Unit, onCancel: () -> Unit) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = ZR.S1,
        shape = RoundedCornerShape(26.dp),
        title = { Text("Simpan aktivitas", style = zStyle(15.sp, FontWeight.SemiBold), color = ZR.Tx) },
        text = {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("Judul (opsional)") }, modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Box(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(ZR.Ember)
                    .clickable { onSave(title) }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text("Simpan", style = zStyle(14.sp, FontWeight.Bold), color = Color.White)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Buang",
                    style = zStyle(13.sp, FontWeight.SemiBold),
                    color = ZR.Stop,
                    modifier = Modifier.clickable(onClick = onDiscard).padding(10.dp)
                )
                Text(
                    "Batal",
                    style = zStyle(13.sp, FontWeight.SemiBold),
                    color = ZR.Mut,
                    modifier = Modifier.clickable(onClick = onCancel).padding(10.dp)
                )
            }
        }
    )
}

private fun pace(meters: Double, ms: Long): Long {
    val km = meters / 1000.0
    return if (km > 0) (ms / 1000.0 / km).toLong() else 0L
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0B0E, widthDp = 360, heightDp = 780)
@Composable
private fun RunPreview() {
    val route = listOf(
        GeoPoint(-6.2088, 106.8456, 1L),
        GeoPoint(-6.2098, 106.8466, 2L),
        GeoPoint(-6.2108, 106.8476, 3L),
        GeoPoint(-6.2118, 106.8466, 4L)
    )
    RunContent(
        data = TrackingData(
            isTracking = true, isPaused = false,
            elapsedMillis = 1_669_000L, movingMillis = 1_600_000L,
            distanceMeters = 5_200.0, currentSpeedKmh = 11.2, route = route
        ),
        song = null, isPlaying = false,
        onMusic = {}, onMain = {}, onStop = {}
    )
}
