package com.ruleup.notification.presentation.settings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.notification.presentation.settings
import com.ruleup.notification.presentation.settings.viewmodel.NotificationSettingsState
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

/**
 * 알림 설정.
 *
 * 두 가지를 말하지 않으면 화면이 거짓말한다 — **마스터가 꺼지면 그룹이 켜져 있어도 안 온다**는 것,
 * 그리고 **OS 권한이 꺼져 있으면 앱 설정과 무관하게 안 온다**는 것.
 *
 * 야간 토글은 없다 — 9/5 개정에서 `nightPush` 가 삭제됐고 야간 보류는 서버 고정 규칙이다.
 */
@RunWith(RobolectricTestRunner::class)
class NotificationSettingsContentTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `그룹 세 종을 보여 준다`() {
        show(NotificationSettingsState.initial.copy(isLoading = false, settings = settings()))

        compose.onNodeWithText("계정").assertExists()
        compose.onNodeWithText("챌린지").assertExists()
        compose.onNodeWithText("마케팅 정보 수신").assertExists()
    }

    @Test
    fun `마스터가 꺼져 있으면 그룹이 켜져 있어도 안 온다고 말한다`() {
        show(NotificationSettingsState.initial.copy(isLoading = false, settings = settings(pushEnabled = false)))

        // 그룹 세 행 모두에 붙어야 한다 — 하나만 말하면 나머지는 오는 줄 안다.
        compose.onAllNodesWithText("푸시 알림이 꺼져 있어 지금은 오지 않아요", substring = true).assertCountEquals(3)
    }

    @Test
    fun `휴대폰 권한이 꺼져 있으면 배너로 알린다`() {
        // 서버 설정값은 그대로다 — 배너 없이 토글만 켜져 있으면 사용자는 오는 줄 안다.
        show(
            NotificationSettingsState.initial.copy(
                isLoading = false,
                settings = settings(),
                systemPermissionDenied = true,
            ),
        )

        compose.onNodeWithText("휴대폰에서 알림이 꺼져 있어요").assertExists()
    }

    @Test
    fun `야간 토글을 두지 않는다`() {
        // 9/5 개정에서 삭제됐다. 사용자 토글이 아니라 서버 고정 규칙이다.
        show(NotificationSettingsState.initial.copy(isLoading = false, settings = settings()))

        compose.onNodeWithText("야간에는 받지 않기").assertDoesNotExist()
    }

    @Test
    fun `설정을 꺼도 알림함에는 남는다는 걸 말한다`() {
        show(NotificationSettingsState.initial.copy(isLoading = false, settings = settings()))

        compose.onNodeWithText("알림함에 그대로 남아요", substring = true).assertExists()
    }

    @Test
    fun `마케팅을 끄면 수신 동의도 철회된다는 걸 미리 말한다`() {
        show(NotificationSettingsState.initial.copy(isLoading = false, settings = settings()))

        compose.onNodeWithText("광고성 정보 수신 동의도 함께 철회돼요", substring = true).assertExists()
    }

    private fun show(state: NotificationSettingsState) {
        compose.setContent {
            RuleUpTheme {
                NotificationSettingsContent(state = state, onIntent = { })
            }
        }
    }
}
