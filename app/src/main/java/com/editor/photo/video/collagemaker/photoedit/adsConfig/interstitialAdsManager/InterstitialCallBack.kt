package com.editor.photo.video.collagemaker.photoedit.adsConfig.interstitialAdsManager


interface InterstitialCallBack {
    fun onAdFailedToLoad(adError:String)
    fun onAdLoaded()
    fun onAdDismissed()
    fun onAdClicked()

}