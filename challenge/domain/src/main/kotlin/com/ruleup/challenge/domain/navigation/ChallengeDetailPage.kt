package com.ruleup.challenge.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/**
 * 챌린지 상세/참여 페이지(명세 3.3). 홈·탐색 카드에서 challengeId 와 함께 진입한다.
 * 자동 인증 챌린지면 "참여하기" 시 권한을 확인하고, 미허용이면 권한 허용 모달을 띄운다.
 */
data class ChallengeDetailPage(
    val challengeId: String,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_CHALLENGE_ID to challengeId))

    companion object {
        const val PATH = AppRoutes.CHALLENGE_DETAIL
        const val ARG_CHALLENGE_ID = "challengeId"
    }
}
