package com.editor.photo.video.collagemaker.photoedit.domain.usecase

import android.net.Uri
import com.editor.photo.video.collagemaker.photoedit.domain.repository.EditorRepository
import javax.inject.Inject

class LoadImageUseCase @Inject constructor(private val editorRepository: EditorRepository) {
    suspend operator fun invoke(uri: Uri) {
        editorRepository.initialize(uri)
    }
}
