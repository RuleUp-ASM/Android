package com.ruleup.challenge.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 내 챌린지 목록 페이지 (하단 「챌린지」 탭 → 진행 중 / 완료·이탈). */
data object MyChallengesPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.CHALLENGE_LIST
}
