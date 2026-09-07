package com.ruleup.challenge.presentation.observability

import com.ruleup.observability.domain.model.TtiPage
import com.ruleup.observability.domain.model.TtiTimeline

/**
 * 챌린지 상세 화면의 TTI 정의.
 *
 * [TtiTimeline.API_RESPONSE] 는 상세 조회 왕복, [TtiTimeline.VIEW_BINDING] 은 응답을 화면 상태로
 * 반영하기까지다. 둘을 나눠야 **네트워크가 느린 건지 렌더가 느린 건지**가 갈린다.
 *
 * 여기 선언한 단계가 하나라도 안 채워지면 이탈로 집계된다 — 화면이 다 뜨기 전에 나간 경우다.
 */
object ChallengeDetailTtiPage : TtiPage {
    override val pageName = "challenge_detail"
    override val timelines =
        listOf(
            TtiTimeline.API_RESPONSE,
            TtiTimeline.VIEW_BINDING,
        )
}
