package com.zaaam.Zmusic.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pembungkus tipis Firebase Analytics yang AMAN saat Firebase belum dikonfigurasi.
 *
 * Kenapa perlu guard: plugin google-services baru aktif kalau ada
 * app/google-services.json. Tanpa file itu (build CI/debug, atau sebelum user
 * memasangnya), FirebaseApp TIDAK ter-inisialisasi. Memanggil
 * FirebaseAnalytics.getInstance() dalam kondisi itu sia-sia/berisiko, jadi semua
 * akses dilewatkan lewat [firebaseAnalytics] yang null-kalau-belum-siap → setiap
 * method jadi no-op. App tidak akan pernah crash gara-gara analytics.
 *
 * Begitu user menaruh google-services.json dan rebuild, FirebaseApp auto-init via
 * ContentProvider (sebelum Application.onCreate), guard di sini langsung valid, dan
 * Analytics aktif tanpa perubahan kode lain.
 */
@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val firebaseAnalytics: FirebaseAnalytics? by lazy {
        try {
            // JANGAN panggil getInstance() kalau belum ada FirebaseApp terkonfigurasi.
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAnalytics.getInstance(context)
            } else {
                Log.d(TAG, "Firebase belum dikonfigurasi (google-services.json?). Analytics inert.")
                null
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Gagal inisialisasi FirebaseAnalytics — analytics inert", e)
            null
        }
    }

    /** true kalau Firebase aktif (google-services.json terpasang). Untuk debugging. */
    val isAvailable: Boolean get() = firebaseAnalytics != null

    /** Hidup/matikan pengumpulan data (dipanggil dari toggle Settings + saat startup). */
    fun setEnabled(enabled: Boolean) {
        firebaseAnalytics?.setAnalyticsCollectionEnabled(enabled)
    }

    /** Catat event kustom. No-op kalau Firebase belum siap. */
    fun logEvent(name: String, params: Bundle? = null) {
        firebaseAnalytics?.logEvent(name, params)
    }

    private companion object {
        const val TAG = "ZmusicAnalytics"
    }
}
