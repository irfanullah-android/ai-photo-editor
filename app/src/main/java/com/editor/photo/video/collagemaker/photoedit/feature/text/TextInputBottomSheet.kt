package com.editor.photo.video.collagemaker.photoedit.feature.text

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.databinding.BottomSheetTextInputBinding
import com.editor.photo.video.collagemaker.photoedit.editor.session.EditorSessionViewModel
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class TextInputBottomSheet : DialogFragment() {

    private var _binding: BottomSheetTextInputBinding? = null
    private val binding get() = _binding!!

    private val sessionViewModel: EditorSessionViewModel by activityViewModels()
    private var existingTextId: String? = null

    override fun getTheme(): Int = R.style.Theme_FullScreenBottomSheet

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.Theme_FullScreenBottomSheet)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }
        dialog.setCancelable(true)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetTextInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        existingTextId = arguments?.getString(ARG_TEXT_ID)

        val existingText = existingTextId
            ?.let { id -> sessionViewModel.editorState.value.textLayers.firstOrNull { it.id == id } }
            ?.text
            .orEmpty()

        binding.etTextInputMode.setText(existingText)
        binding.etTextInputMode.setSelection(existingText.length)

        setupBlurredBackdrop()
        setupListeners()

        binding.etTextInputMode.post {
            binding.etTextInputMode.requestFocus()
            showKeyboard(binding.etTextInputMode)
        }
    }

    private fun setupListeners() {
        binding.btnInputCancel.setOnClickListener { dismiss() }
        binding.btnInputDone.setOnClickListener { onDoneClicked() }
    }

    // TextInputBottomSheet.kt ke onDoneClicked() mein:
    private fun onDoneClicked() {
        val text = binding.etTextInputMode.text.toString().trim()
        if (text.isEmpty()) {
            dismiss()
            return
        }
        hideKeyboard()
        sessionViewModel.submitTextInput(existingTextId, text)   // ← startOrUpdateTextDraft ki jagah

        TextEditorBottomSheet.newInstance()                       // ← ab koi arg nahi
            .show(parentFragmentManager, "text_editor_style")
        dismiss()
    }

    private fun setupBlurredBackdrop() {
        val previewBitmap = currentPreviewBitmap() ?: return
        binding.ivInputBackdrop.post {
            val w = binding.ivInputBackdrop.width.coerceAtLeast(1)
            val h = binding.ivInputBackdrop.height.coerceAtLeast(1)
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                val blurred = try {
                    BlurUtils.softBlurDimmed(previewBitmap, w, h, dimAlpha = 140)
                } catch (e: Exception) {
                    null
                }
                withContext(Dispatchers.Main) {
                    if (blurred != null && _binding != null) {
                        binding.ivInputBackdrop.setImageBitmap(blurred)
                    }
                }
            }
        }
    }

    private fun currentPreviewBitmap(): Bitmap? =
        (sessionViewModel.uiState.value as? EditorUiState.Editing)?.previewBitmap

    private fun showKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.etTextInputMode.windowToken, 0)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_TEXT_ID = "arg_text_id"

        fun newInstance(existingTextId: String? = null): TextInputBottomSheet {
            return TextInputBottomSheet().apply {
                arguments = Bundle().apply {
                    existingTextId?.let { putString(ARG_TEXT_ID, it) }
                }
            }
        }
    }
}