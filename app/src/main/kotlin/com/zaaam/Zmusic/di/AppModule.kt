package com.zaaam.Zmusic.di

import android.content.Context
import androidx.room.Room
import com.zaaam.Zmusic.data.local.ActivityDao
import com.zaaam.Zmusic.data.local.AppDatabase
import com.zaaam.Zmusic.data.local.PlayHistoryDao
import com.zaaam.Zmusic.data.local.PlaylistDao
import com.zaaam.Zmusic.data.local.SearchHistoryDao
import com.zaaam.Zmusic.data.local.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ══════════════════════════════════════════════════════════════════════════
    // FIX #7: Shared OkHttpClient instance.
    //
    // MASALAH LAMA:
    //   NewPipeDownloader, LyricsRepository, AudioDownloadManager masing-masing
    //   buat OkHttpClient sendiri → 3 connection pool terpisah → buang resource,
    //   tidak bisa reuse koneksi keep-alive antar class.
    //
    // SOLUSI:
    //   Satu OkHttpClient @Singleton dengan timeout default. Masing-masing class
    //   yang butuh timeout berbeda pakai client.newBuilder() — ini TIDAK membuat
    //   connection pool baru, hanya override config untuk instance tersebut.
    // ══════════════════════════════════════════════════════════════════════════
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "zmusic.db"
        )
        .addMigrations(
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,  // FIX #4 + #15: index pada play_history + search_history
            AppDatabase.MIGRATION_6_7   // FITUR STRAVA: tabel activities
        )
        .build()

    @Provides
    fun provideSongDao(db: AppDatabase): SongDao = db.songDao()

    @Provides
    fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao()

    @Provides
    fun providePlayHistoryDao(db: AppDatabase): PlayHistoryDao = db.playHistoryDao()

    @Provides
    fun provideSearchHistoryDao(db: AppDatabase): SearchHistoryDao = db.searchHistoryDao()

    @Provides
    fun provideActivityDao(db: AppDatabase): ActivityDao = db.activityDao()
}
