package com.editor.photo.video.collagemaker.photoedit.feature.remove

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.editor.photo.video.collagemaker.photoedit.core.BaseEditorBottomSheet
import com.editor.photo.video.collagemaker.photoedit.databinding.BottomSheetRemoveBinding
import com.editor.photo.video.collagemaker.photoedit.editor.session.EditorSessionViewModel

class RemoveBottomSheet :
    BaseEditorBottomSheet<BottomSheetRemoveBinding>() {

    private val sessionViewModel: EditorSessionViewModel by activityViewModels()

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): BottomSheetRemoveBinding {
        return BottomSheetRemoveBinding.inflate(inflater, container, false)
    }

    override fun setupUI() {
        binding.btnRemoveBg.isEnabled = true
    }

    override fun setupListeners() {
        binding.btnRemoveBg.setOnClickListener {
            sessionViewModel.removeBackground()
            dismiss()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        fun newInstance() = RemoveBottomSheet()
    }
}