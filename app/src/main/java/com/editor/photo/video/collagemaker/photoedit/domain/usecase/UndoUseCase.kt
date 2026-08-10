package com.editor.photo.video.collagemaker.photoedit.domain.usecase

import com.editor.photo.video.collagemaker.photoedit.domain.repository.EditorRepository
import javax.inject.Inject

class UndoUseCase @Inject constructor(private val editorRepository: EditorRepository) {
    operator fun invoke(): Boolean {
        return editorRepository.undo()
    }
}
