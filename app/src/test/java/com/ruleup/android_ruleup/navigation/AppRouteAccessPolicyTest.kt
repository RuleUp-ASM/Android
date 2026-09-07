package com.ruleup.android_ruleup.navigation

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
        // 지금은 공개 라우트가 없다. 생기면 이 목록이 늘고, 늘어난 화면이 인증된 API 를 호출하지
        // 않는지 확인하는 것이 리뷰 포인트가 된다.
        val public = appRoutes.filterNot { it.isLoginRequired }.map { it.path }

        assertEquals(emptyList<String>(), public)
    }
}
