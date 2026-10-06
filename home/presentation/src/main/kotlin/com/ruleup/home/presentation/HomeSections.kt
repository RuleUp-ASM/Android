package com.ruleup.home.presentation

import com.ruleup.challenge.domain.entity.ChallengeLimits
import com.ruleup.challenge.domain.entity.TrendingChallenge
import com.ruleup.domain.entity.category.Category
import com.ruleup.verification.domain.entity.TodayStatus

/** 매일 해야 하는 루틴인가(주 7회). 횟수를 모르면 매일로 본다 — 주 N회로 접으면 「오늘」 목록에서 빠져 놓친다. */
val HomeChallengeUi.isDaily: Boolean
    get() = weeklyCount == null || weeklyCount >= ChallengeLimits.WEEKLY_COUNT_MAX

/**
 * 「오늘 해 볼까요?」 후보. 오늘 대상인데 아직 안 끝난 매일 루틴이 먼저, 그다음 주 N회 루틴.
 * 인증 방식은 여기서 모른다 — [pickHero] 가 직접 체크인 것만 고른다.
 */
fun heroCandidates(cards: List<HomeChallengeUi>): List<HomeChallengeUi> {
    val open = cards.filter { it.active && it.todayStatus !in CLOSED_TODAY }
    val daily = open.filter { it.isDaily && it.todayTarget }
    // 주 N회는 정해진 요일이 없으니 NOT_TARGET 을 「이번 주 다 채움」으로 가정한다.
    val weekly = open.filter { !it.isDaily && it.todayStatus != TodayStatus.NOT_TARGET }
    return daily + weekly
}

/** 후보 중 직접 체크하는 첫 챌린지. 자동 인증은 사용자가 할 게 없어 올리지 않는다. */
fun pickHero(
    cards: List<HomeChallengeUi>,
    manualCheckable: Map<String, Boolean>,
): HomeChallengeUi? = heroCandidates(cards).firstOrNull { manualCheckable[it.challengeId] == true }

/** 상단 요약. 「오늘 할 일」에는 매일 루틴만 센다 — 주 N회는 오늘 안 해도 되는 날이 있다. */
data class HomeSummary(
    val dailyDone: Int,
    val dailyTotal: Int,
    val weeklyTotal: Int,
)

fun homeSummary(cards: List<HomeChallengeUi>): HomeSummary {
    val active = cards.filter { it.active }
    val dailyTargets = active.filter { it.isDaily && it.todayTarget }
    return HomeSummary(
        dailyDone = dailyTargets.count { it.todayStatus == TodayStatus.DONE },
        dailyTotal = dailyTargets.size,
        weeklyTotal = active.count { !it.isDaily },
    )
}

/** 시간대 인사(서비스 기준 시각). */
fun greeting(hour: Int): String =
    when (hour) {
        in 5..10 -> "좋은 아침이에요"
        in 11..16 -> "좋은 오후예요"
        in 17..21 -> "좋은 저녁이에요"
        else -> "편안한 밤이에요"
    }

/** 신규 이용자에게 보여 줄 첫 챌린지. 실시간 인기 순서는 지키되 관심 분야 것을 앞에 세운다. */
fun pickStarters(
    trending: List<TrendingChallenge>,
    interests: List<Category>,
    limit: Int = STARTER_LIMIT,
): List<TrendingChallenge> {
    val (interested, others) = trending.partition { it.category != null && it.category in interests }
    return (interested + others).take(limit)
}

private const val STARTER_LIMIT = 6

private val CLOSED_TODAY = setOf(TodayStatus.DONE, TodayStatus.FAILED)
