package com.stravamusic.app.music

import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for the music engine. The UI only talks to this interface, so you can
 * drop in your own playback source code by implementing it (e.g. wrap ExoPlayer,
 * a streaming SDK, etc.) and swapping the instance created in [MusicViewModel].
 */
interface MusicController {

    /** Observable playback state for the UI. */
    val state: StateFlow<MusicState>

    /** Replace the current queue. Does not start playback automatically. */
    fun setQueue(tracks: List<Track>)

    /** Start/resume playback of the current (or first) track. */
    fun play()

    fun pause()

    fun togglePlayPause()

    fun next()

    fun previous()

    /** Jump to a position within the current track (milliseconds). */
    fun seekTo(positionMs: Long)

    /** Play a specific track from the queue by index. */
    fun playAt(index: Int)

    /**
     * Pull the latest playback position into [state]. Called periodically by the
     * ViewModel to drive the progress bar. Default no-op for engines that push
     * their own position updates.
     */
    fun refreshPosition() {}

    /** Free underlying resources. Call from ViewModel.onCleared(). */
    fun release()
}
