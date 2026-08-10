package com.editor.photo.video.collagemaker.photoedit.models

import android.graphics.Bitmap

sealed class GenerationResult {
    object Idle : GenerationResult()
    object Loading : GenerationResult()
    data class Success(val bitmap: Bitmap) : GenerationResult()
    data class Error(val message: String) : GenerationResult()
}