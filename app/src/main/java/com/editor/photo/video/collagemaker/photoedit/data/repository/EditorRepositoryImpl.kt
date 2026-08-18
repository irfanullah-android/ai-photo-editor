package com.editor.photo.video.collagemaker.photoedit.data.repository

import android.graphics.Bitmap
import android.net.Uri
import com.editor.photo.video.collagemaker.photoedit.domain.repository.EditorRepository
import com.editor.photo.video.collagemaker.photoedit.editor.engine.EditOperation
import com.editor.photo.video.collagemaker.photoedit.editor.engine.EditorEngine
import javax.inject.Inject

class EditorRepositoryImpl @Inject constructor(private val editorEngine: EditorEngine) : EditorRepository {
    override suspend fun initialize(uri: Uri) {
        editorEngine.initialize(uri)
    }

    override fun getBaseUri(): Uri? = editorEngine.getBaseUri()

    override fun getOriginalBitmap(): Bitmap? = editorEngine.getOriginalBitmap()

    override fun addOperation(operation: EditOperation) {
        editorEngine.addOperation(operation)
    }

    override fun undo(): Boolean = editorEngine.undo()

    override fun redo(): Boolean = editorEngine.redo()

    override fun canUndo(): Boolean = editorEngine.canUndo()

    override fun canRedo(): Boolean = editorEngine.canRedo()

    override fun getActiveOperations(): List<EditOperation> = editorEngine.getActiveOperations()

    override suspend fun renderPreview(
        widthLimit: Int,
        excludeTextId: String?,
        excludeStickerId: String?
    ): Bitmap? {
        return editorEngine.renderPreview(widthLimit, excludeTextId, excludeStickerId)
    }

    override suspend fun renderPreviewWithoutFrame(widthLimit: Int, excludeTextId: String?): Bitmap? {
        return editorEngine.renderPreviewWithoutFrame(widthLimit, excludeTextId)
    }

    override suspend fun renderHighRes(): Bitmap? {
        return editorEngine.renderHighRes()
    }

    override fun clear() {
        editorEngine.clean()
    }
}