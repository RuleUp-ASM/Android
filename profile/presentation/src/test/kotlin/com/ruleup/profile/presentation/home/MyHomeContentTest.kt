package com.ruleup.profile.presentation.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.domain.entity.user.NicknameStatus
import com.ruleup.profile.domain.entity.GroupChallengeSummary
import com.ruleup.profile.domain.entity.MyHome
import com.ruleup.profile.domain.entity.MyHomeCounts
import com.ruleup.profile.presentation.home.viewmodel.MyHomeIntent
import com.ruleup.profile.presentation.home.viewmodel.MyHomeState
import com.ruleup.profile.presentation.renderScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 마이 홈. MY 탭의 루트라 **화면 복귀마다 조용히 갱신**되는데, 갱신 실패로 보여 주던 프로필이
 * 오류 화면으로 바뀌면 사용자는 없던 사고를 본다.
 *
 * 그룹이 여럿일 때 뜨는 선택 시트도 여기 산다 — 하나뿐이면 묻지 않고 바로 가야 한다.
 */
@RunWith(RobolectricTestRunner::class)
class MyHomeContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `프로필을 받으면 닉네임과 집계를 보여 준다`() {
        render(MyHomeState.initial.copy(isLoading = false, home = home(nickname = "지현")))

        compose.onNodeWithText("지현의 도전").assertExists()
        compose.onNodeWithText("완주").assertExists()
        compose.onNodeWithText("진행 중").assertExists()
    }

    @Test
    fun `첫 조회에 실패하면 사유를 보여 준다`() {
        render(MyHomeState.initial.copy(isLoading = false, home = null, errorMessage = "네트워크가 끊겼어요"))

        compose.onNodeWithText("네트워크가 끊겼어요").assertExists()
    }

    @Test
    fun `사유를 모르는 실패도 빈 화면으로 두지 않는다`() {
        render(MyHomeState.initial.copy(isLoading = false, home = null, errorMessage = null))

        compose.onNodeWithText("마이 정보를 불러오지 못했어요").assertExists()
    }

    @Test
    fun `프로필이 있으면 갱신에 실패해도 오류 화면으로 바꾸지 않는다`() {
        // 복귀마다 도는 조용한 갱신이다 — 여기서 오류를 띄우면 없던 사고를 보여 준다.
        render(MyHomeState.initial.copy(isLoading = false, home = home(), errorMessage = null))

        compose.onNodeWithText("마이 정보를 불러오지 못했어요").assertDoesNotExist()
    }

    @Test
    fun `그룹이 여럿일 때만 어느 랭킹을 볼지 묻는다`() {
        render(
            MyHomeState.initial.copy(
                isLoading = false,
                home = home(),
                rankingPicker = listOf(group("ch1"), group("ch2")),
            ),
        )

        compose.onNodeWithText("어느 그룹의 랭킹을 볼까요?").assertExists()
    }

    @Test
    fun `묻지 않을 때는 선택 시트를 띄우지 않는다`() {
        render(MyHomeState.initial.copy(isLoading = false, home = home(), rankingPicker = null))

        compose.onNodeWithText("어느 그룹의 랭킹을 볼까요?").assertDoesNotExist()
    }

    @Test
    fun `검수 중인 닉네임은 그 사실을 함께 보여 준다`() {
        // 모르면 사용자는 닉네임이 왜 안 바뀌는지 알 수 없다.
        render(
            MyHomeState.initial.copy(
                isLoading = false,
                home = home(nicknameStatus = NicknameStatus.PENDING),
            ),
        )

        compose.onNodeWithText("검수 중").assertExists()
    }

    private fun home(
        nickname: String = "지현",
        nicknameStatus: NicknameStatus = NicknameStatus.APPROVED,
    ) = MyHome(
        nickname = nickname,
        nicknameStatus = nicknameStatus,
        profileImageUrl = null,
        mannerTemperature = 36.5,
        counts = MyHomeCounts(completed = 2, inProgress = 1, groups = 1),
    )

    private fun group(id: String) = GroupChallengeSummary(challengeId = id, title = "아침 러닝")

    private fun render(
        state: MyHomeState,
        onIntent: (MyHomeIntent) -> Unit = {},
    ) {
        compose.renderScreen { MyHomeContent(state = state, onIntent = onIntent) }
    }
}
