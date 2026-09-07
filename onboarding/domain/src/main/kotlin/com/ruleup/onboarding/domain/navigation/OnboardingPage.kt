package com.ruleup.onboarding.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/**
 * 가입 온보딩 6단계.
 *
 * 순서: 닉네임 → 관심 → 생일 → 성별 → 사진 → 약관. 권한 단계는 없다 — 온보딩에서 몰아 받으면
 * 거부율과 이탈이 커져, 각 기능에 들어갈 때 목적을 설명하고 요청한다.
 *
 * **어느 단계도 인자를 받지 않는다.** 가입 토큰은 [com.ruleup.onboarding.domain.auth.SignupSession]
 * 이 메모리로만 들고 있다 — 백스택은 직렬화되어 saved state 에 남는다.
 */
object OnboardingNicknamePage : Page {
    const val PATH = AppRoutes.ONBOARDING_NICKNAME

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

object OnboardingInterestPage : Page {
    const val PATH = AppRoutes.ONBOARDING_INTEREST

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

object OnboardingBirthPage : Page {
    const val PATH = AppRoutes.ONBOARDING_BIRTH

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

object OnboardingGenderPage : Page {
    const val PATH = AppRoutes.ONBOARDING_GENDER

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

object OnboardingPhotoPage : Page {
    const val PATH = AppRoutes.ONBOARDING_PHOTO

    override fun toRoute(): NavRoute = NavRoute(PATH)
}

object OnboardingTermsPage : Page {
    const val PATH = AppRoutes.ONBOARDING_TERMS

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
