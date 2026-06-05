package com.ruleup.domain

import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

object LoginPage : Page {
    const val PATH = "login"

    override fun toRoute(): NavRoute = NavRoute(PATH)
}
