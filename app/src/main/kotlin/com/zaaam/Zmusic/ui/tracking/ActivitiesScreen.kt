package com.zaaam.Zmusic.ui.tracking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.Zmusic.model.entity.ActivityEntity
import com.zaaam.Zmusic.util.LocationUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivitiesScreen(
    onOpenActivity: (Long) -> Unit,
    viewModel: ActivitiesViewModel = hiltViewModel()
) {
    val activities by viewModel.activities.collectAsState()

    if (activities.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "Belum ada aktivitas.\nMulai lari pertamamu di tab Rekam!",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
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
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.DirectionsRun,
                contentDescription = activity.type,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(activity.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    dateFormat.format(Date(activity.startTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text("${LocationUtils.formatDistanceKm(activity.distanceMeters)} km",
                        style = MaterialTheme.typography.bodyMedium)
                    Text(LocationUtils.formatDuration(activity.durationMillis),
                        style = MaterialTheme.typography.bodyMedium)
                    Text("${LocationUtils.formatPace(activity.avgPaceSecPerKm)} /km",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private val dateFormat = SimpleDateFormat("EEE, d MMM yyyy • HH:mm", Locale.getDefault())
