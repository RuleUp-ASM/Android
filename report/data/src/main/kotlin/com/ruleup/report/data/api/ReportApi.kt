package com.ruleup.report.data.api

import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.EmptyData
import com.ruleup.report.data.dto.BlockListResponse
import com.ruleup.report.data.dto.ReportCreateResponse
import com.ruleup.report.data.dto.ReportRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ReportApi {
    // 신고 접수(201). base(.../api/) + v1/reports → /api/v1/reports
    @POST("v1/reports")
    suspend fun report(
        @Body request: ReportRequest,
    ): BaseResponse<ReportCreateResponse>

    // 내가 차단한 목록. → /api/v1/users/me/blocks
    @GET("v1/users/me/blocks")
    suspend fun getBlocks(): BaseResponse<BlockListResponse>

    // 사용자 차단 해제. → /api/v1/users/me/blocks/users/{blockedUserId}
    // 응답 `{removed:true}` 는 쓰지 않는다 — 해제할 게 없으면 404 로 갈리므로 성공이면 언제나 true 다.
    @DELETE("v1/users/me/blocks/users/{blockedUserId}")
    suspend fun unblockUser(
        @Path("blockedUserId") blockedUserId: String,
    ): BaseResponse<EmptyData>

    // 챌린지 차단 해제. → /api/v1/users/me/blocks/challenges/{challengeId}
    @DELETE("v1/users/me/blocks/challenges/{challengeId}")
    suspend fun unblockChallenge(
        @Path("challengeId") challengeId: String,
    ): BaseResponse<EmptyData>
}
