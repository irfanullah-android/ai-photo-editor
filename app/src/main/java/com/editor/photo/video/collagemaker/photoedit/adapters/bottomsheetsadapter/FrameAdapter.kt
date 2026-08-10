package com.editor.photo.video.collagemaker.photoedit.adapters.bottomsheetsadapter

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.editor.photo.video.collagemaker.photoedit.databinding.ItemFrameBinding
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.FrameModel

class FrameAdapter(
    private val frames: List<FrameModel>,
    private val originalBitmap: Bitmap?,
    private val onFrameClick: (FrameModel) -> Unit
) : RecyclerView.Adapter<FrameAdapter.FrameViewHolder>() {

    private var selectedPosition = 0

    inner class FrameViewHolder(val binding: ItemFrameBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(frame: FrameModel, position: Int) {
            binding.tvFrameName.text = frame.name

            // Generate preview
            originalBitmap?.let { bitmap ->
                val preview = if (frame.name == "None") {
                    // Show small original image
                    Bitmap.createScaledBitmap(bitmap, 200, 200, true)
                } else {
                    createFramePreview(bitmap, frame)
                }
                binding.ivFramePreview.setImageBitmap(preview)
            }

            // Selection state
            val isSelected = position == selectedPosition
            binding.root.alpha = if (isSelected) 1.0f else 0.6f
            binding.root.scaleX = if (isSelected) 1.05f else 1.0f
            binding.root.scaleY = if (isSelected) 1.05f else 1.0f

            binding.root.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onFrameClick(frame)
            }
        }

        private fun createFramePreview(bitmap: Bitmap, frame: FrameModel): Bitmap {
            return try {
                val frameDrawable = ContextCompat.getDrawable(binding.root.context, frame.frameRes)
                    ?: return Bitmap.createScaledBitmap(bitmap, 200, 200, true)

                val previewSize = 200
                val result = Bitmap.createBitmap(previewSize, previewSize, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(result)

                // Padding based on frame type
                val padding = when (frame.name) {
                    "Classic" -> 15
                    "Vintage" -> 18
                    "Gold" -> 17
                    "Silver" -> 15
                    "Polaroid" -> 12
                    else -> 12
                }

                val topPadding = padding
                val bottomPadding = if (frame.name == "Polaroid") 30 else padding
                val leftPadding = padding
                val rightPadding = padding

                val imageWidth = previewSize - leftPadding - rightPadding
                val imageHeight = previewSize - topPadding - bottomPadding

                // Draw scaled image
                val scaledImage = Bitmap.createScaledBitmap(bitmap, imageWidth, imageHeight, true)
                canvas.drawBitmap(scaledImage, leftPadding.toFloat(), topPadding.toFloat(), null)

                // Draw frame
                val frameBitmap = drawableToBitmap(frameDrawable, previewSize, previewSize)
                canvas.drawBitmap(frameBitmap, 0f, 0f, null)

                // Cleanup
                scaledImage.recycle()
                frameBitmap.recycle()

                result
            } catch (e: Exception) {
                e.printStackTrace()
                Bitmap.createScaledBitmap(bitmap, 200, 200, true)
            }
        }

        private fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
            if (drawable is BitmapDrawable) {
                return Bitmap.createScaledBitmap(drawable.bitmap, width, height, true)
            }

            // For XML drawables
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            return bitmap
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FrameViewHolder {
        val binding = ItemFrameBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FrameViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FrameViewHolder, position: Int) {
        holder.bind(frames[position], position)
    }

    override fun getItemCount() = frames.size

    fun updateSelection(position: Int) {
        val previousPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousPosition)
        notifyItemChanged(selectedPosition)
    }

    fun cleanup() {
        // Cleanup if needed
    }
}