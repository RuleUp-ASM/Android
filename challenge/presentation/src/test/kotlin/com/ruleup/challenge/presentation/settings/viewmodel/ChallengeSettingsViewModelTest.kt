package com.ruleup.challenge.presentation.settings.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeConfig
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeField
import com.ruleup.challenge.domain.entity.ChallengeGate
import com.ruleup.challenge.domain.entity.ChallengeLimits
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengeModeration
import com.ruleup.challenge.domain.entity.ChallengePenalties
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeSettings
import com.ruleup.challenge.domain.entity.ChallengeStats
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.ChallengeUpdate
import com.ruleup.challenge.domain.entity.ChallengeUpdateResult
import com.ruleup.challenge.domain.entity.ChallengeVersionConflictException
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.JoinNote
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.ModerationState
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.fake.FakeChallengeRepository
import com.ruleup.domain.entity.category.Category
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
 * 챌린지 수정(방장 전용). 저장은 남의 방을 바꾸는 일이라 **실패했을 때 화면을 떠나지 않는 것**과,
 * 남이 먼저 고쳤을 때 **덮어쓰지 않고 다시 읽어 오는 것**이 계약이다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChallengeSettingsViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `불러오면 지금 설정을 편집 상태로 올린다`() =
        runTest {
            val viewModel = viewModel(repo())

            viewModel.onIntent(ChallengeSettingsIntent.Load("ch1"))

            assertEquals("아침 6시 기상", viewModel.uiState.value.title)
        }

    @Test
    fun `현재 인원을 못 받아도 편집을 막지 않는다`() =
        runTest {
            // 하한만 못 잠글 뿐이고 서버가 CAPACITY_BELOW_CURRENT 로 최종 방어한다.
            val viewModel =
                viewModel(
                    FakeChallengeRepository(
                        settings = { settings() },
                        detail = { throw IllegalStateException("상세 조회 실패") },
                    ),
                )

            viewModel.onIntent(ChallengeSettingsIntent.Load("ch1"))

            assertEquals("아침 6시 기상", viewModel.uiState.value.title)
        }

    @Test
    fun `설정 조회에 실패하면 사유를 남긴다`() =
        runTest {
            val viewModel = viewModel(FakeChallengeRepository(settings = { throw IllegalStateException("설정 오류") }))

            viewModel.onIntent(ChallengeSettingsIntent.Load("ch1"))

            assertEquals("설정 오류", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `바꾼 게 없으면 저장하지 않는다`() =
        runTest {
            val repo = repo()
            val viewModel = viewModel(repo)
            viewModel.onIntent(ChallengeSettingsIntent.Load("ch1"))

            viewModel.onIntent(ChallengeSettingsIntent.Save)

            assertTrue(repo.calls.none { it == "update" })
        }

    @Test
    fun `정원은 현재 인원보다 작게 줄일 수 없다`() =
        runTest {
            // 이미 들어와 있는 사람을 밀어낼 수 없다.
            val viewModel = viewModel(repo(participantCount = 5))
            viewModel.onIntent(ChallengeSettingsIntent.Load("ch1"))

            viewModel.onIntent(ChallengeSettingsIntent.SetCapacity(2))

            assertEquals(5, viewModel.uiState.value.capacity)
        }

    @Test
    fun `정원은 상한을 넘길 수 없다`() =
        runTest {
            val viewModel = viewModel(repo())
            viewModel.onIntent(ChallengeSettingsIntent.Load("ch1"))

            viewModel.onIntent(ChallengeSettingsIntent.SetCapacity(ChallengeLimits.CAPACITY_MAX + 1))

            assertEquals(ChallengeLimits.CAPACITY_MAX, viewModel.uiState.value.capacity)
        }

    @Test
    fun `제목을 바꿔 저장하면 알리고 화면을 떠난다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(repo(), nav)
            viewModel.onIntent(ChallengeSettingsIntent.Load("ch1"))
            viewModel.onIntent(ChallengeSettingsIntent.SetTitle("새 제목"))

            viewModel.onIntent(ChallengeSettingsIntent.Save)

            assertEquals(1, nav.backCount)
        }

    @Test
    fun `새 사진을 골랐으면 먼저 올린 뒤 저장한다`() =
        runTest {
            // 서버가 발급 주체를 검증하므로 URL 을 먼저 확보해야 한다.
            val repo = repo(uploadImage = { "https://cdn/uploaded.png" })
            val viewModel = viewModel(repo)
            viewModel.onIntent(ChallengeSettingsIntent.Load("ch1"))
            viewModel.onIntent(ChallengeSettingsIntent.SetCoverImage("content://picked"))

            viewModel.onIntent(ChallengeSettingsIntent.Save)

            assertEquals(listOf("getSettings", "getChallenge", "uploadImage", "update"), repo.calls)
        }

    @Test
    fun `저장에 실패하면 화면을 떠나지 않는다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(repo(update = { throw IllegalStateException("저장 실패") }), nav)
            viewModel.onIntent(ChallengeSettingsIntent.Load("ch1"))
            viewModel.onIntent(ChallengeSettingsIntent.SetTitle("새 제목"))

            viewModel.onIntent(ChallengeSettingsIntent.Save)

            assertEquals(0, nav.backCount)
            assertFalse(viewModel.uiState.value.isSaving)
        }

    @Test
    fun `남이 먼저 고쳤으면 덮어쓰지 않고 서버 기준으로 다시 읽는다`() =
        runTest {
            // 버전이 어긋난 채로 다시 누르면 남의 수정을 지운다 — 재조회로 최신 범위·버전을 받는다.
            val nav = RecordingNavigationHelper()
            val repo = repo(update = { throw ChallengeVersionConflictException() })
            val viewModel = viewModel(repo, nav)
            viewModel.onIntent(ChallengeSettingsIntent.Load("ch1"))
            viewModel.onIntent(ChallengeSettingsIntent.SetTitle("새 제목"))

            viewModel.onIntent(ChallengeSettingsIntent.Save)

            assertEquals(2, repo.calls.count { it == "getSettings" })
            assertEquals(0, nav.backCount)
        }

    @Test
    fun `뒤로 가기는 이동 없이 화면만 닫는다`() {
        val nav = RecordingNavigationHelper()

        viewModel(repo(), nav).onIntent(ChallengeSettingsIntent.Back)

        assertEquals(1, nav.backCount)
        assertEquals(emptyList(), nav.routes)
    }

    private fun repo(
        participantCount: Int = 1,
        uploadImage: ((String) -> String)? = { "https://cdn/uploaded.png" },
        update: ((ChallengeUpdate) -> ChallengeUpdateResult)? = {
            ChallengeUpdateResult(challengeId = "ch1", moderation = null, updatedFields = emptySet())
        },
    ) = FakeChallengeRepository(
        settings = { settings() },
        detail = { detail(participantCount) },
        update = update,
        uploadImage = uploadImage,
    )

    private fun settings() =
        ChallengeSettings(
            config =
                ChallengeConfig(
                    title = "아침 6시 기상",
                    description = "매일 아침",
                    imageUrl = null,
                    category = Category.entries.first(),
                    mode = ChallengeMode.GROUP,
                    visibility = ChallengeVisibility.PUBLIC,
                    rankingVisible = null,
                    capacity = 10,
                    minTier = null,
                    period = ChallengePeriod(start = "2026-09-01", end = "2026-10-01"),
                    weeklyCount = 5,
                    params = emptyList(),
                    verification =
                        VerificationConfig(
                            type = VerificationType.entries.first(),
                            method = VerificationMethod.entries.first(),
                        ),
                    penalties = ChallengePenalties(score = true, groupShare = true, watcher = false),
                ),
            editableFields = ChallengeField.entries.toSet(),
            version = 1,
            moderation =
                ChallengeModeration(
                    title = ModerationState.APPROVED,
                    description = ModerationState.APPROVED,
                    image = ModerationState.APPROVED,
                ),
        )

    /** 이 화면이 상세에서 쓰는 건 참여 인원 하나뿐이다 — 나머지는 형식만 채운다. */
    private fun detail(participantCount: Int) =
        ChallengeDetail(
            challengeId = "ch1",
            title = "아침 6시 기상",
            description = null,
            imageUrl = null,
            category = Category.entries.first(),
            mode = ChallengeMode.GROUP,
            visibility = ChallengeVisibility.PUBLIC,
            status = ChallengeStatus.ACTIVE,
            owner = null,
            ownerType = OwnerType.USER,
            participantCount = participantCount,
            capacity = 10,
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
            myRole = MemberRole.OWNER,
            moderation = null,
        )

    private fun viewModel(
        repo: FakeChallengeRepository = FakeChallengeRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = ChallengeSettingsViewModel(challengeRepository = repo, navigationHelper = nav)
}
