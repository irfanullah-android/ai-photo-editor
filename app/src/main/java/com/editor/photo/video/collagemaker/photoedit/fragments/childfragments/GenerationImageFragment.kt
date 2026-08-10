package com.editor.photo.video.collagemaker.photoedit.fragments.childfragments

import android.net.Uri
import android.util.Log
import android.view.View
import android.view.View.VISIBLE
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.adsConfig.bannerAdsManager.BannerAd
import com.editor.photo.video.collagemaker.photoedit.adsConfig.bannerAdsManager.BannerCallBack
import com.editor.photo.video.collagemaker.photoedit.databinding.FragmentGenerationImageBinding
import com.editor.photo.video.collagemaker.photoedit.fragments.basefragments.BaseFragment
import com.editor.photo.video.collagemaker.photoedit.models.AiTemplate
import com.editor.photo.video.collagemaker.photoedit.models.GenerationResult
import com.editor.photo.video.collagemaker.photoedit.viewmodel.AiGenerationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GenerationImageFragment :
    BaseFragment<FragmentGenerationImageBinding>(R.layout.fragment_generation_image) {

    private val TAG = "GenerationFragment"
    private var isBannerLoaded = false
    private val viewModel: AiGenerationViewModel by activityViewModels()
    private var navigatedToResult = false

    private lateinit var template: AiTemplate
    private lateinit var person1Uri: Uri
    private lateinit var person2Uri: Uri

    override fun onViewCreatedOneTime() {
        loadBannerAds()
        template = requireArguments().getSerializable("template") as AiTemplate
        person1Uri = Uri.parse(requireArguments().getString("person1Uri"))
        person2Uri = Uri.parse(requireArguments().getString("person2Uri"))

        binding.btnGenerateNewPhoto.setOnClickListener {
            Log.d(TAG, "Generate new photo clicked — clearing result")
            viewModel.clearResult()
            val navController = requireActivity().findNavController(R.id.fcv_container_main)
            val poppedGroup = navController.popBackStack(R.id.aiGroupPhotoFragment, false)
            if (!poppedGroup) {
                val poppedSingle = navController.popBackStack(R.id.aiSinglePhotoFragment, false)
                if (!poppedSingle) navController.navigateUp()
            }
        }
    }

    override fun onViewCreatedEverytime() {
        navigatedToResult = false
        observeViewModel()
        Log.d(TAG, "Starting generation — template=${template.title}, p1=$person1Uri, p2=$person2Uri")
        binding.root.post {
            viewModel.generate(person1Uri, person2Uri, template)
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
                    binding.lottieLoading.visibility = VISIBLE
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
                        parentNavigate(R.id.aiGroupPhotoResultFragment)
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

    private lateinit var bannerAds: BannerAd
    private fun loadBannerAds() {
        bannerAds = BannerAd(requireActivity())
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                binding?.let { b ->
                    bannerAds.loadBannerAds(
                        b.bannerAd,
                        b.admobPlace,
                        b.loading,
                        object : BannerCallBack {
                            override fun onAdFailedToLoad(adError: String) {
                                b.bannerAd.visibility = View.GONE
                                isBannerLoaded = false

                            }

                            override fun onAdLoaded() {
                                b.bannerAd.visibility = VISIBLE
                                b.loading.visibility = View.GONE
                                isBannerLoaded = true

                            }

                            override fun onAdImpression() {}
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("AdLoadingError", "Error loading ads", e)
                isBannerLoaded = false

            }
        }
    }
}