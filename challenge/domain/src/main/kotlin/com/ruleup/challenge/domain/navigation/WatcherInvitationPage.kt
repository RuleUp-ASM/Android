package com.ruleup.challenge.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/**
 * 감시자 초대 수락 페이지. 카카오톡 초대 카드의 링크(딥링크)로 token 과 함께 진입한다.
 * 룰업 유저의 인앱 수락 = 수신동의(감시자 통지 스펙 §1).
 */
data class WatcherInvitationPage(
    val token: String,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_TOKEN to token))

    companion object {
        const val PATH = AppRoutes.WATCHER_INVITATION
        const val ARG_TOKEN = "token"
    }
}
