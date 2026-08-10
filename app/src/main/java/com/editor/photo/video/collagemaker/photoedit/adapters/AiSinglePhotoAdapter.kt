package com.editor.photo.video.collagemaker.photoedit.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemGlamToolBinding
import com.editor.photo.video.collagemaker.photoedit.models.PhotoTool

class AiSinglePhotoAdapter(
    private val onItemClick: (PhotoTool) -> Unit
) : ListAdapter<PhotoTool, AiSinglePhotoAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PhotoTool>() {
            override fun areItemsTheSame(oldItem: PhotoTool, newItem: PhotoTool) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: PhotoTool, newItem: PhotoTool) =
                oldItem == newItem
        }
    }

    inner class ViewHolder(
        private val binding: ItemGlamToolBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PhotoTool) {
            binding.tvGlamTitle.text = item.title
            binding.ivGlamImage.load(item.imageRes) { crossfade(true) }
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGlamToolBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )

        // Exactly 3 cards visible — padding 16dp start+end, gap 8dp between cards
        val density = parent.resources.displayMetrics.density
        val horizontalPadding = (16 * 2 * density).toInt()  // start + end padding of RV
        val totalGaps = (8 * 2 * density).toInt()            // 2 gaps between 3 cards
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