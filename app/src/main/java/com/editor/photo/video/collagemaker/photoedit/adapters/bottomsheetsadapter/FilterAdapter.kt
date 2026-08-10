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
import com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine.ColorMatrixEngine
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorFilter
import kotlinx.coroutines.*

class FilterAdapter(
    private val context: Context,
    private val list: List<EditorFilter>,
    private val originalBitmap: Bitmap?,
    private val onClick: (EditorFilter) -> Unit
) : RecyclerView.Adapter<FilterAdapter.VH>() {

    private var selectedPosition = 0
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    private val previewCache = mutableMapOf<EditorFilter, Bitmap>()

    /**
     * ivPreview is a FIXED 64dp x 64dp ImageView (see item_filter.xml).
     * The old hardcoded 120px thumbnail only matched ~hdpi screens — on
     * xhdpi/xxhdpi/xxxhdpi (64dp = 128/192/256px) Android had to upscale
     * that 120px bitmap to fill the view, which is what caused the
     * pixelation. Resolving the real pixel size from density here means
     * the thumbnail is generated at (or slightly above) native display
     * resolution on every device, so it's never stretched.
     */
    private val previewSizePx: Int by lazy {
        val dp = 64f
        val px = (dp * context.resources.displayMetrics.density).toInt()
        // Small safety margin above exact size so centerCrop scaling
        // (which can sample slightly larger than the view) still stays sharp.
        (px * 1.2f).toInt().coerceAtLeast(64)
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
        val filter = list[position]
        holder.name.text = filter.displayName

        val isSelected = position == selectedPosition
        holder.selectedIndicator?.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.itemView.alpha = if (isSelected) 1.0f else 0.7f

        if (previewCache.containsKey(filter)) {
            holder.preview.setImageBitmap(previewCache[filter])
        } else {
            holder.preview.setImageDrawable(null)

            originalBitmap?.let { bitmap ->
                scope.launch {
                    val previewBitmap = generateFilterPreview(bitmap, filter, previewSizePx)
                    previewCache[filter] = previewBitmap

                    withContext(Dispatchers.Main) {
                        if (holder.adapterPosition == position) {
                            holder.preview.setImageBitmap(previewBitmap)
                        }
                    }
                }
            } ?: holder.preview.setImageResource(R.drawable.ic_filter)
        }

        holder.itemView.setOnClickListener {
            val old = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(old)
            notifyItemChanged(selectedPosition)
            onClick(filter)
        }
    }

    override fun getItemCount(): Int = list.size

    override fun onViewRecycled(holder: VH) {
        super.onViewRecycled(holder)
        // Don't clear cached previews to save memory
    }

    fun updateSelection(position: Int) {
        val old = selectedPosition
        selectedPosition = position
        notifyItemChanged(old)
        notifyItemChanged(selectedPosition)
    }

    fun cleanup() {
        job.cancel()
        previewCache.clear()
    }

    companion object {
        /**
         * Generates a preview bitmap sized to the real on-screen pixel size
         * of ivPreview (targetSize), using pure android.graphics.ColorMatrix.
         * No external image library involved.
         */
        private fun generateFilterPreview(
            bitmap: Bitmap,
            filter: EditorFilter,
            targetSize: Int
        ): Bitmap {
            val size = targetSize.coerceAtLeast(64)
            val scale = maxOf(
                size.toFloat() / bitmap.width,
                size.toFloat() / bitmap.height
            )
            val scaledWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val scaledHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)

            val thumbnail = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

            if (filter == EditorFilter.NORMAL) {
                return thumbnail
            }

            val spec = filter.buildFilterSpec(80)
            val filteredBitmap = ColorMatrixEngine.render(thumbnail, spec)

            if (thumbnail != bitmap && thumbnail != filteredBitmap) {
                thumbnail.recycle()
            }

            return filteredBitmap
        }
    }
}