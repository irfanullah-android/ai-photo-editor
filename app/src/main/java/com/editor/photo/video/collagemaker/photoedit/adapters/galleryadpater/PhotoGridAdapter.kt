package com.editor.photo.video.collagemaker.photoedit.adapters.galleryadpater

import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemPhotoGridBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotoGridAdapter(
    private var photoList: List<Uri>,
    private val onPhotoClick: (Uri, Int) -> Unit
) : RecyclerView.Adapter<PhotoGridAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(private val binding: ItemPhotoGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(uri: Uri, position: Int) {
            Glide.with(binding.root.context)
                .load(uri)
                .centerCrop()
                .override(300, 300)
                .thumbnail(0.1f)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .error(R.drawable.ic_photo)
                .placeholder(R.color.gray)
                .into(binding.imgPhoto)

            binding.root.setOnClickListener {
                onPhotoClick(uri, position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photoList[position], position)
    }

    override fun getItemCount(): Int = photoList.size

    // ── DiffUtil background thread pe ────────────────────────────────────
    fun updateList(newList: List<Uri>) {
        val newCopy = newList.toList()

        CoroutineScope(Dispatchers.Default).launch {
            val diffCallback = PhotoDiffCallback(photoList, newCopy)
            val diffResult = DiffUtil.calculateDiff(diffCallback, false)

            withContext(Dispatchers.Main) {
                photoList = newCopy
                diffResult.dispatchUpdatesTo(this@PhotoGridAdapter)
                Log.d("PhotoGridAdapter", "Updated: ${newCopy.size} items")
            }
        }
    }

    private class PhotoDiffCallback(
        private val oldList: List<Uri>,
        private val newList: List<Uri>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
            return oldList[oldPos] == newList[newPos]
        }

        override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
            return oldList[oldPos] == newList[newPos]
        }
    }
}