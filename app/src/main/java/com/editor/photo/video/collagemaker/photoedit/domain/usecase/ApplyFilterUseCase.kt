package com.editor.photo.video.collagemaker.photoedit.domain.usecase

import com.editor.photo.video.collagemaker.photoedit.domain.repository.EditorRepository
import com.editor.photo.video.collagemaker.photoedit.editor.engine.EditOperation
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorFilter
import javax.inject.Inject

class ApplyFilterUseCase @Inject constructor(private val editorRepository: EditorRepository) {
    operator fun invoke(filter: EditorFilter, intensity: Int) {
        editorRepository.addOperation(EditOperation.Filter(filter, intensity))
    }
}
