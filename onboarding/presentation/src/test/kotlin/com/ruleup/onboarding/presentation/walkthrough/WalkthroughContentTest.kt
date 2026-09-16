package com.ruleup.onboarding.presentation.walkthrough

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.onboarding.presentation.onboarding.clickPastGuard
import com.ruleup.onboarding.presentation.onboarding.renderOnboarding
import com.ruleup.onboarding.presentation.walkthrough.viewmodel.WalkthroughIntent
import com.ruleup.onboarding.presentation.walkthrough.viewmodel.WalkthroughPageIndex
import com.ruleup.onboarding.presentation.walkthrough.viewmodel.WalkthroughState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

/**
 * 첫 실행 소개. 기대 문구의 출처는 Figma(`1460:2`·`1461:2`·`1462:2`)다.
 *
 * 마지막 장에서 **건너뛰기가 사라지고 CTA 문구가 바뀌는 것**이 계약이다 — 둘 다 끝내기라서,
 * 남겨 두면 같은 일을 하는 버튼이 한 화면에 둘이 된다.
 */
@RunWith(RobolectricTestRunner::class)
class WalkthroughContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `첫 장은 만들기 단계를 보여 준다`() {
        render(WalkthroughPageIndex.CREATE)

        compose.onNodeWithText("01 · 만들기").assertExists()
        compose.onNodeWithText("AI가 초안을 만들었어요").assertExists()
    }

    @Test
    fun `둘째 장은 자동 인증 신호 네 가지를 보여 준다`() {
        render(WalkthroughPageIndex.KEEP)

        compose.onNodeWithText("02 · 지키기").assertExists()
        listOf("위치", "걸음", "앱 사용 시간", "기상").forEach { compose.onNodeWithText(it).assertExists() }
    }

    @Test
    fun `셋째 장은 티어 사다리를 보여 준다`() {
        render(WalkthroughPageIndex.STAY)

        compose.onNodeWithText("03 · 남기기").assertExists()
        listOf("브론즈", "실버", "골드", "다이아", "루비").forEach { compose.onNodeWithText(it).assertExists() }
    }

    @Test
    fun `마지막 장에서는 건너뛰기가 없고 CTA 가 시작하기다`() {
        render(WalkthroughPageIndex.STAY)

        compose.onNodeWithText("건너뛰기").assertDoesNotExist()
        compose.onNodeWithText("시작하기").assertExists()
    }

    @Test
    fun `중간 장에서는 CTA 가 다음이다`() {
        render(WalkthroughPageIndex.KEEP)

        compose.onNodeWithText("다음").assertExists()
        compose.onNodeWithText("건너뛰기").assertExists()
    }

    @Test
    fun `건너뛰기를 누르면 건너뛰기 의도를 올린다`() {
        val intents = mutableListOf<WalkthroughIntent>()
        render(WalkthroughPageIndex.CREATE, onIntent = intents::add)

        compose.onNodeWithText("건너뛰기").clickPastGuard()

        assertEquals(WalkthroughIntent.Skip, intents.single())
    }

    @Test
    fun `CTA 를 누르면 다음 의도를 올린다`() {
        val intents = mutableListOf<WalkthroughIntent>()
        render(WalkthroughPageIndex.CREATE, onIntent = intents::add)

        compose.onNodeWithText("다음").clickPastGuard()

        assertEquals(WalkthroughIntent.Next, intents.single())
    }

    private fun render(
        page: WalkthroughPageIndex,
        onIntent: (WalkthroughIntent) -> Unit = {},
    ) {
        compose.renderOnboarding {
            WalkthroughContent(state = WalkthroughState(page = page), onIntent = onIntent)
        }
    }
}
