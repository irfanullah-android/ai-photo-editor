package com.editor.photo.video.collagemaker.photoedit.feature.text

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
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

/**
 * SINGLE bottom sheet for typing AND styling text.
 *
 * There is no separate input dialog anymore (TextInputBottomSheet is gone from this
 * flow). etTextEditorInput lives right here and streams live into the active
 * TextOverlayView through EditorSessionViewModel.updateText(), the same way the
 * seekbar/color/font panels already worked.
 */
class TextEditorBottomSheet : BaseEditorBottomSheet<BottomSheetTextEditorBinding>() {

    private val sessionViewModel: EditorSessionViewModel by activityViewModels()

    private var activeTool: TextToolType = TextToolType.STYLE

    // Nullable on purpose: if the active layer can't be resolved in setupUI(),
    // this stays null and the sheet dismisses itself immediately. onDismiss()
    // must tolerate that instead of assuming a layer was ever bound (that used
    // to be a lateinit and crashed with UninitializedPropertyAccessException
    // when setupUI() bailed out early).
    private var working: TextLayer? = null

    // Remembers the host Activity's window soft-input mode so it can be restored
    // when this sheet closes. We temporarily force the Activity to ADJUST_NOTHING
    // while the sheet is open so the background canvas (photoEditorView) never
    // resizes when the keyboard shows — only this sheet's own window (which keeps
    // ADJUST_RESIZE, set in onCreateDialog) moves. This is the standard editor-app
    // pattern: the canvas stays fixed, the input UI is what slides.
    private var savedActivitySoftInputMode: Int? = null

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        BottomSheetTextEditorBinding.inflate(inflater, container, false)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d(TAG, "onCreateDialog")
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
            // The sheet owns an EditText now, so it must resize/slide when the
            // keyboard shows instead of getting covered by it.
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        return dialog
    }

    override fun onStart() {
        super.onStart()
        // Root cause of the "opens small" bug: this is a plain Dialog (not a real
        // BottomSheetDialog), so onCreateDialog() sets window layout before the
        // content view has been measured. The theme then locks in an initial
        // (small) size. By onStart() the view is attached and measured, so
        // re-forcing WRAP_CONTENT here makes the window take the real content
        // height instead of the stale initial one.
        Log.d(TAG, "onStart -> re-forcing window layout params")
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Root cause of the "canvas shrinks when keyboard opens" bug: the host
        // Activity's own window was also reacting to the keyboard with its
        // default soft-input mode, squashing photoEditorView behind this sheet.
        // Force the Activity to ADJUST_NOTHING for as long as this sheet is open
        // — only this sheet's own window should move, never the screen behind
        // it. Restored in onDestroyView().
        val activityWindow = activity?.window
        if (activityWindow != null && savedActivitySoftInputMode == null) {
            savedActivitySoftInputMode = activityWindow.attributes.softInputMode
            Log.d(TAG, "onStart -> saving activity softInputMode=$savedActivitySoftInputMode, forcing ADJUST_NOTHING")
            activityWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }
    }

    override fun setupUI() {
        Log.d(TAG, "setupUI")
        val activeId = sessionViewModel.activeTextId.value
        val activeLayer = activeId?.let { id ->
            sessionViewModel.editorState.value.textLayers.firstOrNull { it.id == id }
        }
        if (activeLayer == null) {
            Log.d(TAG, "setupUI: no active layer found for id=$activeId, dismissing")
            dismiss()
            return
        }
        working = activeLayer

        bindTextInput(activeLayer)
        setupTopIntensitySeekBar()
        setupToolsRecyclerView()
        showPanelFor(activeTool)

        binding.etTextEditorInput.requestFocus()
        binding.etTextEditorInput.setSelection(binding.etTextEditorInput.text?.length ?: 0)
        showKeyboard()

        // Extra safety net: after this layout pass (tools/panels now inflated),
        // force the window size one more time in case the keyboard/resize
        // sequence squashed it back down.
        binding.root.post {
            Log.d(TAG, "post-layout re-force, rootHeight=${binding.root.height}")
            dialog?.window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    /**
     * Fetches the up-to-date TextLayer from EditorSessionViewModel to ensure canvas transformations
     * (like size, scale, translation, rotation) are preserved before applying sheet mutations.
     * Returns null only in the edge case where setupUI() never managed to bind a
     * layer (active id didn't resolve) — callers that run after a normal bind
     * (bindTextInput, panels, etc.) can safely assume non-null via requireWorkingLayer().
     */
    private fun getLatestWorkingLayer(): TextLayer? {
        val activeId = sessionViewModel.activeTextId.value
        return activeId?.let { id ->
            sessionViewModel.editorState.value.textLayers.firstOrNull { it.id == id }
        } ?: working
    }

    /**
     * Same as getLatestWorkingLayer() but for call sites that only ever run after
     * setupUI() has successfully bound `working` (panels, seekbars, mutateAndCommit).
     * Those paths are unreachable before a successful bind because setupUI()
     * dismisses immediately when the layer can't be resolved.
     */
    private fun requireWorkingLayer(): TextLayer =
        getLatestWorkingLayer() ?: working ?: error("TextEditorBottomSheet: working layer accessed before bind")

    /**
     * Helper that merges local sheet mutations with the latest ViewModel state before updating.
     */
    private inline fun mutateAndCommit(transform: TextLayer.() -> TextLayer) {
        val latest = requireWorkingLayer()
        val updated = latest.transform()
        working = updated
        sessionViewModel.updateText(updated)
    }

    // ---------------------------------------------------------------------
    // Live text input + clear / cancel / confirm
    // ---------------------------------------------------------------------

    private var isProgrammaticTextUpdate = false
    private var wasCancelled = false

    private fun bindTextInput(layer: TextLayer) {
        setEditTextTextSilently(layer.text)

        binding.etTextEditorInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isProgrammaticTextUpdate) return
                val newText = s?.toString().orEmpty()
                mutateAndCommit { copy(text = newText) }
            }
        })

        binding.btnTextClear.setOnClickListener {
            binding.etTextEditorInput.text?.clear()
            binding.etTextEditorInput.requestFocus()
        }

        binding.btnTextCancel.setOnClickListener {
            Log.d(TAG, "cancel tapped")
            wasCancelled = true
            hideKeyboard()
            dismiss()
        }

        binding.btnTextConfirm.setOnClickListener {
            Log.d(TAG, "confirm tapped")
            hideKeyboard()
            dismiss()
        }
    }

    /**
     * Only call this to push state INTO the EditText (initial bind, switching to a
     * different layer, or an external mutation like undo). Never call it from inside
     * the TextWatcher's own afterTextChanged — that's the state-loop that causes
     * cursor jumping. Skips the write entirely if the text already matches, and
     * clamps the restored selection to the new string's length otherwise.
     */
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

    // ---------------------------------------------------------------------
    // Size slider (top, always visible)
    // ---------------------------------------------------------------------

    private var isProgrammaticSeekUpdate = false

    private fun setupTopIntensitySeekBar() {
        binding.sbTextIntensity.max = 200
        val initialProgress = (requireWorkingLayer().size * 1000).toInt().coerceIn(10, 300)
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
        // No "+ Add Text" capsule anymore — this sheet only ever edits the single
        // active layer it was opened for.
    }

    private fun setupToolsRecyclerView() {
        binding.rvTools.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvTools.adapter = TextToolAdapter(TextToolType.values().toList()) { tool ->
            Log.d(TAG, "tool selected: $tool")
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
        val currentLayer = requireWorkingLayer()
        val initialIndex = TEXT_COLORS.indexOf(currentLayer.color).let { if (it < 0) 0 else it }
        rv.adapter = ColorAdapter(TEXT_COLORS, initialIndex) { color ->
            mutateAndCommit { copy(color = color) }
        }
        return rv
    }

    private fun buildStrokePanel(): View {
        val column = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        var strokeLayer = requireWorkingLayer()

        if (strokeLayer.strokeWidth <= 0f) {
            mutateAndCommit { copy(strokeWidth = 0.012f) }
            strokeLayer = requireWorkingLayer()
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
            progress = (requireWorkingLayer().size * 1000).toInt().coerceIn(10, 300)
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
        Log.d(TAG, "onDismiss, wasCancelled=$wasCancelled")
        // getLatestWorkingLayer() can legitimately be null here: setupUI() calls
        // dismiss() itself when the active layer id didn't resolve to anything in
        // editorState.textLayers, and in that case `working` was never bound.
        // There's nothing to clean up on the layer list in that case — just make
        // sure activeTextId doesn't stay pointed at a ghost id.
        val latest = getLatestWorkingLayer()
        if (latest == null) {
            Log.d(TAG, "onDismiss: no working layer was ever bound, nothing to clean up")
            sessionViewModel.setActiveTextId(null)
            return
        }
        // Cancel button, or backing out with nothing typed: drop the layer instead of
        // leaving an invisible empty overlay on the canvas.
        if (wasCancelled || latest.text.isBlank()) {
            sessionViewModel.removeText(latest.id)
        }
        sessionViewModel.setActiveTextId(null)
    }

    override fun onDestroyView() {
        // Restore the Activity's original soft-input mode before this fragment's
        // view (and binding) go away, so the rest of the editor behaves normally
        // again once the text sheet is closed.
        savedActivitySoftInputMode?.let { mode ->
            Log.d(TAG, "onDestroyView -> restoring activity softInputMode=$mode")
            activity?.window?.setSoftInputMode(mode)
        }
        savedActivitySoftInputMode = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "TextEditorBottomSheet"

        private val TEXT_COLORS = listOf(
            Color.WHITE, Color.BLACK, Color.RED,
            Color.parseColor("#FF5722"), Color.parseColor("#FFEB3B"), Color.parseColor("#4CAF50"),
            Color.parseColor("#2196F3"), Color.parseColor("#9C27B0"), Color.parseColor("#E91E63"),
            Color.parseColor("#00BCD4"), Color.parseColor("#FF9800"), Color.parseColor("#8BC34A"),
            Color.parseColor("#3F51B5"), Color.parseColor("#FFC107"), Color.parseColor("#795548"),
            Color.GRAY
        )

        /** Opens for the currently active text layer (see EditorSessionViewModel.activeTextId). */
        fun newInstance(): TextEditorBottomSheet = TextEditorBottomSheet()
    }
}