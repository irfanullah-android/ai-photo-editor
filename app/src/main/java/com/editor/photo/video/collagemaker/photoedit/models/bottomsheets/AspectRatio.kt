package com.editor.photo.video.collagemaker.photoedit.models.bottomsheets

import com.editor.photo.video.collagemaker.photoedit.R

data class AspectRatio(
    val id: String,
    val iconRes: Int,
    val label: String,
    val ratioX: Float?,
    val ratioY: Float?
) {
    companion object {

        val NO_FRAME = AspectRatio(
            id = "no_frame",
            iconRes = R.drawable.ic_ratio_free,
            label = "Free",
            ratioX = null,
            ratioY = null
        )

        val RATIO_1_1 = AspectRatio(
            "1x1",
            R.drawable.ic_ratio_1_1,
            "1:1",
            1f,
            1f
        )

        val RATIO_4_5 = AspectRatio(
            "4x5",
            R.drawable.ic_ratio_4_5,
            "4:5",
            4f,
            5f
        )

        val RATIO_9_16_YT = AspectRatio(
            "9x16_yt",
            R.drawable.ic_ratio_9_16,
            "9:16",
            9f,
            16f
        )

        val RATIO_16_9 = AspectRatio(
            "16x9",
            R.drawable.ic_ratio_16_9,
            "16:9",
            16f,
            9f
        )

        val RATIO_3_4 = AspectRatio(
            "3x4",
            R.drawable.ic_ratio_3_4,
            "3:4",
            3f,
            4f
        )

        val RATIO_9_16_V = AspectRatio(
            "9x16_v",
            R.drawable.ic_ratio_9_16,
            "9:16",
            9f,
            16f
        )

        val RATIO_2_3 = AspectRatio(
            "2x3",
            R.drawable.ic_ratio_2_3,
            "2:3",
            2f,
            3f
        )

        val RATIO_21_9 = AspectRatio(
            "21x9",
            R.drawable.ic_ratio_21_9,
            "2.35:1",
            2.35f,
            1f
        )

        val RATIO_2_1 = AspectRatio(
            "2x1",
            R.drawable.ic_ratio_2_1,
            "2:1",
            2f,
            1f
        )

        val RATIO_1_2 = AspectRatio(
            "1x2",
            R.drawable.ic_ratio_1_2,
            "1:2",
            1f,
            2f
        )
    }
}