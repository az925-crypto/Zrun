package com.stravamusic.app.tracking

import android.Manifest
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.stravamusic.app.MainActivity
import com.stravamusic.app.R
import com.stravamusic.app.StravaMusicApp
import com.stravamusic.app.data.local.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that records GPS samples into [TrackingBus] and keeps a
 * running timer, so recording survives the app going to the background.
 */
class TrackingService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null

    /** Wall-clock reference points so the timer is robust across pauses. */
    private var accumulatedMillis = 0L
    private var segmentStartElapsed = 0L

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val data = TrackingBus.data.value
            if (!data.isTracking || data.isPaused) return
            result.lastLocation?.let { onNewLocation(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        TrackingBus.reset()
        TrackingBus.update { it.copy(isTracking = true, isPaused = false) }
        accumulatedMillis = 0L
        segmentStartElapsed = SystemClock.elapsedRealtime()

        startForeground(NOTIFICATION_ID, buildNotification())
        requestLocationUpdates()
        startTimer()
    }

    private fun pauseTracking() {
        accumulatedMillis += SystemClock.elapsedRealtime() - segmentStartElapsed
        TrackingBus.update { it.copy(isPaused = true, currentSpeedKmh = 0.0) }
    }

    private fun resumeTracking() {
        segmentStartElapsed = SystemClock.elapsedRealtime()
        TrackingBus.update { it.copy(isPaused = false) }
    }

    private fun stopTracking() {
        if (!TrackingBus.data.value.isPaused) {
            accumulatedMillis += SystemClock.elapsedRealtime() - segmentStartElapsed
        }
        TrackingBus.update { it.copy(isTracking = false, isPaused = false, elapsedMillis = accumulatedMillis) }
        fusedClient.removeLocationUpdates(locationCallback)
        timerJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                val data = TrackingBus.data.value
                if (data.isTracking && !data.isPaused) {
                    val live = accumulatedMillis + (SystemClock.elapsedRealtime() - segmentStartElapsed)
                    TrackingBus.update { it.copy(elapsedMillis = live) }
                }
                delay(1000)
            }
        }
    }

    private fun requestLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(2000L)
            .setMinUpdateDistanceMeters(0f)
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // No permission: stop gracefully. UI is responsible for requesting it first.
            stopTracking()
            return
        }
        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun onNewLocation(location: Location) {
        val point = GeoPoint(location.latitude, location.longitude, System.currentTimeMillis())
        TrackingBus.update { current ->
            val lastPoint = current.route.lastOrNull()
            val addedDistance = if (lastPoint != null) LocationUtils.distanceMeters(lastPoint, point) else 0.0
            // Filter GPS jitter: ignore sub-meter jumps when essentially standing still.
            val accept = lastPoint == null || addedDistance >= 1.0
            if (!accept) {
                current.copy(currentSpeedKmh = if (location.hasSpeed()) location.speed * 3.6 else current.currentSpeedKmh)
            } else {
                current.copy(
                    route = current.route + point,
                    distanceMeters = current.distanceMeters + addedDistance,
                    currentSpeedKmh = if (location.hasSpeed()) location.speed * 3.6 else current.currentSpeedKmh
                )
            }
        }
    }

    private fun buildNotification(): android.app.Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, StravaMusicApp.TRACKING_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Recording your activity…")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.stravamusic.app.action.START"
        const val ACTION_PAUSE = "com.stravamusic.app.action.PAUSE"
        const val ACTION_RESUME = "com.stravamusic.app.action.RESUME"
        const val ACTION_STOP = "com.stravamusic.app.action.STOP"
        private const val NOTIFICATION_ID = 42
    }
}
