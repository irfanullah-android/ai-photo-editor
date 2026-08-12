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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter.FrameAdapter
import com.editor.photo.video.collagemaker.photoedit.databinding.BottomSheetFrameBinding
import com.editor.photo.video.collagemaker.photoedit.editor.session.EditorSessionViewModel
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.FrameLayer
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.FrameModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import ja.burhanrashid52.photoeditor.PhotoEditorView
import java.util.UUID
import kotlin.math.min

@AndroidEntryPoint
class FrameBottomSheet : BottomSheetDialogFragment() {

    private val sessionViewModel: EditorSessionViewModel by activityViewModels()
    private val frameViewModel: FrameViewModel by viewModels()

    private lateinit var binding: BottomSheetFrameBinding
    private var photoEditorView: PhotoEditorView? = null
    private var baseBitmap: Bitmap? = null

    private var originalBitmap: Bitmap? = null
    private var frameAdapter: FrameAdapter? = null
    private var wasApplied = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetFrameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            originalBitmap = baseBitmap?.copy(Bitmap.Config.ARGB_8888, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        setupFrameRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupFrameRecyclerView() {
        binding.rvFrames.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )

        lifecycleScope.launchWhenStarted {
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

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            frameViewModel.selectedFrame.collect { frame ->
                frame?.let { applyFrame(it) }
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
                        padding = padding.toFloat()
                    )
                )
                wasApplied = true
            } else if (selected?.name == "None") {
                // Clear frame if user selected None
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

            // Dynamic scale so high-res image doesn't crush the padding
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

            val frameColor = when (frame.name) {
                "Classic" -> Color.parseColor("#8B4513")
                "Modern" -> Color.parseColor("#2C3E50")
                "Vintage" -> Color.parseColor("#D4A574")
                "Gold" -> Color.parseColor("#FFD700")
                "Silver" -> Color.parseColor("#C0C0C0")
                "Wood" -> Color.parseColor("#8B4513")
                "Metal" -> Color.parseColor("#708090")
                "Black" -> Color.parseColor("#000000")
                "White" -> Color.parseColor("#FFFFFF")
                "Polaroid" -> Color.parseColor("#F5F5DC")
                "Film" -> Color.parseColor("#1C1C1C")
                else -> Color.WHITE
            }

            // 1. Fill canvas background with frame color
            canvas.drawColor(frameColor)

            // 2. Draw original bitmap strictly in the center region
            canvas.drawBitmap(bitmap, leftPadding.toFloat(), topPadding.toFloat(), null)

            // 3. Draw subtle inner shadow
            addFrameShadow(canvas, leftPadding, topPadding, bitmap.width, bitmap.height, scaleFactor)

            result
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    private fun addFrameShadow(
        canvas: Canvas,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        scaleFactor: Float = 1.0f
    ) {
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = 40
            style = Paint.Style.STROKE
            strokeWidth = (2f * scaleFactor).coerceAtLeast(1f)
        }

        canvas.drawRect(
            left.toFloat(),
            top.toFloat(),
            (left + width).toFloat(),
            (top + height).toFloat(),
            shadowPaint
        )
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // If user cancelled (did not press check), restore original image preview
        if (!wasApplied) {
            val original = originalBitmap
            val editorView = photoEditorView
            if (original != null && editorView != null) {
                editorView.source.setImageBitmap(original)
            }
        }
    }

    override fun onDestroyView() {
        frameAdapter?.cleanup()
        frameAdapter = null
        originalBitmap?.recycle()
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