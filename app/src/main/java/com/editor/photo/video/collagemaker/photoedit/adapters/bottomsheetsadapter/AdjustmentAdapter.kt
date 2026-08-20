package com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemAdjustmentBinding
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.AdjustmentModel

class AdjustmentAdapter(
    private val adjustments: MutableList<AdjustmentModel>,
    private val onAdjustmentClick: (AdjustmentModel, Int) -> Unit
) : RecyclerView.Adapter<AdjustmentAdapter.AdjustmentViewHolder>() {

    private var selectedPosition = adjustments.indexOfFirst { it.isSelected }.coerceAtLeast(0)

    inner class AdjustmentViewHolder(
        private val binding: ItemAdjustmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(adjustment: AdjustmentModel, position: Int) {
            binding.apply {
                tvAdjustmentName.text = adjustment.name
                ivAdjustmentIcon.setImageResource(adjustment.iconRes)

                val isSelected = position == selectedPosition

                if (isSelected) {
                    cardAdjustment.setCardBackgroundColor(
                        ContextCompat.getColor(root.context, R.color.white)
                    )
                    ivAdjustmentIcon.setColorFilter(
                        ContextCompat.getColor(root.context, R.color.black)
                    )
                } else {
                    cardAdjustment.setCardBackgroundColor(
                        ContextCompat.getColor(root.context, R.color.dark_gray)
                    )
                    ivAdjustmentIcon.setColorFilter(
                        ContextCompat.getColor(root.context, R.color.white)
                    )
                }

                tvAdjustmentName.setTextColor(
                    ContextCompat.getColor(root.context, R.color.white)
                )

                root.setOnClickListener {
                    val previousSelected = selectedPosition
                    selectedPosition = adapterPosition

                    if (selectedPosition != RecyclerView.NO_POSITION) {
                        adjustments.getOrNull(previousSelected)?.isSelected = false
                        adjustments.getOrNull(selectedPosition)?.isSelected = true

                        notifyItemChanged(previousSelected)
                        notifyItemChanged(selectedPosition)

                        onAdjustmentClick(adjustments[selectedPosition], selectedPosition)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdjustmentViewHolder {
        val binding = ItemAdjustmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AdjustmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdjustmentViewHolder, position: Int) {
        holder.bind(adjustments[position], position)
    }

    override fun getItemCount(): Int = adjustments.size
}