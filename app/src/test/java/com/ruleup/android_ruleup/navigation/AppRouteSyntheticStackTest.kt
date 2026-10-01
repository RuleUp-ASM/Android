package com.ruleup.android_ruleup.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

/** 딥링크·콜드 스타트는 [AppRoute.syntheticStack] 으로 백스택을 통째로 세운다. */
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
    fun `챌린지 상세로 스택을 갈아끼워도 뒤로 갈 홈이 남는다`() {
        val route = appRoutes.first { it.path == com.ruleup.challenge.domain.navigation.ChallengeDetailPage.PATH }

        val stack = route.syntheticStack(mapOf("challengeId" to "c1"))

        assertEquals(
            listOf(com.ruleup.onboarding.domain.navigation.HomePage.PATH, route.path),
            stack.map { it.path },
        )
    }

    @Test
    fun `목적지 키에는 링크 인자가 그대로 실린다`() {
        val route = appRoutes.first { it.path == com.ruleup.challenge.domain.navigation.ChallengeInvitePage.PATH }

        assertEquals(mapOf("token" to "T"), route.syntheticStack(mapOf("token" to "T")).last().args)
    }
}
