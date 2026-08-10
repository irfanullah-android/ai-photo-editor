package com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemCropAspectBinding
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.CropAspect

class CropAspectAdapter(
    private val aspects: List<CropAspect>,
    private val onAspectClick: (CropAspect) -> Unit
) : RecyclerView.Adapter<CropAspectAdapter.ViewHolder>() {

    private var selectedPosition = 0

    inner class ViewHolder(private val binding: ItemCropAspectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(aspect: CropAspect, isSelected: Boolean) {
            binding.tvAspectName.text = aspect.name
            binding.root.isSelected = isSelected

            binding.root.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onAspectClick(aspect)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCropAspectBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(aspects[position], position == selectedPosition)
    }

    override fun getItemCount() = aspects.size
}
