package com.zaaam.Zmusic.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("zmusic_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FLOATING_BUBBLE  = "floating_bubble_enabled"
        private const val KEY_MAX_CACHE_MB      = "max_cache_mb"
        private const val KEY_ANALYTICS_ENABLED = "analytics_enabled"
        const val DEFAULT_MAX_CACHE_MB          = 512   // 512 MB default
        val CACHE_SIZE_OPTIONS                  = listOf(256, 512, 1024, 2048) // MB
    }

    // ── Floating Bubble ───────────────────────────────────────────────────

    private val _floatingBubbleEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_FLOATING_BUBBLE, false)
    )
    val floatingBubbleEnabled: StateFlow<Boolean> = _floatingBubbleEnabled.asStateFlow()

    fun setFloatingBubbleEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_FLOATING_BUBBLE, enabled) }
        _floatingBubbleEnabled.value = enabled
    }

    fun isFloatingBubbleEnabled(): Boolean =
        prefs.getBoolean(KEY_FLOATING_BUBBLE, false)

    // ── Cache Size Limit ──────────────────────────────────────────────────

    private val _maxCacheMb = MutableStateFlow(
        prefs.getInt(KEY_MAX_CACHE_MB, DEFAULT_MAX_CACHE_MB)
    )
    val maxCacheMb: StateFlow<Int> = _maxCacheMb.asStateFlow()

    fun setMaxCacheMb(mb: Int) {
        prefs.edit { putInt(KEY_MAX_CACHE_MB, mb) }
        _maxCacheMb.value = mb
    }

    fun getMaxCacheMb(): Int = prefs.getInt(KEY_MAX_CACHE_MB, DEFAULT_MAX_CACHE_MB)

    // ── Statistik Anonim (Firebase Analytics) ─────────────────────────────
    // Default TRUE (opt-out): pengumpulan aktif kecuali user mematikannya.

    private val _analyticsEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_ANALYTICS_ENABLED, true)
    )
    val analyticsEnabled: StateFlow<Boolean> = _analyticsEnabled.asStateFlow()

    fun setAnalyticsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_ANALYTICS_ENABLED, enabled) }
        _analyticsEnabled.value = enabled
    }

    fun isAnalyticsEnabled(): Boolean =
        prefs.getBoolean(KEY_ANALYTICS_ENABLED, true)
}

