package com.ruleup.challenge.presentation.create.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeDraft
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengePenalties
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.entity.MyChallengeSummary
import com.ruleup.challenge.domain.entity.RecommendationRateLimitedException
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.fake.FakeChallengeRepository
import com.ruleup.challenge.domain.repository.MyChallengeStore
import com.ruleup.challenge.domain.repository.SetupNotifier
import com.ruleup.challenge.domain.usecase.CreateChallengeUseCase
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.observability.domain.test.testObservability
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 챌린지 생성. 초안을 서버가 만들어 주는 흐름이라 **실패의 종류가 결과를 가른다** —
 * AI 가 못 만든 것(폴백)과 너무 자주 부른 것(쿨다운)은 사용자가 취할 행동이 다르다.
 * 하나로 뭉개면 기다려야 할 사람이 계속 누르고, 다시 쓰면 되는 사람이 기다린다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateChallengeViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `설명이 비어 있으면 초안을 만들지 않는다`() =
        runTest {
            val repo = FakeChallengeRepository(draftResult = ok())
            val viewModel = viewModel(repo)

            viewModel.onIntent(CreateChallengeIntent.SubmitDescription)

            assertTrue(repo.calls.none { it == "createDraft" })
        }

    @Test
    fun `설명을 적어 보내면 초안을 화면에 올린다`() =
        runTest {
            val viewModel = viewModel(FakeChallengeRepository(draftResult = ok()))
            viewModel.onIntent(CreateChallengeIntent.SetRoutineDescription("평일 아침 헬스장에 가고 싶어요"))

            viewModel.onIntent(CreateChallengeIntent.SubmitDescription)

            assertEquals("아침 6시 기상", viewModel.uiState.value.title)
            assertNotNull(viewModel.uiState.value.draftId)
        }

    @Test
    fun `AI 가 못 만들면 폴백 안내로 떨어뜨린다`() =
        runTest {
            // 실패가 아니라 "직접 채워 달라"는 안내다 — 오류로 다루면 사용자가 포기한다.
            val viewModel =
                viewModel(FakeChallengeRepository(draftResult = DraftResult.Fallback("직접 입력해 주세요")))
            viewModel.onIntent(CreateChallengeIntent.SetRoutineDescription("뭔가 애매한 설명"))

            viewModel.onIntent(CreateChallengeIntent.SubmitDescription)

            assertEquals("직접 입력해 주세요", viewModel.uiState.value.fallbackMessage)
        }

    @Test
    fun `너무 자주 부르면 쿨다운으로 구분해 잠근다`() =
        runTest {
            // 폴백과 달리 "다시 쓰면" 되는 게 아니라 기다려야 한다 — 뭉개면 계속 누르게 된다.
            val viewModel =
                viewModel(
                    FakeChallengeRepository(draftError = RecommendationRateLimitedException(retryAfterSeconds = 30)),
                )
            viewModel.onIntent(CreateChallengeIntent.SetRoutineDescription("평일 아침 헬스장"))

            viewModel.onIntent(CreateChallengeIntent.SubmitDescription)

            assertEquals(30, viewModel.uiState.value.retryAfterSeconds)
        }

    @Test
    fun `쿨다운 중에는 다시 보내지 않는다`() =
        runTest {
            val repo = FakeChallengeRepository(draftError = RecommendationRateLimitedException(retryAfterSeconds = 30))
            val viewModel = viewModel(repo)
            viewModel.onIntent(CreateChallengeIntent.SetRoutineDescription("평일 아침 헬스장"))
            viewModel.onIntent(CreateChallengeIntent.SubmitDescription)
            val before = repo.calls.count { it == "createDraft" }

            viewModel.onIntent(CreateChallengeIntent.SubmitDescription)

            assertEquals(before, repo.calls.count { it == "createDraft" })
        }

    @Test
    fun `초안이 없으면 만들지 않는다`() =
        runTest {
            // 초안 없이 보내면 서버가 튕기고 사용자는 왜인지 모른 채 확인 화면에 갇힌다.
            val repo = FakeChallengeRepository()
            val viewModel = viewModel(repo)

            viewModel.onIntent(CreateChallengeIntent.Create)

            assertTrue(repo.calls.none { it == "create" })
        }

    private fun TestScope.collectEffects(viewModel: CreateChallengeViewModel): List<CreateChallengeEffect> {
        val effects = mutableListOf<CreateChallengeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }
        return effects
    }

    private fun ok() =
        DraftResult.Ok(
            draftId = "draft-1",
            draft =
                ChallengeDraft(
                    title = "아침 6시 기상",
                    description = "매일 아침 6시에 일어나기",
                    category = Category.entries.first(),
                    mode = ChallengeMode.SOLO,
                    visibility = null,
                    rankingVisible = true,
                    capacity = 1,
                    minTier = null,
                    period = ChallengePeriod(start = "2026-09-01", end = "2026-10-01"),
                    weeklyCount = 5,
                    params = emptyList(),
                    verification =
                        VerificationConfig(
                            type = VerificationType.entries.first(),
                            method = VerificationMethod.entries.first(),
                        ),
                    penalties = ChallengePenalties(score = true, groupShare = false, watcher = false),
                ),
        )

    private fun viewModel(
        repo: FakeChallengeRepository = FakeChallengeRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = CreateChallengeViewModel(
        createChallengeUseCase = CreateChallengeUseCase(repo, NoSetupNotifier),
        challengeRepository = repo,
        myChallengeStore = RecordingChallengeStore(),
        navigationHelper = nav,
        observability = testObservability(),
    )
}

/** 셋업 알림은 이 테스트의 관심사가 아니다 — 생성이 성공한 뒤에야 불린다. */
private object NoSetupNotifier : SetupNotifier {
    override fun notifyAfterCreate(
        challengeId: String,
        title: String,
        verification: VerificationConfig,
        personalSetupRequired: Boolean,
    ) = Unit
}

/** 생성 직후 홈이 서버보다 먼저 아는 챌린지를 담는 곳. 무엇이 담겼는지가 계약이라 기록한다. */
private class RecordingChallengeStore : MyChallengeStore {
    val added = mutableListOf<MyChallengeSummary>()

    override fun all(): List<MyChallengeSummary> = added

    override fun add(summary: MyChallengeSummary) {
        added += summary
    }
}
