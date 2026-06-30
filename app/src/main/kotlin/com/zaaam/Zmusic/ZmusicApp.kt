package com.zaaam.Zmusic

import android.app.Application
import android.util.Log
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.zaaam.Zmusic.analytics.AnalyticsManager
import com.zaaam.Zmusic.data.NewPipeDownloader
import com.zaaam.Zmusic.data.SettingsRepository
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
    @Inject lateinit var analyticsManager: AnalyticsManager
    @Inject lateinit var settingsRepository: SettingsRepository

    // Flag agar bisa di-retry dari luar (misal HomeViewModel) kalau init gagal
    var isNewPipeReady = false
        private set

    override fun onCreate() {
        super.onCreate()
        initCoilSingleton()
        initNewPipe()
        initAnalytics()
    }

    // Terapkan preferensi opt-out yang tersimpan ke Firebase Analytics. Inert
    // (no-op) kalau google-services.json belum dipasang — lihat AnalyticsManager.
    private fun initAnalytics() {
        try {
            analyticsManager.setEnabled(settingsRepository.isAnalyticsEnabled())
        } catch (e: Exception) {
            Log.e("ZmusicApp", "Gagal set status analytics", e)
        }
    }

    // FIX POTENSI #5: Inisialisasi Coil singleton di Application.
    //
    // MASALAH LAMA: FloatingPlayerService (dan NowPlayingCardGenerator) masing-masing
    // membuat ImageLoader baru via ImageLoader(context). Akibatnya:
    //   1. Setiap service memiliki OkHttpClient, MemoryCache, dan DiskCache tersendiri
    //      → duplikasi resource, memori boros, koneksi HTTP tidak bisa di-reuse.
    //   2. Cache thumbnail tidak di-share — gambar yang sudah di-load di PlayerScreen
    //      harus di-load ulang di FloatingPlayerService.
    //
    // FIX: Setup Coil singleton sekali di sini. Semua komponen yang memanggil
    // Coil.imageLoader(context) atau context.imageLoader akan mendapat instance
    // yang sama dengan cache dan connection pool yang di-share.
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
