package com.editor.photo.video.collagemaker.photoedit.fragments.childfragments

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.adapters.GalleryDualAdapter
import com.editor.photo.video.collagemaker.photoedit.databinding.FragmentCustomGalleryBinding
import com.editor.photo.video.collagemaker.photoedit.fragments.basefragments.BaseFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CustomGalleryFragment :
    BaseFragment<FragmentCustomGalleryBinding>(R.layout.fragment_custom_gallery) {

    private val photoList = mutableListOf<Uri>()

    private var adapter: GalleryDualAdapter? = null

    private var person1Uri: Uri? = null
    private var person2Uri: Uri? = null
    private var isSingleMode = false
    private var singleUri: Uri? = null

    private var selectedFolderPath: String? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it }) loadAllImages()
        else Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
    }

    override fun onViewCreatedOneTime() {
        isSingleMode = arguments?.getBoolean("isSingleMode", false) ?: false
        WindowCompat.setDecorFitsSystemWindows(requireActivity().window, false)
        requireActivity().window.statusBarColor = Color.TRANSPARENT

        // Update UI based on mode
        if (isSingleMode) {
            // Single mode: hide person 2, update labels
            binding.flPerson2.visibility = View.GONE
            binding.tvSelectLabel.text = "Select 1 photo"
            binding.tvSelectCount.text = "1"
            // Keep confirm button visible for single mode
            binding.btnConfirm.visibility = View.VISIBLE
        } else {
            // Dual mode: show both, confirm button already visible
            binding.flPerson2.visibility = View.VISIBLE
            binding.tvSelectLabel.text = "Select 2 photos"
            binding.tvSelectCount.text = "2"
            binding.btnConfirm.visibility = View.VISIBLE
        }

        setupRecyclerView()
        setupClickListeners()
        checkPermissions()
        showPhotoGuide()
    }

    override fun onViewCreatedEverytime() {

        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Bundle>("folderResult")
            ?.observe(viewLifecycleOwner) { bundle ->

                val name = bundle.getString("selectedFolderName") ?: "All Photos"
                val path = bundle.getString("selectedFolderPath")

                binding.tvAlbumName.text = name
                selectedFolderPath = path

                loadAllImages()
            }
    }

    // ── Setup ─────────────────────────────────────────────────────────

    private fun setupRecyclerView() {

        val galleryAdapter = GalleryDualAdapter(
            mutableListOf(),

            onPhotoClick = { uri ->
                handlePhotoTap(uri)
            },

            onExpandClick = { uri ->
                showExpandedImage(uri)
            }
        )

        adapter = galleryAdapter

        binding.rvPhotoGrid.apply {
            layoutManager = GridLayoutManager(requireContext(), 4)
            adapter = galleryAdapter
            setHasFixedSize(true)

            addItemDecoration(object : RecyclerView.ItemDecoration() {

                private val spanCount = 4
                private val spacing = 5

                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val position = parent.getChildAdapterPosition(view)
                    val column = position % spanCount

                    outRect.left = spacing - column * spacing / spanCount
                    outRect.right = (column + 1) * spacing / spanCount
                    outRect.top = spacing
                    outRect.bottom = spacing
                }
            })
        }
    }

    private fun setupClickListeners() {
        binding.ivClose.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.llAlbumDropdown.setOnClickListener {
            findNavController().navigate(R.id.folderListFragment)
        }

        // Confirm button logic for both modes
        binding.btnConfirm.setOnClickListener {
            if (isSingleMode) {
                // Single mode: check if a photo is selected
                val p1 = person1Uri ?: return@setOnClickListener
                findNavController()
                    .previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(
                        "singleSelectionResult",
                        Bundle().apply {
                            putString("personUri", p1.toString())
                        }
                    )
                requireActivity().onBackPressedDispatcher.onBackPressed()
            } else {
                // Dual mode: check both photos are selected
                val p1 = person1Uri ?: return@setOnClickListener
                val p2 = person2Uri ?: return@setOnClickListener
                findNavController()
                    .previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("dualSelectionResult", Bundle().apply {
                        putString("person1Uri", p1.toString())
                        putString("person2Uri", p2.toString())
                    })
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        binding.ivRemove1.setOnClickListener {
            person1Uri = null
            adapter?.setSelections(person1Uri, person2Uri)
            refreshPreviews()
        }

        binding.ivRemove2.setOnClickListener {
            if (isSingleMode) return@setOnClickListener
            person2Uri = null
            adapter?.setSelections(person1Uri, person2Uri)
            refreshPreviews()
        }
    }

    private fun showPhotoGuide() {
        PhotoGuideBottomSheet
            .newInstance()
            .show(childFragmentManager, "PhotoGuide")
    }

    private fun showExpandedImage(uri: Uri) {
        val dialog = Dialog(requireContext(),
            android.R.style.Theme_Black_NoTitleBar_Fullscreen
        )

        val imageView = ImageView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
        }

        Glide.with(requireContext())
            .load(uri)
            .into(imageView)

        imageView.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(imageView)
        dialog.show()
    }

    // ── Photo tap ─────────────────────────────────────────────────────

    private fun handlePhotoTap(uri: Uri) {
        if (isSingleMode) {
            // Single mode: select/deselect single photo
            if (uri == person1Uri) {
                person1Uri = null // Deselect if already selected
            } else {
                person1Uri = uri // Select new photo
            }
            adapter?.setSelections(person1Uri, null)
            refreshPreviews()
            return
        }

        // Dual mode logic
        when {
            uri == person1Uri -> person1Uri = null
            uri == person2Uri -> person2Uri = null
            person1Uri == null -> person1Uri = uri
            person2Uri == null -> person2Uri = uri
            else -> {
                person1Uri = uri
                person2Uri = null
            }
        }

        adapter?.setSelections(person1Uri, person2Uri)
        refreshPreviews()
    }

    // ── Previews ──────────────────────────────────────────────────────

    private fun refreshPreviews() {
        // Person 1 preview
        if (person1Uri != null) {
            Glide.with(this).load(person1Uri).centerCrop().into(binding.ivPerson1)
            binding.ivRemove1.visibility = View.VISIBLE
        } else {
            binding.ivPerson1.setImageResource(R.drawable.ic_photo_placeholder)
            binding.ivRemove1.visibility = View.GONE
        }

        // Person 2 preview (only in dual mode)
        if (!isSingleMode) {
            if (person2Uri != null) {
                Glide.with(this).load(person2Uri).centerCrop().into(binding.ivPerson2)
                binding.ivRemove2.visibility = View.VISIBLE
            } else {
                binding.ivPerson2.setImageResource(R.drawable.ic_photo_placeholder)
                binding.ivRemove2.visibility = View.GONE
            }

            // Update confirm button state for dual mode
            val bothReady = person1Uri != null && person2Uri != null
            binding.btnConfirm.alpha = if (bothReady) 1f else 0.4f
            binding.btnConfirm.isEnabled = bothReady
        } else {
            // Update confirm button state for single mode
            val hasSelection = person1Uri != null
            binding.btnConfirm.alpha = if (hasSelection) 1f else 0.4f
            binding.btnConfirm.isEnabled = hasSelection

            // Update label text based on selection
            binding.tvSelectLabel.text = if (hasSelection) "Photo selected" else "Select 1 photo"
            binding.tvSelectCount.text = if (hasSelection) "1" else "0"
        }
    }

    // ── Permissions ───────────────────────────────────────────────────

    private fun checkPermissions() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        else
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

        val allGranted = perms.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) loadAllImages()
        else permissionLauncher.launch(perms)
    }

    // ── Media loading ─────────────────────────────────────────────────

    private fun loadAllImages() {
        // Reset selections when folder changes
        person1Uri = null
        person2Uri = null
        adapter?.setSelections(null, null)
        refreshPreviews()

        CoroutineScope(Dispatchers.IO).launch {

            val list = mutableListOf<Uri>()

            try {
                requireContext().contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DATA
                    ),
                    null,
                    null,
                    "${MediaStore.Images.Media.DATE_ADDED} DESC"
                )?.use { c ->

                    val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val dataCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

                    while (c.moveToNext()) {

                        val filePath = c.getString(dataCol) ?: ""
                        val parentPath = File(filePath).parent ?: ""

                        if (selectedFolderPath != null &&
                            selectedFolderPath != parentPath
                        ) continue

                        list.add(
                            Uri.withAppendedPath(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                c.getLong(idCol).toString()
                            )
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e("GalleryDebug", "Error: ${e.message}")
            }

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext

                val rvAdapter = binding.rvPhotoGrid.adapter as? GalleryDualAdapter
                rvAdapter?.updateList(list)
            }
        }
    }
}