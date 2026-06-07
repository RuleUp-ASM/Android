package com.ruleup.challenge.domain

import com.ruleup.domain.navigation.AppEntryRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/**
 * 챌린지 생성 플로우 페이지.
 * 단계 순서: 입력(제목·설명) → AI 추천 확인(수정·확정).
 * 다른 feature 는 [AppEntryRoutes.CHALLENGE_CREATE] 로 진입한다.
 */
object ChallengeCreatePage : Page {
    const val PATH = AppEntryRoutes.CHALLENGE_CREATE

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

object ChallengeConfirmPage : Page {
    const val PATH = "challenge/confirm"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
