package com.ruleup.challenge.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 그룹 랭킹 페이지. 방 홈(그룹 챌린지 상세)의 랭킹 섹션으로 진입한다. */
data class ChallengeRankingPage(
    val challengeId: String,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_CHALLENGE_ID to challengeId))

    companion object {
        const val PATH = AppRoutes.CHALLENGE_RANKING
        const val ARG_CHALLENGE_ID = "challengeId"
    }
}
