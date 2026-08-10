package com.editor.photo.video.collagemaker.photoedit.feature.effect

import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EffectType
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EffectViewModel : ViewModel() {
    private val _selectedEffect = MutableStateFlow(EffectType.NONE)
    val selectedEffect: StateFlow<EffectType> = _selectedEffect.asStateFlow()

    private val _intensity = MutableStateFlow(1.0f)
    val intensity: StateFlow<Float> = _intensity.asStateFlow()

    fun selectEffect(effect: EffectType) {
        _selectedEffect.value = effect
    }

    fun setIntensity(intensity: Float) {
        _intensity.value = intensity
    }
}
