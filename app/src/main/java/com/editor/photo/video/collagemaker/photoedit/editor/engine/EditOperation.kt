package com.editor.photo.video.collagemaker.photoedit.editor.engine

import android.graphics.Bitmap
import android.net.Uri
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.AdjustmentType
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorFilter
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EffectType
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.StickerLayer
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.TextLayer
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.FrameLayer
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.DoodlePath
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.CropData

sealed class EditOperation {
    data class Adjust(val adjustments: Map<AdjustmentType, Int>) : EditOperation()
    data class Filter(val filter: EditorFilter, val intensity: Int) : EditOperation()
    data class Effect(val effectType: EffectType, val intensity: Float) : EditOperation()
    data class Crop(val croppedUri: Uri, val cropData: CropData? = null) : EditOperation()
    data class Rotate(val rotation: Float, val flipHorizontal: Boolean, val flipVertical: Boolean) : EditOperation()
    data class Canvas(val aspectRatio: Float?, val zoom: Float, val translationX: Float) : EditOperation()
    data class AddSticker(val sticker: StickerLayer) : EditOperation()
    data class UpdateSticker(val sticker: StickerLayer) : EditOperation()
    data class RemoveSticker(val stickerId: String) : EditOperation()
    data class AddText(val text: TextLayer) : EditOperation()
    data class UpdateText(val text: TextLayer) : EditOperation()
    data class RemoveText(val textId: String) : EditOperation()
    data class ApplyFrame(val frame: FrameLayer?) : EditOperation()
    data class Doodle(val path: DoodlePath) : EditOperation()
    data class BackgroundRemoved(val bitmap: Bitmap) : EditOperation()
}