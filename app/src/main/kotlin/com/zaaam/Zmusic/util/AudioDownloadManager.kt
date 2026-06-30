package com.zaaam.Zmusic.util

import android.content.Context
import com.zaaam.Zmusic.data.SettingsRepository
import com.zaaam.Zmusic.data.local.SongDao
import com.zaaam.Zmusic.model.Song
import com.zaaam.Zmusic.model.entity.toEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Float = 0f) : DownloadState()
    object Done : DownloadState()
    data class Error(val message: String) : DownloadState()
}

@Singleton
class AudioDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val songDao: SongDao,
    private val settingsRepository: SettingsRepository,
    // FIX BUG #3: Inject shared OkHttpClient dari AppModule (@Singleton).
    // Sebelumnya AudioDownloadManager membuat OkHttpClient sendiri → connection pool
    // terpisah dari NewPipeDownloader dan LyricsRepository → saat download dan
    // streaming terjadi bersamaan, pool bisa exhausted → timeout → stream error →
    // musik berhenti. newBuilder() di sini hanya override readTimeout untuk download
    // tanpa membuat connection pool baru.
    sharedHttpClient: OkHttpClient
) {
    private val client = sharedHttpClient.newBuilder()
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val _states = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val states: StateFlow<Map<String, DownloadState>> = _states.asStateFlow()

    fun getState(songId: String): DownloadState =
        _states.value[songId] ?: if (isDownloaded(songId)) DownloadState.Done else DownloadState.Idle

    fun isDownloaded(songId: String): Boolean {
        val file = getFile(songId)
        return file.exists() && file.length() > 0
    }

    private fun getFile(songId: String): File =
        File(context.filesDir, "audio_$songId.m4a")

    /** Total bytes used by all downloaded audio files. */
    fun getStorageUsedBytes(): Long =
        context.filesDir
            .listFiles { f -> f.name.startsWith("audio_") && f.name.endsWith(".m4a") }
            ?.sumOf { it.length() } ?: 0L

    /** Human-readable storage usage string, e.g. "124 MB". */
    fun getStorageUsedLabel(): String {
        val bytes = getStorageUsedBytes()
        return when {
            bytes < 1024 * 1024      -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else                       -> String.format("%.1f GB", bytes / (1024f * 1024f * 1024f))
        }
    }

    /** Returns true if adding estimatedBytes would exceed the user-set cache limit. */
    private fun wouldExceedLimit(estimatedBytes: Long): Boolean {
        val limitBytes = settingsRepository.getMaxCacheMb().toLong() * 1024L * 1024L
        return getStorageUsedBytes() + estimatedBytes > limitBytes
    }

    // FIX #13: Cancel-safe download — cleanup partial file saat CancellationException
    suspend fun download(
        song: Song,
        getStreamUrl: suspend (String) -> String
    ) = withContext(Dispatchers.IO) {

        val current = getState(song.id)
        if (current is DownloadState.Downloading) return@withContext

        if (isDownloaded(song.id)) {
            songDao.insertOrIgnore(song.toEntity())
            songDao.updateLocalPath(song.id, getFile(song.id).absolutePath)
            setState(song.id, DownloadState.Done)
            return@withContext
        }

        // ── Cek cache limit sebelum mulai download ─────────────────────────
        // Estimasi kasar: rata-rata lagu YouTube ~5 MB
        val estimatedSizeBytes = 5L * 1024L * 1024L
        if (wouldExceedLimit(estimatedSizeBytes)) {
            setState(song.id, DownloadState.Error(
                "Penyimpanan penuh. Hapus beberapa lagu atau naikkan batas di Pengaturan."
            ))
            return@withContext
        }

        setState(song.id, DownloadState.Downloading(0f))

        val file = getFile(song.id)
        try {
            val streamUrl = getStreamUrl(song.id)
            val request = Request.Builder().url(streamUrl).build()

            // ══════════════════════════════════════════════════════════════
            // FIX #19: response.use {} memastikan response body SELALU
            // ditutup, bahkan jika contentLength() atau byteStream() throw.
            //
            // MASALAH LAMA:
            //   response.body?.contentLength() bisa throw sebelum
            //   byteStream().use{} → response body TIDAK di-close →
            //   connection LEAK di OkHttp connection pool → pool penuh →
            //   request berikutnya timeout.
            //
            // response.use {} = try-finally yang close response di finally.
            // ══════════════════════════════════════════════════════════════
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    setState(song.id, DownloadState.Error("Gagal download: ${response.code}"))
                    return@withContext
                }

                val totalBytes = response.body?.contentLength() ?: -1L

                response.body?.byteStream()?.use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (totalBytes > 0) {
                                val progress = (totalRead.toFloat() / totalBytes).coerceIn(0f, 1f)
                                setState(song.id, DownloadState.Downloading(progress))
                            }
                        }
                    }
                }
            }

            if (!file.exists() || file.length() == 0L) {
                setState(song.id, DownloadState.Error("File kosong setelah download"))
                return@withContext
            }

            songDao.insertOrIgnore(song.toEntity())
            songDao.updateLocalPath(song.id, file.absolutePath)
            setState(song.id, DownloadState.Done)

        } catch (e: CancellationException) {
            // FIX #13: Cleanup partial file saat coroutine di-cancel
            if (file.exists()) file.delete()
            setState(song.id, DownloadState.Idle)
            throw e
        } catch (e: Exception) {
            if (file.exists()) file.delete()
            setState(song.id, DownloadState.Error("Download gagal: ${e.message}"))
        }
    }

    fun deleteDownload(songId: String) {
        val file = getFile(songId)
        if (file.exists()) file.delete()
        setState(songId, DownloadState.Idle)
    }

    private fun setState(songId: String, state: DownloadState) {
        _states.update { currentMap ->
            currentMap.toMutableMap().apply { put(songId, state) }
        }
    }
}
