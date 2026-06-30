package com.stravamusic.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.stravamusic.app.StravaMusicApp
import com.stravamusic.app.data.local.ActivityEntity
import com.stravamusic.app.tracking.LocationUtils
import com.stravamusic.app.ui.components.StatBlock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    activityId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repo = (context.applicationContext as StravaMusicApp).activityRepository

    val activity by produceState<ActivityEntity?>(initialValue = null, activityId) {
        value = repo.getActivity(activityId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(activity?.title ?: "Activity") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val current = activity
        if (current == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val points = current.route.map { LatLng(it.latitude, it.longitude) }
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(
                    points.firstOrNull() ?: LatLng(-6.2088, 106.8456),
                    15f
                )
            }
            GoogleMap(
                modifier = Modifier.fillMaxWidth().height(320.dp),
                cameraPositionState = cameraPositionState
            ) {
                if (points.size >= 2) {
                    Polyline(
                        points = points,
                        color = MaterialTheme.colorScheme.primary,
                        width = 14f
                    )
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                StatBlock(
                    label = "DISTANCE",
                    value = LocationUtils.formatDistanceKm(current.distanceMeters),
                    unit = "km",
                    modifier = Modifier.fillMaxWidth()
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(20.dp))
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DetailStat("TIME", LocationUtils.formatDuration(current.durationMillis))
                    DetailStat("AVG PACE", "${LocationUtils.formatPace(current.avgPaceSecPerKm)} /km")
                    DetailStat("AVG SPEED", "%.1f km/h".format(current.avgSpeedKmh))
                }
            }
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
