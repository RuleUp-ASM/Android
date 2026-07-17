package com.ruleup.profile.data.api

import com.ruleup.network.dto.BaseResponse
import com.ruleup.profile.data.dto.MyChallengesSliceResponse
import com.ruleup.profile.data.dto.MyHomeResponse
import com.ruleup.profile.data.dto.ReputationHistoryResponse
import com.ruleup.profile.data.dto.ReputationResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MyPageApi {
    // 마이 홈 일괄 조회 (토큰의 userId 사용)
    @GET("v1/me/home")
    suspend fun getHome(): BaseResponse<MyHomeResponse>

    // 내 챌린지 목록 (챌린지 생성 스펙 계약) — 그룹 랭킹 진입용 최소 필드만 역직렬화한다.
    @GET("v1/challenges")
    suspend fun getMyChallenges(
        @Query("scope") scope: String = "ACTIVE",
    ): BaseResponse<MyChallengesSliceResponse>

    // 매너 온도 상세 (변동 로그는 서버 고정 최근 10건)
    @GET("v1/me/reputation")
    suspend fun getReputation(): BaseResponse<ReputationResponse>

    // 평판 히스토리 (전체 반환 — 서버 상한 50건)
    @GET("v1/me/reputation/history")
    suspend fun getReputationHistory(): BaseResponse<ReputationHistoryResponse>
}
