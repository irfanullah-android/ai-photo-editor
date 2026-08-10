package com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EffectType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Central engine that turns an [EffectType] into a fully rendered [Bitmap].
 *
 * Every effect is a pure function: it never mutates [source] and always
 * returns a new bitmap (except [EffectType.NONE], which is a pass-through).
 * Callers are responsible for recycling bitmaps they no longer need.
 *
 * NOTE: every intermediate bitmap allocated *inside* an effect is recycled
 * as soon as it's no longer needed. Full-size ARGB_8888 bitmaps are large
 * (width * height * 4 bytes) and effects like bloom/oilPaint/doubleExposure
 * were previously creating 2-4 of them per call without recycling, which
 * pinned the Java heap against its growth limit and caused OOMs even though
 * the actual "live" working set at any instant was much smaller.
 */
object EffectsEngine {

    fun apply(type: EffectType, source: Bitmap): Bitmap = when (type) {
        EffectType.NONE -> source
        EffectType.HDR -> hdr(source)
        EffectType.VINTAGE -> vintage(source)
        EffectType.CINEMATIC -> cinematic(source)
        EffectType.BLACK_WHITE -> blackWhite(source)
        EffectType.SEPIA -> sepia(source)
        EffectType.BLOOM -> bloom(source)
        EffectType.SOFT_FOCUS -> softFocus(source)
        EffectType.OIL_PAINT -> oilPaint(source)
        EffectType.MATTE -> matte(source)
        EffectType.PIXELATE -> pixelate(source, blockSize = 14)
        EffectType.GRAIN -> grain(source, intensity = 0.6f)
        EffectType.DUOTONE -> duotone(source)
        EffectType.DOUBLE_EXPOSURE -> doubleExposure(source)
    }

    // ---------------------------------------------------------------------
    // Color-matrix based effects
    // ---------------------------------------------------------------------

    private fun blackWhite(source: Bitmap): Bitmap {
        val matrix = ColorMatrix().apply { setSaturation(0f) }
        return applyColorMatrix(source, matrix)
    }

    private fun sepia(source: Bitmap): Bitmap {
        val desaturate = ColorMatrix().apply { setSaturation(0f) }
        val tone = ColorMatrix().apply { setScale(1.07f, 0.94f, 0.71f, 1f) }
        desaturate.postConcat(tone)
        return applyColorMatrix(source, desaturate)
    }

    private fun hdr(source: Bitmap): Bitmap {
        val contrast = 1.25f
        val translate = (-0.5f * contrast + 0.5f) * 255f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val saturation = ColorMatrix().apply { setSaturation(1.3f) }
        contrastMatrix.postConcat(saturation)

        val boosted = applyColorMatrix(source, contrastMatrix)
        val result = unsharpMask(boosted, radius = 6, amount = 0.45f)
        boosted.recycle()
        return result
    }

    private fun vintage(source: Bitmap): Bitmap {
        val fade = ColorMatrix(
            floatArrayOf(
                0.9f, 0f, 0f, 0f, 20f,
                0f, 0.85f, 0f, 0f, 15f,
                0f, 0f, 0.8f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val desaturate = ColorMatrix().apply { setSaturation(0.75f) }
        fade.postConcat(desaturate)

        val faded = applyColorMatrix(source, fade)
        val result = applyVignette(faded, strength = 0.45f)
        faded.recycle()
        return result
    }

    private fun cinematic(source: Bitmap): Bitmap {
        // Teal shadows / orange highlights split-tone, plus a touch of contrast.
        val contrast = 1.1f
        val translate = (-0.5f * contrast + 0.5f) * 255f
        val grade = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate + 8f,   // slightly warmer reds
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate - 6f,   // pull blues toward teal
                0f, 0f, 0f, 1f, 0f
            )
        )
        val saturation = ColorMatrix().apply { setSaturation(1.05f) }
        grade.postConcat(saturation)
        return applyColorMatrix(source, grade)
    }

