package com.editor.photo.video.collagemaker.photoedit.domain.usecase

import com.editor.photo.video.collagemaker.photoedit.domain.repository.EditorRepository
import com.editor.photo.video.collagemaker.photoedit.editor.engine.EditOperation
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.StickerLayer
import javax.inject.Inject

class ApplyStickerUseCase @Inject constructor(private val editorRepository: EditorRepository) {
    fun add(sticker: StickerLayer) {
        editorRepository.addOperation(EditOperation.AddSticker(sticker))
    }
    fun update(sticker: StickerLayer) {
        editorRepository.addOperation(EditOperation.UpdateSticker(sticker))
    }
    fun remove(stickerId: String) {
        editorRepository.addOperation(EditOperation.RemoveSticker(stickerId))
    }
}
