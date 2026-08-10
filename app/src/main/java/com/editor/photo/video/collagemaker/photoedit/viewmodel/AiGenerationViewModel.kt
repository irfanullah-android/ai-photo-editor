package com.editor.photo.video.collagemaker.photoedit.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.editor.photo.video.collagemaker.photoedit.models.AiTemplate
import com.editor.photo.video.collagemaker.photoedit.models.GenerationResult
import com.editor.photo.video.collagemaker.photoedit.models.PhotoTool
import com.editor.photo.video.collagemaker.photoedit.repository.AiGenerationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

class AiGenerationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AiGenerationRepository(application.contentResolver)

    private val _result = MutableLiveData<GenerationResult>()
    val result: LiveData<GenerationResult> = _result

    private val _generatedBitmap = MutableLiveData<Bitmap?>()
    val generatedBitmap: LiveData<Bitmap?> = _generatedBitmap

    private val _elapsedSeconds = MutableLiveData<Int>(0)
    val elapsedSeconds: LiveData<Int> = _elapsedSeconds

    private var generationAttempt = 0
    private val maxRetries = 3
    private var generationJob: Job? = null
    private var timerJob: Job? = null

    private fun retryDelay(attempt: Int): Long = when (attempt) {
        1 -> 1000L
        2 -> 2000L
        else -> 3000L
    }

    // ── Warm Up ───────────────────────────────────────────────────────────────

    fun warmUp() {
        viewModelScope.launch(Dispatchers.IO) { repository.pingServer() }
    }

    // ── Timer ─────────────────────────────────────────────────────────────────

    private fun startTimer() {
        timerJob?.cancel()
        _elapsedSeconds.value = 0
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedSeconds.value = (_elapsedSeconds.value ?: 0) + 1
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

    fun clearResult() {
        generationJob?.cancel()
        generationJob = null
        stopTimer()
        generationAttempt = 0
        _result.postValue(GenerationResult.Idle)
        _elapsedSeconds.postValue(0)
    }

    override fun onCleared() {
        super.onCleared()
        generationJob?.cancel()
        timerJob?.cancel()
        _generatedBitmap.value?.recycle()
        _generatedBitmap.value = null
    }

    // ── Success handler ───────────────────────────────────────────────────────

    private fun handleSuccess(bitmap: Bitmap) {
        stopTimer()
        _generatedBitmap.postValue(bitmap)
        _result.postValue(GenerationResult.Success(bitmap))
    }

    // ── Bitmap Similarity Checkers ──────────────────────────────────────────

    private fun isSameAsTemplate(generatedBitmap: Bitmap, templateResId: Int): Boolean {
        return try {
            val options = BitmapFactory.Options().apply {
                inSampleSize = 4
            }
            val templateBitmap = BitmapFactory.decodeResource(
                getApplication<Application>().resources,
                templateResId,
                options
            ) ?: return false

            val isSimilar = areBitmapsSimilar(generatedBitmap, templateBitmap, 0.95)

            // <--- YE LOGS ADD KAREIN --->
            Log.d("AiGenerationVM", "=== SIMILARITY CHECK ===")
            Log.d("AiGenerationVM", "Is image same as template? $isSimilar")

            templateBitmap.recycle()
            isSimilar
        } catch (e: Exception) {
            Log.e("AiGenerationVM", "Similarity check failed with exception: ${e.message}")
            false
        }
    }

    private fun areBitmapsSimilar(bmp1: Bitmap, bmp2: Bitmap, threshold: Double = 0.95): Boolean {
        // Dono images ko 16x16 grid par downscale karein taake size independent check ho
        val scaled1 = Bitmap.createScaledBitmap(bmp1, 16, 16, true)
        val scaled2 = Bitmap.createScaledBitmap(bmp2, 16, 16, true)

        var diff = 0L
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                val color1 = scaled1.getPixel(x, y)
                val color2 = scaled2.getPixel(x, y)

                val r1 = (color1 shr 16) and 0xff
                val g1 = (color1 shr 8) and 0xff
                val b1 = color1 and 0xff

                val r2 = (color2 shr 16) and 0xff
                val g2 = (color2 shr 8) and 0xff
                val b2 = color2 and 0xff

                diff += abs(r1 - r2) + abs(g1 - g2) + abs(b1 - b2)
            }
        }

        scaled1.recycle()
        scaled2.recycle()

        val maxDiff = 16 * 16 * 3 * 255.0
        val similarity = 1.0 - (diff / maxDiff)
        return similarity >= threshold
    }

    // ── Group Photo ───────────────────────────────────────────────────────────

    fun generate(person1Uri: Uri, person2Uri: Uri, template: AiTemplate) {
        generationAttempt = 0
        _result.value = GenerationResult.Loading
        startTimer()
        attemptGeneration(person1Uri, person2Uri, template)
    }

    private fun attemptGeneration(person1Uri: Uri, person2Uri: Uri, template: AiTemplate) {
        generationAttempt++
        generationJob = viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    repository.generate(
                        person1Uri, person2Uri,
                        template.imageResId, template.prompt,
                        getApplication<Application>().resources
                    )
                }
                if (bitmap.width < 100 || bitmap.height < 100) {
                    bitmap.recycle()
                    retryOrFail(person1Uri, person2Uri, template, "Image too small")
                } else if (isSameAsTemplate(bitmap, template.imageResId)) {
                    bitmap.recycle()
                    retryOrFail(person1Uri, person2Uri, template, "Unchanged template image returned")
                } else {
                    handleSuccess(bitmap)
                }
            } catch (e: Exception) {
                retryOrFail(person1Uri, person2Uri, template, e.localizedMessage ?: "Unknown error")
            }
        }
    }

    private fun retryOrFail(person1Uri: Uri, person2Uri: Uri, template: AiTemplate, reason: String) {
        if (generationAttempt < maxRetries) {
            generationJob?.cancel()
            generationJob = viewModelScope.launch {
                delay(retryDelay(generationAttempt))
                attemptGeneration(person1Uri, person2Uri, template)
            }
        } else {
            stopTimer()
            _result.value = GenerationResult.Error("Failed after $maxRetries attempts: $reason")
        }
    }

    // ── Single via AiTemplate ─────────────────────────────────────────────────

    fun generateSingleFromTemplate(personUri: Uri, template: AiTemplate) {
        generationAttempt = 0
        _result.value = GenerationResult.Loading
        startTimer()
        attemptSingleFromTemplate(personUri, template)
    }

    private fun attemptSingleFromTemplate(personUri: Uri, template: AiTemplate) {
        generationAttempt++
        generationJob = viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    repository.generateSingle(
                        personUri,
                        template.imageResId,
                        template.prompt,
                        getApplication<Application>().resources
                    )
                }
                if (bitmap.width < 100 || bitmap.height < 100) {
                    bitmap.recycle()
                    retryOrFailFromTemplate(personUri, template, "Image too small")
                } else if (isSameAsTemplate(bitmap, template.imageResId)) {
                    bitmap.recycle()
                    retryOrFailFromTemplate(personUri, template, "Unchanged template image returned")
                } else {
                    handleSuccess(bitmap)
                }
            } catch (e: Exception) {
                retryOrFailFromTemplate(personUri, template, e.localizedMessage ?: "Unknown error")
            }
        }
    }

    private fun retryOrFailFromTemplate(personUri: Uri, template: AiTemplate, reason: String) {
        if (generationAttempt < maxRetries) {
            generationJob?.cancel()
            generationJob = viewModelScope.launch {
                delay(retryDelay(generationAttempt))
                attemptSingleFromTemplate(personUri, template)
            }
        } else {
            stopTimer()
            _result.value = GenerationResult.Error("Failed after $maxRetries attempts: $reason")
        }
    }

    // ── Single via PhotoTool ──────────────────────────────────────────────────

    fun generateSingle(personUri: Uri, tool: PhotoTool) {
        generationAttempt = 0
        _result.value = GenerationResult.Loading
        startTimer()
        attemptSingleGeneration(personUri, tool)
    }

    private fun attemptSingleGeneration(personUri: Uri, tool: PhotoTool) {
        generationAttempt++
        generationJob = viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    repository.generateSingle(
                        personUri,
                        tool.imageRes,
                        tool.prompt,
                        getApplication<Application>().resources
                    )
                }
                if (bitmap.width < 100 || bitmap.height < 100) {
                    bitmap.recycle()
                    retryOrFailSingle(personUri, tool, "Image too small")
                } else if (isSameAsTemplate(bitmap, tool.imageRes)) {
                    bitmap.recycle()
                    retryOrFailSingle(personUri, tool, "Unchanged template image returned")
                } else {
                    handleSuccess(bitmap)
                }
            } catch (e: Exception) {
                retryOrFailSingle(personUri, tool, e.localizedMessage ?: "Unknown error")
            }
        }
    }

    private fun retryOrFailSingle(personUri: Uri, tool: PhotoTool, reason: String) {
        if (generationAttempt < maxRetries) {
            generationJob?.cancel()
            generationJob = viewModelScope.launch {
                delay(retryDelay(generationAttempt))
                attemptSingleGeneration(personUri, tool)
            }
        } else {
            stopTimer()
            _result.value = GenerationResult.Error("Failed after $maxRetries attempts: $reason")
        }
    }
}