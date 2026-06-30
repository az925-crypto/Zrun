package com.zaaam.Zmusic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zaaam.Zmusic.model.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(song: SongEntity)

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getById(id: String): SongEntity?

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteById(id: String)

    // NEW: offline support
    @Query("UPDATE songs SET localPath = :localPath WHERE id = :id")
    suspend fun updateLocalPath(id: String, localPath: String?)

    @Query("SELECT localPath FROM songs WHERE id = :id")
    suspend fun getLocalPath(id: String): String?

    @Query("SELECT * FROM songs WHERE localPath IS NOT NULL")
    fun getDownloadedSongs(): Flow<List<SongEntity>>

    // FIX #3: Ambil durasi asli lagu dari songs table.
    // Digunakan oleh MusicRepository.getRecentSongs() agar duration yang ditampilkan
    // adalah durasi lagu sebenarnya, bukan totalListened dari play_history.
    @Query("SELECT duration FROM songs WHERE id = :id")
    suspend fun getDuration(id: String): Long?
}
