package com.ruleup.onboarding.domain

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

object LoginPage : Page {
    const val PATH = AppRoutes.LOGIN

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
