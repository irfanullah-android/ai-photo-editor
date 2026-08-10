package com.editor.photo.video.collagemaker.photoedit.adsConfig.appOpenManager

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner

object AppOpenAdHelper {
    @SuppressLint("StaticFieldLeak")
    private lateinit var manager: AppOpenAd

    fun init(application: Application, activity: Activity) {
        manager = AppOpenAd(application, activity)
        ProcessLifecycleOwner.get().lifecycle.addObserver(LifecycleObserver {
            manager.onAppForegrounded()
        })
    }

    fun getManager(): AppOpenAd? {
        return if (::manager.isInitialized) manager else null
    }

    fun resetForegroundFlag() {
        if (::manager.isInitialized) {
            manager.resetForegroundFlag()
        }
    }
}