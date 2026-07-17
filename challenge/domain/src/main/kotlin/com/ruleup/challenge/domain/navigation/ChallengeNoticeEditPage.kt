package com.ruleup.challenge.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/**
 * 공지 작성/수정 페이지 (방장 전용 메뉴로만 진입).
 * [noticeId] 가 null 이면 작성, 있으면 해당 공지 수정 — 기존 내용은 화면이 직접 조회한다.
 */
data class ChallengeNoticeEditPage(
    val challengeId: String,
    val noticeId: String? = null,
) : Page {
    override fun toRoute(): NavRoute =
        NavRoute(
            PATH,
            buildMap {
                put(ARG_CHALLENGE_ID, challengeId)
                noticeId?.let { put(ARG_NOTICE_ID, it) }
            },
        )

    companion object {
        const val PATH = AppRoutes.CHALLENGE_NOTICE_EDIT
        const val ARG_CHALLENGE_ID = "challengeId"
        const val ARG_NOTICE_ID = "noticeId"
    }
}
