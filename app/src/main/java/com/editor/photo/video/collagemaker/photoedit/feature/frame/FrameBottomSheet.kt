package com.editor.photo.video.collagemaker.photoedit.feature.frame

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter.FrameAdapter
import com.editor.photo.video.collagemaker.photoedit.databinding.BottomSheetFrameBinding
import com.editor.photo.video.collagemaker.photoedit.editor.session.EditorSessionViewModel
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.FrameLayer
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.FrameModel
import ja.burhanrashid52.photoeditor.PhotoEditorView
import java.util.UUID

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
        val frames = getFramesList()

        binding.rvFrames.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )

        frameAdapter = FrameAdapter(
            frames = frames,
            originalBitmap = originalBitmap
        ) { frame ->
            frameViewModel.selectFrame(frame)
        }

        binding.rvFrames.adapter = frameAdapter
    }

    private fun getFramesList(): List<FrameModel> {
        return listOf(
            FrameModel("None", R.drawable.ic_no_frame),
            FrameModel("Classic", R.drawable.frame_classic),
            FrameModel("Modern", R.drawable.frame_modern),
            FrameModel("Vintage", R.drawable.frame_vintage),
            FrameModel("Polaroid", R.drawable.frame_polaroid),
            FrameModel("Film", R.drawable.frame_film),
            FrameModel("Wood", R.drawable.frame_wood),
            FrameModel("Metal", R.drawable.frame_metal),
            FrameModel("Gold", R.drawable.frame_gold),
            FrameModel("Silver", R.drawable.frame_silver),
            FrameModel("Black", R.drawable.frame_black),
            FrameModel("White", R.drawable.frame_white)
        )
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
                        UUID.randomUUID().toString(),
                        selected.frameRes,
                        padding.toFloat()
                    )
                )
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
            val padding = calculatePadding(frame.name)
            val topPadding = padding
            val bottomPadding = if (frame.name == "Polaroid") 90 else padding
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

            canvas.drawColor(frameColor)
            canvas.drawBitmap(bitmap, leftPadding.toFloat(), topPadding.toFloat(), null)
            addFrameShadow(canvas, leftPadding, topPadding, bitmap.width, bitmap.height)

            result
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    private fun addFrameShadow(canvas: Canvas, left: Int, top: Int, width: Int, height: Int) {
        val shadowPaint = Paint().apply {
            color = Color.BLACK
            alpha = 50
            style = Paint.Style.STROKE
            strokeWidth = 2f
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
