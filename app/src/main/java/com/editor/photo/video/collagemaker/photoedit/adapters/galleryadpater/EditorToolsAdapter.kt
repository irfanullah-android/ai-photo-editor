package com.editor.photo.video.collagemaker.photoedit.adapters.galleryadpater

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemEditorToolBinding
import com.editor.photo.video.collagemaker.photoedit.models.gallery.EditorItemModel
import com.editor.photo.video.collagemaker.photoedit.models.gallery.EditorTool

class EditorToolsAdapter(
    private val tools: List<EditorItemModel>,
    private val onToolClick: (EditorItemModel) -> Unit
) : RecyclerView.Adapter<EditorToolsAdapter.ToolViewHolder>() {

    private var selectedPosition = -1

    inner class ToolViewHolder(private val binding: ItemEditorToolBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(tool: EditorItemModel, position: Int) {
            binding.apply {
                tvToolName.text = tool.name
                ivToolIcon.setImageResource(tool.iconRes)

                // Selected state handling (optional)
                val isSelected = position == selectedPosition
                root.alpha = if (isSelected) 1.0f else 0.7f

                root.setOnClickListener {
                    val previousPosition = selectedPosition
                    selectedPosition = adapterPosition

                    // Notify previous and current selected items
                    notifyItemChanged(previousPosition)
                    notifyItemChanged(selectedPosition)

                    onToolClick(tool)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        val binding = ItemEditorToolBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ToolViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ToolViewHolder, position: Int) {
        holder.bind(tools[position], position)
    }

    override fun getItemCount(): Int = tools.size

    fun setSelectedTool(tool: EditorTool) {
        val newPosition = tools.indexOfFirst { it.name.equals(tool.name, ignoreCase = true) }

        if (newPosition == -1) return

        val previousPosition = selectedPosition
        selectedPosition = newPosition

        if (previousPosition != -1) {
            notifyItemChanged(previousPosition)
        }
        notifyItemChanged(selectedPosition)
    }
}