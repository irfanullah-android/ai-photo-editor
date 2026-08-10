package com.editor.photo.video.collagemaker.photoedit.domain.repository

import android.graphics.Bitmap
import android.net.Uri

interface CacheRepository {
    suspend fun cacheBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, ext: String): Uri?
    fun clearCache()
}
