package com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Pure android.graphics.ColorMatrix based filter engine.
 *
 * No third-party image libraries (GPUImage, OpenGL, RenderScript, etc.)
 * are used anywhere here — only Android's built-in Canvas / Paint /
 * ColorMatrix / RadialGradient APIs, which ship with the platform.
 */

/** Radial darkening applied on top of the color-matrix result (vignette can't be done with a ColorMatrix alone). */
data class VignetteSpec(
    val startRadius: Float,   // 0f..1f, relative to half-diagonal — where darkening begins
    val endRadius: Float,     // 0f..1f — where darkening reaches full strength
    val strength: Float       // 0f..1f — max opacity of the vignette
)

data class FilterSpec(
    val matrix: ColorMatrix,
    val vignette: VignetteSpec? = null
)

object ColorMatrixEngine {

    // ---------------------------------------------------------------
    // Building blocks
    // ---------------------------------------------------------------

    fun identity(): ColorMatrix = ColorMatrix()

    fun saturation(sat: Float): ColorMatrix {
        val m = ColorMatrix()
        m.setSaturation(sat.coerceAtLeast(0f))
        return m
    }

    fun contrast(value: Float): ColorMatrix {
        val v = value.coerceAtLeast(0f)
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

    /** [value] in roughly -255f..255f */
    fun brightness(value: Float): ColorMatrix {
        return ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, value,
                0f, 1f, 0f, 0f, value,
                0f, 0f, 1f, 0f, value,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    /**
     * [temperature]: ~5000 = neutral. Higher = warmer (more red/yellow),
     * lower = cooler (more blue). [tint]: negative = green, positive = magenta.
     */
    fun whiteBalance(temperature: Float, tint: Float): ColorMatrix {
        val temp = (temperature - 5000f) / 5000f
        val rMul = (1f + temp * 0.35f).coerceIn(0.5f, 1.8f)
        val bMul = (1f - temp * 0.35f).coerceIn(0.5f, 1.8f)
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

    /** Rotates hue by [degrees] while preserving perceived luminance. */
    fun hueRotate(degrees: Float): ColorMatrix {
        if (degrees == 0f) return ColorMatrix()
        val cosVal = cos(Math.toRadians(degrees.toDouble())).toFloat()
        val sinVal = sin(Math.toRadians(degrees.toDouble())).toFloat()
        val lumR = 0.213f
        val lumG = 0.715f
        val lumB = 0.072f

        val mat = floatArrayOf(
            lumR + cosVal * (1 - lumR) + sinVal * (-lumR),
            lumG + cosVal * (-lumG) + sinVal * (-lumG),
            lumB + cosVal * (-lumB) + sinVal * (1 - lumB),
            0f, 0f,

            lumR + cosVal * (-lumR) + sinVal * (0.143f),
            lumG + cosVal * (1 - lumG) + sinVal * (0.140f),
            lumB + cosVal * (-lumB) + sinVal * (-0.283f),
            0f, 0f,

            lumR + cosVal * (-lumR) + sinVal * (-(1 - lumR)),
            lumG + cosVal * (-lumG) + sinVal * (lumG),
            lumB + cosVal * (1 - lumB) + sinVal * (lumB),
            0f, 0f,

            0f, 0f, 0f, 1f, 0f
        )
        return ColorMatrix(mat)
    }

    /** Classic sepia tone, blended in by [amount] 0f..1f. */
    fun sepia(amount: Float): ColorMatrix {
        val a = amount.coerceIn(0f, 1f)
        val fullSepia = floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        val identityArr = ColorMatrix().array
        val blended = FloatArray(20) { idx ->
            identityArr[idx] + (fullSepia[idx] - identityArr[idx]) * a
        }
        return ColorMatrix(blended)
    }

    /**
     * Approximates a "clarity / sharpen" feel using extra local contrast.
     * True sharpening needs a spatial convolution kernel (per-neighbor-pixel
     * math), which a per-pixel ColorMatrix cannot do — this is the closest
     * ColorMatrix-only substitute and reads well in practice.
     */
    fun clarityBoost(amount: Float): ColorMatrix {
        return contrast(1f + 0.12f * amount)
    }

    /** Combines matrices in order (first one applied first). */
    fun combine(vararg matrices: ColorMatrix): ColorMatrix {
        val result = ColorMatrix()
        for (m in matrices) {
            result.postConcat(m)
        }
        return result
    }

    // ---------------------------------------------------------------
    // Rendering
    // ---------------------------------------------------------------

    private fun isIdentity(matrix: ColorMatrix): Boolean {
        val arr = matrix.array
        return arr[0] == 1f && arr[1] == 0f && arr[2] == 0f && arr[3] == 0f && arr[4] == 0f &&
               arr[5] == 0f && arr[6] == 1f && arr[7] == 0f && arr[8] == 0f && arr[9] == 0f &&
               arr[10] == 0f && arr[11] == 0f && arr[12] == 1f && arr[13] == 0f && arr[14] == 0f &&
               arr[15] == 0f && arr[16] == 0f && arr[17] == 0f && arr[18] == 1f && arr[19] == 0f
    }

    /** Applies a [FilterSpec] to [source] and returns a NEW bitmap (source untouched). */
    fun render(source: Bitmap, spec: FilterSpec): Bitmap {
        if (isIdentity(spec.matrix) && spec.vignette == null) {
            return source
        }
        val config = source.config ?: Bitmap.Config.ARGB_8888
        val result = Bitmap.createBitmap(source.width, source.height, config)
        val canvas = Canvas(result)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(spec.matrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        spec.vignette?.let { v -> drawVignette(canvas, result.width, result.height, v) }

        return result
    }

    /** Cheap color filter for instant live preview on an ImageView (no bitmap allocation, no vignette). */
    fun asColorFilter(spec: FilterSpec): ColorMatrixColorFilter = ColorMatrixColorFilter(spec.matrix)

    private fun drawVignette(canvas: Canvas, width: Int, height: Int, v: VignetteSpec) {
        if (v.strength <= 0f) return
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = hypot(cx, cy)

        val startPos = v.startRadius.coerceIn(0f, 1f)
        val endPos = v.endRadius.coerceIn(startPos, 1f)

        val gradient = RadialGradient(
            cx, cy, maxRadius,
            intArrayOf(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                Color.argb((v.strength * 255).toInt(), 0, 0, 0)
            ),
            floatArrayOf(0f, startPos, endPos),
            Shader.TileMode.CLAMP
        )

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = gradient }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    }
}