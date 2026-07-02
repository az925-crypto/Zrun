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

    // Waktu BERGERAK (ala Strava §1.4): diakumulasi tiap tick timer saat kecepatan
    // di atas ambang. Istirahat tanpa tekan Jeda otomatis tak merusak pace.
    private var movingAccumMillis = 0L

    // Smoothing tampilan (riset §langkah-7): rata-rata bergerak 5 sampel speed
    // terakhir supaya angka km/j di layar tidak loncat-loncat.
    private val recentSpeedsKmh = ArrayDeque<Double>()

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
        movingAccumMillis = 0L
        recentSpeedsKmh.clear()
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
            it.copy(
                isTracking = false, isPaused = false,
                elapsedMillis = accumulatedMillis, movingMillis = movingAccumMillis
            )
        }
        fusedClient.removeLocationUpdates(locationCallback)
        timerJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var lastTick = SystemClock.elapsedRealtime()
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                val delta = now - lastTick
                lastTick = now
                val data = stateHolder.data.value
                if (data.isTracking && !data.isPaused) {
                    val live = accumulatedMillis + (now - segmentStartElapsed)
                    // Moving time: hanya bertambah saat kecepatan di atas ambang "diam".
                    if (data.currentSpeedKmh >= MIN_MOVING_KMH) movingAccumMillis += delta
                    stateHolder.update { it.copy(elapsedMillis = live, movingMillis = movingAccumMillis) }
                }
                delay(1000)
            }
        }
    }

    private fun requestLocationUpdates() {
        // Riset §langkah-1: update tiap ±1 detik, prioritas akurasi tinggi.
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(1000L)
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

    /**
     * Pipeline akurasi (riset: filter akurasi → speed bawaan GPS → filter glitch →
     * ambang bergerak/diam → smoothing tampilan).
     */
    private fun onNewLocation(location: Location) {
        // Langkah 2: buang fix dengan radius error terlalu besar.
        if (location.hasAccuracy() && location.accuracy > MAX_ACCURACY_M) return

        val point = GeoPoint(location.latitude, location.longitude, System.currentTimeMillis())
        // Langkah 3: pakai speed bawaan chip GPS (Doppler), bukan hitung ulang manual.
        val rawKmh = if (location.hasSpeed()) location.speed * 3.6 else 0.0
        val movingSlow = location.hasSpeed() && location.speed * 3.6 < MIN_MOVING_KMH

        // Langkah 7: smoothing tampilan — rata-rata 5 sampel terakhir.
        recentSpeedsKmh.addLast(rawKmh)
        if (recentSpeedsKmh.size > SPEED_WINDOW) recentSpeedsKmh.removeFirst()
        val smoothKmh = if (movingSlow) 0.0 else recentSpeedsKmh.average()

        stateHolder.update { current ->
            val last = current.route.lastOrNull()
            if (last == null) {
                return@update current.copy(route = listOf(point), currentSpeedKmh = smoothKmh)
            }
            val d = LocationUtils.distanceMeters(last, point)
            val dtSec = (point.timestamp - last.timestamp) / 1000.0

            // Langkah 2 lanjutan: buang segmen "teleport" (kecepatan tersirat mustahil).
            if (dtSec > 0 && (d / dtSec) > MAX_PLAUSIBLE_MPS) return@update current

            // Langkah 4-5: jarak hanya bertambah saat benar-benar bergerak.
            val accept = d >= MIN_STEP_M && !movingSlow
            if (accept) {
                current.copy(
                    route = current.route + point,
                    distanceMeters = current.distanceMeters + d,
                    currentSpeedKmh = smoothKmh
                )
            } else {
                current.copy(currentSpeedKmh = smoothKmh)
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

        // ── Ambang pipeline akurasi (dari dokumen riset) ─────────────────────
        private const val MAX_ACCURACY_M = 15f      // buang fix radius error >15m
        private const val MIN_MOVING_KMH = 2.5      // <2.5 km/j = dianggap diam (ala Strava)
        private const val MAX_PLAUSIBLE_MPS = 12.0  // >43 km/j antar titik = glitch GPS
        private const val MIN_STEP_M = 2.0          // gerakan <2m diabaikan (drift)
        private const val SPEED_WINDOW = 5          // jendela smoothing tampilan
    }
}
