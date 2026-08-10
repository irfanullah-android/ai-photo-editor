package com.editor.photo.video.collagemaker.photoedit.feature.enhance

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
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
import com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter.EnhanceAdapter
import com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter.EnhanceNamesAdapter
import com.editor.photo.video.collagemaker.photoedit.databinding.BottomSheetEnhanceBinding
import com.editor.photo.video.collagemaker.photoedit.editor.session.EditorSessionViewModel
import com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine.EnhanceEngine
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorEnhance
import kotlinx.coroutines.*

class EnhanceBottomSheet : BottomSheetDialogFragment() {

    private val sessionViewModel: EditorSessionViewModel by activityViewModels()
    private val enhanceViewModel: EnhanceViewModel by viewModels()

    private lateinit var binding: BottomSheetEnhanceBinding
    private var imageView: ImageView? = null
    private var imageUri: Uri? = null
    private var sourceBitmap: Bitmap? = null
    private var onDismissed: (() -> Unit)? = null

    private var originalBitmap: Bitmap? = null
    private var previewBitmap: Bitmap? = null
    private var baselineBitmap: Bitmap? = null
    private var wasApplied = false

    private var enhanceAdapter: EnhanceAdapter? = null
    private var enhanceNamesAdapter: EnhanceNamesAdapter? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var renderJob: Job? = null
    private var lastBitmap: Bitmap? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.window?.setDimAmount(0f)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetEnhanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadOriginalBitmap()
        setupEnhanceNamesRecyclerView()
        setupEnhancePreviewsRecyclerView()
        setupSeekBar()
        setupTopBar()
        observeViewModel()
        
