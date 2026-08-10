package com.editor.photo.video.collagemaker.photoedit.utlis

import android.annotation.SuppressLint
import android.content.Context
import com.editor.photo.video.collagemaker.photoedit.MyApplication

object SharedPreference {
    private const val PREFS_NAME = "AppPrefs"
    private const val KEY_ONBOARDED = "isOnBoarded"
    private const val KEY_SORT_ORDER = "sortOrder"
    private const val KEY_SELECTED_ROLE = "selectedRole"
    private const val KEY_SELECTED_FEATURES = "selectedFeatures"
    private const val KEY_LANGUAGE_SELECTED = "languageSelected"
    private const val KEY_KEEP_SCREEN_ON = "keepScreenOn"


    private val sharedPreferences =
        MyApplication.context?.getSharedPreferences("AppPreference", Context.MODE_PRIVATE)


    var AppLanguageCode: String
        get() = sharedPreferences?.getString("appLanguageKey", "en") ?: "en"
        @SuppressLint("UseKtx")
        set(value) {
            val editor = sharedPreferences?.edit()
            editor?.putString("appLanguageKey", value)
            editor?.apply()
        }

    var AppTheme: String
        get() = sharedPreferences?.getString("appThemeKey", "system") ?: "system"
        @SuppressLint("UseKtx")
        set(value) {
            val editor = sharedPreferences?.edit()
            editor?.putString("appThemeKey", value)
            editor?.apply()
        }

    fun setOnBoardingShown(context: Context, shown: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ONBOARDED, shown).apply()
    }

    fun isOnBoardingShown(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ONBOARDED, false)
    }
    fun setLanguageSelected(context: Context, selected: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_LANGUAGE_SELECTED, selected).apply()
    }

    fun isLanguageSelected(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LANGUAGE_SELECTED, false)
    }

    var lastInterstitialShownTime: Long
        get() = sharedPreferences?.getLong("lastInterstitialShownTime", 0L) ?: 0L
        set(value) {
            sharedPreferences?.edit()?.apply {
                putLong("lastInterstitialShownTime", value)
                apply()
            }
        }
}