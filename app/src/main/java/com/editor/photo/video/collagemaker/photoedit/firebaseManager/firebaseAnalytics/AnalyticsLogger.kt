package com.editor.photo.video.collagemaker.photoedit.firebaseManager.firebaseAnalytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.editor.photo.video.collagemaker.photoedit.MyApplication

object AnalyticsLogger {

    private const val TAG = "AnalyticsLogger"
    private const val PREFS_NAME = "EventTracker"
    private const val PREFIX_FIRST_TIME = "FT_"
    private const val PREFIX_SECOND_TIME = "ST_"

    private var firebaseAnalytics: FirebaseAnalytics? = null

    private val sharedPreferences by lazy {
        try {
            val prefs = MyApplication.context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            Log.d(TAG, "SharedPreferences initialized: ${prefs != null}")
            prefs
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SharedPreferences: ${e.message}", e)
            null
        }
    }

    fun init(analytics: FirebaseAnalytics) {
        firebaseAnalytics = analytics
        Log.d(TAG, "FirebaseAnalytics initialized successfully")
    }

    fun logEvent(eventName: String, params: Bundle? = Bundle()) {
        try {
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            Log.d(TAG, "📊 Logging Event: $eventName")

            // Check karen ke event first time hai ya nahi
            val isFirstTime = isEventFirstTime(eventName)
            Log.d(TAG, "🔍 Is First Time: $isFirstTime")

            // Prefix add karen
            val prefixedEventName = if (isFirstTime) {
                markEventAsLogged(eventName)
                "$PREFIX_FIRST_TIME$eventName"
            } else {
                "$PREFIX_SECOND_TIME$eventName"
            }

            Log.d(TAG, "🏷️  Prefixed Event Name: $prefixedEventName")

            // Params log karen
            if (params != null && !params.isEmpty) {
                Log.d(TAG, "📦 Event Parameters:")
                for (key in params.keySet()) {
                    Log.d(TAG, "   ➜ $key: ${params.get(key)}")
                }
            } else {
                Log.d(TAG, "📦 No parameters provided")
            }

            // Firebase ko prefixed event bhejen
            if (firebaseAnalytics != null) {
                firebaseAnalytics?.logEvent(prefixedEventName, params)
                Log.d(TAG, "✅ Event logged to Firebase successfully")
            } else {
                Log.w(TAG, "⚠️  FirebaseAnalytics not initialized. Call init() first!")
            }

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error logging event '$eventName': ${e.message}", e)
        }
    }

    /**
     * Check karta hai ke event pehli dafa ho raha hai ya nahi
     */
    private fun isEventFirstTime(eventName: String): Boolean {
        return try {
            val isFirstTime = sharedPreferences?.getBoolean(eventName, true) ?: true
            Log.d(TAG, "🔎 Checking if '$eventName' is first time: $isFirstTime")
            isFirstTime
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking first time for '$eventName': ${e.message}", e)
            true // Default to first time if error
        }
    }

    /**
     * Event ko logged mark karta hai
     */
    private fun markEventAsLogged(eventName: String) {
        try {
            sharedPreferences?.edit()?.putBoolean(eventName, false)?.apply()
            Log.d(TAG, "💾 Event '$eventName' marked as logged")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error marking event '$eventName' as logged: ${e.message}", e)
        }
    }

    /**
     * Kisi specific event ko reset karne ke liye (testing purposes)
     */
    fun resetEvent(eventName: String) {
        try {
            sharedPreferences?.edit()?.remove(eventName)?.apply()
            Log.d(TAG, "🔄 Event '$eventName' reset successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error resetting event '$eventName': ${e.message}", e)
        }
    }

    /**
     * Saari events ko reset karne ke liye (testing purposes)
     */
    fun resetAllEvents() {
        try {
            sharedPreferences?.edit()?.clear()?.apply()
            Log.d(TAG, "🔄 All events reset successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error resetting all events: ${e.message}", e)
        }
    }

    /**
     * Current status check karne ke liye (debugging)
     */
    fun getEventStatus(eventName: String): String {
        return try {
            val isFirstTime = isEventFirstTime(eventName)
            val status = if (isFirstTime) "Not logged yet (First Time)" else "Already logged (Subsequent)"
            Log.d(TAG, "📋 Status of '$eventName': $status")
            status
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting status for '$eventName': ${e.message}", e)
            "Error getting status"
        }
    }

    /**
     * All logged events ki list return karta hai (debugging)
     */
    fun getAllLoggedEvents(): List<String> {
        return try {
            val allKeys = sharedPreferences?.all?.keys?.toList() ?: emptyList()
            Log.d(TAG, "📜 Total logged events: ${allKeys.size}")
            allKeys.forEach { Log.d(TAG, "   ➜ $it") }
            allKeys
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting all logged events: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Analytics initialized hai ya nahi check karta hai
     */
    fun isInitialized(): Boolean {
        val initialized = firebaseAnalytics != null
        Log.d(TAG, "🔍 FirebaseAnalytics initialized: $initialized")
        return initialized
    }
}

