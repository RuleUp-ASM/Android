package com.ruleup.onboarding.domain.navigation

import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/**
 * 약관 원문 열람. 어느 약관인지는 [AgreementType.key] 로 넘긴다.
 *
 * 원문이 앱에 번들돼 있어 **로그인 전에도 열린다** — 가입 화면에서 동의하기 전에 읽을 수 있어야
 * 동의가 성립한다. 공개 웹 URL 이 생기면 그쪽을 우선 열도록 바꾼다(그때까지의 임시 경로다).
 */
data class TermsDocumentPage(
    val type: AgreementType,
) : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH, mapOf(ARG_TYPE to type.key))

    companion object {
        const val PATH = AppRoutes.TERMS_DOCUMENT
        const val ARG_TYPE = "type"
    }
}
