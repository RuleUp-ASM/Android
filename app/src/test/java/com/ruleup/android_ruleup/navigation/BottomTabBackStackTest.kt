package com.ruleup.android_ruleup.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ruleup.challenge.domain.navigation.ChallengeExplorePage
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.observability.domain.test.testObservability
import com.ruleup.onboarding.domain.navigation.HomePage
import com.ruleup.profile.domain.navigation.MyHomePage
import org.junit.Test
import kotlin.test.assertEquals

/** 탭끼리 옮겨 다닌 기록이 쌓이면 뒤로가기가 홈이 아니라 직전 탭으로 간다(NAV-07). */
class BottomTabBackStackTest {
    @Test
    fun `탭을 여러 번 옮겨도 백스택은 홈과 마지막 탭뿐이다`() {
        val backStack = NavBackStack<NavKey>(GenericNavKey(HomePage.PATH))

        handleNavRoute(MyHomePage.toRoute(), backStack, testObservability())
        handleNavRoute(NavRoute(ChallengeExplorePage.PATH), backStack, testObservability())

        assertEquals(
            listOf(HomePage.PATH, ChallengeExplorePage.PATH),
            backStack.map { (it as GenericNavKey).path },
        )
    }
}
