package com.ruleup.observability.domain.model

/**
 * TTI 측정 단위가 되는 화면. 관측 모듈이 화면을 알 필요가 없도록 feature 의 presentation 이
 * 자기 페이지를 선언한다(예: `ChallengeDetailTtiPage`).
 */
interface TtiPage {
    /** 같은 화면은 항상 같은 값을 반환해야 한다. */
    val pageName: String

    /** **선언이자 기대치**다 — 하나라도 기록되지 않은 채 화면이 끝나면 [TtiOutcome.ABANDONED] 로 판정된다. */
    val timelines: List<TtiTimeline>
}
