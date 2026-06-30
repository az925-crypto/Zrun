package com.zaaam.Zmusic.data

import android.content.Context
import android.util.Log
import com.zaaam.Zmusic.data.potoken.ChallengeData
import com.zaaam.Zmusic.data.potoken.JavaScriptUtil
import com.zaaam.Zmusic.data.potoken.PoTokenWebView
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.services.youtube.PoTokenProvider
import org.schabi.newpipe.extractor.services.youtube.PoTokenResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PoToken provider berbasis WebView untuk YouTube extractor.
 *
 * Alur saat getWebClientPoToken(videoId) dipanggil:
 * 1. [ensureReady] — fetch BotGuard challenge dari YouTube WAA endpoint
 * 2. Buat WebView tersembunyi, jalankan BotGuard JS program
 * 3. Generate dua token:
 *    - playerToken  : bound ke videoId  (untuk player request)
 *    - streamToken  : bound ke visitorData (untuk streaming URL)
 * 4. Kembalikan PoTokenResult ke NewPipeExtractor
 *
 * THREADING:
 * PoTokenProvider interface-nya synchronous (Java) — dipanggil NewPipe dari
 * background thread. Kita pakai runBlocking sebagai jembatan ke coroutine.
 * Aman karena Dispatchers.IO thread pool tidak terbatas (tidak akan deadlock).
 *
 * EXPIRY:
 * BotGuard challenge valid ~1 jam. Setelah expired, provider otomatis re-init.
 */
@Singleton
class ZmusicPoTokenProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient          // shared OkHttpClient dari AppModule
) : PoTokenProvider {

    private val TAG = "ZmusicPoToken"

    private val EXPIRY_MS = 60 * 60 * 1000L  // 1 jam

    private val mutex = Mutex()
    private var webView: PoTokenWebView? = null
    private var challenge: ChallengeData? = null
    private var lastInitMs = 0L

    private fun isExpired() = System.currentTimeMillis() - lastInitMs > EXPIRY_MS

    // ── Init & Reset ────────────────────────────────────────────────────────

    private suspend fun ensureReady() = mutex.withLock {
        if (webView != null && challenge != null && !isExpired()) return@withLock

        Log.d(TAG, "Inisialisasi poToken provider...")
        webView?.destroy()
        webView = null
        challenge = null

        // 1) Fetch BotGuard challenge dari YouTube
        val ch = JavaScriptUtil.fetchChallenge(httpClient)
        if (ch == null) {
            Log.e(TAG, "Gagal fetch BotGuard challenge — audio mungkin tetap 0 stream")
            return@withLock
        }

        // 2) Buat WebView & jalankan program BotGuard
        val wv = PoTokenWebView.create(context)
        if (wv == null) {
            Log.e(TAG, "Gagal buat PoTokenWebView")
            return@withLock
        }

        val ok = wv.initBotGuard(ch.program, ch.globalName)
        if (!ok) {
            Log.e(TAG, "BotGuard init gagal di WebView — lihat log untuk detail")
            wv.destroy()
            return@withLock
        }

        webView = wv
        challenge = ch
        lastInitMs = System.currentTimeMillis()
        Log.d(TAG, "poToken provider siap (visitorData=${ch.visitorData.take(10)}...)")
    }

    // ── Token Generation ────────────────────────────────────────────────────

    private fun getToken(videoId: String): PoTokenResult? {
        // runBlocking aman di sini — kita di background thread milik NewPipe,
        // bukan di dalam coroutine yang bisa deadlock.
        return runBlocking {
            try {
                ensureReady()
                val wv = webView ?: run {
                    Log.w(TAG, "WebView tidak siap, skip poToken untuk $videoId")
                    return@runBlocking null
                }
                val ch = challenge ?: return@runBlocking null

                if (ch.visitorData.isEmpty()) {
                    // Tanpa visitorData token tidak akan diterima YouTube — skip,
                    // dan paksa re-init di panggilan berikutnya
                    Log.w(TAG, "visitorData kosong — skip poToken untuk $videoId")
                    webView = null
                    return@runBlocking null
                }

                // Player token: bound ke videoId
                val playerToken = wv.generatePoToken(videoId)
                // Streaming token: bound ke visitorData (bisa di-reuse antar video)
                val streamToken = wv.generatePoToken(ch.visitorData.ifEmpty { videoId })

                if (playerToken == null || streamToken == null) {
                    Log.w(TAG, "Token null untuk $videoId — force re-init berikutnya")
                    webView = null  // expired / BotGuard stale, paksa re-init
                    return@runBlocking null
                }

                Log.d(TAG, "poToken OK untuk $videoId (player=${playerToken.take(8)}...)")

                // Constructor v0.26.x: (visitorData, playerRequestPoToken, streamingDataPoToken)
                PoTokenResult(ch.visitorData, playerToken, streamToken)

            } catch (e: Exception) {
                Log.e(TAG, "getToken gagal untuk $videoId", e)
                null
            }
        }
    }

    // ── PoTokenProvider Interface ───────────────────────────────────────────

    // Web client = client utama untuk audio streaming → yang paling penting diisi
    override fun getWebClientPoToken(videoId: String): PoTokenResult? {
        Log.d(TAG, "▶️ getWebClientPoToken DIPANGGIL untuk $videoId")
        return getToken(videoId)
    }

    // Sisanya null — tidak dibutuhkan untuk audio streaming biasa
    override fun getWebEmbedClientPoToken(videoId: String): PoTokenResult? {
        Log.d(TAG, "▶️ getWebEmbedClientPoToken DIPANGGIL untuk $videoId — return null")
        return null
    }

    override fun getAndroidClientPoToken(videoId: String): PoTokenResult? {
        Log.d(TAG, "▶️ getAndroidClientPoToken DIPANGGIL untuk $videoId — return null")
        return null
    }

    override fun getIosClientPoToken(videoId: String): PoTokenResult? {
        Log.d(TAG, "▶️ getIosClientPoToken DIPANGGIL untuk $videoId — return null")
        return null
    }
}
