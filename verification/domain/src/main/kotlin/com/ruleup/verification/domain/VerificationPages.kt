package com.ruleup.verification.domain

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 내 챌린지 진행률 일괄 페이지(명세 §6.1). */
object VerificationProgressPage : Page {
    const val PATH = AppRoutes.VERIFICATION_PROGRESS

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

/** 수동 인증 제출 페이지(명세 §6.5, VF-04). 자동 판정 불가 방식의 당일 제출. */
data class VerificationManualPage(
    val challengeId: String,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_CHALLENGE_ID to challengeId))

    companion object {
        const val PATH = AppRoutes.VERIFICATION_MANUAL
        const val ARG_CHALLENGE_ID = "challengeId"
    }
}

/**
 * 챌린지 인증 결과/실패 상세 페이지(명세 §6.2). 홈 카드에서 challengeId 와 함께 진입한다.
 * 다른 feature 는 [AppRoutes.VERIFICATION_DETAIL] 로 진입한다.
 */
data class VerificationDetailPage(
    val challengeId: String,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_CHALLENGE_ID to challengeId))

    companion object {
        const val PATH = AppRoutes.VERIFICATION_DETAIL
        const val ARG_CHALLENGE_ID = "challengeId"
    }
}

/**
 * 지도 핀(좌표 바인딩) 페이지(명세 §5). 챌린지 생성/참여 시 GPS 루틴이면 진입한다.
 */
data class VerificationLocationPage(
    val challengeMemberId: String,
    val defaultRadiusM: Float,
    val dwellMinutes: Int,
) : Page {
    override fun toRoute(): NavRoute =
        NavRoute(
            PATH,
            mapOf(
                ARG_MEMBER_ID to challengeMemberId,
                ARG_RADIUS to defaultRadiusM.toString(),
                ARG_DWELL to dwellMinutes.toString(),
            ),
        )

    companion object {
        const val PATH = AppRoutes.VERIFICATION_LOCATION
        const val ARG_MEMBER_ID = "challengeMemberId"
        const val ARG_RADIUS = "defaultRadiusM"
        const val ARG_DWELL = "dwellMinutes"
    }
}
