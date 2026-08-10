package com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemFontBinding

class FontAdapter(
    private val fonts: List<Pair<String, Int>>,   // (displayName, fontResId)
    private val onFontSelected: (Int) -> Unit      // returns fontResId
) : RecyclerView.Adapter<FontAdapter.FontViewHolder>() {

    private var selectedPosition = 0

    inner class FontViewHolder(val binding: ItemFontBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val binding = ItemFontBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FontViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        val (name, fontRes) = fonts[position]

        holder.binding.tvFontName.apply {
            text = name
            try {
                typeface = ResourcesCompat.getFont(context, fontRes)
            } catch (e: Exception) {
                typeface = Typeface.DEFAULT
            }
        }

        holder.binding.root.isSelected = position == selectedPosition

        holder.binding.root.setOnClickListener {
            val previous = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previous)
            notifyItemChanged(selectedPosition)
            onFontSelected(fontRes)
        }
    }

    override fun getItemCount(): Int = fonts.size
}