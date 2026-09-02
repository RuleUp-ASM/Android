package com.ruleup.verification.presentation.location.viewmodel

import com.ruleup.domain.test.FakeTokenRepository
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.verification.domain.entity.GeofenceTarget
import com.ruleup.verification.domain.entity.Place
import com.ruleup.verification.domain.repository.GeofenceRegister
import com.ruleup.verification.domain.test.FakeVerificationRepository
import com.ruleup.verification.domain.usecase.BindLocationUseCase
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
 * 지오펜스 좌표 바인딩. 이 화면이 저장하는 값이 **매일 인증의 기준점**이 되므로, 잘못 저장되면
 * 사용자는 매일 실패하고 그 원인을 알 수 없다.
 *
 * 그래서 앵커가 하나도 없으면 서버에 보내지 않고, 변경 잠금에 걸렸으면 왕복 전에 알린다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VerificationLocationViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `앵커가 하나도 없으면 서버에 보내지 않고 먼저 알린다`() =
        runTest {
            // 빈 앵커로 저장하면 인증 기준점이 없어 매일 실패한다.
            val repo = FakeVerificationRepository()
            val viewModel = viewModel(repo)
            val effects = collectEffects(viewModel)

            viewModel.onIntent(VerificationLocationIntent.Submit("ch1", dwellMinutes = 10, targetPackages = emptyList()))

            assertEquals(
                listOf<VerificationLocationEffect>(VerificationLocationEffect.ShowMessage("앵커를 1개 이상 추가해 주세요")),
                effects,
            )
            assertTrue(repo.calls.isEmpty())
        }

    @Test
    fun `빈 검색어로는 서버를 두드리지 않는다`() =
        runTest {
            // 타이핑 중 지우는 순간마다 요청이 나가면 낭비다.
            val repo = FakeVerificationRepository(places = { emptyList() })
            val viewModel = viewModel(repo)

            viewModel.onIntent(VerificationLocationIntent.Search(query = "   "))

            assertTrue(repo.calls.none { it == "searchPlaces" })
        }

    @Test
    fun `검색하면 결과를 목록에 올린다`() =
        runTest {
            val viewModel = viewModel(FakeVerificationRepository(places = { listOf(place("헬스장")) }))

            viewModel.onIntent(VerificationLocationIntent.Search(query = "헬스"))

            assertEquals(
                listOf("헬스장"),
                viewModel.uiState.value.places
                    .map { it.name },
            )
        }

    @Test
    fun `검색에 실패하면 목록을 비우고 사유를 알린다`() =
        runTest {
            // 실패했는데 직전 결과가 남아 있으면 사용자가 엉뚱한 장소를 고른다.
            val viewModel = viewModel(FakeVerificationRepository(places = { throw IllegalStateException("검색 실패") }))
            val effects = collectEffects(viewModel)

            viewModel.onIntent(VerificationLocationIntent.Search(query = "헬스"))

            assertEquals(emptyList(), viewModel.uiState.value.places)
            assertTrue(effects.isNotEmpty())
        }

    @Test
    fun `검색 결과를 지우면 목록도 비운다`() =
        runTest {
            val viewModel = viewModel(FakeVerificationRepository(places = { listOf(place("헬스장")) }))
            viewModel.onIntent(VerificationLocationIntent.Search(query = "헬스"))

            viewModel.onIntent(VerificationLocationIntent.ClearSearch)

            assertEquals(emptyList(), viewModel.uiState.value.places)
        }

    @Test
    fun `뒤로 가기는 이동 없이 화면만 닫는다`() {
        val nav = RecordingNavigationHelper()

        viewModel(nav = nav).onIntent(VerificationLocationIntent.Back)

        assertEquals(1, nav.backCount)
        assertEquals(emptyList(), nav.routes)
    }

    private fun TestScope.collectEffects(viewModel: VerificationLocationViewModel): List<VerificationLocationEffect> {
        val effects = mutableListOf<VerificationLocationEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }
        return effects
    }

    private fun place(name: String) = Place(name = name, lat = 37.5, lng = 127.0, address = "서울시", category = null)

    private fun viewModel(
        repo: FakeVerificationRepository = FakeVerificationRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = VerificationLocationViewModel(
        verificationRepository = repo,
        // 이 테스트는 저장까지 가지 않는다 — 앵커가 없거나 검색 단계에서 끝나는 규칙만 본다.
        bindLocationUseCase = BindLocationUseCase(NoGeofenceRegister, FakeTokenRepository(storedUserId = "u1")),
        navigationHelper = nav,
    )
}

/** 지오펜스 등록은 이 테스트의 관심사가 아니다 — 불리면 그 자체가 의도치 않은 호출이다. */
private object NoGeofenceRegister : GeofenceRegister {
    override suspend fun reconcile(targets: List<GeofenceTarget>) = throw NotImplementedError()

    override suspend fun reconcilePersisted() = throw NotImplementedError()

    override suspend fun bind(
        requestIdPrefix: String,
        targets: List<GeofenceTarget>,
    ) = throw NotImplementedError()

    override suspend fun unbind(requestIdPrefix: String) = throw NotImplementedError()

    override suspend fun clear() = throw NotImplementedError()
}
