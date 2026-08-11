package com.editor.photo.video.collagemaker.photoedit.feature.doodle

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter.BrushTypeAdapter
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.BrushItem
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.core.BaseEditorBottomSheet
import com.editor.photo.video.collagemaker.photoedit.databinding.BottomSheetDoodleBinding
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.DoodlePath
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.DoodlePoint
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.DoodleShapeType
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.PhotoEditorView
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder
import ja.burhanrashid52.photoeditor.shape.ShapeType
import java.lang.ref.WeakReference
import java.util.UUID

class DoodleBottomSheet : BaseEditorBottomSheet<BottomSheetDoodleBinding>() {

    // Use WeakReference to prevent memory leaks
    private var photoEditorViewRef: WeakReference<PhotoEditorView>? = null
    private var photoEditorRef: WeakReference<PhotoEditor>? = null
    private var onDoodleApplied: (() -> Unit)? = null

    private var onStrokeCompleted: ((DoodlePath) -> Unit)? = null

    private var currentColor: Int = Color.RED
    private var currentBrushSize: Float = 25f
    private var currentShapeType: ShapeType = ShapeType.Brush
    private var isEraserMode: Boolean = false
    private var isDestroyed: Boolean = false

    private var brushAdapter: BrushTypeAdapter? = null
    private var touchInterceptor: FrameLayout? = null

    // Cache for performance
    private val editorLocation = IntArray(2)
    private val sheetLocation = IntArray(2)

    // Points of the stroke currently being drawn, in NORMALIZED (0f..1f) coordinates
    // relative to the PhotoEditorView. Cleared/rebuilt per stroke; never written to history
    // until the stroke finishes (ACTION_UP), so dragging never spams undo/redo.
    private val currentStrokePoints = mutableListOf<DoodlePoint>()
    private var isStrokeInProgress = false

    private fun currentDoodleShapeType(): DoodleShapeType = when (currentShapeType) {
        ShapeType.Line -> DoodleShapeType.LINE
        ShapeType.Rectangle -> DoodleShapeType.RECTANGLE
        ShapeType.Oval -> DoodleShapeType.OVAL
        else -> DoodleShapeType.FREEHAND
    }

