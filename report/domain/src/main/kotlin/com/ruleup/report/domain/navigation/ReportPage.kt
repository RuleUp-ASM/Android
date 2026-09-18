package com.ruleup.report.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/**
 * 신고하기 (명세 POST /reports).
 *
 * 대상이 유저·챌린지 둘이고 사유 목록이 갈리므로 어느 쪽인지를 인자로 받는다.
 * [challengeId] 는 유저 신고에도 필요하다 — 서버가 신고 시점 방 정보를 스냅샷에 담는다.
 *
 * @param userId 유저 신고일 때만 채운다.
 * @param challengeId 챌린지 신고면 대상, 유저 신고면 신고가 일어난 방.
 * @param targetName 화면 상단 대상 카드에 쓸 이름. 조회를 한 번 더 하지 않으려고 넘겨받는다.
 */
data class ReportPage(
    val userId: String? = null,
    val challengeId: String? = null,
    val targetName: String,
) : Page {
    override fun toRoute(): NavRoute =
        NavRoute(
            PATH,
            buildMap {
                userId?.let { put(ARG_USER_ID, it) }
                challengeId?.let { put(ARG_CHALLENGE_ID, it) }
                put(ARG_TARGET_NAME, targetName)
            },
        )

    companion object {
        const val PATH = AppRoutes.REPORT
        const val ARG_USER_ID = "userId"
        const val ARG_CHALLENGE_ID = "challengeId"
        const val ARG_TARGET_NAME = "targetName"
    }
}
