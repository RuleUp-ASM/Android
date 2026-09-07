package com.ruleup.profile.presentation.watching

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.domain.entity.WatcherStatus
import com.ruleup.challenge.domain.entity.Watching
import com.ruleup.profile.presentation.renderScreen
import com.ruleup.profile.presentation.watching.viewmodel.WatchingIntent
import com.ruleup.profile.presentation.watching.viewmodel.WatchingState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 패널티 수신 관리 (Figma 1134:2221).
 *
 * 토글이 무엇을 멈추는지가 이 화면의 핵심이다 — **푸시만 멈추고 알림함에는 남는다**는 걸 말하지
 * 않으면 사용자는 껐는데도 알림함에 쌓이는 걸 보고 설정을 믿지 않게 된다.
 */
@RunWith(RobolectricTestRunner::class)
class WatchingContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `감시자로 지정된 곳이 없으면 빈 목록 대신 그 사실을 말한다`() {
        render(WatchingState.initial.copy(isLoading = false))

        compose.onNodeWithText("아직 감시자로 지정된 곳이 없어요").assertExists()
    }

    @Test
    fun `토글이 푸시만 멈춘다는 걸 함께 말한다`() {
        render(WatchingState.initial.copy(isLoading = false, items = listOf(watching())))

        compose.onNodeWithText("알림함에는 남아요", substring = true).assertExists()
    }

    @Test
    fun `누구의 어떤 챌린지인지 함께 보여 준다`() {
        // 감시자에게 보이는 정보는 닉네임·챌린지명뿐이라 둘 다 빠지면 무엇을 끄는지 알 수 없다.
        render(WatchingState.initial.copy(isLoading = false, items = listOf(watching())))

        compose.onNodeWithText("수민").assertExists()
        compose.onNodeWithText("아침 6:30 기상").assertExists()
    }

    @Test
    fun `수신거부 확인에서는 되돌릴 수 없다는 걸 먼저 말한다`() {
        render(
            WatchingState.initial.copy(
                isLoading = false,
                items = listOf(watching()),
                revokeTarget = "w1",
            ),
        )

        compose.onNodeWithText("다시 켤 수 없어요", substring = true).assertExists()
    }

    private fun watching() =
        Watching(
            watcherId = "w1",
            challengeTitle = "아침 6:30 기상",
            ownerNickname = "수민",
            status = WatcherStatus.ACTIVE,
            pushEnabled = true,
            consentAt = null,
        )

    private fun render(
        state: WatchingState,
        onIntent: (WatchingIntent) -> Unit = {},
    ) {
        compose.renderScreen { WatchingContent(state = state, onIntent = onIntent) }
    }
}
