package com.ruleup.profile.presentation.sanctions

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.domain.entity.user.AccountStatus
import com.ruleup.profile.domain.entity.ActiveSanction
import com.ruleup.profile.domain.entity.SanctionHistory
import com.ruleup.profile.domain.entity.SanctionType
import com.ruleup.profile.presentation.renderScreen
import com.ruleup.profile.presentation.sanctions.viewmodel.SanctionsIntent
import com.ruleup.profile.presentation.sanctions.viewmodel.SanctionsState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 제재 이력. **영구 정지에는 해제일이 없다** — 빈칸으로 두면 "곧 풀린다"로 읽혀서, 사용자가
 * 기다리다 문의로 온다.
 *
 * 열람 전용이라 이의 제기 버튼이 없다는 것도 화면이 말해 줘야 한다.
 */
@RunWith(RobolectricTestRunner::class)
class SanctionsContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `받은 제재가 없으면 빈 화면 대신 그 사실을 말한다`() {
        render(SanctionsState.initial.copy(isLoading = false, history = history()))

        compose.onNodeWithText("받은 제재가 없어요").assertExists()
    }

    @Test
    fun `영구 정지는 해제일이 없다는 걸 분명히 말한다`() {
        render(
            SanctionsState.initial.copy(
                isLoading = false,
                history = history(active = sanction(type = SanctionType.BAN, endsAt = null)),
            ),
        )

        compose.onNodeWithText("해제일 없음 (영구)").assertExists()
        compose.onNodeWithText("영구 정지").assertExists()
    }

    @Test
    fun `해제일이 있으면 언제까지인지 보여 준다`() {
        render(
            SanctionsState.initial.copy(
                isLoading = false,
                history = history(active = sanction(type = SanctionType.LOCK, endsAt = "2026-09-20T00:00:00Z")),
            ),
        )

        compose.onNodeWithText("2026.09.20까지").assertExists()
    }

    @Test
    fun `이의 제기 대신 고객센터 문의라는 걸 말한다`() {
        // 열람 전용 화면이라 여기서 보낼 수 있는 요청이 없다.
        render(SanctionsState.initial.copy(isLoading = false, history = history(active = sanction())))

        compose.onNodeWithText("열람 전용", substring = true).assertExists()
    }

    @Test
    fun `조회에 실패하면 사유를 보여 준다`() {
        render(SanctionsState.initial.copy(isLoading = false, errorMessage = "네트워크가 끊겼어요"))

        compose.onNodeWithText("네트워크가 끊겼어요").assertExists()
    }

    private fun history(active: ActiveSanction? = null) =
        SanctionHistory(
            accountStatus = if (active == null) AccountStatus.ACTIVE else AccountStatus.LOCKED,
            activeSanction = active,
            admin = emptyList(),
            auto = emptyList(),
        )

    private fun sanction(
        type: SanctionType? = SanctionType.LOCK,
        endsAt: String? = "2026-09-20T00:00:00Z",
    ) = ActiveSanction(
        sanctionId = "s1",
        track = null,
        type = type,
        featureCode = null,
        reasonCode = "REPORT_REVIEW",
        reasonText = "신고 검토 결과 커뮤니티 가이드 위반이 확인되었습니다.",
        startsAt = "2026-08-20T04:12:33Z",
        endsAt = endsAt,
        reviewRequestable = true,
    )

    private fun render(
        state: SanctionsState,
        onIntent: (SanctionsIntent) -> Unit = {},
    ) {
        compose.renderScreen { SanctionsContent(state = state, onIntent = onIntent) }
    }
}
