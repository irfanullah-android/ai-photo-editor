package com.editor.photo.video.collagemaker.photoedit.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemPopularFeatureBinding
import com.editor.photo.video.collagemaker.photoedit.models.PopularFeature

class PopularFeatureAdapter(
    private val onItemClick: (PopularFeature) -> Unit
) : ListAdapter<PopularFeature, PopularFeatureAdapter.PopularFeatureViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PopularFeature>() {
            override fun areItemsTheSame(oldItem: PopularFeature, newItem: PopularFeature) =
                oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: PopularFeature, newItem: PopularFeature) =
                oldItem == newItem
        }
    }

    inner class PopularFeatureViewHolder(
        private val binding: ItemPopularFeatureBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PopularFeature) {
            binding.ivFeatureImage.setImageResource(item.imageRes)
            binding.tvFeatureTitle.text = item.title
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PopularFeatureViewHolder {
        val binding = ItemPopularFeatureBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PopularFeatureViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PopularFeatureViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
