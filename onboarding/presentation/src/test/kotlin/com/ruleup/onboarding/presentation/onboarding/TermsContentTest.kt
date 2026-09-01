package com.ruleup.onboarding.presentation.onboarding

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.onboarding.presentation.onboarding.viewmodel.OnboardingIntent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 06 · 약관. 마지막 단계라 여기서 막히면 **앞의 다섯 단계가 통째로 헛수고**가 된다.
 *
 * 필수 동의를 다 채우기 전에 제출하면 서버가 `REQUIRED_AGREEMENT_MISSING` 으로 튕기는데,
 * signupToken 은 5분이라 그 왕복이 곧 가입 이탈이다 — 화면이 먼저 막아야 한다.
 *
 * 기대 문구 출처: Figma `1134:1867`「온보딩 6 · 약관」.
 */
@RunWith(RobolectricTestRunner::class)
class TermsContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `마지막 단계라는 것과 시작하기 버튼을 보여 준다`() {
        compose.renderOnboarding { TermsContent(onIntent = {}) }

        compose.onNodeWithText("마지막이에요").assertExists()
        compose.onNodeWithText("시작하기").assertExists()
        compose.onNodeWithText("전체 동의").assertExists()
    }

    @Test
    fun `필수 동의를 다 채우기 전에는 제출하지 않는다`() {
        // 서버 왕복 전에 막는다 — 튕겨 돌아오면 signupToken 이 만료돼 처음부터 다시다.
        val intents = mutableListOf<OnboardingIntent>()
        compose.renderOnboarding { TermsContent(onIntent = { intents += it }, checked = emptySet()) }

        compose.onNodeWithText("시작하기").clickPastGuard()

        assertTrue(intents.none { it is OnboardingIntent.Submit })
    }

    @Test
    fun `필수만 채워도 제출할 수 있다`() {
        // 선택 항목까지 요구하면 동의를 강요하는 셈이 된다.
        val intents = mutableListOf<OnboardingIntent>()
        compose.renderOnboarding {
            TermsContent(onIntent = { intents += it }, checked = AgreementType.REQUIRED.toSet())
        }

        compose.onNodeWithText("시작하기").clickPastGuard()

        assertEquals(listOf<OnboardingIntent>(OnboardingIntent.Submit), intents)
    }

    @Test
    fun `제출하는 동안에는 다시 누를 수 없다`() {
        // 연타하면 가입 요청이 두 번 나간다.
        val intents = mutableListOf<OnboardingIntent>()
        compose.renderOnboarding {
            TermsContent(
                onIntent = { intents += it },
                checked = AgreementType.REQUIRED.toSet(),
                submitting = true,
            )
        }

        compose.onNodeWithText("시작하기").clickPastGuard()

        assertTrue(intents.none { it is OnboardingIntent.Submit })
    }

    @Test
    fun `전체 동의를 누르면 한 번에 처리한다`() {
        val intents = mutableListOf<OnboardingIntent>()
        compose.renderOnboarding { TermsContent(onIntent = { intents += it }) }

        compose.onNodeWithText("전체 동의").clickPastGuard()

        assertTrue(intents.contains(OnboardingIntent.ToggleAllAgreements))
    }
}
