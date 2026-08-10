package com.editor.photo.video.collagemaker.photoedit.domain.usecase

import com.editor.photo.video.collagemaker.photoedit.domain.repository.EditorRepository
import com.editor.photo.video.collagemaker.photoedit.editor.engine.EditOperation
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.DoodlePath
import javax.inject.Inject

class ApplyDoodleUseCase @Inject constructor(private val editorRepository: EditorRepository) {
    operator fun invoke(path: DoodlePath) {
        editorRepository.addOperation(EditOperation.Doodle(path))
    }
}
