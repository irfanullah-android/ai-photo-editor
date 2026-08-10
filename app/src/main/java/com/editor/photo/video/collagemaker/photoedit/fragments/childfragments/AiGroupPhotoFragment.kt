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
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.adapters.ThumbnailAdapter
import com.editor.photo.video.collagemaker.photoedit.databinding.FragmentAiGroupPhotoBinding
import com.editor.photo.video.collagemaker.photoedit.fragments.basefragments.BaseFragment
import com.editor.photo.video.collagemaker.photoedit.helpers.TemplateSelectionManager
import com.editor.photo.video.collagemaker.photoedit.helpers.scrollToCenter
import com.editor.photo.video.collagemaker.photoedit.helpers.smoothScrollToCenter
import com.editor.photo.video.collagemaker.photoedit.models.AiTemplate
import com.editor.photo.video.collagemaker.photoedit.models.ThumbnailItem
import com.editor.photo.video.collagemaker.photoedit.repository.AiTemplateRepository
import kotlin.math.abs

class AiGroupPhotoFragment :
    BaseFragment<FragmentAiGroupPhotoBinding>(R.layout.fragment_ai_group_photo) {

    private val TAG = "GroupPhotoFragment"

    private lateinit var thumbnailAdapter: ThumbnailAdapter
    private var selectedTemplate: AiTemplate? = null
    private val templateList by lazy { AiTemplateRepository.getGroupTemplates() }

    override fun onViewCreatedOneTime() {
        setupRecyclerView()
        setupImageSwipe()
        setupClickListeners()
    }

    override fun onViewCreatedEverytime() {
        val targetIndex = templateList
            .indexOfFirst { it.id == TemplateSelectionManager.selectedGroupTemplateId }
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

        Log.d(TAG, "Selected template — id=${selectedTemplate?.id}, title=${selectedTemplate?.title}")

        val savedState = findNavController().currentBackStackEntry?.savedStateHandle
        savedState?.getLiveData<Bundle>("dualSelectionResult")?.observe(viewLifecycleOwner) { bundle ->
            savedState.remove<Bundle>("dualSelectionResult")
            val p1 = Uri.parse(bundle.getString("person1Uri") ?: return@observe)
            val p2 = Uri.parse(bundle.getString("person2Uri") ?: return@observe)
            navigateToGeneration(p1, p2)
        }
    }

    private fun setupRecyclerView() {
        val initialIndex = templateList
            .indexOfFirst { it.id == TemplateSelectionManager.selectedGroupTemplateId }
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
            TemplateSelectionManager.selectedGroupTemplateId = templateList[position].id
            Log.d(
                TAG,
                "Thumbnail tapped — id=${selectedTemplate?.id}, title=${selectedTemplate?.title}"
            )
            updateBackgroundSmoothly(templateList[position].imageResId)
            binding.rvThumbnails.smoothScrollToCenter(position)
        }

        binding.rvThumbnails.apply {
            layoutManager = LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false
            )
            adapter = thumbnailAdapter

            val itemWidthPx = (56 * dm.density).toInt()
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
                Glide.with(this@AiGroupPhotoFragment)
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
                        TemplateSelectionManager.selectedGroupTemplateId = templateList[target].id
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
            Log.d(TAG, "Generate Now clicked — navigating to gallery")
            parentNavigate(R.id.customGalleryFragment)
        }
    }

    private fun navigateToGeneration(p1: Uri, p2: Uri) {
        val template = selectedTemplate ?: run {
            Log.e(TAG, "selectedTemplate is NULL — cannot navigate")
            return
        }
        Log.d(TAG, "Navigating to generation — id=${template.id}, title=${template.title}")
        Log.d(TAG, "Prompt preview — ${template.prompt.take(80)}...")
        Log.d(TAG, "Person1 URI — $p1")
        Log.d(TAG, "Person2 URI — $p2")

        val bundle = Bundle().apply {
            putSerializable("template", template)
            putString("person1Uri", p1.toString())
            putString("person2Uri", p2.toString())
        }
        parentNavigate(R.id.generationImageFragment, bundle)
    }
}