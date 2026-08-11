package com.editor.photo.video.collagemaker.photoedit.feature.text

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint


object BlurUtils {

    fun softBlur(source: Bitmap, downscaleFactor: Int = 16): Bitmap {
        val smallWidth = (source.width / downscaleFactor).coerceAtLeast(1)
        val smallHeight = (source.height / downscaleFactor).coerceAtLeast(1)

        val small = Bitmap.createScaledBitmap(source, smallWidth, smallHeight, true)
        val blurred = Bitmap.createScaledBitmap(small, source.width, source.height, true)
        if (small != blurred) small.recycle()
        return blurred
    }


    fun softBlurDimmed(source: Bitmap, targetWidth: Int, targetHeight: Int, dimAlpha: Int = 110): Bitmap {
        val blurred = softBlur(source)
        val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val srcRatio = blurred.width.toFloat() / blurred.height.toFloat()
        val dstRatio = targetWidth.toFloat() / targetHeight.toFloat()

        val src: android.graphics.Rect = if (srcRatio > dstRatio) {
            // Source is relatively wider than target — crop its left/right edges.
            val cropWidth = (blurred.height * dstRatio).toInt().coerceAtMost(blurred.width)
            val left = (blurred.width - cropWidth) / 2
            android.graphics.Rect(left, 0, left + cropWidth, blurred.height)
        } else {
            // Source is relatively taller than target — crop its top/bottom edges.
            val cropHeight = (blurred.width / dstRatio).toInt().coerceAtMost(blurred.height)
            val top = (blurred.height - cropHeight) / 2
            android.graphics.Rect(0, top, blurred.width, top + cropHeight)
        }
        val dst = android.graphics.Rect(0, 0, targetWidth, targetHeight)
        canvas.drawBitmap(blurred, src, dst, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.drawColor(android.graphics.Color.argb(dimAlpha, 0, 0, 0))
        if (blurred != source) blurred.recycle()
        return result
    }
}