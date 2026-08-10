package com.editor.photo.video.collagemaker.photoedit.firebaseManager.firebaseRemoteConfig

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.editor.photo.video.collagemaker.photoedit.BuildConfig
import com.editor.photo.video.collagemaker.photoedit.helpers.managers.InternetManager
import com.editor.photo.video.collagemaker.photoedit.firebaseManager.firebaseCrashlytics.FirebaseUtils.recordException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RemoteConfiguration(
    private val internetManager: InternetManager
) {
    private val configTag = "REMOTE_CONFIG"
    private val remoteConfig = Firebase.remoteConfig
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    init {
        val settings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600
        }
        remoteConfig.setConfigSettingsAsync(settings)
    }

    fun fetchRemoteValues() {
        scope.launch {
            delay(1000)
            if (!internetManager.isInternetConnected) {
                Log.w(configTag, "No internet connection.")
                return@launch
            }
            try {
                remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        try {
                            applyFetchedValues()
                            Log.d(configTag, "Remote config applied successfully")
                        } catch (ex: Exception) {
                            ex.recordException("applyFetchedValues Exception")
                        }
                    } else {
                        task.exception?.recordException("Remote fetch failed")
                    }
                }
            } catch (e: Exception) {
                e.recordException("fetchRemoteValues Exception")
            }
        }
    }

    private fun applyFetchedValues() {
        with(RemoteConstants) {
            splashInterstitialControl =
                remoteConfig.getBoolean("splashInterstitialControl")
            interstitialControl =
                remoteConfig.getBoolean("interstitialControl")

            timeDelayInterstitialControl =
                remoteConfig.getBoolean("timeDelayInterstitialControl")
            bannerControl =
                remoteConfig.getBoolean("bannerControl")
            languageNativeControl =
                remoteConfig.getBoolean("languageNativeControl")
            exitNativeControl =
                remoteConfig.getBoolean("exitNativeControl")

            AppUpdateVersion = remoteConfig.getString("Release_Version")
            trendingVideosNativeControl =
                remoteConfig.getBoolean("trendingVideosNativeControl")

            songsNativeControl =
                remoteConfig.getBoolean("songsNativeControl")
            appOpenControl =
                remoteConfig.getBoolean("appOpenControl")
            interstitialShowDelay = remoteConfig.getLong("interstitialShowDelay")

            permissionInterstitialControl = remoteConfig.getBoolean("permissionInterstitial")



            splashInterstitialAdControlAB = remoteConfig.getBoolean("splashInterstitialAdControlAB")
            splashAppOpenAdControlAB = remoteConfig.getBoolean("splashAppOpenAdControlAB")
            Splash_OpenAPP_Control = remoteConfig.getBoolean("splashAppOpenControl")

            interstitialClickThreshold = remoteConfig.getLong("interstitialClickThreshold")

        }
        if (BuildConfig.DEBUG) {
            log()
        }
        Log.d(configTag, "checkRemoteConfig: Fetched Successfully")
    }


    fun clear() {
        job.cancel()
    }


    private fun log() {
        Log.d(configTag, "splashInterstitialControl= ${RemoteConstants.splashInterstitialControl}")
        Log.d(configTag, "interstitialControl= ${RemoteConstants.interstitialControl}")
        Log.d(configTag, "bannerControl= ${RemoteConstants.bannerControl}")
        Log.d(configTag, "AppUpdateVersion: ${RemoteConstants.AppUpdateVersion}")

        Log.d(configTag, "languageNativeControl= ${RemoteConstants.languageNativeControl}")
        Log.d(configTag, "appOpenControl= ${RemoteConstants.appOpenControl}")
    }

}
