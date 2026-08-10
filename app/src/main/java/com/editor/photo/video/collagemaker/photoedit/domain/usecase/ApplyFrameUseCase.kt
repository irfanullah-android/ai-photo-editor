package com.editor.photo.video.collagemaker.photoedit.domain.usecase

import com.editor.photo.video.collagemaker.photoedit.domain.repository.EditorRepository
import com.editor.photo.video.collagemaker.photoedit.editor.engine.EditOperation
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.FrameLayer
import javax.inject.Inject

class ApplyFrameUseCase @Inject constructor(private val editorRepository: EditorRepository) {
    operator fun invoke(frame: FrameLayer?) {
        editorRepository.addOperation(EditOperation.ApplyFrame(frame))
    }
}
