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

    /**
     * 내 프로필 조회. 로그인 응답과 같은 user 스키마에 생일·성별·약관 동의가 더 붙는다.
     * 아래 [getProfile] 은 구 엔드포인트 — 매너 온도·닉네임 변경 이력이 거기에만 있어 함께 남긴다.
     */
    @GET("v1/users/me")
    suspend fun getMyProfile(): BaseResponse<MyProfileResponse>

    // 4.8 내 프로필 조회 (레거시)
    @GET("v1/profile")
    suspend fun getProfile(): BaseResponse<ProfileResponse>

    // 구 PATCH v1/profile 은 스테이징이 405 로 막는다 — 현행 명세는 마이페이지 모듈 경로다.
    @PATCH("v1/users/me/profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest,
    ): BaseResponse<UpdateProfileResponse>

    /**
     * 타인 프로필 조회. 공개 범위가 좁아 내 프로필과 응답 스키마가 다르다 —
     * 점수·통계·진행 중 목록은 오지 않는다.
     */
    @GET("v1/users/{userId}/profile")
    suspend fun getMemberProfile(
        @Path("userId") userId: String,
    ): BaseResponse<MemberProfileResponse>

    // 프로필 사진 업로드 + 등록. 가입 직후와 프로필 편집이 같이 쓴다(accessToken 필요).
    @Multipart
    @POST("v1/users/me/profile-image")
    suspend fun uploadProfileImage(
        @Part image: MultipartBody.Part,
    ): BaseResponse<ProfileImageResponse>
}
