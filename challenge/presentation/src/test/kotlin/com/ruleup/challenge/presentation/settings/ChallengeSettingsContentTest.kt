package com.ruleup.challenge.presentation.settings

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.presentation.renderScreen
import com.ruleup.challenge.presentation.settings.viewmodel.ChallengeSettingsIntent
import com.ruleup.challenge.presentation.settings.viewmodel.ChallengeSettingsState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 챌린지 수정(방장 전용). */
@RunWith(RobolectricTestRunner::class)
class ChallengeSettingsContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `불러오는 중에는 오류를 띄우지 않는다`() {
        render(ChallengeSettingsState.initial.copy(isLoading = true))

        compose.onNodeWithText("설정을 불러오지 못했어요").assertDoesNotExist()
    }

    @Test
    fun `조회에 실패하면 사유를 보여 준다`() {
        render(ChallengeSettingsState.initial.copy(isLoading = false, errorMessage = "권한이 없어요"))

        compose.onNodeWithText("권한이 없어요").assertExists()
    }

    @Test
    fun `사유를 모르는 실패도 빈 화면으로 두지 않는다`() {
        render(ChallengeSettingsState.initial.copy(isLoading = false, errorMessage = null))

        compose.onNodeWithText("설정을 불러오지 못했어요").assertExists()
    }

    private fun render(
        state: ChallengeSettingsState,
        onIntent: (ChallengeSettingsIntent) -> Unit = {},
    ) {
        compose.renderScreen { ChallengeSettingsContent(state = state, onIntent = onIntent) }
    }
}
