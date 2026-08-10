package com.editor.photo.video.collagemaker.photoedit.utlis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp

class BackgroundRemover(context: Context) {

    companion object {
        private const val TAG = "BackgroundRemover"
    }

    private var interpreter: Interpreter? = null

    // Will be set dynamically based on model
    private var inputWidth = 320
    private var inputHeight = 320
    private var isChannelFirst = false

    // Normalization values for U2NET
    private val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val std = floatArrayOf(0.229f, 0.224f, 0.225f)

    init {
        setupInterpreter(context)
    }

    private fun setupInterpreter(context: Context) {
        try {
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }

            val model = loadModelFile(context, "u2netp.tflite")
            interpreter = Interpreter(model, options)

            // Get model info
            analyzeModel()

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up interpreter", e)
            e.printStackTrace()
        }
    }

    private fun analyzeModel() {
        interpreter?.let { interp ->
            // Analyze input
            val inputTensor = interp.getInputTensor(0)
            val inputShape = inputTensor.shape()
            Log.d(TAG, "Input shape: ${inputShape.contentToString()}")
            Log.d(TAG, "Input type: ${inputTensor.dataType()}")

            // Determine input dimensions
            // Shape could be [1, H, W, 3] (NHWC) or [1, 3, H, W] (NCHW)
            if (inputShape.size == 4) {
                if (inputShape[1] == 3) {
                    // NCHW format
                    isChannelFirst = true
                    inputHeight = inputShape[2]
                    inputWidth = inputShape[3]
                } else {
                    // NHWC format
                    isChannelFirst = false
                    inputHeight = inputShape[1]
                    inputWidth = inputShape[2]
                }
            }
            Log.d(TAG, "Input size: ${inputWidth}x${inputHeight}, ChannelFirst: $isChannelFirst")

            // Analyze all outputs
            val outputCount = interp.outputTensorCount
            Log.d(TAG, "Output count: $outputCount")

            for (i in 0 until outputCount) {
                val outputTensor = interp.getOutputTensor(i)
                Log.d(TAG, "Output $i shape: ${outputTensor.shape().contentToString()}")
                Log.d(TAG, "Output $i type: ${outputTensor.dataType()}")
            }
        }
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelName)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun removeBackground(bitmap: Bitmap): Bitmap {
        return removeBackgroundSoft(bitmap)
    }

    fun removeBackgroundSoft(bitmap: Bitmap): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        Log.d(TAG, "Original image size: ${originalWidth}x${originalHeight}")

        // Resize bitmap to model input size
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true)

        // Prepare input buffer
        val inputBuffer = prepareInputBuffer(resizedBitmap)

        // Run inference and get mask
        val maskBitmap = runInference(inputBuffer)

        // Resize mask to original size
        val resizedMask = Bitmap.createScaledBitmap(maskBitmap, originalWidth, originalHeight, true)

        // Apply mask to original image
        return applyMask(bitmap, resizedMask)
    }

    private fun prepareInputBuffer(bitmap: Bitmap): ByteBuffer {
        val bufferSize = 1 * 3 * inputHeight * inputWidth * 4 // Float32 = 4 bytes
        val inputBuffer = ByteBuffer.allocateDirect(bufferSize)
        inputBuffer.order(ByteOrder.nativeOrder())
        inputBuffer.rewind()

        val pixels = IntArray(inputWidth * inputHeight)
        bitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)

        if (isChannelFirst) {
            // NCHW format: all R values, then all G values, then all B values
            // R channel
            for (y in 0 until inputHeight) {
                for (x in 0 until inputWidth) {
                    val pixel = pixels[y * inputWidth + x]
                    val r = ((Color.red(pixel) / 255.0f) - mean[0]) / std[0]
                    inputBuffer.putFloat(r)
                }
            }
            // G channel
            for (y in 0 until inputHeight) {
                for (x in 0 until inputWidth) {
                    val pixel = pixels[y * inputWidth + x]
                    val g = ((Color.green(pixel) / 255.0f) - mean[1]) / std[1]
                    inputBuffer.putFloat(g)
                }
            }
            // B channel
            for (y in 0 until inputHeight) {
                for (x in 0 until inputWidth) {
                    val pixel = pixels[y * inputWidth + x]
                    val b = ((Color.blue(pixel) / 255.0f) - mean[2]) / std[2]
                    inputBuffer.putFloat(b)
                }
            }
        } else {
            // NHWC format: RGB interleaved
            for (y in 0 until inputHeight) {
                for (x in 0 until inputWidth) {
                    val pixel = pixels[y * inputWidth + x]
                    val r = ((Color.red(pixel) / 255.0f) - mean[0]) / std[0]
                    val g = ((Color.green(pixel) / 255.0f) - mean[1]) / std[1]
                    val b = ((Color.blue(pixel) / 255.0f) - mean[2]) / std[2]
                    inputBuffer.putFloat(r)
                    inputBuffer.putFloat(g)
                    inputBuffer.putFloat(b)
                }
            }
        }

        inputBuffer.rewind()
        return inputBuffer
    }

    private fun runInference(inputBuffer: ByteBuffer): Bitmap {
        val interp = interpreter ?: throw IllegalStateException("Interpreter not initialized")

        // Get output tensor info
        val outputTensor = interp.getOutputTensor(0)
        val outputShape = outputTensor.shape()
        Log.d(TAG, "Running inference, output shape: ${outputShape.contentToString()}")

        // Calculate output buffer size
        val outputSize = outputShape.fold(1) { acc, i -> acc * i }
        val outputBuffer = ByteBuffer.allocateDirect(outputSize * 4)
        outputBuffer.order(ByteOrder.nativeOrder())

        // Run inference
        interp.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()

        // Convert output to float array
        val outputArray = FloatArray(outputSize)
        outputBuffer.asFloatBuffer().get(outputArray)

        // Find min and max for normalization
        var minVal = Float.MAX_VALUE
        var maxVal = Float.MIN_VALUE
        for (value in outputArray) {
            if (value < minVal) minVal = value
            if (value > maxVal) maxVal = value
        }
        Log.d(TAG, "Output range: min=$minVal, max=$maxVal")

        // Determine output dimensions
        val outHeight: Int
        val outWidth: Int

        when (outputShape.size) {
            4 -> {
                // Could be [1, H, W, 1] or [1, 1, H, W]
                if (outputShape[1] == 1) {
                    // [1, 1, H, W] - channel first
                    outHeight = outputShape[2]
                    outWidth = outputShape[3]
                } else if (outputShape[3] == 1) {
                    // [1, H, W, 1] - channel last
                    outHeight = outputShape[1]
                    outWidth = outputShape[2]
                } else {
                    // [1, C, H, W] - use first channel
                    outHeight = outputShape[2]
                    outWidth = outputShape[3]
                }
            }
            3 -> {
                // [1, H, W]
                outHeight = outputShape[1]
                outWidth = outputShape[2]
            }
            2 -> {
                // [H, W]
                outHeight = outputShape[0]
                outWidth = outputShape[1]
            }
            else -> {
                outHeight = inputHeight
                outWidth = inputWidth
            }
        }

        Log.d(TAG, "Output dimensions: ${outWidth}x${outHeight}")

        // Create mask bitmap
        val maskBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(outWidth * outHeight)

        // Process each pixel
        for (i in 0 until minOf(outputArray.size, outWidth * outHeight)) {
            var value = outputArray[i]

            // Apply sigmoid if values are not already in 0-1 range
            if (minVal < 0 || maxVal > 1) {
                value = sigmoid(value)
            }

            // Normalize to 0-255
            val alpha = (value * 255).toInt().coerceIn(0, 255)
            pixels[i] = Color.argb(alpha, 255, 255, 255)
        }

        maskBitmap.setPixels(pixels, 0, outWidth, 0, 0, outWidth, outHeight)
        return maskBitmap
    }

    private fun sigmoid(x: Float): Float {
        return (1.0f / (1.0f + exp(-x.coerceIn(-20f, 20f))))
    }

    private fun applyMask(original: Bitmap, mask: Bitmap): Bitmap {
        val width = original.width
        val height = original.height

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val originalPixels = IntArray(width * height)
        val maskPixels = IntArray(width * height)
        val resultPixels = IntArray(width * height)

        original.getPixels(originalPixels, 0, width, 0, 0, width, height)
        mask.getPixels(maskPixels, 0, width, 0, 0, width, height)

        // Find threshold dynamically
        var totalAlpha = 0L
        for (i in maskPixels.indices) {
            totalAlpha += Color.alpha(maskPixels[i])
        }
        val avgAlpha = (totalAlpha / maskPixels.size).toInt()
        val threshold = maxOf(avgAlpha, 128)

        Log.d(TAG, "Mask average alpha: $avgAlpha, threshold: $threshold")

        for (i in originalPixels.indices) {
            val originalPixel = originalPixels[i]
            val maskAlpha = Color.alpha(maskPixels[i])

            // Use smooth alpha for better edges
            val alpha = when {
                maskAlpha > threshold + 30 -> 255
                maskAlpha < threshold - 30 -> 0
                else -> ((maskAlpha - (threshold - 30)) * 255 / 60).coerceIn(0, 255)
            }

            resultPixels[i] = Color.argb(
                alpha,
                Color.red(originalPixel),
                Color.green(originalPixel),
                Color.blue(originalPixel)
            )
        }

        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return result
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}