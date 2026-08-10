package com.editor.photo.video.collagemaker.photoedit.adapters.galleryadpater

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.models.gallery.FolderModel

class FolderAdapter(
    private var folders: List<FolderModel>,
    private val onFolderClick: (FolderModel) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    inner class FolderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivFolderThumbnail: ImageView = view.findViewById(R.id.ivFolderThumbnail)
        val tvFolderName: TextView = view.findViewById(R.id.tvFolderName)
        val tvFolderPath: TextView = view.findViewById(R.id.tvFolderPath)

        fun bind(folder: FolderModel) {

            // "Camera (118)" — screenshot jesa
            tvFolderName.text = "${folder.name} (${folder.photoCount})"

            // Path show karo, agar empty ho toh hide karo
            if (folder.path.isNotEmpty()) {
                tvFolderPath.visibility = View.VISIBLE
                tvFolderPath.text = folder.path
            } else {
                tvFolderPath.visibility = View.GONE
            }

            folder.thumbnailUri?.let { uri ->
                Glide.with(itemView.context)
                    .load(uri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_photo)
                    .into(ivFolderThumbnail)
            } ?: run {
                ivFolderThumbnail.setImageResource(R.drawable.ic_photo)
            }

            itemView.setOnClickListener {
                onFolderClick(folder)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_folder, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(folders[position])
    }

    override fun getItemCount() = folders.size

    fun updateList(newFolders: List<FolderModel>) {
        folders = newFolders
        notifyDataSetChanged()
    }
}