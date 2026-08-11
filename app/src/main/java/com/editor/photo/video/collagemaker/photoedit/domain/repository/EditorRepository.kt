package com.editor.photo.video.collagemaker.photoedit.domain.repository

import android.graphics.Bitmap
import android.net.Uri
import com.editor.photo.video.collagemaker.photoedit.editor.engine.EditOperation

interface EditorRepository {
    suspend fun initialize(uri: Uri)
    fun getBaseUri(): Uri?
    fun getOriginalBitmap(): Bitmap?
    fun addOperation(operation: EditOperation)
    fun undo(): Boolean
    fun redo(): Boolean
    fun canUndo(): Boolean
    fun canRedo(): Boolean
    fun getActiveOperations(): List<EditOperation>
    suspend fun renderPreview(widthLimit: Int = 1080, excludeTextId: String? = null): Bitmap?
    suspend fun renderPreviewWithoutFrame(widthLimit: Int = 1080, excludeTextId: String? = null): Bitmap?
    suspend fun renderHighRes(): Bitmap?
    fun clear()
}
