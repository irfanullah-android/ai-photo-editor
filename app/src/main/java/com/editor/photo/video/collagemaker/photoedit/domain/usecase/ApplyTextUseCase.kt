package com.editor.photo.video.collagemaker.photoedit.domain.usecase

import com.editor.photo.video.collagemaker.photoedit.domain.repository.EditorRepository
import com.editor.photo.video.collagemaker.photoedit.editor.engine.EditOperation
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.TextLayer
import javax.inject.Inject

class ApplyTextUseCase @Inject constructor(private val editorRepository: EditorRepository) {
    fun add(text: TextLayer) {
        editorRepository.addOperation(EditOperation.AddText(text))
    }
    fun update(text: TextLayer) {
        editorRepository.addOperation(EditOperation.UpdateText(text))
    }
    fun remove(textId: String) {
        editorRepository.addOperation(EditOperation.RemoveText(textId))
    }
}
