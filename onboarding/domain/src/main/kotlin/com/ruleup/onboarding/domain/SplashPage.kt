package com.ruleup.onboarding.domain

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 스플래시. 앱 진입 시 자동 로그인 여부를 판별해 홈/인트로로 분기하는 시작 화면. */
object SplashPage : Page {
    const val PATH = AppRoutes.SPLASH

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
