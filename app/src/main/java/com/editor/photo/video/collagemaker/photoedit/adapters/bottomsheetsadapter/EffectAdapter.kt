package com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemEffectBinding
import com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine.EffectsEngine
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EffectModel
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap

class EffectAdapter(
    private val context: Context,
    private val effects: List<EffectModel>,
    private val originalBitmap: Bitmap?,
    private val onEffectClick: (EffectModel, Int) -> Unit
) : RecyclerView.Adapter<EffectAdapter.EffectViewHolder>() {

    private var selectedPosition = 0

    private val adapterScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Finished results. bind() always checks this FIRST for an instant, no-delay display. */
    private val thumbnailCache = ConcurrentHashMap<Int, Bitmap>()

    /**
     * One shared Deferred per position. computeIfAbsent guarantees a given
     * effect is generated AT MOST ONCE, no matter how many times bind()
     * is called for that position or whether preload already started it.
     */
    private val thumbnailDeferreds = ConcurrentHashMap<Int, Deferred<Bitmap>>()

    private val effectComputeSemaphore = Semaphore(2)

    private val previewSizePx: Int by lazy {
        val dp = ITEM_PREVIEW_DP
        val px = (dp * context.resources.displayMetrics.density).toInt()
        (px * 1.2f).toInt().coerceAtLeast(64) // small safety margin so crop/scale stays sharp
    }

    private var sharedPreviewBase: Bitmap? = null
    private val previewBaseMutex = Mutex()

    init {
        // Kick off generation for every effect immediately, in the background.
        // Semaphore(2) throttles actual concurrent work, so this does NOT
        // flood the CPU or block the UI thread.
        originalBitmap?.let {
            adapterScope.launch(Dispatchers.Default) {
                for (pos in effects.indices) {
                    if (!isActive) return@launch
                    getOrLaunchThumbnail(pos, effects[pos])
                }
            }
        }
    }

    private suspend fun getOrCreatePreviewBase(bitmap: Bitmap): Bitmap {
        sharedPreviewBase?.let { if (!it.isRecycled) return it }
        return previewBaseMutex.withLock {
            sharedPreviewBase?.let { if (!it.isRecycled) return@withLock it }
            val size = previewSizePx
            val scale = maxOf(
                size.toFloat() / bitmap.width,
                size.toFloat() / bitmap.height
            )
            val scaledWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val scaledHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true).also {
                sharedPreviewBase = it
            }
        }
    }

    /**
     * Runs the effect against the already-downscaled shared [base] bitmap.
     * [base] is owned by the adapter and must never be recycled here.
     * Adapter-level (not ViewHolder-level) so getOrLaunchThumbnail can call it
     * directly without needing a ViewHolder instance.
     */
    private fun createThumbnail(base: Bitmap, effect: EffectModel): Bitmap {
        return try {
            EffectsEngine.apply(effect.type, base)
        } catch (e: Exception) {
            e.printStackTrace()
            base
        }
    }

    /**
     * Returns the (single, shared) Deferred that will produce position's thumbnail.
     * If preload already created it -> returns the same in-flight/completed Deferred.
     * If not -> creates it now (fallback path), still respecting the semaphore.
     * On success the result is written into [thumbnailCache].
     */
    private fun getOrLaunchThumbnail(position: Int, effect: EffectModel): Deferred<Bitmap>? {
        val bitmap = originalBitmap ?: return null
        return thumbnailDeferreds.computeIfAbsent(position) {
            adapterScope.async(Dispatchers.Default) {
                val base = getOrCreatePreviewBase(bitmap)
                val result = effectComputeSemaphore.withPermit {
                    createThumbnail(base, effect)
                }
                thumbnailCache[position] = result
                result
            }
        }
    }

    inner class EffectViewHolder(
        val binding: ItemEffectBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        var thumbnailJob: Job? = null

        fun bind(effect: EffectModel, position: Int) {

            binding.tvEffectName.text = effect.name

            val isSelected = position == selectedPosition

            binding.vSelected.visibility =
                if (isSelected) android.view.View.VISIBLE
                else android.view.View.GONE

            binding.tvEffectName.setTextColor(
                androidx.core.content.ContextCompat.getColor(
                    context,
                    if (isSelected)
                        android.R.color.white
                    else
                        android.R.color.darker_gray
                )
            )

            thumbnailJob?.cancel()

            // 1) Cache hit -> show instantly, no animation, no work.
            thumbnailCache[position]?.let { cachedBitmap ->
                if (!cachedBitmap.isRecycled) {
                    setPreview(cachedBitmap, animate = false)
                    setupClickListener(position)
                    return
                } else {
                    thumbnailCache.remove(position)
                }
            }

            clearPreview()

            // 2) Not cached yet -> attach to the shared in-flight/preload
            //    generation for this position (fallback path). No duplicate
            //    computation is ever started.
            val deferred = getOrLaunchThumbnail(position, effect)
            if (deferred != null) {
                thumbnailJob = adapterScope.launch {
                    try {
                        val preview = deferred.await()
                        if (isActive && adapterPosition == position) {
                            setPreview(preview, animate = true)
                        }
                    } catch (_: CancellationException) {
                    } catch (e: Exception) {
                        e.printStackTrace()
                        if (isActive && adapterPosition == position) {
                            withContext(Dispatchers.Main) {
                                clearPreview()
                            }
                        }
                    }
                }
            }

            setupClickListener(position)
        }

        private fun setPreview(bitmap: Bitmap, animate: Boolean) {
            binding.ivEffectPreview.animate().cancel()
            if (animate) {
                binding.ivEffectPreview.alpha = 0f
                binding.ivEffectPreview.setImageBitmap(bitmap)
                binding.ivEffectPreview.animate()
                    .alpha(1f)
                    .setDuration(FADE_DURATION_MS)
                    .start()
            } else {
                binding.ivEffectPreview.alpha = 1f
                binding.ivEffectPreview.setImageBitmap(bitmap)
            }
            binding.ivEffectPreview.requestLayout()
            binding.ivEffectPreview.invalidate()
        }

        private fun clearPreview() {
            binding.ivEffectPreview.animate().cancel()
            binding.ivEffectPreview.alpha = 0f
            binding.ivEffectPreview.setImageDrawable(null)
        }

        private fun setupClickListener(position: Int) {
            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    updateSelection(adapterPosition)
                    onEffectClick(effects[adapterPosition], adapterPosition)
                }
            }
        }

        fun clear() {
            thumbnailJob?.cancel()
            binding.ivEffectPreview.animate().cancel()
            binding.ivEffectPreview.alpha = 1f
            binding.ivEffectPreview.setImageDrawable(null)
            binding.root.setOnClickListener(null)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EffectViewHolder {
        val binding = ItemEffectBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EffectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EffectViewHolder, position: Int) {
        holder.bind(effects[position], position)
    }

    override fun getItemCount() = effects.size

    override fun onViewRecycled(holder: EffectViewHolder) {
        super.onViewRecycled(holder)
        holder.clear()
    }

    override fun onViewDetachedFromWindow(holder: EffectViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.thumbnailJob?.cancel()
    }

    fun updateSelection(position: Int) {
        if (position == selectedPosition || position < 0 || position >= effects.size) return

        val old = selectedPosition
        selectedPosition = position
        notifyItemChanged(old)
        notifyItemChanged(position)
    }

    fun clear() {
        adapterScope.cancel()

        thumbnailCache.values.forEach { bitmap ->
            if (!bitmap.isRecycled && bitmap != originalBitmap && bitmap !== sharedPreviewBase) {
                bitmap.recycle()
            }
        }
        thumbnailCache.clear()
        thumbnailDeferreds.clear()

        sharedPreviewBase?.let {
            if (!it.isRecycled && it !== originalBitmap) {
                it.recycle()
            }
        }
        sharedPreviewBase = null
    }

    companion object {
        private const val ITEM_PREVIEW_DP = 64f
        private const val FADE_DURATION_MS = 180L
    }
}