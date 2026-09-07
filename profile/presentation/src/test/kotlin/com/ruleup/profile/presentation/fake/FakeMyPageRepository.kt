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

/**
 * 테스트용 [MyPageRepository]. 검증 대상 메서드만 답을 돌려주고 나머지는 호출되면 실패한다 —
 * ViewModel 이 의도치 않은 조회를 해도 조용히 지나가지 않게 하려는 것이다.
 *
 * 답은 **호출마다 계산**하므로(`() -> T`) 재시도·재조회에서 중간에 결과를 바꿔 끼울 수 있다.
 */
class FakeMyPageRepository(
    private val home: (() -> MyHome)? = null,
    private val tier: (() -> MyTier)? = null,
    private val tierHistory: (() -> TierHistory)? = null,
    private val scoreChanges: ((String?) -> ScoreChangePage)? = null,
    private val calendar: ((String) -> ActivityCalendar)? = null,
    private val calendarDay: ((String) -> CalendarDayDetail)? = null,
    private val stats: (() -> StatsReport)? = null,
    private val invitation: (() -> FriendInvitation)? = null,
    private val groupChallenges: (() -> List<GroupChallengeSummary>)? = null,
) : com.ruleup.profile.domain.repository.MyPageRepository {
    /** 어떤 인자로 몇 번 불렸는지. "안 불렀다"도 계약이라 호출 자체를 남긴다. */
    val calls = mutableListOf<String>()

    val historyMonths = mutableListOf<Int>()

    /** 어떤 커서로 이력을 물었는지. 첫 페이지는 null 이다. */
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
