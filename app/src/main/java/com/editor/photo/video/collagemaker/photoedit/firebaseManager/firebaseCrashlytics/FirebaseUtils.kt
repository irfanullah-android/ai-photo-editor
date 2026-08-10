package com.editor.photo.video.collagemaker.photoedit.firebaseManager.firebaseCrashlytics

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

object FirebaseUtils {
    private const val TAG_FIREBASE = "firebase_tag"

    fun Throwable.recordException(log: String) {
        try {
            FirebaseCrashlytics.getInstance().log(log)
            FirebaseCrashlytics.getInstance().recordException(this)
            Log.e(TAG_FIREBASE, "recordException: ${this.message.toString()}")
        } catch (e: Exception) {
            Log.e(TAG_FIREBASE, "recordException: ${this.message.toString()}")
        }
    }
}