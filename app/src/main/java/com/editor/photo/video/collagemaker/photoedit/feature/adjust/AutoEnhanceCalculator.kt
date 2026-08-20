package com.editor.photo.video.collagemaker.photoedit.feature.adjust

import android.graphics.Bitmap
import android.graphics.Color
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.AdjustmentType
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

object AutoEnhanceCalculator {

    private const val ANALYSIS_MAX_DIMENSION = 200

    // Enhanced professional limits for stronger pop and clarity
    private const val MAX_BRIGHTNESS = 40
    private const val MAX_CONTRAST = 32
    private const val MAX_SHADOW = 35
    private const val MAX_HIGHLIGHT = 28
    private const val MAX_SATURATION = 32
    private const val MAX_WARMTH = 22
    private const val MAX_TINT = 22
    private const val MAX_SHARPEN = 26

    private const val TARGET_LUMINANCE = 138.0
    private const val IDEAL_DYNAMIC_RANGE = 0.85f
    private const val TARGET_SATURATION = 0.42f
    private const val SOFTNESS_EDGE_THRESHOLD = 0.055f

    fun calculate(source: Bitmap): Map<AdjustmentType, Int> {
        if (source.isRecycled || source.width == 0 || source.height == 0) {
            return emptyMap()
        }

        val sample = downscale(source)
        try {
            val stats = analyze(sample)
            return buildValues(stats)
        } finally {
            if (sample != source && !sample.isRecycled) {
                sample.recycle()
            }
        }
    }

    private fun downscale(source: Bitmap): Bitmap {
        val longSide = max(source.width, source.height)
        if (longSide <= ANALYSIS_MAX_DIMENSION) return source
        val scale = ANALYSIS_MAX_DIMENSION / longSide.toFloat()
        val w = (source.width * scale).toInt().coerceAtLeast(1)
        val h = (source.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, w, h, true)
    }

    private data class ImageStats(
        val r: Int,
        val g: Int,
        val b: Int,
        val avgLuminance: Double,
        val avgSaturation: Double,
        val dynamicRange: Float,
        val avgEdgeGradient: Float
    )

    private fun analyze(bitmap: Bitmap): ImageStats {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        var totalR = 0L
        var totalG = 0L
        var totalB = 0L

        val bucketCount = 64
        val histogram = IntArray(bucketCount)
        val luma = FloatArray(w * h)

        val total = pixels.size
        for (i in 0 until total) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)

            totalR += r
            totalG += g
            totalB += b

            val l = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
            luma[i] = l

            val bucket = (l * (bucketCount - 1)).roundToInt().coerceIn(0, bucketCount - 1)
            histogram[bucket]++
        }

        val r = (totalR / total).toInt()
        val g = (totalG / total).toInt()
        val b = (totalB / total).toInt()

        val avgLum = r * 0.299 + g * 0.587 + b * 0.114

        val mx = maxOf(r, g, b)
        val mn = minOf(r, g, b)
        val avgSat = if (mx == 0) 0.0 else (mx - mn).toDouble() / mx

        val p5 = percentileFromHistogram(histogram, total, 0.04f)
        val p95 = percentileFromHistogram(histogram, total, 0.96f)
        val dynamicRange = (p95 - p5).coerceIn(0f, 1f)

        val avgEdgeGradient = averageEdgeGradient(luma, w, h)

        return ImageStats(
            r = r,
            g = g,
            b = b,
            avgLuminance = avgLum,
            avgSaturation = avgSat,
            dynamicRange = dynamicRange,
            avgEdgeGradient = avgEdgeGradient
        )
    }

    private fun averageEdgeGradient(luma: FloatArray, w: Int, h: Int): Float {
        if (w < 3 || h < 3) return 0f

        var sumGradient = 0f
        var count = 0
        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val idx = row + x
                val gx = luma[idx + 1] - luma[idx - 1]
                val gy = luma[idx + w] - luma[idx - w]
                sumGradient += sqrt(gx * gx + gy * gy)
                count++
            }
        }
        return if (count > 0) sumGradient / count else 0f
    }

    private fun percentileFromHistogram(histogram: IntArray, total: Int, percentile: Float): Float {
        val target = (total * percentile).roundToInt()
        var cumulative = 0
        for (i in histogram.indices) {
            cumulative += histogram[i]
            if (cumulative >= target) {
                return i / (histogram.size - 1).toFloat()
            }
        }
        return 1f
    }

    private fun smoothstep(x: Float): Float {
        val t = x.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun buildValues(s: ImageStats): Map<AdjustmentType, Int> {
        val result = mutableMapOf<AdjustmentType, Int>()

        // 1. Stronger Professional Brightness
        val brightness = ((TARGET_LUMINANCE - s.avgLuminance) * 0.5)
            .coerceIn(-MAX_BRIGHTNESS.toDouble(), MAX_BRIGHTNESS.toDouble())
            .roundToInt()

        // 2. High Impact Contrast
        val rangeDeficit = ((IDEAL_DYNAMIC_RANGE - s.dynamicRange) / IDEAL_DYNAMIC_RANGE).coerceIn(0f, 1f)
        val contrast = (smoothstep(rangeDeficit) * MAX_CONTRAST.toFloat())
            .roundToInt()
            .coerceIn(8, MAX_CONTRAST)

        // 3. Deep Shadows & Highlights Opening
        val shadows = if (s.avgLuminance < 120.0) {
            ((120.0 - s.avgLuminance) * 0.55).roundToInt().coerceIn(12, MAX_SHADOW)
        } else {
            6
        }

        val highlights = if (s.avgLuminance > 140.0) {
            ((s.avgLuminance - 140.0) * 0.45).roundToInt().coerceIn(10, MAX_HIGHLIGHT)
        } else {
            6
        }

        // 4. Vibrant Color Saturation
        val satDeficit = ((TARGET_SATURATION - s.avgSaturation.toFloat()) / TARGET_SATURATION).coerceIn(0f, 1f)
        val saturation = (smoothstep(satDeficit) * MAX_SATURATION.toFloat())
            .roundToInt()
            .coerceIn(10, MAX_SATURATION)

        // 5. White Balance Warmth Correction
        val warmCast = (s.r - s.b).toDouble()
        val warmth = (-warmCast * 0.5)
            .coerceIn(-MAX_WARMTH.toDouble(), MAX_WARMTH.toDouble())
            .roundToInt()

        // 6. Tint Balance Correction
        val greenCast = s.g - ((s.r + s.b) / 2.0)
        val tint = (greenCast * 0.5)
            .coerceIn(-MAX_TINT.toDouble(), MAX_TINT.toDouble())
            .roundToInt()

        // 7. Crystal Clear Sharpening
        val softnessDeficit = ((SOFTNESS_EDGE_THRESHOLD - s.avgEdgeGradient) / SOFTNESS_EDGE_THRESHOLD)
            .coerceIn(0f, 1f)
        val sharpen = (smoothstep(softnessDeficit) * MAX_SHARPEN.toFloat())
            .roundToInt()
            .coerceIn(10, MAX_SHARPEN)

        result[AdjustmentType.BRIGHTNESS] = brightness
        result[AdjustmentType.CONTRAST] = contrast
        result[AdjustmentType.SHADOW] = shadows
        result[AdjustmentType.HIGHLIGHT] = highlights
        result[AdjustmentType.SATURATION] = saturation
        result[AdjustmentType.WARMTH] = warmth
        result[AdjustmentType.TINT] = tint
        result[AdjustmentType.SHARPEN] = sharpen

        return result
    }
}