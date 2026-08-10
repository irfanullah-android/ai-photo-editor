package com.editor.photo.video.collagemaker.photoedit.models

import java.io.Serializable

data class AiTemplate(
    val id: Int,
    val imageResId: Int,
    val title: String,
    val prompt: String
) : Serializable