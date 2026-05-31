package com.ruleup.data.api

import com.ruleup.data.dto.request.UpdateProfileRequest
import com.ruleup.data.dto.response.ProfileImageResponse
import com.ruleup.data.dto.response.ProfileResponse
import com.ruleup.network.dto.BaseResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part

interface ProfileApi {

    @GET("profile/me")
    suspend fun getMyProfile(): BaseResponse<ProfileResponse>

    @PATCH("profile/me")
    suspend fun updateMyProfile(
        @Body request: UpdateProfileRequest,
    ): BaseResponse<ProfileResponse>

    @Multipart
    @POST("profile/image")
    suspend fun uploadProfileImage(
        @Part image: MultipartBody.Part,
    ): BaseResponse<ProfileImageResponse>

    @DELETE("profile/image")
    suspend fun deleteProfileImage(): BaseResponse<Unit>
}
