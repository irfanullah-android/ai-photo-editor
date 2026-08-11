package com.editor.photo.video.collagemaker.photoedit.feature.rotate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.editor.photo.video.collagemaker.photoedit.core.BaseEditorBottomSheet
import com.editor.photo.video.collagemaker.photoedit.databinding.BottomSheetRotateBinding
import com.editor.photo.video.collagemaker.photoedit.editor.session.EditorSessionViewModel

class RotateBottomSheet :
    BaseEditorBottomSheet<BottomSheetRotateBinding>() {

    private val sessionViewModel: EditorSessionViewModel by activityViewModels()
    private val rotateViewModel: RotateViewModel by viewModels()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): BottomSheetRotateBinding {
        return BottomSheetRotateBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        val state = sessionViewModel.editorState.value

        rotateViewModel.initState(
            state.rotation,
            state.flipHorizontal,
            state.flipVertical,
            state.canvasZoom
        )

        observeViewModel()
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

    override fun setupListeners() {

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
            toggleSeekBar(
                binding.layoutAngleSeekBar,
                binding.layoutZoomSeekBar
            )
        }

        binding.layoutZoom.setOnClickListener {
            toggleSeekBar(
                binding.layoutZoomSeekBar,
                binding.layoutAngleSeekBar
            )
        }

        binding.seekBarAngle.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        rotateViewModel.setRotation(progress.toFloat())
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    sessionViewModel.saveCanvasState()
                }
            }
        )

        binding.seekBarZoom.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        rotateViewModel.setZoom(progressToZoom(progress))
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    sessionViewModel.saveCanvasState()
                }
            }
        )
    }

    private fun toggleSeekBar(
        showLayout: View,
        hideLayout: View
    ) {
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