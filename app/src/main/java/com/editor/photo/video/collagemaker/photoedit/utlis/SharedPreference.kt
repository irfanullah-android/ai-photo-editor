package com.editor.photo.video.collagemaker.photoedit.utlis

import android.annotation.SuppressLint
import android.content.Context
import com.editor.photo.video.collagemaker.photoedit.MyApplication

object SharedPreference {
    private const val KEY_ONBOARDED = "isOnBoarded"
    private const val KEY_LANGUAGE_SELECTED = "languageSelected"

    private val sharedPreferences =
        MyApplication.context?.getSharedPreferences("AppPreference", Context.MODE_PRIVATE)

    var AppLanguageCode: String
        get() = sharedPreferences?.getString("appLanguageKey", "en") ?: "en"
        @SuppressLint("UseKtx")
        set(value) {
            sharedPreferences?.edit()?.putString("appLanguageKey", value)?.apply()
        }

    var AppTheme: String
        get() = sharedPreferences?.getString("appThemeKey", "system") ?: "system"
        @SuppressLint("UseKtx")
        set(value) {
            sharedPreferences?.edit()?.putString("appThemeKey", value)?.apply()
        }

    fun setOnBoardingShown(context: Context, shown: Boolean) {
        sharedPreferences?.edit()?.putBoolean(KEY_ONBOARDED, shown)?.apply()
    }

    fun isOnBoardingShown(context: Context): Boolean {
        return sharedPreferences?.getBoolean(KEY_ONBOARDED, false) ?: false
    }

    fun setLanguageSelected(context: Context, selected: Boolean) {
        sharedPreferences?.edit()?.putBoolean(KEY_LANGUAGE_SELECTED, selected)?.apply()
    }

    fun isLanguageSelected(context: Context): Boolean {
        return sharedPreferences?.getBoolean(KEY_LANGUAGE_SELECTED, false) ?: false
    }

    var lastInterstitialShownTime: Long
        get() = sharedPreferences?.getLong("lastInterstitialShownTime", 0L) ?: 0L
        set(value) {
            sharedPreferences?.edit()?.putLong("lastInterstitialShownTime", value)?.apply()
        }
}