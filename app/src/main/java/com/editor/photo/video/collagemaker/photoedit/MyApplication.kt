package com.editor.photo.video.collagemaker.photoedit

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import com.google.firebase.analytics.FirebaseAnalytics
import coil.memory.MemoryCache
import com.editor.photo.video.collagemaker.photoedit.firebaseManager.firebaseAnalytics.AnalyticsLogger
import com.editor.photo.video.collagemaker.photoedit.firebaseManager.firebaseRemoteConfig.RemoteConfiguration
import com.editor.photo.video.collagemaker.photoedit.helpers.managers.InternetManager
import com.editor.photo.video.collagemaker.photoedit.utlis.SharedPreference
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application(), ImageLoaderFactory {

    companion object {
        lateinit var context: Application
            private set
    }

    override fun onCreate() {
        super.onCreate()
        context = this
        val analytics = FirebaseAnalytics.getInstance(this)
        AnalyticsLogger.init(analytics)
        InternetManager.init(this)
        initRemoteConfig()
        val languageCode = SharedPreference.AppLanguageCode.ifEmpty { "en" }
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024L * 1024L)
                    .build()
            }
            .crossfade(false)
            .build()
    }

    private fun initRemoteConfig() {
        val remoteConfiguration = RemoteConfiguration(InternetManager)
        remoteConfiguration.fetchRemoteValues()
    }
}
