package com.editor.photo.video.collagemaker.photoedit.feature.canvas

import androidx.lifecycle.ViewModel
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.AspectRatio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CanvasViewModel : ViewModel() {
    private val _selectedRatio = MutableStateFlow<AspectRatio>(AspectRatio.NO_FRAME)
    val selectedRatio: StateFlow<AspectRatio> = _selectedRatio.asStateFlow()

    private val _zoomProgress = MutableStateFlow(50)
    val zoomProgress: StateFlow<Int> = _zoomProgress.asStateFlow()

    fun selectRatio(ratio: AspectRatio) {
        _selectedRatio.value = ratio
    }

    fun setZoomProgress(progress: Int) {
        _zoomProgress.value = progress
    }
}
