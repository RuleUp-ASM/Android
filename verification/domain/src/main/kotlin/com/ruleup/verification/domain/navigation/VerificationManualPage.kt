package com.ruleup.verification.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 수동 인증 제출 페이지. */
data class VerificationManualPage(
    val challengeId: String,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_CHALLENGE_ID to challengeId))

    companion object {
        const val PATH = AppRoutes.VERIFICATION_MANUAL
        const val ARG_CHALLENGE_ID = "challengeId"
    }
}
