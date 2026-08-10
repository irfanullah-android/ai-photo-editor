package com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.R
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemColorBinding

class ColorAdapter(
    private val colors: List<Int>,
    initialPosition: Int = 0,
    private val onColorSelected: (Int) -> Unit
) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

    private var selectedPosition = initialPosition

    inner class ColorViewHolder(private val binding: ItemColorBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(color: Int, position: Int) {
            // Transparent color ke liye checkerboard pattern
            if (color == Color.TRANSPARENT) {
                binding.viewColor.setBackgroundResource(R.drawable.bg_exit_dialog)
            } else {
                val drawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(color)

                    // Add border for visibility
                    if (color == Color.WHITE || isLightColor(color)) {
                        setStroke(3, Color.LTGRAY)
                    }
                }
                binding.viewColor.background = drawable
            }

            // Show selection indicator
            if (position == selectedPosition) {
                binding.viewSelection.visibility = View.VISIBLE
                val selectionDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setStroke(6, ContextCompat.getColor(binding.root.context, R.color.dark_gray))
                }
                binding.viewSelection.background = selectionDrawable
            } else {
                binding.viewSelection.visibility = View.GONE
            }

            // Click listener
            binding.root.setOnClickListener {
                val oldPosition = selectedPosition
                selectedPosition = position
                notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)
                onColorSelected(color)
            }
        }

        private fun isLightColor(color: Int): Boolean {
            val darkness = 1 - (0.299 * Color.red(color) +
                    0.587 * Color.green(color) +
                    0.114 * Color.blue(color)) / 255
            return darkness < 0.5
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val binding = ItemColorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ColorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(colors[position], position)
    }

    override fun getItemCount() = colors.size
}