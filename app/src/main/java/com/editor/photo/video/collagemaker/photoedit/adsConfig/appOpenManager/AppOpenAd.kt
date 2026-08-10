package com.editor.photo.video.collagemaker.photoedit.adsConfig.appOpenManager

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd as GoogleAppOpenAd
import com.editor.photo.video.collagemaker.photoedit.activities.WelcomeBackScreenActivity
import com.editor.photo.video.collagemaker.photoedit.firebaseManager.firebaseRemoteConfig.RemoteConstants
import com.editor.photo.video.collagemaker.photoedit.utlis.AppState
import com.editor.photo.video.collagemaker.photoedit.adsConfig.interstitialAdsManager.AdController
import com.editor.photo.video.collagemaker.photoedit.utlis.CommonData
import java.util.Date

class AppOpenAd(
    private val application: Application,
    private val activityContext: Activity
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var appOpenAd: GoogleAppOpenAd? = null
    private var isShowingAd = false
    private var isLoadingAd = false
    private var loadTime: Long = 0L
    private var currentActivity: Activity? = null
    private var hasShownInThisForeground = false

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        CommonData.debugLog("AppOpenAd", " Lifecycle observers registered")
    }

    fun loadAd() {
        if (RemoteConstants.appOpenControl && !RemoteConstants.inAppPurchase) {
            if (isAdAvailable() && isLoadingAd) {
                CommonData.debugLog("AppOpenAd", "🔁 Ad is already loaded or loading.")
                return
            }
            isLoadingAd = true
            val request = AdRequest.Builder().build()

            GoogleAppOpenAd.load(
                activityContext,
                RemoteConstants.appOpenId,
                request,
                object : GoogleAppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: GoogleAppOpenAd) {
                        appOpenAd = ad
                        loadTime = Date().time
                        isLoadingAd = false
                        CommonData.debugLog("AppOpenAd", "Ad Loaded Successfully.")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        isLoadingAd = false
                        CommonData.debugLog("AppOpenAd", "Failed to load ad: ${error.message}")
                    }
                }
            )
        }
    }

    private fun isAdAvailable(): Boolean {
        val isFresh = (Date().time - loadTime) < 4 * 60 * 60 * 1000
        val available = appOpenAd != null && isFresh
        CommonData.debugLog("AppOpenAd", "isAdAvailable = $available | Fresh = $isFresh")
        return available
    }

    fun showAdIfAvailable() {
        CommonData.debugLog("AppOpenAd", "Attempting to show App Open Ad...")
        if (AdController.isInterstitialShowing) return
        if (!isAdAvailable() && RemoteConstants.appOpenControl && !RemoteConstants.inAppPurchase) {
            CommonData.debugLog("AppOpenAd", "Ad not available, loading...")
            return
        }
        if (isShowingAd) {
            CommonData.debugLog("AppOpenAd", "Ad is already being shown.")
            return
        }
        if (currentActivity == null) {
            CommonData.debugLog("AppOpenAd", "currentActivity is null — delaying ad show.")
            Handler(Looper.getMainLooper()).postDelayed({ showAdIfAvailable() }, 800)
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                CommonData.debugLog("AppOpenAd", "Ad is now showing.")
                isShowingAd = true
            }

            override fun onAdDismissedFullScreenContent() {
                CommonData.debugLog("AppOpenAd", "Ad was dismissed.")
                appOpenAd = null
                isShowingAd = false
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                CommonData.debugLog("AppOpenAd", "Failed to show ad: ${error.message}")
                isShowingAd = false
            }
        }

        CommonData.debugLog("AppOpenAd", "Showing App Open Ad now...")
        appOpenAd?.show(currentActivity!!)
    }

    fun onAppForegrounded() {
        CommonData.debugLog("AppOpenAd", "🔍 onAppForegrounded called")

        if (hasShownInThisForeground) {
            CommonData.debugLog("AppOpenAd", "Already shown in this foreground session. Skipping.")
            return
        }

        hasShownInThisForeground = true
        CommonData.debugLog("AppOpenAd", " Flag set - will check activity in 300ms")

        Handler(Looper.getMainLooper()).postDelayed({
            CommonData.debugLog(
                "AppOpenAd",
                "Current activity after delay: ${currentActivity?.javaClass?.simpleName}"
            )

            if (AppState.isColdStart) {
                CommonData.debugLog("AppOpenAd", " isColdStart true - skipping ad")
                AppState.isColdStart = false
                hasShownInThisForeground = false
                return@postDelayed
            }

            CommonData.debugLog(
                "AppOpenAd",
                " background to foreground - showing WelcomeBackScreenActivity"
            )
            val intent = Intent(activityContext, WelcomeBackScreenActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activityContext.startActivity(intent)
        }, 300)
    }

    fun resetForegroundFlag() {
        CommonData.debugLog("AppOpenAd", " Resetting foreground flag")
        hasShownInThisForeground = false
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        CommonData.debugLog("AppOpenAd", " Activity resumed: ${activity.javaClass.simpleName}")
    }

    override fun onActivityPaused(activity: Activity) {
        CommonData.debugLog("AppOpenAd", " Activity paused: ${activity.javaClass.simpleName}")
        if (currentActivity == activity) currentActivity = null
    }

    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}