package com.zaaam.Zmusic.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.imageLoader
import coil.request.ImageRequest
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.zaaam.Zmusic.R
import com.zaaam.Zmusic.util.QueueManager
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

@AndroidEntryPoint
class FloatingPlayerService : Service() {

    @Inject lateinit var queueManager: QueueManager

    private lateinit var windowManager: WindowManager

    // Views
    private var bubbleView: View? = null
    private var playerView: View? = null

    // FIX TAMBAHAN: Simpan future agar bisa di-cancel di onDestroy()
    // mencegah memory leak jika service di-destroy sebelum future selesai.
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    companion object {
        private const val TAG = "ZmusicFloating"
        private const val CHANNEL_ID = "zmusic_floating_player"
        private const val NOTIFICATION_ID = 2
    }

    private val bubbleParams by lazy {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 300
        }
    }

    private val playerParams by lazy {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            y = 100
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        connectToMediaSession()
        observeQueue()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Floating bubble player Zmusic"
                setShowBadge(false)
            }
            val notifManager = getSystemService(NotificationManager::class.java)
            notifManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Zmusic Floating Player")
            .setContentText("Floating player aktif")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun connectToMediaSession() {
        try {
            val sessionToken = SessionToken(
                this,
                ComponentName(this, MusicService::class.java)
            )
            // FIX TAMBAHAN: Simpan future ke property agar bisa di-release di onDestroy()
            val future = MediaController.Builder(this, sessionToken).buildAsync()
            controllerFuture = future

            future.addListener({
                try {
                    controller = future.get()
                    controller?.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            updatePlayPauseButton(isPlaying)
                        }
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == Player.STATE_ENDED) updatePlayPauseButton(false)
                        }
                    })
                    startProgressUpdater()
                } catch (_: Exception) {}
            }, MoreExecutors.directExecutor())
        } catch (_: Exception) {}
    }

    private fun observeQueue() {
        serviceScope.launch {
            queueManager.queue.collect { queue ->
                if (queue.isEmpty()) {
                    removeAllViews()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    if (bubbleView == null && playerView == null) {
                        showBubble()
                    }
                    updateSongInfo()
                }
            }
        }

        serviceScope.launch {
            queueManager.currentIndex.collect {
                updateSongInfo()
            }
        }
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun showBubble() {
        if (bubbleView != null) return

        val view = LayoutInflater.from(this).inflate(R.layout.layout_floating_bubble, null)
        bubbleView = view

        var lastX = 0
        var lastY = 0
        var isDragging = false

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX.toInt()
                    lastY = event.rawY.toInt()
                    isDragging = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX.toInt() - lastX
                    val dy = event.rawY.toInt() - lastY
                    if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                        isDragging = true
                        bubbleParams.x += dx
                        bubbleParams.y += dy
                        lastX = event.rawX.toInt()
                        lastY = event.rawY.toInt()
                        try { windowManager.updateViewLayout(view, bubbleParams) } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        showPlayer()
                        removeBubble()
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager.addView(view, bubbleParams)
        } catch (e: Exception) {
            // Biasanya izin SYSTEM_ALERT_WINDOW dicabut — bubble tidak muncul
            Log.e(TAG, "addView bubble gagal — cek izin overlay", e)
        }

        updateSongInfo()
    }

    @SuppressLint("InflateParams")
    private fun showPlayer() {
        if (playerView != null) return

        val view = LayoutInflater.from(this).inflate(R.layout.layout_floating_player, null)
        playerView = view

        val btnClose = view.findViewById<ImageButton>(R.id.btnClose)
        val btnPlayPause = view.findViewById<ImageButton>(R.id.btnPlayPause)
        val btnPrev = view.findViewById<ImageButton>(R.id.btnPrev)
        val btnNext = view.findViewById<ImageButton>(R.id.btnNext)
        val seekBar = view.findViewById<SeekBar>(R.id.seekBar)

        btnClose.setOnClickListener {
            removePlayerView()
            showBubble()
        }

        btnPlayPause.setOnClickListener {
            val ctrl = controller ?: return@setOnClickListener
            if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
        }

        btnPrev.setOnClickListener {
            val prev = queueManager.previous() ?: return@setOnClickListener
            queueManager.requestPlay(prev)
        }

        btnNext.setOnClickListener {
            val next = queueManager.next() ?: return@setOnClickListener
            queueManager.requestPlay(next)
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val ctrl = controller ?: return
                    val duration = ctrl.duration
                    if (duration > 0) ctrl.seekTo((progress / 100f * duration).toLong())
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        try {
            windowManager.addView(view, playerParams)
        } catch (e: Exception) {
            // Biasanya izin SYSTEM_ALERT_WINDOW dicabut — mini player tidak muncul
            Log.e(TAG, "addView floating player gagal — cek izin overlay", e)
        }

        updateSongInfo()
        updatePlayPauseButton(controller?.isPlaying ?: false)
    }

    private fun updateSongInfo() {
        val song = queueManager.current() ?: return

        bubbleView?.let { view ->
            val imgThumb = view.findViewById<ImageView>(R.id.imgBubbleThumb) ?: return@let
            loadImage(song.thumbnailUrl, imgThumb)
        }

        playerView?.let { view ->
            val imgThumb = view.findViewById<ImageView>(R.id.imgAlbumArt) ?: return@let
            val tvTitle = view.findViewById<TextView>(R.id.tvTitle) ?: return@let
            val tvArtist = view.findViewById<TextView>(R.id.tvArtist) ?: return@let

            loadImage(song.thumbnailUrl, imgThumb)
            tvTitle.text = song.title
            tvArtist.text = song.artist
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        val btn = playerView?.findViewById<ImageButton>(R.id.btnPlayPause) ?: return
        btn.setImageResource(
            if (isPlaying) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play
        )

        bubbleView?.let { view ->
            val indicator = view.findViewById<View>(R.id.playingIndicator)
            indicator?.visibility = if (isPlaying) View.VISIBLE else View.GONE
        }
    }

    private fun startProgressUpdater() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive) {
                delay(500)
                if (playerView == null) {
                    delay(1500)
                    continue
                }
                val ctrl = controller ?: continue
                val duration = ctrl.duration
                val position = ctrl.currentPosition
                if (duration > 0) {
                    val progress = (position * 100 / duration).toInt()
                    playerView?.findViewById<SeekBar>(R.id.seekBar)?.progress = progress
                }
            }
        }
    }

    private fun loadImage(url: String, imageView: ImageView) {
        val request = ImageRequest.Builder(this)
            .data(url)
            .target(imageView)
            .crossfade(true)
            .build()
        this.imageLoader.enqueue(request)
    }

    private fun removeBubble() {
        bubbleView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            bubbleView = null
        }
    }

    private fun removePlayerView() {
        playerView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            playerView = null
        }
    }

    private fun removeAllViews() {
        removeBubble()
        removePlayerView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val song = queueManager.current()
        if (song != null && bubbleView == null && playerView == null) {
            showBubble()
        }
        // FIX BUG #4: START_NOT_STICKY menggantikan START_STICKY.
        //
        // START_STICKY lama menyebabkan sistem selalu merestart service setelah di-kill OOM.
        // Saat restart dengan intent = null, QueueManager mungkin masih punya data lama
        // → showBubble() terpanggil → overlay zombie muncul tanpa konteks yang benar.
        // Dua serviceScope berjalan bersamaan + progressJob 500ms terus polling = battery drain.
        //
        // FloatingPlayerService adalah UI overlay — bukan komponen audio inti.
        // Jika di-kill sistem, user harus eksplisit aktifkan kembali dari dalam app.
        // MusicService (MediaSessionService) yang bertanggung jawab atas keberlangsungan audio.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        removeAllViews()
        progressJob?.cancel()
        serviceScope.cancel()
        // FIX TAMBAHAN: Release future agar tidak terjadi leak
        // jika service di-destroy sebelum future selesai.
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
