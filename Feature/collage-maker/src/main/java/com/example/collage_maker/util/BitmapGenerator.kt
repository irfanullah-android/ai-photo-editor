package com.example.collage_maker.util

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.collage_maker.model.CollageState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BitmapGenerator {

    suspend fun generateBitmap(
        context: Context,
        collageState: CollageState,
        outputSize: Int = 1080
    ): Bitmap = withContext(Dispatchers.IO) {

        val bitmap = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Step 1: Pura background border color se bharo
        canvas.drawColor(collageState.borderColor)

        val borderPx   = collageState.borderWidth * (outputSize / 360f)
        val halfBorder = borderPx / 2f
        val cornerPx   = collageState.cornerRadius * (outputSize / 360f)

        val imageLoader = ImageLoader(context)

        collageState.template.slots.forEachIndexed { _, slot ->
            val slotLeft   = slot.left   * outputSize + halfBorder
            val slotTop    = slot.top    * outputSize + halfBorder
            val slotRight  = slot.right  * outputSize - halfBorder
            val slotBottom = slot.bottom * outputSize - halfBorder

            val slotWidth  = (slotRight  - slotLeft).coerceAtLeast(1f)
            val slotHeight = (slotBottom - slotTop).coerceAtLeast(1f)

            val rectF = RectF(slotLeft, slotTop, slotRight, slotBottom)

            // Rounded corner path banana
            val path = Path().apply {
                if (cornerPx > 0) {
                    addRoundRect(rectF, cornerPx, cornerPx, Path.Direction.CW)
                } else {
                    addRect(rectF, Path.Direction.CW)
                }
            }


            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = collageState.backgroundColor
            }
            canvas.drawPath(path, bgPaint)

            // Is slot ki image dhundo
            val slotImage = collageState.images.find { it.slotIndex == slot.index }
                ?: return@forEachIndexed


            val request = ImageRequest.Builder(context)
                .data(slotImage.uri)
                .allowHardware(false)
                .build()

            val result = imageLoader.execute(request)
            if (result !is SuccessResult) return@forEachIndexed

            val srcBitmap = (result.drawable as? BitmapDrawable)?.bitmap
                ?: return@forEachIndexed


            canvas.save()
            canvas.clipPath(path)


            val scaleX = slotWidth  / srcBitmap.width.toFloat()
            val scaleY = slotHeight / srcBitmap.height.toFloat()
            val coverScale = maxOf(scaleX, scaleY) * slotImage.scale

            val scaledW = srcBitmap.width  * coverScale
            val scaledH = srcBitmap.height * coverScale


            val drawLeft = slotLeft + (slotWidth  - scaledW) / 2f + slotImage.offsetX
            val drawTop  = slotTop  + (slotHeight - scaledH) / 2f + slotImage.offsetY

            val destRect = RectF(drawLeft, drawTop, drawLeft + scaledW, drawTop + scaledH)

            val imgPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(srcBitmap, null, destRect, imgPaint)

            canvas.restore()
        }

        return@withContext bitmap
    }
}