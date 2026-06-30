package com.zaaam.Zmusic.model.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.zaaam.Zmusic.model.Playlist
import com.zaaam.Zmusic.model.Song

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    indices = [Index(value = ["songId"])]    // FIX: cegah full-table-scan saat Room JOIN playlist
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: String,
    val position: Int
)

data class PlaylistWithSongs(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistSongCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "songId"
        )
    )
    val songs: List<SongEntity>
)

fun PlaylistWithSongs.toPlaylist() = Playlist(
    id = playlist.id,
    name = playlist.name,
    createdAt = playlist.createdAt,
    songs = songs.map { it.toSong() }
)

fun PlaylistEntity.toPlaylist(songs: List<Song> = emptyList()) = Playlist(
    id = id,
    name = name,
    createdAt = createdAt,
    songs = songs
)
