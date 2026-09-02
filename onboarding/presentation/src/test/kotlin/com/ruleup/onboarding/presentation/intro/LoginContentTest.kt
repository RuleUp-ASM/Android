package com.ruleup.onboarding.presentation.intro

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.onboarding.domain.auth.entity.OAuthProvider
import com.ruleup.onboarding.presentation.intro.screen.LoginContent
import com.ruleup.onboarding.presentation.intro.viewmodel.LoginIntent
import com.ruleup.onboarding.presentation.onboarding.clickPastGuard
import com.ruleup.onboarding.presentation.onboarding.renderOnboarding
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 로그인. 앱의 **유일한 진입점**이라 두 제공사 버튼이 각각 자기 제공사로 이어져야 한다 —
 * 뒤바뀌면 사용자가 쓰지 않는 계정으로 가입하고, 그 사실을 나중에야 안다.
 *
 * 문구는 **일부러 단언하지 않는다.** Figma `1134:1670` 은 `그룹과 함께, 습관이 기록이 되도록` /
 * `카카오로 계속하기` 인데 코드는 `RuleUp에 오신 것을 환영해요` / `카카오로 시작하기` 다.
 * 어느 쪽이 맞는지는 기획 판단이라 한쪽을 못 박으면 그게 정답이 되어 버린다.
 */
@RunWith(RobolectricTestRunner::class)
class LoginContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `두 제공사로 시작할 수 있다`() {
        // 하나만 있으면 그 계정이 없는 사용자가 앱에 못 들어온다.
        compose.renderOnboarding { LoginContent(onIntent = {}) }

        compose.onNodeWithText("카카오로 시작하기").assertExists()
        compose.onNodeWithText("Google로 시작하기").assertExists()
    }

    @Test
    fun `카카오 버튼은 카카오로 이어진다`() {
        val intents = mutableListOf<LoginIntent>()
        compose.renderOnboarding { LoginContent(onIntent = { intents += it }) }

        compose.onNodeWithText("카카오로 시작하기").clickPastGuard()

        assertEquals(listOf<LoginIntent>(LoginIntent.LoginClicked(OAuthProvider.KAKAO)), intents)
    }

    @Test
    fun `구글 버튼은 구글로 이어진다`() {
        // 뒤바뀌면 사용자가 쓰지 않는 계정으로 가입한다.
        val intents = mutableListOf<LoginIntent>()
        compose.renderOnboarding { LoginContent(onIntent = { intents += it }) }

        compose.onNodeWithText("Google로 시작하기").clickPastGuard()

        assertEquals(listOf<LoginIntent>(LoginIntent.LoginClicked(OAuthProvider.GOOGLE)), intents)
    }

    @Test
    fun `약관 동의가 시작과 함께 이뤄진다는 것을 알린다`() {
        // 별도 동의 화면이 없으므로 여기서 말하지 않으면 사용자가 모른 채 동의하게 된다.
        compose.renderOnboarding { LoginContent(onIntent = {}) }

        compose.onNodeWithText("시작과 동시에 서비스 이용약관 및 개인정보 처리방침에 동의하게 됩니다").assertExists()
    }

    @Test
    fun `버튼을 누르기 전에는 아무 의도도 올라가지 않는다`() {
        val intents = mutableListOf<LoginIntent>()
        compose.renderOnboarding { LoginContent(onIntent = { intents += it }) }

        assertTrue(intents.isEmpty())
    }
}
