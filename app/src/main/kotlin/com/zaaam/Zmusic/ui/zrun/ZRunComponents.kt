package com.zaaam.Zmusic.ui.zrun

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.composed
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zaaam.Zmusic.data.MusicRepository
import com.zaaam.Zmusic.model.GeoPoint
import com.zaaam.Zmusic.model.Song

/** Tombol utama solid Ember, bentuk pil. */
@Composable
fun EmberButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Int = 56,
    fontSize: TextUnit = 16.sp,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(height.dp)
            .clip(RoundedCornerShape((height / 2).dp))
            .background(ZR.Ember)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = zStyle(fontSize, FontWeight.Bold), color = Color.White)
    }
}

@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(ZR.S2)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = zStyle(14.sp, FontWeight.Bold), color = ZR.Tx)
    }
}

/** Ikon dalam area sentuh 48dp. */
@Composable
fun ZRIconButton(
    icon: ImageVector,
    desc: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = ZR.Tx,
    iconSize: Dp = 20.dp
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Kotak art solid + lingkaran hitam 18% yang menyembul di kanan-bawah. */
@Composable
fun ZRArt(
    seed: String,
    size: Dp,
    radius: Dp,
    modifier: Modifier = Modifier,
    thumbnailUrl: String? = null
) {
    // Pop pegas saat artwork muncul/ganti (semangat, bukan fade)
    val pop = remember(seed) { Animatable(0.85f) }
    LaunchedEffect(seed) {
        pop.animateTo(1f, spring(stiffness = 900f, dampingRatio = 0.65f))
    }
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer(scaleX = pop.value, scaleY = pop.value)
            .clip(RoundedCornerShape(radius))
            .background(ZR.artColor(seed))
    ) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(RoundedCornerShape(radius))
            )
        } else {
            val dot = size * 0.7f
            Box(
                Modifier
                    .size(dot)
                    .align(Alignment.BottomEnd)
                    .offset(x = size * 0.2f, y = size * 0.2f)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.18f))
            )
        }
    }
}

/** Miniatur rute asli: polyline Ember di kotak S1. */
@Composable
fun ZRRouteThumb(
    route: List<GeoPoint>,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(ZR.S1),
        contentAlignment = Alignment.Center
    ) {
        if (route.isEmpty()) return@Box
        val density = LocalDensity.current
        Canvas(Modifier.size(size)) {
            val pad = with(density) { 8.dp.toPx() }
            val w = size.toPx() - pad * 2
            val h = size.toPx() - pad * 2
            val minLat = route.minOf { it.latitude }
            val maxLat = route.maxOf { it.latitude }
            val minLng = route.minOf { it.longitude }
            val maxLng = route.maxOf { it.longitude }
            val spanLat = (maxLat - minLat).takeIf { it > 0 } ?: 1.0
            val spanLng = (maxLng - minLng).takeIf { it > 0 } ?: 1.0
            val sw = with(density) { 2.6.dp.toPx() }
            if (route.size == 1 || (spanLat == 1.0 && spanLng == 1.0 && route.size < 2)) {
                drawCircle(color = ZR.Ember, radius = sw, center = center)
                return@Canvas
            }
            val path = Path()
            route.forEachIndexed { i, p ->
                val x = pad + ((p.longitude - minLng) / spanLng * w).toFloat()
                val y = pad + ((maxLat - p.latitude) / spanLat * h).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = ZR.Ember,
                style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

/** Angka besar + satuan di samping (semua Bricolage tnum, warna eksplisit). */
@Composable
fun ZRBigNumber(value: String, unit: String, size: TextUnit = 54.sp, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Text(
            value,
            style = zStyle(size, FontWeight.Bold, ls = (size.value * -0.04f).sp),
            color = ZR.Tx,
            modifier = Modifier.alignByBaseline()
        )
        Spacer(Modifier.width(5.dp))
        Text(
            " $unit".trimStart(),
            style = zStyle(16.sp, FontWeight.Medium),
            color = ZR.Mut,
            modifier = Modifier.alignByBaseline()
        )
    }
}

/** Outline putus-putus (untuk keadaan kosong): dashed 1.5dp. */
fun Modifier.dashedOutline(
    color: Color,
    stroke: Dp = 1.5.dp,
    radius: Dp = 8.dp,
    on: Float = 8f,
    off: Float = 6f
): Modifier = composed {
    val sw = with(LocalDensity.current) { stroke.toPx() }
    val rr = with(LocalDensity.current) { radius.toPx() }
    drawBehind {
        drawRoundRect(
            color = color,
            topLeft = Offset(sw / 2, sw / 2),
            size = Size(size.width - sw, size.height - sw),
            cornerRadius = CornerRadius(rr, rr),
            style = Stroke(
                width = sw,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(on, off), 0f)
            )
        )
    }
}

/** 3 batang equalizer statis warna Violet. */
@Composable
fun ZREqBars(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(0.6f, 1f, 0.4f).forEach { f ->
            Box(
                Modifier
                    .width(3.dp)
                    .height((16 * f).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ZR.Violet)
            )
        }
    }
}

