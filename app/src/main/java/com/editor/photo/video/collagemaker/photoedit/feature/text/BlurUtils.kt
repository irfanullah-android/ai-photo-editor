package com.editor.photo.video.collagemaker.photoedit.feature.text

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

/**
 * Produces a cheap, dependency-free "blur" of [source] for use as a UI-only backdrop while the
 * text editing sheet is open. This never touches the actual source bitmap used for export —
 * EditorEngine/ExportImageUseCase are untouched and keep rendering full-quality output.
 *
 * Implementation: downscale to a small fraction of the original size (losing detail), then
 * scale back up with bilinear filtering. This gives a soft, blurred look without RenderScript
 * (deprecated) or a third-party blur library.
 */
object BlurUtils {

    fun softBlur(source: Bitmap, downscaleFactor: Int = 16): Bitmap {
        val smallWidth = (source.width / downscaleFactor).coerceAtLeast(1)
        val smallHeight = (source.height / downscaleFactor).coerceAtLeast(1)

        val small = Bitmap.createScaledBitmap(source, smallWidth, smallHeight, true)
        val blurred = Bitmap.createScaledBitmap(small, source.width, source.height, true)
        if (small != blurred) small.recycle()
        return blurred
    }

    /**
     * Draws [source] blurred + dimmed onto a bitmap of [targetWidth]x[targetHeight].
     *
     * Uses a center-crop ("cover") mapping so the photo's aspect ratio is preserved — it fills
     * the target fully with no empty bars, cropping any excess, rather than being stretched/
     * squashed to fit. This is purely a decorative full-bleed backdrop (the actual coordinate
     * reference for text placement is ivCanvasPhoto's letterboxed rect), but it should still
     * look like an undistorted version of the photo.
     */
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