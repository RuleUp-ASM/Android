package com.ruleup.onboarding.presentation.onboarding

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.domain.entity.user.Gender
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 04 · 성별. 고르기 전에는 넘어가지 않는다 — 마지막 제출에서 되돌아오면 signupToken(5분)이
 * 만료돼 처음부터 다시 해야 한다.
 *
 * 기대 문구 출처: Figma `1134:1798`「온보딩 4 · 성별」.
 */
@RunWith(RobolectricTestRunner::class)
class GenderContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `통계에만 쓴다는 것을 함께 알린다`() {
        // 왜 받는지 없으면 민감한 질문으로 읽혀 이탈한다.
        compose.renderOnboarding { GenderContent(onIntent = {}) }

        compose.onNodeWithText("성별을 알려주세요").assertExists()
        compose.onNodeWithText("통계에만 사용해요").assertExists()
    }

    @Test
    fun `고르기 전에는 다음을 눌러도 넘어가지 않는다`() {
        val nav = compose.renderOnboarding { GenderContent(onIntent = {}, gender = null) }

        compose.onNodeWithText("다음").clickPastGuard()

        assertTrue(nav.didNotMove)
    }

    @Test
    fun `고르고 나면 다음 단계로 넘어간다`() {
        val nav = compose.renderOnboarding { GenderContent(onIntent = {}, gender = Gender.FEMALE) }

        compose.onNodeWithText("다음").clickPastGuard()

        assertEquals(1, nav.pages.size)
    }
}
