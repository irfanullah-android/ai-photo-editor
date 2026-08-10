package com.editor.photo.video.collagemaker.photoedit.feature.frame

import androidx.lifecycle.ViewModel
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.FrameModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FrameViewModel : ViewModel() {
    private val _selectedFrame = MutableStateFlow<FrameModel?>(null)
    val selectedFrame: StateFlow<FrameModel?> = _selectedFrame.asStateFlow()

    fun selectFrame(frame: FrameModel) {
        _selectedFrame.value = frame
    }
}
