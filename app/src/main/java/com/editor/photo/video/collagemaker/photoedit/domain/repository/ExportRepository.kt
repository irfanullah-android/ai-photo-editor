package com.editor.photo.video.collagemaker.photoedit.domain.repository

import android.graphics.Bitmap
import android.net.Uri

interface ExportRepository {
    suspend fun saveImageToMediaStore(bitmap: Bitmap, filename: String): Uri?
}
