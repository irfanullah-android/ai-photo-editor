package com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemAdjustmentBinding
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.AdjustmentModel

class AdjustmentAdapter(
    private val adjustments: List<AdjustmentModel>,
    private val onAdjustmentClick: (AdjustmentModel, Int) -> Unit
) : RecyclerView.Adapter<AdjustmentAdapter.AdjustmentViewHolder>() {

    inner class AdjustmentViewHolder(
        private val binding: ItemAdjustmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(adjustment: AdjustmentModel, position: Int) {
            binding.apply {
                tvAdjustmentName.text = adjustment.name
                ivAdjustmentIcon.setImageResource(adjustment.iconRes)

                if (adjustment.isSelected) {
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
                    adjustments.forEach { it.isSelected = false }
                    adjustment.isSelected = true
                    notifyDataSetChanged()
                    onAdjustmentClick(adjustment, position)
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