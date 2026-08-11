package com.ruleup.challenge.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/**
 * 초안 생성 폴백 페이지 (Figma `1134:737`).
 *
 * `POST /challenges/draft` 가 `result=FALLBACK` 을 준 경우다 — **에러가 아니라 정상 분기**라,
 * 실패 화면이 아니라 "다음에 뭘 할지" 를 고르는 화면이다.
 */
data object ChallengeDraftFallbackPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.CHALLENGE_DRAFT_FALLBACK
}
