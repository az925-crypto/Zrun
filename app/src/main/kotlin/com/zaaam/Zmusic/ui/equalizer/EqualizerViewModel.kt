package com.zaaam.Zmusic.ui.equalizer

import androidx.lifecycle.ViewModel
import com.zaaam.Zmusic.util.EqPreset
import com.zaaam.Zmusic.util.EqualizerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    val equalizerManager: EqualizerManager
) : ViewModel() {

    val isEnabled: StateFlow<Boolean>       = equalizerManager.isEnabled
    val bandLevels: StateFlow<List<Int>>    = equalizerManager.bandLevels
    val bandFreqs: StateFlow<List<Int>>     = equalizerManager.bandFrequencies
    val levelRange: StateFlow<Pair<Int, Int>> = equalizerManager.levelRange
    val numberOfBands: StateFlow<Int>       = equalizerManager.numberOfBands
    val activePreset: StateFlow<EqPreset?>  = equalizerManager.activePreset

    init {
        // Attach ke audio session saat screen dibuka
        equalizerManager.ensureAttached()
    }

    fun setEnabled(enabled: Boolean) = equalizerManager.setEnabled(enabled)

    fun setBandLevel(band: Int, levelMb: Int) = equalizerManager.setBandLevel(band, levelMb)

    fun applyPreset(preset: EqPreset) = equalizerManager.applyPreset(preset)

    fun resetToFlat() = equalizerManager.resetToFlat()
}
