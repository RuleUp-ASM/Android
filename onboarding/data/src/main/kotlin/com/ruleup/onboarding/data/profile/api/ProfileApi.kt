package com.ruleup.onboarding.data.profile.api

import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.EmptyData
import com.ruleup.onboarding.data.profile.dto.CategoriesResponse
import com.ruleup.onboarding.data.profile.dto.NicknameCheckRequest
import com.ruleup.onboarding.data.profile.dto.NicknameCheckResponse
import com.ruleup.onboarding.data.profile.dto.OnboardingMeRequest
import com.ruleup.onboarding.data.profile.dto.ProfileImageResponse
import com.ruleup.onboarding.data.profile.dto.ProfileResponse
import com.ruleup.onboarding.data.profile.dto.UpdateProfileRequest
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

interface ProfileApi {
    // 4.6 닉네임 형식/중복 검사
    @POST("v1/nicknames/check")
    suspend fun checkNickname(
        @Body request: NicknameCheckRequest,
    ): BaseResponse<NicknameCheckResponse>

    // 4.7 관심 카테고리 마스터
    @GET("v1/categories")
    suspend fun getCategories(): BaseResponse<CategoriesResponse>

    // 4.8 내 프로필 조회
    @GET("v1/profile")
    suspend fun getProfile(): BaseResponse<ProfileResponse>

    // 4.9 프로필 수정
    @PATCH("v1/profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest,
    ): BaseResponse<ProfileResponse>

    // 4.10 프로필 사진 업로드 (multipart: image)
    @Multipart
    @POST("v1/profile/image")
    suspend fun uploadProfileImage(
        @Part image: MultipartBody.Part,
    ): BaseResponse<ProfileImageResponse>

    // 4.11 프로필 사진 제거
    @DELETE("v1/profile/image")
    suspend fun deleteProfileImage(): BaseResponse<EmptyData>

    // 가입 기본정보 입력 (PUT /onboarding/me — birthDate·gender 선택, 추천 세그먼트용)
    @PUT("v1/onboarding/me")
    suspend fun updateOnboardingMe(
        @Body request: OnboardingMeRequest,
    ): BaseResponse<EmptyData>
}