        binding.intensityContainer.visibility = View.VISIBLE
        binding.seekBarEnhance.visibility = View.VISIBLE
    }

    private fun loadOriginalBitmap() {
        try {
            baselineBitmap = (imageView?.drawable as? BitmapDrawable)?.bitmap
            originalBitmap = sourceBitmap ?: baselineBitmap ?: imageUri?.let {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
            }
            previewBitmap = originalBitmap?.let { buildPreviewBitmap(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildPreviewBitmap(source: Bitmap): Bitmap {
        val longSide = maxOf(source.width, source.height)
        if (longSide <= PREVIEW_MAX_DIMENSION) return source
        val scale = PREVIEW_MAX_DIMENSION / longSide.toFloat()
        val w = (source.width * scale).toInt().coerceAtLeast(1)
        val h = (source.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, w, h, true)
    }

    private fun matchOriginalSize(bitmap: Bitmap): Bitmap {
        val original = originalBitmap ?: return bitmap
        if (bitmap.width == original.width && bitmap.height == original.height) return bitmap

        val matched = Bitmap.createBitmap(
            original.width, original.height, bitmap.config ?: Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(matched)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
        val dstRect = Rect(0, 0, original.width, original.height)
        canvas.drawBitmap(bitmap, srcRect, dstRect, paint)

        if (bitmap != original && bitmap != previewBitmap) {
            bitmap.recycle()
        }
        return matched
    }

    private fun setupEnhanceNamesRecyclerView() {
        binding.rvEnhanceNames.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        enhanceNamesAdapter = EnhanceNamesAdapter(EditorEnhance.values().toList()) { tool ->
            enhanceViewModel.selectTool(tool)
        }
        binding.rvEnhanceNames.adapter = enhanceNamesAdapter
    }

    private fun setupEnhancePreviewsRecyclerView() {
        binding.rvEnhanceOptions.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        enhanceAdapter = EnhanceAdapter(requireContext(), EditorEnhance.values().toList(), originalBitmap) { tool ->
            enhanceViewModel.selectTool(tool)
        }
        binding.rvEnhanceOptions.adapter = enhanceAdapter
    }

    private fun setupSeekBar() {
        binding.seekBarEnhance.max = 200

        binding.seekBarEnhance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val value = (progress - 100) / 100f
                binding.tvEnhanceValue.text = (progress - 100).toString()
                enhanceViewModel.updateValue(enhanceViewModel.selectedTool.value, value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                applyEnhance(activeValues(), debounce = false)
            }
        })
    }

    private fun activeValues(): EnhanceEngine.EnhanceValues {
        val tool = enhanceViewModel.selectedTool.value
        val value = enhanceViewModel.toolValues.value[tool] ?: 0f
        return EnhanceEngine.EnhanceValues().with(tool, value)
    }

    private fun updateSeekBarValue(tool: EditorEnhance) {
        val value = enhanceViewModel.toolValues.value[tool] ?: 0f
        val progress = ((value * 100) + 100).toInt()
        binding.seekBarEnhance.progress = progress
        binding.tvEnhanceValue.text = (progress - 100).toString()
    }

    private fun setupTopBar() {
        binding.ivCheck.setOnClickListener {
            renderJob?.cancel()
            val values = activeValues()
            if (values.isIdentity()) {
                dismiss()
                return@setOnClickListener
            }

            wasApplied = true
            sessionViewModel.applyEnhance(
                enhanceViewModel.selectedTool.value,
                enhanceViewModel.toolValues.value[enhanceViewModel.selectedTool.value] ?: 0f
            )
            dismiss()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            enhanceViewModel.selectedTool.collect { tool ->
                updateSeekBarValue(tool)
                applyEnhance(activeValues(), debounce = false)

                val position = EditorEnhance.values().indexOf(tool)
                enhanceAdapter?.updateSelection(position)
                enhanceNamesAdapter?.updateSelection(position)
                binding.rvEnhanceOptions.smoothScrollToPosition(position)
                binding.rvEnhanceNames.smoothScrollToPosition(position)
            }
        }

        lifecycleScope.launchWhenStarted {
            enhanceViewModel.toolValues.collect { valuesMap ->
                val tool = enhanceViewModel.selectedTool.value
                val value = valuesMap[tool] ?: 0f
                imageView?.colorFilter = EnhanceEngine.quickPreviewFilter(activeValues())
                applyEnhance(activeValues(), debounce = true)
            }
        }
    }

    private fun applyEnhance(values: EnhanceEngine.EnhanceValues, debounce: Boolean) {
        renderJob?.cancel()

        if (values.isIdentity()) {
            imageView?.colorFilter = null
            imageView?.setImageBitmap(originalBitmap)
            val oldBitmap = lastBitmap
            lastBitmap = null
            recycleIfSafe(oldBitmap, originalBitmap)
            return
        }

        val source = if (debounce) (previewBitmap ?: originalBitmap) else originalBitmap
        val bitmap = source ?: return

        renderJob = coroutineScope.launch {
            try {
                if (debounce) {
                    delay(16)
                }
                if (!isActive) return@launch

                val rendered = EnhanceEngine.render(bitmap, values)

                if (!isActive) {
                    rendered.recycle()
                    return@launch
                }

                val displayBitmap = matchOriginalSize(rendered)

                if (!isActive) {
                    if (displayBitmap != rendered) rendered.recycle()
                    displayBitmap.recycle()
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    if (!isActive) {
                        displayBitmap.recycle()
                        return@withContext
                    }
                    imageView?.colorFilter = null
                    imageView?.setImageBitmap(displayBitmap)
                    val oldBitmap = lastBitmap
                    lastBitmap = displayBitmap
                    recycleIfSafe(oldBitmap, displayBitmap)
                }
            } catch (e: CancellationException) {
                // Ignore
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    cleanup()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun recycleIfSafe(old: Bitmap?, keep: Bitmap?) {
        try {
            if (old != null && !old.isRecycled && old != keep && old != originalBitmap && old != previewBitmap) {
                old.recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cleanup() {
        renderJob?.cancel()
        lastBitmap = null
    }

    override fun onDestroyView() {
        coroutineScope.cancel()
        cleanup()
        imageView?.colorFilter = null
        enhanceAdapter?.cleanup()
        enhanceNamesAdapter = null
        if (previewBitmap != null && previewBitmap != originalBitmap && previewBitmap?.isRecycled == false) {
            previewBitmap?.recycle()
        }
        previewBitmap = null
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
        private const val PREVIEW_MAX_DIMENSION = 1024

        fun newInstance(
            imageUri: Uri?,
            sourceBitmap: Bitmap?,
            imageView: ImageView,
            onDismissed: (() -> Unit)? = null
        ): EnhanceBottomSheet {
            return EnhanceBottomSheet().apply {
                this.imageUri = imageUri
                this.sourceBitmap = sourceBitmap
                this.imageView = imageView
                this.onDismissed = onDismissed
            }
        }
    }
}
