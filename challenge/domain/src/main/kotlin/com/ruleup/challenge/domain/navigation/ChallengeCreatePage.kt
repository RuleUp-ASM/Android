package com.ruleup.challenge.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 챌린지 생성 플로우 페이지. */
object ChallengeCreatePage : Page {
    const val PATH = AppRoutes.CHALLENGE_CREATE

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

object ChallengeConfirmPage : Page {
    const val PATH = AppRoutes.CHALLENGE_CONFIRM

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
