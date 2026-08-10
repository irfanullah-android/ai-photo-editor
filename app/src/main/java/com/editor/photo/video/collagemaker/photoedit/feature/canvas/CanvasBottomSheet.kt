package com.editor.photo.video.collagemaker.photoedit.feature.canvas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter.AspectRatioAdapter
import com.editor.photo.video.collagemaker.photoedit.databinding.BottomSheetCanvasBinding
import com.editor.photo.video.collagemaker.photoedit.editor.session.EditorSessionViewModel
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.AspectRatio
import ja.burhanrashid52.photoeditor.PhotoEditorView

class CanvasBottomSheet : BottomSheetDialogFragment() {

    private val sessionViewModel: EditorSessionViewModel by activityViewModels()
    private val canvasViewModel: CanvasViewModel by viewModels()

    private var photoEditorView: PhotoEditorView? = null
    private lateinit var binding: BottomSheetCanvasBinding
    private lateinit var aspectRatioAdapter: AspectRatioAdapter

    private var onCropClickListener: ((Float?) -> Unit)? = null
    private var selectedAspectRatio: Float? = null
    private var hasAspectRatioChanged = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetCanvasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupListeners()
        observeViewModel()
    }

    private fun setupUI() {
        val savedScale = sessionViewModel.editorState.value.canvasZoom
        val progress = scaleToProgress(savedScale)
        canvasViewModel.setZoomProgress(progress)
        applyZoomToView(savedScale)

        setupAspectRatioRecyclerView()
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            canvasViewModel.zoomProgress.collect { progress ->
                binding.seekBarZoom.progress = progress
                updateZoom(progress)
            }
        }
    }

    private fun setupListeners() {
        binding.btnCheck.setOnClickListener {
            if (hasAspectRatioChanged && selectedAspectRatio != null) {
                onCropClickListener?.invoke(selectedAspectRatio)
            } else {
                sessionViewModel.saveCanvasState()
            }
            dismiss()
        }

        binding.seekBarZoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                canvasViewModel.setZoomProgress(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                sessionViewModel.saveCanvasState()
            }
        })

        binding.btnZoomIn.setOnClickListener {
            val progress = canvasViewModel.zoomProgress.value
            val newProgress = (progress + ZOOM_STEP_PROGRESS).coerceAtMost(SEEKBAR_MAX)
            canvasViewModel.setZoomProgress(newProgress)
            sessionViewModel.saveCanvasState()
        }

        binding.btnFlipLeft.setOnClickListener {
            sessionViewModel.flipHorizontal()
        }

        binding.btnFlipUp.setOnClickListener {
            sessionViewModel.flipVertical()
        }
    }

    private fun updateZoom(progress: Int) {
        val scale = progressToScale(progress)
        applyZoomToView(scale)
        sessionViewModel.setCanvasZoom(scale)
    }

    private fun applyZoomToView(scale: Float) {
        photoEditorView?.let { view ->
            view.scaleX = scale
            view.scaleY = scale
        }
    }

    private fun progressToScale(progress: Int): Float =
        ZOOM_MIN_SCALE + (progress / SEEKBAR_MAX.toFloat()) * ZOOM_RANGE

    private fun scaleToProgress(scale: Float): Int =
        (((scale - ZOOM_MIN_SCALE) / ZOOM_RANGE) * SEEKBAR_MAX)
            .toInt()
            .coerceIn(0, SEEKBAR_MAX)

    private fun setupAspectRatioRecyclerView() {
        val aspectRatios = listOf(
            AspectRatio.NO_FRAME,
            AspectRatio.RATIO_1_1,
            AspectRatio.RATIO_4_5,
            AspectRatio.RATIO_9_16_YT,
            AspectRatio.RATIO_16_9,
            AspectRatio.RATIO_9_16_V,
            AspectRatio.RATIO_3_4,
            AspectRatio.RATIO_2_3,
            AspectRatio.RATIO_21_9,
            AspectRatio.RATIO_2_1,
            AspectRatio.RATIO_1_2
        )

        aspectRatioAdapter = AspectRatioAdapter(aspectRatios) { selectedItem ->
            canvasViewModel.selectRatio(selectedItem)
            handleAspectRatioSelection(selectedItem)
        }

        binding.rvAspectRatios.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = aspectRatioAdapter
            setHasFixedSize(true)
        }
    }

    private fun handleAspectRatioSelection(item: AspectRatio) {
        val ratioValue = when (item.id) {
            "no_frame" -> {
                hasAspectRatioChanged = false
                selectedAspectRatio = null
                sessionViewModel.setAspectRatio(null)
                return
            }
            "1x1" -> 1f / 1f
            "4x5" -> 4f / 5f
            "9x16_yt", "9x16_v" -> 9f / 16f
            "16x9" -> 16f / 9f
            "3x4" -> 3f / 4f
            "2x3" -> 2f / 3f
            "235x1" -> 2.35f / 1f
            "2x1" -> 2f / 1f
            "1x2" -> 1f / 2f
            else -> null
        }

        hasAspectRatioChanged = true
        selectedAspectRatio = ratioValue
        sessionViewModel.setAspectRatio(ratioValue)
    }

    override fun onDestroyView() {
        binding.rvAspectRatios.adapter = null
        hasAspectRatioChanged = false
        selectedAspectRatio = null
        photoEditorView = null
        super.onDestroyView()
    }

    fun setPhotoEditorView(view: PhotoEditorView) {
        this.photoEditorView = view
    }

    fun setOnCropClickListener(listener: (Float?) -> Unit) {
        this.onCropClickListener = listener
    }

    companion object {
        private const val ZOOM_MIN_SCALE = 0.5f
        private const val ZOOM_MAX_SCALE = 2.0f
        private const val ZOOM_RANGE = ZOOM_MAX_SCALE - ZOOM_MIN_SCALE
        private const val SEEKBAR_MAX = 100
        private const val ZOOM_STEP_PROGRESS = 10

        fun newInstance(
            photoEditorView: PhotoEditorView,
            onCropClick: ((Float?) -> Unit)? = null
        ): CanvasBottomSheet {
            return CanvasBottomSheet().apply {
                setPhotoEditorView(photoEditorView)
                onCropClick?.let { setOnCropClickListener(it) }
            }
        }
    }
}
