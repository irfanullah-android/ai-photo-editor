package com.editor.photo.video.collagemaker.photoedit.repository

import com.editor.photo.video.collagemaker.photoedit.AiApiService.NetworkModule

import android.content.ContentResolver
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class AiGenerationRepository(private val contentResolver: ContentResolver) {

    private val TAG = "AiGenerationRepo"
    private val api = NetworkModule.aiApiService

    // ── Ping ────────────────────────────────────────────────────────────────

    suspend fun pingServer() {
        try {
            api.ping()
            Log.d(TAG, "Server ping OK")
        } catch (e: Exception) {
            Log.d(TAG, "Ping failed (ignored): ${e.message}")
        }
    }

    // ── Group Photo (existing) ──────────────────────────────────────────────

    suspend fun generate(
        person1Uri: Uri,
        person2Uri: Uri,
        templateResId: Int,
        templatePrompt: String,
        resources: Resources
    ): Bitmap {
        val person1Part = prepareImagePart("person1_image", person1Uri)
            ?: error("Could not process person1 image")
        val person2Part = prepareImagePart("person2_image", person2Uri)
            ?: error("Could not process person2 image")
        val refPart = getDrawableAsMultipart(resources, templateResId, "reference_image")

        val finalPrompt = buildWrappedPrompt(templatePrompt)
        Log.d(TAG, "=== PROMPT BEING SENT ===")
        Log.d(TAG, finalPrompt)

        val promptBody = finalPrompt.toRequestBody("text/plain".toMediaTypeOrNull())

        Log.d(TAG, "=== SENDING REQUEST ===")
        Log.d(TAG, "person1Part name: ${person1Part.headers}")
        Log.d(TAG, "person2Part name: ${person2Part.headers}")
        Log.d(TAG, "refPart name: ${refPart.headers}")

        val raw = api.generateGroupPhoto(person1Part, person2Part, refPart, promptBody).string()

        Log.d(TAG, "=== RAW RESPONSE ===")
        Log.d(TAG, "Length: ${raw.length}")
        Log.d(TAG, "First 300: ${raw.take(300)}")
        Log.d(TAG, "Last 100: ${raw.takeLast(100)}")

        // JSON keys log karo
        try {
            val json = JSONObject(raw)
            val keys = json.keys().asSequence().toList()
            Log.d(TAG, "=== JSON KEYS: $keys ===")
            keys.forEach { key ->
                val value = json.optString(key, "")
                Log.d(TAG, "  KEY='$key' | value_length=${value.length} | preview='${value.take(80)}'")
            }
        } catch (e: Exception) {
            Log.e(TAG, "=== NOT JSON — raw start: ${raw.take(200)}")
        }

        if (raw.trimStart().startsWith("<")) error("Server returned HTML error page")
        if (raw.length < 100) error("Server returned empty response")

        return parseAndDecodeBitmap(raw) ?: error("Could not decode image from response")
    }

    private fun buildWrappedPrompt(templatePrompt: String): String = buildString {
        // AI ko forceful instructions dein
        append("SYSTEM: This is an AI generation task. ")
        append("The provided 'reference_image' is for POSE and STRUCTURE guidance only. ")
        append("DO NOT copy the reference_image appearance, style, or content. ")
        append("The output must be a completely original generation based on the text prompt below. ")
        append("IDENTITY LOCK: Use person1_image and person2_image faces exactly as provided. Do not blend. ")
        append("\n\nPROMPT: ")
        append(templatePrompt)
    }


    // ── Single Pose (new) ───────────────────────────────────────────────────

    suspend fun generateSingle(
        personUri: Uri,
        templateResId: Int,
        templatePrompt: String,
        resources: Resources
    ): Bitmap {
        val personPart = prepareImagePart("person_image", personUri)
            ?: error("Could not process person image")
        val refPart = getDrawableAsMultipart(resources, templateResId, "reference_image")
        val promptBody = buildWrappedSinglePrompt(templatePrompt)
            .toRequestBody("text/plain".toMediaTypeOrNull())

        val raw = api.generateSinglePose(personPart, refPart, promptBody).string()
        Log.d(TAG, "Single response length: ${raw.length}, preview: ${raw.take(200)}")

        if (raw.trimStart().startsWith("<")) error("Server returned HTML error page")
        if (raw.length < 100) error("Server returned empty response")

        return parseAndDecodeBitmap(raw) ?: error("Could not decode image from response")
    }

    private fun buildWrappedSinglePrompt(templatePrompt: String): String = buildString {
        append("SYSTEM: This is an AI generation task. ")
        append("The provided 'reference_image' is for POSE and STRUCTURE guidance only. ")
        append("DO NOT copy the reference_image appearance, style, or content. ")
        append("The output must be a completely original generation based on the text prompt below. ")
        append("IDENTITY LOCK: Use person_image face exactly as provided. Do not modify. ")
        append("\n\nPROMPT: ")
        append(templatePrompt)
    }

    // ── Image Helpers (shared) ──────────────────────────────────────────────

    private fun prepareImagePart(partName: String, uri: Uri): MultipartBody.Part? {
        return try {
            val maxDim = 1500

            var rotation = 0f
            contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                rotation = when (exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90  -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }

            val sizeOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, sizeOpts)
            }
            if (sizeOpts.outWidth <= 0 || sizeOpts.outHeight <= 0) return null

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(sizeOpts.outWidth, sizeOpts.outHeight, maxDim)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val sampled = contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null

            val scaled = ensureMaxDim(sampled, maxDim)
            val final = if (rotation != 0f) {
                Bitmap.createBitmap(
                    scaled, 0, 0, scaled.width, scaled.height,
                    Matrix().apply { postRotate(rotation) }, true
                ).also { if (scaled !== sampled) scaled.recycle() }
            } else scaled

            val bytes = ByteArrayOutputStream().use { out ->
                final.compress(Bitmap.CompressFormat.JPEG, 100, out)
                final.recycle()
                out.toByteArray()
            }

            MultipartBody.Part.createFormData(
                partName, "img.jpg",
                bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            )
        } catch (e: Exception) {
            Log.e(TAG, "prepareImagePart failed for $partName", e)
            null
        }
    }

    private fun getDrawableAsMultipart(
        resources: Resources,
        drawableId: Int,
        partName: String
    ): MultipartBody.Part {
        val bitmap = BitmapFactory.decodeResource(
            resources, drawableId,
            BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        )
        val bytes = ByteArrayOutputStream().use { out ->
            // Quality ko 95 se kam karke 60-70 par layein
            // Is se AI model reference image ke pixels ko "blur" dekhega
            // aur copy karne ke bajaye prompt par focus karega
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
            bitmap.recycle()
            out.toByteArray()
        }
        return MultipartBody.Part.createFormData(
            partName, "ref.jpg",
            bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        )
    }

    private fun parseAndDecodeBitmap(responseString: String): Bitmap? {
        return try {
            val json = JSONObject(responseString)

            // Log ALL keys for debugging
            val allKeys = json.keys().asSequence().toList()
            Log.d(TAG, "Response JSON keys: $allKeys")

            // Nested "data" object bhi check karo
            val dataObj = json.optJSONObject("data")
            if (dataObj != null) {
                val dataKeys = dataObj.keys().asSequence().toList()
                Log.d(TAG, "data{} keys: $dataKeys")
            }

            val keys = listOf(
                "image_base64", "image", "result",
                "generated_image", "output", "data", "base64",
                "url", "image_url"  // URL-based response bhi ho sakta hai
            )

            var base64Str = keys.firstNotNullOfOrNull { key ->
                json.optString(key, "").takeIf { it.length > 100 }
            } ?: run {
                keys.firstNotNullOfOrNull { key ->
                    dataObj?.optString(key, "")?.takeIf { it.length > 100 }
                }
            } ?: ""

            Log.d(TAG, "base64Str length: ${base64Str.length}, preview: ${base64Str.take(50)}")

            if (base64Str.isEmpty()) null else decodeBase64ToBitmap(base64Str)
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse failed: ${e.message}")
            if (responseString.length > 100) decodeBase64ToBitmap(responseString) else null
        }

    }

    private fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val clean = if (base64Str.contains(",")) base64Str.substringAfter(",") else base64Str
            val bytes = Base64.decode(clean.trim(), Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Base64 decode failed", e); null
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var size = 1; var hw = width / 2; var hh = height / 2
        while (hw >= maxDim || hh >= maxDim) { size *= 2; hw /= 2; hh /= 2 }
        return size
    }

    private fun ensureMaxDim(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width; val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        return if (w >= h)
            Bitmap.createScaledBitmap(bitmap, maxDim, (h.toFloat() / w * maxDim).toInt().coerceAtLeast(1), true)
        else
            Bitmap.createScaledBitmap(bitmap, (w.toFloat() / h * maxDim).toInt().coerceAtLeast(1), maxDim, true)
    }
}