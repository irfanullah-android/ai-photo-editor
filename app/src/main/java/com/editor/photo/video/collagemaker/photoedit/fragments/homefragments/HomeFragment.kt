package com.editor.photo.video.collagemaker.photoedit.fragments.homefragments

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.adapters.AiGroupPhotoAdapter
import com.editor.photo.video.collagemaker.photoedit.adapters.AiSinglePhotoAdapter
import com.editor.photo.video.collagemaker.photoedit.adapters.SmallIconAdapter
import com.editor.photo.video.collagemaker.photoedit.databinding.FragmentHomeBinding
import com.editor.photo.video.collagemaker.photoedit.fragments.basefragments.BaseFragment
import com.editor.photo.video.collagemaker.photoedit.helpers.TemplateSelectionManager
import com.editor.photo.video.collagemaker.photoedit.models.PhotoTool
import com.editor.photo.video.collagemaker.photoedit.models.SmallIconItem
import com.editor.photo.video.collagemaker.photoedit.models.ToolType
import com.editor.photo.video.collagemaker.photoedit.repository.AiTemplateRepository
import com.editor.photo.video.collagemaker.photoedit.utlis.CommonData
import com.editor.photo.video.collagemaker.photoedit.viewmodel.AiGenerationViewModel
import com.google.android.material.appbar.AppBarLayout
import kotlin.math.abs

class HomeFragment : BaseFragment<FragmentHomeBinding>(R.layout.fragment_home) {

    private val tag = "HomeFragment"
    private var isBannerLoaded = false

    private lateinit var aiGroupPhotoAdapter: AiGroupPhotoAdapter
    private lateinit var aiSinglePhotoAdapter: AiSinglePhotoAdapter
    private lateinit var smallIconAdapter: SmallIconAdapter
    private val aiViewModel: AiGenerationViewModel by activityViewModels()

    private var isToolbarCollapsed = false
    private var appBarOffsetListener: AppBarLayout.OnOffsetChangedListener? = null

    private var allPhotoTools: List<PhotoTool> = emptyList()
    private var allSmallIcons: List<SmallIconItem> = emptyList()
    private var initialSearchBarWidth = 0
    private var isSearchExpanded = false

