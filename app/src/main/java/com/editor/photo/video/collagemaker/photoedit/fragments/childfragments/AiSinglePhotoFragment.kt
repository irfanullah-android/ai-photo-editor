package com.editor.photo.video.collagemaker.photoedit.fragments.childfragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.editor.photo.video.collagemaker.photoedit.adapters.ThumbnailAdapter
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.databinding.FragmentAiSinglePhotoBinding
import com.editor.photo.video.collagemaker.photoedit.fragments.basefragments.BaseFragment
import com.editor.photo.video.collagemaker.photoedit.helpers.TemplateSelectionManager
import com.editor.photo.video.collagemaker.photoedit.helpers.scrollToCenter
import com.editor.photo.video.collagemaker.photoedit.helpers.smoothScrollToCenter
import com.editor.photo.video.collagemaker.photoedit.models.AiTemplate
import com.editor.photo.video.collagemaker.photoedit.models.ThumbnailItem
import com.editor.photo.video.collagemaker.photoedit.repository.AiTemplateRepository
import kotlin.math.abs

class AiSinglePhotoFragment :
    BaseFragment<FragmentAiSinglePhotoBinding>(R.layout.fragment_ai_single_photo) {

    private lateinit var thumbnailAdapter: ThumbnailAdapter
    private var selectedTemplate: AiTemplate? = null
    private val templateList by lazy { AiTemplateRepository.getSingleTemplates() }

    override fun onViewCreatedOneTime() {
        setupRecyclerView()
        setupImageSwipe()
        setupClickListeners()
    }

    override fun onViewCreatedEverytime() {
        val targetIndex = templateList
            .indexOfFirst { it.id == TemplateSelectionManager.selectedSingleTemplateId }
            .coerceAtLeast(0)

        selectedTemplate = templateList[targetIndex]

        val dm = resources.displayMetrics
        Glide.with(this)
            .load(templateList[targetIndex].imageResId)
            .override(dm.widthPixels, dm.heightPixels)
            .centerCrop()
            .dontAnimate()
            .placeholder(R.drawable.bg_gradient_purple_white)
            .into(binding.viewGradientBg)

        if (::thumbnailAdapter.isInitialized) {
            thumbnailAdapter.selectPosition(targetIndex)
            binding.rvThumbnails.post {
                binding.rvThumbnails.scrollToCenter(targetIndex)
            }
        }

        val savedState = findNavController().currentBackStackEntry?.savedStateHandle
        savedState?.getLiveData<Bundle>("singleSelectionResult")?.observe(viewLifecycleOwner) { bundle ->
            savedState.remove<Bundle>("singleSelectionResult")
            val personUri = Uri.parse(bundle.getString("personUri") ?: return@observe)
            navigateToGeneration(personUri)
        }
    }

    private fun setupRecyclerView() {
        val initialIndex = templateList
            .indexOfFirst { it.id == TemplateSelectionManager.selectedSingleTemplateId }
            .coerceAtLeast(0)

        selectedTemplate = templateList[initialIndex]

        val dm = resources.displayMetrics
        Glide.with(this)
            .load(templateList[initialIndex].imageResId)
            .override(dm.widthPixels, dm.heightPixels)
            .centerCrop()
            .dontAnimate()
            .placeholder(R.drawable.bg_gradient_purple_white)
            .into(binding.viewGradientBg)

        thumbnailAdapter = ThumbnailAdapter(
            templateList.map { ThumbnailItem(id = it.id, imageResId = it.imageResId) }
        ) { position ->
            // ✅ Sirf click pe selection + background change
            selectedTemplate = templateList[position]
            TemplateSelectionManager.selectedSingleTemplateId = templateList[position].id
            updateBackgroundSmoothly(templateList[position].imageResId)
            binding.rvThumbnails.smoothScrollToCenter(position)
        }

        binding.rvThumbnails.apply {
            layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false
            )
            adapter = thumbnailAdapter

            // ✅ 60dp — actual item width se match karo
            val itemWidthPx = (60 * dm.density).toInt()
            val padding = (dm.widthPixels - itemWidthPx) / 2
            setPadding(padding, 0, padding, 0)
            clipToPadding = false

            post {
                scrollToCenter(initialIndex)
                thumbnailAdapter.selectPosition(initialIndex)
            }
        }
    }

    private fun updateBackgroundSmoothly(newImageResId: Int) {
        val dm = resources.displayMetrics
        val fadeOut = AlphaAnimation(1f, 0.4f).apply { duration = 150 }
        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                Glide.with(this@AiSinglePhotoFragment)
                    .load(newImageResId)
                    .override(dm.widthPixels, dm.heightPixels)
                    .centerCrop()
                    .dontAnimate()
                    .placeholder(R.drawable.bg_gradient_purple_white)
                    .thumbnail(0.1f)
                    .into(binding.viewGradientBg)
                binding.viewGradientBg.startAnimation(
                    AlphaAnimation(0.4f, 1f).apply { duration = 150 }
                )
            }
        })
        binding.viewGradientBg.startAnimation(fadeOut)
    }

    private fun setupImageSwipe() {
        val gestureDetector = GestureDetector(requireContext(),
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?, e2: MotionEvent, vX: Float, vY: Float
                ): Boolean {
                    if (e1 == null) return false
                    val diffX = e2.x - e1.x
                    if (abs(diffX) > 50 && abs(vX) > 50) {
                        val current = templateList.indexOfFirst { it.id == selectedTemplate?.id }
                        val target = when {
                            diffX > 0 && current > 0 -> current - 1
                            diffX < 0 && current < templateList.size - 1 -> current + 1
                            else -> return false
                        }
                        // ✅ Swipe pe bhi selection + center scroll
                        selectedTemplate = templateList[target]
                        TemplateSelectionManager.selectedSingleTemplateId = templateList[target].id
                        thumbnailAdapter.selectPosition(target)
                        updateBackgroundSmoothly(templateList[target].imageResId)
                        binding.rvThumbnails.smoothScrollToCenter(target)
                        return true
                    }
                    return false
                }
            })
        binding.viewGradientBg.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.btnGenerateNow.setOnClickListener {
            val bundle = Bundle().apply { putBoolean("isSingleMode", true) }
            parentNavigate(R.id.customGalleryFragment, bundle)
        }
    }

    private fun navigateToGeneration(personUri: Uri) {
        val template = selectedTemplate ?: run {
            Log.e("AiSinglePhotoFragment", "selectedTemplate is NULL")
            return
        }
        val bundle = Bundle().apply {
            putSerializable("template", template)
            putString("personUri", personUri.toString())
        }
        parentNavigate(R.id.singleGenerationFragment, bundle)
    }
}