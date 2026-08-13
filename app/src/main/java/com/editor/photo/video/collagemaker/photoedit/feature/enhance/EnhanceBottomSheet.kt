package com.editor.photo.video.collagemaker.photoedit.feature.enhance

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.graphics.drawable.BitmapDrawable
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import java.util.concurrent.atomic.AtomicLong

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

    /**
     * True once the FIRST toolValues emission has been consumed. StateFlow
     * replays its current value to a fresh collector immediately on
     * subscribe — this flag lets us skip that replay so opening the sheet
     * never renders on its own. Only a real value change (from
     * updateValue(), i.e. an actual slider drag) after this point renders.
     */
    private var toolValuesInitialized = false

    /**
     * Monotonically increasing token for race-safety. Every call to
     * applyEnhance() claims the next token; only the render whose token
     * still matches [renderToken] at completion time is allowed to touch
     * the ImageView or lastBitmap. This guarantees "latest value always
     * wins" even under rapid slider drags where multiple coroutines may be
     * mid-render simultaneously — stronger than relying on isActive alone,
     * since isActive only reflects cancellation, not "am I still the
     * newest request."
     */
    private val renderToken = AtomicLong(0L)

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

        // Hide UI controls briefly while bitmap loads off-thread to prevent UI jank
        binding.intensityContainer.visibility = View.GONE
        binding.seekBarEnhance.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            loadOriginalBitmapAsync()
            setupEnhanceNamesRecyclerView()
            setupEnhancePreviewsRecyclerView()
            setupSeekBar()
            setupTopBar()
            observeViewModel()

            binding.intensityContainer.visibility = View.VISIBLE
            binding.seekBarEnhance.visibility = View.VISIBLE

            // Sync seekbar instantly for initial selected tool
            updateSeekBarValue(enhanceViewModel.selectedTool.value)
        }
    }

    private suspend fun loadOriginalBitmapAsync() {
        withContext(Dispatchers.IO) {
            try {
                baselineBitmap = withContext(Dispatchers.Main) {
                    (imageView?.drawable as? BitmapDrawable)?.bitmap
                }

                val rawBitmap = sourceBitmap ?: baselineBitmap ?: imageUri?.let { uri ->
                    val context = context ?: return@let null
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.isMutableRequired = true
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                }

                originalBitmap = rawBitmap
                previewBitmap = rawBitmap?.let { buildPreviewBitmap(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
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
                // Bounded preview only — seekbar release must never trigger a
                // full-resolution render (previewBitmap source, no size branching).
                applyEnhance(activeValues(), debounce = false)
            }
        })
    }

    /**
     * Returns the COMPLETE 13-value Enhance snapshot built from every tool
     * currently held in EnhanceViewModel.toolValues — not just the selected
     * tool. Used for live preview, quick color-filter preview, and the final
     * Done commit, so all three always agree, and so switching tools always
     * shows every previously-applied value at once.
     */
    private fun activeValues(): EnhanceEngine.EnhanceValues {
        return enhanceViewModel.buildEnhanceValues()
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
            // ONE complete snapshot committed as ONE EditOperation.Enhance.
            // EditorEngine.addOperation() replaces any existing Enhance op
            // in place, so this stays a single history entry no matter how
            // many times Enhance is reopened and edited.
            sessionViewModel.applyEnhance(values)
            dismiss()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    enhanceViewModel.selectedTool.collect { tool ->
                        // Sync SeekBar UI to selected tool's value
                        updateSeekBarValue(tool)

                        val position = EditorEnhance.values().indexOf(tool)
                        enhanceAdapter?.updateSelection(position)
                        enhanceNamesAdapter?.updateSelection(position)
                        binding.rvEnhanceOptions.smoothScrollToPosition(position)
                        binding.rvEnhanceNames.smoothScrollToPosition(position)

                        // Immediately show full active state preview upon switching tools
                        applyEnhance(activeValues(), debounce = false)
                    }
                }

                launch {
                    enhanceViewModel.toolValues.collect {
                        // Skip the very first emission (StateFlow replay on
                        // subscribe) so opening the sheet never renders on its own.
                        if (!toolValuesInitialized) {
                            toolValuesInitialized = true
                            return@collect
                        }
                        // Real value change from an actual slider drag — this is
                        // the ONLY place a render/preview-image change happens,
                        // always using the complete current Enhance state.
                        imageView?.colorFilter = EnhanceEngine.quickPreviewFilter(activeValues())
                        applyEnhance(activeValues(), debounce = true)
                    }
                }
            }
        }
    }

    /**
     * Renders the complete Enhance snapshot against the BOUNDED preview
     * bitmap only — full-resolution rendering never happens here. It
     * happens once, later, through EditorEngine's existing preview/export
     * pipeline after Done commits the EditOperation.Enhance.
     *
     * Race-safety: claims a fresh [renderToken] before launching. The
     * coroutine only applies its result to the ImageView / lastBitmap if
     * its token is STILL the newest one when it finishes — any older,
     * slower render is silently dropped and its bitmap recycled, even if
     * it happens to finish after being technically cancelled. This gives
     * a hard "latest value always wins" guarantee under rapid dragging.
     */
    private fun applyEnhance(values: EnhanceEngine.EnhanceValues, debounce: Boolean) {
        val myToken = renderToken.incrementAndGet()
        renderJob?.cancel()

        if (values.isIdentity()) {
            imageView?.colorFilter = null
            imageView?.setImageBitmap(originalBitmap)
            val oldBitmap = lastBitmap
            lastBitmap = null
            recycleIfSafe(oldBitmap, originalBitmap)
            return
        }

        val bitmap = previewBitmap ?: originalBitmap ?: return

        renderJob = coroutineScope.launch {
            try {
                if (debounce) {
                    delay(16)
                }
                if (myToken != renderToken.get()) return@launch
                if (!isActive) return@launch

                val rendered = EnhanceEngine.render(bitmap, values)

                if (myToken != renderToken.get() || !isActive) {
                    rendered.recycle()
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    if (myToken != renderToken.get() || !isActive) {
                        rendered.recycle()
                        return@withContext
                    }
                    imageView?.colorFilter = null
                    imageView?.setImageBitmap(rendered)
                    val oldBitmap = lastBitmap
                    lastBitmap = rendered
                    recycleIfSafe(oldBitmap, rendered)
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