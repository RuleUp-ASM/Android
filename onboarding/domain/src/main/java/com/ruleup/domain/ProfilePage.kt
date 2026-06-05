package com.ruleup.domain

import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/**
 * 프로필 설정(신규 가입) 각 단계 페이지.
 * 단계 순서: 아이콘 → 닉네임 → 관심사 → 권한 → 약관동의.
 */
object ProfileIconPage : Page {
    const val PATH = "profile/icon"

    /** 프로필 설정 진입 시 함께 전달되는 가입 토큰 인자 키. */
    const val ARG_SIGNUP_TOKEN = "signupToken"

    override fun toRoute(): NavRoute = NavRoute(PATH)

    /** 로그인 신규 분기에서 signupToken 을 실어 진입할 때 사용한다. */
    fun routeWithToken(signupToken: String): NavRoute = NavRoute(PATH, mapOf(ARG_SIGNUP_TOKEN to signupToken))
}

object ProfileNicknamePage : Page {
    const val PATH = "profile/nickname"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

object ProfileInterestPage : Page {
    const val PATH = "profile/interest"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

object ProfilePermissionPage : Page {
    const val PATH = "profile/permission"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

object ProfileAgreementPage : Page {
    const val PATH = "profile/agreement"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
