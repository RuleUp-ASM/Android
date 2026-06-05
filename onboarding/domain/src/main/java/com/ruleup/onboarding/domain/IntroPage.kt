package com.ruleup.onboarding.domain

import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 온보딩 인트로 1 · 🎯 "함께 정한 약속, 혼자가 아니에요". */
object IntroPromisePage : Page {
    const val PATH = "intro/promise"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

/** 온보딩 인트로 2 · 🤖 "AI가 인증을 자동으로 확인해요". */
object IntroVerifyPage : Page {
    const val PATH = "intro/verify"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

/** 온보딩 인트로 3 · 🌡️ "매너 온도로 신뢰를 쌓아가요". */
object IntroTrustPage : Page {
    const val PATH = "intro/trust"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
