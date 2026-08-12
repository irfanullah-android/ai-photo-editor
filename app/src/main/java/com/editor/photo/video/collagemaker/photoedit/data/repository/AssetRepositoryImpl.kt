package com.editor.photo.video.collagemaker.photoedit.data.repository

import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.domain.repository.AssetRepository
import javax.inject.Inject

class AssetRepositoryImpl @Inject constructor() : AssetRepository {
    override fun getStickers(): List<Int> {
        return emptyList()
    }

    override fun getFrames(): List<Int> {
        return listOf(
            R.drawable.frame_classic,
            R.drawable.frame_modern,
            R.drawable.frame_vintage,
            R.drawable.frame_polaroid,
            R.drawable.frame_film,
            R.drawable.frame_wood,
            R.drawable.frame_metal,
            R.drawable.frame_gold,
            R.drawable.frame_silver,
            R.drawable.frame_black,
            R.drawable.frame_white
        )
    }
}