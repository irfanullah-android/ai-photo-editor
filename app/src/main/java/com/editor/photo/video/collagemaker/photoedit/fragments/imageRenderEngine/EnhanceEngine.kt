package com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorEnhance
import kotlin.math.pow


object EnhanceEngine {

    /** One Float per EditorEnhance tool, all centered at 0f = no change, range roughly -1f..1f. */
    data class EnhanceValues(
        val exposure: Float = 0f,
        val contrast: Float = 0f,
        val highlights: Float = 0f,
        val shadows: Float = 0f,
        val whites: Float = 0f,
        val blacks: Float = 0f,
        val temperature: Float = 0f,
        val tint: Float = 0f,
        val vibrance: Float = 0f,
        val saturation: Float = 0f,
        val clarity: Float = 0f,
        val structure: Float = 0f,
        val dehaze: Float = 0f
    ) {
        fun get(tool: EditorEnhance): Float = when (tool) {
            EditorEnhance.EXPOSURE -> exposure
            EditorEnhance.CONTRAST -> contrast
            EditorEnhance.HIGHLIGHTS -> highlights
            EditorEnhance.SHADOWS -> shadows
            EditorEnhance.WHITES -> whites
            EditorEnhance.BLACKS -> blacks
            EditorEnhance.TEMPERATURE -> temperature
            EditorEnhance.TINT -> tint
            EditorEnhance.VIBRANCE -> vibrance
            EditorEnhance.SATURATION -> saturation
            EditorEnhance.CLARITY -> clarity
            EditorEnhance.STRUCTURE -> structure
            EditorEnhance.DEHAZE -> dehaze
        }

        /** Returns a copy with just [tool] updated — keeps every other slider untouched. */
        fun with(tool: EditorEnhance, value: Float): EnhanceValues = when (tool) {
            EditorEnhance.EXPOSURE -> copy(exposure = value)
            EditorEnhance.CONTRAST -> copy(contrast = value)
            EditorEnhance.HIGHLIGHTS -> copy(highlights = value)
            EditorEnhance.SHADOWS -> copy(shadows = value)
            EditorEnhance.WHITES -> copy(whites = value)
            EditorEnhance.BLACKS -> copy(blacks = value)
            EditorEnhance.TEMPERATURE -> copy(temperature = value)
            EditorEnhance.TINT -> copy(tint = value)
            EditorEnhance.VIBRANCE -> copy(vibrance = value)
            EditorEnhance.SATURATION -> copy(saturation = value)
            EditorEnhance.CLARITY -> copy(clarity = value)
            EditorEnhance.STRUCTURE -> copy(structure = value)
            EditorEnhance.DEHAZE -> copy(dehaze = value)
        }

        /** True if every slider is at its neutral 0f — lets callers skip rendering entirely. */
        fun isIdentity(): Boolean = this == EnhanceValues()
    }

    // ---------------------------------------------------------------
    // Pass 1 — ColorMatrix (Exposure / Contrast / Whites / Blacks /
    // Temperature / Tint / Saturation / Dehaze's tonal part)
    // ---------------------------------------------------------------

