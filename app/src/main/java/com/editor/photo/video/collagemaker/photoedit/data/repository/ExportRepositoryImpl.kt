package com.editor.photo.video.collagemaker.photoedit.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.editor.photo.video.collagemaker.photoedit.domain.repository.ExportRepository
import com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine.ImageProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ExportRepositoryImpl @Inject constructor(@ApplicationContext private val context: Context) : ExportRepository {
    private val imageProcessor = ImageProcessor(context)

    override suspend fun saveImageToMediaStore(bitmap: Bitmap, filename: String): Uri? = withContext(Dispatchers.IO) {
        try {
            imageProcessor.saveToMediaStore(context, bitmap, filename)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
