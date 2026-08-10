package com.editor.photo.video.collagemaker.photoedit.fragments.childfragments

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.editor.photo.video.collagemaker.photoedit.adapters.galleryadpater.FolderAdapter
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.databinding.FragmentFolderListBinding
import com.editor.photo.video.collagemaker.photoedit.fragments.basefragments.BaseFragment
import com.editor.photo.video.collagemaker.photoedit.models.gallery.FolderModel
import java.io.File

class FolderListFragment : BaseFragment<FragmentFolderListBinding>(R.layout.fragment_folder_list) {

    private lateinit var folderAdapter: FolderAdapter

    override fun onViewCreatedOneTime() {
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        requireActivity().window.statusBarColor = Color.TRANSPARENT
        setupAdapter()
        setupClickListeners()
        loadFolders()
    }

    override fun onViewCreatedEverytime() {}

    // ── Adapter setup ─────────────────────────────────────────────────────
    private fun setupAdapter() {
        folderAdapter = FolderAdapter(emptyList()) { folder ->

            val bundle = Bundle().apply {
                putString("selectedFolderName", folder.name)
                putString("selectedFolderPath", folder.path)
                putString("selectedFolderThumb", folder.thumbnailUri?.toString() ?: "")
                putInt("selectedFolderCount", folder.photoCount)
            }

            findNavController().previousBackStackEntry
                ?.savedStateHandle
                ?.set("folderResult", bundle)
            findNavController().popBackStack()
        }

        binding.rvFolderGrid.apply {
            layoutManager = GridLayoutManager(requireContext(), 1)
            adapter = folderAdapter
            setHasFixedSize(true)
        }
    }

    // ── Click listeners ───────────────────────────────────────────────────
    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    // ── Load all folders ──────────────────────────────────────────────────
    private fun loadFolders() {
        folderAdapter.updateList(getAllFolders())
    }

    // ── ALL FOLDERS from MediaStore ───────────────────────────────────────
    private fun getAllFolders(): List<FolderModel> {
        data class FolderData(
            val uris: MutableList<Uri> = mutableListOf(),
            var path: String = ""
        )
        val map = mutableMapOf<String, FolderData>()

        try {
            requireContext().contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.Media.DATA
                ),
                null, null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { c ->
                val idCol     = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val bucketCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val dataCol   = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

                while (c.moveToNext()) {
                    val name       = c.getString(bucketCol) ?: "Unknown"
                    val uri        = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        c.getLong(idCol).toString()
                    )
                    val filePath   = c.getString(dataCol) ?: ""
                    val folderPath = File(filePath).parent ?: ""

                    val data = map.getOrPut(name) { FolderData() }
                    data.uris.add(uri)
                    if (data.path.isEmpty()) data.path = folderPath
                }
            }
        } catch (e: Exception) {
            Log.e("FolderListFragment", "getAllFolders: ${e.message}")
        }

        return map.entries.sortedBy { it.key }
            .map { (name, data) ->
                FolderModel(name, data.uris.size, data.uris.firstOrNull(), data.path)
            }
    }
}