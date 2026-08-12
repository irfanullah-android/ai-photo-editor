package com.editor.photo.video.collagemaker.photoedit.feature.frame

import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter.FrameAdapter
import com.editor.photo.video.collagemaker.photoedit.core.BaseEditorBottomSheet
import com.editor.photo.video.collagemaker.photoedit.databinding.BottomSheetFrameBinding
import com.editor.photo.video.collagemaker.photoedit.editor.session.EditorSessionViewModel
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.FrameLayer
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.FrameModel
import dagger.hilt.android.AndroidEntryPoint
import ja.burhanrashid52.photoeditor.PhotoEditorView
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.min

@AndroidEntryPoint
class FrameBottomSheet : BaseEditorBottomSheet<BottomSheetFrameBinding>() {

    private val sessionViewModel: EditorSessionViewModel by activityViewModels()
    private val frameViewModel: FrameViewModel by viewModels()

    private var photoEditorView: PhotoEditorView? = null
    private var baseBitmap: Bitmap? = null

    private var originalBitmap: Bitmap? = null
    private var frameAdapter: FrameAdapter? = null
    private var wasApplied = false

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): BottomSheetFrameBinding {
        return BottomSheetFrameBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        try {
            originalBitmap = baseBitmap?.copy(Bitmap.Config.ARGB_8888, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        setupFrameRecyclerView()
        observeViewModel()
    }

    override fun setupListeners() {
        setupClickListeners()
    }

    private fun setupFrameRecyclerView() {
        binding.rvFrames.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                frameViewModel.frames.collect { frames ->
                    if (frames.isEmpty()) return@collect
                    if (frameAdapter == null) {
                        frameAdapter = FrameAdapter(
                            frames = frames,
                            originalBitmap = originalBitmap
                        ) { frame ->
                            frameViewModel.selectFrame(frame)
                        }
                        binding.rvFrames.adapter = frameAdapter
                    }
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                frameViewModel.selectedFrame.collect { frame ->
                    frame?.let { applyFrame(it) }
                }
            }
        }
    }

    private fun applyFrame(frame: FrameModel) {
        val original = originalBitmap ?: return
        val editorView = photoEditorView ?: return

        if (frame.name == "None") {
            editorView.source.setImageBitmap(original)
            return
        }

        try {
            val framedBitmap = createFramedBitmap(original, frame)
            framedBitmap?.let {
                editorView.source.setImageBitmap(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            editorView.source.setImageBitmap(original)
        }
    }

    private fun setupClickListeners() {
        binding.ivCheck.setOnClickListener {
            val selected = frameViewModel.selectedFrame.value
            if (selected != null && selected.name != "None") {
                val padding = calculatePadding(selected.name)
                sessionViewModel.applyFrame(
                    FrameLayer(
                        id = UUID.randomUUID().toString(),
                        resourceId = selected.frameRes,
                        padding = padding.toFloat(),
                        frameName = selected.name
                    )
                )
                wasApplied = true
            } else if (selected?.name == "None") {
                sessionViewModel.applyFrame(null)
                wasApplied = true
            }
            dismiss()
        }

        binding.ivClose.setOnClickListener {
            dismiss()
        }
    }

    private fun calculatePadding(name: String): Int {
        return when (name) {
            "Classic" -> 45
            "Vintage" -> 55
            "Gold" -> 52
            "Silver" -> 45
            "Wood" -> 45
            "Film" -> 45
            "Metal" -> 38
            "Modern" -> 32
            "Black" -> 36
            "White" -> 38
            "Polaroid" -> 35
            else -> 40
        }
    }

    private fun createFramedBitmap(bitmap: Bitmap, frame: FrameModel): Bitmap? {
        return try {
            val rawPadding = calculatePadding(frame.name)

            val referenceSize = 1080f
            val minDim = min(bitmap.width, bitmap.height).toFloat()
            val scaleFactor = (minDim / referenceSize).coerceAtLeast(1.0f)

            val padding = (rawPadding * scaleFactor).toInt()
            val topPadding = padding
            val bottomPadding = if (frame.name == "Polaroid") (padding * 2.2f).toInt() else padding
            val leftPadding = padding
            val rightPadding = padding

            val resultWidth = bitmap.width + leftPadding + rightPadding
            val resultHeight = bitmap.height + topPadding + bottomPadding

            val result = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)

            // 1. Frame drawable PEHLE draw karo
            val frameDrawable = try {
                androidx.core.content.ContextCompat.getDrawable(requireContext(), frame.frameRes)
            } catch (e: Exception) {
                null
            }
            if (frameDrawable != null) {
                frameDrawable.setBounds(0, 0, resultWidth, resultHeight)
                frameDrawable.draw(canvas)
            }

            // 2. Image UPAR draw karo
            canvas.drawBitmap(bitmap, leftPadding.toFloat(), topPadding.toFloat(), null)

            result
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }


    }


    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!wasApplied) {
            sessionViewModel.refreshPreview()
        }
    }

    override fun onDestroyView() {
        frameAdapter?.cleanup()
        frameAdapter = null


        photoEditorView?.source?.setImageDrawable(null)

        originalBitmap = null
        photoEditorView = null
        baseBitmap = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(
            baseBitmap: Bitmap,
            photoEditorView: PhotoEditorView,
            onFrameApplied: (Bitmap) -> Unit
        ): FrameBottomSheet {
            return FrameBottomSheet().apply {
                this.baseBitmap = baseBitmap
                this.photoEditorView = photoEditorView
            }
        }
    }
}