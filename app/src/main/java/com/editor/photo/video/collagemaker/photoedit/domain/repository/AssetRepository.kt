package com.editor.photo.video.collagemaker.photoedit.domain.repository

interface AssetRepository {
    fun getStickers(): List<Int>
    fun getFrames(): List<Int>
}
