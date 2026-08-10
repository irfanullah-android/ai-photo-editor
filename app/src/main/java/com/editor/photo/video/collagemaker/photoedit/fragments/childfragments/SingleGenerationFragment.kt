package com.editor.photo.video.collagemaker.photoedit.fragments.childfragments

import android.net.Uri
import android.util.Log
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.databinding.FragmentGenerationImageBinding
import com.editor.photo.video.collagemaker.photoedit.fragments.basefragments.BaseFragment
import com.editor.photo.video.collagemaker.photoedit.models.AiTemplate
import com.editor.photo.video.collagemaker.photoedit.models.GenerationResult
import com.editor.photo.video.collagemaker.photoedit.viewmodel.AiGenerationViewModel

class SingleGenerationFragment :
    BaseFragment<FragmentGenerationImageBinding>(R.layout.fragment_generation_image) {

    private val TAG = "SingleGenerationFragment"
    private val viewModel: AiGenerationViewModel by activityViewModels()
    private var navigatedToResult = false

    private lateinit var template: AiTemplate
    private lateinit var personUri: Uri

    override fun onViewCreatedOneTime() {
        template = requireArguments().getSerializable("template") as AiTemplate
        personUri = Uri.parse(requireArguments().getString("personUri"))

        binding.btnGenerateNewPhoto.setOnClickListener {
            Log.d(TAG, "Generate new photo clicked — clearing result")
            viewModel.clearResult()
            val navController = requireActivity().findNavController(R.id.fcv_container_main)
            val popped = navController.popBackStack(R.id.aiSinglePhotoFragment, false)
            if (!popped) navController.navigateUp()
        }
    }

    override fun onViewCreatedEverytime() {
        navigatedToResult = false
        observeViewModel()
        Log.d(TAG, "Starting generation — template=${template.title}, person=$personUri")
        binding.root.post {
            viewModel.generateSingleFromTemplate(personUri, template)
        }
    }

    private fun observeViewModel() {
        viewModel.result.observe(viewLifecycleOwner) { result ->
            when (result) {
                is GenerationResult.Idle -> {
                    Log.d(TAG, "State: Idle")
                }
                is GenerationResult.Loading -> {
                    Log.d(TAG, "State: Loading")
                    binding.lottieLoading.visibility = View.VISIBLE
                    binding.lottieLoading.playAnimation()
                    binding.btnGenerateNewPhoto.isEnabled = true
                    binding.tvGenerationTitle.text = "Your Photo will be ready in some minutes."
                    binding.tvGenerationSubtitle.text = "Cloud Processing... Your new photo is on its way!"
                }
                is GenerationResult.Success -> {
                    Log.d(TAG, "State: Success — navigatedToResult=$navigatedToResult")
                    if (!navigatedToResult) {
                        navigatedToResult = true
                        binding.lottieLoading.cancelAnimation()
                        binding.lottieLoading.visibility = View.GONE
                        parentNavigate(R.id.aiSinglePhotoResultFragment)
                    }
                }
                is GenerationResult.Error -> {
                    Log.e(TAG, "State: Error — ${result.message}")
                    binding.lottieLoading.cancelAnimation()
                    binding.lottieLoading.visibility = View.GONE
                    binding.tvGenerationTitle.text = "Generation failed"
                    binding.tvGenerationSubtitle.text = result.message
                    binding.btnGenerateNewPhoto.isEnabled = true
                }
            }
        }

        viewModel.elapsedSeconds.observe(viewLifecycleOwner) { seconds ->
            if (viewModel.result.value is GenerationResult.Loading) {
                binding.tvGenerationSubtitle.text = "Cloud Processing... ${seconds}s elapsed"
            }
        }
    }
}