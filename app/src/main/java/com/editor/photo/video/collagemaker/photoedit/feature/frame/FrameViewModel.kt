package com.editor.photo.video.collagemaker.photoedit.feature.frame

import androidx.lifecycle.ViewModel
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.domain.repository.AssetRepository
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.FrameModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class FrameViewModel @Inject constructor(
    private val assetRepository: AssetRepository
) : ViewModel() {

    private val frameNames = listOf(
        "Classic", "Modern", "Vintage", "Polaroid", "Film",
        "Wood", "Metal", "Gold", "Silver", "Black", "White"
    )

    private val _frames = MutableStateFlow<List<FrameModel>>(emptyList())
    val frames: StateFlow<List<FrameModel>> = _frames.asStateFlow()

    private val _selectedFrame = MutableStateFlow<FrameModel?>(null)
    val selectedFrame: StateFlow<FrameModel?> = _selectedFrame.asStateFlow()

    init {
        loadFrames()
    }

    private fun loadFrames() {
        val frameResIds = assetRepository.getFrames()
        val models = mutableListOf(FrameModel("None", R.drawable.ic_no_frame))
        frameResIds.forEachIndexed { index, resId ->
            val name = frameNames.getOrElse(index) { "Frame ${index + 1}" }
            models.add(FrameModel(name, resId))
        }
        _frames.value = models
    }

    fun selectFrame(frame: FrameModel) {
        _selectedFrame.value = frame
    }
}