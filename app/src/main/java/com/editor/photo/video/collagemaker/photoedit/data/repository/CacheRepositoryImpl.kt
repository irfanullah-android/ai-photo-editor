package com.editor.photo.video.collagemaker.photoedit.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.editor.photo.video.collagemaker.photoedit.domain.repository.CacheRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class CacheRepositoryImpl @Inject constructor(@ApplicationContext private val context: Context) : CacheRepository {
    override suspend fun cacheBitmap(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        ext: String
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val cacheFile = File(
                context.cacheDir,
                "edited_${System.currentTimeMillis()}.$ext"
            )
            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(format, 95, out)
                out.flush()
            }
            Uri.fromFile(cacheFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun clearCache() {
        try {
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("edited_")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
