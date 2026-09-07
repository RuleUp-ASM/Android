package com.ruleup.challenge.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/**
 * 감시자 초대 수락 페이지. 카카오톡으로 받은 `/w/{token}` 링크가 여기로 온다.
 *
 * **로그인이 필요하다** — 수락이 곧 동의이고 서버가 토큰과 로그인 상태를 함께 확인한다.
 * 미로그인 상태로 들어오면 보류됐다가 자동 로그인 뒤에 열린다(PendingDeepLink).
 */
data class WatcherAcceptPage(
    val token: String,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_TOKEN to token))

    companion object {
        const val PATH = AppRoutes.CHALLENGE_WATCHER_ACCEPT
        const val ARG_TOKEN = "token"
    }
}
