package com.ruleup.profile.presentation.settings

import androidx.compose.ui.test.junit4.createComposeRule
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
 * 탈퇴는 되돌리기 어려운 동작이라 확인 시트를 반드시 거친다.
 */
@RunWith(RobolectricTestRunner::class)
class SettingsContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `알림 설정과 알림함으로 가는 길을 모두 둔다`() {
        // 알림 센터는 설정으로 막을 수 없는 기록이라 설정과 별개의 진입점이 필요하다.
        render(SettingsState.initial.copy(isLoading = false))

        compose.onNodeWithText("알림 설정").assertExists()
        compose.onNodeWithText("알림함").assertExists()
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
