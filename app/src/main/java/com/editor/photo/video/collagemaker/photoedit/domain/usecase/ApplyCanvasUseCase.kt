package com.editor.photo.video.collagemaker.photoedit.domain.usecase

import com.editor.photo.video.collagemaker.photoedit.domain.repository.EditorRepository
import com.editor.photo.video.collagemaker.photoedit.editor.engine.EditOperation
import javax.inject.Inject

class ApplyCanvasUseCase @Inject constructor(private val editorRepository: EditorRepository) {
    operator fun invoke(aspectRatio: Float?, zoom: Float, translationX: Float) {
        editorRepository.addOperation(EditOperation.Canvas(aspectRatio, zoom, translationX))
    }
}
