package com.ruleup.challenge.data.api

import com.ruleup.challenge.data.dto.ChallengeDetailResponse
import com.ruleup.challenge.data.dto.ChallengeImageResponse
import com.ruleup.challenge.data.dto.ChallengeMembersResponse
import com.ruleup.challenge.data.dto.ChallengeResponse
import com.ruleup.challenge.data.dto.CreateChallengeRequest
import com.ruleup.challenge.data.dto.MemberDecisionRequest
import com.ruleup.challenge.data.dto.MemberStatusResponse
import com.ruleup.challenge.data.dto.RecommendationRequest
import com.ruleup.challenge.data.dto.RecommendationResponse
import com.ruleup.challenge.data.dto.UpdateChallengeRequest
import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.EmptyData
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.PATCH
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query
import io.ktor.client.request.forms.MultiPartFormDataContent

interface ChallengeApi {
    // 3.1 LLM 기본값 추천 (초안)
    @POST("v1/challenges/recommendation")
    suspend fun recommend(
        @Body request: RecommendationRequest,
    ): BaseResponse<RecommendationResponse>

    // 3.2 챌린지 생성
    @POST("v1/challenges")
    suspend fun create(
        @Body request: CreateChallengeRequest,
    ): BaseResponse<ChallengeResponse>

    // 3.3 챌린지 상세 + 참여 자격
    @GET("v1/challenges/{challengeId}")
    suspend fun getChallenge(
        @Path("challengeId") challengeId: String,
    ): BaseResponse<ChallengeDetailResponse>

    // 3.4 챌린지 수정 (시작 전, 생성자만)
    @PATCH("v1/challenges/{challengeId}")
    suspend fun update(
        @Path("challengeId") challengeId: String,
        @Body request: UpdateChallengeRequest,
    ): BaseResponse<ChallengeResponse>

    // 3.5 챌린지 삭제 (소프트)
    @DELETE("v1/challenges/{challengeId}")
    suspend fun delete(
        @Path("challengeId") challengeId: String,
    ): BaseResponse<EmptyData>

    // 3.6 챌린지 참여 신청
    @POST("v1/challenges/{challengeId}/members")
    suspend fun join(
        @Path("challengeId") challengeId: String,
    ): BaseResponse<MemberStatusResponse>

    // 3.7 참여 승인/거절 (운영자)
    @PATCH("v1/challenges/{challengeId}/members/{userId}")
    suspend fun decideMember(
        @Path("challengeId") challengeId: String,
        @Path("userId") userId: String,
        @Body request: MemberDecisionRequest,
    ): BaseResponse<MemberStatusResponse>

    // 3.8 멤버 목록 조회
    @GET("v1/challenges/{challengeId}/members")
    suspend fun getMembers(
        @Path("challengeId") challengeId: String,
        @Query("status") status: String? = null,
    ): BaseResponse<ChallengeMembersResponse>

    // 3.9 챌린지 대표 이미지 업로드 (생성/수정 전 호출, challengeId 불필요)
    @POST("v1/challenges/image")
    suspend fun uploadImage(
        @Body content: MultiPartFormDataContent,
    ): BaseResponse<ChallengeImageResponse>
}
