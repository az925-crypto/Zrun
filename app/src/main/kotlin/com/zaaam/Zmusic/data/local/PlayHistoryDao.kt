package com.zaaam.Zmusic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zaaam.Zmusic.model.entity.MoodStatResult
import com.zaaam.Zmusic.model.entity.PlayHistoryEntity
import com.zaaam.Zmusic.model.entity.RecentSongResult
import com.zaaam.Zmusic.model.entity.RecentSongWithDuration
import com.zaaam.Zmusic.model.entity.SongMoodHistoryResult
import com.zaaam.Zmusic.model.entity.TopArtistResult
import com.zaaam.Zmusic.model.entity.TopSongResult
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: PlayHistoryEntity)

    @Query("SELECT COUNT(*) FROM play_history")
    fun getTotalPlays(): Flow<Int>

    @Query("SELECT SUM(durationListened) FROM play_history")
    fun getTotalDuration(): Flow<Long?>

    @Query("""
        SELECT songId, title, artist, thumbnailUrl, COUNT(*) as playCount 
        FROM play_history 
        GROUP BY songId 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    fun getTopSongs(limit: Int = 5): Flow<List<TopSongResult>>

    @Query("""
        SELECT songId, title, artist, thumbnailUrl, 
               MAX(playedAt) as lastPlayed,
               COUNT(*) as playCount, 
               SUM(durationListened) as totalListened
        FROM play_history
        GROUP BY songId
        ORDER BY lastPlayed DESC
        LIMIT :limit
    """)
    fun getRecentlyPlayed(limit: Int = 50): Flow<List<RecentSongResult>>

    // FIX #3: JOIN query — ambil duration asli dari songs table dalam 1 query
    @Query("""
        SELECT ph.songId, ph.title, ph.artist, ph.thumbnailUrl,
               MAX(ph.playedAt) as lastPlayed,
               COUNT(*) as playCount,
               SUM(ph.durationListened) as totalListened,
               s.duration as originalDuration
        FROM play_history ph
        LEFT JOIN songs s ON ph.songId = s.id
        GROUP BY ph.songId
        ORDER BY lastPlayed DESC
        LIMIT :limit
    """)
    fun getRecentlyPlayedWithDuration(limit: Int = 50): Flow<List<RecentSongWithDuration>>

    @Query("""
        SELECT artist, COUNT(*) as playCount 
        FROM play_history 
        GROUP BY artist 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    fun getTopArtists(limit: Int = 3): Flow<List<TopArtistResult>>

    // ── Mood Stats ─────────────────────────────────────────────────────────

    @Query("""
        SELECT mood, COUNT(*) as playCount 
        FROM play_history 
        WHERE mood IS NOT NULL 
        GROUP BY mood 
        ORDER BY playCount DESC
    """)
    fun getMoodDistribution(): Flow<List<MoodStatResult>>

    @Query("""
        SELECT mood, COUNT(*) as playCount 
        FROM (
            SELECT mood FROM play_history 
            WHERE mood IS NOT NULL 
            ORDER BY playedAt DESC 
            LIMIT 10
        ) 
        GROUP BY mood 
        ORDER BY playCount DESC 
        LIMIT 1
    """)
    fun getRecentMood(): Flow<MoodStatResult?>

    // ── Mood Intelligence Queries ─────────────────────────────────────────

    @Query("""
        SELECT songId, mood, COUNT(*) as moodCount
        FROM play_history
        WHERE songId = :songId AND mood IS NOT NULL
        GROUP BY mood
        ORDER BY moodCount DESC
        LIMIT 1
    """)
    suspend fun getMostFrequentMoodForSong(songId: String): SongMoodHistoryResult?

    @Query("""
        SELECT mood, COUNT(*) as playCount
        FROM play_history
        WHERE mood IS NOT NULL
          AND playedAt BETWEEN :sessionStart AND :sessionEnd
          AND songId != :excludeSongId
        GROUP BY mood
        ORDER BY playCount DESC
        LIMIT 1
    """)
    suspend fun getSessionMood(
        sessionStart: Long,
        sessionEnd: Long,
        excludeSongId: String
    ): MoodStatResult?

    @Query("SELECT MAX(playedAt) FROM play_history WHERE songId = :songId")
    suspend fun getLastPlayedAt(songId: String): Long?

    @Query("""
        SELECT mood, COUNT(*) as playCount
        FROM (
            SELECT mood FROM play_history
            WHERE mood IS NOT NULL
            ORDER BY playedAt DESC
            LIMIT :recentCount
        )
        GROUP BY mood
        ORDER BY playCount DESC
    """)
    suspend fun getRecentMoodPattern(recentCount: Int = 20): List<MoodStatResult>

    // ── Streak Query — NEW ─────────────────────────────────────────────────

    /**
     * Ambil semua tanggal unik (format "YYYY-MM-DD") saat user mendengarkan musik,
     * diurutkan dari yang terbaru. Digunakan MilestoneRepository untuk hitung streak.
     *
     * Pakai localtime agar menyesuaikan timezone perangkat user.
     * playedAt disimpan dalam milidetik → dibagi 1000 jadi detik untuk strftime.
     */
    @Query("""
        SELECT DISTINCT strftime('%Y-%m-%d', playedAt / 1000, 'unixepoch', 'localtime') as day
        FROM play_history
        ORDER BY day DESC
    """)
    fun getDistinctListeningDays(): Flow<List<String>>

    /**
     * Total play count sebagai suspend function — untuk cek milestone satu kali.
     */
    @Query("SELECT COUNT(*) FROM play_history")
    suspend fun getTotalPlaysOnce(): Int

    /**
     * Total durasi didengarkan dalam ms — untuk milestone jam mendengarkan.
     */
    @Query("SELECT COALESCE(SUM(durationListened), 0) FROM play_history")
    suspend fun getTotalDurationOnce(): Long

    /**
     * Jumlah artis unik — untuk milestone "kolektor artis".
     */
    @Query("SELECT COUNT(DISTINCT artist) FROM play_history")
    suspend fun getUniqueArtistCount(): Int

    // ── FIX POTENSI #2: Date-filtered queries untuk WrappedViewModel ──────
    //
    // MASALAH LAMA: WrappedViewModel memanggil getTotalPlays(), getTopSongs(),
    // dll. yang mengambil SEMUA data tanpa filter tanggal. Akibatnya
    // "Monthly Wrapped" dan "Yearly Wrapped" menampilkan data yang sama persis
    // — tidak ada perbedaan antara mode monthly vs yearly.
    //
    // FIX: Tambahkan versi query yang menerima startMs dan endMs (epoch ms)
    // sehingga WrappedViewModel bisa filter berdasarkan rentang bulan/tahun.

    @Query("SELECT COUNT(*) FROM play_history WHERE playedAt BETWEEN :startMs AND :endMs")
    fun getTotalPlaysBetween(startMs: Long, endMs: Long): Flow<Int>

    @Query("SELECT SUM(durationListened) FROM play_history WHERE playedAt BETWEEN :startMs AND :endMs")
    fun getTotalDurationBetween(startMs: Long, endMs: Long): Flow<Long?>

    @Query("""
        SELECT songId, title, artist, thumbnailUrl, COUNT(*) as playCount 
        FROM play_history 
        WHERE playedAt BETWEEN :startMs AND :endMs
        GROUP BY songId 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    fun getTopSongsBetween(startMs: Long, endMs: Long, limit: Int = 5): Flow<List<TopSongResult>>

    @Query("""
        SELECT artist, COUNT(*) as playCount 
        FROM play_history 
        WHERE playedAt BETWEEN :startMs AND :endMs
        GROUP BY artist 
        ORDER BY playCount DESC 
        LIMIT :limit
    """)
    fun getTopArtistsBetween(startMs: Long, endMs: Long, limit: Int = 3): Flow<List<TopArtistResult>>

    @Query("""
        SELECT mood, COUNT(*) as playCount 
        FROM play_history 
        WHERE mood IS NOT NULL
          AND playedAt BETWEEN :startMs AND :endMs
        GROUP BY mood 
        ORDER BY playCount DESC
    """)
    fun getMoodDistributionBetween(startMs: Long, endMs: Long): Flow<List<MoodStatResult>>

    // ── Smart Playlist Queries ────────────────────────────────────────────

    /**
     * Ambil lagu yang pernah diputar dengan mood tertentu, diurutkan play count desc.
     */
    @Query("""
        SELECT songId, title, artist, thumbnailUrl, COUNT(*) as playCount
        FROM play_history
        WHERE mood = :mood
        GROUP BY songId
        ORDER BY playCount DESC
        LIMIT :limit
    """)
    suspend fun getSongsByMood(mood: String, limit: Int = 30): List<TopSongResult>

    /**
     * Lagu yang diputar setelah :sinceMs dan minimal :minPlays kali.
     * Untuk "Recent Faves" smart playlist.
     */
    @Query("""
        SELECT songId, title, artist, thumbnailUrl, COUNT(*) as playCount
        FROM play_history
        WHERE playedAt >= :sinceMs
        GROUP BY songId
        HAVING playCount >= :minPlays
        ORDER BY playCount DESC
        LIMIT :limit
    """)
    suspend fun getRecentFaveSongs(sinceMs: Long, minPlays: Int, limit: Int = 30): List<TopSongResult>

    // ── Delete History ─────────────────────────────────────────────────────

    @Query("DELETE FROM play_history WHERE songId = :songId")
    suspend fun deleteBySongId(songId: String)

    @Query("DELETE FROM play_history")
    suspend fun clearAll()
}
