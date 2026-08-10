package com.editor.photo.video.collagemaker.photoedit.domain.usecase

import com.editor.photo.video.collagemaker.photoedit.domain.repository.EditorRepository
import com.editor.photo.video.collagemaker.photoedit.editor.engine.EditOperation
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorEnhance
import javax.inject.Inject

class ApplyEnhanceUseCase @Inject constructor(private val editorRepository: EditorRepository) {
    operator fun invoke(enhanceType: EditorEnhance, intensity: Float) {
        editorRepository.addOperation(EditOperation.Enhance(enhanceType, intensity))
    }
}
