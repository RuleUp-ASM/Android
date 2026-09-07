package com.ruleup.challenge.presentation.targets.viewmodel

import com.ruleup.challenge.presentation.fake.FakeTargetAppStore
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.verification.domain.entity.MyScreenApps
import com.ruleup.verification.domain.entity.PendingScreenApps
import com.ruleup.verification.domain.entity.ScreenApp
import com.ruleup.verification.domain.entity.ScreenAppsUpdate
import com.ruleup.verification.domain.entity.SettingChangeLimitException
import com.ruleup.verification.domain.test.FakeVerificationRepository
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
 * 대상 앱 등록. 이 화면의 저장은 **월 1회**만 되고 **내일부터** 적용된다 — 둘 다 모르면 사용자가
 * 오늘부터 측정되는 줄 알거나, 막힌 줄 모르고 계속 눌러 본다. 그래서 안내 문구가 곧 기능이다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChallengeTargetsViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `적용 대기 중인 세트가 있으면 그쪽을 시드로 복원한다`() =
        runTest {
            // 사용자가 마지막으로 고른 것이 대기 세트다. 적용 중인 세트를 보여 주면 방금 고른 게 사라진다.
            val viewModel =
                viewModel(
                    FakeVerificationRepository(
                        myScreenApps = {
                            MyScreenApps(
                                apps = listOf(app("com.old")),
                                appliedFrom = "2026-08-01T00:00:00Z",
                                pending = PendingScreenApps(apps = listOf(app("com.new")), effectiveFrom = "2026-09-02T00:00:00Z"),
                            )
                        },
                    ),
                )

            viewModel.onIntent(ChallengeTargetsIntent.Load("ch1"))

            assertEquals(setOf("com.new"), viewModel.uiState.value.restoredPackages)
        }

    @Test
    fun `아직 등록한 적이 없으면 조용히 빈 상태로 시작한다`() =
        runTest {
            // 최초 진입이라 복원할 게 없는 것뿐이다 — 오류로 다루면 없던 문제를 보여 준다.
            val viewModel = viewModel(FakeVerificationRepository(myScreenApps = { null }))

            viewModel.onIntent(ChallengeTargetsIntent.Load("ch1"))

            assertEquals(emptySet<String>(), viewModel.uiState.value.restoredPackages)
        }

    @Test
    fun `복원 조회에 실패해도 화면을 막지 않는다`() =
        runTest {
            val viewModel = viewModel(FakeVerificationRepository(myScreenApps = { throw IllegalStateException("조회 실패") }))

            viewModel.onIntent(ChallengeTargetsIntent.Load("ch1"))

            assertEquals(emptySet<String>(), viewModel.uiState.value.restoredPackages)
        }

    @Test
    fun `하나도 고르지 않으면 서버에 묻지 않고 먼저 알린다`() =
        runTest {
            val repo = FakeVerificationRepository()
            val viewModel = viewModel(repo)
            val effects = collectEffects(viewModel)

            viewModel.onIntent(ChallengeTargetsIntent.Save("ch1", emptyList()))

            assertEquals(listOf(ChallengeTargetsEffect.ShowMessage("대상 앱을 1개 이상 선택해주세요")), effects)
            assertTrue(repo.calls.none { it == "updateMyScreenApps" })
        }

    @Test
    fun `저장에 성공하면 내일부터 적용된다는 것과 다음 변경 시점을 함께 알린다`() =
        runTest {
            // "등록됐어요" 로만 끝내면 오늘부터 측정되는 줄 알고, 월 1회 소진을 모르면 곧바로 또 바꾸려 한다.
            val viewModel =
                viewModel(
                    FakeVerificationRepository(
                        updateScreenApps = { _, _ ->
                            ScreenAppsUpdate(
                                apps = listOf(app("com.new")),
                                nextChangeAvailableAt = "2026-10-01T00:00:00Z",
                                appliedFrom = "2026-09-02T00:00:00Z",
                            )
                        },
                    ),
                )
            val effects = collectEffects(viewModel)

            viewModel.onIntent(ChallengeTargetsIntent.Save("ch1", listOf(app("com.new"))))

            val message = (effects.single() as ChallengeTargetsEffect.ShowMessage).message
            assertTrue(message.contains("내일부터"), message)
            assertTrue(message.contains("10월 1일"), message)
        }

    @Test
    fun `다음 변경 시점을 모르면 날짜를 지어내지 않는다`() =
        runTest {
            val viewModel =
                viewModel(
                    FakeVerificationRepository(
                        updateScreenApps = { _, _ ->
                            ScreenAppsUpdate(
                                apps = listOf(app("com.new")),
                                nextChangeAvailableAt = null,
                                appliedFrom = "2026-09-02T00:00:00Z",
                            )
                        },
                    ),
                )
            val effects = collectEffects(viewModel)

            viewModel.onIntent(ChallengeTargetsIntent.Save("ch1", listOf(app("com.new"))))

            assertEquals(
                listOf(ChallengeTargetsEffect.ShowMessage("대상 앱이 등록됐어요. 내일부터 적용돼요")),
                effects,
            )
        }

    @Test
    fun `저장에 성공한 뒤에만 등록됨으로 남기고 화면을 떠난다`() =
        runTest {
            val store = FakeTargetAppStore()
            val nav = RecordingNavigationHelper()
            val viewModel =
                viewModel(
                    FakeVerificationRepository(
                        updateScreenApps = { _, _ ->
                            ScreenAppsUpdate(apps = listOf(app("com.accepted")), appliedFrom = "2026-09-02T00:00:00Z")
                        },
                    ),
                    store,
                    nav,
                )

            viewModel.onIntent(ChallengeTargetsIntent.Save("ch1", listOf(app("com.picked"))))

            // 서버가 접수한 세트를 남긴다 — 고른 것과 다를 수 있다.
            assertEquals(listOf("com.accepted"), store.registered("ch1"))
            assertEquals(1, nav.backCount)
        }

    @Test
    fun `저장에 실패하면 등록됨으로 남기지 않고 화면에 머문다`() =
        runTest {
            val store = FakeTargetAppStore()
            val nav = RecordingNavigationHelper()
            val viewModel =
                viewModel(
                    FakeVerificationRepository(updateScreenApps = { _, _ -> throw IllegalStateException("서버 오류") }),
                    store,
                    nav,
                )

            viewModel.onIntent(ChallengeTargetsIntent.Save("ch1", listOf(app("com.picked"))))

            assertEquals(emptyList(), store.registered("ch1"))
            assertTrue(nav.didNotMove)
        }

    @Test
    fun `이번 달 변경을 다 썼으면 잠시 후 다시 시도하라고 하지 않는다`() =
        runTest {
            // 기본 문구로 뭉개면 사용자가 계속 눌러 본다. 다음 달까지 기다려야 하는 실패다.
            val viewModel =
                viewModel(FakeVerificationRepository(updateScreenApps = { _, _ -> throw SettingChangeLimitException() }))
            val effects = collectEffects(viewModel)

            viewModel.onIntent(ChallengeTargetsIntent.Save("ch1", listOf(app("com.picked"))))

            val message = (effects.single() as ChallengeTargetsEffect.ShowMessage).message
            assertTrue(message.contains("변경 횟수"), message)
            assertTrue(!message.contains("잠시 후"), message)
        }

    @Test
    fun `뒤로 가기는 이동 없이 화면만 닫는다`() {
        val nav = RecordingNavigationHelper()

        viewModel(nav = nav).onIntent(ChallengeTargetsIntent.Back)

        assertEquals(1, nav.backCount)
        assertEquals(emptyList(), nav.routes)
    }

    private fun TestScope.collectEffects(viewModel: ChallengeTargetsViewModel): List<ChallengeTargetsEffect> {
        val effects = mutableListOf<ChallengeTargetsEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }
        return effects
    }

    private fun viewModel(
        repo: FakeVerificationRepository = FakeVerificationRepository(),
        store: FakeTargetAppStore = FakeTargetAppStore(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = ChallengeTargetsViewModel(verificationRepository = repo, targetAppStore = store, navigationHelper = nav)

    private fun app(packageName: String) = ScreenApp(packageName = packageName, appName = packageName)
}
