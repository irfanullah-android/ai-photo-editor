package com.editor.photo.video.collagemaker.photoedit.feature.filter

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter.FilterAdapter
import com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter.FilterNamesAdapter
import com.editor.photo.video.collagemaker.photoedit.databinding.BottomSheetFilterBinding
import com.editor.photo.video.collagemaker.photoedit.editor.session.EditorSessionViewModel
import com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine.ColorMatrixEngine
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorFilter
import kotlinx.coroutines.*

class FilterBottomSheet : BottomSheetDialogFragment() {

    private val sessionViewModel: EditorSessionViewModel by activityViewModels()
    private val filterViewModel: FilterViewModel by viewModels()

    private lateinit var binding: BottomSheetFilterBinding
    private var imageView: ImageView? = null
    private var imageUri: Uri? = null
    private var sourceBitmap: Bitmap? = null
    private var onDismissed: (() -> Unit)? = null

    private var originalBitmap: Bitmap? = null
    private var baselineBitmap: Bitmap? = null
    private var wasApplied = false

    private var filterAdapter: FilterAdapter? = null
    private var filterNamesAdapter: FilterNamesAdapter? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var filterJob: Job? = null
    private var lastBitmap: Bitmap? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.window?.setDimAmount(0f)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadOriginalBitmap()
        setupFilterNamesRecyclerView()
        setupFilterPreviewsRecyclerView()
        setupSeekBar()
        setupTopBar()
        observeViewModel()
    }

    private fun loadOriginalBitmap() {
        try {
            baselineBitmap = (imageView?.drawable as? BitmapDrawable)?.bitmap
            originalBitmap = sourceBitmap ?: baselineBitmap ?: imageUri?.let {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupFilterNamesRecyclerView() {
        binding.rvFilterNames.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        filterNamesAdapter = FilterNamesAdapter(EditorFilter.values().toList()) { filter ->
            filterViewModel.selectFilter(filter)
        }
        binding.rvFilterNames.adapter = filterNamesAdapter
    }

    private fun setupFilterPreviewsRecyclerView() {
        binding.rvFilters.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        filterAdapter = FilterAdapter(requireContext(), EditorFilter.values().toList(), originalBitmap) { filter ->
            filterViewModel.selectFilter(filter)
        }
        binding.rvFilters.adapter = filterAdapter
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            filterViewModel.selectedFilter.collect { filter ->
                updateSeekBarVisibility(filter)
                val position = EditorFilter.values().indexOf(filter)
                
                filterNamesAdapter?.updateSelection(position)
                binding.rvFilterNames.smoothScrollToPosition(position)
                
                filterAdapter?.updateSelection(position)
                binding.rvFilters.smoothScrollToPosition(position)
                
                applyFilter(filter, filterViewModel.intensity.value)
            }
        }

        lifecycleScope.launchWhenStarted {
            filterViewModel.intensity.collect { intensity ->
                binding.seekBarIntensity.progress = intensity
                binding.tvIntensity.text = intensity.toString()
                applyFilter(filterViewModel.selectedFilter.value, intensity)
            }
        }
    }

    private fun updateSeekBarVisibility(filter: EditorFilter) {
        if (filter == EditorFilter.NORMAL) {
            binding.intensityContainer.visibility = View.GONE
            binding.seekBarIntensity.visibility = View.GONE
        } else {
            binding.intensityContainer.visibility = View.VISIBLE
            binding.seekBarIntensity.visibility = View.VISIBLE
        }
    }

    private fun setupSeekBar() {
        binding.seekBarIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                filterViewModel.setIntensity(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupTopBar() {
        binding.tvFilter.setOnClickListener {
            toggleTabs(isFilterSelected = true)
        }

        binding.ivCheck.setOnClickListener {
            wasApplied = true
            sessionViewModel.applyFilter(
                filterViewModel.selectedFilter.value,
                filterViewModel.intensity.value
            )
            dismiss()
        }
    }

    private fun toggleTabs(isFilterSelected: Boolean) {
        if (isFilterSelected) {
            binding.tvFilter.setTextColor(0xFFFFFFFF.toInt())
            binding.tvFilter.setTypeface(null, Typeface.BOLD)
            binding.rvFilterNames.visibility = View.VISIBLE
            binding.rvFilters.visibility = View.VISIBLE
        } else {
            binding.tvFilter.setTextColor(0xFFAAAAAA.toInt())
            binding.tvFilter.setTypeface(null, Typeface.NORMAL)
            binding.rvFilterNames.visibility = View.GONE
            binding.rvFilters.visibility = View.GONE
        }
    }

    private fun applyFilter(filter: EditorFilter, intensity: Int) {
        filterJob?.cancel()
        imageView?.colorFilter = null

        if (filter == EditorFilter.NORMAL) {
            imageView?.setImageBitmap(originalBitmap)
            return
        }

        val bitmap = originalBitmap ?: return
        filterJob = coroutineScope.launch {
            val spec = filter.buildFilterSpec(intensity)
            val newBitmap = ColorMatrixEngine.render(bitmap, spec)

            withContext(Dispatchers.Main) {
                if (!isActive) {
                    newBitmap.recycle()
                    return@withContext
                }

                imageView?.setImageBitmap(newBitmap)

                val oldBitmap = lastBitmap
                lastBitmap = newBitmap
                if (oldBitmap != null && oldBitmap != originalBitmap) {
                    oldBitmap.recycle()
                }
            }
        }
    }

    private fun cleanup() {
        filterJob?.cancel()
        lastBitmap = null
    }

    override fun onDestroyView() {
        coroutineScope.cancel()
        cleanup()
        imageView?.colorFilter = null
        filterAdapter?.cleanup()
        filterNamesAdapter = null
        originalBitmap = null
        imageView = null
        super.onDestroyView()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!wasApplied) {
            imageView?.colorFilter = null
            baselineBitmap?.let { safe ->
                if (!safe.isRecycled) imageView?.setImageBitmap(safe)
            }
        }
        onDismissed?.invoke()
    }

    companion object {
        fun newInstance(
            imageUri: Uri?,
            sourceBitmap: Bitmap?,
            imageView: ImageView,
            onDismissed: (() -> Unit)? = null
        ): FilterBottomSheet {
            return FilterBottomSheet().apply {
                this.imageUri = imageUri
                this.sourceBitmap = sourceBitmap
                this.imageView = imageView
                this.onDismissed = onDismissed
            }
        }
    }
}
