package com.ruleup.profile.data.api

import com.ruleup.network.dto.BaseResponse
import com.ruleup.profile.data.dto.CategoriesResponse
import com.ruleup.profile.data.dto.MemberProfileResponse
import com.ruleup.profile.data.dto.MyProfileResponse
import com.ruleup.profile.data.dto.NicknameCheckRequest
import com.ruleup.profile.data.dto.NicknameCheckResponse
import com.ruleup.profile.data.dto.ProfileImageResponse
import com.ruleup.profile.data.dto.ProfileResponse
import com.ruleup.profile.data.dto.UpdateProfileRequest
import com.ruleup.profile.data.dto.UpdateProfileResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ProfileApi {
    // 4.6 닉네임 형식/중복 검사
    @POST("v1/nicknames/check")
    suspend fun checkNickname(
        @Body request: NicknameCheckRequest,
    ): BaseResponse<NicknameCheckResponse>

    // 4.7 관심 카테고리 마스터
    @GET("v1/categories")
    suspend fun getCategories(): BaseResponse<CategoriesResponse>

    /** 내 프로필 조회. */
    @GET("v1/users/me")
    suspend fun getMyProfile(): BaseResponse<MyProfileResponse>

    // 4.8 내 프로필 조회 (레거시)
    @GET("v1/profile")
    suspend fun getProfile(): BaseResponse<ProfileResponse>

    // 구 PATCH v1/profile 은 스테이징이 405 로 막는다
    @PATCH("v1/users/me/profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest,
    ): BaseResponse<UpdateProfileResponse>

    /** 타인 프로필 조회. */
    @GET("v1/users/{userId}/profile")
    suspend fun getMemberProfile(
        @Path("userId") userId: String,
    ): BaseResponse<MemberProfileResponse>

    // 프로필 사진 업로드 + 등록.
    @Multipart
    @POST("v1/users/me/profile-image")
    suspend fun uploadProfileImage(
        @Part image: MultipartBody.Part,
    ): BaseResponse<ProfileImageResponse>
}
