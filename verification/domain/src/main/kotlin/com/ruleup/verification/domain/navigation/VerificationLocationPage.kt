package com.ruleup.verification.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/**
 * 지도 핀(좌표 바인딩) 페이지(명세 §5). 챌린지 생성/참여 시 GPS 루틴이면 진입한다.
 */
data class VerificationLocationPage(
    val challengeId: String,
    val defaultRadiusM: Float,
    val dwellMinutes: Int,
    // 셋업 제출 시 앵커와 함께 보낼 대상 앱 패키지(로컬 등록분). 없으면 빈 리스트.
    val targetPackages: List<String> = emptyList(),
) : Page {
    override fun toRoute(): NavRoute =
        NavRoute(
            PATH,
            mapOf(
                ARG_CHALLENGE_ID to challengeId,
                ARG_RADIUS to defaultRadiusM.toString(),
                ARG_DWELL to dwellMinutes.toString(),
                ARG_TARGET_PACKAGES to targetPackages.joinToString(TARGET_PACKAGES_DELIMITER),
            ),
        )

    companion object {
        const val PATH = AppRoutes.VERIFICATION_LOCATION
        const val ARG_CHALLENGE_ID = "challengeId"
        const val ARG_RADIUS = "defaultRadiusM"
        const val ARG_DWELL = "dwellMinutes"
        const val ARG_TARGET_PACKAGES = "targetPackages"

        const val TARGET_PACKAGES_DELIMITER = ","
    }
}

/** 권한 재연결 페이지 (권한 회수 감지 → 신호별 연결 상태·재허용). */
data object VerificationPermissionRepairPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.VERIFICATION_PERMISSION_REPAIR
}
