package com.ruleup.profile.presentation.invite

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.profile.domain.entity.FriendInvitation
import com.ruleup.profile.presentation.invite.viewmodel.FriendInviteIntent
import com.ruleup.profile.presentation.invite.viewmodel.FriendInviteState
import com.ruleup.profile.presentation.renderScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 친구 초대. 초대 코드를 받기 전에는 **공유할 것이 없다** — 빈 코드를 보여 주면 사용자가 그걸
 * 지인에게 보낸다. 초대 현황이 비었을 때도 "아직 없다"고 말해야 조회 실패와 구분된다.
 */
@RunWith(RobolectricTestRunner::class)
class FriendInviteContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `불러오는 중에는 실패 문구를 띄우지 않는다`() {
        render(FriendInviteState.initial.copy(isLoading = true))

        compose.onNodeWithText("초대 정보를 불러오지 못했어요").assertDoesNotExist()
    }

    @Test
    fun `조회에 실패하면 사유를 보여 준다`() {
        render(FriendInviteState.initial.copy(isLoading = false, errorMessage = "코드를 못 받았어요"))

        compose.onNodeWithText("코드를 못 받았어요").assertExists()
    }

    @Test
    fun `사유를 모르는 실패도 빈 화면으로 두지 않는다`() {
        render(FriendInviteState.initial.copy(isLoading = false, errorMessage = null))

        compose.onNodeWithText("초대 정보를 불러오지 못했어요").assertExists()
    }

    @Test
    fun `초대를 받으면 코드를 보여 준다`() {
        render(FriendInviteState.initial.copy(isLoading = false, invitation = invitation()))

        compose.onNodeWithText("내 초대 코드").assertExists()
        compose.onNodeWithText("ABC123").assertExists()
    }

    @Test
    fun `초대로 가입한 친구가 없으면 아직 없다고 말한다`() {
        // 빈 목록을 그냥 두면 조회 실패와 구분되지 않는다.
        render(FriendInviteState.initial.copy(isLoading = false, invitation = invitation()))

        compose.onNodeWithText("아직 초대로 가입한 친구가 없어요").assertExists()
    }

    private fun invitation() =
        FriendInvitation(
            inviteCode = "ABC123",
            inviteUrl = "https://ruleup.co.kr/inv/ABC123",
            rewardDescription = "친구가 완주하면 보상을 드려요",
            invitees = emptyList(),
        )

    private fun render(
        state: FriendInviteState,
        onIntent: (FriendInviteIntent) -> Unit = {},
    ) {
        compose.renderScreen { FriendInviteContent(state = state, onIntent = onIntent) }
    }
}
