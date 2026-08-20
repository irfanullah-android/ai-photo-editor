package com.editor.photo.video.collagemaker.photoedit.fragments.childfragments

import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.adapters.galleryadpater.EditorToolsAdapter
import com.editor.photo.video.collagemaker.photoedit.databinding.FragmentPhotoStudioBinding
import com.editor.photo.video.collagemaker.photoedit.editor.session.EditorSessionViewModel
import com.editor.photo.video.collagemaker.photoedit.feature.adjust.AdjustBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.crop.CropFeature
import com.editor.photo.video.collagemaker.photoedit.feature.doodle.DoodleBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.effect.EffectBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.filter.FilterBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.frame.FrameBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.remove.RemoveBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.rotate.RotateBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.sticker.StickerBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.sticker.StickerOverlayView
import com.editor.photo.video.collagemaker.photoedit.feature.text.TextEditorBottomSheet
import com.editor.photo.video.collagemaker.photoedit.feature.text.TextFonts
import com.editor.photo.video.collagemaker.photoedit.feature.text.TextOverlayView
import com.editor.photo.video.collagemaker.photoedit.fragments.basefragments.BaseFragment
import com.editor.photo.video.collagemaker.photoedit.helpers.FullscreenHelper
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorUiState
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.StickerLayer
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.TextAlignment
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.TextLayer
import com.editor.photo.video.collagemaker.photoedit.models.gallery.EditorItemModel
import com.editor.photo.video.collagemaker.photoedit.models.gallery.EditorTool
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.PhotoEditorView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotoStudioFragment :
    BaseFragment<FragmentPhotoStudioBinding>(R.layout.fragment_photo_studio) {

    companion object {
        private const val TAG = "PhotoStudioFragment"
        private const val MIN_SCALE = 0.5f
        private const val MAX_SCALE = 3.0f
        private const val STICKER_BASE_SIZE_RATIO = 0.25f

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

    private var textOverlayView: TextOverlayView? = null
    private var isOverlayTransforming = false
    private var layoutRetryToken = 0

    private var stickerOverlayView: StickerOverlayView? = null
    private var isStickerOverlayTransforming = false
    private var stickerLayoutRetryToken = 0

    private var lastAppliedStickerId: String? = null
    private var lastAppliedStickerScale: Float = Float.NaN
    private var lastAppliedStickerX: Float = Float.NaN
    private var lastAppliedStickerY: Float = Float.NaN
    private var lastAppliedStickerRotation: Float = Float.NaN

    private val cropFeature = CropFeature(
        caller = this,
        onCropSuccess = { uri ->
            viewModel.applyCrop(uri)
            showToast("Image cropped")
        },
        onCropError = { message -> showToast(message) }
    )

    override fun onViewCreatedOneTime() {
        setupPhotoEditorView()
        setupFullscreenHelper()
        setupToolsRecyclerView()
        setupClickListeners()
        setupGestureDetectors()
        setupBackPressHandler()
        observeUiState()
        observeEditorState()
        observeActiveTool()
        observeActiveText()
        observeActiveSticker()
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
            EditorItemModel("Filter", R.drawable.ic_filter),
            EditorItemModel("Adjust", R.drawable.ic_adjust),
            EditorItemModel("Effect", R.drawable.ic_effect),
            EditorItemModel("Sticker", R.drawable.ic_sticker),
            EditorItemModel("Text", R.drawable.ic_text),
            EditorItemModel("Remove", R.drawable.ic_bg_remove),
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
            btnExport.setOnClickListener {
                commitAndCloseAllOverlays()
                viewModel.exportImage("PhotoFix_" + System.currentTimeMillis() + ".jpg")
            }
            btnFullscreen.setOnClickListener { toggleFullscreen() }
            binding.stickerOverlayContainer.setOnClickListener(null)
            binding.stickerOverlayContainer.isClickable = false

            setupCompareButton()
        }
    }

    private fun setupCompareButton() {
        binding.btnCompare.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    showOriginal()
                    view.isPressed = true
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    restoreEdited()
                    view.isPressed = false
                    view.performClick()
                    true
                }

                else -> false
            }
        }
        binding.btnCompare.setOnClickListener {}
    }

    private fun showOriginal() {
        if (isComparingOriginal) return
        val editingState = viewModel.uiState.value as? EditorUiState.Editing ?: return

        isComparingOriginal = true
        editingState.originalBitmap?.let {
            photoEditorView.source.setImageBitmap(it)
        }
        setOverlaysVisibility(View.GONE)
        binding.btnCompare.setImageResource(R.drawable.ic_back_orignial)
    }

    private fun restoreEdited() {
        if (!isComparingOriginal) return
        isComparingOriginal = false

        val bitmap = (viewModel.uiState.value as? EditorUiState.Editing)?.previewBitmap
        bitmap?.let {
            photoEditorView.source.setImageBitmap(it)
        }
        setOverlaysVisibility(View.VISIBLE)
        binding.btnCompare.setImageResource(R.drawable.ic_compare)
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
            if (event.action == MotionEvent.ACTION_DOWN) {
                handleOutsideTap(event)
            }
            true
        }
    }

    private fun handleOutsideTap(event: MotionEvent) {
        val rawX = event.rawX.toInt()
        val rawY = event.rawY.toInt()

        textOverlayView?.let { overlay ->
            val rect = Rect()
            overlay.getGlobalVisibleRect(rect)
            if (!rect.contains(rawX, rawY)) {
                commitAndCloseTextOverlay()
            }
        }

        stickerOverlayView?.let { overlay ->
            val rect = Rect()
            overlay.getGlobalVisibleRect(rect)
            if (!rect.contains(rawX, rawY)) {
                commitAndCloseStickerOverlay()
            }
        }

        if (viewModel.activeTextId.value == null && viewModel.activeStickerId.value == null) {
            trySelectLayerAt(rawX, rawY)
        }
    }

    private fun trySelectLayerAt(rawX: Int, rawY: Int) {
        val container = binding.textOverlayContainer
        val loc = IntArray(2)
        container.getLocationOnScreen(loc)
        val localX = (rawX - loc[0]).toFloat()
        val localY = (rawY - loc[1]).toFloat()

        val imageRect = fittedImageRect(container.width, container.height, currentPreviewBitmapForOverlay())
        val state = viewModel.editorState.value

        val hitSticker = state.stickerLayers.lastOrNull { isPointInSticker(it, localX, localY, imageRect) }
        if (hitSticker != null) {
            viewModel.setActiveStickerId(hitSticker.id)
            return
        }

        val hitText = state.textLayers.lastOrNull { isPointInText(it, localX, localY, imageRect) }
        if (hitText != null) {
            viewModel.setActiveTextId(hitText.id)
        }
    }

    private fun isPointInText(layer: TextLayer, x: Float, y: Float, imageRect: RectF): Boolean {
        val centerX = imageRect.left + layer.x * imageRect.width()
        val centerY = imageRect.top + layer.y * imageRect.height()
        val halfW = (imageRect.width() * 0.85f) / 2f
        val halfH = (layer.size * imageRect.width() * 2.4f).coerceAtLeast(dp(48).toFloat()) / 2f
        return x in (centerX - halfW)..(centerX + halfW) && y in (centerY - halfH)..(centerY + halfH)
    }

    private fun isPointInSticker(layer: StickerLayer, x: Float, y: Float, imageRect: RectF): Boolean {
        val centerX = imageRect.left + layer.x * imageRect.width()
        val centerY = imageRect.top + layer.y * imageRect.height()
        val half = (imageRect.width() * STICKER_BASE_SIZE_RATIO * layer.scale) / 2f
        return x in (centerX - half)..(centerX + half) && y in (centerY - half)..(centerY + half)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun commitAndCloseTextOverlay() {
        val activeId = viewModel.activeTextId.value ?: return
        val layer = viewModel.editorState.value.textLayers.firstOrNull { it.id == activeId }

        if (layer?.text.isNullOrBlank()) {
            viewModel.removeText(activeId)
        }
        viewModel.setActiveTextId(null)
    }

    private fun commitAndCloseStickerOverlay() {
        if (viewModel.activeStickerId.value == null) return
        viewModel.setActiveStickerId(null)
    }

    private fun commitAndCloseAllOverlays() {
        commitAndCloseTextOverlay()
        commitAndCloseStickerOverlay()
    }

    fun onTextEditorClosed() {
        commitAndCloseTextOverlay()
    }

    fun onStickerEditorClosed() {
        commitAndCloseStickerOverlay()
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

    private fun observeEditorState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.editorState.collectLatest {
                applyCurrentTransforms()
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

    private fun renderTextOverlay(layer: TextLayer) {
        binding.textOverlayContainer.isClickable = false
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

        val imageRect =
            fittedImageRect(containerWidth, containerHeight, currentPreviewBitmapForOverlay())

        if (textOverlayView == null || container.childCount > 1 || (container.childCount == 1 && container.getChildAt(
                0
            ) != textOverlayView)
        ) {
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
        overlay.setScaleFactorProgrammatic(1f)

        overlay.setStroke(layer.strokeWidth * imageRect.width(), layer.strokeColor)

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
            val layer =
                viewModel.editorState.value.textLayers.firstOrNull { it.id == textId } ?: return

            val container = binding.textOverlayContainer
            val imageRect =
                fittedImageRect(container.width, container.height, currentPreviewBitmapForOverlay())

            val centerX = container.width / 2f + overlay.translationX
            val centerY = container.height / 2f + overlay.translationY

            val finalSize = (layer.size * overlay.scaleFactor).coerceIn(0.01f, 0.5f)
            overlay.setScaleFactorProgrammatic(1f)

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
            if (viewModel.activeTextId.value == textId) {
                viewModel.setActiveTextId(null)
            }
        }

        override fun onEditRequested() {
            val existing =
                childFragmentManager.findFragmentByTag("text_editor") as? TextEditorBottomSheet
            if (existing != null && existing.isAdded) {
                existing.enterEditMode()
            } else {
                viewModel.setActiveTextId(textId)
                val sheet = TextEditorBottomSheet.newInstance()
                sheet.show(childFragmentManager, "text_editor")
                childFragmentManager.executePendingTransactions()
                sheet.enterEditMode()
            }
        }
    }

    private fun removeTextOverlay() {
        textOverlayView?.let { binding.textOverlayContainer.removeView(it) }
        textOverlayView = null
        binding.textOverlayContainer.isClickable = false
    }

    private fun observeActiveSticker() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                viewModel.activeStickerId,
                viewModel.editorState,
                viewModel.uiState
            ) { activeId, state, _ ->
                activeId to state.stickerLayers.firstOrNull { it.id == activeId }
            }.collectLatest { (activeId, layer) ->
                if (activeId == null || layer == null) {
                    removeStickerOverlay()
                } else {
                    renderStickerOverlay(layer)
                }
            }
        }
    }

    private fun renderStickerOverlay(layer: StickerLayer) {
        if (isStickerOverlayTransforming) {
            Log.d(TAG, "renderStickerOverlay SKIP -> isStickerOverlayTransforming=true")
            return
        }

        val container = binding.stickerOverlayContainer
        val containerWidth = container.width
        val containerHeight = container.height
        if (containerWidth <= 0 || containerHeight <= 0) {
            Log.d(
                TAG,
                "renderStickerOverlay RETRY -> container not laid out yet (w=$containerWidth h=$containerHeight)"
            )
            val token = ++stickerLayoutRetryToken
            container.post {
                if (_binding != null && token == stickerLayoutRetryToken) {
                    renderStickerOverlay(layer)
                }
            }
            return
        }
        stickerLayoutRetryToken++

        if (stickerOverlayView == null || container.childCount > 1 ||
            (container.childCount == 1 && container.getChildAt(0) != stickerOverlayView)
        ) {
            Log.d(
                TAG,
                "renderStickerOverlay -> recreating overlay view (childCount=${container.childCount})"
            )
            container.removeAllViews()
            stickerOverlayView = null
            lastAppliedStickerId = null
        }

        val isNewOverlay = stickerOverlayView == null
        val overlay =
            stickerOverlayView ?: StickerOverlayView(requireContext()).also { newOverlay ->
                val params = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
                container.addView(newOverlay, params)
                newOverlay.listener = buildStickerOverlayListener(layer.id)
                stickerOverlayView = newOverlay
            }

        if (layer.emojiContent.isNotEmpty()) {
            overlay.setEmoji(layer.emojiContent)
        } else if (layer.resourceId > 0) {
            overlay.setStickerDrawable(layer.resourceId)
        }

        Log.d(
            TAG,
            "renderStickerOverlay CALLED -> isNewOverlay=$isNewOverlay " +
                    "incoming(id=${layer.id}, scale=${layer.scale}, x=${layer.x}, y=${layer.y}, rot=${layer.rotation}) " +
                    "cached(id=$lastAppliedStickerId, scale=$lastAppliedStickerScale, x=$lastAppliedStickerX, y=$lastAppliedStickerY, rot=$lastAppliedStickerRotation)"
        )

        val alreadyApplied = !isNewOverlay &&
                lastAppliedStickerId == layer.id &&
                lastAppliedStickerScale == layer.scale &&
                lastAppliedStickerX == layer.x &&
                lastAppliedStickerY == layer.y &&
                lastAppliedStickerRotation == layer.rotation

        if (alreadyApplied) {
            Log.d(TAG, "renderStickerOverlay SKIP -> alreadyApplied=true (no visual change)")
            return
        }

        val imageRect =
            fittedImageRect(containerWidth, containerHeight, currentPreviewBitmapForOverlay())
        val baseSizePx = imageRect.width() * STICKER_BASE_SIZE_RATIO

        Log.d(
            TAG,
            "renderStickerOverlay APPLY -> imageRect=$imageRect baseSizePx=$baseSizePx " +
                    "willSetScale=${layer.scale} finalVisualSizePx=${baseSizePx * layer.scale}"
        )

        overlay.setBaseEmojiSizePx(baseSizePx)
        overlay.setScale(layer.scale)
        overlay.rotation = layer.rotation

        val targetCenterX = imageRect.left + layer.x * imageRect.width()
        val targetCenterY = imageRect.top + layer.y * imageRect.height()
        overlay.translationX = targetCenterX - containerWidth / 2f
        overlay.translationY = targetCenterY - containerHeight / 2f

        Log.d(
            TAG,
            "renderStickerOverlay APPLIED -> translationX=${overlay.translationX} translationY=${overlay.translationY} " +
                    "overlay.scaleFactor=${overlay.scaleFactor} overlay.rotation=${overlay.rotation}"
        )

        lastAppliedStickerId = layer.id
        lastAppliedStickerScale = layer.scale
        lastAppliedStickerX = layer.x
        lastAppliedStickerY = layer.y
        lastAppliedStickerRotation = layer.rotation
    }

    private fun buildStickerOverlayListener(stickerId: String) =
        object : StickerOverlayView.Listener {
            override fun onTransformChanged() {
                isStickerOverlayTransforming = true
            }

            override fun onTransformCommitted() {
                isStickerOverlayTransforming = false
                val overlay = stickerOverlayView ?: return
                val layer =
                    viewModel.editorState.value.stickerLayers.firstOrNull { it.id == stickerId }
                        ?: return

                val container = binding.stickerOverlayContainer
                val imageRect =
                    fittedImageRect(
                        container.width,
                        container.height,
                        currentPreviewBitmapForOverlay()
                    )

                val centerX = container.width / 2f + overlay.translationX
                val centerY = container.height / 2f + overlay.translationY
                val finalScale = overlay.scaleFactor.coerceIn(0.1f, 6f)

                viewModel.updateSticker(
                    layer.copy(
                        x = ((centerX - imageRect.left) / imageRect.width()).coerceIn(0f, 1f),
                        y = ((centerY - imageRect.top) / imageRect.height()).coerceIn(0f, 1f),
                        scale = finalScale,
                        rotation = overlay.rotation % 360f
                    )
                )
            }

            override fun onDeleteRequested() {
                viewModel.removeSticker(stickerId)
                if (viewModel.activeStickerId.value == stickerId) {
                    viewModel.setActiveStickerId(null)
                }
            }
        }

    private fun removeStickerOverlay() {
        stickerOverlayView?.let { binding.stickerOverlayContainer.removeView(it) }
        stickerOverlayView = null
        lastAppliedStickerId = null
        lastAppliedStickerScale = Float.NaN
        lastAppliedStickerX = Float.NaN
        lastAppliedStickerY = Float.NaN
        lastAppliedStickerRotation = Float.NaN
    }

    private fun currentPreviewBitmapForOverlay(): Bitmap? =
        (viewModel.uiState.value as? EditorUiState.Editing)?.previewBitmap

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
        commitAndCloseAllOverlays()

        when (tool) {
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
            else -> showToast("$tool coming soon")
        }
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
            photoEditor = photoEditor
        ).apply {
            onDoodleApplied = {
                photoEditor.setBrushDrawingMode(false)
                showToast("Doodle applied")
            }
            onStrokeCompleted = { doodlePath ->
                viewModel.addDoodle(doodlePath)
            }
        }.show(childFragmentManager, "doodle")
    }

    private fun showRotateBottomSheet() {
        RotateBottomSheet.newInstance()
            .show(childFragmentManager, "rotate")
    }

    private fun showTextBottomSheet(existingTextId: String? = null) {
        if (existingTextId != null) {
            viewModel.setActiveTextId(existingTextId)
        } else {
            val newLayer = TextLayer(
                id = java.util.UUID.randomUUID().toString(),
                text = ""
            )
            viewModel.addText(newLayer)
            viewModel.setActiveTextId(newLayer.id)
        }
        TextEditorBottomSheet.newInstance().show(childFragmentManager, "text_editor")
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
        cropFeature.launch(requireContext(), Uri.parse(uriString), aspectRatio)
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
        binding.stickerOverlayContainer.visibility = visibility
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
        removeStickerOverlay()
        progressDialog?.dismiss()
        progressDialog = null
    }
}