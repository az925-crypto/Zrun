package com.zaaam.Zmusic.data.potoken

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.*
import kotlinx.coroutines.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * WebView tersembunyi yang menjalankan BotGuard JS untuk generate poToken YouTube.
 *
 * THREADING MODEL:
 * - WebView wajib dibuat dan dioperasikan di main thread.
 * - Public methods adalah suspend functions yang internally switch ke Dispatchers.Main.
 * - Caller boleh dari thread mana aja (termasuk background IO coroutine).
 *
 * LIFECYCLE:
 * - Buat instance via PoTokenWebView.create(context) — coroutine-safe.
 * - Satu instance dipakai berulang kali untuk banyak generatePoToken() call.
 * - Panggil destroy() kalau sudah tidak dipakai (biasanya saat ZmusicPoTokenProvider expired).
 */
class PoTokenWebView private constructor(
    private val webView: WebView
) {
    private val TAG = "ZmusicPoToken"

    // Map callbackId → Deferred yang menunggu hasil dari JS
    private val pending = ConcurrentHashMap<String, CompletableDeferred<String?>>()

    // ── JavaScript Interface ────────────────────────────────────────────────

    /**
     * Dipanggil dari JS (po_token.html) via AndroidBridge.onInitResult(id, result).
     */
    inner class AndroidBridge {

        @JavascriptInterface
        fun onInitResult(callbackId: String, result: String) {
            Log.d(TAG, "onInitResult: $result")
            pending.remove(callbackId)?.complete(result)
        }

        @JavascriptInterface
        fun onPoTokenResult(callbackId: String, token: String) {
            pending.remove(callbackId)?.complete(token.takeIf { it.isNotEmpty() })
        }
    }

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Jalankan BotGuard program di WebView.
     * Harus dipanggil sekali setelah create(), sebelum generatePoToken().
     *
     * @return true kalau BotGuard berhasil init, false kalau gagal.
     */
    suspend fun initBotGuard(program: String, globalName: String): Boolean {
        val id = newId()
        val deferred = newDeferred(id)

        withContext(Dispatchers.Main) {
            // Pakai template literal (backtick) biar program nggak perlu escape quote
            // tapi perlu escape backtick dan backslash di dalam program
            val escaped = program
                .replace("\\", "\\\\")
                .replace("`", "\\`")
            val js = "initBotGuard(`$escaped`, ${globalName.jsStr()}, ${id.jsStr()})"
            webView.evaluateJavascript(js, null)
        }

        val result = withTimeoutOrNull(15_000) { deferred.await() }
        if (result != "ok") {
            Log.e(TAG, "initBotGuard gagal: $result")
            return false
        }
        return true
    }

    /**
     * Generate poToken untuk contentBinding (videoId atau visitorData).
     * Panggil setelah initBotGuard() sukses.
     *
     * @return token string, atau null kalau gagal / timeout.
     */
    suspend fun generatePoToken(contentBinding: String): String? {
        val id = newId()
        val deferred = newDeferred(id)

        withContext(Dispatchers.Main) {
            val js = "generatePoToken(${contentBinding.jsStr()}, ${id.jsStr()})"
            webView.evaluateJavascript(js, null)
        }

        val result = withTimeoutOrNull(10_000) { deferred.await() }
        if (result == null) Log.w(TAG, "generatePoToken timeout untuk binding=${contentBinding.take(10)}")
        return result
    }

    /** Destroy WebView dan batalkan semua callback yang pending. */
    fun destroy() {
        CoroutineScope(Dispatchers.Main).launch {
            webView.destroy()
        }
        pending.values.forEach { it.cancel() }
        pending.clear()
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun newId() = UUID.randomUUID().toString()
    private fun newDeferred(id: String): CompletableDeferred<String?> {
        return CompletableDeferred<String?>().also { pending[id] = it }
    }

    // ── Static Factory ──────────────────────────────────────────────────────

    companion object {

        private const val TAG = "ZmusicPoToken"

        /**
         * Buat instance PoTokenWebView baru.
         * Switch ke main thread secara internal — aman dipanggil dari coroutine mana aja.
         *
         * @return instance, atau null kalau gagal (log sudah tertulis).
         */
        @SuppressLint("SetJavaScriptEnabled")
        suspend fun create(context: Context): PoTokenWebView? = withContext(Dispatchers.Main) {
            try {
                val wv = WebView(context.applicationContext).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                    }
                    // WebView tidak perlu render — sembunyikan
                    visibility = android.view.View.GONE
                }

                val instance = PoTokenWebView(wv)
                wv.addJavascriptInterface(instance.AndroidBridge(), "AndroidBridge")

                // Tunggu HTML selesai load
                val loadDone = CompletableDeferred<Unit>()
                wv.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        loadDone.complete(Unit)
                    }
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        loadDone.completeExceptionally(
                            Exception("WebView load error: ${error?.description}")
                        )
                    }
                }

                wv.loadUrl("file:///android_asset/po_token.html")

                withTimeoutOrNull(8_000) { loadDone.await() }
                    ?: throw Exception("Timeout load po_token.html")

                Log.d(TAG, "PoTokenWebView berhasil dibuat")
                instance

            } catch (e: Exception) {
                Log.e(TAG, "PoTokenWebView.create gagal", e)
                null
            }
        }
    }
}

/** Wrap string sebagai JS string literal dengan escape aman */
private fun String.jsStr(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "")}\""
