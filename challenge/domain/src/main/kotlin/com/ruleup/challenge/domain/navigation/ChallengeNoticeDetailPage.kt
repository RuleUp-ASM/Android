package com.ruleup.challenge.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/**
 * 공지 상세 페이지. 목록 항목/고정 공지 배너로 진입하며, 서버가 조회 시점에 읽음 처리한다.
 * [canManage] 가 true 면 수정·삭제·고정 메뉴를 노출한다(실제 권한 판정은 서버).
 */
data class ChallengeNoticeDetailPage(
    val challengeId: String,
    val noticeId: String,
    val canManage: Boolean,
) : Page {
    override fun toRoute(): NavRoute =
        NavRoute(
            PATH,
            mapOf(
                ARG_CHALLENGE_ID to challengeId,
                ARG_NOTICE_ID to noticeId,
                ARG_CAN_MANAGE to canManage.toString(),
            ),
        )

    companion object {
        const val PATH = AppRoutes.CHALLENGE_NOTICE_DETAIL
        const val ARG_CHALLENGE_ID = "challengeId"
        const val ARG_NOTICE_ID = "noticeId"
        const val ARG_CAN_MANAGE = "canManage"
    }
}
