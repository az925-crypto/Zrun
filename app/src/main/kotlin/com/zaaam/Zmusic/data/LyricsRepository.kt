package com.zaaam.Zmusic.data

import android.net.Uri
import android.util.Log
import com.zaaam.Zmusic.model.LyricLine
import com.zaaam.Zmusic.model.Lyrics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepository @Inject constructor(
    // FIX #7: Inject shared OkHttpClient dari AppModule.
    // newBuilder() share connection pool, override timeout lebih pendek untuk lyrics API.
    baseClient: OkHttpClient
) {

    companion object {
        private const val TAG = "ZmusicLyrics"
    }

    private val client = baseClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // ── LRCLIB: Fetch Lyrics ───────────────────────────────────────────────

    suspend fun getLyrics(title: String, artist: String): Lyrics? = withContext(Dispatchers.IO) {
        try {
            val url = "https://lrclib.net/api/get?" +
                    "artist_name=${Uri.encode(artist)}&track_name=${Uri.encode(title)}"

            val request = Request.Builder()
                .url(url)
                .header("Lrclib-Client", "Zmusic Android v1.0")
                .build()

            // FIX #8: Wrap response dalam .use {} agar body selalu di-close
            val response = client.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) return@withContext null

                val body = resp.body?.string() ?: return@withContext null
                val json = JSONObject(body)

                val plain = if (json.has("plainLyrics") && !json.isNull("plainLyrics"))
                    json.getString("plainLyrics") else ""

                val syncedRaw = if (json.has("syncedLyrics") && !json.isNull("syncedLyrics"))
                    json.getString("syncedLyrics") else ""

                if (plain.isBlank() && syncedRaw.isBlank()) return@withContext null

                val synced = parseLrc(syncedRaw)

                Lyrics(plain = plain, synced = synced)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "getLyrics gagal untuk \"$title\" - \"$artist\"", e)
            null
        }
    }

    // ── LRC Parser ─────────────────────────────────────────────────────────

    // FIX #20-equivalent: Regex dicompile sekali
    private val lrcPattern = Regex("""^\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")

    private fun parseLrc(lrc: String): List<LyricLine> {
        if (lrc.isBlank()) return emptyList()
        val result = mutableListOf<LyricLine>()

        lrc.lines().forEach { line ->
            val match = lrcPattern.find(line.trim()) ?: return@forEach
            val (min, sec, ms, text) = match.destructured
            val timeMs = min.toLong() * 60_000 +
                         sec.toLong() * 1_000 +
                         (if (ms.length == 2) ms.toLong() * 10 else ms.toLong())
            result.add(LyricLine(timeMs = timeMs, text = text.trim()))
        }

        return result.sortedBy { it.timeMs }
    }

    // ── MyMemory: Translate Lyrics ─────────────────────────────────────────

    suspend fun translateLyrics(text: String, targetLang: String = "id"): String? =
        withContext(Dispatchers.IO) {
            try {
                val maxChunkSize = 450

                if (text.length <= maxChunkSize) {
                    translateChunk(text, targetLang)
                } else {
                    val lines = text.lines()
                    val chunks = mutableListOf<String>()
                    val currentChunk = StringBuilder()

                    for (line in lines) {
                        if (currentChunk.length + line.length + 1 > maxChunkSize) {
                            if (currentChunk.isNotEmpty()) {
                                chunks.add(currentChunk.toString())
                                currentChunk.clear()
                            }
                        }
                        if (currentChunk.isNotEmpty()) currentChunk.append("\n")
                        currentChunk.append(line)
                    }
                    if (currentChunk.isNotEmpty()) {
                        chunks.add(currentChunk.toString())
                    }

                    val translatedChunks = chunks.mapNotNull { chunk ->
                        translateChunk(chunk, targetLang)
                    }

                    if (translatedChunks.isEmpty()) {
                        fallbackTranslate(text, targetLang)
                    } else {
                        translatedChunks.joinToString("\n")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "translateLyrics gagal — coba fallback Lingva", e)
                fallbackTranslate(text, targetLang)
            }
        }

    // FIX #8: response.use {} untuk mencegah connection leak
    private fun translateChunk(text: String, targetLang: String): String? {
        return try {
            val url = "https://api.mymemory.translated.net/get?" +
                    "q=${Uri.encode(text)}&langpair=${Uri.encode("en|$targetLang")}"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) return null

                val body = resp.body?.string() ?: return null
                val json = JSONObject(body)

                val responseData = json.optJSONObject("responseData")
                val translated = responseData?.optString("translatedText")

                if (translated.isNullOrBlank() || translated == text) null
                else translated
            }
        } catch (e: Exception) {
            Log.w(TAG, "translateChunk (MyMemory) gagal", e)
            null
        }
    }

    // FIX #8: response.use {} untuk mencegah connection leak
    private fun fallbackTranslate(text: String, targetLang: String): String? {
        return try {
            val jsonBody = JSONObject().apply {
                put("q", text)
                put("source", "en")
                put("target", targetLang)
            }.toString()
            val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://lingva.ml/api/v1/")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val json = JSONObject(body)
                json.optString("translation").takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fallbackTranslate (Lingva) gagal — terjemahan menyerah", e)
            null
        }
    }
}
