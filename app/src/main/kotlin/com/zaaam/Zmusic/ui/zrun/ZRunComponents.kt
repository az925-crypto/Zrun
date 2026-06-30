package com.zaaam.Zmusic.ui.zrun

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zaaam.Zmusic.model.Song

/** Tombol utama dengan gradient Ember + glow. */
@Composable
fun EmberButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Int = 54,
    brush: Brush = ZR.Ember,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(height.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(9.dp))
        }
        Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ZR.S2)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = ZR.Tx, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun ZRCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(ZR.S1)
    ) { content() }
}

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier, accent: Color = ZR.Tx) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(ZR.S1)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, letterSpacing = (-1).sp)
        Spacer(Modifier.height(3.dp))
        Text(label, color = ZR.Mut, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.6.sp)
    }
}

/** Cincin progress dengan gradient Ember. */
@Composable
fun ProgressRing(progress: Float, size: Int = 84, stroke: Float = 9f, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size.dp)) {
        val sw = stroke.dp.toPx()
        drawArc(
            color = ZR.S3,
            startAngle = -90f, sweepAngle = 360f, useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = sw, cap = StrokeCap.Round)
        )
        drawArc(
            brush = ZR.Ember,
            startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f), useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = sw, cap = StrokeCap.Round)
        )
    }
}

/** Kotak artwork — pakai thumbnail Coil kalau ada, fallback gradient. */
@Composable
fun ArtBox(seed: String, thumbnailUrl: String?, size: Int, corner: Int = 12, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(corner.dp))
            .background(ZR.artBrush(seed)),
        contentAlignment = Alignment.Center
    ) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().size(size.dp).clip(RoundedCornerShape(corner.dp))
            )
        } else {
            Icon(Icons.Filled.MusicNote, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size((size / 2.4f).dp))
        }
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = ZR.Tx, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, letterSpacing = (-0.2).sp)
        if (action != null) {
            Text(action, color = ZR.Mut, fontSize = 12.5.sp,
                modifier = Modifier.clickable(enabled = onAction != null) { onAction?.invoke() })
        }
    }
}

@Composable
fun SongRow(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    index: Int? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (index != null) {
            Text("$index", color = ZR.Faint, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                modifier = Modifier.width(20.dp))
        }
        ArtBox(seed = song.id, thumbnailUrl = song.thumbnailUrl, size = 44)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                color = if (isPlaying) ZR.Mint else ZR.Tx,
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(song.artist, color = ZR.Mut, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (isPlaying) {
            Icon(Icons.Filled.MusicNote, null, tint = ZR.Mint, modifier = Modifier.size(18.dp))
        }
    }
}

/** Mini-player melayang di atas bottom-nav. */
@Composable
fun ZRMiniPlayer(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF241A2E), Color(0xFF2A1622))))
                .clickable(onClick = onClick)
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArtBox(seed = song.id, thumbnailUrl = song.thumbnailUrl, size = 38, corner = 10)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(song.title, color = ZR.Tx, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, color = ZR.Mut, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    "play/pause", tint = ZR.Tx,
                    modifier = Modifier.size(28.dp).clip(CircleShape).clickable { onPlayPause() }
                )
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.SkipNext, "next", tint = ZR.Tx,
                    modifier = Modifier.size(26.dp).clip(CircleShape).clickable { onNext() })
            }
            // progress garis tipis di bawah
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(2.dp)
                    .background(ZR.Ember)
            )
        }
    }
}
