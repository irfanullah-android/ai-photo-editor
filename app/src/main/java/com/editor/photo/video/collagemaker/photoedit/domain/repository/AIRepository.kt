package com.editor.photo.video.collagemaker.photoedit.domain.repository

import android.graphics.Bitmap

interface AIRepository {
    suspend fun removeBackground(bitmap: Bitmap): Bitmap?

    /**
     * Releases any underlying AI/ML resources (e.g. TFLite interpreter) held by this
     * repository. Must be called when the owning session (ViewModel) is cleared.
     */
    fun release()
}
