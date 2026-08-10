package com.editor.photo.video.collagemaker.photoedit.feature.effect

import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EffectType
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.adapters.EffectNameAdapter
import com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter.EffectAdapter
import com.editor.photo.video.collagemaker.photoedit.databinding.BottomSheetEffectBinding
import com.editor.photo.video.collagemaker.photoedit.editor.session.EditorSessionViewModel
import com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine.EffectsEngine
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EffectModel
import kotlinx.coroutines.*
import kotlin.math.max

class EffectBottomSheet : BottomSheetDialogFragment() {

    private val sessionViewModel: EditorSessionViewModel by activityViewModels()
    private val effectViewModel: EffectViewModel by viewModels()

    private lateinit var binding: BottomSheetEffectBinding
    private var imageView: ImageView? = null
    private var imageUri: Uri? = null
    private var sourceBitmap: Bitmap? = null

    private var originalBitmap: Bitmap? = null
    private var baselineBitmap: Bitmap? = null
    private var wasApplied = false

    @Volatile
    private var isClosing = false

    private var displayBase: Bitmap? = null
    private var filteredBitmap: Bitmap? = null
    private var lastBlendedBitmap: Bitmap? = null

    private var previewOriginal: Bitmap? = null
    private var previewFiltered: Bitmap? = null

    private lateinit var effectAdapter: EffectAdapter
    private lateinit var effectNameAdapter: EffectNameAdapter
    private var isSyncingScroll = false

    private var intensityJob: Job? = null
    private var effectJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val blendPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.window?.setDimAmount(0f)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetEffectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadOriginalBitmap()
        setupEffectsList()
        setupSeekBar()
        setupListeners()
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

    private fun setupEffectsList() {
        val effects = listOf(
            EffectModel("None", R.drawable.ic_effect, EffectType.NONE),
            EffectModel("HDR", R.drawable.ic_effect, EffectType.HDR),
            EffectModel("Vintage", R.drawable.ic_effect, EffectType.VINTAGE),
            EffectModel("Cinematic", R.drawable.ic_effect, EffectType.CINEMATIC),
            EffectModel("B&W", R.drawable.ic_effect, EffectType.BLACK_WHITE),
            EffectModel("Sepia", R.drawable.ic_effect, EffectType.SEPIA),
            EffectModel("Bloom", R.drawable.ic_effect, EffectType.BLOOM),
            EffectModel("Soft Focus", R.drawable.ic_effect, EffectType.SOFT_FOCUS),
            EffectModel("Oil Paint", R.drawable.ic_effect, EffectType.OIL_PAINT),
            EffectModel("Matte", R.drawable.ic_effect, EffectType.MATTE),
            EffectModel("Pixelate", R.drawable.ic_effect, EffectType.PIXELATE),
            EffectModel("Grain", R.drawable.ic_effect, EffectType.GRAIN),
            EffectModel("Duotone", R.drawable.ic_effect, EffectType.DUOTONE),
            EffectModel("Double Exposure", R.drawable.ic_effect, EffectType.DOUBLE_EXPOSURE)
        )

        effectNameAdapter = EffectNameAdapter(effects) { effect, position ->
            effectViewModel.selectEffect(effect.type)
            effectAdapter.updateSelection(position)
        }

        binding.rvEffectNames.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = effectNameAdapter
            setHasFixedSize(true)
        }

        effectAdapter = EffectAdapter(requireContext(), effects, originalBitmap) { effect, position ->
            effectViewModel.selectEffect(effect.type)
            effectNameAdapter.updateSelection(position)
        }

