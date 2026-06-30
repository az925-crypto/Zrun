package com.stravamusic.app.music

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * A straightforward [MusicController] backed by [android.media.MediaPlayer].
 *
 * This is intentionally simple so the app builds and runs out of the box. When you
 * paste your own music playback source, either:
 *   1. Replace the body of these methods with your engine's calls, or
 *   2. Write a new class implementing [MusicController] and point
 *      [MusicViewModel] at it instead.
 */
class MediaPlayerMusicController(
    private val context: Context
) : MusicController {

    private val _state = MutableStateFlow(MusicState())
    override val state: StateFlow<MusicState> = _state.asStateFlow()

    private var player: MediaPlayer? = null
    private var currentIndex: Int = -1

    override fun setQueue(tracks: List<Track>) {
        _state.update { it.copy(queue = tracks) }
        if (tracks.isNotEmpty() && currentIndex == -1) {
            prepareTrack(0, autoPlay = false)
        }
    }

    override fun play() {
        val queue = _state.value.queue
        if (queue.isEmpty()) return
        if (player == null) {
            prepareTrack(if (currentIndex >= 0) currentIndex else 0, autoPlay = true)
        } else {
            player?.start()
            _state.update { it.copy(isPlaying = true) }
        }
    }

    override fun pause() {
        player?.takeIf { it.isPlaying }?.pause()
        _state.update { it.copy(isPlaying = false) }
    }

    override fun togglePlayPause() {
        if (_state.value.isPlaying) pause() else play()
    }

    override fun next() {
        val queue = _state.value.queue
        if (queue.isEmpty()) return
        val nextIndex = (currentIndex + 1) % queue.size
        prepareTrack(nextIndex, autoPlay = true)
    }

    override fun previous() {
        val queue = _state.value.queue
        if (queue.isEmpty()) return
        val prevIndex = if (currentIndex - 1 < 0) queue.size - 1 else currentIndex - 1
        prepareTrack(prevIndex, autoPlay = true)
    }

    override fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs.toInt())
        _state.update { it.copy(positionMs = positionMs) }
    }

    override fun playAt(index: Int) {
        val queue = _state.value.queue
        if (index in queue.indices) prepareTrack(index, autoPlay = true)
    }

    override fun refreshPosition() {
        val p = player ?: return
        if (p.isPlaying) {
            _state.update { it.copy(positionMs = p.currentPosition.toLong()) }
        }
    }

    override fun release() {
        player?.release()
        player = null
        currentIndex = -1
        _state.update { it.copy(isPlaying = false, positionMs = 0L) }
    }

    private fun prepareTrack(index: Int, autoPlay: Boolean) {
        val queue = _state.value.queue
        if (index !in queue.indices) return
        val track = queue[index]
        currentIndex = index

        player?.release()
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setOnCompletionListener { next() }
            setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                true
            }
            try {
                setDataSource(context, Uri.parse(track.uri))
                setOnPreparedListener { mp ->
                    _state.update {
                        it.copy(
                            currentTrack = track.copy(durationMs = mp.duration.toLong()),
                            positionMs = 0L,
                            isPlaying = autoPlay
                        )
                    }
                    if (autoPlay) mp.start()
                }
                prepareAsync()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load track ${track.uri}", e)
            }
        }
        _state.update { it.copy(currentTrack = track) }
    }

    companion object {
        private const val TAG = "MusicController"
    }
}
