package com.ruleup.challenge.presentation.watchers.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeGate
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengePenalties
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeStats
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.entity.JoinNote
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.entity.WatcherInvitation
import com.ruleup.challenge.domain.fake.FakeChallengeRepository
import com.ruleup.challenge.domain.fake.FakeWatcherRepository
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.test.RecordingNavigationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** 마이 허브 「감시자」로 들어오는 감시자 관리 화면(#559). */
@OptIn(ExperimentalCoroutinesApi::class)
class ChallengeWatchersViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `감시자 벌칙을 쓰는 방은 내 감시자 목록을 그린다`() =
        runTest {
            val watchers = FakeWatcherRepository(watchers = { ChallengeWatchers(limit = 3, watchers = emptyList()) })
            val viewModel = viewModel(watcherEnabled = true, watchers = watchers)

            viewModel.onIntent(ChallengeWatchersIntent.Load("ch1"))

            val state = viewModel.uiState.value
            assertTrue(state.watcherEnabled)
            assertEquals(3, state.watchers?.limit)
            assertEquals("아침 6시 기상", state.challengeTitle)
        }

    @Test
    fun `감시자 벌칙이 꺼진 방은 목록을 묻지 않고 안내로 선다`() =
        runTest {
            val watchers = FakeWatcherRepository()
            val viewModel = viewModel(watcherEnabled = false, watchers = watchers)

            viewModel.onIntent(ChallengeWatchersIntent.Load("ch1"))

            assertFalse(viewModel.uiState.value.watcherEnabled)
            assertFalse(viewModel.uiState.value.isLoading)
            assertTrue(watchers.calls.isEmpty())
        }

    @Test
    fun `초대를 만들면 카카오톡 공유를 띄우고 목록을 다시 묻는다`() =
        runTest {
            val watchers =
                FakeWatcherRepository(
                    watchers = { ChallengeWatchers(limit = 3, watchers = emptyList()) },
                    invitation = {
                        WatcherInvitation(
                            invitationId = "i1",
                            token = "t1",
                            inviteUrl = "https://example.com/w/t1",
                            expiresAt = null,
                            kakaoShare = null,
                        )
                    },
                )
            val viewModel = viewModel(watcherEnabled = true, watchers = watchers)
            val effects = mutableListOf<ChallengeWatchersEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }
            viewModel.onIntent(ChallengeWatchersIntent.Load("ch1"))

            viewModel.onIntent(ChallengeWatchersIntent.Invite)

            assertEquals("https://example.com/w/t1", assertIs<ChallengeWatchersEffect.ShareInvite>(effects.single()).inviteUrl)
            assertEquals(listOf("getWatchers", "createInvitation", "getWatchers"), watchers.calls)
        }

    private fun viewModel(
        watcherEnabled: Boolean,
        watchers: FakeWatcherRepository,
    ) = ChallengeWatchersViewModel(
        challengeRepository = FakeChallengeRepository(detail = { detail(watcherEnabled) }),
        watcherRepository = watchers,
        navigationHelper = RecordingNavigationHelper(),
    )

    private fun detail(watcherEnabled: Boolean) =
        ChallengeDetail(
            title = "아침 6시 기상",
            category = Category.entries.first(),
            imageUrl = null,
            challengeId = "ch1",
            description = null,
            mode = ChallengeMode.GROUP,
            visibility = ChallengeVisibility.PUBLIC,
            status = ChallengeStatus.ACTIVE,
            owner = null,
            ownerType = OwnerType.USER,
            participantCount = 3,
            capacity = 4,
            isFull = false,
            period = ChallengePeriod(start = "2026-09-01", end = "2026-10-01"),
            verification =
                VerificationConfig(
                    type = VerificationType.entries.first(),
                    method = VerificationMethod.entries.first(),
                ),
            stats = ChallengeStats(completionRate = null, retentionRate = null),
            gate = ChallengeGate(minTier = null, myDisplayTier = null, eligible = true),
            joinBlockReason = null,
            rejoinAvailableAt = null,
            joinNote = JoinNote.IMMEDIATE,
            cloneable = false,
            myRole = MemberRole.MEMBER,
            moderation = null,
            penalties = ChallengePenalties(score = false, groupShare = false, watcher = watcherEnabled),
        )
}
