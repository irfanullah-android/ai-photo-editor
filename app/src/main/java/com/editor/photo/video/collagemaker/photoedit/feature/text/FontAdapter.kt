package com.editor.photo.video.collagemaker.photoedit.feature.text

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemFontBinding

class FontAdapter(
    private val fonts: List<TextFontOption> = TextFonts.OPTIONS,
    initialKey: String?,
    private val onFontSelected: (TextFontOption) -> Unit
) : RecyclerView.Adapter<FontAdapter.FontViewHolder>() {

    private var selectedKey: String? = initialKey

    inner class FontViewHolder(private val binding: ItemFontBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(font: TextFontOption) {
            binding.tvFontSample.text = "Aa"
            binding.tvFontSample.typeface = font.typefaceFamily
            binding.tvFontName.text = font.displayName
            binding.fontItemRoot.isSelected = font.key == selectedKey
            binding.root.setOnClickListener {
                val oldKey = selectedKey
                selectedKey = font.key
                notifyItemChanged(fonts.indexOfFirst { it.key == oldKey })
                notifyItemChanged(fonts.indexOfFirst { it.key == font.key })
                onFontSelected(font)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val binding = ItemFontBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FontViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        holder.bind(fonts[position])
    }

    override fun getItemCount() = fonts.size
}
