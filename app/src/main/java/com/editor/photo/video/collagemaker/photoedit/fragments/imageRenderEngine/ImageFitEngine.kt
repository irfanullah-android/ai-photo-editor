package com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine

import kotlin.math.max
import kotlin.math.min

/**
 * ImageFitEngine
 * ---------------
 * Pure, side-effect-free math for computing how an image should be scaled and
 * positioned inside a canvas of arbitrary size, mirroring the "fit, never crop
 * until the user zooms" behavior of InShot / PicsArt / Canva.
 *
 * This class has ZERO Android view dependencies on purpose:
 *  - fully unit-testable without instrumentation
 *  - can be called from a ViewModel (no Context/View needed)
 *  - the caller (a custom View or a Matrix-driven layer) applies the result
 *
 * KEY RULE: fitScale uses min(), never max(). max() is what centerCrop-style
 * logic uses and is exactly what causes automatic cropping on aspect change.
 */
object ImageFitEngine {

    const val DEFAULT_MAX_ZOOM_MULTIPLIER = 6f

    /**
     * The scale at which the entire image is visible inside the canvas,
     * touching the canvas on at least one axis, with empty space (letterboxing)
     * on the other axis if the aspect ratios don't match exactly.
     *
     * This is intentionally min(), not max(). max() would be centerCrop
     * (image overflows canvas, gets cropped) — never use it here.
     */
    fun computeFitScale(
        canvasWidth: Float,
        canvasHeight: Float,
        imageWidth: Float,
        imageHeight: Float
    ): Float {
        if (imageWidth <= 0f || imageHeight <= 0f) return 1f
        val scaleX = canvasWidth / imageWidth
        val scaleY = canvasHeight / imageHeight
        return min(scaleX, scaleY) // NEVER max() — max() = centerCrop = auto-crop bug
    }

    /**
     * Bounds for user-driven zoom. Minimum is always fitScale (you can never
     * zoom OUT past "whole image visible"). Maximum is a multiplier of fitScale
     * so zoom-in headroom scales proportionally with how "zoomed out" the fit
     * naturally is.
     */
    fun computeZoomBounds(
        fitScale: Float,
        maxZoomMultiplier: Float = DEFAULT_MAX_ZOOM_MULTIPLIER
    ): ZoomBounds = ZoomBounds(
        minScale = fitScale,
        maxScale = fitScale * maxZoomMultiplier
    )

    fun coerceScale(scale: Float, bounds: ZoomBounds): Float =
        scale.coerceIn(bounds.minScale, bounds.maxScale)

    /**
     * Centers the image inside the canvas at the given scale.
     * translation is the offset (in canvas px) of the image's top-left corner
     * from the canvas's top-left corner, assuming the image is drawn at
     * (imageWidth * scale) x (imageHeight * scale).
     */
    fun computeCenterTranslation(
        canvasWidth: Float,
        canvasHeight: Float,
        imageWidth: Float,
        imageHeight: Float,
        scale: Float
    ): Translation {
        val scaledWidth = imageWidth * scale
        val scaledHeight = imageHeight * scale
        return Translation(
            x = (canvasWidth - scaledWidth) / 2f,
            y = (canvasHeight - scaledHeight) / 2f
        )
    }

    /**
     * Full recompute for an aspect-ratio change: given the new canvas size and
     * the image's intrinsic size, produce the new fitScale + centered
     * translation + zoom bounds in one call. This is what you invoke whenever
     * the canvas aspect ratio changes.
     *
     * `preserveUserZoomRatio`: if the user had manually zoomed in past fitScale
     * before the ratio change (e.g. at 2x their old fitScale), pass their old
     * (scale / oldFitScale) ratio here to preserve *relative* zoom level
     * instead of snapping back to 1x fit. Pass null to always reset to fit
     * (matches "Minimum zoom must always equal fitScale" reset-on-ratio-change
     * behavior, which is what InShot does).
     */
    fun recomputeForCanvasChange(
        newCanvasWidth: Float,
        newCanvasHeight: Float,
        imageWidth: Float,
        imageHeight: Float,
        maxZoomMultiplier: Float = DEFAULT_MAX_ZOOM_MULTIPLIER,
        preserveUserZoomRatio: Float? = null
    ): FitResult {
        val fitScale = computeFitScale(newCanvasWidth, newCanvasHeight, imageWidth, imageHeight)
        val bounds = computeZoomBounds(fitScale, maxZoomMultiplier)
        val targetScale = if (preserveUserZoomRatio != null) {
            coerceScale(fitScale * preserveUserZoomRatio, bounds)
        } else {
            fitScale
        }
        val translation = computeCenterTranslation(
            newCanvasWidth, newCanvasHeight, imageWidth, imageHeight, targetScale
        )
        return FitResult(
            fitScale = fitScale,
            scale = targetScale,
            bounds = bounds,
            translation = translation
        )
    }

    data class ZoomBounds(val minScale: Float, val maxScale: Float)

    data class Translation(val x: Float, val y: Float)

    data class FitResult(
        val fitScale: Float,
        val scale: Float,
        val bounds: ZoomBounds,
        val translation: Translation
    )
}