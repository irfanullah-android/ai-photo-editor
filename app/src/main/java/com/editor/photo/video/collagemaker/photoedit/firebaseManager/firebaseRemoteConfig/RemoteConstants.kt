package com.editor.photo.video.collagemaker.photoedit.firebaseManager.firebaseRemoteConfig
import com.editor.photo.video.collagemaker.photoedit.BuildConfig

object RemoteConstants {
    var inAppPurchase = false

    var openAppAdIdTest = "ca-app-pub-3940256099942544/9257395921"
    var bannerAdIdTest = "ca-app-pub-3940256099942544/2014213617"
    var interstitialAdIdTest = "ca-app-pub-3940256099942544/1033173712"
    var Native_Ad_Id_Test = "ca-app-pub-3940256099942544/2247696110"

    var AppUpdateVersion = BuildConfig.VERSION_NAME
    var Appversion = BuildConfig.VERSION_NAME

    var interstitialShowDelay: Long = 1
    var splashInterstitialId =
        if (BuildConfig.DEBUG) interstitialAdIdTest else ""
    var interstitialId =
        if (BuildConfig.DEBUG) interstitialAdIdTest else ""


    var timeDelayInterstitialId =
        if (BuildConfig.DEBUG) interstitialAdIdTest else ""
    var permissionInterstitial =
        if (BuildConfig.DEBUG) interstitialAdIdTest else ""
    var bannerId =
        if (BuildConfig.DEBUG) bannerAdIdTest else "ca-app-pub-2971760624474212/6993254494"
    var languageNativeId =
        if (BuildConfig.DEBUG) Native_Ad_Id_Test else ""
    var exitNativeId =
        if (BuildConfig.DEBUG) Native_Ad_Id_Test else ""
    var trendingVideosNativeId =
        if (BuildConfig.DEBUG) Native_Ad_Id_Test else ""


    var songsNativeId =
        if (BuildConfig.DEBUG) Native_Ad_Id_Test else ""
    var appOpenId =
        if (BuildConfig.DEBUG) openAppAdIdTest else ""


    var Splash_OpenAPP_Id =
        if (BuildConfig.DEBUG) openAppAdIdTest else ""

    var splashInterstitialControl = true
    var splashInterstitialAdControlAB = true
    var splashAppOpenAdControlAB = false
    var interstitialControl = true
    var timeDelayInterstitialControl = true
    var bannerControl = true
    var languageNativeControl = true
    var exitNativeControl = true


    var songsNativeControl = true

    var trendingVideosNativeControl = true
    var appOpenControl = true

    //click counter for interstitial ad
    var interstitialClickThreshold: Long = 3  // default

    var Splash_OpenAPP_Control = true


    var permissionInterstitialControl= true
}