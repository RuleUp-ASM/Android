package com.ruleup.profile.presentation.settings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.profile.presentation.clickPastGuard
import com.ruleup.profile.presentation.renderScreen
import com.ruleup.profile.presentation.settings.viewmodel.SettingsDialog
import com.ruleup.profile.presentation.settings.viewmodel.SettingsIntent
import com.ruleup.profile.presentation.settings.viewmodel.SettingsState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

/**
 * 설정 허브 (Figma 1134:2164).
 *
 * 알림 두 항목은 **서버가 아직 없다** — 눌러도 아무 일이 없는 스위치를 두면 사용자는 앱이 고장난
 * 줄 안다. 그래서 자리는 두되 준비 중임을 말한다.
 *
 * 탈퇴는 되돌리기 어려운 동작이라 확인 시트를 반드시 거친다.
 */
@RunWith(RobolectricTestRunner::class)
class SettingsContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `아직 없는 알림 기능은 준비 중이라고 말한다`() {
        render(SettingsState.initial.copy(isLoading = false))

        compose.onNodeWithText("푸시 알림").assertExists()
        compose.onNodeWithText("알림 시간대 · 종류").assertExists()
        // 두 항목 모두 서버가 없다 — 하나만 안내하면 나머지는 되는 줄 안다.
        compose.onAllNodesWithText("준비 중").assertCountEquals(2)
    }

    @Test
    fun `재동의가 필요하면 약관 행에 몇 건인지 보여 준다`() {
        render(SettingsState.initial.copy(isLoading = false, reconsentCount = 2))

        compose.onNodeWithText("재동의 2건").assertExists()
    }

    @Test
    fun `재동의할 게 없으면 뱃지를 붙이지 않는다`() {
        render(SettingsState.initial.copy(isLoading = false, reconsentCount = 0))

        compose.onNodeWithText("재동의", substring = true).assertDoesNotExist()
    }

    @Test
    fun `효력 중인 제재가 있으면 제재 이력 행에 표시한다`() {
        render(SettingsState.initial.copy(isLoading = false, hasActiveSanction = true))

        compose.onNodeWithText("진행 중").assertExists()
    }

    @Test
    fun `탈퇴는 곧바로 실행하지 않고 확인을 먼저 받는다`() {
        val intents = mutableListOf<SettingsIntent>()
        render(SettingsState.initial.copy(isLoading = false), onIntent = intents::add)

        compose.onNodeWithText("회원 탈퇴").clickPastGuard()

        assertTrue(SettingsIntent.ConfirmWithdraw in intents)
        assertTrue(SettingsIntent.Withdraw !in intents)
    }

    @Test
    fun `탈퇴 확인에서는 복원 조건을 함께 말한다`() {
        // 되돌릴 수 없다고만 하면 실제로는 되는 복원을 모르고 문의로 온다.
        render(SettingsState.initial.copy(isLoading = false, dialog = SettingsDialog.WITHDRAW))

        compose.onNodeWithText("1년 안에", substring = true).assertExists()
    }

    private fun render(
        state: SettingsState,
        onIntent: (SettingsIntent) -> Unit = {},
    ) {
        compose.renderScreen { SettingsContent(state = state, onIntent = onIntent) }
    }
}
