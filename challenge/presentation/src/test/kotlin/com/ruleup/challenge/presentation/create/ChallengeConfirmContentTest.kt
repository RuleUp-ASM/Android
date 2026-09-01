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
 * 생성 · 확인. 만들기는 **되돌리기 어려운 동작**이다 — 만든 뒤에는 이름·설명·정원만 바꿀 수
 * 있으므로, 그 사실을 만들기 전에 알려야 한다.
 *
 * 만드는 중 연타하면 챌린지가 두 개 생긴다. 잠금이 곧 계약이다.
 *
 * 기대 문구 출처: Figma `1134:604`「생성 · 확인」.
 */
@RunWith(RobolectricTestRunner::class)
class ChallengeConfirmContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `만든 뒤에 못 바꾸는 것을 만들기 전에 알린다`() {
        render(CreateChallengeState.initial)

        compose.onNodeWithText("만든 뒤에는 이름·설명·정원만 바꿀 수 있어요").assertExists()
    }

    @Test
    fun `만드는 중에는 진행 중임을 보여 주고 다시 누를 수 없다`() {
        // 연타하면 챌린지가 두 개 생긴다.
        val intents = mutableListOf<CreateChallengeIntent>()
        render(CreateChallengeState.initial.copy(isCreating = true)) { intents += it }

        compose.onNodeWithText("만드는 중…").assertExists()
        compose.onNodeWithText("만드는 중…").clickPastGuard()

        assertTrue(intents.none { it is CreateChallengeIntent.Create })
    }

    @Test
    fun `초안이 없으면 만들 수 없다`() {
        // 초안 없이 누르면 서버가 튕기고, 사용자는 왜인지 모른 채 확인 화면에 갇힌다.
        val intents = mutableListOf<CreateChallengeIntent>()
        render(CreateChallengeState.initial) { intents += it }

        compose.onNodeWithText("이대로 만들기").clickPastGuard()

        assertTrue(intents.none { it is CreateChallengeIntent.Create })
    }

    private fun render(
        state: CreateChallengeState,
        onIntent: (CreateChallengeIntent) -> Unit = {},
    ) {
        compose.renderScreen { ChallengeConfirmContent(onIntent = onIntent, state = state) }
    }
}
