package com.editor.photo.video.collagemaker.photoedit.feature.text

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemTextToolBinding

/**
 * Single horizontal RecyclerView for ALL text tools (Style, Font, Color, Stroke, Align, Size).
 *
 * Per the design requirement, this is the ONLY RecyclerView for tool tabs — do not create a
 * separate RecyclerView per tool. Selecting a tool here just swaps the panel shown in the
 * tool-content FrameLayout ([TextEditorBottomSheet.toolContentContainer]).
 */
class TextToolAdapter(
    private val tools: List<TextToolType>,
    private val onToolSelected: (TextToolType) -> Unit
) : RecyclerView.Adapter<TextToolAdapter.ToolViewHolder>() {

    private var selected: TextToolType = tools.first()

    inner class ToolViewHolder(private val binding: ItemTextToolBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(tool: TextToolType) {
            binding.ivToolIcon.setImageResource(tool.iconRes)
            binding.tvToolLabel.text = tool.label
            val isSelected = tool == selected
            binding.ivToolIcon.alpha = if (isSelected) 1f else 0.6f
            binding.tvToolLabel.alpha = if (isSelected) 1f else 0.6f
            binding.root.setOnClickListener {
                val old = selected
                selected = tool
                notifyItemChanged(tools.indexOf(old))
                notifyItemChanged(tools.indexOf(tool))
                onToolSelected(tool)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        val binding = ItemTextToolBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ToolViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ToolViewHolder, position: Int) {
        holder.bind(tools[position])
    }

    override fun getItemCount() = tools.size

    fun setSelected(tool: TextToolType) {
        val old = selected
        selected = tool
        notifyItemChanged(tools.indexOf(old))
        notifyItemChanged(tools.indexOf(tool))
    }
}
