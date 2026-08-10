package com.editor.photo.video.collagemaker.photoedit.fragments.childfragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.adapters.galleryadpater.PhotoGridAdapter
import com.editor.photo.video.collagemaker.photoedit.databinding.FragmentSelectImageBinding
import com.editor.photo.video.collagemaker.photoedit.fragments.basefragments.BaseFragment
import java.io.File

class SelectImageFragment :
    BaseFragment<FragmentSelectImageBinding>(R.layout.fragment_select_image) {

    private var currentGridSpanCount = 4
    private val photoList = mutableListOf<Uri>()
    private lateinit var photoAdapter: PhotoGridAdapter
    private var currentTab = "all"

    private var cameraImageUri: Uri? = null


    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { uri ->
                try {
                    navigateToPhotoStudio(uri)
                } catch (e: Exception) {
                    Log.e("SelectImage", "Navigation error after camera: ${e.message}", e)
                }
            }
        } else {
            // Cancel — temp file delete karo
            try {
                cameraImageUri?.path?.let { File(it).delete() }
            } catch (_: Exception) {
            }
            cameraImageUri = null
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            loadMediaByTab(currentTab)
        } else {
            Toast.makeText(requireContext(), "Permission denied.", Toast.LENGTH_LONG).show()
        }
    }

    // ✅ Camera permission launcher (alag — sirf camera ke liye)
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission required.", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onViewCreatedOneTime() {
        requireActivity().window.statusBarColor = Color.TRANSPARENT

        setupPhotoAdapter()
        setupTabListeners()
        setupClickListeners()
        observeFolderResult()
        checkPermissions()
    }

    override fun onViewCreatedEverytime() {}

    private fun setupPhotoAdapter() {
        photoAdapter = PhotoGridAdapter(photoList.toList()) { uri, _ ->
            navigateToPhotoStudio(uri)
        }
        binding.rvPhotoGrid.apply {
            layoutManager = GridLayoutManager(requireContext(), currentGridSpanCount)
            adapter = photoAdapter
            setHasFixedSize(true)
        }
        updateGridIcon()
    }

    private fun observeFolderResult() {
        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Bundle>("folderResult")
            ?.observe(viewLifecycleOwner) { bundle ->
                val folderName = bundle.getString("selectedFolderName") ?: return@observe
                binding.tvTitle.text = folderName
                loadPhotosFromFolder(folderName)
            }
    }

    private fun setupTabListeners() {
        binding.tabAll.setOnClickListener { selectTab("all") }
        binding.tabImages.setOnClickListener { selectTab("images") }
        binding.tabCollage.setOnClickListener { selectTab("collage") }
        binding.tabCamera.setOnClickListener { checkCameraPermissionAndOpen() } // ✅ updated
    }

    private fun selectTab(tab: String) {
        binding.tabAll.setBackgroundResource(R.drawable.bg_tab_unselected)
        binding.tabImages.setBackgroundResource(R.drawable.bg_tab_unselected)
        binding.tabCollage.setBackgroundResource(R.drawable.bg_tab_unselected)

        when (tab) {
            "all" -> {
                currentTab = tab
                binding.tabAll.setBackgroundResource(R.drawable.bg_tab_selected)
                binding.tvTitle.text = "All Photos"
            }

            "images" -> {
                currentTab = tab
                binding.tabImages.setBackgroundResource(R.drawable.bg_tab_selected)
                binding.tvTitle.text = "Camera Roll"
            }

            "collage" -> {
                currentTab = tab
                binding.tabCollage.setBackgroundResource(R.drawable.bg_tab_selected)
                binding.tvTitle.text = "Collage"
            }
        }

        loadMediaByTab(tab)
    }

    private fun loadMediaByTab(tab: String) {
        when (tab) {
            "all" -> loadAllImages()
            "images" -> loadCameraImages()
            "collage" -> {
                currentTab = "all"
                binding.tabAll.setBackgroundResource(R.drawable.bg_tab_selected)
                binding.tabCollage.setBackgroundResource(R.drawable.bg_tab_unselected)
                binding.tvTitle.text = "All Photos"

                val bundle = Bundle().apply { putString("source", "select") }
                findNavController().navigate(
                    R.id.action_selectImageFragment_to_collageFragment,
                    bundle
                )
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.btnMenu.setOnClickListener {
            findNavController().navigate(R.id.action_selectImageFragment_to_folderListFragment)
        }
        binding.btnGridLayout.setOnClickListener { toggleGridLayout() }
    }

    private fun toggleGridLayout() {
        currentGridSpanCount = if (currentGridSpanCount == 4) 2 else 4
        (binding.rvPhotoGrid.layoutManager as? GridLayoutManager)?.spanCount = currentGridSpanCount
        updateGridIcon()
    }

    private fun updateGridIcon() {
        binding.btnGridLayout.setImageResource(
            if (currentGridSpanCount == 4) R.drawable.ic_gridview else R.drawable.ic_photo
        )
    }

    private fun checkPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(
                requireContext(),
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) loadMediaByTab(currentTab)
        else permissionLauncher.launch(permissions)
    }

    // ✅ Camera permission check
    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // ✅ FileProvider camera — gallery save NAHI
    private fun openCamera() {
        try {
            val picturesDir =
                if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                    requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                } else {
                    requireContext().filesDir
                }

            if (picturesDir == null) {
                Toast.makeText(requireContext(), "Storage not available.", Toast.LENGTH_SHORT)
                    .show()
                return
            }

            if (!picturesDir.exists()) picturesDir.mkdirs()

            val imageFile = File(picturesDir, "temp_${System.currentTimeMillis()}.jpg")

            cameraImageUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                imageFile
            )

            cameraLauncher.launch(cameraImageUri)

        } catch (e: Exception) {
            Log.e("SelectImage", "openCamera error: ${e.message}", e)
            Toast.makeText(requireContext(), "Camera open failed.", Toast.LENGTH_SHORT).show()
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // MEDIA LOADING
    // ════════════════════════════════════════════════════════════════════

    private fun loadAllImages() {
        val list = mutableListOf<Uri>()
        try {
            requireContext().contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED),
                "${MediaStore.Images.Media.SIZE} > ?",
                arrayOf("1000"),
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (c.moveToNext()) {
                    list.add(
                        Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            c.getLong(idCol).toString()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("SelectImage", "loadAllImages: ${e.message}")
        }
        photoList.clear()
        photoList.addAll(list)
        photoAdapter.updateList(photoList.toList())
    }

    private fun loadCameraImages() {
        val list = mutableListOf<Uri>()
        try {
            requireContext().contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED),
                "${MediaStore.Images.Media.SIZE} > ? AND (" +
                        "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ? OR " +
                        "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ? OR " +
                        "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ?)",
                arrayOf("1000", "Camera", "DCIM", "100ANDRO"),
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (c.moveToNext()) {
                    list.add(
                        Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            c.getLong(idCol).toString()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("SelectImage", "loadCameraImages: ${e.message}")
        }
        photoList.clear()
        photoList.addAll(list)
        photoAdapter.updateList(photoList.toList())
    }

    private fun loadPhotosFromFolder(folderName: String) {
        val list = mutableListOf<Uri>()
        try {
            requireContext().contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                "${MediaStore.Images.Media.BUCKET_DISPLAY_NAME} = ? AND " +
                        "${MediaStore.Images.Media.SIZE} > ?",
                arrayOf(folderName, "1000"),
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { c ->
                val col = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (c.moveToNext()) {
                    list.add(
                        Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            c.getLong(col).toString()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("SelectImage", "loadPhotosFromFolder: ${e.message}")
        }
        photoList.clear()
        photoList.addAll(list)
        photoAdapter.updateList(photoList.toList())
    }

    private fun navigateToPhotoStudio(uri: Uri) {
        val bundle = Bundle().apply {
            putString("imageUri", uri.toString())
            arguments?.getString("selectedTool")?.let { putString("selectedTool", it) }
        }
        findNavController().navigate(R.id.photoStudioFragment, bundle)
    }
}