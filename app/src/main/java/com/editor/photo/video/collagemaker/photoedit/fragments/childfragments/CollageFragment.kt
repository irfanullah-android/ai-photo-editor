package com.editor.photo.video.collagemaker.photoedit.fragments.childfragments

import android.content.ContentValues
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.transition.Transition
import android.widget.Toast
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.collage_maker.CollageKitConfig
import com.example.collage_maker.CollageKitEditor
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.databinding.FragmentCollageBinding
import com.editor.photo.video.collagemaker.photoedit.fragments.basefragments.BaseFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class CollageFragment : BaseFragment<FragmentCollageBinding>(R.layout.fragment_collage) {

    override fun onViewCreatedOneTime() {
        val source = arguments?.getString("source") ?: "home"

        // Native Transition listener check karega ke kab slide/fade animation complete hoti hai
        val enterTrans = enterTransition as? Transition
        if (enterTrans != null) {
            enterTrans.addListener(object : Transition.TransitionListener {
                override fun onTransitionStart(transition: Transition) {}

                override fun onTransitionEnd(transition: Transition) {
                    if (isAdded && _binding != null) {
                        loadCollageMaker(source)
                    }
                    transition.removeListener(this)
                }

                override fun onTransitionCancel(transition: Transition) {
                    if (isAdded && _binding != null) {
                        loadCollageMaker(source)
                    }
                    transition.removeListener(this)
                }

                override fun onTransitionPause(transition: Transition) {}
                override fun onTransitionResume(transition: Transition) {}
            })
        } else {
            // Agar koi transition animation nahi chal rahi, toh direct load karein (No delay)
            loadCollageMaker(source)
        }
    }

    override fun onViewCreatedEverytime() {}

    private fun loadCollageMaker(source: String) {
        // XML se direct ComposeView binding ke zariye access karein
        binding.composeView.apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )

            setContent {
                CollageKitEditor(
                    config = CollageKitConfig(showPickerInitially = true),
                    onResult = { result ->
                        lifecycleScope.launch {
                            val success = saveToGallery(result.outputBitmap)
                            if (success && isAdded && context != null) {
                                Toast.makeText(
                                    requireContext(),
                                    "Collage saved to Gallery",
                                    Toast.LENGTH_SHORT
                                ).show()
                                navigateBack(source)
                            }
                        }
                    },
                    onCancel = {
                        if (isAdded) {
                            navigateBack(source)
                        }
                    }
                )
            }
        }
    }

    private fun navigateBack(source: String) {
        if (!isAdded) return
        when (source) {
            "home" -> findNavController().popBackStack()
            "select" -> findNavController().popBackStack()
            else -> findNavController().popBackStack()
        }
    }

    private suspend fun saveToGallery(bitmap: Bitmap): Boolean {
        val appContext = context?.applicationContext ?: return false
        val filename = "Collage_${System.currentTimeMillis()}.jpg"

        return withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/PhotoFix"
                        )
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }

                    val uri = appContext.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )

                    if (uri == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                appContext,
                                "Failed to create file",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@withContext false
                    }

                    appContext.contentResolver.openOutputStream(uri)?.use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    appContext.contentResolver.update(uri, contentValues, null, null)

                } else {
                    val picturesDir = File(
                        Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES
                        ),
                        "PhotoFix"
                    )
                    picturesDir.mkdirs()

                    val file = File(picturesDir, filename)
                    FileOutputStream(file).use { stream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                    }

                    MediaScannerConnection.scanFile(
                        appContext,
                        arrayOf(file.absolutePath),
                        arrayOf("image/jpeg"),
                        null
                    )
                }
                true
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        appContext,
                        "Failed to save: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                false
            }
        }
    }
}