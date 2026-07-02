package com.zaaam.Zmusic.ui.library

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.Zmusic.data.MusicRepository
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.model.entity.PlaylistEntity
import com.zaaam.Zmusic.model.entity.toSong
import com.zaaam.Zmusic.util.AudioDownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val downloadManager: AudioDownloadManager
) : ViewModel() {

    val playlists: StateFlow<List<PlaylistEntity>> = repository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val downloadedSongs: StateFlow<List<Song>> = repository.getDownloadedSongs()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ── Export / Import events ke UI ───────────────────────────────────────
    private val _exportIntent = MutableSharedFlow<Intent>()
    val exportIntent: SharedFlow<Intent> = _exportIntent

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage

    // ── Playlist CRUD ──────────────────────────────────────────────────────
    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try { repository.createPlaylist(name.trim()) }
            catch (e: Exception) { _error.value = "Gagal membuat playlist: ${e.message}" }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            try { repository.deletePlaylist(playlistId) }
            catch (e: Exception) { _error.value = "Gagal menghapus playlist: ${e.message}" }
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            try { repository.addToPlaylist(playlistId, song) }
            catch (e: Exception) { _error.value = "Gagal menambah lagu: ${e.message}" }
        }
    }

    /** Buat playlist baru lalu langsung tambahkan lagu ke dalamnya (dipakai UI ZRun). */
    fun createPlaylistAndAdd(name: String, song: Song) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val id = repository.createPlaylist(name.trim())
                repository.addToPlaylist(id, song)
            } catch (e: Exception) { _error.value = "Gagal membuat playlist: ${e.message}" }
        }
    }

    fun deleteDownload(song: Song) {
        viewModelScope.launch {
            try {
                downloadManager.deleteDownload(song.id)
                repository.clearLocalPath(song.id)
            } catch (e: Exception) { _error.value = "Gagal menghapus download: ${e.message}" }
        }
    }

    fun clearError() { _error.value = null }

    // ── Export playlist yang dipilih ke JSON → share intent ────────────────
    fun exportPlaylist(context: Context, playlistId: Long) {
        viewModelScope.launch {
            try {
                val data  = repository.getPlaylistWithSongs(playlistId).first()
                val songs = data.songs.map { it.toSong() }
                val name  = data.playlist.name

                val json = JSONObject().apply {
                    put("playlistName", name)
                    put("version", 1)
                    put("exportedAt", System.currentTimeMillis())
                    put("songs", JSONArray().apply {
                        songs.forEach { s ->
                            put(JSONObject().apply {
                                put("id",           s.id)
                                put("title",        s.title)
                                put("artist",       s.artist)
                                put("thumbnailUrl", s.thumbnailUrl)
                                put("duration",     s.duration)
                            })
                        }
                    })
                }

                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val fileName  = "${name.replace(Regex("[^A-Za-z0-9_\\-]"), "_")}.json"
                val file      = File(exportDir, fileName)
                file.writeText(json.toString(2))

                val uri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Playlist ZMusic — $name")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                _exportIntent.emit(Intent.createChooser(shareIntent, "Export Playlist"))
            } catch (e: Exception) {
                _toastMessage.emit("Gagal export: ${e.message}")
            }
        }
    }

    // ── Import playlist dari JSON file ─────────────────────────────────────
    fun importPlaylist(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val jsonStr = context.contentResolver
                    .openInputStream(uri)?.bufferedReader()?.readText()
                    ?: run { _toastMessage.emit("Gagal membaca file"); return@launch }

                val jsonObj      = JSONObject(jsonStr)
                val playlistName = jsonObj.optString("playlistName", "Imported Playlist")
                val songsArray   = jsonObj.getJSONArray("songs")

                val newId = repository.createPlaylist(playlistName)
                var count = 0
                repeat(songsArray.length()) { i ->
                    val s = songsArray.getJSONObject(i)
                    repository.addToPlaylist(newId, Song(
                        id           = s.getString("id"),
                        title        = s.getString("title"),
                        artist       = s.getString("artist"),
                        thumbnailUrl = s.getString("thumbnailUrl"),
                        duration     = s.getLong("duration")
                    ))
                    count++
                }
                _toastMessage.emit("Import berhasil: \"$playlistName\" ($count lagu)")
            } catch (e: Exception) {
                _toastMessage.emit("Gagal import: ${e.message}")
            }
        }
    }
}
