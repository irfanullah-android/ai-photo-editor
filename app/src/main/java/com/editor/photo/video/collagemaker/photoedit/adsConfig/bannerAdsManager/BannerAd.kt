package com.editor.photo.video.collagemaker.photoedit.adsConfig.bannerAdsManager

import android.app.Activity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.editor.photo.video.collagemaker.photoedit.firebaseManager.firebaseRemoteConfig.RemoteConstants
import com.editor.photo.video.collagemaker.photoedit.utlis.CommonData

@Suppress("DEPRECATION")
class BannerAd(activity: Activity) {
    private var bannerCallBack: BannerCallBack? = null
    private val mActivity: Activity = activity
    private var adaptiveAdView: AdView? = null
    private val tag = "BannerAds"

    fun loadBannerAds(
        bannerLayout: ViewGroup,
        adsHolder: LinearLayout,
        loading: FrameLayout,
        mListener: BannerCallBack
    ) {
        bannerCallBack = mListener

        if (CommonData.isInternetConnected(mActivity) && RemoteConstants.bannerControl && !RemoteConstants.inAppPurchase) {

            if (adsHolder.childCount > 0) {
                Log.i(tag, "Banner already exists, skipping load")
                loading.visibility = View.GONE
                bannerCallBack?.onAdLoaded()
                return
            }

            adaptiveAdView = AdView(mActivity)
            adsHolder.addView(adaptiveAdView)
            adaptiveAdView?.adUnitId = RemoteConstants.bannerId
            adaptiveAdView?.setAdSize(getAdSize(adsHolder))

            val extras = Bundle()/*.apply {  putString("collapsible", "bottom") }*/
            val adRequest = AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                .build()

            adaptiveAdView?.loadAd(adRequest)

            adaptiveAdView?.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.i(tag, "admob banner onAdLoaded")
                    loading.visibility = View.GONE
                    bannerCallBack?.onAdLoaded()
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.i(tag, "admob banner onAdFailedToLoad $adError")
                    bannerCallBack?.onAdFailedToLoad(adError.message)
                }

                override fun onAdImpression() {
                    Log.i(tag, "admob banner onAdImpression")
                    bannerCallBack?.onAdImpression()
                    super.onAdImpression()
                }
            }

        } else {
            bannerLayout.visibility = View.GONE
        }
    }

    fun loadCollapsableBannerAds(
        bannerLayout: ViewGroup,
        adsHolder: LinearLayout,
        loading: FrameLayout,
        mListener: BannerCallBack
    ) {
        bannerCallBack = mListener

        if (CommonData.isInternetConnected(mActivity) && RemoteConstants.bannerControl && !RemoteConstants.inAppPurchase) {

            // ⭐ IMPORTANT: Pehle check karein ke already AdView exist karta hai
            if (adsHolder.childCount > 0) {
                Log.i(tag, "Banner already exists, skipping load")
                loading.visibility = View.GONE
                bannerCallBack?.onAdLoaded()
                return
            }

            adaptiveAdView = AdView(mActivity)
            adsHolder.addView(adaptiveAdView)
            adaptiveAdView?.adUnitId = RemoteConstants.bannerId
            adaptiveAdView?.setAdSize(getAdSize(adsHolder))

            val extras = Bundle().apply { putString("collapsible", "bottom") }
            val adRequest = AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                .build()

            adaptiveAdView?.loadAd(adRequest)

            adaptiveAdView?.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.i(tag, "admob banner onAdLoaded")
                    loading.visibility = View.GONE
                    bannerCallBack?.onAdLoaded()
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.i(tag, "admob banner onAdFailedToLoad $adError")
                    bannerCallBack?.onAdFailedToLoad(adError.message)
                }

                override fun onAdImpression() {
                    Log.i(tag, "admob banner onAdImpression")
                    bannerCallBack?.onAdImpression()
                    super.onAdImpression()
                }
            }

        } else {
            bannerLayout.visibility = View.GONE
        }
    }

    private fun getAdSize(adContainer: LinearLayout): AdSize {
        val display = mActivity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density
        var adWidthPixels = adContainer.width.toFloat()

        if (adWidthPixels == 0f) {
            adWidthPixels = outMetrics.widthPixels.toFloat()
        }

        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(mActivity, adWidth)
    }


    fun removeBanner() {
        adaptiveAdView?.destroy()
    }
}