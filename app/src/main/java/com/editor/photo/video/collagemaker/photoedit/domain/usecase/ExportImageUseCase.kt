package com.editor.photo.video.collagemaker.photoedit.domain.usecase

import android.net.Uri
import com.editor.photo.video.collagemaker.photoedit.domain.repository.EditorRepository
import com.editor.photo.video.collagemaker.photoedit.domain.repository.ExportRepository
import javax.inject.Inject

class ExportImageUseCase @Inject constructor(
    private val editorRepository: EditorRepository,
    private val exportRepository: ExportRepository
) {
    suspend operator fun invoke(filename: String): Uri? {
        val highResBitmap = editorRepository.renderHighRes() ?: return null
        val outputUri = exportRepository.saveImageToMediaStore(highResBitmap, filename)
        highResBitmap.recycle()
        return outputUri
    }
}
