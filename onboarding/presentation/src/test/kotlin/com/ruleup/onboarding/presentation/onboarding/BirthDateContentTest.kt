package com.ruleup.onboarding.presentation.onboarding

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 03 · 생일. 가입 조건(연령) 확인에 쓰는 값이라 **형식이 맞기 전에는 넘어가지 않는다** — 넘어가면
 * 마지막 제출에서 되돌아오고, signupToken 은 5분이라 그 왕복이 곧 이탈이 된다.
 *
 * 기대 문구 출처: Figma `1134:1765`「온보딩 3 · 생일」.
 */
@RunWith(RobolectricTestRunner::class)
class BirthDateContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `무엇에 쓰는 값인지 함께 알린다`() {
        // 생일을 왜 받는지 없으면 사용자가 이탈한다.
        compose.renderOnboarding { BirthDateContent(onIntent = {}) }

        compose.onNodeWithText("생일이 언제예요?").assertExists()
        compose.onNodeWithText("가입 조건 확인에만 사용해요").assertExists()
    }

    @Test
    fun `형식이 맞기 전에는 다음을 눌러도 넘어가지 않는다`() {
        val nav = compose.renderOnboarding { BirthDateContent(onIntent = {}, birthDateValid = false) }

        compose.onNodeWithText("다음").clickPastGuard()

        assertTrue(nav.didNotMove)
    }

    @Test
    fun `형식이 맞으면 다음 단계로 넘어간다`() {
        val nav = compose.renderOnboarding { BirthDateContent(onIntent = {}, birthDateValid = true) }

        compose.onNodeWithText("다음").clickPastGuard()

        assertEquals(1, nav.pages.size)
    }

    @Test
    fun `입력이 잘못됐으면 무엇이 잘못됐는지 보여 준다`() {
        compose.renderOnboarding {
            BirthDateContent(onIntent = {}, birthDateInput = "2030/01/01", birthDateError = "미래 날짜는 쓸 수 없어요")
        }

        compose.onNodeWithText("미래 날짜는 쓸 수 없어요").assertExists()
    }

    @Test
    fun `오류가 없으면 오류 자리를 비워 둔다`() {
        // 빈 문자열이라도 자리를 차지하면 화면이 흔들린다.
        compose.renderOnboarding { BirthDateContent(onIntent = {}, birthDateError = null) }

        compose.onNodeWithText("미래 날짜는 쓸 수 없어요").assertDoesNotExist()
    }

    @Test
    fun `뒤로 가면 앞 단계로 돌아간다`() {
        val nav = compose.renderOnboarding { BirthDateContent(onIntent = {}) }

        compose.onNodeWithContentDescription("이전 단계로").clickPastGuard()

        assertEquals(1, nav.backCount)
    }
}
