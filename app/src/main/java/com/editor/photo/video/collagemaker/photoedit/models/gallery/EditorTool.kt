package com.editor.photo.video.collagemaker.photoedit.models.gallery

enum class EditorTool {
    CANVAS, FILTER, ADJUST, EFFECT, STICKER, TEXT,
    REMOVE, ENHANCE, DOODLE, CROP, FRAME, ROTATE, NONE;

    companion object {
        fun fromName(name: String) = values().find {
            it.name.equals(name, ignoreCase = true)
        } ?: NONE
    }
}