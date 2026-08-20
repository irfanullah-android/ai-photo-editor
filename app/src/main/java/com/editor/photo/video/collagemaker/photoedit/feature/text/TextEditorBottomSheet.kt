package com.editor.photo.video.collagemaker.photoedit.feature.text

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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

    private var activeTool: TextToolType = TextToolType.COLOR
    private var working: TextLayer? = null
    private var savedActivitySoftInputMode: Int? = null
    private var isProgrammaticTextUpdate = false
    private var wasCancelled = false

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        BottomSheetTextEditorBinding.inflate(inflater, container, false)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setGravity(Gravity.BOTTOM)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setDimAmount(0f)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val activityWindow = activity?.window
        if (activityWindow != null && savedActivitySoftInputMode == null) {
            savedActivitySoftInputMode = activityWindow.attributes.softInputMode
            activityWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        }
    }

    override fun setupUI() {
        val activeId = sessionViewModel.activeTextId.value
        val activeLayer = activeId?.let { id ->
            sessionViewModel.editorState.value.textLayers.firstOrNull { it.id == id }
        }
        if (activeLayer == null) { dismiss(); return }
        working = activeLayer

        setToolsOnlyMode()
        setupToolsRecyclerView()
        showPanelFor(activeTool)
        bindTextInput(activeLayer)

        binding.root.post {
            dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun setToolsOnlyMode() {
        binding.textInputContainer.visibility    = View.GONE
        binding.divider1.visibility              = View.GONE
        binding.toolContentContainer.visibility  = View.VISIBLE
        binding.divider2.visibility              = View.VISIBLE
        binding.bottomToolbarContainer.visibility = View.VISIBLE
    }

    private fun setEditInputMode() {
        binding.textInputContainer.visibility    = View.VISIBLE
        binding.divider1.visibility              = View.VISIBLE
        binding.toolContentContainer.visibility  = View.GONE
        binding.divider2.visibility              = View.GONE
        binding.bottomToolbarContainer.visibility = View.GONE
        binding.etTextEditorInput.requestFocus()
        showKeyboard()
    }

    private fun setConfirmedMode() {
        binding.textInputContainer.visibility    = View.GONE
        binding.divider1.visibility              = View.GONE
        binding.toolContentContainer.visibility  = View.VISIBLE
        binding.divider2.visibility              = View.VISIBLE
        binding.bottomToolbarContainer.visibility = View.VISIBLE
        hideKeyboard()
    }

    fun enterEditMode() {
        setEditInputMode()
    }

    private fun getLatestWorkingLayer(): TextLayer? {
        val activeId = sessionViewModel.activeTextId.value
        return activeId?.let { id ->
            sessionViewModel.editorState.value.textLayers.firstOrNull { it.id == id }
        } ?: working
    }

    private fun requireWorkingLayer(): TextLayer =
        getLatestWorkingLayer() ?: working ?: error("TextEditorBottomSheet: working layer accessed before bind")

    private inline fun mutateAndCommit(transform: TextLayer.() -> TextLayer) {
        val updated = requireWorkingLayer().transform()
        working = updated
        sessionViewModel.updateText(updated)
    }

    private fun bindTextInput(layer: TextLayer) {
        setEditTextTextSilently(layer.text)

        binding.etTextEditorInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isProgrammaticTextUpdate) return
                mutateAndCommit { copy(text = s?.toString().orEmpty()) }
            }
        })

        binding.etTextEditorInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                setConfirmedMode()
                true
            } else false
        }

        binding.btnTextClear.setOnClickListener {
            binding.etTextEditorInput.text?.clear()
            binding.etTextEditorInput.requestFocus()
        }

        binding.btnTextCancel.setOnClickListener {
            wasCancelled = true
            hideKeyboard()
            setToolsOnlyMode()
        }

        binding.btnTextConfirm.setOnClickListener {
            setConfirmedMode()
        }
    }

    private fun setEditTextTextSilently(text: String) {
        val current = binding.etTextEditorInput.text?.toString().orEmpty()
        if (current == text) return
        val selection = binding.etTextEditorInput.selectionStart.coerceIn(0, text.length)
        isProgrammaticTextUpdate = true
        binding.etTextEditorInput.setText(text)
        binding.etTextEditorInput.setSelection(selection)
        isProgrammaticTextUpdate = false
    }

    private fun showKeyboard() {
        binding.etTextEditorInput.post {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(binding.etTextEditorInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.etTextEditorInput.windowToken, 0)
    }

    override fun setupListeners() {}

    private fun setupToolsRecyclerView() {
        binding.rvTools.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvTools.adapter = TextToolAdapter(TextToolType.values().toList()) { tool ->
            activeTool = tool
            hideKeyboard()
            showPanelFor(tool)
            setToolsOnlyMode()
        }
    }

    private fun showPanelFor(tool: TextToolType) {
        val container = binding.toolContentContainer
        container.removeAllViews()
        container.addView(
            when (tool) {
                TextToolType.STYLE  -> buildStylePanel()
                TextToolType.FONT   -> buildFontPanel()
                TextToolType.COLOR  -> buildColorPanel()
                TextToolType.STROKE -> buildStrokePanel()
                TextToolType.ALIGN  -> buildAlignPanel()
                TextToolType.SIZE   -> buildSizePanel()
            }
        )
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
        row.addView(styleButton(R.drawable.ic_bold, { requireWorkingLayer().isBold }) {
            mutateAndCommit { copy(isBold = !isBold) }
        })
        row.addView(styleButton(R.drawable.ic_italic, { requireWorkingLayer().isItalic }) {
            mutateAndCommit { copy(isItalic = !isItalic) }
        })
        row.addView(styleButton(R.drawable.ic_underline, { requireWorkingLayer().isUnderline }) {
            mutateAndCommit { copy(isUnderline = !isUnderline) }
        })
        return row
    }

    private fun buildFontPanel(): View {
        val rv = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
        rv.adapter = FontAdapter(initialKey = requireWorkingLayer().fontFamily) { font ->
            mutateAndCommit { copy(fontFamily = font.key) }
        }
        return rv
    }

    private fun buildColorPanel(): View {
        val rv = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
        val initialIndex = TEXT_COLORS.indexOf(requireWorkingLayer().color).let { if (it < 0) 0 else it }
        rv.adapter = ColorAdapter(TEXT_COLORS, initialIndex) { color ->
            mutateAndCommit { copy(color = color) }
        }
        return rv
    }

    private fun buildStrokePanel(): View {
        val column = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        if (requireWorkingLayer().strokeWidth <= 0f) {
            mutateAndCommit { copy(strokeWidth = 0.012f) }
        }
        val strokeLayer = requireWorkingLayer()

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
                isSelected = requireWorkingLayer().alignment == align
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
        row.addView(alignButton(R.drawable.ic_align_left,   TextAlignment.LEFT))
        row.addView(alignButton(R.drawable.ic_align_center, TextAlignment.CENTER))
        row.addView(alignButton(R.drawable.ic_align_right,  TextAlignment.RIGHT))
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
            progress = (requireWorkingLayer().size * 1000).toInt().coerceIn(10, 300)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mutateAndCommit { copy(size = (progress / 1000f).coerceIn(0.01f, 0.4f)) }
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
        val latest = getLatestWorkingLayer()
        if (latest == null) {
            sessionViewModel.setActiveTextId(null)
            return
        }
        if (wasCancelled || latest.text.isBlank()) {
            sessionViewModel.removeText(latest.id)
        }
        sessionViewModel.setActiveTextId(null)
    }

    override fun onDestroyView() {
        savedActivitySoftInputMode?.let { activity?.window?.setSoftInputMode(it) }
        savedActivitySoftInputMode = null
        super.onDestroyView()
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