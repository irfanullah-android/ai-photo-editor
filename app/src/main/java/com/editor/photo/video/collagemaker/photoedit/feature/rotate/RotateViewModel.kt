package com.editor.photo.video.collagemaker.photoedit.feature.rotate

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RotateViewModel : ViewModel() {
    private val _rotation = MutableStateFlow(0f)
    val rotation: StateFlow<Float> = _rotation.asStateFlow()

    private val _flipHorizontal = MutableStateFlow(false)
    val flipHorizontal: StateFlow<Boolean> = _flipHorizontal.asStateFlow()

    private val _flipVertical = MutableStateFlow(false)
    val flipVertical: StateFlow<Boolean> = _flipVertical.asStateFlow()

    private val _zoom = MutableStateFlow(1f)
    val zoom: StateFlow<Float> = _zoom.asStateFlow()

    fun initState(initialRotation: Float, initialFlipHorizontal: Boolean, initialFlipVertical: Boolean, initialZoom: Float) {
        _rotation.value = initialRotation
        _flipHorizontal.value = initialFlipHorizontal
        _flipVertical.value = initialFlipVertical
        _zoom.value = initialZoom
    }

    fun rotate90() {
        _rotation.value = (_rotation.value + 90f) % 360f
    }

    fun setRotation(angle: Float) {
        _rotation.value = angle % 360f
    }

    fun toggleFlipHorizontal() {
        _flipHorizontal.value = !_flipHorizontal.value
    }

    fun toggleFlipVertical() {
        _flipVertical.value = !_flipVertical.value
    }

    fun setZoom(zoom: Float) {
        _zoom.value = zoom
    }
}
