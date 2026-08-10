package com.editor.photo.video.collagemaker.photoedit.domain.usecase
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EffectType
import com.editor.photo.video.collagemaker.photoedit.domain.repository.EditorRepository
import com.editor.photo.video.collagemaker.photoedit.editor.engine.EditOperation
import javax.inject.Inject

class ApplyEffectUseCase @Inject constructor(private val editorRepository: EditorRepository) {
    operator fun invoke(effectType: EffectType, intensity: Float) {
        editorRepository.addOperation(EditOperation.Effect(effectType, intensity))
    }
}