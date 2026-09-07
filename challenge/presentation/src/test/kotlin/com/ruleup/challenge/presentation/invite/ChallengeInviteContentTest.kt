package com.ruleup.challenge.presentation.invite

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.domain.entity.ChallengeInvitationPreview
import com.ruleup.challenge.domain.entity.InvitedChallenge
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.presentation.invite.viewmodel.ChallengeInviteIntent
import com.ruleup.challenge.presentation.invite.viewmodel.ChallengeInviteState
import com.ruleup.challenge.presentation.renderScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 멤버 초대 링크 진입 (Figma 1134:1646 위쪽 카드).
 *
 * 막힌 이유를 **수락 버튼 대신** 보여 주는 것이 이 화면의 핵심이다 — 버튼을 눌러 보게 두면
 * 409 를 받고 나서야 이유를 알게 된다.
 */
@RunWith(RobolectricTestRunner::class)
class ChallengeInviteContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `누가 어떤 방에 초대했는지 보여 준다`() {
        render(ChallengeInviteState.initial.copy(isLoading = false, preview = preview()))

        compose.onNodeWithText("김지수님이 초대했어요").assertExists()
        compose.onNodeWithText("새벽 러닝 크루").assertExists()
        compose.onNodeWithText("4/10명", substring = true).assertExists()
    }

    @Test
    fun `들어갈 수 있으면 참여 버튼을 보여 준다`() {
        render(ChallengeInviteState.initial.copy(isLoading = false, preview = preview()))

        compose.onNodeWithText("참여하기").assertExists()
    }

    @Test
    fun `정원이 찼으면 참여 버튼 대신 이유를 보여 준다`() {
        render(
            ChallengeInviteState.initial.copy(
                isLoading = false,
                preview = preview(joinable = false, reason = JoinBlockReason.FULL),
            ),
        )

        compose.onNodeWithText("참여하기").assertDoesNotExist()
        compose.onNodeWithText("정원이 다 찼어요", substring = true).assertExists()
    }

    @Test
    fun `영구 차단은 이유를 설명하지 않는다`() {
        // 정책상 회피를 막기 위해 사유를 밝히지 않는다.
        render(
            ChallengeInviteState.initial.copy(
                isLoading = false,
                preview = preview(joinable = false, reason = JoinBlockReason.BANNED),
            ),
        )

        compose.onNodeWithText("이 챌린지에는 참여할 수 없어요", substring = true).assertExists()
    }

    @Test
    fun `초대를 불러오지 못하면 홈으로 갈 길을 남긴다`() {
        // 링크로 들어온 화면이라 뒤로 갈 곳이 없다 — 막다른 길이 되면 안 된다.
        render(ChallengeInviteState.initial.copy(isLoading = false, errorMessage = "초대가 만료됐어요"))

        compose.onNodeWithText("초대가 만료됐어요").assertExists()
        compose.onNodeWithText("홈으로").assertExists()
    }

    private fun preview(
        joinable: Boolean = true,
        reason: JoinBlockReason? = null,
    ) = ChallengeInvitationPreview(
        invitationId = "inv1",
        challenge =
            InvitedChallenge(
                challengeId = "ch1",
                title = "새벽 러닝 크루",
                imageUrl = null,
                category = null,
                participantCount = 4,
                capacity = 10,
                minTier = null,
                startDate = "2026-08-17",
                endDate = "2026-08-31",
            ),
        inviterNickname = "김지수",
        joinable = joinable,
        blockReason = reason,
        expiresAt = null,
    )

    private fun render(
        state: ChallengeInviteState,
        onIntent: (ChallengeInviteIntent) -> Unit = {},
    ) {
        compose.renderScreen { ChallengeInviteContent(state = state, onIntent = onIntent) }
    }
}
