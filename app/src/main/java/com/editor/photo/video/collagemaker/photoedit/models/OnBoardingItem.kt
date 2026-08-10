package com.editor.photo.video.collagemaker.photoedit.models

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class OnBoardingItem(
    @DrawableRes val imageResId: Int,
    @StringRes val titleResId: Int,
    @StringRes val whiteWordResId: Int,
    @StringRes val descriptionResId: Int
)