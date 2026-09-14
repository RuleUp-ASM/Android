package com.ruleup.verification.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/**
 * 수동 인증 제출 페이지 (Figma `1443:2`).
 *
 * **수동 방에서만 연다.** 자동 방의 실패 구제는 이의 제기가 담당하고, 자동 방에 대고 제출하면
 * 서버가 `NOT_MANUAL_CHALLENGE`(409)로 막는다 — 화면이 그 진입점을 두지 않는 것이 전제다.
 *
 * 솔로·그룹 상세가 같은 경로로 들어온다. 체크 CTA 가 방 정보 탭 안에만 있던 동안 솔로는 방 홈을
 * 받지 않아 수동 인증을 할 방법이 아예 없었다.
 */
data class VerificationManualPage(
    val challengeId: String,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_CHALLENGE_ID to challengeId))

    companion object {
        const val PATH = AppRoutes.VERIFICATION_MANUAL
        const val ARG_CHALLENGE_ID = "challengeId"
    }
}
