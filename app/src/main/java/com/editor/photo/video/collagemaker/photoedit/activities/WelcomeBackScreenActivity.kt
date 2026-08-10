package com.editor.photo.video.collagemaker.photoedit.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.editor.photo.video.collagemaker.photoedit.R
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.editor.photo.video.collagemaker.photoedit.adsConfig.appOpenManager.AppOpenAdHelper
import com.editor.photo.video.collagemaker.photoedit.firebaseManager.firebaseRemoteConfig.RemoteConstants
import com.editor.photo.video.collagemaker.photoedit.firebaseManager.firebaseAnalytics.AnalyticsLogger

class WelcomeBackScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome_back_screen)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        findViewById<ShimmerFrameLayout>(R.id.shimmer_text)?.startShimmer()

        Handler(Looper.getMainLooper()).postDelayed({
            loadAndShowAppOpenAd()
        }, 700)
    }

    private fun loadAndShowAppOpenAd() {

        val request = AdRequest.Builder().build()

        AppOpenAd.load(
            this,
            RemoteConstants.appOpenId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {

                override fun onAdLoaded(appOpenAd: AppOpenAd) {

                    AnalyticsLogger.logEvent("app_open_ad_loaded")

                    appOpenAd.fullScreenContentCallback =
                        object : FullScreenContentCallback() {

                            override fun onAdDismissedFullScreenContent() {
                                AppOpenAdHelper.resetForegroundFlag()
                                AnalyticsLogger.logEvent("app_open_ad_dismissed")
                                finish()
                            }
                        }

                    appOpenAd.show(this@WelcomeBackScreenActivity)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    AppOpenAdHelper.resetForegroundFlag()
                    AnalyticsLogger.logEvent("app_open_ad_failed_to_load")
                    finish()
                }
            }
        )
    }
}