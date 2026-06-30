package com.zaaam.Zmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Tampilan empty/error konsisten gaya mockup 09:
 * ikon kotak gradient → judul → copy yang ngarahin → tombol aksi (opsional).
 *
 * - Empty (isError=false): nuansa VIOLET — tenang, mengajak.
 * - Error (isError=true):  nuansa MAGENTA — menarik perhatian.
 *
 * Pemakaian:
 * ```
 * StateDisplay(
 *     icon = Icons.Default.MusicNote,
 *     title = "Belum ada lagu",
 *     message = "Cari lagu favoritmu dan mulai bangun koleksi pertamamu.",
 *     actionLabel = "Mulai cari",
 *     onAction = { ... }
 * )
 * ```
 */
@Composable
fun StateDisplay(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val tint = if (isError) Color(0xFFFF4D8D) else Color(0xFFB66BFF)

    Column(
        modifier = modifier.padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(tint.copy(alpha = 0.30f), Color(0xFF1E1828)),
                        radius = 220f
                    )
                )
                .border(
                    0.8.dp,
                    tint.copy(alpha = if (isError) 0.30f else 0.18f),
                    RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(38.dp))
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text      = title,
            style     = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text      = message,
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .let {
                        if (isError) it
                            .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.05f))
                        else it.background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFF9D5C), Color(0xFFFF4D8D))
                            )
                        )
                    }
                    .clickable { onAction() }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text       = actionLabel,
                    style      = MaterialTheme.typography.titleSmall,
                    color      = if (isError) Color(0xFFF3EEFB) else Color(0xFF0C0A12)
                )
            }
        }
    }
}
