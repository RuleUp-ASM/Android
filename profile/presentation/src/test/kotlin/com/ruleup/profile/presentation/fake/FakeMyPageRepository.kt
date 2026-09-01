package com.ruleup.profile.presentation.fake

import com.ruleup.profile.domain.entity.ActivityCalendar
import com.ruleup.profile.domain.entity.CalendarDayDetail
import com.ruleup.profile.domain.entity.FriendInvitation
import com.ruleup.profile.domain.entity.GroupChallengeSummary
import com.ruleup.profile.domain.entity.MyHome
import com.ruleup.profile.domain.entity.ReputationDetail
import com.ruleup.profile.domain.entity.ReputationHistory
import com.ruleup.profile.domain.entity.StatsPeriod
import com.ruleup.profile.domain.entity.StatsReport

/**
 * 테스트용 [MyPageRepository]. 검증 대상 메서드만 답을 돌려주고 나머지는 호출되면 실패한다 —
 * ViewModel 이 의도치 않은 조회를 해도 조용히 지나가지 않게 하려는 것이다.
 *
 * 답은 **호출마다 계산**하므로(`() -> T`) 재시도·재조회에서 중간에 결과를 바꿔 끼울 수 있다.
 */
class FakeMyPageRepository(
    private val home: (() -> MyHome)? = null,
    private val reputation: (() -> ReputationDetail)? = null,
    private val reputationHistory: (() -> ReputationHistory)? = null,
    private val calendar: ((String) -> ActivityCalendar)? = null,
    private val calendarDay: ((String) -> CalendarDayDetail)? = null,
    private val stats: ((StatsPeriod) -> StatsReport)? = null,
    private val invitation: (() -> FriendInvitation)? = null,
    private val groupChallenges: (() -> List<GroupChallengeSummary>)? = null,
) : com.ruleup.profile.domain.repository.MyPageRepository {
    /** 어떤 인자로 몇 번 불렸는지. "안 불렀다"도 계약이라 호출 자체를 남긴다. */
    val calls = mutableListOf<String>()

    val statsPeriods = mutableListOf<StatsPeriod>()
    val calendarMonths = mutableListOf<String>()

    override suspend fun getHome(): MyHome {
        calls += "getHome"
        return requireNotNull(home) { "getHome 을 준비하지 않았다" }()
    }

    override suspend fun getMyGroupChallenges(): List<GroupChallengeSummary> {
        calls += "getMyGroupChallenges"
        return requireNotNull(groupChallenges) { "getMyGroupChallenges 를 준비하지 않았다" }()
    }

    override suspend fun getReputation(): ReputationDetail {
        calls += "getReputation"
        return requireNotNull(reputation) { "getReputation 을 준비하지 않았다" }()
    }

    override suspend fun getReputationHistory(): ReputationHistory {
        calls += "getReputationHistory"
        return requireNotNull(reputationHistory) { "getReputationHistory 를 준비하지 않았다" }()
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

    override suspend fun getStats(period: StatsPeriod): StatsReport {
        calls += "getStats"
        statsPeriods += period
        return requireNotNull(stats) { "getStats 를 준비하지 않았다" }(period)
    }

    override suspend fun getInvitation(): FriendInvitation {
        calls += "getInvitation"
        return requireNotNull(invitation) { "getInvitation 을 준비하지 않았다" }()
    }
}
