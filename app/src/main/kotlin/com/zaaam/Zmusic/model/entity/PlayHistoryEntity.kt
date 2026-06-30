package com.zaaam.Zmusic.model.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// FIX #4: Index pada songId dan playedAt untuk query performance.
// Banyak query WHERE/GROUP BY songId (getMostFrequentMoodForSong, getLastPlayedAt,
// deleteBySongId, getRecentlyPlayed) yang sebelumnya full table scan.
@Entity(
    tableName = "play_history",
    indices = [
        Index(value = ["songId"]),
        Index(value = ["playedAt"])
    ]
)
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val playedAt: Long = System.currentTimeMillis(),
    val durationListened: Long,
    val mood: String? = null,
    val sourceQuery: String? = null
)

data class TopSongResult(
    val songId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val playCount: Int
)

data class RecentSongResult(
    val songId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val lastPlayed: Long,
    val playCount: Int,
    val totalListened: Long
)

// FIX #3: Model baru untuk JOIN query — menggantikan N+1 pattern
data class RecentSongWithDuration(
    val songId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val lastPlayed: Long,
    val playCount: Int,
    val totalListened: Long,
    val originalDuration: Long?
)

data class TopArtistResult(
    val artist: String,
    val playCount: Int
)

data class MoodStatResult(
    val mood: String,
    val playCount: Int
)

data class SongMoodHistoryResult(
    val songId: String,
    val mood: String,
    val moodCount: Int
)
