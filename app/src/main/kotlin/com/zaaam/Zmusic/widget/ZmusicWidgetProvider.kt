package com.zaaam.Zmusic.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.zaaam.Zmusic.R
import com.zaaam.Zmusic.data.local.PlaylistDao
import com.zaaam.Zmusic.model.entity.PlaylistEntity
import com.zaaam.Zmusic.service.MusicService
import com.zaaam.Zmusic.ui.MainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ZmusicWidgetProvider : AppWidgetProvider() {

    // ── FIX BOCOR DB: ambil DAO dari singleton Hilt, BUKAN bikin database baru ──
    //
    // MASALAH LAMA: loadPlaylists() memanggil Room.databaseBuilder(...).build()
    // setiap onUpdate() (per widget ID!) dan tidak pernah close(). Tiap instance
    // membuka koneksi SQLite + file WAL sendiri → handle bocor menumpuk + risiko
    // SQLiteDatabaseLockedException saat app utama ikut menulis.
    //
    // FIX: BroadcastReceiver tidak bisa @Inject field biasa, jadi pakai Hilt
    // EntryPoint untuk mengambil PlaylistDao dari AppDatabase singleton yang sama
    // dengan yang dipakai seluruh app (provideDatabase di AppModule).
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun playlistDao(): PlaylistDao
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == MusicService.ACTION_UPDATE_WIDGET) {
            val title     = intent.getStringExtra(MusicService.EXTRA_SONG_TITLE)  ?: "Tidak ada lagu"
            val artist    = intent.getStringExtra(MusicService.EXTRA_SONG_ARTIST) ?: "Tap playlist di bawah"
            val isPlaying = intent.getBooleanExtra(MusicService.EXTRA_IS_PLAYING, false)

            val manager = AppWidgetManager.getInstance(context)
            val ids     = manager.getAppWidgetIds(ComponentName(context, ZmusicWidgetProvider::class.java))
            if (ids.isEmpty()) return

            val purple = android.graphics.Color.parseColor("#BB86FC")
            val views  = RemoteViews(context.packageName, R.layout.widget_zmusic)

            views.setTextViewText(R.id.widget_song_title, title)
            views.setTextViewText(R.id.widget_artist, artist)
            views.setImageViewResource(
                R.id.widget_play_pause,
                if (isPlaying) android.R.drawable.ic_media_pause
                else           android.R.drawable.ic_media_play
            )
            views.setInt(R.id.widget_play_pause, "setColorFilter", purple)
            views.setInt(R.id.widget_next,       "setColorFilter", purple)

            for (id in ids) {
                manager.partiallyUpdateAppWidget(id, views)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            // goAsync() biar onUpdate bisa await coroutine tanpa ANR
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                try {
                    val playlists = loadPlaylists(context)
                    updateWidget(context, appWidgetManager, widgetId, playlists)
                } finally {
                    pending.finish()
                }
            }
        }
    }

    // ── Load 3 playlist teratas dari Room (via singleton Hilt) ──────────────
    private suspend fun loadPlaylists(context: Context): List<PlaylistEntity> {
        val dao = EntryPointAccessors
            .fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
            .playlistDao()
        return dao.getAllPlaylists().first().take(3)
    }

    // ── Build & update RemoteViews ──────────────────────────────────────────
    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        playlists: List<PlaylistEntity>
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_zmusic)

        // Tap logo → buka app
        val openAppIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_app_logo, openAppIntent)

        // Set tint icon play/pause & next via kode (android:tint tidak support di RemoteViews)
        val purple = android.graphics.Color.parseColor("#BB86FC")
        views.setInt(R.id.widget_play_pause, "setColorFilter", purple)
        views.setInt(R.id.widget_next, "setColorFilter", purple)

        // Tombol Play/Pause
        val playPausePending = PendingIntent.getForegroundService(
            context, 1,
            Intent(context, MusicService::class.java).apply {
                action = MusicService.ACTION_TOGGLE_PLAY_PAUSE
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_play_pause, playPausePending)

        // Tombol Next
        val nextPending = PendingIntent.getForegroundService(
            context, 2,
            Intent(context, MusicService::class.java).apply {
                action = MusicService.ACTION_NEXT
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_next, nextPending)

        // ── Playlist buttons (maks 3) ───────────────────────────────────────
        val slots = listOf(
            Triple(R.id.widget_playlist_1, R.id.widget_playlist_1_name, 10),
            Triple(R.id.widget_playlist_2, R.id.widget_playlist_2_name, 11),
            Triple(R.id.widget_playlist_3, R.id.widget_playlist_3_name, 12),
        )

        slots.forEachIndexed { index, (containerId, nameId, requestCode) ->
            val playlist = playlists.getOrNull(index)
            if (playlist != null) {
                views.setViewVisibility(containerId, View.VISIBLE)
                views.setTextViewText(nameId, playlist.name)

                val playlistPending = PendingIntent.getForegroundService(
                    context, requestCode,
                    Intent(context, MusicService::class.java).apply {
                        action = MusicService.ACTION_PLAY_PLAYLIST
                        putExtra(MusicService.EXTRA_PLAYLIST_ID, playlist.id)
                    },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                views.setOnClickPendingIntent(containerId, playlistPending)
            } else {
                // Sembunyikan slot kalau playlist tidak ada
                views.setViewVisibility(containerId, View.INVISIBLE)
            }
        }

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}
