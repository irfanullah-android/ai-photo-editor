package com.editor.photo.video.collagemaker.photoedit.utlis

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.editor.photo.video.collagemaker.photoedit.R
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.editor.photo.video.collagemaker.photoedit.databinding.CustomPermissionDialogBinding
import com.editor.photo.video.collagemaker.photoedit.utlis.DebounceListener.setDebounceClickListener

object CommonData {

    private const val PREFS_NAME = "permissions_prefs"




    /** Request Storage / Media Permissions **/
    fun requestMediaPermissionsWithDexter(
        context: Context,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // ✅ Android 13+ — sirf images permission
            Dexter.withContext(context)
                .withPermission(Manifest.permission.READ_MEDIA_IMAGES)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {
                        onGranted()
                    }
                    override fun onPermissionDenied(response: PermissionDeniedResponse) {
                        if (response.isPermanentlyDenied) showPermissionSettingsDialog(context, "media")
                        else onDenied()
                    }
                    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest, token: PermissionToken) {
                        token.continuePermissionRequest()
                    }
                }).check()
        } else {
            // ✅ Android 12 aur below
            Dexter.withContext(context)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {
                        onGranted()
                    }
                    override fun onPermissionDenied(response: PermissionDeniedResponse) {
                        if (response.isPermanentlyDenied) showPermissionSettingsDialog(context, "media")
                        else onDenied()
                    }
                    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest, token: PermissionToken) {
                        token.continuePermissionRequest()
                    }
                }).check()
        }
    }

    /** Storage Permission Check — sirf images **/
    fun isStoragePermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }


    /** Camera Permission **/
    fun isCameraPermissionGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    fun requestCameraPermissionWithDexter(
        context: Context,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        Dexter.withContext(context)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) = onGranted()
                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    if (response.isPermanentlyDenied) showPermissionSettingsDialog(context, "camera")
                    else onDenied()
                }

                override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest, token: PermissionToken) {
                    token.continuePermissionRequest()
                }
            }).check()
    }

    /** Notification Permission (Android 13+) **/
    fun isNotificationPermissionGranted(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        else true

    fun requestNotificationPermissionWithDexter(
        context: Context,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) { onGranted(); return }
        Dexter.withContext(context)
            .withPermission(Manifest.permission.POST_NOTIFICATIONS)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse) = onGranted()
                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    if (response.isPermanentlyDenied) showPermissionSettingsDialog(context, "notification")
                    else onDenied()
                }

                override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest, token: PermissionToken) {
                    token.continuePermissionRequest()
                }
            }).check()
    }

    /** Check if all required permissions granted **/
    fun isAllPermissionsGranted(context: Context): Boolean {
        val camera = isCameraPermissionGranted(context)
        val storage = isStoragePermissionGranted(context)
        val notification = isNotificationPermissionGranted(context)
        return camera && storage && notification
    }

    /** Permission Settings Dialog **/
    fun showPermissionSettingsDialog(context: Context, permissionType: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "${permissionType}_dialog_shown"
        if (prefs.getBoolean(key, false)) return  // Prevent multiple dialogs
        prefs.edit().putBoolean(key, true).apply()

        val binding = CustomPermissionDialogBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .setCancelable(false)
            .create()

        when (permissionType) {
            "camera" -> {
                binding.dialogNickName.text = context.getString(R.string.require_camera_permission)
                binding.dialogUserId.text = context.getString(R.string.camera_permission_description)
                binding.lottieTitle.setAnimation(R.raw.camera_permission_lottie)
            }
            "notification" -> {
                binding.dialogNickName.text = context.getString(R.string.require_notification_permission)
                binding.dialogUserId.text = context.getString(R.string.notification_permission_description)
                binding.lottieTitle.setAnimation(R.raw.notification_permission_lottie)
            }
            "media" -> {
                binding.dialogNickName.text = context.getString(R.string.require_storage_permission)
                binding.dialogUserId.text = context.getString(R.string.storage_permission_description)
                binding.lottieTitle.setAnimation(R.raw.notification_permission_lottie)
            }
        }

        binding.dialogCancel.setDebounceClickListener { dialog.dismiss() }
        binding.dialogSettingButton.setDebounceClickListener {
            dialog.dismiss()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", context.packageName, null)
            context.startActivity(intent)
        }

        dialog.window?.setBackgroundDrawableResource(R.color.light_gray)
        dialog.show()
    }

    /** Navigate to Activity **/
    fun navigateToActivity(context: Context, targetActivity: Class<*>) {
        val intent = Intent(context, targetActivity)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    /** Switch UI helper **/
    fun updateSwitchUI(switch: SwitchCompat, isChecked: Boolean) {
        switch.thumbTintList = ColorStateList.valueOf(Color.parseColor("#19B593"))
        switch.trackTintList = ColorStateList.valueOf(
            if (isChecked) Color.parseColor("#19B593") else Color.parseColor("#757575")
        )
    }

    fun debugLog(tag: Any, message: String) {
        Log.d(tag.toString(), message)
    }


    fun isInternetConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}
