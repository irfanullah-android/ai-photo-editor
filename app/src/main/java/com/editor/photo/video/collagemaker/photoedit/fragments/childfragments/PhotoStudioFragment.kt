package com.editor.photo.video.collagemaker.photoedit.fragments.childfragments

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.ScaleGestureDetector
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.adapters.galleryadpater.EditorToolsAdapter
import com.editor.photo.video.collagemaker.photoedit.databinding.FragmentPhotoStudioBinding
import com.editor.photo.video.collagemaker.photoedit.fragments.basefragments.BaseFragment
import com.editor.photo.video.collagemaker.photoedit.editor.session.EditorSessionViewModel
import com.editor.photo.video.collagemaker.photoedit.feature.adjust.AdjustBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.canvas.CanvasBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.effect.EffectBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.enhance.EnhanceBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.filter.FilterBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.frame.FrameBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.remove.RemoveBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.rotate.RotateBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.sticker.StickerBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.text.TextBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.doodle.DoodleBottomSheet
import com.editor.photo.video.collagemaker.photoedit.helpers.FullscreenHelper
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorUiState
import com.editor.photo.video.collagemaker.photoedit.models.gallery.EditorItemModel
import com.editor.photo.video.collagemaker.photoedit.models.gallery.EditorTool
// Unused imports removed
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.PhotoEditorView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class PhotoStudioFragment :
    BaseFragment<FragmentPhotoStudioBinding>(R.layout.fragment_photo_studio) {

    companion object {
        private const val TAG = "PhotoStudioFragment"
        private const val MIN_SCALE = 0.5f
        private const val MAX_SCALE = 3.0f

        fun newInstance(imageUri: String) = PhotoStudioFragment().apply {
            arguments = Bundle().apply { putString("imageUri", imageUri) }
        }
    }

    private val viewModel: EditorSessionViewModel by activityViewModels()
    private lateinit var fullscreenHelper: FullscreenHelper
    private lateinit var toolsAdapter: EditorToolsAdapter

    private lateinit var photoEditor: PhotoEditor
    private lateinit var photoEditorView: PhotoEditorView


    private var isFullscreen = false
    private var isComparingOriginal = false
    private var currentScale = 1f

    private lateinit var scaleDetector: ScaleGestureDetector

    private var progressDialog: Dialog? = null

    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleCropResult(result)
        }



    override fun onViewCreatedOneTime() {
        setupPhotoEditorView()
        setupFullscreenHelper()
        setupToolsRecyclerView()
        setupClickListeners()
        setupGestureDetectors()
        setupBackPressHandler()
        observeUiState()
        observeActiveTool()
        loadInitialImage()
        openToolFromArguments()
    }

    override fun onViewCreatedEverytime() {}

    override fun onResume() {
        super.onResume()
        if (isFullscreen) fullscreenHelper.hideSystemUI()
    }

    override fun onPause() {
        super.onPause()
        if (isFullscreen) fullscreenHelper.showSystemUI()
    }

    override fun onDestroyView() {
        cleanup()
        super.onDestroyView()
    }


    private fun loadInitialImage() {
        val uriString = arguments?.getString("imageUri") ?: return
        val uri = Uri.parse(uriString)
        viewModel.loadImage(uri)
    }

    private fun setupPhotoEditorView() {
        photoEditorView = binding.photoEditorView
        photoEditor = PhotoEditor.Builder(requireContext(), photoEditorView)
            .setPinchTextScalable(true)
            .build()
        photoEditorView.source.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        photoEditorView.source.adjustViewBounds = true
    }

    private fun setupFullscreenHelper() {
        fullscreenHelper = FullscreenHelper(requireActivity(), binding)
        fullscreenHelper.setupSystemUIListener { isFullscreen }
    }

    private fun setupToolsRecyclerView() {
        val tools = listOf(
            EditorItemModel("Canvas",  R.drawable.ic_canvas),
            EditorItemModel("Filter",  R.drawable.ic_filter),
            EditorItemModel("Adjust",  R.drawable.ic_adjust),
            EditorItemModel("Effect",  R.drawable.ic_effect),
            EditorItemModel("Sticker", R.drawable.ic_sticker),
            EditorItemModel("Text",    R.drawable.ic_text),
            EditorItemModel("Remove",  R.drawable.ic_bg_remove),
            EditorItemModel("Enhance", R.drawable.ic_enhance),
            EditorItemModel("Doodle",  R.drawable.ic_doodle),
            EditorItemModel("Crop",    R.drawable.ic_crop),
            EditorItemModel("Frame",   R.drawable.ic_frame),
            EditorItemModel("Rotate",  R.drawable.ic_rotate)
        )
        toolsAdapter = EditorToolsAdapter(tools) { tool ->
            val editorTool = EditorTool.fromName(tool.name)
            viewModel.setActiveTool(editorTool)
            openToolBottomSheet(editorTool)
        }
        binding.rvEditorTools.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = toolsAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            btnBack.setOnClickListener    { findNavController().popBackStack() }
            btnUndo.setOnClickListener    { viewModel.undo() }
            btnRedo.setOnClickListener    { viewModel.redo() }
            btnExport.setOnClickListener  { viewModel.exportImage("PhotoFix_" + System.currentTimeMillis() + ".jpg") }
            btnCompare.setOnClickListener { toggleCompare() }
            btnFullscreen.setOnClickListener { toggleFullscreen() }
        }
    }

    private fun setupGestureDetectors() {
        scaleDetector = ScaleGestureDetector(requireContext(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    currentScale = (currentScale * detector.scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
                    viewModel.setCanvasZoom(currentScale)
                    applyZoomTransform(currentScale)
                    return true
                }
            })

        binding.photoEditorView.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)
            true
        }
    }

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isFullscreen) {
                        exitFullscreen()
                    } else {
                        isEnabled = false
                        @Suppress("DEPRECATION")
                        requireActivity().onBackPressed()
                    }
                }
            })
    }

    private fun openToolFromArguments() {
        val toolName = arguments?.getString("selectedTool") ?: return
        binding.photoEditorView.post {
            if (_binding == null) return@post
            openToolBottomSheet(EditorTool.fromName(toolName))
        }
    }



    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is EditorUiState.Idle       -> {  }
                    is EditorUiState.Loading    -> showLoading(true)

                    is EditorUiState.Processing -> {
                        showLoading(false)
                        showExportProgressDialog(state.message)
                    }

                    is EditorUiState.Editing -> {
                        dismissExportProgressDialog()
                        renderEditingState(state)
                    }

                    is EditorUiState.ExportSuccess -> {
                        dismissExportProgressDialog()
                        handleExportSuccess(state.uri)
                    }

                    is EditorUiState.Error -> {
                        dismissExportProgressDialog()
                        showLoading(false)
                        showToast(state.message)
                    }
                }
            }
        }
    }

    private fun observeActiveTool() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeTool.collectLatest { tool ->
                toolsAdapter.setSelectedTool(tool)
            }
        }
    }


    private fun renderEditingState(state: EditorUiState.Editing) {
        showLoading(false)

        state.previewBitmap?.let { bitmap ->
            if (!isComparingOriginal) {
                photoEditorView.source.setImageBitmap(bitmap)
            }
        }


        binding.btnUndo.apply {
            isEnabled = state.canUndo
            alpha = if (isEnabled) 1f else 0.5f
        }
        binding.btnRedo.apply {
            isEnabled = state.canRedo
            alpha = if (isEnabled) 1f else 0.5f
        }

        applyCurrentTransforms()
    }

    private fun handleExportSuccess(uri: Uri) {
        showLoading(false)
        showExportSuccessDialog(uri)
    }


    private fun syncDisplayedBitmapToViewModel() {
        val currentBitmap = (photoEditorView.source.drawable
                as? android.graphics.drawable.BitmapDrawable)?.bitmap
            ?: return
        viewModel.syncPreviewBitmap(currentBitmap)
    }


    private fun openToolBottomSheet(tool: EditorTool) {
        when (tool) {
            EditorTool.CANVAS   -> showCanvasBottomSheet()
            EditorTool.FILTER   -> showFilterBottomSheet()
            EditorTool.ADJUST   -> showAdjustBottomSheet()
            EditorTool.EFFECT   -> showEffectBottomSheet()
            EditorTool.REMOVE   -> showRemoveBottomSheet()
            EditorTool.DOODLE   -> showDoodleBottomSheet()
            EditorTool.ROTATE   -> showRotateBottomSheet()
            EditorTool.CROP     -> openCropScreen(null)
            EditorTool.TEXT     -> showTextBottomSheet()
            EditorTool.STICKER  -> showStickerBottomSheet()
            EditorTool.FRAME    -> showFrameBottomSheet()
            EditorTool.ENHANCE  -> showEnhanceBottomSheet()
            else                -> showToast("$tool coming soon")
        }
    }

    private fun showCanvasBottomSheet() {
        CanvasBottomSheet.newInstance(photoEditorView) { aspectRatio ->
            viewModel.setAspectRatio(aspectRatio)
            openCropScreen(aspectRatio)
        }.show(childFragmentManager, "canvas")
    }

    private fun showFilterBottomSheet() {
        val currentBitmap = getCurrentPreviewBitmapSafe() ?: return
        val currentUri = viewModel.editorState.value.baseImageUri?.let { Uri.parse(it) }

        FilterBottomSheet.newInstance(
            imageUri = currentUri,
            sourceBitmap = currentBitmap,
            imageView = photoEditorView.source,
            onDismissed = { syncDisplayedBitmapToViewModel() }
        ).show(childFragmentManager, "filter")
    }

    private fun showAdjustBottomSheet() {
        val currentBitmap = getCurrentPreviewBitmapSafe() ?: return
        val currentUri = viewModel.editorState.value.baseImageUri?.let { Uri.parse(it) }

        AdjustBottomSheet.newInstance(currentUri, photoEditorView.source, currentBitmap) { adjusted ->
            adjusted?.let {
                viewModel.syncPreviewBitmap(it)
                showToast("Adjustments applied")
            }
            syncDisplayedBitmapToViewModel()
        }.show(childFragmentManager, "adjust")
    }

    private fun showEffectBottomSheet() {
        val currentBitmap = getCurrentPreviewBitmapSafe() ?: return
        val currentUri = viewModel.editorState.value.baseImageUri?.let { Uri.parse(it) }

        EffectBottomSheet.newInstance(
            imageUri = currentUri,
            sourceBitmap = currentBitmap,
            imageView = photoEditorView.source,
            onEffectApplied = {}
        ).show(childFragmentManager, "effect")
    }

    private fun showEnhanceBottomSheet() {
        val currentBitmap = getCurrentPreviewBitmapSafe() ?: return
        val currentUri = viewModel.editorState.value.baseImageUri?.let { Uri.parse(it) }

        EnhanceBottomSheet.newInstance(
            imageUri = currentUri,
            sourceBitmap = currentBitmap,
            imageView = photoEditorView.source,
            onDismissed = { syncDisplayedBitmapToViewModel() }
        ).show(childFragmentManager, "enhance")
    }

    private fun showFrameBottomSheet() {
        viewLifecycleOwner.lifecycleScope.launch {
            showLoading(true)
            val baseBitmap = viewModel.getHighResBitmapWithoutFrame()
            showLoading(false)

            if (baseBitmap != null) {
                FrameBottomSheet.newInstance(
                    baseBitmap = baseBitmap,
                    photoEditorView = photoEditorView,
                    onFrameApplied = {}
                ).show(childFragmentManager, "frame")
            } else {
                showToast("Failed to prepare image for framing")
            }
        }
    }

    private fun showDoodleBottomSheet() {
        photoEditor.setBrushDrawingMode(true)
        photoEditor.brushColor = Color.RED
        photoEditor.brushSize = 50f

        DoodleBottomSheet.newInstance(
            photoEditorView = photoEditorView,
            photoEditor = photoEditor,
            onDoodleApplied = {
                photoEditor.setBrushDrawingMode(false)
                showToast("Doodle applied")
            },
            onStrokeCompleted = { doodlePath ->
                // Each completed stroke becomes one entry in the app's own undo/redo history,
                // independent of the PhotoEditor library's transient drawing surface.
                viewModel.addDoodle(doodlePath)
            }
        ).show(childFragmentManager, "doodle")
    }

    private fun showRotateBottomSheet() {
        RotateBottomSheet.newInstance()
            .show(childFragmentManager, "rotate")
    }

    private fun showTextBottomSheet() {
        TextBottomSheet.newInstance()
            .show(childFragmentManager, "text")
    }

    private fun showStickerBottomSheet() {
        StickerBottomSheet.newInstance()
            .show(childFragmentManager, "sticker")
    }

    private fun showRemoveBottomSheet() {
        RemoveBottomSheet.newInstance().show(childFragmentManager, "remove")
    }


    private fun openCropScreen(aspectRatio: Float?) {
        val uriString = viewModel.editorState.value.baseImageUri ?: run {
            showToast("No image to crop"); return
        }
        val sourceUri = Uri.parse(uriString)
        val destUri = Uri.fromFile(
            File(requireContext().cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        )

        val options = UCrop.Options().apply {
            setToolbarColor(ContextCompat.getColor(requireContext(), R.color.black))
            setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.black))
            setActiveControlsWidgetColor(ContextCompat.getColor(requireContext(), R.color.white))
            setToolbarWidgetColor(ContextCompat.getColor(requireContext(), R.color.white))
            setToolbarTitle("Crop Image")
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(95)
            setMaxBitmapSize(10000)
            setShowCropGrid(true)
            setShowCropFrame(true)
            setCropGridStrokeWidth(2)
            setCropGridColor(ContextCompat.getColor(requireContext(), R.color.white))
            setCropFrameColor(ContextCompat.getColor(requireContext(), R.color.white))
            setHideBottomControls(false)
            setFreeStyleCropEnabled(true)
            setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL)
            setRootViewBackgroundColor(ContextCompat.getColor(requireContext(), R.color.black))
        }

        val uCrop = UCrop.of(sourceUri, destUri).withOptions(options)
        aspectRatio?.let { ratio ->
            when {
                ratio == 1f -> uCrop.withAspectRatio(1f, 1f)
                ratio < 1f  -> uCrop.withAspectRatio(1f, 1f / ratio)
                else        -> uCrop.withAspectRatio(ratio, 1f)
            }
        }
        cropLauncher.launch(uCrop.getIntent(requireContext()))
    }

    private fun handleCropResult(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                result.data?.let { UCrop.getOutput(it) }?.let { croppedUri ->
                    viewModel.applyCrop(croppedUri)
                    showToast("Image cropped")
                }
            }
            UCrop.RESULT_ERROR -> {
                val error = result.data?.let { UCrop.getError(it) }
                showToast(error?.message ?: "Crop failed")
            }
        }
    }



    private fun toggleCompare() {
        val editingState = viewModel.uiState.value as? EditorUiState.Editing ?: return

        isComparingOriginal = !isComparingOriginal

        if (isComparingOriginal) {
            editingState.originalBitmap?.let {
                photoEditorView.source.setImageBitmap(it)
            }
            setOverlaysVisibility(View.GONE)
            binding.btnCompare.setImageResource(R.drawable.ic_back_orignial)
        } else {

            editingState.previewBitmap?.let {
                photoEditorView.source.setImageBitmap(it)
            }
            setOverlaysVisibility(View.VISIBLE)
            binding.btnCompare.setImageResource(R.drawable.ic_compare)
        }
    }

    private fun setOverlaysVisibility(visibility: Int) {
        for (i in 0 until photoEditorView.childCount) {
            val child = photoEditorView.getChildAt(i)
            if (child != photoEditorView.source) child.visibility = visibility
        }
    }



    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) enterFullscreen() else exitFullscreen()
    }

    private fun enterFullscreen() {
        fullscreenHelper.animateUIOut { }
        fullscreenHelper.animateFullscreenButton(true)
        fullscreenHelper.hideSystemUI()
    }

    private fun exitFullscreen() {
        fullscreenHelper.animateUIIn()
        fullscreenHelper.animateFullscreenButton(false)
        fullscreenHelper.showSystemUI()
    }


    private fun applyCurrentTransforms() {
        val state = viewModel.editorState.value
        val zoom = state.canvasZoom
        photoEditorView.rotation = state.rotation
        photoEditorView.source.scaleX = zoom * if (state.flipHorizontal) -1f else 1f
        photoEditorView.source.scaleY = zoom * if (state.flipVertical) -1f else 1f
        applyTranslationLimit(state.imageTranslationX)
    }

    private fun applyZoomTransform(zoom: Float) {
        val state = viewModel.editorState.value
        photoEditorView.source.scaleX = zoom * if (state.flipHorizontal) -1f else 1f
        photoEditorView.source.scaleY = zoom * if (state.flipVertical) -1f else 1f
    }

    private fun applyTranslationLimit(requestedX: Float) {
        val containerWidth = binding.imageContainer.width.toFloat()
        val imageWidth = photoEditorView.source.width * photoEditorView.source.scaleX
        val maxT = ((imageWidth - containerWidth) / 2).coerceAtLeast(0f)
        val limitedX = requestedX.coerceIn(-maxT, maxT)
        photoEditorView.source.translationX = limitedX
        if (limitedX != requestedX) {
            viewModel.setImageTranslationX(limitedX)
        }
    }



    private fun showExportProgressDialog(message: String) {
        if (progressDialog?.isShowing == true) {
            progressDialog?.findViewById<TextView>(R.id.tvExportStatus)?.text = message
            return
        }

        progressDialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_export_progress)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCancelable(false)

            findViewById<TextView>(R.id.tvExportStatus).text = message
            show()
        }
    }

    private fun dismissExportProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun showExportSuccessDialog(uri: Uri) {
        val dialog = Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_export_success)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCancelable(true)
        }

        val ivPreview = dialog.findViewById<ImageView>(R.id.ivExportPreview)
        val tvImageInfo = dialog.findViewById<TextView>(R.id.tvImageInfo)

        viewLifecycleOwner.lifecycleScope.launch {

            val bitmap = withContext(Dispatchers.IO) {
                try {
                    requireContext().contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(
                            input,
                            null,
                            BitmapFactory.Options().apply {
                                inSampleSize = 4
                            }
                        )
                    }
                } catch (e: Exception) {
                    null
                }
            }

            bitmap?.let {

                ivPreview.setImageBitmap(it)

                val fileSize = withContext(Dispatchers.IO) {
                    try {
                        requireContext().contentResolver.openFileDescriptor(uri, "r")?.use {
                            it.statSize
                        } ?: 0L
                    } catch (e: Exception) {
                        0L
                    }
                }

                tvImageInfo.text =
                    "${it.width} × ${it.height} • ${formatFileSize(fileSize)}"
            }
        }

        dialog.findViewById<TextView>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<TextView>(R.id.btnOpen).setOnClickListener {
            dialog.dismiss()
            openImage(uri)
        }

        dialog.findViewById<TextView>(R.id.btnShare).setOnClickListener {
            dialog.dismiss()
            shareImage(uri)
        }

        dialog.show()
    }

    private fun shareImage(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(intent, "Share Image"))
    }

    private fun openImage(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No app found to open image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 ->
                String.format("%.2f MB", bytes / 1024f / 1024f)

            bytes >= 1024 ->
                String.format("%.1f KB", bytes / 1024f)

            else ->
                "$bytes B"
        }
    }


    private fun getCurrentPreviewBitmapSafe(): android.graphics.Bitmap? {
        return (viewModel.uiState.value as? EditorUiState.Editing)?.previewBitmap
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar?.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }



    private fun cleanup() {
        if (isFullscreen) {
            fullscreenHelper.showSystemUI()
            isFullscreen = false
        }
        if (::photoEditor.isInitialized) photoEditor.clearAllViews()
        binding.rvEditorTools.adapter = null
        progressDialog?.dismiss()
        progressDialog = null
    }
}