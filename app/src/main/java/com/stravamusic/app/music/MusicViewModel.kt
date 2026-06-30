package com.stravamusic.app.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicViewModel(app: Application) : AndroidViewModel(app) {

    // Swap this for your own MusicController implementation to use your music code.
    private val controller: MusicController = MediaPlayerMusicController(app)

    val state: StateFlow<MusicState> = controller.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MusicState()
    )

    init {
        // Demo queue so the player has something to show. Replace with your library.
        controller.setQueue(SampleTracks.demo)

        // Drive the progress bar.
        viewModelScope.launch {
            while (isActive) {
                controller.refreshPosition()
                delay(500)
            }
        }
    }

    fun setQueue(tracks: List<Track>) = controller.setQueue(tracks)
    fun togglePlayPause() = controller.togglePlayPause()
    fun next() = controller.next()
    fun previous() = controller.previous()
    fun seekTo(positionMs: Long) = controller.seekTo(positionMs)
    fun playAt(index: Int) = controller.playAt(index)

    override fun onCleared() {
        controller.release()
        super.onCleared()
    }
}

/** Placeholder tracks until you wire in a real library. URIs point at sample audio. */
object SampleTracks {
    val demo = listOf(
        Track(
            id = "1",
            title = "Sample Track One",
            artist = "Demo Artist",
            uri = "https://www.kozco.com/tech/piano2.wav"
        ),
        Track(
            id = "2",
            title = "Sample Track Two",
            artist = "Demo Artist",
            uri = "https://www.kozco.com/tech/organfinale.wav"
        )
    )
}
