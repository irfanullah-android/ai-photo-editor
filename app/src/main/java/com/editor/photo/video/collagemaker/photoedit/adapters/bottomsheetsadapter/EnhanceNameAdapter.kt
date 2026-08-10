package com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemFilterNameBinding
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorEnhance

/**
 * Exact mirror of FilterNamesAdapter.kt — same ItemFilterNameBinding layout,
 * same tvFilterName id, same bind()/selection pattern. The previous version
 * of this file guessed a different id (tvName) that doesn't exist in
 * item_filter_name.xml, which is what caused the findViewById NPE crash.
 */
class EnhanceNamesAdapter(
    private val tools: List<EditorEnhance>,
    private val onToolClick: (EditorEnhance) -> Unit
) : RecyclerView.Adapter<EnhanceNamesAdapter.EnhanceNameViewHolder>() {

    private var selectedPosition = 0

    inner class EnhanceNameViewHolder(private val binding: ItemFilterNameBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(tool: EditorEnhance, isSelected: Boolean) {
            binding.tvFilterName.text = tool.displayName

            if (isSelected) {
                binding.tvFilterName.setTextColor(0xFFFFFFFF.toInt())
                binding.tvFilterName.textSize = 16f
                binding.tvFilterName.setTypeface(null, Typeface.BOLD)
            } else {
                binding.tvFilterName.setTextColor(0xFFAAAAAA.toInt())
                binding.tvFilterName.textSize = 16f
                binding.tvFilterName.setTypeface(null, Typeface.NORMAL)
            }

            binding.root.setOnClickListener {
                val clickedPosition = adapterPosition
                if (clickedPosition != RecyclerView.NO_POSITION) {
                    if (selectedPosition != clickedPosition) {
                        val old = selectedPosition
                        selectedPosition = clickedPosition
                        notifyItemChanged(old)
                        notifyItemChanged(selectedPosition)
                    }
                    onToolClick(tool)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EnhanceNameViewHolder {
        val binding = ItemFilterNameBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EnhanceNameViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EnhanceNameViewHolder, position: Int) {
        holder.bind(tools[position], position == selectedPosition)
    }

    override fun getItemCount() = tools.size

    fun updateSelection(position: Int) {
        if (position in 0 until tools.size && position != selectedPosition) {
            val old = selectedPosition
            selectedPosition = position
            notifyItemChanged(old)
            notifyItemChanged(selectedPosition)
        }
    }

    fun getSelectedTool(): EditorEnhance = tools[selectedPosition]
}