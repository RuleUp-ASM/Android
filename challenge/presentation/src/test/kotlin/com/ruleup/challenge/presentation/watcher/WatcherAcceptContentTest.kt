package com.ruleup.challenge.presentation.watcher

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.domain.entity.WatcherAcceptance
import com.ruleup.challenge.domain.entity.WatcherChannel
import com.ruleup.challenge.domain.entity.WatcherStatus
import com.ruleup.challenge.presentation.renderScreen
import com.ruleup.challenge.presentation.watcher.viewmodel.WatcherAcceptFailure
import com.ruleup.challenge.presentation.watcher.viewmodel.WatcherAcceptIntent
import com.ruleup.challenge.presentation.watcher.viewmodel.WatcherAcceptState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 감시자 초대 수락. */
@RunWith(RobolectricTestRunner::class)
class WatcherAcceptContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `수락 전에 무엇에 동의하는지 말한다`() {
        render(WatcherAcceptState.initial)

        compose.onNodeWithText("실패한 날 알림을 받아요", substring = true).assertExists()
        compose.onNodeWithText("수락하기").assertExists()
    }

    @Test
    fun `수락하면 실패 확정일의 알림을 안내한다`() {
        // 제공하지 않는 설정 경로는 안내하지 않는다.
        render(WatcherAcceptState.initial.copy(accepted = acceptance()))

        compose.onNodeWithText("이제 감시자예요").assertExists()
        compose.onNodeWithText("실패가 확정된 날에만 알림이 가요.").assertExists()
        compose.onNodeWithText("내가 받는 알림", substring = true).assertDoesNotExist()
    }

    @Test
    fun `만료된 초대는 수락 버튼을 감추고 사유를 말한다`() {
        render(
            WatcherAcceptState.initial.copy(
                failure = WatcherAcceptFailure.EXPIRED,
                errorMessage = "초대가 만료됐어요. 다시 초대해 달라고 해주세요.",
            ),
        )

        compose.onNodeWithText("수락하기").assertDoesNotExist()
        compose.onNodeWithText("초대가 만료됐어요").assertExists()
    }

    @Test
    fun `본인 초대는 안 되는 일이라고 말한다`() {
        render(WatcherAcceptState.initial.copy(failure = WatcherAcceptFailure.SELF, errorMessage = "내가 만든 챌린지예요"))

        compose.onNodeWithText("내 챌린지예요").assertExists()
    }

    private fun acceptance() = WatcherAcceptance(watcherId = "w1", status = WatcherStatus.ACTIVE, channel = WatcherChannel.IN_APP)

    private fun render(
        state: WatcherAcceptState,
        onIntent: (WatcherAcceptIntent) -> Unit = {},
    ) {
        compose.renderScreen { WatcherAcceptContent(state = state, onIntent = onIntent) }
    }
}
