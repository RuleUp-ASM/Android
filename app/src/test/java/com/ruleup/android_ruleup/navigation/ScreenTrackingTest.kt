package com.ruleup.android_ruleup.navigation

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.onboarding.domain.navigation.HomePage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class ScreenTrackingTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `복원된 첫 화면과 다른 인자의 상세 및 뒤로가기를 각각 기록한다`() {
        val home = GenericNavKey(HomePage.PATH)
        val first = GenericNavKey(ChallengeDetailPage.PATH, mapOf("challengeId" to "1"))
        val second = first.copy(args = mapOf("challengeId" to "2"))
        val stack = NavBackStack<NavKey>(home)
        val entered = mutableListOf<String>()
        compose.setContent { TrackVisibleScreen(stack, entered::add) }
        compose.runOnIdle { stack.add(first) }
        compose.runOnIdle { stack.add(second) }
        compose.runOnIdle { stack.removeLastOrNull() }
        compose.runOnIdle { stack.removeLastOrNull() }
        compose.runOnIdle {
            assertEquals(listOf(home.path, first.path, second.path, first.path, home.path), entered)
        }
    }

    @Test
    fun `같은 화면 교체와 미등록 경로는 새 진입으로 기록하지 않는다`() {
        val home = GenericNavKey(HomePage.PATH)
        val stack = NavBackStack<NavKey>(home)
        val entered = mutableListOf<String>()
        compose.setContent { TrackVisibleScreen(stack, entered::add) }
        compose.runOnIdle { stack[0] = home.copy() }
        compose.runOnIdle { stack.add(GenericNavKey("unknown")) }
        compose.runOnIdle { assertEquals(listOf(home.path), entered) }
    }
}
