package com.editor.photo.video.collagemaker.photoedit.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemThumbnailBinding
import com.editor.photo.video.collagemaker.photoedit.models.ThumbnailItem

class ThumbnailAdapter(
    items: List<ThumbnailItem>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<ThumbnailAdapter.ViewHolder>() {

    private val itemList = items.toMutableList()
    private var selectedPosition: Int = 0

    inner class ViewHolder(val binding: ItemThumbnailBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemThumbnailBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]
        val isSelected = (position == selectedPosition)

        val radius = (10 * holder.itemView.context.resources.displayMetrics.density).toInt()

        Glide.with(holder.itemView.context)
            .load(item.imageResId)
            .override(120, 144)
            .centerCrop()
            .dontAnimate()
            .transform(RoundedCorners(radius))
            .placeholder(R.drawable.bg_thumb_normal)
            .into(holder.binding.viewThumb)



        holder.binding.viewSelectedFrame.visibility =
            if (isSelected) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_POSITION || pos == selectedPosition) return@setOnClickListener
            val previous = selectedPosition
            selectedPosition = pos
            notifyItemChanged(previous)
            notifyItemChanged(pos)
            onItemClick(pos)
        }
    }


    override fun getItemCount(): Int = itemList.size

    fun selectPosition(position: Int) {
        if (position == selectedPosition || position !in itemList.indices) return
        val previous = selectedPosition
        selectedPosition = position
        notifyItemChanged(previous)
        notifyItemChanged(position)
    }

    fun getSelectedPosition(): Int = selectedPosition
}