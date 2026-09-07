package com.ruleup.profile.data.api

import com.ruleup.network.dto.BaseResponse
import com.ruleup.profile.data.dto.ActivityCalendarResponse
import com.ruleup.profile.data.dto.CalendarDayDetailResponse
import com.ruleup.profile.data.dto.FriendInvitationResponse
import com.ruleup.profile.data.dto.MyChallengesSliceResponse
import com.ruleup.profile.data.dto.MyHomeResponse
import com.ruleup.profile.data.dto.MyTierResponse
import com.ruleup.profile.data.dto.StatsResponse
import com.ruleup.profile.data.dto.TierHistoryResponse
import retrofit2.http.GET
import retrofit2.http.Path
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

    // 내 티어 상세 (변동 로그는 서버 고정 최근 10건)
    @GET("v1/me/tier")
    suspend fun getTier(): BaseResponse<MyTierResponse>

    // 티어 히스토리 그래프 원천 — months 는 1~12, 생략 시 서버 기본 12(보관 자체가 1년)
    @GET("v1/me/tier/history")
    suspend fun getTierHistory(
        @Query("months") months: Int? = null,
    ): BaseResponse<TierHistoryResponse>

    // 활동 캘린더 월 조회 (판정 대상일만 내려옴)
    @GET("v1/me/calendar")
    suspend fun getCalendar(
        @Query("month") month: String,
    ): BaseResponse<ActivityCalendarResponse>

    // 캘린더 일자 상세 (VerificationDaily 조회)
    @GET("v1/me/calendar/{date}")
    suspend fun getCalendarDay(
        @Path("date") date: String,
    ): BaseResponse<CalendarDayDetailResponse>

    // 통계 리포트 — 정책이 지표 5종을 고정해 기간 파라미터가 없다(구 period 폐기)
    @GET("v1/me/stats")
    suspend fun getStats(): BaseResponse<StatsResponse>

    // 친구 초대 정보 (유저당 1개 — 없으면 서버가 생성 후 반환, 멱등)
    @GET("v1/me/invitation")
    suspend fun getInvitation(): BaseResponse<FriendInvitationResponse>
}
