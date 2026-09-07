package com.ruleup.challenge.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/**
 * 챌린지 수정 페이지(방장 전용). 방 홈의 설정에서 진입한다.
 *
 * 수정 가능 범위는 서버가 `editableFields` 로 계산해 주므로 진입 시 설정을 다시 조회한다 —
 * 상세에서 들고 온 값으로 폼을 열면 그 사이 가입·탈퇴로 바뀐 잠금 범위를 놓친다.
 */
data class ChallengeSettingsPage(
    val challengeId: String,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_CHALLENGE_ID to challengeId))

    companion object {
        const val PATH = AppRoutes.CHALLENGE_SETTINGS
        const val ARG_CHALLENGE_ID = "challengeId"
    }
}
