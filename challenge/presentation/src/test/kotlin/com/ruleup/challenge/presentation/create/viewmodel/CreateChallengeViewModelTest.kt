package com.ruleup.challenge.presentation.create.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.ruleup.challenge.domain.entity.ChallengeDraft
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengeModeration
import com.ruleup.challenge.domain.entity.ChallengePenalties
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.CreatedChallenge
import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.entity.ModerationState
import com.ruleup.challenge.domain.entity.MyChallengeSummary
import com.ruleup.challenge.domain.entity.RecommendationRateLimitedException
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.fake.FakeChallengeRepository
import com.ruleup.challenge.domain.repository.MyChallengeStore
import com.ruleup.challenge.domain.repository.SetupNotifier
import com.ruleup.challenge.domain.usecase.CreateChallengeUseCase
import com.ruleup.challenge.presentation.fake.FakeAccountRepository
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.logging.domain.test.RecordingBizLogger
import com.ruleup.onboarding.domain.fake.FakeIntroRepository
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.domain.repository.PermissionStatusProvider
import com.ruleup.verification.domain.usecase.AgreeVerificationConsentUseCase
import com.ruleup.verification.domain.usecase.CheckVerificationAccessUseCase
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 챌린지 생성. */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateChallengeViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `만들지 않고 나가면 입력한 설명을 지우고 다시 들어와도 남지 않는다`() =
        runTest {
            // ViewModel 이 액티비티 범위라 지우지 않으면 다음 진입에 지난 설명이 그대로 보인다(#589).
            val nav = RecordingNavigationHelper()
            val saved = SavedStateHandle()
            val viewModel = viewModel(nav = nav, saved = saved)
            viewModel.onIntent(CreateChallengeIntent.SetRoutineDescription("매일 아침 6시에 일어나기"))

            viewModel.onIntent(CreateChallengeIntent.Exit)

            assertEquals("", viewModel.uiState.value.routineDescription)
            assertEquals(1, nav.backCount)
            // 프로세스가 되살아나도 복원되지 않는다
            assertEquals("", viewModel(saved = saved).uiState.value.routineDescription)
        }

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
            // 실패가 아니라 "직접 채워 달라"는 안내다
            val viewModel =
                viewModel(FakeChallengeRepository(draftResult = DraftResult.Fallback("직접 입력해 주세요")))
            viewModel.onIntent(CreateChallengeIntent.SetRoutineDescription("뭔가 애매한 설명"))

            viewModel.onIntent(CreateChallengeIntent.SubmitDescription)

            assertEquals("직접 입력해 주세요", viewModel.uiState.value.fallbackMessage)
        }

    @Test
    fun `너무 자주 부르면 쿨다운으로 구분해 잠근다`() =
        runTest {
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
    fun `복제한 초안은 확인 화면에서 한 번만 적용한다`() =
        runTest {
            val pending =
                com.ruleup.challenge.presentation.create
                    .PendingChallengeDraft()
            pending.put(ok())
            val vm = viewModel(pending = pending)
            vm.onIntent(CreateChallengeIntent.ConfirmOpened)
            assertEquals("draft-1", vm.uiState.value.draftId)
            vm.onIntent(CreateChallengeIntent.SetTitle("수정한 제목"))
            vm.onIntent(CreateChallengeIntent.ConfirmOpened)
            assertEquals("수정한 제목", vm.uiState.value.title)
        }

    @Test
    fun `대기 시간이 없는 429도 재요청을 제한한다`() =
        runTest {
            val repo = FakeChallengeRepository(draftError = RecommendationRateLimitedException())
            val vm = viewModel(repo)
            vm.onIntent(CreateChallengeIntent.SetRoutineDescription("평일 아침 헬스장"))
            vm.onIntent(CreateChallengeIntent.SubmitDescription)
            assertEquals(60, vm.uiState.value.retryAfterSeconds)
            vm.onIntent(CreateChallengeIntent.SubmitDescription)
            assertEquals(1, repo.calls.count { it == "createDraft" })
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

    @Test
    fun `프로세스가 죽었다 살아나도 적던 루틴 설명이 남는다`() =
        runTest {
            // 입력이 사라지면 사용자가 문장을 처음부터 다시 쳐야 한다(ENV-05).
            val viewModel = viewModel(saved = SavedStateHandle(mapOf("routineDescription" to "평일 아침 7시 러닝")))

            assertEquals("평일 아침 7시 러닝", viewModel.uiState.value.routineDescription)
        }

    private fun TestScope.collectEffects(viewModel: CreateChallengeViewModel): List<CreateChallengeEffect> {
        val effects = mutableListOf<CreateChallengeEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }
        return effects
    }

    @Test
    fun `서버가 위치와 건강 권한을 요구하면 두 동의를 모두 받은 뒤 생성한다`() =
        runTest {
            val tokens = listOf("ACCESS_FINE_LOCATION", "READ_STEPS")
            val repo = FakeChallengeRepository(draftResult = ok(tokens), created = created(tokens))
            val account = FakeAccountRepository()
            val model = viewModel(repo = repo, account = account)
            model.onIntent(CreateChallengeIntent.SetRoutineDescription("아침 활동"))
            model.onIntent(CreateChallengeIntent.SubmitDescription)

            model.onIntent(CreateChallengeIntent.Create)
            assertEquals(
                listOf(AgreementType.LOCATION_INFO, AgreementType.HEALTH_INFO),
                model.uiState.value.pendingAccess
                    ?.missingConsents,
            )
            assertTrue("create" !in repo.calls)

            model.onIntent(CreateChallengeIntent.ConfirmVerificationAccess)
            assertEquals(listOf(AgreementType.LOCATION_INFO, AgreementType.HEALTH_INFO), account.submitted.map { it.type })
            assertEquals(1, repo.calls.count { it == "create" })
            assertNull(model.uiState.value.pendingAccess)
        }

    @Test
    fun `필요 권한이 없으면 인증 방식이 위치여도 동의를 추론하지 않고 생성한다`() =
        runTest {
            val draft =
                ok().let {
                    it.copy(
                        draft =
                            it.draft.copy(verification = it.draft.verification.copy(method = VerificationMethod.GPS_PRESENCE)),
                    )
                }
            val repo = FakeChallengeRepository(draftResult = draft, created = created(emptyList()))
            val model = viewModel(repo)
            model.onIntent(CreateChallengeIntent.SetRoutineDescription("아침 활동"))
            model.onIntent(CreateChallengeIntent.SubmitDescription)

            model.onIntent(CreateChallengeIntent.Create)

            assertNull(model.uiState.value.pendingAccess)
            assertEquals(1, repo.calls.count { it == "create" })
        }

    @Test
    fun `동의 저장이 실패하면 동의 시트를 유지하고 생성하지 않는다`() =
        runTest {
            val repo = FakeChallengeRepository(draftResult = ok(listOf("READ_STEPS")))
            val account = FakeAccountRepository(submitError = IllegalStateException("저장 실패"))
            val model = viewModel(repo = repo, account = account)
            val effects = collectEffects(model)
            model.onIntent(CreateChallengeIntent.SetRoutineDescription("아침 활동"))
            model.onIntent(CreateChallengeIntent.SubmitDescription)
            model.onIntent(CreateChallengeIntent.Create)

            model.onIntent(CreateChallengeIntent.ConfirmVerificationAccess)

            assertEquals(
                listOf(AgreementType.HEALTH_INFO),
                model.uiState.value.pendingAccess
                    ?.missingConsents,
            )
            assertTrue("create" !in repo.calls)
            assertTrue(effects.any { it is CreateChallengeEffect.ShowError })
        }

    @Test
    fun `자동 인증 초안을 수동으로 바꾸면 남아 있는 자동 권한으로 동의를 요구하지 않는다`() =
        runTest {
            val repo = FakeChallengeRepository(draftResult = ok(listOf("READ_STEPS")), created = created(emptyList()))
            val model = viewModel(repo)
            model.onIntent(CreateChallengeIntent.SetRoutineDescription("아침 활동"))
            model.onIntent(CreateChallengeIntent.SubmitDescription)
            model.onIntent(CreateChallengeIntent.SetVerificationType(VerificationType.MANUAL))

            model.onIntent(CreateChallengeIntent.Create)

            assertNull(model.uiState.value.pendingAccess)
            assertEquals(1, repo.calls.count { it == "create" })
        }

    @Test
    fun `동의를 한 번에 저장한 뒤 권한을 요청하고 거부하면 같은 설정을 유지한다`() =
        runTest {
            val tokens = listOf("ACCESS_FINE_LOCATION", "READ_STEPS")
            val repo = FakeChallengeRepository(draftResult = ok(tokens), created = created(tokens))
            val account = FakeAccountRepository()
            var permissions = snapshot().copy(location = PermissionState.DENIED, healthSteps = PermissionState.DENIED)
            val model = viewModel(repo = repo, account = account, permissionStatus = PermissionStatusProvider { permissions })
            val effects = collectEffects(model)
            model.onIntent(CreateChallengeIntent.SetRoutineDescription("아침 활동"))
            model.onIntent(CreateChallengeIntent.SubmitDescription)
            model.onIntent(CreateChallengeIntent.Create)
            assertTrue(effects.none { it is CreateChallengeEffect.RequestPermissions })

            model.onIntent(CreateChallengeIntent.ConfirmVerificationAccess)

            assertEquals(1, account.submissionBatches.size)
            assertEquals(2, account.submissionBatches.single().size)
            assertEquals(tokens, effects.filterIsInstance<CreateChallengeEffect.RequestPermissions>().single().tokens)
            assertTrue(
                model.uiState.value.pendingAccess!!
                    .missingConsents
                    .isEmpty(),
            )
            assertTrue("create" !in repo.calls)

            model.onIntent(CreateChallengeIntent.VerificationPermissionsReturned)
            assertNotNull(model.uiState.value.pendingAccess)
            assertTrue("create" !in repo.calls)
            assertEquals(1, effects.filterIsInstance<CreateChallengeEffect.RequestPermissions>().size)

            permissions = snapshot()
            model.onIntent(CreateChallengeIntent.VerificationPermissionsReturned)
            model.onIntent(CreateChallengeIntent.VerificationPermissionsReturned)
            assertNull(model.uiState.value.pendingAccess)
            assertEquals(1, repo.calls.count { it == "create" })
            assertEquals(1, account.submissionBatches.size)
        }

    @Test
    fun `동의 저장 실패 시 시스템 권한을 요청하지 않는다`() =
        runTest {
            val repo = FakeChallengeRepository(draftResult = ok(listOf("READ_STEPS")))
            val model =
                viewModel(
                    repo = repo,
                    account = FakeAccountRepository(submitError = IllegalStateException("저장 실패")),
                    permissionStatus = PermissionStatusProvider { snapshot().copy(healthSteps = PermissionState.DENIED) },
                )
            val effects = collectEffects(model)
            model.onIntent(CreateChallengeIntent.SetRoutineDescription("아침 활동"))
            model.onIntent(CreateChallengeIntent.SubmitDescription)
            model.onIntent(CreateChallengeIntent.Create)

            model.onIntent(CreateChallengeIntent.ConfirmVerificationAccess)

            assertNotNull(model.uiState.value.pendingAccess)
            assertTrue(effects.none { it is CreateChallengeEffect.RequestPermissions })
            assertTrue("create" !in repo.calls)
        }

    @Test
    fun `설정을 닫은 뒤 권한 결과가 늦게 도착해도 생성하지 않는다`() =
        runTest {
            val repo = FakeChallengeRepository(draftResult = ok(listOf("READ_STEPS")))
            val model = viewModel(repo)
            model.onIntent(CreateChallengeIntent.SetRoutineDescription("아침 활동"))
            model.onIntent(CreateChallengeIntent.SubmitDescription)
            model.onIntent(CreateChallengeIntent.Create)

            model.onIntent(CreateChallengeIntent.DismissVerificationAccess)
            model.onIntent(CreateChallengeIntent.VerificationPermissionsReturned)

            assertNull(model.uiState.value.pendingAccess)
            assertTrue("create" !in repo.calls)
        }

    private fun created(tokens: List<String>) =
        CreatedChallenge(
            challengeId = "ch1",
            status = ChallengeStatus.ACTIVE,
            moderation = ChallengeModeration(ModerationState.NONE, ModerationState.NONE, ModerationState.NONE),
            verification = VerificationConfig(VerificationType.AUTO, VerificationMethod.SELF_CHECK, requiredPermissions = tokens),
            personalSetupRequired = false,
            createdAt = "2026-09-28T00:00:00Z",
        )

    private fun ok(requiredPermissions: List<String> = emptyList()) =
        DraftResult.Ok(
            draftId = "draft-1",
            draft =
                ChallengeDraft(
                    title = "아침 6시 기상",
                    category = Category.entries.first(),
                    imageUrl = null,
                    description = "매일 아침 6시에 일어나기",
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
                            method = VerificationMethod.SELF_CHECK,
                            requiredPermissions = requiredPermissions,
                        ),
                    penalties = ChallengePenalties(score = true, groupShare = false, watcher = false),
                ),
        )

    private fun viewModel(
        repo: FakeChallengeRepository = FakeChallengeRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
        saved: SavedStateHandle = SavedStateHandle(),
        pending: com.ruleup.challenge.presentation.create.PendingChallengeDraft =
            com.ruleup.challenge.presentation.create
                .PendingChallengeDraft(),
        account: FakeAccountRepository = FakeAccountRepository(),
        permissionStatus: PermissionStatusProvider = PermissionStatusProvider { snapshot() },
    ) = CreateChallengeViewModel(
        pendingDraft = pending,
        createChallengeUseCase = CreateChallengeUseCase(repo, NoSetupNotifier),
        challengeRepository = repo,
        myChallengeStore = RecordingChallengeStore(),
        navigationHelper = nav,
        bizLogger = RecordingBizLogger(),
        savedStateHandle = saved,
        permissionStatusProvider = permissionStatus,
        checkVerificationAccess = CheckVerificationAccessUseCase(permissionStatus, account),
        agreeVerificationConsent = AgreeVerificationConsentUseCase(account, FakeIntroRepository()),
    )

    private fun snapshot() =
        PermissionSnapshot(
            location = PermissionState.GRANTED,
            backgroundLocation = PermissionState.GRANTED,
            usageStats = PermissionState.GRANTED,
            postNotifications = PermissionState.GRANTED,
            healthDistance = PermissionState.GRANTED,
            healthSteps = PermissionState.GRANTED,
            healthSleep = PermissionState.GRANTED,
            healthBackground = PermissionState.GRANTED,
        )
}

/** 셋업 알림은 이 테스트의 관심사가 아니다 */
private object NoSetupNotifier : SetupNotifier {
    override fun notifyAfterCreate(
        challengeId: String,
        title: String,
        verification: VerificationConfig,
        personalSetupRequired: Boolean,
    ) = Unit
}

/** 생성 직후 홈이 서버보다 먼저 아는 챌린지를 담는 곳. */
private class RecordingChallengeStore : MyChallengeStore {
    val added = mutableListOf<MyChallengeSummary>()

    override fun all(): List<MyChallengeSummary> = added

    override fun add(summary: MyChallengeSummary) {
        added += summary
    }
}
