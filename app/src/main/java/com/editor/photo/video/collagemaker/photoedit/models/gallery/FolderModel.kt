package com.editor.photo.video.collagemaker.photoedit.models.gallery

import android.net.Uri

data class FolderModel(
    val name: String,
    val photoCount: Int,
    val thumbnailUri: Uri?,
    val path: String = ""
)