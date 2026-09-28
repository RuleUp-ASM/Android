package com.ruleup.challenge.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 멤버 초대 링크 진입 페이지. */
data class ChallengeInvitePage(
    val token: String,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_TOKEN to token))

    companion object {
        const val PATH = AppRoutes.CHALLENGE_INVITE
        const val ARG_TOKEN = "token"
    }
}
