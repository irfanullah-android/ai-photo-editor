package com.editor.photo.video.collagemaker.photoedit.feature.adjust

import android.graphics.Bitmap
import android.graphics.Color
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.AdjustmentType
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

object AutoEnhanceCalculator {

    private const val ANALYSIS_MAX_DIMENSION = 150

    private const val MAX_BRIGHTNESS = 22
    private const val MAX_CONTRAST = 18
    private const val MAX_SHADOW = 20
    private const val MAX_SATURATION = 16
    private const val MAX_WARMTH = 12
    private const val MAX_TINT = 10
    private const val MAX_SHARPEN = 14

    private const val MIN_CONTRAST_POLISH = 8
    private const val MIN_SATURATION_POLISH = 6
    private const val MIN_SHARPEN_POLISH = 6

    private const val CONTRAST_SKIP_POLISH_ABOVE = 0.90f
    private const val SATURATION_SKIP_POLISH_ABOVE = 0.70f
    private const val SHARPEN_SKIP_POLISH_ABOVE = 0.09f

    private const val DARK_THRESHOLD = 70f / 255f
    private const val TARGET_LUMINANCE = 0.5f
    private const val TARGET_SATURATION = 0.42f
    private const val IDEAL_DYNAMIC_RANGE = 0.75f
    private const val SHADOW_TOLERANCE = 0.12f

    private const val SOFTNESS_EDGE_THRESHOLD = 0.045f

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
        val avgLuminance: Float,
        val shadowPercent: Float,
        val dynamicRange: Float,
        val avgSaturation: Float,
        val warmthBias: Float,
        val tintBias: Float,
        val avgEdgeGradient: Float
    )

    private fun analyze(bitmap: Bitmap): ImageStats {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        var sumLuma = 0f
        var sumSat = 0f
        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var darkCount = 0

        val bucketCount = 64
        val histogram = IntArray(bucketCount)
        val hsv = FloatArray(3)

        val luma = FloatArray(w * h)

        val total = pixels.size
        for (i in 0 until total) {
            val pixel = pixels[i]
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)

            val l = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
            luma[i] = l
            sumLuma += l
            sumR += r
            sumG += g
            sumB += b

            if (l < DARK_THRESHOLD) darkCount++

            val bucket = (l * (bucketCount - 1)).roundToInt().coerceIn(0, bucketCount - 1)
            histogram[bucket]++

            Color.RGBToHSV(r, g, b, hsv)
            sumSat += hsv[1]
        }

        val avgLuminance = sumLuma / total
        val avgSaturation = sumSat / total
        val shadowPercent = darkCount / total.toFloat()

        val p5 = percentileFromHistogram(histogram, total, 0.05f)
        val p95 = percentileFromHistogram(histogram, total, 0.95f)
        val dynamicRange = (p95 - p5).coerceIn(0f, 1f)

        val avgR = sumR / total.toFloat()
        val avgG = sumG / total.toFloat()
        val avgB = sumB / total.toFloat()
        val warmthBias = ((avgR - avgB) / 255f).coerceIn(-1f, 1f)
        val tintBias = (((avgR + avgB) / 2f - avgG) / 255f).coerceIn(-1f, 1f)

        val avgEdgeGradient = averageEdgeGradient(luma, w, h)

        return ImageStats(
            avgLuminance = avgLuminance,
            shadowPercent = shadowPercent,
            dynamicRange = dynamicRange,
            avgSaturation = avgSaturation,
            warmthBias = warmthBias,
            tintBias = tintBias,
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

        val lumaDeficit = ((TARGET_LUMINANCE - s.avgLuminance) / TARGET_LUMINANCE).coerceIn(0f, 1f)
        val brightness = (smoothstep(lumaDeficit) * MAX_BRIGHTNESS).roundToInt()
            .coerceIn(0, MAX_BRIGHTNESS)

        val rangeDeficit = ((IDEAL_DYNAMIC_RANGE - s.dynamicRange) / IDEAL_DYNAMIC_RANGE).coerceIn(0f, 1f)
        val contrastCorrection = smoothstep(rangeDeficit) * MAX_CONTRAST
        val contrastPolish = if (s.dynamicRange < CONTRAST_SKIP_POLISH_ABOVE) MIN_CONTRAST_POLISH.toFloat() else 0f
        val contrast = max(contrastCorrection, contrastPolish).roundToInt()
            .coerceIn(0, MAX_CONTRAST)

        val shadowExcess = ((s.shadowPercent - SHADOW_TOLERANCE) / (1f - SHADOW_TOLERANCE)).coerceIn(0f, 1f)
        val shadow = (smoothstep(shadowExcess) * MAX_SHADOW).roundToInt()
            .coerceIn(0, MAX_SHADOW)

        val highlight = 0

        val satDeficit = ((TARGET_SATURATION - s.avgSaturation) / TARGET_SATURATION).coerceIn(0f, 1f)
        val saturationCorrection = smoothstep(satDeficit) * MAX_SATURATION
        val saturationPolish = if (s.avgSaturation < SATURATION_SKIP_POLISH_ABOVE) MIN_SATURATION_POLISH.toFloat() else 0f
        val saturation = max(saturationCorrection, saturationPolish).roundToInt()
            .coerceIn(0, MAX_SATURATION)

        val warmth = if (s.warmthBias < -0.04f) {
            (smoothstep(-s.warmthBias) * MAX_WARMTH).roundToInt().coerceIn(0, MAX_WARMTH)
        } else 0

        val tint = if (s.tintBias > 0.04f) {
            (smoothstep(s.tintBias) * MAX_TINT).roundToInt().coerceIn(0, MAX_TINT)
        } else 0

        val softnessDeficit = ((SOFTNESS_EDGE_THRESHOLD - s.avgEdgeGradient) / SOFTNESS_EDGE_THRESHOLD)
            .coerceIn(0f, 1f)
        val sharpen = (smoothstep(softnessDeficit) * MAX_SHARPEN).roundToInt()
            .coerceIn(0, MAX_SHARPEN)

        result[AdjustmentType.BRIGHTNESS] = brightness
        result[AdjustmentType.CONTRAST] = contrast
        result[AdjustmentType.SHADOW] = shadow
        result[AdjustmentType.HIGHLIGHT] = highlight
        result[AdjustmentType.SATURATION] = saturation
        result[AdjustmentType.WARMTH] = warmth
        result[AdjustmentType.TINT] = tint
        result[AdjustmentType.SHARPEN] = sharpen

        return result
    }
}