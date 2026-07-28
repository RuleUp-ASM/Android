package com.ruleup.observability.domain.model

/**
 * TTI 측정 단위가 되는 화면.
 *
 * **어떤 화면이 존재하고 각각 무엇을 재는지는 관측 모듈이 알 필요가 없다.** feature 의
 * presentation 모듈이 자기 페이지를 선언한다 — `BusinessPayload.Custom` 팩토리와 같은 이유로,
 * 도메인 지식이 횡단 모듈로 새지 않게 한다.
 *
 * ```kotlin
 * object ChallengeDetailTtiPage : TtiPage {
 *     override val pageName = "challenge_detail"
 *     override val timelines = listOf(TtiTimeline.API_RESPONSE, TtiTimeline.VIEW_BINDING)
 * }
 * ```
 *
 * [timelines] 는 **선언이자 기대치**다. 여기 적힌 단계가 하나라도 기록되지 않은 채 화면이 끝나면
 * [TtiOutcome.ABANDONED] 로 판정된다 — 호출부가 "이탈했다"고 알려줄 필요가 없다.
 *
 * 같은 화면은 항상 같은 [pageName] 을 반환해야 한다.
 */
interface TtiPage {
    val pageName: String
    val timelines: List<TtiTimeline>
}
