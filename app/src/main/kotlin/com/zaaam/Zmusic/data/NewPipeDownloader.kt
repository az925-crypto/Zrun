package com.zaaam.Zmusic.data

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewPipeDownloader @Inject constructor(
    // FIX #7: Inject shared OkHttpClient dari AppModule.
    // FIX STOP BUG: Buat dedicated client untuk NewPipe dengan timeout yang lebih tinggi.
    //
    // ROOT CAUSE: shared OkHttpClient dari AppModule punya readTimeout 30 detik.
    // NewPipe's StreamInfo.getInfo() untuk YouTube butuh 2-3 HTTP request berurutan:
    //   1. Fetch halaman YouTube (~200KB)
    //   2. Fetch JS player file (~500KB)
    //   3. Parse + jalankan JS untuk deobfuscate nsig signature
    //
    // Di jaringan mobile Indonesia yang sering naik-turun, total request chain ini
    // bisa melebihi 30 detik. Akibatnya: SocketTimeoutException → Exception di
    // getStreamUrl() → 3x retry dengan total ~93 detik → semua gagal →
    // playNextAuto() → kalau lagu terakhir di queue → stopSelf() → MUSIK BERHENTI.
    //
    // Fix: NewPipeDownloader pakai newBuilder() dari shared client sehingga
    // connection pool tetap sama (tidak bikin pool baru), tapi timeout dinaikkan:
    //   - readTimeout 60s: cukup untuk fetch JS file besar di jaringan lambat
    //   - callTimeout 90s: hard ceiling untuk seluruh request chain per call
    //     (mencegah hang tak terbatas kalau server YouTube lambat merespons)
    //
    // AudioDownloadManager sudah pakai newBuilder() dengan readTimeout 120s
    // (benar — download file besar butuh lebih lama). Kita pakai nilai lebih
    // rendah karena ini untuk metadata/stream info, bukan download file.
    sharedClient: OkHttpClient
) : Downloader() {

    private val client = sharedClient.newBuilder()
        .readTimeout(60, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .build()

    override fun execute(request: Request): Response {
        val requestBuilder = okhttp3.Request.Builder().url(request.url())

        request.headers().forEach { (key, values) ->
            values.forEach { value ->
                requestBuilder.addHeader(key, value)
            }
        }

        val body = request.dataToSend()?.let {
            it.toRequestBody("application/json".toMediaType())
        }

        requestBuilder.method(request.httpMethod(), body)

        // FIX #7: Wrap response dalam .use {} agar body SELALU di-close,
        // bahkan kalau ada exception sebelum .string() dipanggil.
        // Tanpa ini, kalau exception terjadi saat iterate headers,
        // response body leak → connection pool exhaustion.
        val response = client.newCall(requestBuilder.build()).execute()
        return response.use { resp ->
            Response(
                resp.code,
                resp.message,
                resp.headers.toMultimap(),
                resp.body?.string() ?: "",
                resp.request.url.toString()
            )
        }
    }
}
