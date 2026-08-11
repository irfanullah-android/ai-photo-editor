package com.editor.photo.video.collagemaker.photoedit.feature.text

import com.editor.photo.video.collagemaker.photoedit.R

/**
 * The set of tools shown in the ONE horizontal tool RecyclerView at the bottom of
 * [TextEditorBottomSheet]. Each entry maps to exactly one panel in the tool-content
 * container (see [TextEditorBottomSheet.showPanelFor]) — panels are never stacked.
 */
enum class TextToolType(val label: String, val iconRes: Int) {
    STYLE("Style", R.drawable.ic_style),
    FONT("Font", R.drawable.ic_font),
    COLOR("Color", R.drawable.ic_circle_checked),
    STROKE("Stroke", R.drawable.ic_stroke),
    ALIGN("Align", R.drawable.ic_align_center),
    SIZE("Size", R.drawable.ic_size)
}

/** A built-in Android font family offered by the Font tool. No bundled font assets. */
data class TextFontOption(
    val key: String?,
    val displayName: String,
    val typefaceFamily: android.graphics.Typeface
)

object TextFonts {
    val OPTIONS = listOf(
        TextFontOption(null, "Default", android.graphics.Typeface.DEFAULT),
        TextFontOption("sans_serif", "Sans", android.graphics.Typeface.SANS_SERIF),
        TextFontOption("serif", "Serif", android.graphics.Typeface.SERIF),
        TextFontOption("monospace", "Mono", android.graphics.Typeface.MONOSPACE),
        TextFontOption("cursive", "Cursive", android.graphics.Typeface.create("cursive", android.graphics.Typeface.NORMAL)),
        TextFontOption("casual", "Casual", android.graphics.Typeface.create("casual", android.graphics.Typeface.NORMAL))
    )
}
