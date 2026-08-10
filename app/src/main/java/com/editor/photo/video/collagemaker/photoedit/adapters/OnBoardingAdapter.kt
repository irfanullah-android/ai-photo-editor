package com.editor.photo.video.collagemaker.photoedit.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.editor.photo.video.collagemaker.photoedit.databinding.OnBoardingLayoutBinding
import com.editor.photo.video.collagemaker.photoedit.models.OnBoardingItem

class OnBoardingAdapter(private val list: List<OnBoardingItem>) :
    RecyclerView.Adapter<OnBoardingAdapter.OnBoardingHolder>() {

    init {
        setHasStableIds(true)
    }

    inner class OnBoardingHolder(val binding: OnBoardingLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnBoardingHolder {
        val binding = OnBoardingLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )

        return OnBoardingHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun getItemId(position: Int): Long = position.toLong()

    override fun onBindViewHolder(holder: OnBoardingHolder, position: Int) {
        val currentItem = list[position]

        // Image set kar rahe hain
        Glide.with(holder.binding.image.context)
            .load(currentItem.imageResId)
            .into(holder.binding.image)
    }
}