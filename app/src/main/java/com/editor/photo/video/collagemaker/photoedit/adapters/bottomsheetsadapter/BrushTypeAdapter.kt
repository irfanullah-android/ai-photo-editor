package com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemBrushTypeBinding
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.BrushItem

class BrushTypeAdapter(
    private val brushItems: List<BrushItem>,
    private val onBrushSelected: (BrushItem) -> Unit
) : RecyclerView.Adapter<BrushTypeAdapter.BrushViewHolder>() {

    private var selectedPosition = 0
    private var currentColor: Int = Color.RED

    fun updateColor(color: Int) {
        if (this.currentColor != color) {
            this.currentColor = color
            if (selectedPosition in brushItems.indices) {
                notifyItemChanged(selectedPosition)
            }
        }
    }

    inner class BrushViewHolder(private val binding: ItemBrushTypeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(brushItem: BrushItem, isSelected: Boolean) {
            binding.ivBrushIcon.setImageResource(brushItem.icon)

            // Direct state setting for zero-lag rendering
            binding.root.alpha = if (isSelected) 1.0f else 0.4f
            binding.root.scaleX = if (isSelected) 1.1f else 1.0f
            binding.root.scaleY = if (isSelected) 1.1f else 1.0f

            // Clean background highlight
            (binding.root.background?.mutate() as? GradientDrawable)?.let { background ->
                val density = binding.root.resources.displayMetrics.density
                if (isSelected) {
                    background.setColor(Color.parseColor("#33FFFFFF"))
                    background.setStroke((2 * density).toInt(), currentColor)
                } else {
                    background.setColor(Color.parseColor("#1AFFFFFF"))
                    background.setStroke((1 * density).toInt(), Color.parseColor("#33FFFFFF"))
                }
            }

            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && selectedPosition != position) {
                    val previousSelected = selectedPosition
                    selectedPosition = position

                    // Synchronous dispatch - immediate UI feedback
                    notifyItemChanged(previousSelected)
                    notifyItemChanged(selectedPosition)

                    onBrushSelected(brushItem)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrushViewHolder {
        val binding = ItemBrushTypeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BrushViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BrushViewHolder, position: Int) {
        holder.bind(brushItems[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = brushItems.size
}