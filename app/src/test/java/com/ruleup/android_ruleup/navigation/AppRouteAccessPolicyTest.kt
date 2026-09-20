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
        // 딥링크는 외부 입력이다. 모르는 경로를 공개로 보면 오타 하나가 인증 우회 통로가 된다.
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
        // 목록이 늘 때마다, 늘어난 화면이 인증된 API 를 호출하지 않는지 확인하는 것이 리뷰 포인트다.
        // 워크쓰루는 첫 실행 소개라 서버를 부르지 않고 기기 저장소 플래그만 읽는다.
        // 약관 원문은 앱 에셋을 읽어 그릴 뿐이고, 동의 전에 읽을 수 있어야 동의가 성립한다.
        val public = appRoutes.filterNot { it.isLoginRequired }.map { it.path }

        assertEquals(listOf(TermsDocumentPage.PATH, WalkthroughPage.PATH), public)
    }
}
