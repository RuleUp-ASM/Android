package com.ruleup.android_ruleup.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 딥링크·콜드 스타트는 [AppRoute.syntheticStack] 으로 백스택을 통째로 세운다. 마지막 키가 목적지가 아니면
 * 링크를 열어도 목적지 대신 부모 화면(예: 홈)만 보인다 — 초대·감시자 수락 링크가 그렇게 홈으로 떨어졌다.
 */
class AppRouteSyntheticStackTest {
    @Test
    fun `시작 백스택의 맨 위는 항상 목적지 화면이다`() {
        val args = mapOf("token" to "T")

        val wrong =
            appRoutes
                .filterNot { it.isBottomTab }
                .filter { it.syntheticStack(args).lastOrNull()?.path != it.path }
                .map { it.path }

        assertEquals(emptyList(), wrong)
    }

    @Test
    fun `목적지 키에는 링크 인자가 그대로 실린다`() {
        val route = appRoutes.first { it.path == com.ruleup.challenge.domain.navigation.ChallengeInvitePage.PATH }

        assertEquals(mapOf("token" to "T"), route.syntheticStack(mapOf("token" to "T")).last().args)
    }
}
