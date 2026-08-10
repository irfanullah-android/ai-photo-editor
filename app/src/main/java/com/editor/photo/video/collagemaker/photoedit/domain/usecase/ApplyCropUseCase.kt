package com.editor.photo.video.collagemaker.photoedit.domain.usecase

import android.net.Uri
import com.editor.photo.video.collagemaker.photoedit.domain.repository.EditorRepository
import com.editor.photo.video.collagemaker.photoedit.editor.engine.EditOperation
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.CropData
import javax.inject.Inject

class ApplyCropUseCase @Inject constructor(private val editorRepository: EditorRepository) {
    operator fun invoke(croppedUri: Uri, cropData: CropData? = null) {
        editorRepository.addOperation(EditOperation.Crop(croppedUri, cropData))
    }
}
