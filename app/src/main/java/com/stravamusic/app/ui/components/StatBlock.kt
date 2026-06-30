package com.stravamusic.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

/** A single big number with a small caption underneath, Strava-style. */
@Composable
fun StatBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (unit != null) "$label ($unit)" else label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

/** A row of three stat blocks. */
@Composable
fun StatRow(
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEach { (label, value) ->
            StatBlock(label = label, value = value)
        }
    }
}