/** Baris daftar: padding vertikal 9dp, gap 12dp, pemisah 1dp. */
@Composable
fun ZRRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
        if (showDivider) Divider(color = ZR.Divider, thickness = 1.dp)
    }
}

/** Pil kecil S2. */
@Composable
fun ZRChip(text: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ZR.S2)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = zStyle(12.sp, FontWeight.Medium), color = ZR.Tx)
    }
}

/** Bilah progres: track S2, isi sesuai fraksi. */
@Composable
fun ZRProgress(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
    fill: Color = ZR.Ember
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(ZR.S2)
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(height)
                .background(fill)
        )
    }
}

/** Judul kecil section (h3): 15sp/600, margin 18 atas / 6 bawah. */
@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = zStyle(15.sp, FontWeight.SemiBold), color = ZR.Tx)
        if (action != null) {
            Text(
                action,
                style = zStyle(13.sp, FontWeight.SemiBold),
                color = ZR.Ember,
                modifier = Modifier.clickable(enabled = onAction != null) { onAction?.invoke() }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    index: Int? = null,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (index != null) {
                Text(
                    fmtInt(index.toLong()),
                    style = zStyle(13.sp, FontWeight.Bold),
                    color = ZR.Faint,
                    modifier = Modifier.width(20.dp)
                )
            }
            ZRArt(seed = song.id, thumbnailUrl = song.thumbnailUrl, size = 48.dp, radius = 14.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    song.title,
                    style = zStyle(14.sp, FontWeight.SemiBold),
                    color = ZR.Tx,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist,
                    style = zStyle(12.sp, FontWeight.Normal),
                    color = ZR.Mut,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            if (isPlaying) ZREqBars()
        }
        if (showDivider) Divider(color = ZR.Divider, thickness = 1.dp)
    }
}

/** Jumlah lagu asli per playlist (dari repository, tanpa ubah ViewModel). */
@Composable
fun playlistSongCount(playlistId: Long, repo: MusicRepository): Int {
    val pws by repo.getPlaylistWithSongs(playlistId)
        .collectAsState(initial = null)
    return pws?.songs?.size ?: 0
}

/** Mini-player melayang di atas bottom bar. */
@Composable
fun ZRMiniPlayer(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    onPlayPause: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(ZR.S2)
            .border(1.dp, ZR.Stroke, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 14.dp)
                .height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ZRArt(seed = song.id, thumbnailUrl = song.thumbnailUrl, size = 40.dp, radius = 12.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    song.title,
                    style = zStyle(13.sp, FontWeight.Bold),
                    color = ZR.Tx,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist,
                    style = zStyle(12.sp, FontWeight.Normal),
                    color = ZR.Mut,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            ZRIconButton(
                icon = if (isPlaying) ZRIcons.Pause else ZRIcons.Play,
                desc = if (isPlaying) "Jeda" else "Putar",
                onClick = onPlayPause,
                tint = ZR.Tx
            )
        }
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(3.dp)
                .background(ZR.Ember)
        )
    }
}
