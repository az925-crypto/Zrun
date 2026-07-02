package com.zaaam.Zmusic.tracking

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
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
import com.zaaam.Zmusic.R
import com.zaaam.Zmusic.model.GeoPoint
import com.zaaam.Zmusic.ui.MainActivity
import com.zaaam.Zmusic.util.LocationUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service yang merekam sampel GPS ke [TrackingStateHolder] + menjaga
 * timer berjalan, supaya rekaman lanjut saat app di background / layar mati.
 * Musik (MusicService) jalan terpisah, jadi keduanya bisa aktif bersamaan.
 */
@AndroidEntryPoint
class TrackingService : Service() {

    @Inject lateinit var stateHolder: TrackingStateHolder

    private lateinit var fusedClient: FusedLocationProviderClient
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null

    // Acuan wall-clock supaya timer tahan pause/resume.
    private var accumulatedMillis = 0L
    private var segmentStartElapsed = 0L

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val data = stateHolder.data.value
            if (!data.isTracking || data.isPaused) return
            result.lastLocation?.let { onNewLocation(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
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
        stateHolder.reset()
        stateHolder.update { it.copy(isTracking = true, isPaused = false) }
        accumulatedMillis = 0L
        segmentStartElapsed = SystemClock.elapsedRealtime()

        startForeground(NOTIFICATION_ID, buildNotification())
        requestLocationUpdates()
        startTimer()
    }

    private fun pauseTracking() {
        accumulatedMillis += SystemClock.elapsedRealtime() - segmentStartElapsed
        stateHolder.update { it.copy(isPaused = true, currentSpeedKmh = 0.0) }
    }

    private fun resumeTracking() {
        segmentStartElapsed = SystemClock.elapsedRealtime()
        stateHolder.update { it.copy(isPaused = false) }
    }

    private fun stopTracking() {
        if (!stateHolder.data.value.isPaused) {
            accumulatedMillis += SystemClock.elapsedRealtime() - segmentStartElapsed
        }
        stateHolder.update {
            it.copy(isTracking = false, isPaused = false, elapsedMillis = accumulatedMillis)
        }
        fusedClient.removeLocationUpdates(locationCallback)
        timerJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                val data = stateHolder.data.value
                if (data.isTracking && !data.isPaused) {
                    val live = accumulatedMillis + (SystemClock.elapsedRealtime() - segmentStartElapsed)
                    stateHolder.update { it.copy(elapsedMillis = live) }
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
            // Tanpa izin: stop dengan rapi. UI yang bertanggung jawab minta izin dulu.
            stopTracking()
            return
        }
        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun onNewLocation(location: Location) {
        // 1) Buang fix GPS berakurasi jelek (>25m) — sumber utama "jarak hantu".
        if (location.hasAccuracy() && location.accuracy > 25f) return

        val point = GeoPoint(location.latitude, location.longitude, System.currentTimeMillis())
        // km/jam dari GPS speed (Doppler) — lebih akurat daripada turunan jarak.
        val speedKmh = if (location.hasSpeed()) location.speed * 3.6 else 0.0
        // Dianggap "diam" kalau GPS speed < ~2.2 km/j.
        val movingSlow = location.hasSpeed() && location.speed < 0.6f

        stateHolder.update { current ->
            val last = current.route.lastOrNull()
            if (last == null) {
                return@update current.copy(route = listOf(point), currentSpeedKmh = speedKmh)
            }
            val d = LocationUtils.distanceMeters(last, point)
            // Tambah jarak hanya kalau gerak ≥2m DAN tidak sedang diam → cegah drift menumpuk.
            val accept = d >= 2.0 && !movingSlow
            if (accept) {
                current.copy(
                    route = current.route + point,
                    distanceMeters = current.distanceMeters + d,
                    currentSpeedKmh = speedKmh
                )
            } else {
                current.copy(currentSpeedKmh = if (movingSlow) 0.0 else speedKmh)
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
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Merekam aktivitas…")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Perekaman Aktivitas",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Menampilkan rekaman lari/olahraga yang sedang berjalan"
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.zaaam.Zmusic.tracking.START"
        const val ACTION_PAUSE = "com.zaaam.Zmusic.tracking.PAUSE"
        const val ACTION_RESUME = "com.zaaam.Zmusic.tracking.RESUME"
        const val ACTION_STOP = "com.zaaam.Zmusic.tracking.STOP"
        private const val CHANNEL_ID = "tracking_channel"
        private const val NOTIFICATION_ID = 73
    }
}
