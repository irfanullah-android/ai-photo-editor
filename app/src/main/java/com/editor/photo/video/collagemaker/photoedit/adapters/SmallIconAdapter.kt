package com.editor.photo.video.collagemaker.photoedit.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemSmallIconBinding
import com.editor.photo.video.collagemaker.photoedit.models.SmallIconItem

class
SmallIconAdapter(
    private val onItemClick: (SmallIconItem) -> Unit
) : ListAdapter<SmallIconItem, SmallIconAdapter.IconViewHolder>(DiffCallback()) {

    inner class IconViewHolder(
        private val binding: ItemSmallIconBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SmallIconItem) {
            binding.ivSmallIcon.setImageResource(item.iconRes)
            binding.tvSmallIconLabel.text = item.label
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val binding = ItemSmallIconBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )

        // 2 rows, 5 columns visible = 10 items visible at once
        // Item width = screenWidth / 5 (5 columns)
        val screenWidth = parent.context.resources.displayMetrics.widthPixels
        val itemWidth = screenWidth / 5

        binding.root.layoutParams = ViewGroup.LayoutParams(
            itemWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT  // height auto — dono rows equal hongi
        )

        return IconViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class DiffCallback : DiffUtil.ItemCallback<SmallIconItem>() {
        override fun areItemsTheSame(old: SmallIconItem, new: SmallIconItem) =
            old.id == new.id
        override fun areContentsTheSame(old: SmallIconItem, new: SmallIconItem) =
            old == new
    }
}