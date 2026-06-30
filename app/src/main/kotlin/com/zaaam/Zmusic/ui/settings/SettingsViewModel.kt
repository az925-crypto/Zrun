package com.zaaam.Zmusic.ui.settings

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zaaam.Zmusic.analytics.AnalyticsManager
import com.zaaam.Zmusic.data.SettingsRepository
import com.zaaam.Zmusic.service.FloatingPlayerService
import com.zaaam.Zmusic.util.AudioDownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val downloadManager: AudioDownloadManager,
    private val analyticsManager: AnalyticsManager
) : AndroidViewModel(application) {

    val floatingBubbleEnabled: StateFlow<Boolean> =
        settingsRepository.floatingBubbleEnabled

    val maxCacheMb: StateFlow<Int> = settingsRepository.maxCacheMb

    val analyticsEnabled: StateFlow<Boolean> = settingsRepository.analyticsEnabled

    private val _storageUsedLabel = MutableStateFlow("0 MB")
    val storageUsedLabel: StateFlow<String> = _storageUsedLabel.asStateFlow()

    init {
        refreshStorageInfo()
    }

    fun refreshStorageInfo() {
        viewModelScope.launch {
            _storageUsedLabel.value = downloadManager.getStorageUsedLabel()
        }
    }

    fun setFloatingBubble(enabled: Boolean, context: Context) {
        if (enabled && !Settings.canDrawOverlays(context)) return
        settingsRepository.setFloatingBubbleEnabled(enabled)
        if (enabled) {
            context.startService(Intent(context, FloatingPlayerService::class.java))
        } else {
            context.stopService(Intent(context, FloatingPlayerService::class.java))
        }
    }

    fun setMaxCacheMb(mb: Int) {
        settingsRepository.setMaxCacheMb(mb)
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        settingsRepository.setAnalyticsEnabled(enabled)
        analyticsManager.setEnabled(enabled)
    }

    fun requestOverlayPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun hasOverlayPermission(context: Context): Boolean =
        Settings.canDrawOverlays(context)
}

