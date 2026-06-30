package com.stravamusic.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stravamusic.app.data.local.ActivityEntity
import com.stravamusic.app.tracking.LocationUtils
import com.stravamusic.app.ui.history.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    onOpenActivity: (Long) -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val activities by viewModel.activities.collectAsState()

    if (activities.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No activities yet.\nGo record your first one!",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(activities, key = { it.id }) { activity ->
            ActivityCard(activity = activity, onClick = { onOpenActivity(activity.id) })
        }
    }
}

@Composable
private fun ActivityCard(activity: ActivityEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = iconFor(activity.type),
                contentDescription = activity.type,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(activity.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    dateFormat.format(Date(activity.startTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    MiniStat("${LocationUtils.formatDistanceKm(activity.distanceMeters)} km")
                    MiniStat(LocationUtils.formatDuration(activity.durationMillis))
                    MiniStat("${LocationUtils.formatPace(activity.avgPaceSecPerKm)} /km")
                }
            }
        }
    }
}

@Composable
private fun MiniStat(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

private fun iconFor(type: String) = when (type.lowercase()) {
    "ride" -> Icons.Filled.DirectionsBike
    "walk" -> Icons.Filled.DirectionsWalk
    else -> Icons.Filled.DirectionsRun
}

private val dateFormat = SimpleDateFormat("EEE, d MMM yyyy • HH:mm", Locale.getDefault())
