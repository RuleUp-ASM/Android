package com.ruleup.verification.data.api

import com.ruleup.network.dto.BaseResponse
import com.ruleup.verification.data.dto.ManualSubmitRequest
import com.ruleup.verification.data.dto.ManualSubmitResponse
import com.ruleup.verification.data.dto.PlaceSearchResponse
import com.ruleup.verification.data.dto.ProgressResponse
import com.ruleup.verification.data.dto.SyncRequest
import com.ruleup.verification.data.dto.SyncResponse
import com.ruleup.verification.data.dto.VerificationDetailResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface VerificationApi {
    // 3.1 30분마다 신호 받고 평가
    @POST("v1/verifications/sync")
    suspend fun sync(
        @Body request: SyncRequest,
    ): BaseResponse<SyncResponse>

    // 3.2 챌린지 진행률 일괄 조회
    @GET("v1/verifications/progress")
    suspend fun getProgress(
        @Query("status") status: String? = null,
    ): BaseResponse<ProgressResponse>

    // 3.3 챌린지 인증 여부 판단(검증 결과 + 실패 사유)
    @GET("v1/challenges/{challengeId}/verification")
    suspend fun getVerification(
        @Path("challengeId") challengeId: String,
        @Query("logDays") logDays: Int? = null,
    ): BaseResponse<VerificationDetailResponse>

    // 3.4 수동 인증 제출 (VF-04)
    @POST("v1/challenges/{challengeId}/verifications")
    suspend fun submitManual(
        @Path("challengeId") challengeId: String,
        @Body request: ManualSubmitRequest,
    ): BaseResponse<ManualSubmitResponse>

    // 11.7 장소 검색(Naver Local 프록시) — 셋업·수정 시 앵커 채우기용
    @GET("v1/places/search")
    suspend fun searchPlaces(
        @Query("q") query: String,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("radiusM") radiusM: Int? = null,
    ): BaseResponse<PlaceSearchResponse>
}
