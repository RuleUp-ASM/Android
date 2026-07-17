package com.ruleup.challenge.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/**
 * 공지 목록 페이지. 방 홈(그룹 챌린지 상세)의 공지 메뉴로 진입한다.
 * [canManage] 는 방 홈이 받은 myRole 기반 — 작성 버튼 노출용이며 실제 권한 판정은 서버가 한다.
 */
data class ChallengeNoticesPage(
    val challengeId: String,
    val canManage: Boolean,
) : Page {
    override fun toRoute(): NavRoute =
        NavRoute(
            PATH,
            mapOf(
                ARG_CHALLENGE_ID to challengeId,
                ARG_CAN_MANAGE to canManage.toString(),
            ),
        )

    companion object {
        const val PATH = AppRoutes.CHALLENGE_NOTICES
        const val ARG_CHALLENGE_ID = "challengeId"
        const val ARG_CAN_MANAGE = "canManage"
    }
}
