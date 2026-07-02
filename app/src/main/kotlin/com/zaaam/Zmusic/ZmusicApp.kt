package com.zaaam.Zmusic

import android.app.Application
import android.util.Log
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.zaaam.Zmusic.data.NewPipeDownloader
import com.zaaam.Zmusic.data.ZmusicPoTokenProvider
import dagger.hilt.android.HiltAndroidApp
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import javax.inject.Inject

@HiltAndroidApp
class ZmusicApp : Application() {

    @Inject lateinit var newPipeDownloader: NewPipeDownloader
    @Inject lateinit var poTokenProvider: ZmusicPoTokenProvider

    // Flag agar bisa di-retry dari luar (misal HomeViewModel) kalau init gagal
    var isNewPipeReady = false
        private set

    override fun onCreate() {
        super.onCreate()
        initCoilSingleton()
        initNewPipe()
    }

    // Coil singleton: satu ImageLoader (cache memori + disk di-share) untuk
    // semua pemuat gambar di app — UI ZRun (ArtBox) & notifikasi MusicService.
    private fun initCoilSingleton() {
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .memoryCache {
                    MemoryCache.Builder(this)
                        .maxSizePercent(0.20) // 20% dari RAM tersedia untuk cache gambar
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizeBytes(50L * 1024 * 1024) // 50MB disk cache untuk thumbnail
                        .build()
                }
                .crossfade(true)
                .build()
        )
    }

    fun initNewPipe() {
        if (isNewPipeReady) return
        try {
            // FIX: Wrap dengan try-catch untuk safety di Android 10-11.
            // NewPipeExtractor v0.26.0 pakai java.time yang di API 29-30 harus lewat
            // desugaring. Kalau desugaring runtime belum siap → ClassNotFoundException.
            // Dengan try-catch, app tetap bisa launch dan retry nanti saat user
            // pertama kali search/buka home.
            NewPipe.init(
                newPipeDownloader,
                Localization("id", "ID"),
                ContentCountry("ID")
            )

            // Daftarkan poToken provider ke YouTube extractor.
            // Tanpa ini audioStreams selalu kosong (totalAudio=0) karena YouTube
            // butuh integrity check token untuk ngebalikin format audio.
            YoutubeStreamExtractor.setPoTokenProvider(poTokenProvider)
            Log.d("ZmusicPoToken", "✅ poToken provider TERDAFTAR ke YoutubeStreamExtractor")

            isNewPipeReady = true
        } catch (e: Exception) {
            Log.e("ZmusicApp", "NewPipe init gagal, akan retry nanti", e)
        }
    }
}
