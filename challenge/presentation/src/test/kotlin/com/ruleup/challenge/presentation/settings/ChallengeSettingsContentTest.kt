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

/**
 * 챌린지 수정(방장 전용). 저장은 **남의 방을 바꾸는 일**이라, 바꾼 게 없거나 저장 중일 때
 * 눌리면 안 된다 — 연타는 수정 요청을 두 번 보내고 그 사이 버전이 어긋난다.
 *
 * 못 바꾸는 항목이 왜 잠겨 있는지도 말해야 한다. 이유 없이 회색이면 고장으로 읽힌다.
 */
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
