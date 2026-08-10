package com.editor.photo.video.collagemaker.photoedit.editor.layer

import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.DoodlePath

sealed class Layer {
    abstract val id: String
    abstract val zIndex: Int
    abstract val isVisible: Boolean

    data class Sticker(
        override val id: String,
        override val zIndex: Int,
        override val isVisible: Boolean,
        val emojiContent: String,
        val resourceId: Int,
        val x: Float,
        val y: Float,
        val scale: Float,
        val rotation: Float,
        val alpha: Int = 255
    ) : Layer()

    data class Text(
        override val id: String,
        override val zIndex: Int,
        override val isVisible: Boolean,
        val text: String,
        val x: Float,
        val y: Float,
        val size: Float,
        val color: Int,
        val alpha: Int = 255,
        val rotation: Float,
        val fontFamily: String?,
        val isBold: Boolean = false,
        val isItalic: Boolean = false
    ) : Layer()

    data class Frame(
        override val id: String,
        override val zIndex: Int,
        override val isVisible: Boolean,
        val resourceId: Int,
        val padding: Float
    ) : Layer()

    data class Doodle(
        override val id: String,
        override val zIndex: Int,
        override val isVisible: Boolean,
        // Full drawing data (points, color, stroke width, shape type) is embedded directly
        // rather than referenced by id, so the layer can always be reconstructed/rendered
        // independently of the operation history it was derived from.
        val doodlePath: DoodlePath
    ) : Layer()
}
