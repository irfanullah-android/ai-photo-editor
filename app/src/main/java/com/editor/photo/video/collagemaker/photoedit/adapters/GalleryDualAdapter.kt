package com.editor.photo.video.collagemaker.photoedit.adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemGalleryImageBinding

class GalleryDualAdapter(
    private val photos: MutableList<Uri>,
    private val onPhotoClick: (Uri) -> Unit,
    private val onExpandClick: (Uri) -> Unit
) : RecyclerView.Adapter<GalleryDualAdapter.VH>() {

    private var person1Uri: Uri? = null
    private var person2Uri: Uri? = null

    inner class VH(val b: ItemGalleryImageBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemGalleryImageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )

        // ── Screen width se square size lo — reliable hai ────────────
        val screenWidth = parent.context.resources.displayMetrics.widthPixels
        val size = screenWidth / 4
        binding.root.layoutParams = ViewGroup.LayoutParams(size, size)

        return VH(binding)
    }

    override fun getItemCount() = photos.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val uri = photos[position]
        val b = holder.b

        val size = b.root.layoutParams.width

        // ── Glide load ───────────────────────────────────────────────
        Glide.with(b.root.context)
            .load(uri)
            .centerCrop()
            .override(if (size > 0) size else 300)
            .placeholder(android.R.color.darker_gray)
            .into(b.ivPhoto)

        // ── Badge + overlay ──────────────────────────────────────────
        when (uri) {
            person1Uri -> {
                b.vOverlay.alpha = 0.45f
                b.tvBadge.text = "1"
                b.tvBadge.visibility = View.VISIBLE
                b.tvBadge.setBackgroundResource(R.drawable.bg_circle_blue)
            }
            person2Uri -> {
                b.vOverlay.alpha = 0.45f
                b.tvBadge.text = "2"
                b.tvBadge.visibility = View.VISIBLE
                b.tvBadge.setBackgroundResource(R.drawable.bg_circle_pink)
            }
            else -> {
                b.vOverlay.alpha = 0f
                b.tvBadge.visibility = View.GONE
            }
        }

        b.root.setOnClickListener { onPhotoClick(uri) }
        b.ivExpand.setOnClickListener {
            onExpandClick(uri)
        }
    }

    fun updateList(list: List<Uri>) {
        photos.clear()
        photos.addAll(list)
        notifyDataSetChanged()
    }

    fun setSelections(p1: Uri?, p2: Uri?) {
        person1Uri = p1
        person2Uri = p2
        notifyDataSetChanged()
    }
}