package com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemStickerBinding

class StickerAdapter(
    private val stickers: List<Pair<Bitmap?, String?>>,
    private val onStickerClick: (Bitmap?, String?) -> Unit
) : RecyclerView.Adapter<StickerAdapter.StickerViewHolder>() {

    inner class StickerViewHolder(val binding: ItemStickerBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StickerViewHolder(
            ItemStickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
        val (bitmap, emoji) = stickers[position]

        if (bitmap != null) {
            holder.binding.ivSticker.visibility = View.VISIBLE
            holder.binding.tvEmoji.visibility = View.GONE
            holder.binding.ivSticker.setImageBitmap(bitmap)
        } else if (emoji != null) {
            holder.binding.ivSticker.visibility = View.GONE
            holder.binding.tvEmoji.visibility = View.VISIBLE
            holder.binding.tvEmoji.text = emoji
        }

        holder.binding.root.setOnClickListener {
            onStickerClick(bitmap, emoji)
        }
    }

    override fun getItemCount() = stickers.size
}

