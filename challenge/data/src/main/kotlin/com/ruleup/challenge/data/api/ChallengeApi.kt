package com.ruleup.challenge.data.api

import com.ruleup.challenge.data.dto.ChallengeCategoriesResponse
import com.ruleup.challenge.data.dto.ChallengeDetailResponse
import com.ruleup.challenge.data.dto.ChallengeImageResponse
import com.ruleup.challenge.data.dto.ChallengeMembersResponse
import com.ruleup.challenge.data.dto.ChallengeResponse
import com.ruleup.challenge.data.dto.ChallengeSetupInfoResponse
import com.ruleup.challenge.data.dto.CreateChallengeRequest
import com.ruleup.challenge.data.dto.ExploreChallengesResponse
import com.ruleup.challenge.data.dto.MemberDecisionRequest
import com.ruleup.challenge.data.dto.MemberStatusResponse
import com.ruleup.challenge.data.dto.MyChallengesResponse
import com.ruleup.challenge.data.dto.RecommendationRequest
import com.ruleup.challenge.data.dto.RecommendationResponse
import com.ruleup.challenge.data.dto.TrendingChallengesResponse
import com.ruleup.challenge.data.dto.UpdateChallengeRequest
import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.EmptyData
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

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

    // 챌린지 최초 조회 (GET setup): 셋업 단계에서 무엇을 바인딩해야 하는지 요구사항 조회
    @GET("v1/challenges/{challengeId}/setup")
    suspend fun getSetup(
        @Path("challengeId") challengeId: String,
    ): BaseResponse<ChallengeSetupInfoResponse>

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
    @Multipart
    @POST("v1/challenges/image")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part,
    ): BaseResponse<ChallengeImageResponse>

    // 내 챌린지 목록 조회 (GET /challenges): scope 기본 ACTIVE, ALL 은 PENDING 포함
    @GET("v1/challenges")
    suspend fun getMyChallenges(
        @Query("scope") scope: String? = null,
    ): BaseResponse<MyChallengesResponse>

    // 탐색: 실시간 인기 챌린지 조회 (파라미터 없음, 서버가 Top 20 반환 · 홈은 상위 일부 사용)
    @GET("v1/challenges/trending")
    suspend fun getTrending(): BaseResponse<TrendingChallengesResponse>

    // 탐색: 카테고리별 진행 중 챌린지 수 조회
    @GET("v1/challenge-categories")
    suspend fun getCategories(): BaseResponse<ChallengeCategoriesResponse>

    // 탐색: 챌린지 둘러보기 (공통 제외 → 필터 AND → 정렬 → 커서 페이지네이션).
    // 매너 온도 컷은 값 대신 joinableOnly 로 — 서버가 토큰 사용자 온도 기준으로 계산한다.
    @GET("v1/challenges/explore")
    suspend fun explore(
        @Query("category") category: String? = null,
        @Query("participationType") participationType: String? = null,
        @Query("verificationType") verificationType: String? = null,
        @Query("joinableOnly") joinableOnly: Boolean? = null,
        @Query("sort") sort: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("size") size: Int? = null,
    ): BaseResponse<ExploreChallengesResponse>
}
