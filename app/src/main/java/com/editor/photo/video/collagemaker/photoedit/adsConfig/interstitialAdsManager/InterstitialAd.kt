package com.editor.photo.video.collagemaker.photoedit.adsConfig.interstitialAdsManager

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback
import com.editor.photo.video.collagemaker.photoedit.firebaseManager.firebaseAnalytics.AnalyticsLogger
import com.editor.photo.video.collagemaker.photoedit.firebaseManager.firebaseRemoteConfig.RemoteConstants
import com.editor.photo.video.collagemaker.photoedit.utlis.CommonData

object InterstitialAd {
    var interstitialAd: AdManagerInterstitialAd? = null
    private var isAdClicked = false
    private var tag = "InterstitialLogs"

    fun loadInterstitialAd(context: Context) {
        if (RemoteConstants.interstitialControl) {
            CommonData.debugLog(tag, "Load  InterstitialAd Method...")

            val adRequest = AdManagerAdRequest.Builder().build()

            AdManagerInterstitialAd.load(
                context, RemoteConstants.interstitialId, adRequest,
                object : AdManagerInterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        super.onAdFailedToLoad(p0)
                        CommonData.debugLog(
                            tag,
                            "InterstitialAd Ad Failed to Load: " + p0.message
                        )
                        AnalyticsLogger.logEvent("main_home_interstitial_ad_failed_to_load")
                        interstitialAd = null
                    }

                    override fun onAdLoaded(p0: AdManagerInterstitialAd) {
                        super.onAdLoaded(p0)
                        CommonData.debugLog(tag, "InterstitialAd Ad Loaded...")
                        AnalyticsLogger.logEvent("main_home_interstitial_ad_loaded")
                        interstitialAd = p0
                    }
                })
        }
    }

    fun showInterstitialAd(
        activity: Activity,
        listener: InterstitialCallBack?
    ) {
        if (interstitialAd != null  && RemoteConstants.interstitialControl && !RemoteConstants.inAppPurchase) {
                val ad = interstitialAd!!
                ad.show(activity)
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdImpression() {
                        super.onAdImpression()
                        CommonData.debugLog(tag, "InterstitialAd Impression Counted...")
                        AnalyticsLogger.logEvent("main_home_interstitial_ad_impression")

                    }

                    override fun onAdShowedFullScreenContent() {
                        super.onAdShowedFullScreenContent()
                        CommonData.debugLog(tag, "InterstitialAd Shown...")
                        AnalyticsLogger.logEvent("main_home_interstitial_ad_shown")

                        isAdClicked = false
                        interstitialAd = null
                    }


                    override fun onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent()
                        CommonData.debugLog(tag, "InterstitialAd Dismissed...")
                        AnalyticsLogger.logEvent("main_home_interstitial_ad_dismissed")
                        loadInterstitialAd(
                            activity
                        )
                        listener?.onAdDismissed()
                    }

                    override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                        super.onAdFailedToShowFullScreenContent(p0)
                        CommonData.debugLog(tag, "InterstitialAd Failed to show: " + p0.message)
                        AnalyticsLogger.logEvent("main_home_interstitial_ad_failed_to_show")
                        interstitialAd = null
                        loadInterstitialAd(
                            activity
                        )
                        listener?.onAdDismissed()
                    }
                }

        } else {
            listener?.onAdDismissed()
        }
    }

//    fun showInterstitialAdWithDelay(
//        activity: Activity,
//        listener: InterstitialCallBack?
//    ) {
//        val currentTime = System.currentTimeMillis()
//        val lastShown = lastInterstitialShownTime
//        val shouldShowAd = (currentTime - lastShown) >= (60_000L * interstitialShowDelay) || lastShown == 0L
//
//        if (shouldShowAd && interstitialAd != null && interstitialControl && !inAppPurchase) {
//            val ad = interstitialAd!!
//            ad.show(activity)
//            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
//                override fun onAdImpression() {
//                    super.onAdImpression()
//                    debugLog(tag, "InterstitialAd Impression Counted...")
//                    logEvent( "main_home_interstitial_ad_impression")
//                }
//
//                override fun onAdShowedFullScreenContent() {
//                    super.onAdShowedFullScreenContent()
//                    debugLog(tag, "InterstitialAd Shown...")
//                    logEvent( "main_home_interstitial_ad_shown")
//
//                    isInterstitialShowing= true
//                    isAdClicked = false
//                    interstitialAd = null
//                    lastInterstitialShownTime = System.currentTimeMillis()
//                }
//
//                override fun onAdDismissedFullScreenContent() {
//                    super.onAdDismissedFullScreenContent()
//                    debugLog(tag, "InterstitialAd Dismissed...")
//                    logEvent( "main_home_interstitial_ad_dismissed")
//                    isInterstitialShowing= false
//                    loadInterstitialAd(activity)
//                    listener?.onAdDismissed()
//                }
//
//                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
//                    super.onAdFailedToShowFullScreenContent(p0)
//                    debugLog(tag, "InterstitialAd Failed to show: ${p0.message}")
//                    logEvent( "main_home_interstitial_ad_failed_to_show")
//                    isInterstitialShowing= false
//                    interstitialAd = null
//                    loadInterstitialAd(activity)
//                    listener?.onAdDismissed()
//                }
//            }
//        } else {
//            debugLog(tag, "InterstitialAd not shown due to time delay or other conditions")
//            listener?.onAdDismissed()
//        }
//    }


}