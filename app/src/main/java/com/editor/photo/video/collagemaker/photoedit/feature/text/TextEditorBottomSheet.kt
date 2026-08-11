package com.editor.photo.video.collagemaker.photoedit.feature.text

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter.ColorAdapter
import com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter.FontAdapter
import com.editor.photo.video.collagemaker.photoedit.core.BaseEditorBottomSheet
import com.editor.photo.video.collagemaker.photoedit.databinding.BottomSheetTextEditorBinding
import com.editor.photo.video.collagemaker.photoedit.editor.session.EditorSessionViewModel
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.TextAlignment
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.TextLayer

class TextEditorBottomSheet : BaseEditorBottomSheet<BottomSheetTextEditorBinding>() {

    private val sessionViewModel: EditorSessionViewModel by activityViewModels()

    private var activeTool: TextToolType = TextToolType.STYLE

    private lateinit var working: TextLayer

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        BottomSheetTextEditorBinding.inflate(inflater, container, false)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(false)

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.BOTTOM)
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0f)
        }

        return dialog
    }

    override fun setupUI() {
        val activeId = sessionViewModel.activeTextId.value
        val activeLayer = activeId?.let { id ->
            sessionViewModel.editorState.value.textLayers.firstOrNull { it.id == id }
        }
        if (activeLayer == null) {
            dismiss()
            return
        }
        working = activeLayer

        binding.btnAddTextCapsule.setOnClickListener {
            dismiss()
            TextInputBottomSheet.newInstance(null).show(parentFragmentManager, "text_input")
        }

        setupTopIntensitySeekBar()
        setupToolsRecyclerView()
        showPanelFor(activeTool)
    }

    /**
     * Fetches the up-to-date TextLayer from EditorSessionViewModel to ensure canvas transformations
     * (like size, scale, translation, rotation) are preserved before applying sheet mutations.
     */
    private fun getLatestWorkingLayer(): TextLayer {
        val activeId = sessionViewModel.activeTextId.value
        return activeId?.let { id ->
            sessionViewModel.editorState.value.textLayers.firstOrNull { it.id == id }
        } ?: working
    }

    /**
     * Helper that merges local sheet mutations with the latest ViewModel state before updating.
     */
    private inline fun mutateAndCommit(transform: TextLayer.() -> TextLayer) {
        val latest = getLatestWorkingLayer()
        val updated = latest.transform()
        working = updated
        sessionViewModel.updateText(updated)
    }

    private var isProgrammaticSeekUpdate = false

    private fun setupTopIntensitySeekBar() {
        binding.sbTextIntensity.max = 200
        val initialProgress = (getLatestWorkingLayer().size * 1000).toInt().coerceIn(10, 300)
        isProgrammaticSeekUpdate = true
        binding.sbTextIntensity.progress = initialProgress
        isProgrammaticSeekUpdate = false

        binding.sbTextIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !isProgrammaticSeekUpdate) {
                    val newSize = (progress / 1000f).coerceIn(0.01f, 0.4f)
                    mutateAndCommit { copy(size = newSize) }
                    syncSizePanelSeekBar(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private var sizePanelSeekBar: SeekBar? = null

    private fun syncSizePanelSeekBar(progress: Int) {
        sizePanelSeekBar?.let { seek ->
            isProgrammaticSeekUpdate = true
            seek.progress = progress
            isProgrammaticSeekUpdate = false
        }
    }

    private fun updateTopIntensitySeekBar(progress: Int) {
        isProgrammaticSeekUpdate = true
        binding.sbTextIntensity.progress = progress
        isProgrammaticSeekUpdate = false
    }

    override fun setupListeners() {
        binding.btnAddTextCapsule.setOnClickListener {
            sessionViewModel.setActiveTextId(null)
            dismiss()
            TextInputBottomSheet.newInstance(null).show(parentFragmentManager, "text_input")
        }
    }

    private fun setupToolsRecyclerView() {
        binding.rvTools.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvTools.adapter = TextToolAdapter(TextToolType.values().toList()) { tool ->
            activeTool = tool
            showPanelFor(tool)
        }
    }

    private fun showPanelFor(tool: TextToolType) {
        val container = binding.toolContentContainer
        container.removeAllViews()
        val panel = when (tool) {
            TextToolType.STYLE -> buildStylePanel()
            TextToolType.FONT -> buildFontPanel()
            TextToolType.COLOR -> buildColorPanel()
            TextToolType.STROKE -> buildStrokePanel()
            TextToolType.ALIGN -> buildAlignPanel()
            TextToolType.SIZE -> buildSizePanel()
        }
        container.addView(panel)
    }

    private fun buildStylePanel(): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        fun styleButton(iconRes: Int, isActive: () -> Boolean, onToggle: () -> Unit): ImageButton {
            return ImageButton(requireContext()).apply {
                setImageResource(iconRes)
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_font_item_selector)
                setPadding(dp(10), dp(10), dp(10), dp(10))
                isSelected = isActive()
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginEnd = dp(14) }
                setOnClickListener {
                    onToggle()
                    isSelected = isActive()
                }
            }
        }
        val layer = getLatestWorkingLayer()
        row.addView(styleButton(R.drawable.ic_bold, { getLatestWorkingLayer().isBold }) {
            mutateAndCommit { copy(isBold = !isBold) }
        })
        row.addView(styleButton(R.drawable.ic_italic, { getLatestWorkingLayer().isItalic }) {
            mutateAndCommit { copy(isItalic = !isItalic) }
        })
        row.addView(styleButton(R.drawable.ic_underline, { getLatestWorkingLayer().isUnderline }) {
            mutateAndCommit { copy(isUnderline = !isUnderline) }
        })
        return row
    }

    private fun buildFontPanel(): View {
        val rv = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
        rv.adapter = FontAdapter(initialKey = getLatestWorkingLayer().fontFamily) { font ->
            mutateAndCommit { copy(fontFamily = font.key) }
        }
        return rv
    }

    private fun buildColorPanel(): View {
        val rv = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
        val currentLayer = getLatestWorkingLayer()
        val initialIndex = TEXT_COLORS.indexOf(currentLayer.color).let { if (it < 0) 0 else it }
        rv.adapter = ColorAdapter(TEXT_COLORS, initialIndex) { color ->
            mutateAndCommit { copy(color = color) }
        }
        return rv
    }

    private fun buildStrokePanel(): View {
        val column = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        var strokeLayer = getLatestWorkingLayer()

        if (strokeLayer.strokeWidth <= 0f) {
            mutateAndCommit { copy(strokeWidth = 0.012f) }
            strokeLayer = getLatestWorkingLayer()
        }

        val toggleRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val toggle = Switch(requireContext()).apply {
            isChecked = strokeLayer.strokeWidth > 0f
            text = getString(R.string.text_editor_stroke_enable)
            setTextColor(Color.WHITE)
        }
        toggleRow.addView(toggle)
        column.addView(toggleRow)

        val seek = SeekBar(requireContext()).apply {
            max = 100
            progress = (strokeLayer.strokeWidth * 1000).toInt().coerceIn(10, 100)
            isEnabled = toggle.isChecked
        }
        val colorRv = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(6), 0, 0) }
        }
        val strokeInitialIndex = TEXT_COLORS.indexOf(strokeLayer.strokeColor).let { if (it < 0) 0 else it }
        colorRv.adapter = ColorAdapter(TEXT_COLORS, strokeInitialIndex) { color ->
            mutateAndCommit { copy(strokeColor = color) }
        }

        toggle.setOnCheckedChangeListener { _, isChecked ->
            seek.isEnabled = isChecked
            mutateAndCommit {
                copy(strokeWidth = if (isChecked) (seek.progress / 1000f).coerceAtLeast(0.006f) else 0f)
            }
        }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (toggle.isChecked && fromUser) {
                    mutateAndCommit { copy(strokeWidth = (progress / 1000f).coerceAtLeast(0.006f)) }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        column.addView(seek)
        column.addView(colorRv)
        return column
    }

    private fun buildAlignPanel(): View {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        fun alignButton(iconRes: Int, align: TextAlignment): ImageButton {
            return ImageButton(requireContext()).apply {
                setImageResource(iconRes)
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_font_item_selector)
                setPadding(dp(10), dp(10), dp(10), dp(10))
                isSelected = getLatestWorkingLayer().alignment == align
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginEnd = dp(14) }
                setOnClickListener {
                    mutateAndCommit { copy(alignment = align) }
                    (parent as? LinearLayout)?.let { parentRow ->
                        for (i in 0 until parentRow.childCount) {
                            parentRow.getChildAt(i).isSelected = (parentRow.getChildAt(i) === this)
                        }
                    }
                }
            }
        }
        row.addView(alignButton(R.drawable.ic_align_left, TextAlignment.LEFT))
        row.addView(alignButton(R.drawable.ic_align_center, TextAlignment.CENTER))
        row.addView(alignButton(R.drawable.ic_align_right, TextAlignment.RIGHT))
        return row
    }

    private fun buildSizePanel(): View {
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        val seek = SeekBar(requireContext()).apply {
            max = 200
            progress = (getLatestWorkingLayer().size * 1000).toInt().coerceIn(10, 300)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        sizePanelSeekBar = seek
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !isProgrammaticSeekUpdate) {
                    val newSize = (progress / 1000f).coerceIn(0.01f, 0.4f)
                    mutateAndCommit { copy(size = newSize) }
                    updateTopIntensitySeekBar(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        container.addView(seek)
        return container
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        sessionViewModel.setActiveTextId(null)
    }

    companion object {
        private val TEXT_COLORS = listOf(
            Color.WHITE, Color.BLACK, Color.RED,
            Color.parseColor("#FF5722"), Color.parseColor("#FFEB3B"), Color.parseColor("#4CAF50"),
            Color.parseColor("#2196F3"), Color.parseColor("#9C27B0"), Color.parseColor("#E91E63"),
            Color.parseColor("#00BCD4"), Color.parseColor("#FF9800"), Color.parseColor("#8BC34A"),
            Color.parseColor("#3F51B5"), Color.parseColor("#FFC107"), Color.parseColor("#795548"),
            Color.GRAY
        )

        fun newInstance(): TextEditorBottomSheet = TextEditorBottomSheet()
    }
}