package com.ruleup.challenge.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 챌린지 수정 페이지(방장 전용). */
data class ChallengeSettingsPage(
    val challengeId: String,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_CHALLENGE_ID to challengeId))

    companion object {
        const val PATH = AppRoutes.CHALLENGE_SETTINGS
        const val ARG_CHALLENGE_ID = "challengeId"
    }
}
