package com.ruleup.android_ruleup.navigation

import com.ruleup.onboarding.domain.navigation.TermsDocumentPage
import com.ruleup.onboarding.domain.navigation.WalkthroughPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRouteAccessPolicyTest {
    private val policy = AppRouteAccessPolicy()

    @Test
    fun `등록되지 않은 경로는 로그인을 요구한다`() {
        assertTrue(policy.requiresLogin("challenge/detai"))
        assertTrue(policy.requiresLogin(""))
        assertTrue(policy.requiresLogin("../admin"))
    }

    @Test
    fun `기본값은 로그인 요구다`() {
        // 새 화면을 등록하면서 깜빡해도 안전한 쪽으로 떨어져야 한다.
        val route = AppRoute(path = "some/new/page", render = {})

        assertTrue(route.isLoginRequired)
    }

    @Test
    fun `공개로 표시된 라우트만 로그인 없이 열린다`() {
        val public = appRoutes.filterNot { it.isLoginRequired }.map { it.path }

        assertEquals(listOf(TermsDocumentPage.PATH, WalkthroughPage.PATH), public)
    }
}
