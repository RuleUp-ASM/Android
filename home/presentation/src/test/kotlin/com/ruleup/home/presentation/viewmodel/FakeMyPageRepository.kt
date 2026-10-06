package com.ruleup.home.presentation.viewmodel

import com.ruleup.profile.domain.entity.ActivityCalendar
import com.ruleup.profile.domain.entity.CalendarDayDetail
import com.ruleup.profile.domain.entity.FriendInvitation
import com.ruleup.profile.domain.entity.GroupChallengeSummary
import com.ruleup.profile.domain.entity.MyHome
import com.ruleup.profile.domain.entity.MyTier
import com.ruleup.profile.domain.entity.ScoreChangePage
import com.ruleup.profile.domain.entity.StatsReport
import com.ruleup.profile.domain.entity.TierHistory
import com.ruleup.profile.domain.repository.MyPageRepository

/** 홈은 이번 주 캘린더만 읽는다. */
internal class FakeMyPageRepository(
    private val day: (date: String) -> CalendarDayDetail = { CalendarDayDetail(date = it, items = emptyList()) },
    private val calendar: (month: String) -> ActivityCalendar = { ActivityCalendar(month = it, days = emptyList()) },
) : MyPageRepository {
    val requestedMonths = mutableListOf<String>()

    override suspend fun getCalendar(month: String): ActivityCalendar {
        requestedMonths += month
        return calendar(month)
    }

    override suspend fun getHome(): MyHome = error("홈에서 쓰지 않는다")

    override suspend fun getMyGroupChallenges(): List<GroupChallengeSummary> = error("홈에서 쓰지 않는다")

    override suspend fun getTier(): MyTier = error("홈에서 쓰지 않는다")

    override suspend fun getTierHistory(months: Int): TierHistory = error("홈에서 쓰지 않는다")

    override suspend fun getScoreChanges(cursor: String?): ScoreChangePage = error("홈에서 쓰지 않는다")

    override suspend fun getCalendarDay(date: String): CalendarDayDetail = day(date)

    override suspend fun getStats(): StatsReport = error("홈에서 쓰지 않는다")

    override suspend fun getInvitation(): FriendInvitation = error("홈에서 쓰지 않는다")
}
