package com.ruleup.onboarding.presentation.onboarding

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.onboarding.presentation.onboarding.viewmodel.OnboardingIntent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 닉네임 단계. **서버 확인을 통과해야만** 다음으로 넘어간다 — 통과 전에 전진시키면 마지막 제출에서
 * 1단계로 되돌아오고, signupToken 은 5분이라 그 왕복이 곧 가입 이탈이 된다.
 *
 * 잠금이 `enabled=false` 가 아니라 **클릭 조기 반환**이라 `assertIsNotEnabled` 로는 잡히지 않는다.
 * 눌러 보고 이동이 없었는지를 봐야 실제 계약을 검증한다.
 */
@RunWith(RobolectricTestRunner::class)
class NicknameContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `서버 확인 전에는 다음을 눌러도 넘어가지 않는다`() {
        val nav = render(nicknameAvailable = null)

        compose.onNodeWithText("다음").clickPastGuard()

        assertTrue(nav.didNotMove)
    }

    @Test
    fun `이미 쓰는 닉네임이면 다음을 눌러도 넘어가지 않는다`() {
        val nav = render(nicknameAvailable = false)

        compose.onNodeWithText("다음").clickPastGuard()

        assertTrue(nav.didNotMove)
    }

    @Test
    fun `확인을 통과하면 다음 단계로 넘어간다`() {
        val nav = render(nicknameAvailable = true)

        compose.onNodeWithText("다음").clickPastGuard()

        assertEquals(1, nav.pages.size)
    }

    @Test
    fun `1단계에서 뒤로 가면 이탈로 다룬다`() {
        // signupToken 만료로 되돌아올 수 없어, 일반 뒤로가기와 다르게 처리해야 한다.
        val intents = mutableListOf<OnboardingIntent>()
        render(nicknameAvailable = true, onIntent = { intents += it })

        compose.onNodeWithContentDescription("이전 단계로").clickPastGuard()

        assertTrue(intents.contains(OnboardingIntent.BackFromFirstStep))
    }

    @Test
    fun `입력한 닉네임은 그대로 의도로 올라간다`() {
        val intents = mutableListOf<OnboardingIntent>()
        render(onIntent = { intents += it })

        compose.onNodeWithText("닉네임을 입력하세요").performTextInput("지현")

        assertEquals(listOf<OnboardingIntent>(OnboardingIntent.SetNickName("지현")), intents)
    }

    private fun render(
        nicknameAvailable: Boolean? = null,
        nickname: String = "",
        onIntent: (OnboardingIntent) -> Unit = {},
    ): RecordingNavigationHelper =
        compose.renderOnboarding {
            NicknameContent(
                onIntent = onIntent,
                nickname = nickname,
                nicknameAvailable = nicknameAvailable,
            )
        }
}
