package com.ruleup.profile.data.api

import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.EmptyData
import com.ruleup.profile.data.dto.CategoriesResponse
import com.ruleup.profile.data.dto.MyProfileResponse
import com.ruleup.profile.data.dto.NicknameCheckRequest
import com.ruleup.profile.data.dto.NicknameCheckResponse
import com.ruleup.profile.data.dto.ProfileImageResponse
import com.ruleup.profile.data.dto.ProfileResponse
import com.ruleup.profile.data.dto.UpdateProfileRequest
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
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

    /**
     * 내 프로필 조회. 로그인 응답과 같은 user 스키마에 생일·성별·약관 동의가 더 붙는다.
     * 아래 [getProfile] 은 구 엔드포인트 — 매너 온도·닉네임 변경 이력이 거기에만 있어 함께 남긴다.
     */
    @GET("v1/users/me")
    suspend fun getMyProfile(): BaseResponse<MyProfileResponse>

    // 4.8 내 프로필 조회 (레거시)
    @GET("v1/profile")
    suspend fun getProfile(): BaseResponse<ProfileResponse>

    // 4.9 프로필 수정
    @PATCH("v1/profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest,
    ): BaseResponse<ProfileResponse>

    // 프로필 사진 업로드 + 등록. 가입 직후와 프로필 편집이 같이 쓴다(accessToken 필요).
    @Multipart
    @POST("v1/users/me/profile-image")
    suspend fun uploadProfileImage(
        @Part image: MultipartBody.Part,
    ): BaseResponse<ProfileImageResponse>

    // 4.11 프로필 사진 제거
    @DELETE("v1/profile/image")
    suspend fun deleteProfileImage(): BaseResponse<EmptyData>
}
