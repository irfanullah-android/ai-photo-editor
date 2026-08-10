package com.editor.photo.video.collagemaker.photoedit.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemGlowEffectBinding
import com.editor.photo.video.collagemaker.photoedit.models.PhotoTool

class AiGroupPhotoAdapter(
    private val onItemClick: (PhotoTool) -> Unit
) : ListAdapter<PhotoTool, AiGroupPhotoAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PhotoTool>() {
            override fun areItemsTheSame(oldItem: PhotoTool, newItem: PhotoTool) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: PhotoTool, newItem: PhotoTool) =
                oldItem == newItem
        }
    }

    inner class ViewHolder(
        private val binding: ItemGlowEffectBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PhotoTool) {
            binding.tvGlowTitle.text = item.title
            binding.ivGlowImage.load(item.imageRes) { crossfade(true) }
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGlowEffectBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )

        // Same logic — exactly 3 cards visible
        val density = parent.resources.displayMetrics.density
        val horizontalPadding = (16 * 2 * density).toInt()
        val totalGaps = (8 * 2 * density).toInt()
        val itemWidth = (parent.measuredWidth - horizontalPadding - totalGaps) / 3

        binding.root.layoutParams = ViewGroup.LayoutParams(
            itemWidth,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}