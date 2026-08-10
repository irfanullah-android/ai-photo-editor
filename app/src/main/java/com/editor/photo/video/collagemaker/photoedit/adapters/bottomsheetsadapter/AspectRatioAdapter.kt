package com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.AspectRatio

class AspectRatioAdapter(
    private val items: List<AspectRatio>,
    private val onItemClick: (AspectRatio) -> Unit
) : RecyclerView.Adapter<AspectRatioAdapter.ViewHolder>() {

    private var selectedPosition = 0

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: LinearLayout = itemView.findViewById(R.id.llAspectRatioItem)
        val icon: ImageView = itemView.findViewById(R.id.ivAspectIcon)
        val label: TextView = itemView.findViewById(R.id.tvAspectRatio)

        fun bind(item: AspectRatio, position: Int) {
            icon.setImageResource(item.iconRes)
            label.text = item.label
            container.isSelected = position == selectedPosition
            container.setBackgroundResource(
                if (position == selectedPosition)
                    R.drawable.bg_aspect_ratio_selected
                else
                    R.drawable.bg_aspect_ratio_unselected
            )

            container.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onItemClick(item)
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_aspect_ratio, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount() = items.size

    fun setSelectedPosition(position: Int) {
        val previousPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousPosition)
        notifyItemChanged(selectedPosition)
    }
}