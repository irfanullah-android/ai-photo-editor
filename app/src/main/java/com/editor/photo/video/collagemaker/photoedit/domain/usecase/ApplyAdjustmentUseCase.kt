package com.editor.photo.video.collagemaker.photoedit.domain.usecase

import com.editor.photo.video.collagemaker.photoedit.domain.repository.EditorRepository
import com.editor.photo.video.collagemaker.photoedit.editor.engine.EditOperation
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.AdjustmentType
import javax.inject.Inject

class ApplyAdjustmentUseCase @Inject constructor(private val editorRepository: EditorRepository) {
    operator fun invoke(adjustments: Map<AdjustmentType, Int>) {
        editorRepository.addOperation(EditOperation.Adjust(adjustments))
    }
}
