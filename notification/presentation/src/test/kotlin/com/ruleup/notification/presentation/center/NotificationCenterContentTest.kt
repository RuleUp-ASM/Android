package com.ruleup.notification.presentation.center

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.notification.domain.entity.NotificationType
import com.ruleup.notification.presentation.center.viewmodel.NotificationCenterState
import com.ruleup.notification.presentation.notification
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

/** 알림 센터. */
@RunWith(RobolectricTestRunner::class)
class NotificationCenterContentTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `알림이 없으면 빈 목록 대신 그 사실을 말한다`() {
        show(NotificationCenterState.initial.copy(isLoading = false))

        compose.onNodeWithText("아직 알림이 없어요").assertExists()
    }

    @Test
    fun `조회에 실패하면 사유를 보여 준다`() {
        show(NotificationCenterState.initial.copy(isLoading = false, errorMessage = "네트워크가 끊겼어요"))

        compose.onNodeWithText("네트워크가 끊겼어요").assertExists()
    }

    @Test
    fun `제목과 본문을 함께 보여 준다`() {
        show(
            NotificationCenterState.initial.copy(
                isLoading = false,
                items = listOf(notification(title = "오늘 판정 완료", body = "기상 챌린지 · 연속 7일")),
            ),
        )

        compose.onNodeWithText("오늘 판정 완료").assertExists()
        compose.onNodeWithText("기상 챌린지 · 연속 7일").assertExists()
    }

    @Test
    fun `모르는 타입도 목록에서 지우지 않는다`() {
        // 적재된 고지를 화면에서 숨기면 사용자가 통지받은 사실을 확인할 길이 없다.
        show(
            NotificationCenterState.initial.copy(
                isLoading = false,
                items = listOf(notification(type = null, title = "새로운 소식")),
            ),
        )

        compose.onNodeWithText("새로운 소식").assertExists()
    }

    @Test
    fun `타입 뱃지는 그룹 단위로 보여 준다`() {
        show(
            NotificationCenterState.initial.copy(
                isLoading = false,
                items = listOf(notification(type = NotificationType.CHALLENGE_KICKED, title = "강퇴됐어요")),
            ),
        )

        compose.onNodeWithText("계정").assertExists()
    }

    @Test
    fun `보관 기간을 개월로 알려 준다`() {
        // 오래된 알림이 사라지는 이유를 말하지 않으면 기록이 유실된 걸로 읽힌다.
        show(NotificationCenterState.initial.copy(isLoading = false, items = listOf(notification()), retentionDays = 180))

        compose.onNodeWithText("알림은 6개월 동안 보관돼요").assertExists()
    }

    @Test
    fun `유형 필터 칩을 두지 않는다`() {
        // Figma 에는 있지만 명세가 "유형 탭 필터는 없다(P2)"로 확정했다.
        show(NotificationCenterState.initial.copy(isLoading = false, items = listOf(notification())))

        compose.onNodeWithText("전체").assertDoesNotExist()
    }

    private fun show(state: NotificationCenterState) {
        compose.setContent {
            RuleUpTheme {
                NotificationCenterContent(state = state, onIntent = { })
            }
        }
    }
}
