package com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine.EnhanceEngine
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorEnhance
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EnhanceAdapter(
    private val context: Context,
    private val list: List<EditorEnhance>,
    private val originalBitmap: Bitmap?,
    private val onClick: (EditorEnhance) -> Unit
) : RecyclerView.Adapter<EnhanceAdapter.VH>() {

    private var selectedPosition = 0
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    private val previewCache = mutableMapOf<EditorEnhance, Bitmap>()
    private val inFlight = mutableSetOf<EditorEnhance>()

    private val previewSizePx: Int by lazy {
        val dp = 64f
        val px = (dp * context.resources.displayMetrics.density).toInt()
        (px * 1.2f).toInt().coerceAtLeast(64)
    }

    /**
     * ONE shared downsampled copy of originalBitmap, built once, reused by
     * every tool's thumbnail render. This is the actual fix for "late load":
     * before, each of the 13 tools independently scaled the FULL-RES source
     * bitmap before rendering — 13x redundant heavy downscales. Now the
     * expensive downscale happens exactly once, up front, off the main
     * thread, and every tool just renders against this small copy.
     *
     * Sized at 2x previewSizePx (not 1x) so the final centerCrop-to-size
     * step downsamples INTO the thumbnail instead of upscaling a
     * same-size/blurry source into it — this is what fixes the pixelation.
     */
    private val sharedPreviewSource: Deferred<Bitmap?> by lazy {
        scope.async {
            val src = originalBitmap ?: return@async null
            if (src.isRecycled) return@async null
            try {
                val targetLong = previewSizePx * 2
                val scale = targetLong.toFloat() / maxOf(src.width, src.height)
                if (scale >= 1f) return@async src // already small, no need to copy
                val w = (src.width * scale).toInt().coerceAtLeast(1)
                val h = (src.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(src, w, h, true)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /** Per-tool demo strength — Exposure toned way down since its curve (2^(v*2)) is much steeper than the others. */
    private fun demoStrengthFor(tool: EditorEnhance): Float = when (tool) {
        EditorEnhance.EXPOSURE -> 0.18f
        EditorEnhance.CONTRAST -> 0.45f
        EditorEnhance.WHITES -> 0.45f
        EditorEnhance.BLACKS -> 0.45f
        EditorEnhance.DEHAZE -> 0.45f
        else -> 0.6f
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvName)
        val preview: ImageView = view.findViewById(R.id.ivPreview)
        val selectedIndicator: View? = view.findViewById(R.id.vSelected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_filter, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tool = list[position]
        holder.name.text = tool.displayName

        val isSelected = position == selectedPosition
        holder.selectedIndicator?.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.itemView.alpha = if (isSelected) 1.0f else 0.7f

        val cached = previewCache[tool]
        if (cached != null && !cached.isRecycled) {
            holder.preview.setImageBitmap(cached)
        } else {
            holder.preview.setImageResource(R.drawable.ic_enhance) // instant placeholder, not blank

            if (tool !in inFlight) {
                inFlight.add(tool)
                scope.launch {
                    try {
                        val base = sharedPreviewSource.await() ?: return@launch
                        if (base.isRecycled) return@launch

                        val rendered =
                            generateEnhancePreview(base, tool, previewSizePx, demoStrengthFor(tool))
                                ?: return@launch

                        previewCache[tool] = rendered
                        inFlight.remove(tool)

                        withContext(Dispatchers.Main) {
                            if (holder.adapterPosition == position) {
                                holder.preview.setImageBitmap(rendered)
                            } else {
                                notifyItemChanged(list.indexOf(tool))
                            }
                        }
                    } catch (e: CancellationException) {
                        inFlight.remove(tool)
                    } catch (e: Exception) {
                        inFlight.remove(tool)
                        e.printStackTrace()
                    }
                }
            }
        }

        holder.itemView.setOnClickListener {
            val old = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(old)
            notifyItemChanged(selectedPosition)
            onClick(tool)
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateSelection(position: Int) {
        val old = selectedPosition
        selectedPosition = position
        notifyItemChanged(old)
        notifyItemChanged(selectedPosition)
    }

    fun cleanup() {
        job.cancel()
        previewCache.values.forEach { if (!it.isRecycled) it.recycle() }
        previewCache.clear()
        inFlight.clear()
    }

    companion object {
        /** Renders tool@strength against the already-small [base] and center-crops down to exactly [targetSize] — the downsample-in step that keeps thumbnails sharp instead of soft/pixelated. */
        private fun generateEnhancePreview(
            base: Bitmap,
            tool: EditorEnhance,
            targetSize: Int,
            strength: Float
        ): Bitmap? {
            if (base.isRecycled) return null

            val size = targetSize.coerceAtLeast(64)
            val scale = maxOf(
                size.toFloat() / base.width,
                size.toFloat() / base.height
            )
            val scaledWidth = (base.width * scale).toInt().coerceAtLeast(1)
            val scaledHeight = (base.height * scale).toInt().coerceAtLeast(1)

            val thumbnail = try {
                Bitmap.createScaledBitmap(base, scaledWidth, scaledHeight, true)
            } catch (e: IllegalArgumentException) {
                return null
            }

            val values = EnhanceEngine.EnhanceValues().with(tool, strength)
            val rendered = EnhanceEngine.render(thumbnail, values)

            if (thumbnail != base && thumbnail != rendered) {
                thumbnail.recycle()
            }

            return rendered
        }
    }
}