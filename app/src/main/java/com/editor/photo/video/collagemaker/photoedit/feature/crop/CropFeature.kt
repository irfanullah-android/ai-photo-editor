package com.editor.photo.video.collagemaker.photoedit.feature.crop

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.editor.photo.video.collagemaker.photoedit.R
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import java.io.File

class CropFeature(
    caller: ActivityResultCaller,
    private val onCropSuccess: (Uri) -> Unit,
    private val onCropError: (String) -> Unit
) {

    private val cropLauncher =
        caller.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleResult(result)
        }

    fun launch(context: Context, sourceUri: Uri, aspectRatio: Float? = null) {
        val cacheFile = File(context.cacheDir, "cropped_temp_${System.currentTimeMillis()}.jpg")
        val destUri = Uri.fromFile(cacheFile)

        val options = UCrop.Options().apply {
            setToolbarColor(ContextCompat.getColor(context, R.color.black))
            setStatusBarColor(ContextCompat.getColor(context, R.color.black))
            setActiveControlsWidgetColor(ContextCompat.getColor(context, R.color.white))
            setToolbarWidgetColor(ContextCompat.getColor(context, R.color.white))
            setToolbarTitle("Crop Image")
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(95)
            setMaxBitmapSize(10000)
            setShowCropGrid(true)
            setShowCropFrame(true)
            setCropGridStrokeWidth(2)
            setCropGridColor(ContextCompat.getColor(context, R.color.white))
            setCropFrameColor(ContextCompat.getColor(context, R.color.white))
            setHideBottomControls(false)
            setFreeStyleCropEnabled(aspectRatio == null)
            setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL)
            setRootViewBackgroundColor(ContextCompat.getColor(context, R.color.black))
        }

        val uCrop = UCrop.of(sourceUri, destUri).withOptions(options)

        aspectRatio?.let { ratio ->
            if (ratio > 0f) {
                uCrop.withAspectRatio(ratio, 1f)
            }
        }

        cropLauncher.launch(uCrop.getIntent(context))
    }

    private fun handleResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val output = result.data?.let { UCrop.getOutput(it) }
                if (output != null) {
                    onCropSuccess(output)
                } else {
                    onCropError("Crop failed: output is null")
                }
            }

            UCrop.RESULT_ERROR -> {
                val error = result.data?.let { UCrop.getError(it) }
                onCropError(error?.message ?: "Crop failed")
            }

            Activity.RESULT_CANCELED -> {

            }
        }
    }
}