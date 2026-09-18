package com.ruleup.profile.presentation.locked

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.profile.domain.entity.ActiveSanction
import com.ruleup.profile.domain.entity.SanctionType
import com.ruleup.profile.presentation.clickPastGuard
import com.ruleup.profile.presentation.locked.viewmodel.AccountLockedIntent
import com.ruleup.profile.presentation.locked.viewmodel.AccountLockedState
import com.ruleup.profile.presentation.renderScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

/**
 * 잠금 화면(Figma `1465:2`·`1465:37`·`1465:89`). 정지된 사용자가 앱에서 볼 수 있는 유일한 화면이라,
 * 여기서 길이 막히면 사용자는 문의할 방법도 잃는다.
 *
 * 영구 정지와 기간 정지는 **해제일의 유무**로 갈린다 — 영구인데 해제일을 그리면 기다리면 풀린다고
 * 읽고, 기간인데 안 그리면 영영 못 쓴다고 읽는다. 둘 다 같은 화면이 내는 반대 방향의 거짓말이다.
 */
@RunWith(RobolectricTestRunner::class)
class AccountLockedContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `영구 정지는 해제일을 그리지 않고 해제되지 않는다고 말한다`() {
        render(state(sanction(type = SanctionType.BAN, endsAt = null)))

        compose.onNodeWithText("계정이 영구 정지됐어요").assertExists()
        compose.onNodeWithText("해제되지 않아요").assertExists()
    }

    @Test
    fun `기간 정지는 해제 예정일을 보여 준다`() {
        render(state(sanction(type = SanctionType.LOCK, endsAt = "2026-10-15T00:00:00+09:00")))

        compose.onNodeWithText("계정 이용이 정지됐어요").assertExists()
        compose.onNodeWithText("2026.10.15").assertExists()
    }

    @Test
    fun `재검토를 요청할 수 없는 제재는 요청 버튼을 두지 않는다`() {
        // 눌러도 서버가 막는 버튼을 두면 사용자는 거절을 두 번 겪는다.
        render(state(sanction(type = SanctionType.LOCK, endsAt = null, reviewRequestable = false)))

        compose.onAllNodesWithText("재검토 요청하기").assertCountEquals(0)
    }

    @Test
    fun `연결이 끊기면 제재 내용을 지어내지 않고 다시 시도를 준다`() {
        val intents = mutableListOf<AccountLockedIntent>()
        render(AccountLockedState.initial.copy(isLoading = false, isOffline = true), onIntent = { intents += it })

        compose.onNodeWithText("다시 시도").clickPastGuard()

        assertTrue(intents.contains(AccountLockedIntent.Retry))
    }

    private fun sanction(
        type: SanctionType,
        endsAt: String?,
        reviewRequestable: Boolean = true,
    ) = ActiveSanction(
        sanctionId = "s-1",
        track = null,
        type = type,
        featureCode = null,
        reasonCode = null,
        reasonText = null,
        startsAt = "2026-09-15T00:00:00+09:00",
        endsAt = endsAt,
        reviewRequestable = reviewRequestable,
    )

    private fun state(sanction: ActiveSanction) = AccountLockedState.initial.copy(isLoading = false, sanction = sanction)

    private fun render(
        state: AccountLockedState,
        onIntent: (AccountLockedIntent) -> Unit = {},
    ) = compose.renderScreen { AccountLockedContent(state = state, onIntent = onIntent) }
}
