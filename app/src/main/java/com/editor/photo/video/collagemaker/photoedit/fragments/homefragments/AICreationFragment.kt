package com.editor.photo.video.collagemaker.photoedit.fragments.homefragments

import android.util.Log
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.databinding.FragmentAiCreationBinding
import com.editor.photo.video.collagemaker.photoedit.fragments.basefragments.BaseFragment

class AICreationFragment :
    BaseFragment<FragmentAiCreationBinding>(R.layout.fragment_ai_creation) {

    private val tag = "AICreationFragment"

    override fun onViewCreatedOneTime() {
        Log.d(tag, "This Will Run onViewCreatedOneTime")
    }

    override fun onViewCreatedEverytime() {
        Log.d(tag, "This Will Run onViewCreatedEveryTime")
    }
}