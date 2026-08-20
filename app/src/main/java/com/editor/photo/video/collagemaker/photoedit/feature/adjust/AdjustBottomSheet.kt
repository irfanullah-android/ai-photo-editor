package com.editor.photo.video.collagemaker.photoedit.feature.adjust

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter.AdjustmentAdapter
import com.editor.photo.video.collagemaker.photoedit.databinding.BottomSheetAdjustmentBinding
import com.editor.photo.video.collagemaker.photoedit.editor.session.EditorSessionViewModel
import com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine.ColorMatrixEngine
import com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine.FilterSpec
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.AdjustmentModel
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.AdjustmentType
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class AdjustBottomSheet : BottomSheetDialogFragment() {

    private val sessionViewModel: EditorSessionViewModel by activityViewModels()
    private val adjustViewModel: AdjustViewModel by viewModels()

    private lateinit var binding: BottomSheetAdjustmentBinding
    private var adjustmentAdapter: AdjustmentAdapter? = null
    private var imageView: ImageView? = null
    private var imageUri: Uri? = null

    private var originalBitmap: Bitmap? = null
    private var baselineBitmap: Bitmap? = null
    private var lastBitmap: Bitmap? = null
    private var wasApplied = false
    private var renderJob: Job? = null
    private var autoAnalysisJob: Job? = null

    private var onAdjustApplied: ((Bitmap?) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetAdjustmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.window?.setDimAmount(0f)
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupListeners()
        observeViewModel()
    }

    private fun setupUI() {
        loadOriginalBitmap()
        initAdjustmentsList()
        setupRecyclerView()
    }

    private fun loadOriginalBitmap() {
        try {
            baselineBitmap = (imageView?.drawable as? BitmapDrawable)?.bitmap
            originalBitmap = baselineBitmap
                ?: imageUri?.let {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initAdjustmentsList() {
        val initialList = listOf(
            AdjustmentModel("Auto", R.drawable.ic_auto_enhance, AdjustmentType.AUTO, value = 100),
            AdjustmentModel("Brightness", R.drawable.ic_brightness, AdjustmentType.BRIGHTNESS, isSelected = true),
            AdjustmentModel("Contrast", R.drawable.ic_contrast, AdjustmentType.CONTRAST),
            AdjustmentModel("Warmth", R.drawable.ic_warmth, AdjustmentType.WARMTH),
            AdjustmentModel("Tint", R.drawable.ic_tint, AdjustmentType.TINT),
            AdjustmentModel("Saturation", R.drawable.ic_saturation, AdjustmentType.SATURATION),
            AdjustmentModel("Fade", R.drawable.ic_fade, AdjustmentType.FADE),
            AdjustmentModel("Highlight", R.drawable.ic_highlight, AdjustmentType.HIGHLIGHT),
            AdjustmentModel("Shadow", R.drawable.ic_shadow, AdjustmentType.SHADOW),
            AdjustmentModel("Hue", R.drawable.ic_hue, AdjustmentType.HUE),
            AdjustmentModel("Sharpen", R.drawable.ic_sharpen, AdjustmentType.SHARPEN)
        )
        adjustViewModel.initAdjustments(initialList)
    }

    private fun setupRecyclerView() {
        binding.rvAdjustments.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvAdjustments.setHasFixedSize(true)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            adjustViewModel.adjustments.collect { list ->
                if (list.isNotEmpty()) {
                    if (adjustmentAdapter == null) {
                        adjustmentAdapter = AdjustmentAdapter(list.toMutableList()) { adj, _ ->
                            adjustViewModel.selectAdjustment(adj)
                            if (adj.type == AdjustmentType.AUTO) {
                                runAutoEnhance()
                            }
                        }
                        binding.rvAdjustments.adapter = adjustmentAdapter
                    } else {
                        adjustmentAdapter?.apply {
                            val adapterList = this.javaClass.getDeclaredField("adjustments").let { field ->
                                field.isAccessible = true
                                @Suppress("UNCHECKED_CAST")
                                field.get(this) as MutableList<AdjustmentModel>
                            }
                            adapterList.clear()
                            adapterList.addAll(list)
                            notifyDataSetChanged()
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            adjustViewModel.selectedAdjustment.collect { adj ->
                adj?.let { updateAdjustmentUI(it) }
            }
        }
    }

    private fun updateAdjustmentUI(adjustment: AdjustmentModel) {
        binding.tvAdjustmentName.text = adjustment.name
        binding.seekBarAdjustment.progress = adjustment.value
        binding.tvAdjustmentValue.text = adjustment.value.toString()
        binding.seekBarAdjustment.isEnabled = true
    }

    private fun setupListeners() {
        binding.seekBarAdjustment.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                if (!fromUser) return
                binding.tvAdjustmentValue.text = p.toString()

                val adj = adjustViewModel.selectedAdjustment.value ?: return
                adjustViewModel.updateValue(adj.type, p)

                if (adj.type == AdjustmentType.AUTO) {
                    runAutoEnhance()
                    return
                }

                if (adj.type == AdjustmentType.SHARPEN) {
                    scheduleRender(debounceMs = 80, isPreview = true)
                    return
                }

                imageView?.colorFilter = ColorMatrixEngine.asColorFilter(buildCombinedSpec())
                scheduleRender(debounceMs = 60, isPreview = true)
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}

            override fun onStopTrackingTouch(sb: SeekBar?) {
                scheduleRender(debounceMs = 0, isPreview = false)
            }
        })

        binding.ivCheck.setOnClickListener { applyAdjustments() }
    }

    private fun ease(t: Float): Float {
        val c = t.coerceIn(-1f, 1f)
        val sign = if (c < 0f) -1f else 1f
        val a = abs(c)
        return sign * (1f - (1f - a) * (1f - a))
    }

    private fun eased(type: AdjustmentType): Float {
        val raw = (adjustViewModel.adjustmentValues.value[type] ?: 0) / 100f
        return ease(raw)
    }

    private fun buildCombinedSpec(): FilterSpec {
        val e = ColorMatrixEngine
        val matrices = mutableListOf<ColorMatrix>()
        val valuesMap = adjustViewModel.adjustmentValues.value

        if ((valuesMap[AdjustmentType.BRIGHTNESS] ?: 0) != 0) {
            val t = eased(AdjustmentType.BRIGHTNESS)
            matrices += e.brightness(t * 55f)
        }

        if ((valuesMap[AdjustmentType.CONTRAST] ?: 0) != 0) {
            val t = eased(AdjustmentType.CONTRAST)
            val factor = if (t >= 0f) 1f + t * 0.6f else 1f + t * 0.4f
            matrices += e.contrast(factor)
        }

        if ((valuesMap[AdjustmentType.HIGHLIGHT] ?: 0) != 0) {
            val t = eased(AdjustmentType.HIGHLIGHT)
            matrices += e.contrast(1f + t * 0.3f)
        }

        if ((valuesMap[AdjustmentType.SHADOW] ?: 0) != 0) {
            val t = eased(AdjustmentType.SHADOW)
            matrices += e.brightness(t * 45f)
        }

        if ((valuesMap[AdjustmentType.FADE] ?: 0) != 0) {
            val t = eased(AdjustmentType.FADE)
            if (t >= 0f) {
                matrices += e.contrast(1f - t * 0.35f)
                matrices += e.brightness(t * 30f)
            } else {
                matrices += e.contrast(1f - t * 0.2f)
            }
        }

        val warmth = valuesMap[AdjustmentType.WARMTH] ?: 0
        val tint = valuesMap[AdjustmentType.TINT] ?: 0
        if (warmth != 0 || tint != 0) {
            val tw = eased(AdjustmentType.WARMTH)
            val tt = eased(AdjustmentType.TINT)
            val temperature = 5000f + tw * 1800f
            val tintAmount = tt * 0.22f
            matrices += e.whiteBalance(temperature, tintAmount)
        }

        if ((valuesMap[AdjustmentType.SATURATION] ?: 0) != 0) {
            val t = eased(AdjustmentType.SATURATION)
            val factor = if (t >= 0f) 1f + t * 0.7f else 1f + t
            matrices += e.saturation(factor.coerceAtLeast(0f))
        }

        if ((valuesMap[AdjustmentType.HUE] ?: 0) != 0) {
            val t = eased(AdjustmentType.HUE)
            matrices += e.hueRotate(t * 180f)
        }

        val matrix = if (matrices.isEmpty()) e.identity() else e.combine(*matrices.toTypedArray())
        return FilterSpec(matrix)
    }

    private fun hasAnyMatrixAdjustment(): Boolean =
        adjustViewModel.adjustmentValues.value.filterKeys { it != AdjustmentType.SHARPEN && it != AdjustmentType.AUTO }.values.any { it != 0 }

    private fun sharpenAmount(): Int = adjustViewModel.adjustmentValues.value[AdjustmentType.SHARPEN] ?: 0

    private fun scheduleRender(debounceMs: Long, isPreview: Boolean = false) {
        val bitmap = originalBitmap ?: return
        renderJob?.cancel()

        if (!hasAnyMatrixAdjustment() && sharpenAmount() == 0) {
            imageView?.colorFilter = null
            imageView?.setImageBitmap(bitmap)
            val old = lastBitmap
            lastBitmap = null
            recycleIfSafe(old, bitmap)
            return
        }

        renderJob = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            try {
                if (debounceMs > 0) delay(debounceMs)
                ensureActive()

                val targetBitmap = if (isPreview && (bitmap.width > 1024 || bitmap.height > 1024)) {
                    val scale = 1024f / max(bitmap.width, bitmap.height)
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).roundToInt().coerceAtLeast(1),
                        (bitmap.height * scale).roundToInt().coerceAtLeast(1),
                        true
                    )
                } else {
                    bitmap
                }

                var result = if (hasAnyMatrixAdjustment()) {
                    ColorMatrixEngine.render(targetBitmap, buildCombinedSpec())
                } else {
                    targetBitmap.copy(targetBitmap.config ?: Bitmap.Config.ARGB_8888, false)
                }

                val amount = sharpenAmount()
                if (amount != 0 && isActive) {
                    val sharpened = applySharpenConvolution(result, amount)
                    if (result != targetBitmap) result.recycle()
                    result = sharpened
                }

                if (targetBitmap != bitmap && targetBitmap != result && !targetBitmap.isRecycled) {
                    targetBitmap.recycle()
                }

                ensureActive()

                withContext(Dispatchers.Main) {
                    imageView?.colorFilter = null
                    imageView?.setImageBitmap(result)
                    val old = lastBitmap
                    lastBitmap = result
                    recycleIfSafe(old, result)
                }
            } catch (e: CancellationException) {
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { cleanup() }
            } catch (e: Exception) {
                Log.e("AdjustBottomSheet", "Render error: ${e.message}")
            }
        }
    }

    private suspend fun applySharpenConvolution(source: Bitmap, amount: Int): Bitmap {
        val rawT = amount / 100f
        val strength = ease(rawT)
        val w = source.width
        val h = source.height
        if (w < 3 || h < 3) return source.copy(source.config ?: Bitmap.Config.ARGB_8888, false)

        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val out = IntArray(w * h)

        val k = abs(strength) * 1.5f
        val center = if (strength >= 0f) 1f + 4f * k else 1f - 3f * k
        val edge = if (strength >= 0f) -k else k * 0.75f

        for (y in 0 until h) {
            if (y % 32 == 0) currentCoroutineContext().ensureActive()

            val yUp = if (y == 0) 0 else y - 1
            val yDown = if (y == h - 1) h - 1 else y + 1
            val yW = y * w
            val yUpW = yUp * w
            val yDownW = yDown * w

            for (x in 0 until w) {
                val xLeft = if (x == 0) 0 else x - 1
                val xRight = if (x == w - 1) w - 1 else x + 1

                val pC = pixels[yW + x]
                val pU = pixels[yUpW + x]
                val pD = pixels[yDownW + x]
                val pL = pixels[yW + xLeft]
                val pR = pixels[yW + xRight]

                val a = (pC ushr 24) and 0xFF

                val rC = (pC ushr 16) and 0xFF
                val rU = (pU ushr 16) and 0xFF
                val rD = (pD ushr 16) and 0xFF
                val rL = (pL ushr 16) and 0xFF
                val rR = (pR ushr 16) and 0xFF
                val r = (rC * center + (rU + rD + rL + rR) * edge).roundToInt().coerceIn(0, 255)

                val gC = (pC ushr 8) and 0xFF
                val gU = (pU ushr 8) and 0xFF
                val gD = (pD ushr 8) and 0xFF
                val gL = (pL ushr 8) and 0xFF
                val gR = (pR ushr 8) and 0xFF
                val g = (gC * center + (gU + gD + gL + gR) * edge).roundToInt().coerceIn(0, 255)

                val bC = pC and 0xFF
                val bU = pU and 0xFF
                val bD = pD and 0xFF
                val bL = pL and 0xFF
                val bR = pR and 0xFF
                val b = (bC * center + (bU + bD + bL + bR) * edge).roundToInt().coerceIn(0, 255)

                out[yW + x] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        val result = Bitmap.createBitmap(w, h, source.config ?: Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, w, 0, 0, w, h)
        return result
    }

    private fun recycleIfSafe(old: Bitmap?, keep: Bitmap?) {
        try {
            if (old != null && !old.isRecycled && old != keep && old != originalBitmap) {
                old.recycle()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applyAdjustments() {
        wasApplied = true
        val values = adjustViewModel.adjustmentValues.value
        sessionViewModel.applyAdjustment(values)
        lastBitmap = null
        onAdjustApplied?.invoke(null)
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!wasApplied) {
            imageView?.colorFilter = null
            baselineBitmap?.let { safe -> if (!safe.isRecycled) imageView?.setImageBitmap(safe) }
        }
    }

    fun setOnAdjustApplied(callback: (Bitmap?) -> Unit) { onAdjustApplied = callback }
    fun setImageView(view: ImageView) { imageView = view }
    fun setOriginalBitmap(bitmap: Bitmap) { originalBitmap = bitmap }

    private fun runAutoEnhance() {
        val baseline = originalBitmap ?: return
        autoAnalysisJob?.cancel()

        val autoIntensity = (adjustViewModel.adjustmentValues.value[AdjustmentType.AUTO] ?: 100) / 100f

        autoAnalysisJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val calculatedValues = withContext(Dispatchers.Default) {
                    AutoEnhanceCalculator.calculate(baseline)
                }
                if (!isActive) return@launch

                val scaledAutoValues = calculatedValues.mapValues { (_, value) ->
                    (value * autoIntensity).roundToInt()
                }

                applyAutoValues(scaledAutoValues)
            } catch (e: CancellationException) {
            } catch (e: Exception) {
                Log.e("AdjustBottomSheet", "Auto enhance failed: ${e.message}")
            }
        }
    }

    private fun applyAutoValues(autoValues: Map<AdjustmentType, Int>) {
        AdjustmentType.values()
            .filter { it != AdjustmentType.AUTO }
            .forEach { type -> adjustViewModel.updateValue(type, autoValues[type] ?: 0) }

        adjustViewModel.selectedAdjustment.value?.let { updateAdjustmentUI(it) }

        originalBitmap?.let { imageView?.setImageBitmap(it) }
        imageView?.colorFilter = ColorMatrixEngine.asColorFilter(buildCombinedSpec())

        scheduleRender(debounceMs = 0, isPreview = false)
    }

    private fun cleanup() {
        renderJob?.cancel()
        autoAnalysisJob?.cancel()
        lastBitmap?.let { if (!it.isRecycled && it != originalBitmap) it.recycle() }
        lastBitmap = null
    }

    override fun onDestroyView() {
        cleanup()
        imageView?.colorFilter = null
        adjustmentAdapter = null
        imageView = null
        originalBitmap = null
        onAdjustApplied = null
        super.onDestroyView()
    }

    companion object {
        fun newInstance(
            imageUri: Uri?,
            view: ImageView,
            bitmap: Bitmap,
            onAdjustApplied: (Bitmap?) -> Unit
        ): AdjustBottomSheet {
            return AdjustBottomSheet().apply {
                this.imageUri = imageUri
                setImageView(view)
                setOriginalBitmap(bitmap)
                setOnAdjustApplied(onAdjustApplied)
            }
        }
    }
}