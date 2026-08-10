package com.editor.photo.video.collagemaker.photoedit.adsConfig.bannerAdsManager

interface BannerCallBack {
    fun onAdFailedToLoad(adError: String)
    fun onAdLoaded()
    fun onAdImpression()
}
