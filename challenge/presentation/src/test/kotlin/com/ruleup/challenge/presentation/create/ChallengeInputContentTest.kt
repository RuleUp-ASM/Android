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

/** 생성 · 입력. */
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