    // Stable listener reference to avoid duplicate scroll listeners and compile errors
    private val smallIconsScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (_binding == null) return
            updateIndicator(recyclerView)
        }
    }

    override fun onViewCreatedOneTime() {
        activity?.let { act ->
            WindowCompat.setDecorFitsSystemWindows(act.window, false)
            act.window.statusBarColor = Color.TRANSPARENT
        }


        setupAdapters()
        loadData()
        setupSearchBar()
        setupToggleButton()
        setupToolClickListeners()
        setupTopBarClicks()
        setupCollapsingToolbarListener()
    }

    override fun onViewCreatedEverytime() {
        Log.d(tag, "onViewCreatedEveryTime")
        aiViewModel.warmUp()
        isToolbarCollapsed = false
        showExpandedBar()

        binding.etSearch.text?.clear()
        collapseSearchBar()

        binding.rvSmallIcons.post {
            if (_binding != null) setupSlidingIndicator()
        }
    }

    private fun setupCollapsingToolbarListener() {
        appBarOffsetListener =
            AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                val absOffset = abs(verticalOffset)
                val totalRange = appBarLayout.totalScrollRange

                when {
                    absOffset >= totalRange -> {
                        if (!isToolbarCollapsed) {
                            isToolbarCollapsed = true
                            showCollapsedBar()
                        }
                        activity?.window?.statusBarColor = Color.BLACK
                    }

                    verticalOffset == 0 -> {
                        if (isToolbarCollapsed) {
                            isToolbarCollapsed = false
                            showExpandedBar()
                        }
                        activity?.window?.statusBarColor = Color.TRANSPARENT
                    }

                    else -> {
                        if (isToolbarCollapsed) {
                            isToolbarCollapsed = false
                            showExpandedBar()
                        }
                    }
                }
            }
        binding.appBarLayout.addOnOffsetChangedListener(appBarOffsetListener)
    }

    private fun showCollapsedBar() {
        binding.searchBarLayout.visibility = View.INVISIBLE
        binding.toolbar.visibility = View.VISIBLE
        binding.toolbar.alpha = 0f
        binding.toolbar.animate().alpha(1f).setDuration(200).start()
        rotateArrow(false)
    }

    private fun showExpandedBar() {
        binding.toolbar.animate()
            .alpha(0f)
            .setDuration(150)
            .withEndAction {
                if (_binding != null) binding.toolbar.visibility = View.GONE
            }.start()
        binding.searchBarLayout.visibility = View.VISIBLE
        rotateArrow(false)
    }

    private fun setupToggleButton() {
        binding.btnToggleExpand.setOnClickListener {
            binding.appBarLayout.setExpanded(true, true)
        }
    }

    private fun rotateArrow(pointUp: Boolean) {
        binding.btnToggleExpand.animate()
            .rotation(if (pointUp) 180f else 0f)
            .setDuration(250).start()
    }

    private fun setupSearchBar() {
        binding.searchBar.post {
            if (_binding != null && initialSearchBarWidth == 0)
                initialSearchBarWidth = binding.searchBar.width
        }

        binding.searchBar.setOnClickListener { if (!isSearchExpanded) expandSearchBar() }

        binding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (!isSearchExpanded) expandSearchBar()
            } else {
                if (isSearchExpanded && binding.etSearch.text.isNullOrEmpty()) collapseSearchBar()
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                binding.btnClearSearch.visibility =
                    if (query.isNotEmpty()) View.VISIBLE else View.GONE
                filterSearch(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
            binding.etSearch.clearFocus()
            collapseSearchBar()
            hideKeyboard()
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                binding.etSearch.clearFocus()
                collapseSearchBar()
                hideKeyboard()
                true
            } else false
        }
    }

    private fun expandSearchBar() {
        if (_binding == null || isSearchExpanded) return
        isSearchExpanded = true
        binding.btnVip.visibility = View.GONE
        val params = binding.searchBar.layoutParams as ConstraintLayout.LayoutParams
        params.width = ConstraintLayout.LayoutParams.MATCH_CONSTRAINT
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        params.marginStart = 16.dpToPx()
        params.marginEnd = 16.dpToPx()
        binding.searchBar.layoutParams = params
        binding.searchBar.requestLayout()
        binding.searchBarLayout.requestLayout()
        binding.etSearch.postDelayed({
            if (_binding != null) {
                binding.etSearch.requestFocus()
                (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                    ?.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
            }
        }, 100)
    }

    private fun collapseSearchBar() {
        if (_binding == null || !isSearchExpanded) return
        isSearchExpanded = false
        val width = if (initialSearchBarWidth > 0) initialSearchBarWidth else 200.dpToPx()
        val params = binding.searchBar.layoutParams as ConstraintLayout.LayoutParams
        params.width = width
        params.endToEnd = ConstraintLayout.LayoutParams.UNSET
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        params.marginStart = 16.dpToPx()
        params.marginEnd = 0
        binding.searchBar.layoutParams = params
        binding.btnVip.visibility = View.VISIBLE
        binding.searchBar.requestLayout()
        binding.searchBarLayout.requestLayout()
        (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    override fun hideKeyboard() {
        if (_binding == null) return
        (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
            ?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        binding.etSearch.clearFocus()
    }

    private fun filterSearch(query: String) {
        if (query.isEmpty()) {
            aiGroupPhotoAdapter.submitList(allPhotoTools.filter { it.type == ToolType.AI_GROUP_PHOTO })
            aiSinglePhotoAdapter.submitList(allPhotoTools.filter { it.type == ToolType.AI_SINGLE_PHOTO })
            smallIconAdapter.submitList(allSmallIcons)
            return
        }
        val lowerQuery = query.lowercase()
        aiGroupPhotoAdapter.submitList(
            allPhotoTools.filter {
                it.type == ToolType.AI_GROUP_PHOTO && it.title.lowercase().contains(lowerQuery)
            }
        )
        aiSinglePhotoAdapter.submitList(
            allPhotoTools.filter {
                it.type == ToolType.AI_SINGLE_PHOTO && it.title.lowercase().contains(lowerQuery)
            }
        )
        smallIconAdapter.submitList(
            allSmallIcons.filter { it.label.lowercase().contains(lowerQuery) }
        )
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun setupAdapters() {
        aiGroupPhotoAdapter = AiGroupPhotoAdapter { item ->
            CommonData.requestMediaPermissionsWithDexter(
                context = requireContext(),
                onGranted = {
                    activity?.runOnUiThread {
                        TemplateSelectionManager.selectedGroupTemplateId = item.templateId
                        parentNavigate(R.id.aiGroupPhotoFragment)
                    }
                },
                onDenied = {
                    activity?.runOnUiThread {
                        Toast.makeText(requireContext(), "Permission Denied", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            )
        }

        binding.rvAiGroupPhoto.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = aiGroupPhotoAdapter
            isNestedScrollingEnabled = true
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect, view: View,
                    parent: RecyclerView, state: RecyclerView.State
                ) {
                    val pos = parent.getChildAdapterPosition(view)
                    if (pos > 0) outRect.left = 8.dpToPx()
                }
            })
        }

        aiSinglePhotoAdapter = AiSinglePhotoAdapter { item ->
            CommonData.requestMediaPermissionsWithDexter(
                context = requireContext(),
                onGranted = {
                    activity?.runOnUiThread {
                        TemplateSelectionManager.selectedSingleTemplateId = item.templateId
                        parentNavigate(R.id.aiSinglePhotoFragment)
                    }
                },
                onDenied = {
                    activity?.runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "Storage permission required.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }

        binding.rvAiSinglePhoto.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = aiSinglePhotoAdapter
            isNestedScrollingEnabled = true
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect, view: View,
                    parent: RecyclerView, state: RecyclerView.State
                ) {
                    val pos = parent.getChildAdapterPosition(view)
                    if (pos > 0) outRect.left = 8.dpToPx()
                }
            })
        }

        smallIconAdapter = SmallIconAdapter { item -> onSmallIconClick(item.tag) }
        binding.rvSmallIcons.apply {
            layoutManager =
                GridLayoutManager(requireContext(), 2, GridLayoutManager.HORIZONTAL, false)
            adapter = smallIconAdapter
        }

    }

    private fun setupSlidingIndicator() {
        binding.rvSmallIcons.removeOnScrollListener(smallIconsScrollListener)
        binding.rvSmallIcons.addOnScrollListener(smallIconsScrollListener)

        val canScroll = binding.rvSmallIcons.computeHorizontalScrollRange() >
                binding.rvSmallIcons.computeHorizontalScrollExtent()
        binding.indicatorTrack.visibility = if (canScroll) View.VISIBLE else View.GONE
    }

    private fun updateIndicator(recyclerView: RecyclerView) {
        val totalScroll = recyclerView.computeHorizontalScrollRange() -
                recyclerView.computeHorizontalScrollExtent()
        if (totalScroll <= 0) {
            binding.indicatorTrack.visibility = View.GONE
            return
        }
        binding.indicatorTrack.visibility = View.VISIBLE
        val progress = recyclerView.computeHorizontalScrollOffset().toFloat() / totalScroll
        val maxTravel =
            binding.indicatorTrack.width.toFloat() - binding.indicatorThumb.width.toFloat()
        binding.indicatorThumb.translationX = progress * maxTravel
    }

    private fun loadData() {
        allPhotoTools = AiTemplateRepository.getGroupTemplates().map { template ->
            PhotoTool(
                id = template.id,
                title = template.title,
                imageRes = template.imageResId,
                type = ToolType.AI_GROUP_PHOTO,
                templateId = template.id,
                prompt = template.prompt
            )
        } + AiTemplateRepository.getSingleTemplates().map { template ->
            PhotoTool(
                id = template.id,
                title = template.title,
                imageRes = template.imageResId,
                type = ToolType.AI_SINGLE_PHOTO,
                templateId = template.id,
                prompt = template.prompt
            )
        }

        allSmallIcons = getSmallIcons()

        aiGroupPhotoAdapter.submitList(allPhotoTools.filter { it.type == ToolType.AI_GROUP_PHOTO })
        aiSinglePhotoAdapter.submitList(allPhotoTools.filter { it.type == ToolType.AI_SINGLE_PHOTO })
        smallIconAdapter.submitList(allSmallIcons)
    }

    private fun getSmallIcons(): List<SmallIconItem> = listOf(
        SmallIconItem(
            id = 1,
            iconRes = R.drawable.ic_canvas,
            label = getString(R.string.label_canvas),
            tag = "Canvas"
        ),
        SmallIconItem(
            id = 2,
            iconRes = R.drawable.ic_filter,
            label = getString(R.string.label_filter),
            tag = "Filter"
        ),
        SmallIconItem(
            id = 3,
            iconRes = R.drawable.ic_adjust,
            label = getString(R.string.label_adjust),
            tag = "Adjust"
        ),
        SmallIconItem(
            id = 4,
            iconRes = R.drawable.ic_effect,
            label = getString(R.string.label_effect),
            tag = "Effect"
        ),
        SmallIconItem(
            id = 5,
            iconRes = R.drawable.ic_sticker,
            label = getString(R.string.label_sticker),
            tag = "Sticker"
        ),
        SmallIconItem(
            id = 6,
            iconRes = R.drawable.ic_text,
            label = getString(R.string.label_text),
            tag = "Text"
        ),
        SmallIconItem(
            id = 7,
            iconRes = R.drawable.ic_remove_bg,
            label = getString(R.string.label_remove),
            tag = "Remove"
        ),
        SmallIconItem(
            id = 8,
            iconRes = R.drawable.ic_enhance,
            label = getString(R.string.label_enhance),
            tag = "Enhance"
        ),
        SmallIconItem(
            id = 9,
            iconRes = R.drawable.ic_doodle,
            label = getString(R.string.label_doodle),
            tag = "Doodle"
        ),
        SmallIconItem(
            id = 10,
            iconRes = R.drawable.ic_crop,
            label = getString(R.string.label_crop),
            tag = "Crop"
        ),
        SmallIconItem(
            id = 11,
            iconRes = R.drawable.ic_frame,
            label = getString(R.string.label_frame),
            tag = "Frame"
        ),
        SmallIconItem(
            id = 12,
            iconRes = R.drawable.ic_rotate,
            label = getString(R.string.label_rotate),
            tag = "Rotate"
        ),
    )

    private fun setupTopBarClicks() {
        binding.btnVip.setOnClickListener { Log.d(tag, "VIP clicked") }
    }

    private fun setupToolClickListeners() {
        binding.btnPhoto.setOnClickListener { onToolClick("Photo") }
        binding.btnBeautify.setOnClickListener { onToolClick("Beautify") }
        binding.btnCollage.setOnClickListener { onToolClick("Collage") }
        binding.btnCollapsedPhoto.setOnClickListener { onToolClick("Photo") }
        binding.btnCollapsedBeautify.setOnClickListener { onToolClick("Beautify") }
        binding.btnCollapsedCollage.setOnClickListener { onToolClick("Collage") }
    }

    private fun onToolClick(toolName: String) {
        Log.d(tag, "Tool clicked: $toolName")
        when (toolName) {
            "Photo", "Beautify" -> {
                CommonData.requestMediaPermissionsWithDexter(
                    context = requireContext(),
                    onGranted = {
                        activity?.runOnUiThread { parentNavigate(R.id.SelectImageFragment) }
                    },
                    onDenied = {
                        activity?.runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "Storage permission required.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }

            "Collage" -> {
                CommonData.requestMediaPermissionsWithDexter(
                    context = requireContext(),
                    onGranted = {
                        activity?.runOnUiThread {
                            val bundle = Bundle().apply { putString("source", "home") }
                            parentNavigate(R.id.collageFragment, bundle)
                        }
                    },
                    onDenied = {
                        activity?.runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "Storage permission required.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            }

            else -> Log.d(tag, "No navigation defined for: $toolName")
        }
    }

    private fun onSmallIconClick(toolName: String) {
        CommonData.requestMediaPermissionsWithDexter(
            context = requireContext(),
            onGranted = {
                activity?.runOnUiThread {
                    val bundle = Bundle().apply { putString("selectedTool", toolName) }
                    parentNavigate(R.id.SelectImageFragment, bundle)
                }
            },
            onDenied = {
                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Storage permission required.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }


    override fun onDestroyView() {
        appBarOffsetListener?.let { binding.appBarLayout.removeOnOffsetChangedListener(it) }
        appBarOffsetListener = null
        binding.rvSmallIcons.removeOnScrollListener(smallIconsScrollListener)
        super.onDestroyView()
    }
}