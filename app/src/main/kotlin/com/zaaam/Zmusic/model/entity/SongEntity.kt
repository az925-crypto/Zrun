package com.zaaam.Zmusic.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zaaam.Zmusic.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val duration: Long,
    val localPath: String? = null   // path file audio lokal untuk offline
)

fun SongEntity.toSong() = Song(
    id = id,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    duration = duration
)

// FIX: Default toEntity() tanpa localPath (untuk insertOrIgnore — aman, tidak overwrite)
fun Song.toEntity(localPath: String? = null) = SongEntity(
    id = id,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    duration = duration,
    localPath = localPath
)
