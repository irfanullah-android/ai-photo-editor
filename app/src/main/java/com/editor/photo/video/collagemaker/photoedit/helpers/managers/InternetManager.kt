package com.editor.photo.video.collagemaker.photoedit.helpers.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.editor.photo.video.collagemaker.photoedit.firebaseManager.firebaseCrashlytics.FirebaseUtils.recordException

object InternetManager {
    private var connectivityManager: ConnectivityManager? = null

    fun init(context: Context) {
        connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    val isInternetConnected: Boolean
        get() {
            return try {
                val network = connectivityManager?.activeNetwork ?: return false
                val capabilities =
                    connectivityManager?.getNetworkCapabilities(network) ?: return false
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            } catch (ex: Exception) {
                ex.recordException("InternetManager")
                false
            }
        }
}

