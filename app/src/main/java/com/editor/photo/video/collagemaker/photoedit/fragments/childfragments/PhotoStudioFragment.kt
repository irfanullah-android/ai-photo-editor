package com.editor.photo.video.collagemaker.photoedit.fragments.childfragments

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ScaleGestureDetector
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.adapters.galleryadpater.EditorToolsAdapter
import com.editor.photo.video.collagemaker.photoedit.databinding.FragmentPhotoStudioBinding
import com.editor.photo.video.collagemaker.photoedit.editor.session.EditorSessionViewModel
import com.editor.photo.video.collagemaker.photoedit.feature.adjust.AdjustBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.canvas.CanvasBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.doodle.DoodleBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.effect.EffectBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.enhance.EnhanceBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.filter.FilterBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.frame.FrameBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.remove.RemoveBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.rotate.RotateBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.sticker.StickerBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.text.TextFonts
import com.editor.photo.video.collagemaker.photoedit.feature.text.TextInputBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.text.TextOverlayView
import com.editor.photo.video.collagemaker.photoedit.fragments.basefragments.BaseFragment
import com.editor.photo.video.collagemaker.photoedit.helpers.FullscreenHelper
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorUiState
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.TextAlignment
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.TextLayer
import com.editor.photo.video.collagemaker.photoedit.models.gallery.EditorItemModel
import com.editor.photo.video.collagemaker.photoedit.models.gallery.EditorTool
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.PhotoEditorView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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

    // ── Real interactive text overlay (PART 4) ──────────────────────────────
    private var textOverlayView: TextOverlayView? = null
    private var isOverlayTransforming = false

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
        observeActiveText()
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
        photoEditorView.source.scaleType = ImageView.ScaleType.FIT_CENTER
        photoEditorView.source.adjustViewBounds = true
    }

    private fun setupFullscreenHelper() {
        fullscreenHelper = FullscreenHelper(requireActivity(), binding)
        fullscreenHelper.setupSystemUIListener { isFullscreen }
    }

    private fun setupToolsRecyclerView() {
        val tools = listOf(
            EditorItemModel("Canvas", R.drawable.ic_canvas),
            EditorItemModel("Filter", R.drawable.ic_filter),
            EditorItemModel("Adjust", R.drawable.ic_adjust),
            EditorItemModel("Effect", R.drawable.ic_effect),
            EditorItemModel("Sticker", R.drawable.ic_sticker),
            EditorItemModel("Text", R.drawable.ic_text),
            EditorItemModel("Remove", R.drawable.ic_bg_remove),
            EditorItemModel("Enhance", R.drawable.ic_enhance),
            EditorItemModel("Doodle", R.drawable.ic_doodle),
            EditorItemModel("Crop", R.drawable.ic_crop),
            EditorItemModel("Frame", R.drawable.ic_frame),
            EditorItemModel("Rotate", R.drawable.ic_rotate)
        )
        toolsAdapter = EditorToolsAdapter(tools) { tool ->
            val editorTool = EditorTool.fromName(tool.name)
            viewModel.setActiveTool(editorTool)
            openToolBottomSheet(editorTool)
        }
        binding.rvEditorTools.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = toolsAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            btnBack.setOnClickListener { findNavController().popBackStack() }
            btnUndo.setOnClickListener { viewModel.undo() }
            btnRedo.setOnClickListener { viewModel.redo() }
            btnExport.setOnClickListener { viewModel.exportImage("PhotoFix_" + System.currentTimeMillis() + ".jpg") }
            btnCompare.setOnClickListener { toggleCompare() }
            btnFullscreen.setOnClickListener { toggleFullscreen() }
        }
    }

    private fun setupGestureDetectors() {
        scaleDetector = ScaleGestureDetector(
            requireContext(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    currentScale =
                        (currentScale * detector.scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
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
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
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
                    is EditorUiState.Idle -> {}
                    is EditorUiState.Loading -> showLoading(true)

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

    private fun observeActiveText() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                viewModel.activeTextId,
                viewModel.editorState,
                viewModel.uiState
            ) { activeId, state, _ ->
                activeId to state.textLayers.firstOrNull { it.id == activeId }
            }.collectLatest { (activeId, layer) ->
                if (activeId == null || layer == null) {
                    removeTextOverlay()
                } else {
                    renderTextOverlay(layer)
                }
            }
        }
    }

    private var layoutRetryToken = 0

    private fun renderTextOverlay(layer: TextLayer) {
        if (isOverlayTransforming) return

        val container = binding.textOverlayContainer
        val containerWidth = container.width
        val containerHeight = container.height
        if (containerWidth <= 0 || containerHeight <= 0) {
            val token = ++layoutRetryToken
            container.post {
                if (_binding != null && token == layoutRetryToken) {
                    renderTextOverlay(layer)
                }
            }
            return
        }
        layoutRetryToken++

        val imageRect = fittedImageRect(containerWidth, containerHeight, currentPreviewBitmapForOverlay())

        if (textOverlayView == null || container.childCount > 1 || (container.childCount == 1 && container.getChildAt(0) != textOverlayView)) {
            container.removeAllViews()
            textOverlayView = null
        }

        val defaultWidth = (imageRect.width() * 0.85f).toInt().coerceAtLeast(300)
        val overlay = textOverlayView ?: TextOverlayView(requireContext()).also { newOverlay ->
            val params = FrameLayout.LayoutParams(
                defaultWidth,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            container.addView(newOverlay, params)
            newOverlay.listener = buildOverlayListener(layer.id)
            textOverlayView = newOverlay
        }
        (overlay.layoutParams as? FrameLayout.LayoutParams)?.let { lp ->
            if (lp.width != defaultWidth) {
                lp.width = defaultWidth
                overlay.layoutParams = lp
            }
        }

        val targetCenterX = imageRect.left + layer.x * imageRect.width()
        val targetCenterY = imageRect.top + layer.y * imageRect.height()
        overlay.translationX = targetCenterX - containerWidth / 2f
        overlay.translationY = targetCenterY - containerHeight / 2f
        overlay.rotation = layer.rotation
        overlay.setBaseTextSizePx(layer.size * imageRect.width())
        overlay.setStroke(layer.strokeWidth * imageRect.width(), layer.strokeColor)
        overlay.resetScale(1f)

        val typeface = TextFonts.OPTIONS.firstOrNull { it.key == layer.fontFamily }?.typefaceFamily
            ?: Typeface.DEFAULT
        overlay.applyStyle(
            text = layer.text,
            color = layer.color,
            isBold = layer.isBold,
            isItalic = layer.isItalic,
            isUnderline = layer.isUnderline,
            typeface = typeface
        )
        val alignmentGravity = when (layer.alignment) {
            TextAlignment.LEFT -> Gravity.CENTER_VERTICAL or Gravity.START
            TextAlignment.CENTER -> Gravity.CENTER
            TextAlignment.RIGHT -> Gravity.CENTER_VERTICAL or Gravity.END
        }
        overlay.applyAlignment(alignmentGravity)
    }

    private fun buildOverlayListener(textId: String) = object : TextOverlayView.Listener {
        override fun onTransformChanged() {
            isOverlayTransforming = true
        }

        override fun onTransformCommitted() {
            isOverlayTransforming = false
            val overlay = textOverlayView ?: return
            val layer = viewModel.editorState.value.textLayers.firstOrNull { it.id == textId } ?: return

            val container = binding.textOverlayContainer
            val imageRect = fittedImageRect(container.width, container.height, currentPreviewBitmapForOverlay())

            val centerX = container.width / 2f + overlay.translationX
            val centerY = container.height / 2f + overlay.translationY
            val finalSize = (layer.size * overlay.scaleFactor).coerceIn(0.01f, 0.5f)

            viewModel.updateText(
                layer.copy(
                    x = ((centerX - imageRect.left) / imageRect.width()).coerceIn(0f, 1f),
                    y = ((centerY - imageRect.top) / imageRect.height()).coerceIn(0f, 1f),
                    size = finalSize,
                    rotation = overlay.rotation % 360f
                )
            )
        }

        override fun onDeleteRequested() {
            viewModel.removeText(textId)
        }

        override fun onEditRequested() {
            // Spec: SAME dialog Phase 1 par wapas jaaye, position/rotation/scale/style preserve.
            (childFragmentManager.findFragmentByTag("text_editor_style") as? DialogFragment)?.dismiss()
            TextInputBottomSheet.newInstance(textId).show(childFragmentManager, "text_input")
        }
    }

    private fun removeTextOverlay() {
        textOverlayView?.let { binding.textOverlayContainer.removeView(it) }
        textOverlayView = null
    }

    private fun currentPreviewBitmapForOverlay(): Bitmap? =
        (viewModel.uiState.value as? EditorUiState.Editing)?.previewBitmap

    /** [PhotoEditorView.source] ka `FIT_CENTER` letterboxed rect (container ke andar). */
    private fun fittedImageRect(containerWidth: Int, containerHeight: Int, bitmap: Bitmap?): RectF {
        val cw = containerWidth.toFloat().coerceAtLeast(1f)
        val ch = containerHeight.toFloat().coerceAtLeast(1f)
        if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) {
            return RectF(0f, 0f, cw, ch)
        }
        val bitmapRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val containerRatio = cw / ch
        return if (bitmapRatio > containerRatio) {
            val fittedHeight = cw / bitmapRatio
            val top = (ch - fittedHeight) / 2f
            RectF(0f, top, cw, top + fittedHeight)
        } else {
            val fittedWidth = ch * bitmapRatio
            val left = (cw - fittedWidth) / 2f
            RectF(left, 0f, left + fittedWidth, ch)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════

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
        val currentBitmap = (photoEditorView.source.drawable as? BitmapDrawable)?.bitmap ?: return
        viewModel.syncPreviewBitmap(currentBitmap)
    }

    private fun openToolBottomSheet(tool: EditorTool) {
        when (tool) {
            EditorTool.CANVAS -> showCanvasBottomSheet()
            EditorTool.FILTER -> showFilterBottomSheet()
            EditorTool.ADJUST -> showAdjustBottomSheet()
            EditorTool.EFFECT -> showEffectBottomSheet()
            EditorTool.REMOVE -> showRemoveBottomSheet()
            EditorTool.DOODLE -> showDoodleBottomSheet()
            EditorTool.ROTATE -> showRotateBottomSheet()
            EditorTool.CROP -> openCropScreen(null)
            EditorTool.TEXT -> showTextBottomSheet()
            EditorTool.STICKER -> showStickerBottomSheet()
            EditorTool.FRAME -> showFrameBottomSheet()
            EditorTool.ENHANCE -> showEnhanceBottomSheet()
            else -> showToast("$tool coming soon")
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

        AdjustBottomSheet.newInstance(
            currentUri,
            photoEditorView.source,
            currentBitmap
        ) { adjusted ->
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
                viewModel.addDoodle(doodlePath)
            }
        ).show(childFragmentManager, "doodle")
    }

    private fun showRotateBottomSheet() {
        RotateBottomSheet.newInstance()
            .show(childFragmentManager, "rotate")
    }

    private fun showTextBottomSheet(existingTextId: String? = null) {
        TextInputBottomSheet.newInstance(existingTextId)
            .show(childFragmentManager, "text_input")
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
                ratio < 1f -> uCrop.withAspectRatio(1f, 1f / ratio)
                else -> uCrop.withAspectRatio(ratio, 1f)
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
        binding.textOverlayContainer.visibility = visibility
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
                        requireContext().contentResolver.openFileDescriptor(uri, "r")
                            ?.use { fileDescriptor ->
                                fileDescriptor.statSize
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
            Toast.makeText(requireContext(), "No app found to open image", Toast.LENGTH_SHORT)
                .show()
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

    private fun getCurrentPreviewBitmapSafe(): Bitmap? {
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
        removeTextOverlay()
        progressDialog?.dismiss()
        progressDialog = null
    }
}