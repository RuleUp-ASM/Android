package com.ruleup.challenge.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 감시자 초대 수락 페이지. */
data class WatcherAcceptPage(
    val token: String,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_TOKEN to token))

    companion object {
        const val PATH = AppRoutes.CHALLENGE_WATCHER_ACCEPT
        const val ARG_TOKEN = "token"
    }
}
