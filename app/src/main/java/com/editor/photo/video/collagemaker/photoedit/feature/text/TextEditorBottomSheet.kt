package com.editor.photo.video.collagemaker.photoedit.feature.text

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * PHASE 2 — Compact STYLE-ONLY sheet. NO Cancel / NO Done (per spec).
 *
 * Koi canvas/preview yahan nahi — real [TextOverlayView] [PhotoStudioFragment] ke andar, real
 * photo view par render hoti hai. Ye sheet sirf ek "remote control" hai: har tool-tap FORAN
 * `sessionViewModel.updateText(...)` call karta hai jo asal layer ko commit karta hai — koi
 * draft/apply/confirm step nahi. Fragment `editorState.textLayers` observe karke overlay ko
 * turant update kar deta hai.
 */
class TextEditorBottomSheet : BaseEditorBottomSheet<BottomSheetTextEditorBinding>() {

    private val sessionViewModel: EditorSessionViewModel by activityViewModels()

    private var activeTool: TextToolType = TextToolType.STYLE

    /** Local mirror — sirf UI state ke liye (selection highlight, seekbar progress). */
    private lateinit var working: TextLayer

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        BottomSheetTextEditorBinding.inflate(inflater, container, false)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.let { sheet ->
                sheet.background = null
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.isDraggable = true
                dialog.window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                sheet.layoutParams?.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        }
        return dialog
    }

    override fun setupUI() {
        val activeId = sessionViewModel.activeTextId.value
        val activeLayer = activeId?.let { id ->
            sessionViewModel.editorState.value.textLayers.firstOrNull { it.id == id }
        }
        // Agar active text id nahi mila (process death jaisi edge-case), sheet ka koi matlab nahi.
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

    private var isProgrammaticSeekUpdate = false

    private fun setupTopIntensitySeekBar() {
        binding.sbTextIntensity.max = 200
        val initialProgress = (working.size * 1000).toInt().coerceIn(10, 300)
        isProgrammaticSeekUpdate = true
        binding.sbTextIntensity.progress = initialProgress
        isProgrammaticSeekUpdate = false

        binding.sbTextIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && !isProgrammaticSeekUpdate) {
                    val newSize = (progress / 1000f).coerceIn(0.01f, 0.4f)
                    working = working.copy(size = newSize)
                    syncSizePanelSeekBar(progress)
                    commit()
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

    /** Har style change ke baad ye call hota hai — FORAN real layer commit karta hai. */
    private fun commit() {
        sessionViewModel.updateText(working)
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
                    commit()
                }
            }
        }
        row.addView(styleButton(R.drawable.ic_bold, { working.isBold }) { working = working.copy(isBold = !working.isBold) })
        row.addView(styleButton(R.drawable.ic_italic, { working.isItalic }) { working = working.copy(isItalic = !working.isItalic) })
        row.addView(styleButton(R.drawable.ic_underline, { working.isUnderline }) { working = working.copy(isUnderline = !working.isUnderline) })
        return row
    }

    private fun buildFontPanel(): View {
        val rv = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
        rv.adapter = FontAdapter(initialKey = working.fontFamily) { font ->
            working = working.copy(fontFamily = font.key)
            commit()
        }
        return rv
    }

    private fun buildColorPanel(): View {
        val rv = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
        val initialIndex = TEXT_COLORS.indexOf(working.color).let { if (it < 0) 0 else it }
        rv.adapter = ColorAdapter(TEXT_COLORS, initialIndex) { color ->
            working = working.copy(color = color)
            commit()
        }
        return rv
    }

    private fun buildStrokePanel(): View {
        val column = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }

        if (working.strokeWidth <= 0f) {
            working = working.copy(strokeWidth = 0.012f)
            commit()
        }

        val toggleRow = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val toggle = Switch(requireContext()).apply {
            isChecked = true
            text = getString(R.string.text_editor_stroke_enable)
            setTextColor(Color.WHITE)
        }
        toggleRow.addView(toggle)
        column.addView(toggleRow)

        val seek = SeekBar(requireContext()).apply {
            max = 100
            progress = (working.strokeWidth * 1000).toInt().coerceIn(10, 100)
            isEnabled = true
        }
        val colorRv = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(6), 0, 0) }
        }
        val strokeInitialIndex = TEXT_COLORS.indexOf(working.strokeColor).let { if (it < 0) 0 else it }
        colorRv.adapter = ColorAdapter(TEXT_COLORS, strokeInitialIndex) { color ->
            working = working.copy(strokeColor = color)
            commit()
        }

        toggle.setOnCheckedChangeListener { _, isChecked ->
            seek.isEnabled = isChecked
            working = working.copy(strokeWidth = if (isChecked) (seek.progress / 1000f).coerceAtLeast(0.006f) else 0f)
            commit()
        }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (toggle.isChecked && fromUser) {
                    working = working.copy(strokeWidth = (progress / 1000f).coerceAtLeast(0.006f))
                    commit()
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
                isSelected = working.alignment == align
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginEnd = dp(14) }
                setOnClickListener {
                    working = working.copy(alignment = align)
                    commit()
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
            progress = (working.size * 1000).toInt().coerceIn(10, 300)
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
                    working = working.copy(size = newSize)
                    updateTopIntensitySeekBar(progress)
                    commit()
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

        /** Ab koi `existingTextId` argument nahi chahiye — sheet [sessionViewModel.activeTextId]
         * se khud target layer nikal leti hai (jo Phase 1 ke "Done" ne set kiya tha). */
        fun newInstance(): TextEditorBottomSheet = TextEditorBottomSheet()
    }
}