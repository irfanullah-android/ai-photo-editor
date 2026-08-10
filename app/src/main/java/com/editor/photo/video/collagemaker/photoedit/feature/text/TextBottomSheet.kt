package com.editor.photo.video.collagemaker.photoedit.feature.text

import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter.ColorAdapter
import com.editor.photo.video.collagemaker.photoedit.core.BaseEditorBottomSheet
import com.editor.photo.video.collagemaker.photoedit.databinding.BottomSheetTextBinding
import com.editor.photo.video.collagemaker.photoedit.editor.session.EditorSessionViewModel
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.TextLayer
import java.util.UUID
import androidx.recyclerview.widget.LinearLayoutManager

/**
 * TextBottomSheet — collects text content and style from the user, then commits a [TextLayer]
 * to [EditorSessionViewModel] (→ [ApplyTextUseCase] → [EditorRepository] → [HistoryManager])
 * when the user taps "Done".
 *
 * The PhotoEditor library is NOT used here. The resulting [TextLayer] is the only source of
 * truth; EditorEngine renders it during preview and high-res export using normalized coordinates.
 */
class TextBottomSheet : BaseEditorBottomSheet<BottomSheetTextBinding>() {

    private val sessionViewModel: EditorSessionViewModel by activityViewModels()

    /** Non-null when editing an existing layer; null when adding a new one. */
    private var existingTextId: String? = null

    private var selectedTextColor: Int = Color.WHITE
    private var textColorAdapter: ColorAdapter? = null
    private var isBold = false
    private var isItalic = false

    private val textColorList = listOf(
        Color.WHITE,
        Color.BLACK,
        Color.RED,
        Color.parseColor("#FF5722"),
        Color.parseColor("#FFEB3B"),
        Color.parseColor("#4CAF50"),
        Color.parseColor("#2196F3"),
        Color.parseColor("#9C27B0"),
        Color.parseColor("#E91E63"),
        Color.parseColor("#00BCD4"),
        Color.parseColor("#FF9800"),
        Color.parseColor("#8BC34A"),
        Color.parseColor("#3F51B5"),
        Color.parseColor("#FFC107"),
        Color.parseColor("#795548"),
        Color.GRAY
    )

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?) =
        BottomSheetTextBinding.inflate(inflater, container, false)

    override fun setupUI() {
        setupTextColorRecyclerView()
        setupTextWatcher()
        updateEditTextStyle()
    }

    private fun setupTextColorRecyclerView() {
        binding.rvTextColors.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        textColorAdapter = ColorAdapter(textColorList, 0) { color ->
            selectedTextColor = color
            updateEditTextStyle()
        }

        binding.rvTextColors.adapter = textColorAdapter
    }

    private fun setupTextWatcher() {
        binding.etText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateEditTextStyle() {
        binding.etText.setTextColor(selectedTextColor)
        val style = when {
            isBold && isItalic -> Typeface.BOLD_ITALIC
            isBold -> Typeface.BOLD
            isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        binding.etText.setTypeface(null, style)
    }

    override fun setupListeners() {
        binding.ivCancel.setOnClickListener {
            dismiss()
        }

        binding.btnBold.setOnClickListener {
            isBold = !isBold
            binding.btnBold.alpha = if (isBold) 1f else 0.5f
            updateEditTextStyle()
        }

        binding.btnItalic.setOnClickListener {
            isItalic = !isItalic
            binding.btnItalic.alpha = if (isItalic) 1f else 0.5f
            updateEditTextStyle()
        }

        binding.ivDone.setOnClickListener {
            val text = binding.etText.text.toString().trim()
            if (text.isNotEmpty()) {
                commitTextToEditor(text)
            }
            dismiss()
        }
    }

    /**
     * Creates (or updates) a [TextLayer] and commits it to the editor via [EditorSessionViewModel].
     *
     * Position defaults to the center of the canvas (0.5, 0.5) in normalized coordinates.
     * Size is 5% of canvas width (0.05 normalized) — a reasonable default.
     * Both can later be updated by a sticker/text transform interaction if needed.
     */
    private fun commitTextToEditor(text: String) {
        val id = existingTextId ?: UUID.randomUUID().toString()
        val layer = TextLayer(
            id = id,
            text = text,
            x = 0.5f,       // normalized center X
            y = 0.5f,       // normalized center Y
            size = 0.05f,   // 5% of canvas width
            color = selectedTextColor,
            alpha = 255,
            rotation = 0f,
            fontFamily = null,
            isBold = isBold,
            isItalic = isItalic
        )

        if (existingTextId == null) {
            sessionViewModel.addText(layer)
        } else {
            sessionViewModel.updateText(layer)
        }
    }

    override fun onDestroyView() {
        textColorAdapter = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(): TextBottomSheet {
            return TextBottomSheet()
        }
    }
}
