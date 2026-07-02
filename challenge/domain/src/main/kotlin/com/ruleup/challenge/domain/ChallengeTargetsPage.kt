package com.ruleup.challenge.domain

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/**
 * 대상 앱 등록 페이지. 상세 화면의 "앱 등록하기" 버튼으로 challengeId 와 함께 진입한다.
 * 사용자가 고른 앱을 [TargetAppStore] 에 저장해 등록 완료로 표시한다.
 */
data class ChallengeTargetsPage(
    val challengeId: String,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_CHALLENGE_ID to challengeId))

    companion object {
        const val PATH = AppRoutes.CHALLENGE_TARGETS
        const val ARG_CHALLENGE_ID = "challengeId"
    }
}
