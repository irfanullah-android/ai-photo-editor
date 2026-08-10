package com.editor.photo.video.collagemaker.photoedit.adsConfig.appOpenManager

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class LifecycleObserver (private val onForeground: () -> Unit) : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        onForeground()
    }
}