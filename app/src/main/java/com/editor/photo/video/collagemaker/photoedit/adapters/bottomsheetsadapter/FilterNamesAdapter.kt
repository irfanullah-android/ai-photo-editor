package com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemFilterNameBinding
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorFilter

class FilterNamesAdapter(
    private val filters: List<EditorFilter>,
    private val onFilterClick: (EditorFilter) -> Unit
) : RecyclerView.Adapter<FilterNamesAdapter.FilterNameViewHolder>() {

    private var selectedPosition = 0

    inner class FilterNameViewHolder(private val binding: ItemFilterNameBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(filter: EditorFilter, isSelected: Boolean) {
            binding.tvFilterName.text = filter.displayName

            // Update colors based on selection
            if (isSelected) {
                binding.tvFilterName.setTextColor(0xFFFFFFFF.toInt())
                binding.tvFilterName.textSize = 16f
                binding.tvFilterName.setTypeface(null, Typeface.BOLD)
            } else {
                binding.tvFilterName.setTextColor(0xFFAAAAAA.toInt())
                binding.tvFilterName.textSize = 16f
                binding.tvFilterName.setTypeface(null, Typeface.NORMAL)
            }

            // 🔥 FIXED: Handle click properly
            binding.root.setOnClickListener {
                val clickedPosition = adapterPosition
                if (clickedPosition != RecyclerView.NO_POSITION) {
                    // Update UI first
                    if (selectedPosition != clickedPosition) {
                        val old = selectedPosition
                        selectedPosition = clickedPosition
                        notifyItemChanged(old)
                        notifyItemChanged(selectedPosition)
                    }
                    // Always trigger callback
                    onFilterClick(filter)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterNameViewHolder {
        val binding = ItemFilterNameBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FilterNameViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FilterNameViewHolder, position: Int) {
        holder.bind(filters[position], position == selectedPosition)
    }

    override fun getItemCount() = filters.size

    fun updateSelection(position: Int) {
        if (position in 0 until filters.size && position != selectedPosition) {
            val old = selectedPosition
            selectedPosition = position
            notifyItemChanged(old)
            notifyItemChanged(selectedPosition)
        }
    }

    fun getSelectedFilter(): EditorFilter = filters[selectedPosition]
}