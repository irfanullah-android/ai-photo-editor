package com.editor.photo.video.collagemaker.photoedit.models.bottomsheets

import ja.burhanrashid52.photoeditor.shape.ShapeType

data class BrushItem(
    val shapeType: ShapeType,
    val icon: Int,
    val name: String,
    val size: Float,
    var isSelected: Boolean = false
)