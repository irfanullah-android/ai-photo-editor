package com.editor.photo.video.collagemaker.photoedit.editor.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import com.editor.photo.video.collagemaker.photoedit.editor.history.HistoryManager
import com.editor.photo.video.collagemaker.photoedit.editor.layer.Layer
import com.editor.photo.video.collagemaker.photoedit.editor.layer.LayerManager
import com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine.ColorMatrixEngine
import com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine.EffectsEngine
import com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine.FilterSpec
import com.editor.photo.video.collagemaker.photoedit.fragments.imageRenderEngine.ImageProcessor
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.AdjustmentType
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.DoodlePath
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.DoodleShapeType
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.FrameLayer
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.StickerLayer
import com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.TextLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

class EditorEngine(
    private val context: Context,
    private val historyManager: HistoryManager = HistoryManager(),
    private val layerManager: LayerManager = LayerManager()
) {
    private val imageProcessor = ImageProcessor(context)
    private var baseUri: Uri? = null
    private var originalBitmap: Bitmap? = null

    private val renderGeneration = AtomicLong(0L)

    suspend fun initialize(uri: Uri) = withContext(Dispatchers.IO) {
        val myGeneration = renderGeneration.incrementAndGet()

        val loadedBitmap = try {
            imageProcessor.loadBitmapForDisplay(uri)
        } catch (e: Exception) {
            null
        }

        // A newer initialize()/render call started while this decode was in flight —
        // this is a stale (previously selected) photo. Discard it instead of
        // overwriting the newer session.
        if (myGeneration != renderGeneration.get()) {
            loadedBitmap?.recycle()
            return@withContext
        }

        baseUri = uri
        originalBitmap?.recycle()
        originalBitmap = loadedBitmap
        historyManager.clear()
        layerManager.clear()
    }

    fun getBaseUri(): Uri? = baseUri

    fun getOriginalBitmap(): Bitmap? = originalBitmap


    fun addOperation(operation: EditOperation) {
        historyManager.addOperation(operation)
        syncLayersFromOperations()
    }

    fun undo(): Boolean {
        val success = historyManager.undo()
        if (success) {
            syncLayersFromOperations()
        }
        return success
    }

    fun redo(): Boolean {
        val success = historyManager.redo()
        if (success) {
            syncLayersFromOperations()
        }
        return success
    }

    fun canUndo(): Boolean = historyManager.canUndo()

    fun canRedo(): Boolean = historyManager.canRedo()

    fun getActiveOperations(): List<EditOperation> = historyManager.getActiveOperations()

    private fun syncLayersFromOperations() {
        layerManager.clear()
        var zIndex = 0
        for (op in historyManager.getActiveOperations()) {
            when (op) {
                is EditOperation.AddSticker -> {
                    layerManager.addOrUpdateLayer(
                        Layer.Sticker(
                            id = op.sticker.id,
                            zIndex = zIndex++,
                            isVisible = true,
                            emojiContent = op.sticker.emojiContent,
                            resourceId = op.sticker.resourceId,
                            x = op.sticker.x,
                            y = op.sticker.y,
                            scale = op.sticker.scale,
                            rotation = op.sticker.rotation,
                            alpha = op.sticker.alpha
                        )
                    )
                }

                is EditOperation.UpdateSticker -> {
                    layerManager.addOrUpdateLayer(
                        Layer.Sticker(
                            id = op.sticker.id,
                            zIndex = zIndex++,
                            isVisible = true,
                            emojiContent = op.sticker.emojiContent,
                            resourceId = op.sticker.resourceId,
                            x = op.sticker.x,
                            y = op.sticker.y,
                            scale = op.sticker.scale,
                            rotation = op.sticker.rotation,
                            alpha = op.sticker.alpha
                        )
                    )
                }

                is EditOperation.RemoveSticker -> {
                    layerManager.removeLayer(op.stickerId)
                }

                is EditOperation.AddText -> {
                    layerManager.addOrUpdateLayer(
                        Layer.Text(
                            id = op.text.id,
                            zIndex = zIndex++,
                            isVisible = true,
                            text = op.text.text,
                            x = op.text.x,
                            y = op.text.y,
                            size = op.text.size,
                            color = op.text.color,
                            alpha = op.text.alpha,
                            rotation = op.text.rotation,
                            fontFamily = op.text.fontFamily,
                            isBold = op.text.isBold,
                            isItalic = op.text.isItalic,
                            isUnderline = op.text.isUnderline,
                            alignment = op.text.alignment,
                            strokeWidth = op.text.strokeWidth,
                            strokeColor = op.text.strokeColor,
                            letterSpacing = op.text.letterSpacing
                        )
                    )
                }

                is EditOperation.UpdateText -> {
                    layerManager.addOrUpdateLayer(
                        Layer.Text(
                            id = op.text.id,
                            zIndex = zIndex++,
                            isVisible = true,
                            text = op.text.text,
                            x = op.text.x,
                            y = op.text.y,
                            size = op.text.size,
                            color = op.text.color,
                            alpha = op.text.alpha,
                            rotation = op.text.rotation,
                            fontFamily = op.text.fontFamily,
                            isBold = op.text.isBold,
                            isItalic = op.text.isItalic,
                            isUnderline = op.text.isUnderline,
                            alignment = op.text.alignment,
                            strokeWidth = op.text.strokeWidth,
                            strokeColor = op.text.strokeColor,
                            letterSpacing = op.text.letterSpacing
                        )
                    )
                }

                is EditOperation.RemoveText -> {
                    layerManager.removeLayer(op.textId)
                }

                is EditOperation.ApplyFrame -> {
                    if (op.frame != null) {
                        layerManager.addOrUpdateLayer(
                            Layer.Frame(
                                id = op.frame.id,
                                zIndex = zIndex++,
                                isVisible = true,
                                resourceId = op.frame.resourceId,
                                padding = op.frame.padding
                            )
                        )
                    }
                }

                is EditOperation.Doodle -> {
                    layerManager.addOrUpdateLayer(
                        Layer.Doodle(
                            id = op.path.id,
                            zIndex = zIndex++,
                            isVisible = true,
                            doodlePath = op.path
                        )
                    )
                }

                else -> { /* Non-layer operations (Adjust, Filter, Enhance, Crop, Rotate, etc.) */
                }
            }
        }
    }


    suspend fun renderPreview(
        widthLimit: Int = 1080,
        excludeTextId: String? = null,
        excludeStickerId: String? = null
    ): Bitmap? =
        withContext(Dispatchers.Default) {
            val myGeneration = renderGeneration.incrementAndGet()

            val original = originalBitmap ?: return@withContext null
            if (original.isRecycled) return@withContext null
            if (myGeneration != renderGeneration.get()) return@withContext null

            var rendered = original.copy(Bitmap.Config.ARGB_8888, true)
            val filteredOps = historyManager.getActiveOperations().filter { op ->
                val passesText = if (excludeTextId != null) {
                    when (op) {
                        is EditOperation.AddText -> op.text.id != excludeTextId
                        is EditOperation.UpdateText -> op.text.id != excludeTextId
                        else -> true
                    }
                } else true

                val passesSticker = if (excludeStickerId != null) {
                    when (op) {
                        is EditOperation.AddSticker -> op.sticker.id != excludeStickerId
                        is EditOperation.UpdateSticker -> op.sticker.id != excludeStickerId
                        else -> true
                    }
                } else true

                passesText && passesSticker
            }
            val ops = consolidateOperations(filteredOps)

            for (op in ops) {
                if (myGeneration != renderGeneration.get()) {
                    if (!rendered.isRecycled) rendered.recycle()
                    return@withContext null
                }
                rendered = applyOperationOnBitmap(rendered, op, isPreview = true) ?: rendered
            }

            if (myGeneration != renderGeneration.get()) {
                if (!rendered.isRecycled) rendered.recycle()
                return@withContext null
            }

            rendered
        }


    suspend fun renderPreviewWithoutFrame(
        widthLimit: Int = 1080,
        excludeTextId: String? = null
    ): Bitmap? = withContext(Dispatchers.Default) {
        val myGeneration = renderGeneration.incrementAndGet()

        val original = originalBitmap ?: return@withContext null
        if (original.isRecycled) return@withContext null
        if (myGeneration != renderGeneration.get()) return@withContext null

        var rendered = original.copy(Bitmap.Config.ARGB_8888, true)
        val filteredOps = historyManager.getActiveOperations().filter { op ->
            if (op is EditOperation.ApplyFrame) return@filter false
            if (excludeTextId != null) {
                when (op) {
                    is EditOperation.AddText -> op.text.id != excludeTextId
                    is EditOperation.UpdateText -> op.text.id != excludeTextId
                    else -> true
                }
            } else {
                true
            }
        }
        val ops = consolidateOperations(filteredOps)

        for (op in ops) {
            if (myGeneration != renderGeneration.get()) {
                if (!rendered.isRecycled) rendered.recycle()
                return@withContext null
            }
            rendered = applyOperationOnBitmap(rendered, op, isPreview = true) ?: rendered
        }

        if (myGeneration != renderGeneration.get()) {
            if (!rendered.isRecycled) rendered.recycle()
            return@withContext null
        }

        rendered
    }

    suspend fun renderHighRes(): Bitmap? = withContext(Dispatchers.Default) {
        val uri = baseUri ?: return@withContext null
        val ops = consolidateOperations(historyManager.getActiveOperations())

        var rendered = try {
            imageProcessor.processImage(
                uri,
                com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.EditorState()
            )
        } catch (e: Exception) {
            return@withContext null
        }

        for (op in ops) {
            rendered = applyOperationOnBitmap(rendered, op, isPreview = false) ?: rendered
        }

        rendered
    }

    private fun consolidateOperations(ops: List<EditOperation>): List<EditOperation> {
        val result = mutableListOf<EditOperation>()
        val latestTextMap = mutableMapOf<String, EditOperation>()
        val latestStickerMap = mutableMapOf<String, EditOperation>()
        var latestFrameOp: EditOperation.ApplyFrame? = null

        for (op in ops) {
            when (op) {
                is EditOperation.AddText -> latestTextMap[op.text.id] = op
                is EditOperation.UpdateText -> latestTextMap[op.text.id] = op
                is EditOperation.RemoveText -> latestTextMap.remove(op.textId)
                is EditOperation.AddSticker -> latestStickerMap[op.sticker.id] = op
                is EditOperation.UpdateSticker -> latestStickerMap[op.sticker.id] = op
                is EditOperation.RemoveSticker -> latestStickerMap.remove(op.stickerId)
                is EditOperation.ApplyFrame -> latestFrameOp = op
                else -> result.add(op)
            }
        }

        val addedTextIds = mutableSetOf<String>()
        val addedStickerIds = mutableSetOf<String>()

        for (op in ops) {
            when (op) {
                is EditOperation.AddText -> {
                    val id = op.text.id
                    if (addedTextIds.add(id)) {
                        latestTextMap[id]?.let { result.add(it) }
                    }
                }

                is EditOperation.UpdateText -> {
                    val id = op.text.id
                    if (addedTextIds.add(id)) {
                        latestTextMap[id]?.let { result.add(it) }
                    }
                }

                is EditOperation.AddSticker -> {
                    val id = op.sticker.id
                    if (addedStickerIds.add(id)) {
                        latestStickerMap[id]?.let { result.add(it) }
                    }
                }

                is EditOperation.UpdateSticker -> {
                    val id = op.sticker.id
                    if (addedStickerIds.add(id)) {
                        latestStickerMap[id]?.let { result.add(it) }
                    }
                }

                else -> {}
            }
        }

        latestFrameOp?.let { result.add(it) }

        return result
    }

    private fun applyOperationOnBitmap(
        bitmap: Bitmap,
        op: EditOperation,
        isPreview: Boolean
    ): Bitmap? {
        if (bitmap.isRecycled) return null
        return when (op) {
            is EditOperation.Adjust -> {
                val spec = buildCombinedSpec(op.adjustments)
                var result = ColorMatrixEngine.render(bitmap, spec)
                val sharpenAmount = op.adjustments[AdjustmentType.SHARPEN] ?: 0
                if (sharpenAmount != 0) {
                    val sharpened = applySharpenConvolution(result, sharpenAmount)
                    if (result != bitmap) result.recycle()
                    result = sharpened
                }
                if (result != bitmap) bitmap.recycle()
                result
            }

            is EditOperation.Filter -> {
                val spec = op.filter.buildFilterSpec(op.intensity)
                val result = ColorMatrixEngine.render(bitmap, spec)
                if (result != bitmap) bitmap.recycle()
                result
            }

            is EditOperation.Effect -> {
                val fullEffect = EffectsEngine.apply(op.effectType, bitmap)
                val blended = blendBitmaps(bitmap, fullEffect, op.intensity)
                if (fullEffect != bitmap && fullEffect != blended) {
                    fullEffect.recycle()
                }
                if (blended != bitmap) bitmap.recycle()
                blended
            }


            is EditOperation.Crop -> {
                val result = try {
                    context.contentResolver.openInputStream(op.croppedUri)?.use { stream ->
                        android.graphics.BitmapFactory.decodeStream(stream)
                    } ?: bitmap
                } catch (e: Exception) {
                    bitmap
                }
                if (result != bitmap) bitmap.recycle()
                result
            }

            is EditOperation.Rotate -> {
                var result = bitmap
                if (op.rotation != 0f) {
                    result = try {
                        val matrix = Matrix().apply { postRotate(op.rotation) }
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    } catch (e: Exception) {
                        bitmap
                    }
                }
                if (op.flipHorizontal || op.flipVertical) {
                    result = try {
                        val matrix = Matrix().apply {
                            postScale(
                                if (op.flipHorizontal) -1f else 1f,
                                if (op.flipVertical) -1f else 1f,
                                result.width / 2f,
                                result.height / 2f
                            )
                        }
                        val flipped = Bitmap.createBitmap(
                            result,
                            0,
                            0,
                            result.width,
                            result.height,
                            matrix,
                            true
                        )
                        if (result != bitmap) result.recycle()
                        flipped
                    } catch (e: Exception) {
                        result
                    }
                }
                if (result != bitmap) bitmap.recycle()
                result
            }

            is EditOperation.Canvas -> {
                val ratio = op.aspectRatio ?: return bitmap
                val srcW = bitmap.width.toFloat()
                val srcH = bitmap.height.toFloat()
                val srcRatio = srcW / srcH

                var cropW = bitmap.width
                var cropH = bitmap.height
                var cropX = 0
                var cropY = 0

                if (srcRatio > ratio) {
                    cropW = (srcH * ratio).toInt()
                    cropX = ((srcW - cropW) / 2).toInt()
                } else {
                    cropH = (srcW / ratio).toInt()
                    cropY = ((srcH - cropH) / 2).toInt()
                }

                val result = try {
                    Bitmap.createBitmap(
                        bitmap,
                        cropX,
                        cropY,
                        cropW.coerceAtLeast(1),
                        cropH.coerceAtLeast(1)
                    )
                } catch (e: Exception) {
                    bitmap
                }
                if (result != bitmap) bitmap.recycle()
                result
            }

            is EditOperation.AddSticker -> drawStickerOnBitmap(bitmap, op.sticker)
            is EditOperation.UpdateSticker -> drawStickerOnBitmap(bitmap, op.sticker)
            is EditOperation.RemoveSticker -> bitmap
            is EditOperation.AddText -> drawTextOnBitmap(bitmap, op.text)
            is EditOperation.UpdateText -> drawTextOnBitmap(bitmap, op.text)
            is EditOperation.RemoveText -> bitmap
            is EditOperation.ApplyFrame -> {
                val frame = op.frame
                if (frame != null) {
                    drawFrameOnBitmap(bitmap, frame)
                } else {
                    bitmap
                }
            }

            is EditOperation.Doodle -> drawDoodleOnBitmap(bitmap, op.path)
            is EditOperation.BackgroundRemoved -> {
                val result = op.bitmap.copy(Bitmap.Config.ARGB_8888, true)
                if (result != bitmap) bitmap.recycle()
                result
            }
        }
    }

    private fun buildCombinedSpec(values: Map<AdjustmentType, Int>): FilterSpec {
        val e = ColorMatrixEngine
        val matrices = mutableListOf<ColorMatrix>()

        fun eased(type: AdjustmentType): Float {
            val raw = (values[type] ?: 0) / 100f
            val sign = if (raw < 0f) -1f else 1f
            val a = kotlin.math.abs(raw).coerceIn(0f, 1f)
            val eased = sign * (1f - (1f - a) * (1f - a))
            return eased
        }

        if (values[AdjustmentType.BRIGHTNESS] != 0) {
            val t = eased(AdjustmentType.BRIGHTNESS)
            matrices += e.brightness(t * 55f)
        }

        if (values[AdjustmentType.CONTRAST] != 0) {
            val t = eased(AdjustmentType.CONTRAST)
            val factor = if (t >= 0f) 1f + t * 0.6f else 1f + t * 0.4f
            matrices += e.contrast(factor)
        }

        if (values[AdjustmentType.HIGHLIGHT] != 0) {
            val t = eased(AdjustmentType.HIGHLIGHT)
            matrices += e.contrast(1f + t * 0.3f)
        }

        if (values[AdjustmentType.SHADOW] != 0) {
            val t = eased(AdjustmentType.SHADOW)
            matrices += e.brightness(t * 45f)
        }

        if (values[AdjustmentType.FADE] != 0) {
            val t = eased(AdjustmentType.FADE)
            if (t >= 0f) {
                matrices += e.contrast(1f - t * 0.35f)
                matrices += e.brightness(t * 30f)
            } else {
                matrices += e.contrast(1f - t * 0.2f)
            }
        }

        val warmth = values[AdjustmentType.WARMTH] ?: 0
        val tint = values[AdjustmentType.TINT] ?: 0
        if (warmth != 0 || tint != 0) {
            val tw = eased(AdjustmentType.WARMTH)
            val tt = eased(AdjustmentType.TINT)
            val temperature = 5000f + tw * 1800f
            val tintAmount = tt * 0.22f
            matrices += e.whiteBalance(temperature, tintAmount)
        }

        if (values[AdjustmentType.SATURATION] != 0) {
            val t = eased(AdjustmentType.SATURATION)
            val factor = if (t >= 0f) 1f + t * 0.7f else 1f + t
            matrices += e.saturation(factor.coerceAtLeast(0f))
        }

        if (values[AdjustmentType.HUE] != 0) {
            val t = eased(AdjustmentType.HUE)
            matrices += e.hueRotate(t * 180f)
        }

        val matrix = if (matrices.isEmpty()) e.identity() else e.combine(*matrices.toTypedArray())
        return FilterSpec(matrix)
    }

    private fun applySharpenConvolution(source: Bitmap, amount: Int): Bitmap {
        val strength = (amount / 100f).coerceIn(-1f, 1f)
        val w = source.width
        val h = source.height
        if (w < 3 || h < 3) return source.copy(source.config ?: Bitmap.Config.ARGB_8888, false)

        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val out = IntArray(w * h)

        val k = kotlin.math.abs(strength) * 1.5f
        val center = if (strength >= 0f) 1f + 4f * k else 1f - 3f * k
        val edge = if (strength >= 0f) -k else k * 0.75f

        for (y in 1 until h - 1) {
            val yOffset = y * w
            val yUpOffset = (y - 1) * w
            val yDownOffset = (y + 1) * w
            for (x in 1 until w - 1) {
                val idx = yOffset + x
                val pC = pixels[idx]
                val pU = pixels[yUpOffset + x]
                val pD = pixels[yDownOffset + x]
                val pL = pixels[idx - 1]
                val pR = pixels[idx + 1]

                val a = pC ushr 24 and 0xFF

                val rC =
                    ((pC ushr 16 and 0xFF) * center + ((pU ushr 16 and 0xFF) + (pD ushr 16 and 0xFF) + (pL ushr 16 and 0xFF) + (pR ushr 16 and 0xFF)) * edge).roundToInt()
                        .coerceIn(0, 255)
                val gC =
                    ((pC ushr 8 and 0xFF) * center + ((pU ushr 8 and 0xFF) + (pD ushr 8 and 0xFF) + (pL ushr 8 and 0xFF) + (pR ushr 8 and 0xFF)) * edge).roundToInt()
                        .coerceIn(0, 255)
                val bC =
                    ((pC and 0xFF) * center + ((pU and 0xFF) + (pD and 0xFF) + (pL and 0xFF) + (pR and 0xFF)) * edge).roundToInt()
                        .coerceIn(0, 255)

                out[idx] = (a shl 24) or (rC shl 16) or (gC shl 8) or bC
            }
        }

        applyEdgeSharpen(pixels, out, w, h, 0, center, edge)
        applyEdgeSharpen(pixels, out, w, h, h - 1, center, edge)
        for (y in 1 until h - 1) {
            applyEdgePixel(pixels, out, w, h, 0, y, center, edge)
            applyEdgePixel(pixels, out, w, h, w - 1, y, center, edge)
        }

        val result = Bitmap.createBitmap(w, h, source.config ?: Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, w, 0, 0, w, h)
        return result
    }

    private fun applyEdgeSharpen(
        pixels: IntArray,
        out: IntArray,
        w: Int,
        h: Int,
        y: Int,
        center: Float,
        edge: Float
    ) {
        val yUp = if (y == 0) 0 else y - 1
        val yDown = if (y == h - 1) h - 1 else y + 1
        val yOffset = y * w
        val yUpOffset = yUp * w
        val yDownOffset = yDown * w

        for (x in 0 until w) {
            val xLeft = if (x == 0) 0 else x - 1
            val xRight = if (x == w - 1) w - 1 else x + 1

            val pC = pixels[yOffset + x]
            val pU = pixels[yUpOffset + x]
            val pD = pixels[yDownOffset + x]
            val pL = pixels[yOffset + xLeft]
            val pR = pixels[yOffset + xRight]

            val a = pC ushr 24 and 0xFF
            val rC =
                ((pC ushr 16 and 0xFF) * center + ((pU ushr 16 and 0xFF) + (pD ushr 16 and 0xFF) + (pL ushr 16 and 0xFF) + (pR ushr 16 and 0xFF)) * edge).roundToInt()
                    .coerceIn(0, 255)
            val gC =
                ((pC ushr 8 and 0xFF) * center + ((pU ushr 8 and 0xFF) + (pD ushr 8 and 0xFF) + (pL ushr 8 and 0xFF) + (pR ushr 8 and 0xFF)) * edge).roundToInt()
                    .coerceIn(0, 255)
            val bC =
                ((pC and 0xFF) * center + ((pU and 0xFF) + (pD and 0xFF) + (pL and 0xFF) + (pR and 0xFF)) * edge).roundToInt()
                    .coerceIn(0, 255)

            out[yOffset + x] = (a shl 24) or (rC shl 16) or (gC shl 8) or bC
        }
    }

    private fun applyEdgePixel(
        pixels: IntArray,
        out: IntArray,
        w: Int,
        h: Int,
        x: Int,
        y: Int,
        center: Float,
        edge: Float
    ) {
        val yUp = if (y == 0) 0 else y - 1
        val yDown = if (y == h - 1) h - 1 else y + 1
        val xLeft = if (x == 0) 0 else x - 1
        val xRight = if (x == w - 1) w - 1 else x + 1

        val pC = pixels[y * w + x]
        val pU = pixels[yUp * w + x]
        val pD = pixels[yDown * w + x]
        val pL = pixels[y * w + xLeft]
        val pR = pixels[y * w + xRight]

        val a = pC ushr 24 and 0xFF
        val rC =
            ((pC ushr 16 and 0xFF) * center + ((pU ushr 16 and 0xFF) + (pD ushr 16 and 0xFF) + (pL ushr 16 and 0xFF) + (pR ushr 16 and 0xFF)) * edge).roundToInt()
                .coerceIn(0, 255)
        val gC =
            ((pC ushr 8 and 0xFF) * center + ((pU ushr 8 and 0xFF) + (pD ushr 8 and 0xFF) + (pL ushr 8 and 0xFF) + (pR ushr 8 and 0xFF)) * edge).roundToInt()
                .coerceIn(0, 255)
        val bC =
            ((pC and 0xFF) * center + ((pU and 0xFF) + (pD and 0xFF) + (pL and 0xFF) + (pR and 0xFF)) * edge).roundToInt()
                .coerceIn(0, 255)

        out[y * w + x] = (a shl 24) or (rC shl 16) or (gC shl 8) or bC
    }

    private fun blendBitmaps(original: Bitmap, filtered: Bitmap, intensity: Float): Bitmap {
        val result = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        paint.alpha = 255
        canvas.drawBitmap(original, 0f, 0f, paint)

        paint.alpha = (intensity * 255).toInt().coerceIn(0, 255)
        canvas.drawBitmap(filtered, 0f, 0f, paint)

        return result
    }

    private fun drawStickerOnBitmap(bitmap: Bitmap, sticker: StickerLayer): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val stickerBaseSizeRatio = 0.25f
        val emojiFillRatio = 0.85f

        try {
            if (sticker.emojiContent.isNotEmpty()) {
                val baseEmojiSizePx = bitmap.width * stickerBaseSizeRatio
                val emojiSizePx = (baseEmojiSizePx * emojiFillRatio * sticker.scale).coerceAtLeast(10f)

                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = emojiSizePx
                    textAlign = Paint.Align.CENTER
                    alpha = sticker.alpha.coerceIn(0, 255)
                }

                val pixelX = sticker.x * bitmap.width
                val pixelY = sticker.y * bitmap.height
                val verticalCenterOffset = (paint.ascent() + paint.descent()) / 2f

                canvas.save()
                canvas.translate(pixelX, pixelY)
                canvas.rotate(sticker.rotation)
                canvas.drawText(sticker.emojiContent, 0f, -verticalCenterOffset, paint)
                canvas.restore()

            } else if (sticker.resourceId > 0) {
                @Suppress("DEPRECATION")
                val drawable = context.resources.getDrawable(sticker.resourceId, null)
                drawable?.let {
                    val baseSize = (bitmap.width * stickerBaseSizeRatio * sticker.scale).toInt().coerceAtLeast(8)
                    val pixelX = (sticker.x * bitmap.width).toInt()
                    val pixelY = (sticker.y * bitmap.height).toInt()

                    canvas.save()
                    canvas.translate(pixelX.toFloat(), pixelY.toFloat())
                    canvas.rotate(sticker.rotation)
                    it.setBounds(-baseSize / 2, -baseSize / 2, baseSize / 2, baseSize / 2)
                    if (sticker.alpha < 255) {
                        it.alpha = sticker.alpha.coerceIn(0, 255)
                    }
                    it.draw(canvas)
                    canvas.restore()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (result != bitmap) bitmap.recycle()
        return result
    }

    private fun drawTextOnBitmap(bitmap: Bitmap, text: TextLayer): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val pixelSize = (text.size * bitmap.width).coerceAtLeast(8f)
        val pixelX = text.x * bitmap.width
        val pixelY = text.y * bitmap.height

        val typeface = resolveTypeface(text.fontFamily, text.isBold, text.isItalic)
        val paintAlign = when (text.alignment) {
            com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.TextAlignment.LEFT -> Paint.Align.LEFT
            com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.TextAlignment.CENTER -> Paint.Align.CENTER
            com.editor.photo.video.collagemaker.photoedit.models.bottomsheets.TextAlignment.RIGHT -> Paint.Align.RIGHT
        }

        fun basePaint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = paintAlign
            this.textSize = pixelSize
            this.typeface = typeface
            isUnderlineText = text.isUnderline
            if (text.letterSpacing != 0f) letterSpacing = text.letterSpacing
        }

        try {
            canvas.save()
            canvas.rotate(text.rotation, pixelX, pixelY)

            if (text.strokeWidth > 0f) {
                val strokePaint = basePaint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = (text.strokeWidth * bitmap.width).coerceAtLeast(1f)
                    color = text.strokeColor
                    alpha = text.alpha.coerceIn(0, 255)
                }
                canvas.drawText(text.text, pixelX, pixelY, strokePaint)
            }

            val fillPaint = basePaint().apply {
                style = Paint.Style.FILL
                color = text.color
                alpha = text.alpha.coerceIn(0, 255)
            }
            canvas.drawText(text.text, pixelX, pixelY, fillPaint)
            canvas.restore()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (result != bitmap) bitmap.recycle()
        return result
    }

    private fun resolveTypeface(fontFamily: String?, isBold: Boolean, isItalic: Boolean): Typeface {
        val style = when {
            isBold && isItalic -> Typeface.BOLD_ITALIC
            isBold -> Typeface.BOLD
            isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        val base = when (fontFamily) {
            "serif" -> Typeface.SERIF
            "monospace" -> Typeface.MONOSPACE
            "sans_serif" -> Typeface.SANS_SERIF
            "casual" -> Typeface.create("casual", Typeface.NORMAL)
            "cursive" -> Typeface.create("cursive", Typeface.NORMAL)
            else -> Typeface.DEFAULT
        }
        return Typeface.create(base, style)
    }

    private fun drawFrameOnBitmap(bitmap: Bitmap, frame: FrameLayer): Bitmap {
        if (bitmap.isRecycled) return bitmap

        return try {
            val referenceDimension = 1080f
            val minDimension = kotlin.math.min(bitmap.width, bitmap.height).toFloat()
            val scaleFactor = (minDimension / referenceDimension).coerceAtLeast(1.0f)

            val basePadding = frame.padding
            val leftPadding = (basePadding * scaleFactor).toInt()
            val rightPadding = (basePadding * scaleFactor).toInt()
            val topPadding = (basePadding * scaleFactor).toInt()
            val bottomPadding = (basePadding * scaleFactor).toInt()

            val resultWidth = bitmap.width + leftPadding + rightPadding
            val resultHeight = bitmap.height + topPadding + bottomPadding

            val result = Bitmap.createBitmap(resultWidth, resultHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)

            val frameDrawable = try {
                androidx.core.content.ContextCompat.getDrawable(context, frame.resourceId)
            } catch (e: Exception) {
                null
            }
            if (frameDrawable != null) {
                frameDrawable.setBounds(0, 0, resultWidth, resultHeight)
                frameDrawable.draw(canvas)
            }

            canvas.drawBitmap(bitmap, leftPadding.toFloat(), topPadding.toFloat(), null)

            result
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }


    }


    private fun drawDoodleOnBitmap(bitmap: Bitmap, doodle: DoodlePath): Bitmap {
        if (doodle.points.isEmpty()) return bitmap

        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val pixelStrokeWidth = (doodle.strokeWidth * bitmap.width).coerceAtLeast(1f)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = doodle.color
            alpha = doodle.alpha.coerceIn(0, 255)
            strokeWidth = pixelStrokeWidth
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        fun px(pointIndex: Int) = doodle.points[pointIndex].x * bitmap.width
        fun py(pointIndex: Int) = doodle.points[pointIndex].y * bitmap.height

        try {
            canvas.save()
            when (doodle.shapeType) {
                DoodleShapeType.FREEHAND -> {
                    val path = Path()
                    path.moveTo(px(0), py(0))
                    for (i in 1 until doodle.points.size) {
                        path.lineTo(px(i), py(i))
                    }
                    if (doodle.points.size == 1) {
                        val cx = px(0)
                        val cy = py(0)
                        val dotPaint = Paint(paint).apply { style = Paint.Style.FILL }
                        canvas.drawCircle(cx, cy, pixelStrokeWidth / 2f, dotPaint)
                    } else {
                        canvas.drawPath(path, paint)
                    }
                }

                DoodleShapeType.LINE -> {
                    val last = doodle.points.size - 1
                    canvas.drawLine(px(0), py(0), px(last), py(last), paint)
                }

                DoodleShapeType.RECTANGLE -> {
                    val last = doodle.points.size - 1
                    val rect = RectF(
                        kotlin.math.min(px(0), px(last)),
                        kotlin.math.min(py(0), py(last)),
                        kotlin.math.max(px(0), px(last)),
                        kotlin.math.max(py(0), py(last))
                    )
                    canvas.drawRect(rect, paint)
                }

                DoodleShapeType.OVAL -> {
                    val last = doodle.points.size - 1
                    val rect = RectF(
                        kotlin.math.min(px(0), px(last)),
                        kotlin.math.min(py(0), py(last)),
                        kotlin.math.max(px(0), px(last)),
                        kotlin.math.max(py(0), py(last))
                    )
                    canvas.drawOval(rect, paint)
                }
            }
            canvas.restore()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (result != bitmap) bitmap.recycle()
        return result
    }

    fun clean() {
        historyManager.clear()
        layerManager.clear()
        originalBitmap?.recycle()
        originalBitmap = null
        baseUri = null
    }
}