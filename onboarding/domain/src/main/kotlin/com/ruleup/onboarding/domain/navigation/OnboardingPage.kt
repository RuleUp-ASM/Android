package com.ruleup.onboarding.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 가입 온보딩 6단계. */
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
