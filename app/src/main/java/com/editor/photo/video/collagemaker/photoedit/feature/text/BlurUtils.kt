package com.editor.photo.video.collagemaker.photoedit.feature.text

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object BlurUtils {

    enum class BlurQuality(
        internal val downscale: Int,
        internal val passes: Int
    ) {
        DRAFT(32, 1),
        BALANCED(16, 2),
        CRISP(8, 3)
    }

    fun blur(
        source: Bitmap,
        quality: BlurQuality = BlurQuality.BALANCED
    ): Bitmap {
        var current = stackBlurPass(source, quality.downscale)
        repeat(quality.passes - 1) {
            val next = stackBlurPass(current, quality.downscale / 2)
            current.recycle()
            current = next
        }
        val result = Bitmap.createScaledBitmap(current, source.width, source.height, true)
        current.recycle()
        return result
    }

    fun blurWithOverlay(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        overlayColor: Int = Color.argb(110, 0, 0, 0),
        quality: BlurQuality = BlurQuality.BALANCED
    ): Bitmap {
        val blurred = blur(source, quality)
        val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val srcRect = centerCropRect(blurred.width, blurred.height, targetWidth, targetHeight)
        val dstRect = Rect(0, 0, targetWidth, targetHeight)

        canvas.drawBitmap(blurred, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.drawColor(overlayColor)

        blurred.recycle()
        return result
    }

    fun blurWithVignette(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        overlayColor: Int = Color.argb(110, 0, 0, 0),
        vignetteAlpha: Int = 180,
        quality: BlurQuality = BlurQuality.BALANCED
    ): Bitmap {
        val base = blurWithOverlay(source, targetWidth, targetHeight, overlayColor, quality)
        applyVignette(Canvas(base), targetWidth.toFloat(), targetHeight.toFloat(), vignetteAlpha)
        return base
    }

    fun softBlurDimmed(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        dimAlpha: Int = 110
    ): Bitmap = blurWithOverlay(
        source = source,
        targetWidth = targetWidth,
        targetHeight = targetHeight,
        overlayColor = Color.argb(dimAlpha, 255, 255, 255),
        quality = BlurQuality.BALANCED
    )

    private fun stackBlurPass(source: Bitmap, downscale: Int): Bitmap {
        val w = max(1, source.width / downscale)
        val h = max(1, source.height / downscale)
        val small = Bitmap.createScaledBitmap(source, w, h, true)

        val pixels = IntArray(w * h)
        small.getPixels(pixels, 0, w, 0, 0, w, h)
        boxBlurHorizontal(pixels, w, h)
        boxBlurVertical(pixels, w, h)
        small.setPixels(pixels, 0, w, 0, 0, w, h)

        return small
    }

    private fun boxBlurHorizontal(pixels: IntArray, width: Int, height: Int) {
        val radius = 2
        val div = (radius * 2 + 1).toFloat()
        for (y in 0 until height) {
            var rSum = 0; var gSum = 0; var bSum = 0
            for (x in -radius..radius) {
                val px = pixels[y * width + x.coerceIn(0, width - 1)]
                rSum += Color.red(px); gSum += Color.green(px); bSum += Color.blue(px)
            }
            for (x in 0 until width) {
                pixels[y * width + x] = Color.rgb(
                    (rSum / div).roundToInt().coerceIn(0, 255),
                    (gSum / div).roundToInt().coerceIn(0, 255),
                    (bSum / div).roundToInt().coerceIn(0, 255)
                )
                val removeX = (x - radius).coerceIn(0, width - 1)
                val addX    = (x + radius + 1).coerceIn(0, width - 1)
                val remove  = pixels[y * width + removeX]
                val add     = pixels[y * width + addX]
                rSum += Color.red(add)   - Color.red(remove)
                gSum += Color.green(add) - Color.green(remove)
                bSum += Color.blue(add)  - Color.blue(remove)
            }
        }
    }

    private fun boxBlurVertical(pixels: IntArray, width: Int, height: Int) {
        val radius = 2
        val div = (radius * 2 + 1).toFloat()
        for (x in 0 until width) {
            var rSum = 0; var gSum = 0; var bSum = 0
            for (y in -radius..radius) {
                val px = pixels[y.coerceIn(0, height - 1) * width + x]
                rSum += Color.red(px); gSum += Color.green(px); bSum += Color.blue(px)
            }
            for (y in 0 until height) {
                pixels[y * width + x] = Color.rgb(
                    (rSum / div).roundToInt().coerceIn(0, 255),
                    (gSum / div).roundToInt().coerceIn(0, 255),
                    (bSum / div).roundToInt().coerceIn(0, 255)
                )
                val removeY = (y - radius).coerceIn(0, height - 1)
                val addY    = (y + radius + 1).coerceIn(0, height - 1)
                val remove  = pixels[removeY * width + x]
                val add     = pixels[addY    * width + x]
                rSum += Color.red(add)   - Color.red(remove)
                gSum += Color.green(add) - Color.green(remove)
                bSum += Color.blue(add)  - Color.blue(remove)
            }
        }
    }

    private fun centerCropRect(srcW: Int, srcH: Int, dstW: Int, dstH: Int): Rect {
        val srcRatio = srcW.toFloat() / srcH
        val dstRatio = dstW.toFloat() / dstH
        return if (srcRatio > dstRatio) {
            val cropW = (srcH * dstRatio).toInt().coerceAtMost(srcW)
            val left  = (srcW - cropW) / 2
            Rect(left, 0, left + cropW, srcH)
        } else {
            val cropH = (srcW / dstRatio).toInt().coerceAtMost(srcH)
            val top   = (srcH - cropH) / 2
            Rect(0, top, srcW, top + cropH)
        }
    }

    private fun applyVignette(canvas: Canvas, width: Float, height: Float, alpha: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(
                width / 2f, height / 2f,
                max(width, height) * 0.7f,
                intArrayOf(Color.TRANSPARENT, Color.argb(alpha, 0, 0, 0)),
                floatArrayOf(0.4f, 1.0f),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(RectF(0f, 0f, width, height), paint)
    }
}