    companion object {
        private const val TAG = "DoodleBottomSheet"
        private const val PEEK_HEIGHT_DP = 400
        private const val SETTINGS_DELAY_MS = 200L
        private const val RESET_DELAY_MS = 100L

        fun newInstance(
            photoEditorView: PhotoEditorView,
            photoEditor: PhotoEditor,
            onDoodleApplied: () -> Unit,
            onStrokeCompleted: (DoodlePath) -> Unit = {}
        ): DoodleBottomSheet {
            return DoodleBottomSheet().apply {
                this.photoEditorViewRef = WeakReference(photoEditorView)
                this.photoEditorRef = WeakReference(photoEditor)
                this.onDoodleApplied = onDoodleApplied
                this.onStrokeCompleted = onStrokeCompleted
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.TransparentBottomSheetDialog)
    }

    override fun onStart() {
        super.onStart()

        if (isDestroyed) return

        try {
            dialog?.window?.apply {
                clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                setDimAmount(0f)
            }

            setupBottomSheetBehavior()
            setupTouchInterceptor()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStart: ${e.message}", e)
        }
    }

    private fun setupBottomSheetBehavior() {
        try {
            (dialog as? BottomSheetDialog)?.behavior?.apply {
                state = BottomSheetBehavior.STATE_COLLAPSED
                peekHeight = (PEEK_HEIGHT_DP * resources.displayMetrics.density).toInt()
                isDraggable = true
                isHideable = false

                // Add callback to prevent crashes
                addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        if (newState == BottomSheetBehavior.STATE_HIDDEN && !isDestroyed) {
                            dismissAllowingStateLoss()
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        // No action needed
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up bottom sheet behavior: ${e.message}", e)
        }
    }

    private fun setupTouchInterceptor() {
        if (isDestroyed) return

        try {
            dialog?.window?.decorView?.let { decorView ->
                touchInterceptor = FrameLayout(requireContext()).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(Color.TRANSPARENT)
                    isClickable = false
                    isFocusable = false

                    setOnTouchListener { _, event ->
                        handleTouchEvent(event)
                    }
                }

                (decorView as? ViewGroup)?.addView(touchInterceptor)
                Log.d(TAG, " Touch interceptor installed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up touch interceptor: ${e.message}", e)
        }
    }

    private fun handleTouchEvent(event: MotionEvent): Boolean {
        if (isDestroyed) return false

        try {
            // Get bottom sheet position
            val bottomSheet = (dialog as? BottomSheetDialog)?.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            ) ?: return false

            bottomSheet.getLocationOnScreen(sheetLocation)
            val sheetTop = sheetLocation[1]

            // If touch is above bottom sheet, forward to PhotoEditorView
            if (event.rawY < sheetTop) {
                val editorView = photoEditorViewRef?.get() ?: return false

                // Only handle single touch to prevent crashes
                if (event.pointerCount != 1) {
                    Log.d(TAG, "Multi-touch ignored (${event.pointerCount} pointers)")
                    return false
                }

                editorView.getLocationOnScreen(editorLocation)

                // Convert screen coordinates to PhotoEditorView coordinates
                val x = event.rawX - editorLocation[0]
                val y = event.rawY - editorLocation[1]

                // Validate coordinates are within bounds
                if (x < 0 || y < 0 || x > editorView.width || y > editorView.height) {
                    return false
                }

                // Record this point (normalized against the PhotoEditorView) so the completed
                // stroke can be committed as ONE EditOperation.Doodle on touch-up. We only do
                // this for real brush strokes, not while the eraser is active - erasing isn't
                // yet modeled as an operation, so it's left to the PhotoEditor overlay only.
                if (!isEraserMode && editorView.width > 0 && editorView.height > 0) {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            currentStrokePoints.clear()
                            currentStrokePoints.add(DoodlePoint(x / editorView.width, y / editorView.height))
                            isStrokeInProgress = true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (isStrokeInProgress) {
                                currentStrokePoints.add(DoodlePoint(x / editorView.width, y / editorView.height))
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            if (isStrokeInProgress) {
                                currentStrokePoints.add(DoodlePoint(x / editorView.width, y / editorView.height))
                                commitCurrentStroke(editorView.width)
                            }
                            isStrokeInProgress = false
                        }
                    }
                }

                // Create new motion event with adjusted coordinates
                val newEvent = MotionEvent.obtain(
                    event.downTime,
                    event.eventTime,
                    event.action,
                    x,
                    y,
                    event.metaState
                )

                try {
                    val handled = editorView.dispatchTouchEvent(newEvent)
                    Log.d(TAG, "Touch: action=${event.action}, x=$x, y=$y, handled=$handled")
                    return handled
                } finally {
                    newEvent.recycle()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Touch handling error: ${e.message}", e)
        }

        return false
    }

    /**
     * Finalizes the in-progress stroke into ONE immutable [DoodlePath] and hands it to
     * [onStrokeCompleted] (wired by PhotoStudioFragment to EditorSessionViewModel.addDoodle),
     * which routes it through ApplyDoodleUseCase -> HistoryManager -> EditorEngine so it becomes
     * the real, exportable, undo/redo-aware app state.
     *
     * The PhotoEditor library's own just-drawn stroke is then undone: it already did its job of
     * giving the user live visual feedback while dragging, but it must not remain a second,
     * competing copy of this stroke once our own operation has taken over as the source of truth
     * (the next refreshPreview() will re-render photoEditorView.source with the doodle baked in).
     */
    private fun commitCurrentStroke(editorViewWidth: Int) {
        try {
            if (currentStrokePoints.isEmpty() || editorViewWidth <= 0) return

            val doodlePath = DoodlePath(
                id = UUID.randomUUID().toString(),
                points = currentStrokePoints.toList(),
                color = currentColor,
                strokeWidth = currentBrushSize / editorViewWidth,
                alpha = Color.alpha(currentColor).let { if (it == 0) 255 else it },
                shapeType = currentDoodleShapeType()
            )

            onStrokeCompleted?.invoke(doodlePath)

            // Remove PhotoEditor's own transient copy of the stroke we just committed.
            photoEditorRef?.get()?.undo()
        } catch (e: Exception) {
            Log.e(TAG, "Error committing stroke: ${e.message}", e)
        } finally {
            currentStrokePoints.clear()
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): BottomSheetDoodleBinding {
        return BottomSheetDoodleBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        if (isDestroyed) return

        try {
            setupColorSeekBar()
            setupBrushSizeSeekBar()
            setupBrushRecyclerView()

            // Initialize visual states
            updateSeekBarThumbColor(binding.seekBarColor, currentColor)
            updateSeekBarThumbColor(binding.seekBarBrushSize, currentColor)
            binding.seekBarBrushSize.progressDrawable?.mutate()?.setTint(currentColor)
            updateBrushPreview()
            updateEraserButtonState()

            enableDrawingMode()
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupUI: ${e.message}", e)
        }
    }

    override fun setupListeners() {
        if (isDestroyed) return

        try {
            setupClickListeners()
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupListeners: ${e.message}", e)
        }
    }

    private fun setupColorSeekBar() {
        binding.seekBarColor.apply {
            progress = 0

            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (isDestroyed) return

                    try {
                        val hsv = floatArrayOf(progress.toFloat(), 1f, 1f)
                        currentColor = Color.HSVToColor(hsv)

                        if (isEraserMode) {
                            isEraserMode = false
                            updateEraserButtonState()
                        }

                        // Dynamic color visual updates
                        updateSeekBarThumbColor(this@apply, currentColor)
                        updateSeekBarThumbColor(binding.seekBarBrushSize, currentColor)
                        binding.seekBarBrushSize.progressDrawable?.mutate()?.setTint(currentColor)
                        brushAdapter?.updateColor(currentColor)

                        updateBrushPreview()
                        applyBrushSettings()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error changing color: ${e.message}", e)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }

    private fun setupBrushSizeSeekBar() {
        binding.seekBarBrushSize.apply {
            progress = currentBrushSize.toInt()
            binding.txtBrushSize.text = "${progress}"

            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (isDestroyed) return

                    try {
                        // Clamp min size to 5 to avoid completely invisible drawings
                        val size = Math.max(5, progress)
                        currentBrushSize = size.toFloat()
                        binding.txtBrushSize.text = "${size}"

                        updateBrushPreview()
                        applyBrushSettings()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error changing brush size: ${e.message}", e)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }

    private fun setupBrushRecyclerView() {
        if (isDestroyed) return

        try {
            val brushTypes = listOf(
                BrushItem(ShapeType.Brush, R.drawable.ic_brush, "Brush", 25f),
                BrushItem(ShapeType.Oval, R.drawable.ic_oval, "Oval", 30f),
                BrushItem(ShapeType.Rectangle, R.drawable.ic_rectangle, "Rectangle", 30f),
                BrushItem(ShapeType.Line, R.drawable.ic_line, "Line", 20f)
            )

            brushAdapter = BrushTypeAdapter(brushTypes) { brushItem ->
                if (isDestroyed) return@BrushTypeAdapter

                try {
                    currentBrushSize = brushItem.size
                    binding.seekBarBrushSize.progress = currentBrushSize.toInt()
                    binding.txtBrushSize.text = "${currentBrushSize.toInt()}"
                    currentShapeType = brushItem.shapeType

                    Log.d(TAG, "Selected: ${brushItem.name}")

                    if (isEraserMode) {
                        isEraserMode = false
                        updateEraserButtonState()
                    }

                    updateBrushPreview()
                    applyBrushSettings()
                } catch (e: Exception) {
                    Log.e(TAG, "Error selecting brush: ${e.message}", e)
                }
            }

            brushAdapter?.updateColor(currentColor)

            binding.rvBrushTypes.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = brushAdapter
                setHasFixedSize(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up brush recycler view: ${e.message}", e)
        }
    }

    private fun setupClickListeners() {
        binding.btnUndo.setOnClickListener {
            safeExecute {
                photoEditorRef?.get()?.undo()
                Log.d(TAG, "Undo clicked")
            }
        }

        binding.btnRedo.setOnClickListener {
            safeExecute {
                photoEditorRef?.get()?.redo()
                Log.d(TAG, "Redo clicked")
            }
        }

        binding.btnReverse.setOnClickListener {
            safeExecute {
                photoEditorRef?.get()?.clearHelperBox()

                currentColor = Color.RED
                currentBrushSize = 25f
                currentShapeType = ShapeType.Brush

                binding.seekBarColor.progress = 0
                binding.seekBarBrushSize.progress = 25
                binding.txtBrushSize.text = "25"

                isEraserMode = false
                updateEraserButtonState()

                // Reset visual states
                updateSeekBarThumbColor(binding.seekBarColor, currentColor)
                updateSeekBarThumbColor(binding.seekBarBrushSize, currentColor)
                binding.seekBarBrushSize.progressDrawable?.mutate()?.setTint(currentColor)
                brushAdapter?.updateColor(currentColor)

                updateBrushPreview()

                binding.root.postDelayed({
                    if (!isDestroyed) {
                        applyBrushSettings()
                        Log.d(TAG, "Settings reset to default")
                    }
                }, RESET_DELAY_MS)
            }
        }

        binding.btnApply.setOnClickListener {
            safeExecute {
                applyDoodle()
            }
        }

        binding.btnEraser.setOnClickListener {
            safeExecute {
                isEraserMode = !isEraserMode
                updateEraserButtonState()
                updateBrushPreview()

                if (isEraserMode) {
                    photoEditorRef?.get()?.brushEraser()
                    Log.d(TAG, "Eraser mode ON")
                } else {
                    applyBrushSettings()
                    Log.d(TAG, "Eraser mode OFF")
                }
            }
        }
    }

    private fun enableDrawingMode() {
        if (isDestroyed) return

        binding.root.postDelayed({
            if (!isDestroyed) {
                applyBrushSettings()
                Log.d(TAG, "✨ Drawing ACTIVE with touch interceptor")
            }
        }, SETTINGS_DELAY_MS)
    }

    private fun applyBrushSettings() {
        if (isDestroyed) return

        val editor = photoEditorRef?.get() ?: return

        try {
            editor.setBrushDrawingMode(true)

            if (isEraserMode) {
                editor.brushEraser()
                editor.brushSize = currentBrushSize
            } else {
                if (currentShapeType == ShapeType.Brush) {
                    editor.brushColor = currentColor
                    editor.brushSize = currentBrushSize
                } else {
                    val shapeBuilder = ShapeBuilder()
                        .withShapeType(currentShapeType)
                        .withShapeSize(currentBrushSize)
                        .withShapeOpacity(255)

                    editor.setShape(shapeBuilder)
                    editor.brushColor = currentColor
                }
            }

            Log.d(TAG, "✅ Brush: $currentShapeType | Size=$currentBrushSize | Color=$currentColor")

        } catch (e: Exception) {
            Log.e(TAG, "Error applying brush settings: ${e.message}", e)
        }
    }

    private fun applyDoodle() {
        if (isDestroyed) return

        try {
            Log.d(TAG, "💾 Saving doodle...")
            photoEditorRef?.get()?.setBrushDrawingMode(false)
            onDoodleApplied?.invoke()
            dismissAllowingStateLoss()
        } catch (e: Exception) {
            Log.e(TAG, "Error applying doodle: ${e.message}", e)
        }
    }

    private fun updateSeekBarThumbColor(seekBar: SeekBar, color: Int) {
        try {
            val thumb = seekBar.thumb?.mutate()
            if (thumb is GradientDrawable) {
                thumb.setColor(color)
                thumb.setStroke(dpToPx(3), Color.WHITE)
            } else {
                seekBar.thumb?.setTint(color)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating thumb color: ${e.message}", e)
        }
    }

    private fun updateBrushPreview() {
        if (isDestroyed) return
        try {
            // Map currentBrushSize (5 to 100) to a visual diameter within the 64dp container (4dp to 48dp)
            val minPx = dpToPx(4)
            val maxPx = dpToPx(48)
            val progressPercent = (currentBrushSize - 5f) / (100f - 5f)
            val sizePx = (minPx + progressPercent * (maxPx - minPx)).toInt()

            val params = binding.viewBrushPreview.layoutParams
            params.width = sizePx
            params.height = sizePx
            binding.viewBrushPreview.layoutParams = params

            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (isEraserMode) {
                    setColor(Color.TRANSPARENT)
                    setStroke(dpToPx(2), Color.WHITE, dpToPx(4).toFloat(), dpToPx(2).toFloat())
                } else {
                    setColor(currentColor)
                }
            }
            binding.viewBrushPreview.background = drawable
        } catch (e: Exception) {
            Log.e(TAG, "Error updating brush preview: ${e.message}", e)
        }
    }

    private fun updateEraserButtonState() {
        if (isDestroyed) return
        try {
            val background = binding.btnEraser.background?.mutate() as? GradientDrawable
            if (background != null) {
                if (isEraserMode) {
                    binding.btnEraser.animate()
                        .alpha(1.0f)
                        .scaleX(1.1f)
                        .scaleY(1.1f)
                        .setDuration(150)
                        .start()
                    background.setColor(Color.parseColor("#33FFFFFF"))
                    background.setStroke(dpToPx(2), Color.WHITE)
                } else {
                    binding.btnEraser.animate()
                        .alpha(0.5f)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(150)
                        .start()
                    background.setColor(Color.parseColor("#1AFFFFFF"))
                    background.setStroke(dpToPx(1), Color.parseColor("#33FFFFFF"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating eraser button state: ${e.message}", e)
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private inline fun safeExecute(block: () -> Unit) {
        if (isDestroyed) return

        try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, "Error in safeExecute: ${e.message}", e)
        }
    }

    override fun onDestroyView() {
        isDestroyed = true

        try {
            // Remove touch interceptor
            touchInterceptor?.let { interceptor ->
                (dialog?.window?.decorView as? ViewGroup)?.removeView(interceptor)
            }
            touchInterceptor = null

            // Disable drawing mode
            photoEditorRef?.get()?.setBrushDrawingMode(false)

            // Clear references
            brushAdapter = null
            photoEditorViewRef = null
            photoEditorRef = null
            onDoodleApplied = null

            Log.d(TAG, "🧹 Cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroyView: ${e.message}", e)
        }

        super.onDestroyView()
    }

    override fun onDestroy() {
        isDestroyed = true
        super.onDestroy()
    }
}
