package com.editor.photo.video.collagemaker.photoedit.adapters

import android.R
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemEffectNameBinding
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EffectModel

class EffectNameAdapter(
    private val effects: List<EffectModel>,
    private val onEffectClick: (EffectModel, Int) -> Unit
) : RecyclerView.Adapter<EffectNameAdapter.EffectNameViewHolder>() {

    private var selectedPosition = 0

    // Kept so we can trigger a centered smooth-scroll whenever selection changes,
    // either from a user tap or from an external updateSelection() call
    // (e.g. when swiping through effect previews).
    private var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    inner class EffectNameViewHolder(private val binding: ItemEffectNameBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(effect: EffectModel, position: Int) {
            binding.tvEffectName.text = effect.name

            // Match filter-name selected style: bold + brighter when selected.
            if (position == selectedPosition) {
                binding.tvEffectName.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.white)
                )
                binding.tvEffectName.textSize = 14f
                binding.tvEffectName.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                binding.tvEffectName.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.darker_gray)
                )
                binding.tvEffectName.textSize = 14f
                binding.tvEffectName.setTypeface(null, android.graphics.Typeface.NORMAL)
            }

            binding.root.setOnClickListener {
                val clickedPosition = adapterPosition
                if (clickedPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                if (selectedPosition != clickedPosition) {
                    val previousPosition = selectedPosition
                    selectedPosition = clickedPosition
                    notifyItemChanged(previousPosition)
                    notifyItemChanged(selectedPosition)
                }

                scrollToCenter(clickedPosition)
                onEffectClick(effect, clickedPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EffectNameViewHolder {
        val binding = ItemEffectNameBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EffectNameViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EffectNameViewHolder, position: Int) {
        holder.bind(effects[position], position)
    }

    override fun getItemCount() = effects.size

    /** Call this when selection changes from outside (e.g. syncing with the
     *  effect-preview adapter). Updates state and auto-scrolls to center. */
    fun updateSelection(position: Int) {
        if (position !in effects.indices || position == selectedPosition) return

        val previousPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousPosition)
        notifyItemChanged(selectedPosition)
        scrollToCenter(position)
    }

    /** Smoothly scrolls the RecyclerView so the item at [position] ends up
     *  centered horizontally, same feel as the filter-name row. */
    private fun scrollToCenter(position: Int) {
        val rv = recyclerView ?: return
        val layoutManager = rv.layoutManager as? LinearLayoutManager ?: return

        val smoothScroller = object : LinearSmoothScroller(rv.context) {
            override fun calculateDtToFit(
                viewStart: Int,
                viewEnd: Int,
                boxStart: Int,
                boxEnd: Int,
                snapPreference: Int
            ): Int {
                // Centers the target view within the RecyclerView's viewport,
                // instead of just snapping it to the nearest edge.
                return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)
            }

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return 60f / displayMetrics.densityDpi
            }
        }

        smoothScroller.targetPosition = position
        layoutManager.startSmoothScroll(smoothScroller)
    }
}