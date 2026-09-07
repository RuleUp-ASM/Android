package com.ruleup.challenge.presentation.invite.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeInvitationPreview
import com.ruleup.challenge.domain.entity.InvitedChallenge
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.domain.entity.JoinBlockedException
import com.ruleup.challenge.domain.entity.JoinResult
import com.ruleup.challenge.domain.fake.FakeChallengeRepository
import com.ruleup.challenge.domain.navigation.ChallengeDetailPage
import com.ruleup.domain.test.RecordingNavigationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 멤버 초대 링크 진입. 조회는 토큰을 소모하지 않고 수락에서만 소모되므로, **들어온 것만으로
 * 가입시키면 안 된다** — 링크를 눌러 본 사람이 자기도 모르게 방에 들어가 있게 된다.
 *
 * 막힌 이유는 서버가 미리 판정해 준다(`joinable`) — 그걸 무시하고 눌러 보게 하면 409 를 받는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChallengeInviteViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `링크로 들어온 것만으로는 가입하지 않는다`() =
        runTest {
            val repo = FakeChallengeRepository(invitationPreview = { preview() })
            val viewModel = viewModel(repo)

            viewModel.onIntent(ChallengeInviteIntent.Load("cinv_1"))

            assertTrue(repo.calls.none { it == "acceptInvitation" })
            assertEquals(
                "새벽 러닝 크루",
                viewModel.uiState.value.preview
                    ?.challenge
                    ?.title,
            )
        }

    @Test
    fun `토큰이 비어 있으면 조회하지 않고 사유를 보여 준다`() =
        runTest {
            val repo = FakeChallengeRepository(invitationPreview = { preview() })
            val viewModel = viewModel(repo)

            viewModel.onIntent(ChallengeInviteIntent.Load(""))

            assertTrue(repo.calls.isEmpty())
            assertEquals("초대 링크를 확인할 수 없어요", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `서버가 못 들어간다고 하면 수락을 시도하지 않는다`() =
        runTest {
            // 눌러 보게 두면 409 를 받고, 사용자는 이유를 그제야 안다.
            val repo = FakeChallengeRepository(invitationPreview = { preview(joinable = false, reason = JoinBlockReason.FULL) })
            val viewModel = viewModel(repo)
            viewModel.onIntent(ChallengeInviteIntent.Load("cinv_1"))

            viewModel.onIntent(ChallengeInviteIntent.Accept)

            assertFalse(viewModel.uiState.value.canAccept)
            assertTrue(repo.calls.none { it == "acceptInvitation" })
        }

    @Test
    fun `수락하면 링크에서 받은 토큰을 그대로 보낸다`() =
        runTest {
            val repo =
                FakeChallengeRepository(
                    invitationPreview = { preview() },
                    acceptInvitation = { joinResult() },
                )
            val viewModel = viewModel(repo)
            viewModel.onIntent(ChallengeInviteIntent.Load("cinv_9d2f"))

            viewModel.onIntent(ChallengeInviteIntent.Accept)

            assertEquals(listOf("cinv_9d2f", "cinv_9d2f"), repo.invitationTokens)
        }

    @Test
    fun `수락에 성공하면 초대 화면으로 되돌아오지 못하게 스택을 방으로 바꾼다`() =
        runTest {
            // 되돌아오면 이미 소모된 토큰으로 다시 수락을 시도하게 된다.
            val nav = RecordingNavigationHelper()
            val repo =
                FakeChallengeRepository(
                    invitationPreview = { preview() },
                    acceptInvitation = { joinResult() },
                )
            val viewModel = viewModel(repo, nav)
            viewModel.onIntent(ChallengeInviteIntent.Load("cinv_1"))

            viewModel.onIntent(ChallengeInviteIntent.Accept)

            assertEquals(ChallengeDetailPage.PATH, nav.replaced.single().path)
            assertEquals(mapOf("challengeId" to "ch1"), nav.replaced.single().args)
        }

    @Test
    fun `수락 중에 막히면 이동하지 않고 사유를 화면에 남긴다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val repo =
                FakeChallengeRepository(
                    invitationPreview = { preview() },
                    acceptInvitation = { throw JoinBlockedException(JoinBlockReason.FREE_LIMIT) },
                )
            val viewModel = viewModel(repo, nav)
            viewModel.onIntent(ChallengeInviteIntent.Load("cinv_1"))

            viewModel.onIntent(ChallengeInviteIntent.Accept)

            assertEquals(JoinBlockReason.FREE_LIMIT, viewModel.uiState.value.blockedBy)
            assertTrue(nav.replaced.isEmpty())
        }

    private fun viewModel(
        repo: FakeChallengeRepository,
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = ChallengeInviteViewModel(challengeRepository = repo, navigationHelper = nav)

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

    private fun joinResult() = JoinResult(countFromCycle = null, requiredPermissions = emptyList(), personalSetupRequired = false)
}
