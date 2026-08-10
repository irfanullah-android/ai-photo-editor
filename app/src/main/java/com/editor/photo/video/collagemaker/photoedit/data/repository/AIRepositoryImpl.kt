package com.editor.photo.video.collagemaker.photoedit.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.editor.photo.video.collagemaker.photoedit.domain.repository.AIRepository
import com.editor.photo.video.collagemaker.photoedit.utlis.BackgroundRemover
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AIRepositoryImpl @Inject constructor(@ApplicationContext private val context: Context) : AIRepository {

    // BackgroundRemover.init loads a TFLite model from assets and constructs an Interpreter -
    // this is expensive (asset I/O + model init), so it is created once and reused for the
    // lifetime of this (ViewModelScoped) repository instance instead of per-call.
    private var backgroundRemover: BackgroundRemover? = null
    private val mutex = Mutex()

    override suspend fun removeBackground(bitmap: Bitmap): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val remover = mutex.withLock {
                backgroundRemover ?: BackgroundRemover(context).also { backgroundRemover = it }
            }
            remover.removeBackgroundSoft(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun release() {
        backgroundRemover?.close()
        backgroundRemover = null
    }
}
