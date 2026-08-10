package com.editor.photo.video.collagemaker.photoedit.models.bottomsheets

data class AdjustmentModel(
    val name: String,
    val iconRes: Int,
    val type: AdjustmentType,
    var value: Int = 0,
    val minValue: Int = -100,
    val maxValue: Int = 100,
    var isSelected: Boolean = false
)
