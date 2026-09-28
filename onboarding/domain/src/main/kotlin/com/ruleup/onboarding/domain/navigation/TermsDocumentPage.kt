package com.ruleup.onboarding.domain.navigation

import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 약관 원문 열람. */
data class TermsDocumentPage(
    val type: AgreementType,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_TYPE to type.key))

    companion object {
        const val PATH = AppRoutes.TERMS_DOCUMENT
        const val ARG_TYPE = "type"
    }
}
