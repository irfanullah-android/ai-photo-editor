package com.editor.photo.video.collagemaker.photoedit.AiApiService

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface AiApiService {

    // ── Group Photo ─────────────────────────────────────────────────────────
    @Multipart
    @POST("generate-template-photo")
    suspend fun generateGroupPhoto(
        @Part person1Image: MultipartBody.Part,
        @Part person2Image: MultipartBody.Part,
        @Part referenceImage: MultipartBody.Part,
        @Part("template_prompt") prompt: RequestBody
    ): ResponseBody

    // ── Single Pose ─────────────────────────────────────────────────────────
    @Multipart
    @POST("generate-single-pose")
    suspend fun generateSinglePose(
        @Part personImage: MultipartBody.Part,
        @Part referenceImage: MultipartBody.Part,
        @Part("single_template_prompt") prompt: RequestBody
    ): ResponseBody

    // ── Health Check ────────────────────────────────────────────────────────
    @GET("health")
    suspend fun ping(): ResponseBody
}