    /**
     * Exposure math: multiplies linear channel values by 2^stops.
     * value range -1f..1f maps to 0.5x..2x brightness (±1 stop) — flattened
     * from the old ±2-stop / 0.25x..4x range so a full slider drag feels
     * the same weight as Contrast/Whites/Blacks instead of blowing out or
     * crushing the image within the first small movement.
     */
    private fun exposureMatrix(value: Float): ColorMatrix {
        val factor = 2f.pow(value.coerceIn(-1f, 1f))
        return ColorMatrix(
            floatArrayOf(
                factor, 0f, 0f, 0f, 0f,
                0f, factor, 0f, 0f, 0f,
                0f, 0f, factor, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    private fun contrastMatrix(value: Float): ColorMatrix {
        val v = (1f + value).coerceAtLeast(0f)
        val translate = (1f - v) / 2f * 255f
        return ColorMatrix(
            floatArrayOf(
                v, 0f, 0f, 0f, translate,
                0f, v, 0f, 0f, translate,
                0f, 0f, v, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    /**
     * Levels-style white/black point remap — Whites moves where 255 clips from,
     * Blacks moves where 0 clips from, independent of each other.
     */
    private fun whitesBlacksMatrix(whites: Float, blacks: Float): ColorMatrix {
        val whitePoint = 255f - whites * 60f
        val blackPoint = 0f + blacks * 60f
        val range = (whitePoint - blackPoint).coerceAtLeast(1f)
        val scale = 255f / range
        val offset = -blackPoint * scale
        return ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, offset,
                0f, scale, 0f, 0f, offset,
                0f, 0f, scale, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    /** Real white balance — warms/cools via R/B multipliers, tint via G ↔ magenta. */
    private fun temperatureTintMatrix(temperature: Float, tint: Float): ColorMatrix {
        val rMul = (1f + temperature * 0.3f).coerceIn(0.5f, 1.8f)
        val bMul = (1f - temperature * 0.3f).coerceIn(0.5f, 1.8f)
        val gMul = (1f + tint * 0.25f).coerceIn(0.5f, 1.8f)
        return ColorMatrix(
            floatArrayOf(
                rMul, 0f, 0f, 0f, 0f,
                0f, gMul, 0f, 0f, 0f,
                0f, 0f, bMul, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    private fun saturationMatrix(value: Float): ColorMatrix {
        val m = ColorMatrix()
        m.setSaturation((1f + value).coerceAtLeast(0f))
        return m
    }

    /**
     * Dehaze's tonal component: cuts a light "haze veil" via a subtractive
     * offset, then punches contrast + saturation back up. This is a
     * lightweight photographic approximation (subtract-veil + re-punch),
     * NOT a full dark-channel-prior implementation — that needs a
     * patch-wise min filter per pixel, which is too costly for a live
     * preview. Negative values do the reverse (adds haze), which is a
     * nice bonus for creative use.
     */
    private fun dehazeMatrix(amount: Float): ColorMatrix {
        val a = amount.coerceIn(-1f, 1f)
        val veil = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, -18f * a,
                0f, 1f, 0f, 0f, -14f * a,
                0f, 0f, 1f, 0f, -10f * a,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val result = ColorMatrix()
        result.postConcat(veil)
        result.postConcat(contrastMatrix(0.35f * a))
        result.postConcat(saturationMatrix(0.25f * a))
        return result
    }

    private fun buildTonalMatrix(v: EnhanceValues): ColorMatrix {
        val result = ColorMatrix()
        result.postConcat(exposureMatrix(v.exposure))
        result.postConcat(contrastMatrix(v.contrast))
        result.postConcat(whitesBlacksMatrix(v.whites, v.blacks))
        result.postConcat(temperatureTintMatrix(v.temperature, v.tint))
        result.postConcat(saturationMatrix(v.saturation))
        result.postConcat(dehazeMatrix(v.dehaze))
        return result
    }

    /**
     * Instant, zero-allocation live preview for the matrix-representable
     * subset only (same trick as FilterBottomSheet's liveSpec ColorFilter).
     * Highlights/Shadows/Vibrance/Clarity/Structure are intentionally
     * excluded — they need the pixel/blur passes below and only show up
     * once the debounced full render lands, same as Filter's vignette.
     */
    fun quickPreviewFilter(values: EnhanceValues): ColorMatrixColorFilter =
        ColorMatrixColorFilter(buildTonalMatrix(values))

    // ---------------------------------------------------------------
    // Pass 2 — fused pixel pass (Highlights / Shadows / Vibrance)
    // ---------------------------------------------------------------

    private fun applySelectivePass(bitmap: Bitmap, v: EnhanceValues): Bitmap {
        if (v.highlights == 0f && v.shadows == 0f && v.vibrance == 0f) return bitmap

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val hsv = FloatArray(3)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            var r = Color.red(pixel)
            var g = Color.green(pixel)
            var b = Color.blue(pixel)
            val a = Color.alpha(pixel)

            if (v.highlights != 0f || v.shadows != 0f) {
                val luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                val highlightWeight = luma * luma
                val shadowWeight = (1f - luma) * (1f - luma)
                val delta = v.highlights * 60f * highlightWeight + v.shadows * 60f * shadowWeight
                r = (r + delta).coerceIn(0f, 255f).toInt()
                g = (g + delta).coerceIn(0f, 255f).toInt()
                b = (b + delta).coerceIn(0f, 255f).toInt()
            }

            if (v.vibrance != 0f) {
                Color.RGBToHSV(r, g, b, hsv)
                val hue = hsv[0]
                val sat = hsv[1]
                // Rough skin-tone hue/sat band — dampens vibrance there so
                // faces don't turn orange the way plain saturation does.
                val isSkinTone = hue in 5f..45f && sat in 0.2f..0.75f
                val protectFactor = if (isSkinTone) 0.35f else 1f
                val boost = v.vibrance * (1f - sat) * protectFactor
                hsv[1] = (sat + boost).coerceIn(0f, 1f)
                val newColor = Color.HSVToColor(hsv)
                r = Color.red(newColor)
                g = Color.green(newColor)
                b = Color.blue(newColor)
            }

            pixels[i] = Color.argb(a, r, g, b)
        }

        val result = Bitmap.createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }

    // ---------------------------------------------------------------
    // Pass 3 — Clarity / Structure (cheap blur + unsharp mask)
    // ---------------------------------------------------------------

    /** Cheap blur: downscale then upscale with bilinear filtering — no per-pixel convolution needed. */
    private fun cheapBlur(bitmap: Bitmap, downsampleFactor: Int): Bitmap {
        val smallW = (bitmap.width / downsampleFactor).coerceAtLeast(1)
        val smallH = (bitmap.height / downsampleFactor).coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(bitmap, smallW, smallH, true)
        val blurred = Bitmap.createScaledBitmap(small, bitmap.width, bitmap.height, true)
        if (small != blurred) small.recycle()
        return blurred
    }

    /** Unsharp-mask local contrast: result = original + (original - blurred) * amount. */
    private fun unsharpMask(bitmap: Bitmap, downsampleFactor: Int, amount: Float): Bitmap {
        if (amount == 0f) return bitmap
        val blurred = cheapBlur(bitmap, downsampleFactor)

        val width = bitmap.width
        val height = bitmap.height
        val src = IntArray(width * height)
        val blur = IntArray(width * height)
        bitmap.getPixels(src, 0, width, 0, 0, width, height)
        blurred.getPixels(blur, 0, width, 0, 0, width, height)
        blurred.recycle()

        for (i in src.indices) {
            val s = src[i]
            val bl = blur[i]
            val a = Color.alpha(s)
            val r = (Color.red(s) + (Color.red(s) - Color.red(bl)) * amount).coerceIn(0f, 255f).toInt()
            val g = (Color.green(s) + (Color.green(s) - Color.green(bl)) * amount).coerceIn(0f, 255f).toInt()
            val b = (Color.blue(s) + (Color.blue(s) - Color.blue(bl)) * amount).coerceIn(0f, 255f).toInt()
            src[i] = Color.argb(a, r, g, b)
        }

        val result = Bitmap.createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        result.setPixels(src, 0, width, 0, 0, width, height)
        return result
    }

    private fun applyLocalContrastPass(bitmap: Bitmap, v: EnhanceValues): Bitmap {
        if (v.clarity == 0f && v.structure == 0f) return bitmap
        var result = bitmap
        if (v.clarity != 0f) {
            // Larger blur radius → broad, "punchy midtone" clarity feel.
            val next = unsharpMask(result, downsampleFactor = 10, amount = v.clarity * 1.4f)
            if (next != result && result != bitmap) result.recycle()
            result = next
        }
        if (v.structure != 0f) {
            // Smaller blur radius → fine-detail "texture" feel.
            val next = unsharpMask(result, downsampleFactor = 4, amount = v.structure * 0.9f)
            if (next != result && result != bitmap) result.recycle()
            result = next
        }
        return result
    }

    // ---------------------------------------------------------------
    // Public entry point
    // ---------------------------------------------------------------

    /**
     * Renders the full [values] stack against [source] and returns a NEW
     * bitmap (source untouched, matching ColorMatrixEngine.render's contract).
     * Always call this against the ORIGINAL bitmap — never chain it against
     * a previously-rendered result, or adjustments will compound and degrade.
     */
    fun render(source: Bitmap, values: EnhanceValues): Bitmap {
        if (values.isIdentity()) {
            return source.copy(source.config ?: Bitmap.Config.ARGB_8888, false)
        }

        val config = source.config ?: Bitmap.Config.ARGB_8888
        val tonal = Bitmap.createBitmap(source.width, source.height, config)
        val canvas = Canvas(tonal)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(buildTonalMatrix(values))
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        val selective = applySelectivePass(tonal, values)
        if (selective != tonal) tonal.recycle()

        val finalBitmap = applyLocalContrastPass(selective, values)
        if (finalBitmap != selective) selective.recycle()

        return finalBitmap
    }
}