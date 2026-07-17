package com.ruleup.challenge.data.api

import com.ruleup.challenge.data.dto.ChallengeCategoriesResponse
import com.ruleup.challenge.data.dto.ChallengeDetailResponse
import com.ruleup.challenge.data.dto.ChallengeImageResponse
import com.ruleup.challenge.data.dto.ChallengeMembersResponse
import com.ruleup.challenge.data.dto.ChallengeResponse
import com.ruleup.challenge.data.dto.ChallengeSetupInfoResponse
import com.ruleup.challenge.data.dto.CreateChallengeRequest
import com.ruleup.challenge.data.dto.CreateNoticeRequest
import com.ruleup.challenge.data.dto.CreateNoticeResponse
import com.ruleup.challenge.data.dto.ExploreChallengesResponse
import com.ruleup.challenge.data.dto.MemberDecisionRequest
import com.ruleup.challenge.data.dto.MemberStatusResponse
import com.ruleup.challenge.data.dto.MyChallengesResponse
import com.ruleup.challenge.data.dto.NoticeDetailResponse
import com.ruleup.challenge.data.dto.NoticesResponse
import com.ruleup.challenge.data.dto.PinNoticeRequest
import com.ruleup.challenge.data.dto.PinNoticeResponse
import com.ruleup.challenge.data.dto.RankingResponse
import com.ruleup.challenge.data.dto.RecommendationRequest
import com.ruleup.challenge.data.dto.RecommendationResponse
import com.ruleup.challenge.data.dto.RoomResponse
import com.ruleup.challenge.data.dto.TrendingChallengesResponse
import com.ruleup.challenge.data.dto.UpdateChallengeRequest
import com.ruleup.challenge.data.dto.UpdateNoticeRequest
import com.ruleup.challenge.data.dto.UpdateNoticeResponse
import com.ruleup.challenge.data.dto.WatcherInvitationInfoResponse
import com.ruleup.challenge.data.dto.WatcherInvitationResponse
import com.ruleup.challenge.data.dto.WatchersResponse
import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.EmptyData
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
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

    // 감시자: 초대 생성 (토큰 7일 만료, 무료 3명 초과 시 에러)
    @POST("v1/challenges/{challengeId}/watchers/invitations")
    suspend fun createWatcherInvitation(
        @Path("challengeId") challengeId: String,
    ): BaseResponse<WatcherInvitationResponse>

    // 감시자: 내 감시자 목록 조회 (참여자 본인 기준)
    @GET("v1/challenges/{challengeId}/watchers")
    suspend fun getWatchers(
        @Path("challengeId") challengeId: String,
        @Query("status") status: String? = null,
    ): BaseResponse<WatchersResponse>

    // 감시자: 해제 (REVOKED + 연락처 파기)
    @DELETE("v1/challenges/{challengeId}/watchers/{watcherId}")
    suspend fun removeWatcher(
        @Path("challengeId") challengeId: String,
        @Path("watcherId") watcherId: String,
    ): BaseResponse<EmptyData>

    // 감시자: 초대 링크 진입 (토큰 검증 + 초대 정보)
    @GET("v1/watchers/invitations/{token}")
    suspend fun getWatcherInvitation(
        @Path("token") token: String,
    ): BaseResponse<WatcherInvitationInfoResponse>

    // 감시자: 인앱 수락 (= 수신동의, 채널 = 인앱 푸시)
    @POST("v1/watchers/invitations/{token}/accept")
    suspend fun acceptWatcherInvitation(
        @Path("token") token: String,
    ): BaseResponse<EmptyData>

    // 방 홈 일괄 조회 (ACTIVE 멤버 전용 — 비멤버 403 NOT_A_MEMBER)
    @GET("v1/challenges/{challengeId}/room")
    suspend fun getRoom(
        @Path("challengeId") challengeId: String,
    ): BaseResponse<RoomResponse>

    // 공지: 목록 조회 (고정 우선 → 최신순, 서버 고정 최근 10건)
    @GET("v1/challenges/{challengeId}/notices")
    suspend fun getNotices(
        @Path("challengeId") challengeId: String,
    ): BaseResponse<NoticesResponse>

    // 공지: 상세 조회 (서버가 조회 시점에 읽음 upsert — 별도 읽음 API 없음)
    @GET("v1/challenges/{challengeId}/notices/{noticeId}")
    suspend fun getNotice(
        @Path("challengeId") challengeId: String,
        @Path("noticeId") noticeId: String,
    ): BaseResponse<NoticeDetailResponse>

    // 공지: 작성 (방장 전용 — 저장 후 ACTIVE 멤버 푸시 fan-out)
    @POST("v1/challenges/{challengeId}/notices")
    suspend fun createNotice(
        @Path("challengeId") challengeId: String,
        @Body request: CreateNoticeRequest,
    ): BaseResponse<CreateNoticeResponse>

    // 공지: 수정 (방장 전용 — resetRead=true 면 읽음 초기화 + 재발송)
    @PUT("v1/challenges/{challengeId}/notices/{noticeId}")
    suspend fun updateNotice(
        @Path("challengeId") challengeId: String,
        @Path("noticeId") noticeId: String,
        @Body request: UpdateNoticeRequest,
    ): BaseResponse<UpdateNoticeResponse>

    // 공지: 삭제 (방장 전용 — 서버는 소프트 삭제)
    @DELETE("v1/challenges/{challengeId}/notices/{noticeId}")
    suspend fun deleteNotice(
        @Path("challengeId") challengeId: String,
        @Path("noticeId") noticeId: String,
    ): BaseResponse<EmptyData>

    // 공지: 고정/해제 (방장 전용 — 단일 pin, 기존 고정 자동 해제)
    @PATCH("v1/challenges/{challengeId}/notices/{noticeId}/pin")
    suspend fun pinNotice(
        @Path("challengeId") challengeId: String,
        @Path("noticeId") noticeId: String,
        @Body request: PinNoticeRequest,
    ): BaseResponse<PinNoticeResponse>

    // 그룹 랭킹 조회 (비정규화 진행률 정렬 — 상위 3 + 전체 + 내 순위)
    @GET("v1/challenges/{challengeId}/ranking")
    suspend fun getRanking(
        @Path("challengeId") challengeId: String,
    ): BaseResponse<RankingResponse>
}
