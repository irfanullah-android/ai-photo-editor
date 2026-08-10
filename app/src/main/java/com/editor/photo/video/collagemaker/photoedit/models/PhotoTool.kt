package com.editor.photo.video.collagemaker.photoedit.models

data class PhotoTool(
    val id: Int,
    val title: String,
    val imageRes: Int,
    val type: ToolType,
    val templateId: Int = 0,
    val prompt: String = ""
)