        binding.rvEffects.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = effectAdapter
            setHasFixedSize(true)
        }

        setupSyncedScrolling()
    }

    private fun setupSyncedScrolling() {
        binding.rvEffectNames.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (isSyncingScroll || dx == 0) return
                isSyncingScroll = true
                binding.rvEffects.scrollBy(dx, 0)
                isSyncingScroll = false
            }
        })

        binding.rvEffects.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (isSyncingScroll || dx == 0) return
                isSyncingScroll = true
                binding.rvEffectNames.scrollBy(dx, 0)
                isSyncingScroll = false
            }
        })
    }

    private fun setupSeekBar() {
        binding.seekBarIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser || isClosing) return
                effectViewModel.setIntensity(progress / 100f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (isClosing) return
                if (effectViewModel.selectedEffect.value != EffectType.NONE) {
                    applyIntensityDisplayQuality()
                }
            }
        })
    }

    private fun setupListeners() {
        binding.ivCheck.setOnClickListener {
            wasApplied = true
            sessionViewModel.applyEffect(
                effectViewModel.selectedEffect.value,
                effectViewModel.intensity.value
            )
            dismiss()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            effectViewModel.selectedEffect.collect { type ->
                updateSeekBarVisibility(type)
                applyEffect(type)
            }
        }

        lifecycleScope.launchWhenStarted {
            effectViewModel.intensity.collect { intensity ->
                binding.seekBarIntensity.progress = (intensity * 100).toInt()
                binding.tvIntensityValue.text = (intensity * 100).toInt().toString()
                if (effectViewModel.selectedEffect.value != EffectType.NONE) {
                    applyIntensityPreview()
                }
            }
        }
    }

    private fun updateSeekBarVisibility(effect: EffectType) {
        if (effect == EffectType.NONE) {
            binding.intensityContainer.visibility = View.GONE
            binding.seekBarIntensity.visibility = View.GONE
        } else {
            binding.intensityContainer.visibility = View.VISIBLE
            binding.seekBarIntensity.visibility = View.VISIBLE
        }
    }

    private fun applyEffect(type: EffectType) {
        if (isClosing) return
        effectJob?.cancel()
        effectJob = coroutineScope.launch {
            try {
                val bitmap = originalBitmap ?: return@launch
                withContext(Dispatchers.Main) {
                    if (isClosing) return@withContext
                    effectViewModel.setIntensity(1.0f)
                }

                val oldDisplayBase = displayBase
                val oldFiltered = filteredBitmap
                val oldBlended = lastBlendedBitmap
                val oldPreviewOriginal = previewOriginal
                val oldPreviewFiltered = previewFiltered

                if (type == EffectType.NONE) {
                    displayBase = null
                    filteredBitmap = null
                    lastBlendedBitmap = null
                    previewOriginal = null
                    previewFiltered = null

                    withContext(Dispatchers.Main) {
                        if (!isClosing) imageView?.setImageBitmap(bitmap)
                    }
                } else {
                    val newDisplayBase = scaleDownForPreview(bitmap, DISPLAY_MAX_DIMENSION)
                    val newFilteredBitmap = EffectsEngine.apply(type, newDisplayBase)

                    if (!isActive || isClosing) {
                        if (newFilteredBitmap != newDisplayBase) newFilteredBitmap.recycle()
                        newDisplayBase.recycle()
                        return@launch
                    }

                    displayBase = newDisplayBase
                    filteredBitmap = newFilteredBitmap
                    lastBlendedBitmap = null

                    previewOriginal = scaleDownForPreview(newDisplayBase, PREVIEW_MAX_DIMENSION)
                    previewFiltered = scaleDownForPreview(newFilteredBitmap, PREVIEW_MAX_DIMENSION)

                    withContext(Dispatchers.Main) {
                        if (!isClosing) imageView?.setImageBitmap(newFilteredBitmap)
                    }
                }

                oldDisplayBase?.let { if (!it.isRecycled) it.recycle() }
                oldFiltered?.let { if (!it.isRecycled && it != oldDisplayBase) it.recycle() }
                oldBlended?.let { if (!it.isRecycled && it != oldDisplayBase && it != oldFiltered) it.recycle() }
                oldPreviewOriginal?.let { if (!it.isRecycled) it.recycle() }
                oldPreviewFiltered?.let { if (!it.isRecycled) it.recycle() }

            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun applyIntensityPreview() {
        if (isClosing) return
        val original = previewOriginal ?: return
        val filtered = previewFiltered ?: return
        val blended = blendBitmapsFast(original, filtered, effectViewModel.intensity.value)
        if (isClosing) {
            blended.recycle()
            return
        }
        imageView?.setImageBitmap(blended)
    }

    private fun applyIntensityDisplayQuality() {
        if (isClosing) return
        coroutineScope.launch {
            try {
                val base = displayBase ?: return@launch
                val filtered = filteredBitmap ?: return@launch
                val newBlended = blendBitmapsFast(base, filtered, effectViewModel.intensity.value)

                if (!isActive || isClosing) {
                    if (newBlended != base) newBlended.recycle()
                    return@launch
                }

                val oldBlended = lastBlendedBitmap
                lastBlendedBitmap = newBlended

                withContext(Dispatchers.Main) {
                    if (!isClosing) imageView?.setImageBitmap(newBlended)
                }

                oldBlended?.let {
                    if (!it.isRecycled && it != base && it != filtered) {
                        it.recycle()
                    }
                }
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun scaleDownForPreview(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxDimension) return bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false)
        val scale = maxDimension.toFloat() / longest
        val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    private fun blendBitmapsFast(original: Bitmap, filtered: Bitmap, intensity: Float): Bitmap {
        val result = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        blendPaint.alpha = 255
        canvas.drawBitmap(original, 0f, 0f, blendPaint)

        blendPaint.alpha = (intensity * 255).toInt().coerceIn(0, 255)
        canvas.drawBitmap(filtered, 0f, 0f, blendPaint)

        return result
    }

    private fun releasePreviewBitmaps() {
        effectJob?.cancel()
        intensityJob?.cancel()

        displayBase?.let { if (!it.isRecycled) it.recycle() }
        filteredBitmap?.let { if (!it.isRecycled && it != displayBase) it.recycle() }
        lastBlendedBitmap?.let { if (!it.isRecycled && it != displayBase && it != filteredBitmap) it.recycle() }
        previewOriginal?.let { if (!it.isRecycled) it.recycle() }
        previewFiltered?.let { if (!it.isRecycled) it.recycle() }

        displayBase = null
        filteredBitmap = null
        lastBlendedBitmap = null
        previewOriginal = null
        previewFiltered = null
    }

    private fun cleanup() {
        isClosing = true
        intensityJob?.cancel()
        effectJob?.cancel()
        coroutineScope.cancel()
        originalBitmap?.let { if (!it.isRecycled) imageView?.setImageBitmap(it) }
        releasePreviewBitmaps()
        imageView = null
    }

    override fun onDestroyView() {
        cleanup()
        originalBitmap = null
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
    }

    companion object {
        private const val DISPLAY_MAX_DIMENSION = 1440
        private const val PREVIEW_MAX_DIMENSION = 720

        fun newInstance(
            imageUri: Uri?,
            sourceBitmap: Bitmap?,
            imageView: ImageView,
            onEffectApplied: ((Bitmap) -> Unit)? = null
        ): EffectBottomSheet {
            return EffectBottomSheet().apply {
                this.sourceBitmap = sourceBitmap
                this.imageView = imageView
                this.imageUri = imageUri
            }
        }
    }
}
