package com.ruleup.profile.presentation.invite.viewmodel

import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.profile.domain.entity.FriendInvitation
import com.ruleup.profile.presentation.fake.FakeMyPageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 친구 초대. 공유·복사는 **초대 코드를 받은 뒤에만** 성립한다 — 아직 못 받았는데 공유를 열면
 * 사용자가 빈 링크를 지인에게 보내게 된다. 그래서 "아무 일도 일어나지 않는다"가 계약이다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FriendInviteViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `불러오면 초대 코드와 링크를 화면에 올린다`() =
        runTest {
            val viewModel = viewModel(FakeMyPageRepository(invitation = { invitation() }))

            viewModel.onIntent(FriendInviteIntent.Load)

            assertEquals(
                "ABC123",
                viewModel.uiState.value.invitation
                    ?.inviteCode,
            )
        }

    @Test
    fun `이미 받아둔 초대가 있으면 다시 묻지 않는다`() =
        runTest {
            // 서버가 멱등이라 다시 물어도 같은 코드지만, 왕복만 늘고 얻는 게 없다.
            val repo = FakeMyPageRepository(invitation = { invitation() })
            val viewModel = viewModel(repo)

            viewModel.onIntent(FriendInviteIntent.Load)
            viewModel.onIntent(FriendInviteIntent.Load)

            assertEquals(1, repo.calls.count { it == "getInvitation" })
        }

    @Test
    fun `초대를 받기 전에는 공유를 열지 않는다`() =
        runTest {
            // 빈 링크를 지인에게 보내는 사고를 막는다.
            val viewModel = viewModel(FakeMyPageRepository())
            val effects = collectEffects(viewModel)

            viewModel.onIntent(FriendInviteIntent.ShareKakao)
            viewModel.onIntent(FriendInviteIntent.CopyLink)

            assertEquals(emptyList(), effects)
        }

    @Test
    fun `초대를 받은 뒤 공유하면 그 링크와 코드를 넘긴다`() =
        runTest {
            val viewModel = viewModel(FakeMyPageRepository(invitation = { invitation() }))
            val effects = collectEffects(viewModel)
            viewModel.onIntent(FriendInviteIntent.Load)

            viewModel.onIntent(FriendInviteIntent.ShareKakao)

            assertEquals(
                listOf(FriendInviteEffect.LaunchKakaoShare(inviteUrl = "https://ruleup.co.kr/inv/ABC123", inviteCode = "ABC123")),
                effects,
            )
        }

    @Test
    fun `링크 복사는 초대 링크를 그대로 넘긴다`() =
        runTest {
            val viewModel = viewModel(FakeMyPageRepository(invitation = { invitation() }))
            val effects = collectEffects(viewModel)
            viewModel.onIntent(FriendInviteIntent.Load)

            viewModel.onIntent(FriendInviteIntent.CopyLink)

            assertEquals(listOf(FriendInviteEffect.CopyToClipboard("https://ruleup.co.kr/inv/ABC123")), effects)
        }

    @Test
    fun `조회에 실패하면 사유를 남긴다`() =
        runTest {
            val viewModel = viewModel(FakeMyPageRepository(invitation = { throw IllegalStateException("코드 발급 실패") }))

            viewModel.onIntent(FriendInviteIntent.Load)

            assertEquals("코드 발급 실패", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `뒤로 가기는 이동 없이 화면만 닫는다`() {
        val nav = RecordingNavigationHelper()

        viewModel(nav = nav).onIntent(FriendInviteIntent.Back)

        assertEquals(1, nav.backCount)
        assertTrue(nav.routes.isEmpty())
    }

    /**
     * effect 는 Channel 이라 소비자가 없으면 아무것도 안 오고 **영원히 기다린다.**
     * 수집을 미리 걸어 두면 "왔다"와 "안 왔다"를 같은 방식으로 볼 수 있다.
     */
    private fun TestScope.collectEffects(viewModel: FriendInviteViewModel): List<FriendInviteEffect> {
        val effects = mutableListOf<FriendInviteEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }
        return effects
    }

    private fun viewModel(
        repo: FakeMyPageRepository = FakeMyPageRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = FriendInviteViewModel(myPageRepository = repo, navigationHelper = nav)

    private fun invitation() =
        FriendInvitation(
            inviteCode = "ABC123",
            inviteUrl = "https://ruleup.co.kr/inv/ABC123",
            rewardDescription = "친구가 챌린지를 완주하면 보상을 드려요",
            invitees = emptyList(),
        )
}
