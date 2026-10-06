package com.ruleup.challenge.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 내 감시자 관리 페이지(Figma 1134:1603). 마이 허브 「감시자」에서 방을 고른 뒤 들어온다. */
data class ChallengeWatchersPage(
    val challengeId: String,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_CHALLENGE_ID to challengeId))

    companion object {
        const val PATH = AppRoutes.CHALLENGE_WATCHERS
        const val ARG_CHALLENGE_ID = "challengeId"
    }
}
