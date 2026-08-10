package com.editor.photo.video.collagemaker.photoedit.adsConfig.nativeAdManager

import android.annotation.SuppressLint
import android.app.Activity
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.editor.photo.video.collagemaker.photoedit.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.formats.NativeAdOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAdView
import com.editor.photo.video.collagemaker.photoedit.adsConfig.nativeAdManager.enums.AdType
import com.editor.photo.video.collagemaker.photoedit.firebaseManager.firebaseRemoteConfig.RemoteConstants
import com.editor.photo.video.collagemaker.photoedit.utlis.CommonData
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object NativeAd {
    private const val TAG = "NativeAds"
    private var nativeAd: NativeAd? = null
    fun loadLanguageNativeAds(
        activity: Activity?,
        adsPlaceHolder: FrameLayout,
        adsContainerLayout: LinearLayout,
        nativeType: AdType,
        adUnit: String,
        callBack: NativeCallBack
    ) {

        val handlerException = CoroutineExceptionHandler { coroutineContext, throwable ->
            Log.e("adStatus", "${throwable.message}")
            callBack.onAdFailedToLoad("${throwable.message}")
        }
        activity?.let { mActivity ->
            try {
                if (CommonData.isInternetConnected(activity) && RemoteConstants.languageNativeControl && !RemoteConstants.inAppPurchase) {
                    adsPlaceHolder.visibility = View.VISIBLE
                    if (AdsConstants.adMobLoadNativeAd != null) {
                        nativeAd = AdsConstants.adMobLoadNativeAd
                        AdsConstants.adMobLoadNativeAd = null
                        Log.d(TAG, "admob native onAdLoaded")
                        callBack.onPreloaded()
                        displayNativeAd(mActivity, adsPlaceHolder, nativeType)
                        return
                    }
                    if (nativeAd == null) {
                        CoroutineScope(Dispatchers.IO + handlerException).launch {
                            val builder: AdLoader.Builder =
                                AdLoader.Builder(mActivity, adUnit)
                            val adLoader =
                                builder.forNativeAd { unifiedNativeAd: NativeAd? ->
                                    if (!mActivity.isDestroyed && !mActivity.isFinishing) {
                                        nativeAd = unifiedNativeAd
                                    } else {
                                        unifiedNativeAd?.destroy()
                                        return@forNativeAd
                                    }
                                }
                                    .withAdListener(object : AdListener() {
                                        override fun onAdImpression() {
                                            super.onAdImpression()
                                            Log.d(TAG, "admob native onAdImpression")
                                            callBack.onAdImpression()
                                            nativeAd = null
                                        }

                                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                            Log.e(
                                                TAG,
                                                "admob native onAdFailedToLoad: " + loadAdError.message
                                            )
                                            callBack.onAdFailedToLoad(loadAdError.message)
                                            adsContainerLayout.visibility = View.GONE
                                            nativeAd = null
                                            super.onAdFailedToLoad(loadAdError)
                                        }

                                        override fun onAdLoaded() {
                                            super.onAdLoaded()
                                            Log.d(TAG, "admob native onAdLoaded")
                                            adsContainerLayout.visibility = View.VISIBLE
                                            callBack.onAdLoaded()
                                            displayNativeAd(mActivity, adsPlaceHolder, nativeType)
                                        }

                                        override fun onAdClicked() {
                                            super.onAdClicked()

                                        }

                                    }).withNativeAdOptions(
                                        com.google.android.gms.ads.nativead.NativeAdOptions.Builder()
                                            .setAdChoicesPlacement(
                                                NativeAdOptions.ADCHOICES_TOP_RIGHT
                                            ).build()
                                    )
                                    .build()
                            adLoader.loadAd(AdRequest.Builder().build())
                        }
                    } else {
                        Log.e(TAG, "Native is already loaded")
                        callBack.onPreloaded()
                        displayNativeAd(mActivity, adsPlaceHolder, nativeType)
                    }
                } else {
                    adsPlaceHolder.visibility = View.GONE
                    callBack.onAdFailedToLoad("fail to load native")
                }
            } catch (ex: Exception) {
                adsPlaceHolder.visibility = View.GONE
                Log.e(TAG, "${ex.message}")
                callBack.onAdFailedToLoad("${ex.message}")
            }
        }
    }


    fun loadNativeAds(
        activity: Activity?,
        adsPlaceHolder: FrameLayout,
        adsContainerLayout: LinearLayout,
        nativeType: AdType,
        adControl:Boolean,
        adUnit: String,
        callBack: NativeCallBack
    ) {

        val handlerException = CoroutineExceptionHandler { coroutineContext, throwable ->
            Log.e("adStatus", "${throwable.message}")
            callBack.onAdFailedToLoad("${throwable.message}")
        }
        activity?.let { mActivity ->
            try {
                if (CommonData.isInternetConnected(activity) && adControl && !RemoteConstants.inAppPurchase) {
                    adsPlaceHolder.visibility = View.VISIBLE
                    if (AdsConstants.adMobLoadNativeAd != null) {
                        nativeAd = AdsConstants.adMobLoadNativeAd
                        AdsConstants.adMobLoadNativeAd = null
                        Log.d(TAG, "admob native onAdLoaded")
                        callBack.onPreloaded()
                        displayNativeAd(mActivity, adsPlaceHolder, nativeType)
                        return
                    }
                    if (nativeAd == null) {
                        CoroutineScope(Dispatchers.IO + handlerException).launch {
                            val builder: AdLoader.Builder =
                                AdLoader.Builder(mActivity, adUnit)
                            val adLoader =
                                builder.forNativeAd { unifiedNativeAd: NativeAd? ->
                                    if (!mActivity.isDestroyed && !mActivity.isFinishing) {
                                        nativeAd = unifiedNativeAd
                                    } else {
                                        unifiedNativeAd?.destroy()
                                        return@forNativeAd
                                    }
                                }
                                    .withAdListener(object : AdListener() {
                                        override fun onAdImpression() {
                                            super.onAdImpression()
                                            Log.d(TAG, "admob native onAdImpression")
                                            callBack.onAdImpression()
                                            nativeAd = null
                                        }

                                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                                            Log.e(
                                                TAG,
                                                "admob native onAdFailedToLoad: " + loadAdError.message
                                            )
                                            callBack.onAdFailedToLoad(loadAdError.message)
                                            adsContainerLayout.visibility = View.GONE
                                            nativeAd = null
                                            super.onAdFailedToLoad(loadAdError)
                                        }

                                        override fun onAdLoaded() {
                                            super.onAdLoaded()
                                            Log.d(TAG, "admob native onAdLoaded")
                                            adsContainerLayout.visibility = View.VISIBLE
                                            callBack.onAdLoaded()
                                            displayNativeAd(mActivity, adsPlaceHolder, nativeType)
                                        }

                                        override fun onAdClicked() {
                                            super.onAdClicked()

                                        }

                                    }).withNativeAdOptions(
                                        com.google.android.gms.ads.nativead.NativeAdOptions.Builder()
                                            .setAdChoicesPlacement(
                                                NativeAdOptions.ADCHOICES_TOP_RIGHT
                                            ).build()
                                    )
                                    .build()
                            adLoader.loadAd(AdRequest.Builder().build())
                        }
                    } else {
                        Log.e(TAG, "Native is already loaded")
                        callBack.onPreloaded()
                        displayNativeAd(mActivity, adsPlaceHolder, nativeType)
                    }
                } else {
                    adsPlaceHolder.visibility = View.GONE
                    callBack.onAdFailedToLoad("fail to load native")
                }
            } catch (ex: Exception) {
                adsPlaceHolder.visibility = View.GONE
                Log.e(TAG, "${ex.message}")
                callBack.onAdFailedToLoad("${ex.message}")
            }
        }
    }



    @SuppressLint("InflateParams")
    private fun displayNativeAd(
        activity: Activity?,
        adMobNativeContainer: FrameLayout,
        nativeType: AdType,
    ) {
        activity?.let { mActivity ->
            try {
                nativeAd?.let { ad ->
                    val inflater = LayoutInflater.from(mActivity)
                    val adView: NativeAdView = if (nativeType == AdType.SMALL) {
                        inflater.inflate(R.layout.admob_native_small2, null) as NativeAdView
                    } else if (nativeType == AdType.LARGE) {
                        inflater.inflate(R.layout.admob_native_medium, null) as NativeAdView
                    } else if (nativeType == AdType.LARGE_ADJUSTED) {
                        if (isSupportFullScreen(mActivity)) {
                            inflater.inflate(R.layout.admob_native_medium, null) as NativeAdView
                        } else {
                            inflater.inflate(R.layout.admob_native_small2, null) as NativeAdView
                        }

                    } else {
                        inflater.inflate(R.layout.admob_native_medium, null) as NativeAdView
                    }
                    adMobNativeContainer.removeAllViews()
                    adMobNativeContainer.addView(adView)

                    if (nativeType == AdType.LARGE) {
                        val mediaView: MediaView = adView.findViewById(R.id.media_view)
                        adView.mediaView = mediaView
                    }
                    if (nativeType == AdType.LARGE_ADJUSTED) {
                        if (isSupportFullScreen(mActivity)) {
                            val mediaView: MediaView = adView.findViewById(R.id.media_view)
                            adView.mediaView = mediaView
                        }
                    }

                    // Set other ad assets.
                    adView.headlineView = adView.findViewById(R.id.ad_headline)
                    adView.bodyView = adView.findViewById(R.id.ad_body)
                    adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
                    adView.iconView = adView.findViewById(R.id.ad_app_icon)

                    //Headline
                    adView.headlineView?.let { headline ->
                        (headline as TextView).text = ad.headline
                        headline.isSelected = true
                    }

                    //Body
                    adView.bodyView?.let { bodyView ->
                        if (ad.body == null) {
                            bodyView.visibility = View.INVISIBLE
                        } else {
                            bodyView.visibility = View.VISIBLE
                            (bodyView as TextView).text = ad.body
                        }

                    }

                    //Call to Action
                    adView.callToActionView?.let { ctaView ->
                        if (ad.callToAction == null) {
                            ctaView.visibility = View.INVISIBLE
                        } else {
                            ctaView.visibility = View.VISIBLE
                            (ctaView as Button).text = ad.callToAction
                        }

                    }

                    //Icon
                    adView.iconView?.let { iconView ->
                        if (ad.icon == null) {
                            iconView.visibility = View.GONE
                        } else {
                            (iconView as ImageView).setImageDrawable(ad.icon?.drawable)
                            iconView.visibility = View.VISIBLE
                        }

                    }

                    adView.advertiserView?.let { adverView ->

                        if (ad.advertiser == null) {
                            adverView.visibility = View.GONE
                        } else {
                            (adverView as TextView).text = ad.advertiser
                            adverView.visibility = View.GONE
                        }
                    }

                    adView.setNativeAd(ad)
                }
            } catch (ex: Exception) {
                Log.e(TAG, "displayNativeAd: ${ex.message}")
            }
        }
    }

    private fun isSupportFullScreen(activity: Activity): Boolean {
        try {
            val outMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(outMetrics)
            if (outMetrics.heightPixels > 1280) {
                return true
            }
        } catch (ignored: Exception) {
        }
        return false
    }

}