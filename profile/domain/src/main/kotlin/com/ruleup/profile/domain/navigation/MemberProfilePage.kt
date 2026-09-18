package com.ruleup.profile.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/**
 * 타인 프로필 (명세 GET /users/{userId}/profile).
 *
 * 마이 홈과 화면을 나눈 이유는 공개 범위다 — 여기서는 닉네임·표시 티어·완주 개수만 보이고
 * 진행 중 목록·통계·점수는 오지 않는다.
 */
data class MemberProfilePage(
    val userId: String,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_USER_ID to userId))

    companion object {
        const val PATH = AppRoutes.MEMBER_PROFILE
        const val ARG_USER_ID = "userId"
    }
}

/**
 * 잠금 화면 (제재 정책 §5.3). 로그인 정지·영구 정지 계정이 로그인 직후 고정 진입한다.
 *
 * **루트로 세운다** — 뒤로가기로 빠져나갈 수 있으면 게이트가 아니다.
 */
data object AccountLockedPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.ACCOUNT_LOCKED
}
