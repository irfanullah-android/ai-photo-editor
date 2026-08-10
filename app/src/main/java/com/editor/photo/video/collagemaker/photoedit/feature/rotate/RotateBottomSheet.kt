package com.editor.photo.video.collagemaker.photoedit.feature.rotate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.editor.photo.video.collagemaker.photoedit.databinding.BottomSheetRotateBinding
import com.editor.photo.video.collagemaker.photoedit.editor.session.EditorSessionViewModel

class RotateBottomSheet : BottomSheetDialogFragment() {

    private val sessionViewModel: EditorSessionViewModel by activityViewModels()
    private val rotateViewModel: RotateViewModel by viewModels()

    private lateinit var binding: BottomSheetRotateBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetRotateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupListeners()
        observeViewModel()
    }

    private fun setupUI() {
        val state = sessionViewModel.editorState.value
        rotateViewModel.initState(
            state.rotation,
            state.flipHorizontal,
            state.flipVertical,
            state.canvasZoom
        )
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            rotateViewModel.rotation.collect { rotation ->
                binding.seekBarAngle.progress = rotation.toInt()
                binding.tvAngleValue.text = "${rotation.toInt()}°"
                sessionViewModel.setRotation(rotation)
            }
        }

        lifecycleScope.launchWhenStarted {
            rotateViewModel.zoom.collect { zoom ->
                binding.seekBarZoom.progress = normalizeZoomToProgress(zoom)
                binding.tvZoomValue.text = String.format("%.1fx", zoom)
                sessionViewModel.setCanvasZoom(zoom)
            }
        }
    }

    private fun setupListeners() {
        binding.btnCheck.setOnClickListener {
            sessionViewModel.saveCanvasState()
            dismiss()
        }

        binding.layoutFlip.setOnClickListener {
            hideAllSeekBars()
            rotateViewModel.toggleFlipHorizontal()
            sessionViewModel.flipHorizontal()
        }

        binding.layoutFlipVertical.setOnClickListener {
            hideAllSeekBars()
            rotateViewModel.toggleFlipVertical()
            sessionViewModel.flipVertical()
        }

        binding.layoutRotate.setOnClickListener {
            hideAllSeekBars()
            rotateViewModel.rotate90()
            sessionViewModel.rotate90()
        }

        binding.layoutAngle.setOnClickListener {
            toggleSeekBar(binding.layoutAngleSeekBar, binding.layoutZoomSeekBar)
        }

        binding.layoutZoom.setOnClickListener {
            toggleSeekBar(binding.layoutZoomSeekBar, binding.layoutAngleSeekBar)
        }

        binding.seekBarAngle.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    rotateViewModel.setRotation(progress.toFloat())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                sessionViewModel.saveCanvasState()
            }
        })

        binding.seekBarZoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val zoom = progressToZoom(progress)
                    rotateViewModel.setZoom(zoom)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                sessionViewModel.saveCanvasState()
            }
        })
    }

    private fun toggleSeekBar(showLayout: View, hideLayout: View) {
        if (showLayout.visibility == View.VISIBLE) {
            showLayout.visibility = View.GONE
        } else {
            hideLayout.visibility = View.GONE
            showLayout.visibility = View.VISIBLE
        }
    }

    private fun hideAllSeekBars() {
        binding.layoutAngleSeekBar.visibility = View.GONE
        binding.layoutZoomSeekBar.visibility = View.GONE
    }

    private fun progressToZoom(progress: Int): Float {
        return 0.5f + (progress / 100f) * 2.5f
    }

    private fun normalizeZoomToProgress(zoom: Float): Int {
        return (((zoom - 0.5f) / 2.5f) * 100f).toInt()
    }

    companion object {
        fun newInstance() = RotateBottomSheet()
    }
}
