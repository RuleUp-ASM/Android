package com.ruleup.onboarding.domain

import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 홈. 온보딩(로그인/가입) 완료 후 진입하는 루트 화면. */
object HomePage : Page {
    const val PATH = "home"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
