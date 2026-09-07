package com.ruleup.profile.domain.repository

import com.ruleup.profile.domain.entity.ActivityCalendar
import com.ruleup.profile.domain.entity.CalendarDayDetail
import com.ruleup.profile.domain.entity.FriendInvitation
import com.ruleup.profile.domain.entity.GroupChallengeSummary
import com.ruleup.profile.domain.entity.MyHome
import com.ruleup.profile.domain.entity.MyTier
import com.ruleup.profile.domain.entity.StatsReport
import com.ruleup.profile.domain.entity.TierHistory

/**
 * 마이 탭 조회 계층 (마이프로필·캘린더 스펙).
 * 인증 파이프라인이 쌓은 데이터를 읽기 전용으로 조립한 응답을 받는다 — 새 판정 로직 없음.
 */
interface MyPageRepository {
    suspend fun getHome(): MyHome

    /** 참여 중(ACTIVE)인 그룹 챌린지만 — 그룹 랭킹 진입용. */
    suspend fun getMyGroupChallenges(): List<GroupChallengeSummary>

    /** 내 티어 상세 (명세: GET /me/tier). 구 매너 온도를 대체한다. */
    suspend fun getTier(): MyTier

    /**
     * 티어 히스토리 (명세: GET /me/tier/history). [months] 는 1~12.
     *
     * 기본값을 여기 두는 이유는 보관 기간이 1년이라 12가 곧 전량이기 때문이다 — 호출부가
     * 매번 12를 적어 넣게 하면 보관 기간이 바뀔 때 고칠 자리가 흩어진다.
     */
    suspend fun getTierHistory(months: Int = MAX_HISTORY_MONTHS): TierHistory

    /** [month] = YYYY-MM. */
    suspend fun getCalendar(month: String): ActivityCalendar

    /** [date] = YYYY-MM-DD. */
    suspend fun getCalendarDay(date: String): CalendarDayDetail

    suspend fun getStats(): StatsReport

    /** 코드/링크가 없으면 서버가 생성 후 반환한다 (멱등). */
    suspend fun getInvitation(): FriendInvitation

    companion object {
        /** 티어 이력 보관 기간(개월). 서버가 이보다 오래된 이력을 삭제한다. */
        const val MAX_HISTORY_MONTHS = 12
    }
}
