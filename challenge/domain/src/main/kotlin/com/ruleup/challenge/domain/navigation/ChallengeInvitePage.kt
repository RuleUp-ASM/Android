package com.ruleup.challenge.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/**
 * 멤버 초대 링크 진입 페이지. 카카오톡으로 받은 `/c/{token}` 링크가 여기로 온다.
 *
 * **로그인이 필요하다** — 멤버가 되려면 룰업 유저여야 하고, 미리보기 API 자체가 로그인 필수다.
 * 미로그인 진입은 보류됐다가 자동 로그인 뒤 열린다(PendingDeepLink).
 */
data class ChallengeInvitePage(
    val token: String,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_TOKEN to token))

    companion object {
        const val PATH = AppRoutes.CHALLENGE_INVITE
        const val ARG_TOKEN = "token"
    }
}
