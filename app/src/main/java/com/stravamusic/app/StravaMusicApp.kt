package com.stravamusic.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.stravamusic.app.data.local.AppDatabase
import com.stravamusic.app.data.repository.ActivityRepository

/**
 * Application entry point. Owns the singleton database/repository so screens and
 * services can share the same instance without a DI framework.
 */
class StravaMusicApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val activityRepository: ActivityRepository by lazy { ActivityRepository(database.activityDao()) }

    override fun onCreate() {
        super.onCreate()
        createTrackingNotificationChannel()
    }

    private fun createTrackingNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TRACKING_CHANNEL_ID,
                getString(R.string.tracking_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.tracking_channel_desc)
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val TRACKING_CHANNEL_ID = "tracking_channel"
    }
}
