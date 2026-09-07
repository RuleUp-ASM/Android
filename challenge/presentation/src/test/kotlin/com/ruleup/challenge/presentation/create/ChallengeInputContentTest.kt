package com.ruleup.challenge.presentation.create

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.presentation.clickPastGuard
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeIntent
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeState
import com.ruleup.challenge.presentation.renderScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

/**
 * 생성 · 입력. 초안을 만드는 동안은 **다시 누를 수 없어야** 한다 — 두 번 누르면 초안이 두 개
 * 생기고, 사용자는 어느 쪽으로 이어졌는지 모른 채 확인 화면을 본다.
 *
 * 기대 문구 출처: Figma `1134:544`「생성 · 입력」.
 */
@RunWith(RobolectricTestRunner::class)
class ChallengeInputContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `무엇을 적어야 하는지와 추천의 쓸모를 알린다`() {
        render(CreateChallengeState.initial)

        compose.onNodeWithText("어떤 습관을 만들까요?").assertExists()
        compose.onNodeWithText("추천으로 시작하면 바로 초안이 만들어져요").assertExists()
    }

    @Test
    fun `템플릿 조회에 실패하면 다시 시도할 길을 준다`() {
        // 추천이 안 떠도 직접 입력은 되지만, 재시도가 없으면 추천을 원하는 사용자가 막힌다.
        render(CreateChallengeState.initial.copy(templatesFailed = true))

        compose.onNodeWithText("추천을 불러오지 못했어요").assertExists()
        compose.onNodeWithText("다시 시도").assertExists()
    }

    @Test
    fun `다시 시도를 누르면 추천을 다시 요청한다`() {
        val intents = mutableListOf<CreateChallengeIntent>()
        render(CreateChallengeState.initial.copy(templatesFailed = true)) { intents += it }

        compose.onNodeWithText("다시 시도").clickPastGuard()

        assertTrue(intents.contains(CreateChallengeIntent.RetryTemplates))
    }

    private fun render(
        state: CreateChallengeState,
        onIntent: (CreateChallengeIntent) -> Unit = {},
    ) {
        compose.renderScreen { ChallengeInputContent(onIntent = onIntent, state = state) }
    }
}
