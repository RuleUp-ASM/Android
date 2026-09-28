package com.ruleup.profile.domain.repository

import com.ruleup.profile.domain.entity.ActivityCalendar
import com.ruleup.profile.domain.entity.CalendarDayDetail
import com.ruleup.profile.domain.entity.FriendInvitation
import com.ruleup.profile.domain.entity.GroupChallengeSummary
import com.ruleup.profile.domain.entity.MyHome
import com.ruleup.profile.domain.entity.MyTier
import com.ruleup.profile.domain.entity.ScoreChangePage
import com.ruleup.profile.domain.entity.StatsReport
import com.ruleup.profile.domain.entity.TierHistory

/** 마이 탭 조회 계층. */
interface MyPageRepository {
    suspend fun getHome(): MyHome

    /** 참여 중(ACTIVE)인 그룹 챌린지만 */
    suspend fun getMyGroupChallenges(): List<GroupChallengeSummary>

    /** 내 티어 상세. */
    suspend fun getTier(): MyTier

    /** 티어 히스토리. */
    suspend fun getTierHistory(months: Int = MAX_HISTORY_MONTHS): TierHistory

    /** 점수 변동 이력. */
    suspend fun getScoreChanges(cursor: String? = null): ScoreChangePage

    /** [month] = YYYY-MM. */
    suspend fun getCalendar(month: String): ActivityCalendar

    /** [date] = YYYY-MM-DD. */
    suspend fun getCalendarDay(date: String): CalendarDayDetail

    suspend fun getStats(): StatsReport

    /** 코드/링크가 없으면 서버가 생성 후 반환한다 (멱등). */
    suspend fun getInvitation(): FriendInvitation

    companion object {
        /** 티어 이력 보관 기간(개월). */
        const val MAX_HISTORY_MONTHS = 12
    }
}
