package com.editor.photo.video.collagemaker.photoedit.models.bottomsheets

data class EditorState(
    val baseImageUri: String? = null,
    val rotation: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val canvasZoom: Float = 1.0f,
    val aspectRatio: Float? = null,
    val imageTranslationX: Float = 0f,

    val filter: String? = null,
    val effects: List<EffectLayer> = emptyList(),
    val textLayers: List<TextLayer> = emptyList(),
    val stickerLayers: List<StickerLayer> = emptyList(),
    val cropData: CropData? = null,
    val frame: FrameLayer? = null,

    val doodlePaths: List<DoodlePath> = emptyList(),
    val background: Int? = null
)

/**
 * Represents a text overlay.
 *
 * Coordinates ([x], [y]) and [size] are NORMALIZED (0f..1f relative to the canvas width/height)
 * so that the same TextLayer renders at the correct proportional position on both the preview
 * bitmap and the full-resolution export bitmap.
 *
 *   pixelX = x * bitmap.width
 *   pixelY = y * bitmap.height
 *   pixelSize = size * bitmap.width   (proportional to width so landscape/portrait both scale right)
 */
data class TextLayer(
    val id: String,
    val text: String,
    /** Normalized x position (0f = left edge, 1f = right edge) */
    val x: Float = 0.5f,
    /** Normalized y position (0f = top edge, 1f = bottom edge) */
    val y: Float = 0.5f,
    /** Normalized text size (fraction of canvas width) */
    val size: Float = 0.05f,
    val color: Int = 0xFFFFFFFF.toInt(),
    val alpha: Int = 255,
    val rotation: Float = 0f,
    val fontFamily: String? = null,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val alignment: TextAlignment = TextAlignment.CENTER,
    /** Normalized stroke width (fraction of canvas width). 0f = no stroke/outline. */
    val strokeWidth: Float = 0f,
    val strokeColor: Int = 0xFF000000.toInt(),
    /** Extra spacing between letters, in Paint.letterSpacing em units. */
    val letterSpacing: Float = 0f
)

/** Horizontal text justification, applied around the TextLayer's anchor point. */
enum class TextAlignment {
    LEFT, CENTER, RIGHT
}

/**
 * Represents a sticker (emoji) overlay.
 *
 * Coordinates ([x], [y]) and [scale] are NORMALIZED (0f..1f) so the sticker renders
 * at the same proportional position on any bitmap size.
 *
 * The actual visual content is [emojiContent] (a Unicode emoji string).
 * [resourceId] is kept for backward compat but is -1 when not applicable.
 */
data class StickerLayer(
    val id: String,
    /** Unicode emoji string, e.g. "😀". Primary content for emoji stickers. */
    val emojiContent: String = "",
    /** Resource ID for drawable stickers; -1 if not used. */
    val resourceId: Int = -1,
    /** Normalized x position (0f..1f) */
    val x: Float = 0.5f,
    /** Normalized y position (0f..1f) */
    val y: Float = 0.5f,
    /** Scale factor (1f = default size) */
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val alpha: Int = 255
)

data class EffectLayer(
    val id: String,
    val name: String,
    val intensity: Float = 1.0f
)

data class CropData(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val aspectRatio: String = "free"
)

data class FrameLayer(
    val id: String,
    val resourceId: Int,
    val padding: Float = 0f,
    val frameName: String = ""
)
