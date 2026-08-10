package com.editor.photo.video.collagemaker.photoedit.models.bottomsheets

import com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine.ColorMatrixEngine
import com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine.FilterSpec
import com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine.VignetteSpec

enum class EditorFilter(
    val id: String,
    val displayName: String
) {

    NORMAL("normal", "Normal"),
    FEATURE("feature", "Feature"),
    LUNAR("lunar", "Lunar"),
    SKIN("skin", "Skin"),
    MOSS("moss", "Moss"),
    BARN("barn", "Barn"),
    MOODY_CREAM("moody_cream", "Moody Cream"),
    AMBER("amber", "Amber"),
    DUO("duo", "Duo"),
    FRESH("fresh", "Fresh"),
    FILM("film", "Film"),
    PLUM("plum", "Plum"),
    SPOT("spot", "Spot"),
    COSY("cosy", "Cosy"),
    NEON("neon", "Neon"),
    CINEMATIC("cinematic", "Cinematic");

    /**
     * Builds the ColorMatrix (+ optional vignette) for this filter at the
     * given [intensity] (0..100). Pure android.graphics.ColorMatrix —
     * no external libraries, no GPU shaders.
     */
    fun buildFilterSpec(intensity: Int): FilterSpec {
        val i = (intensity / 100f).coerceIn(0f, 1f)
        val e = ColorMatrixEngine

        return when (this) {
            NORMAL -> FilterSpec(e.identity())

            FEATURE -> FilterSpec(
                e.combine(
                    e.whiteBalance(5000f + 300f * i, 0f),
                    e.saturation(1f + 0.2f * i),
                    e.contrast(1f + 0.15f * i),
                    e.brightness(-0.03f * i * 255f)
                )
            )

            LUNAR -> FilterSpec(
                e.combine(
                    e.whiteBalance(5000f - 600f * i, 0f),
                    e.saturation(1f - 0.4f * i),
                    e.contrast(1f + 0.15f * i),
                    e.brightness(-0.05f * i * 255f)
                )
            )

            SKIN -> FilterSpec(
                e.combine(
                    e.whiteBalance(5000f + 400f * i, 0.08f * i),
                    e.saturation(1f + 0.08f * i),
                    e.contrast(1f + 0.05f * i),
                    e.brightness(0.02f * i * 255f),
                    e.clarityBoost(-0.15f * i) // gentle softening feel
                )
            )

            MOSS -> FilterSpec(
                e.combine(
                    e.hueRotate(15f * i),
                    e.saturation(1f - 0.2f * i),
                    e.contrast(1f + 0.05f * i),
                    e.brightness(-0.04f * i * 255f)
                )
            )

            BARN -> FilterSpec(
                matrix = e.combine(
                    e.whiteBalance(5000f + 800f * i, 0.06f * i),
                    e.contrast(1f + 0.15f * i),
                    e.brightness(-0.03f * i * 255f)
                ),
                vignette = VignetteSpec(
                    startRadius = 1f - 0.45f * i,
                    endRadius = 1f - 0.2f * i,
                    strength = 0.35f * i
                )
            )

            MOODY_CREAM -> FilterSpec(
                e.combine(
                    e.contrast(1f - 0.1f * i),
                    e.saturation(1f - 0.25f * i),
                    e.brightness(0.02f * i * 255f),
                    e.whiteBalance(5000f + 250f * i, 0.03f * i)
                )
            )

            AMBER -> FilterSpec(
                e.combine(
                    e.whiteBalance(5000f + 1200f * i, 0.12f * i),
                    e.saturation(1f + 0.2f * i),
                    e.contrast(1f + 0.1f * i),
                    e.brightness(-0.02f * i * 255f),
                    e.hueRotate(5f * i)
                )
            )

            DUO -> FilterSpec(
                e.combine(
                    e.sepia(i),
                    e.contrast(1f + 0.1f * i),
                    e.brightness(-0.04f * i * 255f)
                )
            )

            FRESH -> FilterSpec(
                e.combine(
                    e.brightness(0.02f * i * 255f),
                    e.saturation(1f + 0.12f * i),
                    e.contrast(1f + 0.1f * i)
                )
            )

            FILM -> FilterSpec(
                matrix = e.combine(
                    e.contrast(1f + 0.12f * i),
                    e.saturation(1f - 0.15f * i),
                    e.brightness(-0.03f * i * 255f),
                    e.clarityBoost(0.2f * i)
                ),
                vignette = VignetteSpec(
                    startRadius = 1f - 0.5f * i,
                    endRadius = 1f - 0.2f * i,
                    strength = 0.4f * i
                )
            )

            PLUM -> FilterSpec(
                e.combine(
                    e.hueRotate(-8f * i),
                    e.saturation(1f + 0.15f * i),
                    e.brightness(-0.04f * i * 255f),
                    e.whiteBalance(5000f + 200f * i, -0.04f * i)
                )
            )

            SPOT -> FilterSpec(
                matrix = e.combine(
                    e.contrast(1f + 0.25f * i),
                    e.brightness(-0.06f * i * 255f)
                ),
                vignette = VignetteSpec(
                    startRadius = 1f - 0.6f * i,
                    endRadius = 1f - 0.25f * i,
                    strength = 0.5f * i
                )
            )

            COSY -> FilterSpec(
                matrix = e.combine(
                    e.whiteBalance(5000f + 700f * i, 0.08f * i),
                    e.brightness(-0.04f * i * 255f),
                    e.saturation(1f - 0.12f * i)
                ),
                vignette = VignetteSpec(
                    startRadius = 1f - 0.4f * i,
                    endRadius = 1f - 0.15f * i,
                    strength = 0.3f * i
                )
            )

            NEON -> FilterSpec(
                e.combine(
                    e.saturation(1f + 0.3f * i),
                    e.contrast(1f + 0.15f * i),
                    e.brightness(-0.02f * i * 255f),
                    e.clarityBoost(0.3f * i)
                )
            )

            CINEMATIC -> FilterSpec(
                matrix = e.combine(
                    e.contrast(1f + 0.18f * i),
                    e.saturation(1f - 0.1f * i),
                    e.brightness(-0.04f * i * 255f),
                    e.whiteBalance(5000f - 400f * i, 0f)
                ),
                vignette = VignetteSpec(
                    startRadius = 1f - 0.5f * i,
                    endRadius = 1f - 0.2f * i,
                    strength = 0.35f * i
                )
            )
        }
    }

    companion object {
        fun fromId(id: String?): EditorFilter {
            return values().find { it.id == id } ?: NORMAL
        }
    }
}