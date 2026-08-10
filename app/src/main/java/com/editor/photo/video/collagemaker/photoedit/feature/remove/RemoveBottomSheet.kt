package com.editor.photo.video.collagemaker.photoedit.feature.remove

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.editor.photo.video.collagemaker.photoedit.databinding.BottomSheetRemoveBinding
import com.editor.photo.video.collagemaker.photoedit.editor.session.EditorSessionViewModel

class RemoveBottomSheet : BottomSheetDialogFragment() {

    private val sessionViewModel: EditorSessionViewModel by activityViewModels()

    private lateinit var binding: BottomSheetRemoveBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetRemoveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnRemoveBg.isEnabled = true
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnRemoveBg.setOnClickListener {
            sessionViewModel.removeBackground()
            dismiss()
        }
        binding.btnCancel.setOnClickListener { dismiss() }
    }

    companion object {
        fun newInstance() = RemoveBottomSheet()
    }
}
