package com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.CropData
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorState
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EffectLayer
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.FrameLayer
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.StickerLayer
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.TextLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ImageProcessor(private val context: Context) {

    companion object {
        private const val TAG = "ImageProcessor"
        private const val DEFAULT_PREVIEW_WIDTH = 1080
        private const val DEFAULT_EXPORT_QUALITY = 95
    }


    suspend fun generatePreview(
        baseImageUri: Uri,
        state: EditorState,
        previewWidth: Int = DEFAULT_PREVIEW_WIDTH
    ): Bitmap = withContext(Dispatchers.Default) {
        processImage(baseImageUri, state, previewWidth, null)
    }


    suspend fun processImage(
        baseImageUri: Uri,
        state: EditorState,
        targetWidth: Int? = null,
        targetHeight: Int? = null
    ): Bitmap = withContext(Dispatchers.Default) {

        Log.d(TAG, "Processing image: ${targetWidth}x${targetHeight ?: "auto"}")
        var bitmap = loadBitmap(baseImageUri, targetWidth, targetHeight)
        bitmap = applyTransformations(bitmap, state)
        bitmap = applyFiltersAndEffects(bitmap, state)
        bitmap = renderLayers(bitmap, state)
        Log.d(TAG, "Processing complete: ${bitmap.width}x${bitmap.height}")
        bitmap
    }


    suspend fun loadBitmapForDisplay(uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        loadBitmap(uri, targetWidth = 1080, targetHeight = null)
    }


    suspend fun saveToMediaStore(
        context: Context,
        bitmap: Bitmap,
        filename: String,
        quality: Int = DEFAULT_EXPORT_QUALITY
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoFix")
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                ) ?: return@withContext null

                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                    stream.flush()
                }
                uri
            } else {
                @Suppress("DEPRECATION")
                val appDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "PhotoFix"
                ).also { if (!it.exists()) it.mkdirs() }

                val file = File(appDir, filename)
                file.outputStream().use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                    stream.flush()
                }
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save to MediaStore", e)
            null
        }
    }


    private suspend fun loadBitmap(
        uri: Uri,
        targetWidth: Int?,
        targetHeight: Int?
    ): Bitmap = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options()

        if (targetWidth != null || targetHeight != null) {
            options.inJustDecodeBounds = true
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
            options.inSampleSize = calculateInSampleSize(
                options.outWidth, options.outHeight,
                targetWidth ?: options.outWidth,
                targetHeight ?: options.outHeight
            )
            options.inJustDecodeBounds = false
        }

        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }?.also { bmp ->
            if (bmp.config != Bitmap.Config.ARGB_8888 || !bmp.isMutable) {
                val converted = bmp.copy(Bitmap.Config.ARGB_8888, true)
                bmp.recycle()
                return@withContext converted
            }
        } ?: throw IllegalStateException("Failed to load image: $uri")
    }

    private fun calculateInSampleSize(
        width: Int, height: Int,
        reqWidth: Int, reqHeight: Int
    ): Int {
        var sampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            var halfH = height / 2
            var halfW = width / 2
            while (halfH / sampleSize >= reqHeight && halfW / sampleSize >= reqWidth) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }


    private fun applyTransformations(bitmap: Bitmap, state: EditorState): Bitmap {
        var result = bitmap
        result = applyCrop(result, state.cropData)
        result = applyRotation(result, state.rotation)
        result = applyFlip(result, state.flipHorizontal, state.flipVertical)
        result = applyZoomAndAspectRatio(result, state.aspectRatio, state.canvasZoom)
        return result
    }

    private fun applyCrop(bitmap: Bitmap, cropData: CropData?): Bitmap {
        cropData ?: return bitmap
        return try {
            val left   = (cropData.left   * bitmap.width ).toInt().coerceIn(0, bitmap.width)
            val top    = (cropData.top    * bitmap.height).toInt().coerceIn(0, bitmap.height)
            val right  = (cropData.right  * bitmap.width ).toInt().coerceIn(0, bitmap.width)
            val bottom = (cropData.bottom * bitmap.height).toInt().coerceIn(0, bitmap.height)
            val w = (right - left).coerceAtLeast(1)
            val h = (bottom - top).coerceAtLeast(1)
            Bitmap.createBitmap(bitmap, left, top, w, h)
        } catch (e: Exception) {
            Log.e(TAG, "Crop failed", e)
            bitmap
        }
    }

    private fun applyRotation(bitmap: Bitmap, rotation: Float): Bitmap {
        if (rotation == 0f || bitmap.isRecycled) return bitmap
        return try {
            val matrix = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            Log.e(TAG, "Rotation failed", e)
            bitmap
        }
    }

    private fun applyFlip(bitmap: Bitmap, flipH: Boolean, flipV: Boolean): Bitmap {
        if ((!flipH && !flipV) || bitmap.isRecycled) return bitmap
        return try {
            val matrix = Matrix().apply {
                postScale(
                    if (flipH) -1f else 1f,
                    if (flipV) -1f else 1f,
                    bitmap.width / 2f,
                    bitmap.height / 2f
                )
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            Log.e(TAG, "Flip failed", e)
            bitmap
        }
    }

    private fun applyZoomAndAspectRatio(
        bitmap: Bitmap,
        aspectRatio: Float?,
        zoom: Float
    ): Bitmap {
        var result = bitmap

        // Zoom
        if (zoom != 1f && zoom > 0f) {
            val newW = (result.width * zoom).toInt().coerceAtLeast(1)
            val newH = (result.height * zoom).toInt().coerceAtLeast(1)
            result = try {
                Bitmap.createScaledBitmap(result, newW, newH, true)
            } catch (e: Exception) { result }
        }

        // Aspect ratio center-crop
        aspectRatio ?: return result
        val srcW = result.width.toFloat()
        val srcH = result.height.toFloat()
        val srcRatio = srcW / srcH

        var cropW = result.width
        var cropH = result.height
        var cropX = 0
        var cropY = 0

        if (srcRatio > aspectRatio) {
            cropW = (srcH * aspectRatio).toInt()
            cropX = ((srcW - cropW) / 2).toInt()
        } else {
            cropH = (srcW / aspectRatio).toInt()
            cropY = ((srcH - cropH) / 2).toInt()
        }

        return try {
            Bitmap.createBitmap(result, cropX, cropY, cropW.coerceAtLeast(1), cropH.coerceAtLeast(1))
        } catch (e: Exception) {
            Log.e(TAG, "AspectRatio crop failed", e)
            result
        }
    }


    private suspend fun applyFiltersAndEffects(bitmap: Bitmap, state: EditorState): Bitmap {
        var result = bitmap
        result = applyFilter(result, state.filter)
        result = applyEffects(result, state.effects)
        return result
    }

    private suspend fun applyFilter(bitmap: Bitmap, filterName: String?): Bitmap {
        filterName ?: return bitmap
        return withContext(Dispatchers.Default) {
            try {
                when (filterName) {
                    "grayscale" -> applyColorMatrix(bitmap) { setSaturation(0f) }
                    "sepia"     -> applySepiaMatrix(bitmap)
                    "vintage"   -> bitmap  // TODO: implement
                    else        -> { Log.w(TAG, "Unknown filter: $filterName"); bitmap }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Filter '$filterName' failed", e)
                bitmap
            }
        }
    }

    private fun applyColorMatrix(bitmap: Bitmap, configure: ColorMatrix.() -> Unit): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().also(configure))
        }
        canvas.drawBitmap(result, 0f, 0f, paint)
        return result
    }

    private fun applySepiaMatrix(bitmap: Bitmap): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(
                ColorMatrix(floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f,     0f,     0f,     1f, 0f
                ))
            )
        }
        canvas.drawBitmap(result, 0f, 0f, paint)
        return result
    }

    private suspend fun applyEffects(bitmap: Bitmap, effects: List<EffectLayer>): Bitmap {
        if (effects.isEmpty()) return bitmap
        var result = bitmap
        withContext(Dispatchers.Default) {
            effects.forEach { effect ->
                result = try {
                    when (effect.name) {
                        "brightness" -> adjustBrightness(result, effect.intensity)
                        "contrast"   -> adjustContrast(result, effect.intensity)
                        "saturation" -> adjustSaturation(result, effect.intensity)
                        "blur"       -> result
                        else         -> { Log.w(TAG, "Unknown effect: ${effect.name}"); result }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Effect '${effect.name}' failed", e)
                    result
                }
            }
        }
        return result
    }

    private fun adjustBrightness(bitmap: Bitmap, intensity: Float): Bitmap {
        val value = (intensity - 0.5f) * 255f
        return applyColorMatrix(bitmap) {
            set(floatArrayOf(
                1f, 0f, 0f, 0f, value,
                0f, 1f, 0f, 0f, value,
                0f, 0f, 1f, 0f, value,
                0f, 0f, 0f, 1f, 0f
            ))
        }
    }

    private fun adjustContrast(bitmap: Bitmap, intensity: Float): Bitmap {
        val scale = intensity * 2f
        val translate = (1f - scale) * 127.5f
        return applyColorMatrix(bitmap) {
            set(floatArrayOf(
                scale, 0f,    0f,    0f, translate,
                0f,    scale, 0f,    0f, translate,
                0f,    0f,    scale, 0f, translate,
                0f,    0f,    0f,    1f, 0f
            ))
        }
    }

    private fun adjustSaturation(bitmap: Bitmap, intensity: Float): Bitmap =
        applyColorMatrix(bitmap) { setSaturation(intensity) }



    private fun renderLayers(workingBitmap: Bitmap, state: EditorState): Bitmap {
        if (state.frame == null &&
            state.stickerLayers.isEmpty() &&
            state.textLayers.isEmpty() &&
            state.doodlePaths.isEmpty()
        ) return workingBitmap

        val finalBitmap = Bitmap.createBitmap(workingBitmap.width, workingBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)
        canvas.drawBitmap(workingBitmap, 0f, 0f, null)

        drawFrame(canvas, state.frame, finalBitmap.width, finalBitmap.height)
        drawStickers(canvas, state.stickerLayers)
        drawTextLayers(canvas, state.textLayers)

        return finalBitmap
    }

    private fun drawFrame(canvas: Canvas, frame: FrameLayer?, width: Int, height: Int) {
        frame ?: return
        try {
            @Suppress("DEPRECATION")
            val drawable = context.resources.getDrawable(frame.resourceId, null)
            val padding = frame.padding.toInt()
            drawable?.setBounds(padding, padding, width - padding, height - padding)
            drawable?.draw(canvas)
        } catch (e: Exception) {
            Log.e(TAG, "Frame draw failed", e)
        }
    }

    private fun drawStickers(canvas: Canvas, stickers: List<StickerLayer>) {
        stickers.forEach { sticker ->
            try {
                @Suppress("DEPRECATION")
                val drawable = context.resources.getDrawable(sticker.resourceId, null)
                drawable?.let {
                    canvas.save()
                    canvas.translate(sticker.x, sticker.y)
                    canvas.rotate(sticker.rotation, 0f, 0f)
                    canvas.scale(sticker.scale, sticker.scale)
                    it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
                    it.draw(canvas)
                    canvas.restore()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sticker draw failed: ${sticker.id}", e)
            }
        }
    }

    private fun drawTextLayers(canvas: Canvas, textLayers: List<TextLayer>) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }
        textLayers.forEach { text ->
            try {
                paint.color = text.color
                paint.textSize = text.size
                canvas.save()
                canvas.rotate(text.rotation, text.x, text.y)
                canvas.drawText(text.text, text.x, text.y, paint)
                canvas.restore()
            } catch (e: Exception) {
                Log.e(TAG, "Text draw failed: ${text.id}", e)
            }
        }
    }
}