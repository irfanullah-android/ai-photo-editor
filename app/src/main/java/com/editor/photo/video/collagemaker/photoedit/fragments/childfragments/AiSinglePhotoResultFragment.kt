package com.editor.photo.video.collagemaker.photoedit.fragments.childfragments

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.databinding.FragmentAiGroupPhotoResultBinding
import com.editor.photo.video.collagemaker.photoedit.fragments.basefragments.BaseFragment
import com.editor.photo.video.collagemaker.photoedit.viewmodel.AiGenerationViewModel
import java.io.File

class AiSinglePhotoResultFragment :
    BaseFragment<FragmentAiGroupPhotoResultBinding>(R.layout.fragment_ai_group_photo_result) {

    private val TAG = "SingleResultFragment"
    private val viewModel: AiGenerationViewModel by activityViewModels()

    override fun onViewCreatedOneTime() {
        binding.btnSave.setOnClickListener {
            val bitmap = viewModel.generatedBitmap.value
            if (bitmap != null && !bitmap.isRecycled) {
                saveToGallery(bitmap)
            } else {
                Log.w(TAG, "Save clicked but bitmap is null or recycled")
                Toast.makeText(requireContext(), "No image to save", Toast.LENGTH_SHORT).show()
            }
        }

        setupShareButtons()

        val backAction = {
            Log.d(TAG, "Back action — clearing result and navigating to single photo")
            viewModel.clearResult()
            requireActivity().findNavController(R.id.fcv_container_main)
                .popBackStack(R.id.aiSinglePhotoFragment, false)
        }
        binding.ivBack.setOnClickListener { backAction() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            backAction()
        }
    }

    override fun onViewCreatedEverytime() {
        Log.d(TAG, "onViewCreatedEverytime — current bitmap: ${
            viewModel.generatedBitmap.value?.let { "${it.width}x${it.height}" } ?: "null"
        }")

        viewModel.generatedBitmap.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null && !bitmap.isRecycled) {
                Log.d(TAG, "Bitmap set — ${bitmap.width}x${bitmap.height}")
                binding.ivGeneratedPhoto.setImageBitmap(bitmap)
            }
            // null pe kuch nahi karte — purani image rehne do
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private fun saveToGallery(bitmap: Bitmap) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "AI_Single_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoFix")
            }
            val uri = requireContext().contentResolver
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: run {
                    Log.e(TAG, "MediaStore insert returned null")
                    Toast.makeText(requireContext(), "Save failed", Toast.LENGTH_SHORT).show()
                    return
                }
            requireContext().contentResolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }
            Log.d(TAG, "Saved to gallery: $uri")
            Toast.makeText(requireContext(), "Saved to gallery!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "saveToGallery failed", e)
            Toast.makeText(requireContext(), "Save failed", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Share ─────────────────────────────────────────────────────────────────

    private fun saveTempAndShare(bitmap: Bitmap, packageName: String? = null) {
        try {
            val cacheFile = File(requireContext().cacheDir, "share_temp.jpg")
            cacheFile.outputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
            }
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                cacheFile
            )
            Log.d(TAG, "Sharing — package=${packageName ?: "chooser"}, uri=$uri")
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (packageName != null) setPackage(packageName)
            }
            try {
                startActivity(
                    if (packageName != null) intent
                    else Intent.createChooser(intent, "Share via")
                )
            } catch (e: Exception) {
                Log.w(TAG, "Direct share failed, falling back to chooser", e)
                startActivity(Intent.createChooser(intent, "Share via"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveTempAndShare failed", e)
        }
    }

    private fun setupShareButtons() {
        binding.llInstagram.setOnClickListener { shareToApp("com.instagram.android") }
        binding.llFacebook.setOnClickListener  { shareToApp("com.facebook.katana") }
        binding.llTiktok.setOnClickListener    { shareToApp("com.zhiliaoapp.musically") }
        binding.llWechat.setOnClickListener    { shareToApp("com.tencent.mm") }
        binding.llShare.setOnClickListener     { shareGeneral() }
        binding.llMore.setOnClickListener      { shareGeneral() }
    }

    private fun shareToApp(packageName: String) {
        val bitmap = viewModel.generatedBitmap.value ?: run {
            Log.w(TAG, "shareToApp called but bitmap is null")
            return
        }
        saveTempAndShare(bitmap, packageName)
    }

    private fun shareGeneral() {
        val bitmap = viewModel.generatedBitmap.value ?: run {
            Log.w(TAG, "shareGeneral called but bitmap is null")
            return
        }
        saveTempAndShare(bitmap, null)
    }
}