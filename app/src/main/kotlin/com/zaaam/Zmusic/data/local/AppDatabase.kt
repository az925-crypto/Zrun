package com.zaaam.Zmusic.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.zaaam.Zmusic.model.entity.ActivityEntity
import com.zaaam.Zmusic.model.entity.PlayHistoryEntity
import com.zaaam.Zmusic.model.entity.PlaylistEntity
import com.zaaam.Zmusic.model.entity.PlaylistSongCrossRef
import com.zaaam.Zmusic.model.entity.SearchHistoryEntity
import com.zaaam.Zmusic.model.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        PlayHistoryEntity::class,
        SearchHistoryEntity::class,
        ActivityEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(RouteConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun activityDao(): ActivityDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN localPath TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE play_history ADD COLUMN mood TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS search_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        query TEXT NOT NULL,
                        searchedAt INTEGER NOT NULL
                    )"""
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE play_history ADD COLUMN sourceQuery TEXT")
            }
        }

        // FIX #4: Index pada play_history.songId dan play_history.playedAt
        // FIX #15: Unique index pada search_history.query untuk atomic upsert
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Index untuk play_history — mempercepat semua query WHERE/GROUP BY songId
                db.execSQL("CREATE INDEX IF NOT EXISTS index_play_history_songId ON play_history(songId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_play_history_playedAt ON play_history(playedAt)")
                // Unique index untuk search_history — memungkinkan ON CONFLICT di upsert
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_search_history_query ON search_history(query)")
            }
        }

        // FITUR STRAVA: tabel riwayat aktivitas olahraga (rute disimpan sebagai
        // String terenkode via RouteConverters).
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS activities (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        type TEXT NOT NULL,
                        startTime INTEGER NOT NULL,
                        durationMillis INTEGER NOT NULL,
                        distanceMeters REAL NOT NULL,
                        avgSpeedKmh REAL NOT NULL,
                        route TEXT NOT NULL
                    )"""
                )
            }
        }
    }
}
