package com.zaaam.Zmusic.data.potoken

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Data challenge BotGuard dari YouTube WAA endpoint.
 *
 * @param program     JS program yang dijalankan di WebView
 * @param globalName  Nama var global yang ditulis program (mis. "a")
 * @param visitorData Visitor data YouTube (untuk generate streaming token)
 */
data class ChallengeData(
    val program: String,
    val globalName: String,
    val visitorData: String
)

/**
 * Utility untuk fetch BotGuard challenge dari YouTube.
 *
 * Alur:
 * 1. Ambil visitorData dari YouTube homepage (via header X-Goog-Visitor-Id / HTML body)
 * 2. POST ke WAA endpoint dengan request key → dapat BotGuard JS program
 * 3. Parse protobuf response minimal untuk ambil program + globalName
 */
object JavaScriptUtil {

    private const val TAG = "ZmusicPoToken"

    // Request key YouTube Web client (stabil, jarang berubah)
    private const val BOTGUARD_REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"

    // WAA endpoint untuk generate BotGuard challenge
    private const val WAA_URL =
        "https://jnn-pa.googleapis.com/\$rpc/google.internal.waa.v1.Waa/Create"
    private const val WAA_API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"

    // ── Public API ─────────────────────────────────────────────────────────

    suspend fun fetchChallenge(client: OkHttpClient): ChallengeData? =
        withContext(Dispatchers.IO) {
            try {
                val visitorData = fetchVisitorData(client) ?: ""
                Log.d(TAG, "visitorData: ${visitorData.take(12)}...")

                val (program, globalName) = fetchWaaChallenge(client)
                    ?: run {
                        Log.e(TAG, "WAA challenge gagal di-fetch")
                        return@withContext null
                    }

                Log.d(TAG, "Challenge OK — globalName=$globalName, program=${program.length} chars")
                ChallengeData(program, globalName, visitorData)

            } catch (e: Exception) {
                Log.e(TAG, "fetchChallenge gagal", e)
                null
            }
        }

    // ── Private: Visitor Data ──────────────────────────────────────────────

    private fun fetchVisitorData(client: OkHttpClient): String? {
        val request = Request.Builder()
            .url("https://www.youtube.com/")
            .get()
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
            .addHeader("Accept-Language", "en-US,en;q=0.9")
            .build()

        return client.newCall(request).execute().use { resp ->
            // Cara 1: dari response header
            resp.header("X-Goog-Visitor-Id")?.takeIf { it.isNotEmpty() }
            // Cara 2: extract dari HTML body
                ?: resp.body?.string()?.let { extractVisitorDataFromHtml(it) }
        }
    }

    private fun extractVisitorDataFromHtml(html: String): String? {
        // YouTube embed visitorData dalam format: "visitorData":"Cgt..." atau visitorData":"Cgt..."
        val regex = Regex(""""visitorData"\s*:\s*"([^"]{20,})"""")
        return regex.find(html)?.groupValues?.getOrNull(1)
    }

    // ── Private: WAA Challenge ─────────────────────────────────────────────

    private fun fetchWaaChallenge(client: OkHttpClient): Pair<String, String>? {
        // Body: protobuf field 1 = BOTGUARD_REQUEST_KEY
        val body = buildProtoString(fieldNumber = 1, value = BOTGUARD_REQUEST_KEY)

        val request = Request.Builder()
            .url(WAA_URL)
            .post(body.toRequestBody("application/x-protobuf".toMediaType()))
            .addHeader("x-goog-api-key", WAA_API_KEY)
            .addHeader("x-user-agent", "grpc-web-javascript/0.1")
            .addHeader("Content-Type", "application/x-protobuf")
            .build()

        return client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                Log.e(TAG, "WAA endpoint HTTP ${resp.code}")
                return null
            }
            val bytes = resp.body?.bytes() ?: return null
            Log.d(TAG, "WAA response: ${bytes.size} bytes")
            parseWaaResponse(bytes)
        }
    }

    /**
     * Parse WAA Create response (protobuf minimal).
     *
     * Struktur:
     *   outer field 1 (message) → inner:
     *     field 1 (string) = JS program
     *     field 5 (string) = global name (mis. "a")
     */
    private fun parseWaaResponse(bytes: ByteArray): Pair<String, String>? {
        return try {
            // Baca outer field 1 (nested message)
            val inner = readField(bytes, fieldNumber = 1, wireType = 2) ?: return null

            val program = readString(inner, fieldNumber = 1) ?: run {
                Log.e(TAG, "Program tidak ada di WAA response")
                return null
            }
            val globalName = readString(inner, fieldNumber = 5) ?: "a"

            Pair(program, globalName)
        } catch (e: Exception) {
            Log.e(TAG, "parseWaaResponse gagal", e)
            null
        }
    }

    // ── Minimal Protobuf Helpers ───────────────────────────────────────────

    /** Encode single string field sebagai protobuf bytes */
    private fun buildProtoString(fieldNumber: Int, value: String): ByteArray {
        val valueBytes = value.toByteArray(Charsets.UTF_8)
        val tag = ((fieldNumber shl 3) or 2).toByte() // wire type 2
        return byteArrayOf(tag) + encodeVarint(valueBytes.size.toLong()) + valueBytes
    }

    /** Baca field tertentu dari protobuf bytes, return bytes payload-nya */
    private fun readField(bytes: ByteArray, fieldNumber: Int, wireType: Int): ByteArray? {
        var i = 0
        while (i < bytes.size) {
            val (tagValue, tagLen) = readVarint(bytes, i); i += tagLen
            val field = (tagValue ushr 3).toInt()
            val wt = (tagValue and 0x07).toInt()

            when (wt) {
                0 -> { // varint — skip
                    val (_, n) = readVarint(bytes, i); i += n
                }
                2 -> { // length-delimited
                    val (len, lenBytes) = readVarint(bytes, i); i += lenBytes
                    val payload = bytes.copyOfRange(i, i + len.toInt())
                    if (field == fieldNumber && wt == wireType) return payload
                    i += len.toInt()
                }
                1 -> i += 8  // 64-bit — skip
                5 -> i += 4  // 32-bit — skip
                else -> return null
            }
        }
        return null
    }

    /** Baca string field dari protobuf bytes */
    private fun readString(bytes: ByteArray, fieldNumber: Int): String? {
        return readField(bytes, fieldNumber, wireType = 2)
            ?.let { String(it, Charsets.UTF_8) }
            ?.takeIf { it.isNotEmpty() }
    }

    private fun readVarint(bytes: ByteArray, offset: Int): Pair<Long, Int> {
        var result = 0L; var shift = 0; var read = 0; var pos = offset
        while (pos < bytes.size) {
            val b = bytes[pos++].toLong() and 0xFF; read++
            result = result or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0L) break
            shift += 7
        }
        return Pair(result, read)
    }

    private fun encodeVarint(value: Long): ByteArray {
        val out = mutableListOf<Byte>(); var v = value
        while (v > 0x7F) { out.add(((v and 0x7F) or 0x80).toByte()); v = v ushr 7 }
        out.add((v and 0x7F).toByte())
        return out.toByteArray()
    }
}
