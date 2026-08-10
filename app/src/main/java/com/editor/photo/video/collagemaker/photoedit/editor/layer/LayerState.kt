package com.editor.photo.video.collagemaker.photoedit.editor.layer

data class LayerState(
    val layers: List<Layer> = emptyList(),
    val selectedLayerId: String? = null
)
