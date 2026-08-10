package com.editor.photo.video.collagemaker.photoedit.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemLanguageBinding
import com.editor.photo.video.collagemaker.photoedit.models.LanguageModel
import com.editor.photo.video.collagemaker.photoedit.utlis.DebounceListener.setDebounceClickListener

class LanguageAdapter(
    private val languages: List<LanguageModel>,
    private val onLanguageSelected: (LanguageModel) -> Unit,
    private var defaultSelectedCode: String
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private var selectedPosition = languages.indexOfFirst { it.code == defaultSelectedCode }

    inner class LanguageViewHolder(val binding: ItemLanguageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemLanguageBinding.inflate(inflater, parent, false)
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val language = languages[position]
        holder.binding.languageName.text = language.displayName
        holder.binding.flagImage.setImageResource(language.flagResId)

        val isSelected = position == selectedPosition
        holder.binding.languageItemContainer.setBackgroundResource(
            if (isSelected) R.drawable.bg_language_selected else R.drawable.bg_language_unselected
        )
        holder.binding.languageName.setTextColor(
            if (isSelected) holder.itemView.context.getColor(R.color.white) else holder.itemView.context.getColor(
                R.color.white
            )
        )


        holder.binding.checkIcon.setImageResource(
            if (isSelected) R.drawable.ic_circle_checked else R.drawable.ic_circle_unchecked
        )

        holder.binding.root.setDebounceClickListener {
            if (selectedPosition != position) {
                selectedPosition = position
                notifyDataSetChanged()
                onLanguageSelected(language)
            }
        }
    }

    override fun getItemCount() = languages.size
}
