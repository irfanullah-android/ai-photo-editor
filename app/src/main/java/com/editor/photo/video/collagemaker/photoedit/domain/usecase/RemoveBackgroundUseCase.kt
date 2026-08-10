package com.editor.photo.video.collagemaker.photoedit.domain.usecase

import android.graphics.Bitmap
import com.editor.photo.video.collagemaker.photoedit.domain.repository.AIRepository
import com.editor.photo.video.collagemaker.photoedit.domain.repository.EditorRepository
import com.editor.photo.video.collagemaker.photoedit.editor.engine.EditOperation
import javax.inject.Inject

class RemoveBackgroundUseCase @Inject constructor(
    private val aiRepository: AIRepository,
    private val editorRepository: EditorRepository
) {
    suspend operator fun invoke(bitmap: Bitmap): Bitmap? {
        val result = aiRepository.removeBackground(bitmap)
        if (result != null) {
            editorRepository.addOperation(EditOperation.BackgroundRemoved(result))
        }
        return result
    }
}
