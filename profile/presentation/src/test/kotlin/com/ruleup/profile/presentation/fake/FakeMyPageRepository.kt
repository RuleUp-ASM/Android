package com.ruleup.profile.presentation.fake

import com.ruleup.profile.domain.entity.ActivityCalendar
import com.ruleup.profile.domain.entity.CalendarDayDetail
import com.ruleup.profile.domain.entity.FriendInvitation
import com.ruleup.profile.domain.entity.GroupChallengeSummary
import com.ruleup.profile.domain.entity.MyHome
import com.ruleup.profile.domain.entity.MyTier
import com.ruleup.profile.domain.entity.ScoreChangePage
import com.ruleup.profile.domain.entity.StatsReport
import com.ruleup.profile.domain.entity.TierHistory

/** 테스트용 [MyPageRepository]. */
class FakeMyPageRepository(
    private val home: (() -> MyHome)? = null,
    private val tier: (() -> MyTier)? = null,
    private val tierHistory: (() -> TierHistory)? = null,
    private val scoreChanges: ((String?) -> ScoreChangePage)? = null,
    private val calendar: (suspend (String) -> ActivityCalendar)? = null,
    private val calendarDay: ((String) -> CalendarDayDetail)? = null,
    private val stats: (() -> StatsReport)? = null,
    private val invitation: (() -> FriendInvitation)? = null,
    private val groupChallenges: (() -> List<GroupChallengeSummary>)? = null,
) : com.ruleup.profile.domain.repository.MyPageRepository {
    /** 어떤 인자로 몇 번 불렸는지. */
    val calls = mutableListOf<String>()

    val historyMonths = mutableListOf<Int>()

    /** 어떤 커서로 이력을 물었는지. */
    val changeCursors = mutableListOf<String?>()
    val calendarMonths = mutableListOf<String>()

    override suspend fun getHome(): MyHome {
        calls += "getHome"
        return requireNotNull(home) { "getHome 을 준비하지 않았다" }()
    }

    override suspend fun getMyGroupChallenges(): List<GroupChallengeSummary> {
        calls += "getMyGroupChallenges"
        return requireNotNull(groupChallenges) { "getMyGroupChallenges 를 준비하지 않았다" }()
    }

    override suspend fun getTier(): MyTier {
        calls += "getTier"
        return requireNotNull(tier) { "getTier 를 준비하지 않았다" }()
    }

    override suspend fun getTierHistory(months: Int): TierHistory {
        calls += "getTierHistory"
        historyMonths += months
        return requireNotNull(tierHistory) { "getTierHistory 를 준비하지 않았다" }()
    }

    override suspend fun getScoreChanges(cursor: String?): ScoreChangePage {
        calls += "getScoreChanges"
        changeCursors += cursor
        return requireNotNull(scoreChanges) { "getScoreChanges 를 준비하지 않았다" }(cursor)
    }

    override suspend fun getCalendar(month: String): ActivityCalendar {
        calls += "getCalendar"
        calendarMonths += month
        return requireNotNull(calendar) { "getCalendar 를 준비하지 않았다" }(month)
    }

    override suspend fun getCalendarDay(date: String): CalendarDayDetail {
        calls += "getCalendarDay"
        return requireNotNull(calendarDay) { "getCalendarDay 를 준비하지 않았다" }(date)
    }

    override suspend fun getStats(): StatsReport {
        calls += "getStats"
        return requireNotNull(stats) { "getStats 를 준비하지 않았다" }()
    }

    override suspend fun getInvitation(): FriendInvitation {
        calls += "getInvitation"
        return requireNotNull(invitation) { "getInvitation 을 준비하지 않았다" }()
    }
}