    private fun matte(source: Bitmap): Bitmap {
        // Classic "faded film" matte look: blacks lifted off the floor,
        // contrast pulled in slightly, colors gently muted.
        val contrast = 0.85f
        val translate = (-0.5f * contrast + 0.5f) * 255f + 18f
        val fade = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate + 4f, // touch of cool in the shadows
                0f, 0f, 0f, 1f, 0f
            )
        )
        val desaturate = ColorMatrix().apply { setSaturation(0.9f) }
        fade.postConcat(desaturate)
        return applyColorMatrix(source, fade)
    }

    private fun applyColorMatrix(source: Bitmap, matrix: ColorMatrix): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    // ---------------------------------------------------------------------
    // Blur / glow based effects
    // ---------------------------------------------------------------------

    private fun bloom(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        // Isolate bright regions to use as the glow source.
        val bright = IntArray(w * h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val luma = 0.299f * Color.red(p) + 0.587f * Color.green(p) + 0.114f * Color.blue(p)
            bright[i] = if (luma > 170) p else Color.argb(Color.alpha(p), 0, 0, 0)
        }
        val brightBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        brightBitmap.setPixels(bright, 0, w, 0, 0, w, h)

        val glow = boxBlur(brightBitmap, radius = 14)
        brightBitmap.recycle()

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)
        val screenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        }
        canvas.drawBitmap(glow, 0f, 0f, screenPaint)
        glow.recycle()
        return result
    }

    private fun softFocus(source: Bitmap): Bitmap {
        val blurred = boxBlur(source, radius = 10)
        val w = source.width
        val h = source.height
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = 110 // soft, translucent glow over the sharp base
        }
        canvas.drawBitmap(blurred, 0f, 0f, glowPaint)
        blurred.recycle()
        return result
    }

    /**
     * Maps every pixel's luminance onto a two-color gradient (deep indigo
     * shadows -> warm peach highlights). Single pass, no blur — cheap enough
     * to stay smooth even when many thumbnails render at once.
     */
    private fun duotone(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val shadowR = 20; val shadowG = 20; val shadowB = 60
        val highlightR = 255; val highlightG = 200; val highlightB = 120

        val out = IntArray(w * h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val luma = (0.299f * Color.red(p) + 0.587f * Color.green(p) + 0.114f * Color.blue(p)) / 255f
            val r = (shadowR + (highlightR - shadowR) * luma).roundToInt().coerceIn(0, 255)
            val g = (shadowG + (highlightG - shadowG) * luma).roundToInt().coerceIn(0, 255)
            val b = (shadowB + (highlightB - shadowB) * luma).roundToInt().coerceIn(0, 255)
            out[i] = Color.argb(Color.alpha(p), r, g, b)
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, w, 0, 0, w, h)
        return result
    }

    private fun doubleExposure(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height

        // Second "exposure": the image flipped vertically and slightly blurred,
        // desaturated so it reads as a ghosted overlay rather than a duplicate.
        val flipped = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val flipCanvas = Canvas(flipped)
        flipCanvas.save()
        flipCanvas.scale(1f, -1f, w / 2f, h / 2f)
        flipCanvas.drawBitmap(source, 0f, 0f, null)
        flipCanvas.restore()

        val desaturate = ColorMatrix().apply { setSaturation(0.4f) }
        val desaturated = applyColorMatrix(flipped, desaturate)
        flipped.recycle()

        val blurredFlip = boxBlur(desaturated, radius = 3)
        desaturated.recycle()

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)
        val screenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            alpha = 190
        }
        canvas.drawBitmap(blurredFlip, 0f, 0f, screenPaint)
        blurredFlip.recycle()
        return result
    }

    // ---------------------------------------------------------------------
    // Pixel-level effects
    // ---------------------------------------------------------------------

    private fun pixelate(source: Bitmap, blockSize: Int): Bitmap {
        val w = source.width
        val h = source.height
        val block = max(1, blockSize)

        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val out = IntArray(w * h)

        var by = 0
        while (by < h) {
            val blockHeight = min(block, h - by)
            var bx = 0
            while (bx < w) {
                val blockWidth = min(block, w - bx)

                var sumA = 0L; var sumR = 0L; var sumG = 0L; var sumB = 0L
                var count = 0
                for (y in by until by + blockHeight) {
                    val rowStart = y * w
                    for (x in bx until bx + blockWidth) {
                        val p = pixels[rowStart + x]
                        sumA += Color.alpha(p); sumR += Color.red(p)
                        sumG += Color.green(p); sumB += Color.blue(p)
                        count++
                    }
                }
                val avg = Color.argb(
                    (sumA / count).toInt(),
                    (sumR / count).toInt(),
                    (sumG / count).toInt(),
                    (sumB / count).toInt()
                )
                for (y in by until by + blockHeight) {
                    val rowStart = y * w
                    for (x in bx until bx + blockWidth) {
                        out[rowStart + x] = avg
                    }
                }
                bx += block
            }
            by += block
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, w, 0, 0, w, h)
        return result
    }

    private fun grain(source: Bitmap, intensity: Float): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val strength = (intensity.coerceIn(0f, 1f) * 40).toInt()
        val random = Random(System.nanoTime())
        val out = IntArray(w * h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val noise = random.nextInt(-strength, strength + 1)
            val r = (Color.red(p) + noise).coerceIn(0, 255)
            val g = (Color.green(p) + noise).coerceIn(0, 255)
            val b = (Color.blue(p) + noise).coerceIn(0, 255)
            out[i] = Color.argb(Color.alpha(p), r, g, b)
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, w, 0, 0, w, h)
        return result
    }

    /**
     * A lightweight, performant approximation of an oil-painting look: colors are
     * quantized into bands (posterize) and then softened with a blur so brush-stroke
     * style blobs form, instead of running a full most-common-color kernel per pixel.
     */
    private fun oilPaint(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val levels = 6
        val step = 256 / levels
        val posterized = IntArray(w * h)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = ((Color.red(p) / step) * step).coerceIn(0, 255)
            val g = ((Color.green(p) / step) * step).coerceIn(0, 255)
            val b = ((Color.blue(p) / step) * step).coerceIn(0, 255)
            posterized[i] = Color.argb(Color.alpha(p), r, g, b)
        }
        val posterizedBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        posterizedBitmap.setPixels(posterized, 0, w, 0, 0, w, h)

        // Soften block edges and boost saturation slightly for a painterly finish.
        val smoothed = boxBlur(posterizedBitmap, radius = 3)
        posterizedBitmap.recycle()

        val saturation = ColorMatrix().apply { setSaturation(1.2f) }
        val result = applyColorMatrix(smoothed, saturation)
        smoothed.recycle()
        return result
    }

    // ---------------------------------------------------------------------
    // Shared low-level helpers
    // ---------------------------------------------------------------------

    private fun unsharpMask(source: Bitmap, radius: Int, amount: Float): Bitmap {
        val blurred = boxBlur(source, radius)
        val w = source.width
        val h = source.height
        val srcPixels = IntArray(w * h).also { source.getPixels(it, 0, w, 0, 0, w, h) }
        val blurPixels = IntArray(w * h).also { blurred.getPixels(it, 0, w, 0, 0, w, h) }
        blurred.recycle()

        val out = IntArray(w * h)
        for (i in out.indices) {
            val sp = srcPixels[i]
            val bp = blurPixels[i]
            val r = (Color.red(sp) + amount * (Color.red(sp) - Color.red(bp))).roundToInt().coerceIn(0, 255)
            val g = (Color.green(sp) + amount * (Color.green(sp) - Color.green(bp))).roundToInt().coerceIn(0, 255)
            val b = (Color.blue(sp) + amount * (Color.blue(sp) - Color.blue(bp))).roundToInt().coerceIn(0, 255)
            out[i] = Color.argb(Color.alpha(sp), r, g, b)
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, w, 0, 0, w, h)
        return result
    }

    private fun applyVignette(source: Bitmap, strength: Float): Bitmap {
        val w = source.width
        val h = source.height
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawBitmap(source, 0f, 0f, null)

        val cx = w / 2f
        val cy = h / 2f
        val radius = sqrt((cx * cx + cy * cy).toDouble()).toFloat()
        val shader = android.graphics.RadialGradient(
            cx, cy, radius,
            intArrayOf(Color.TRANSPARENT, Color.argb((strength * 255).toInt(), 0, 0, 0)),
            floatArrayOf(0.6f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        return result
    }

    /**
     * A correct, edge-aware box blur implemented with per-row / per-column
     * prefix sums so it runs in O(w*h) regardless of [radius], with no
     * reliance on deprecated RenderScript APIs.
     */
    private fun boxBlur(source: Bitmap, radius: Int): Bitmap {
        if (radius <= 0) return source.copy(source.config ?: Bitmap.Config.ARGB_8888, false)

        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        val horizontal = boxBlurPass(pixels, w, h, radius, horizontal = true)
        val both = boxBlurPass(horizontal, w, h, radius, horizontal = false)

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(both, 0, w, 0, 0, w, h)
        return result
    }

    private fun boxBlurPass(pixels: IntArray, w: Int, h: Int, radius: Int, horizontal: Boolean): IntArray {
        val out = IntArray(w * h)
        val length = if (horizontal) w else h
        val lines = if (horizontal) h else w

        val prefA = IntArray(length + 1)
        val prefR = IntArray(length + 1)
        val prefG = IntArray(length + 1)
        val prefB = IntArray(length + 1)

        for (line in 0 until lines) {
            for (i in 0 until length) {
                val index = if (horizontal) line * w + i else i * w + line
                val p = pixels[index]
                prefA[i + 1] = prefA[i] + Color.alpha(p)
                prefR[i + 1] = prefR[i] + Color.red(p)
                prefG[i + 1] = prefG[i] + Color.green(p)
                prefB[i + 1] = prefB[i] + Color.blue(p)
            }
            for (i in 0 until length) {
                val left = max(0, i - radius)
                val right = min(length - 1, i + radius)
                val count = right - left + 1
                val a = (prefA[right + 1] - prefA[left]) / count
                val r = (prefR[right + 1] - prefR[left]) / count
                val g = (prefG[right + 1] - prefG[left]) / count
                val b = (prefB[right + 1] - prefB[left]) / count
                val index = if (horizontal) line * w + i else i * w + line
                out[index] = Color.argb(a, r, g, b)
            }
        }
        return out
    }
}