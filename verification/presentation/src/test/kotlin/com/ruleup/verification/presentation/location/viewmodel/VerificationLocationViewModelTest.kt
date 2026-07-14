package com.ruleup.verification.presentation.location.viewmodel

import com.ruleup.analytics.domain.AnalyticsEvent
import com.ruleup.analytics.domain.AnalyticsLogger
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.NavSignal
import com.ruleup.domain.navigation.Page
import com.ruleup.domain.token.TokenRepository
import com.ruleup.entity.user.Token
import com.ruleup.verification.domain.entity.ChallengeSetupResult
import com.ruleup.verification.domain.entity.DeviceIntro
import com.ruleup.verification.domain.entity.EnvelopeMetadata
import com.ruleup.verification.domain.entity.GeofenceTarget
import com.ruleup.verification.domain.entity.LocationPin
import com.ruleup.verification.domain.entity.ManualMethod
import com.ruleup.verification.domain.entity.ManualSubmitResult
import com.ruleup.verification.domain.entity.MyLocation
import com.ruleup.verification.domain.entity.Place
import com.ruleup.verification.domain.entity.ProgressFilter
import com.ruleup.verification.domain.entity.ProgressSnapshot
import com.ruleup.verification.domain.entity.SetupStatus
import com.ruleup.verification.domain.entity.SignalBatch
import com.ruleup.verification.domain.entity.SyncPolicy
import com.ruleup.verification.domain.entity.SyncResult
import com.ruleup.verification.domain.entity.VerificationDetail
import com.ruleup.verification.domain.repository.GeofenceRegistrar
import com.ruleup.verification.domain.repository.VerificationRepository
import com.ruleup.verification.domain.usecase.BindLocationUseCase
import com.ruleup.verification.domain.usecase.GetMyLocationUseCase
import com.ruleup.verification.domain.usecase.ReverseGeocodeUseCase
import com.ruleup.verification.domain.usecase.SearchPlacesUseCase
import com.ruleup.verification.domain.usecase.SubmitChallengeSetupUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 이번 PR 에서 바뀐 부분만 검증한다:
 * - [VerificationLocationIntent.Back] → [NavigationHelper.navigateToBack] 호출(신규 인텐트).
 * - [VerificationLocationIntent.TapMap]·[VerificationLocationIntent.SelectPlace] 가 카카오 로컬
 *   category 를 [PendingSelection.category] 에 채워 넣는다(신규 필드).
 * - [VerificationLocationIntent.AddAnchor] 가 [PendingSelection.address] 를 [LocationPin.address] 로
 *   함께 담는다(신규 필드).
 * Submit/Search/Init 등 나머지 흐름은 이번 PR 에서 바뀌지 않았으므로 다루지 않는다.
 */
class VerificationLocationViewModelTest {
    private lateinit var verificationRepository: FakeVerificationRepository
    private lateinit var navigationHelper: FakeNavigationHelper
    private lateinit var viewModel: VerificationLocationViewModel

    @BeforeTest
    fun setup() {
        // viewModelScope 는 Dispatchers.Main 을 요구한다 — JVM 단위 테스트엔 기본 제공되지 않으므로 주입.
        Dispatchers.setMain(UnconfinedTestDispatcher())
        verificationRepository = FakeVerificationRepository()
        navigationHelper = FakeNavigationHelper()
        viewModel =
            VerificationLocationViewModel(
                getMyLocationUseCase = GetMyLocationUseCase(verificationRepository),
                submitChallengeSetupUseCase = SubmitChallengeSetupUseCase(verificationRepository),
                bindLocationUseCase =
                    BindLocationUseCase(FakeGeofenceRegistrar(), FakeTokenRepository(userId = "u1")),
                searchPlacesUseCase = SearchPlacesUseCase(verificationRepository),
                reverseGeocodeUseCase = ReverseGeocodeUseCase(verificationRepository),
                analyticsLogger = FakeAnalyticsLogger(),
                navigationHelper = navigationHelper,
            )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Back intent 는 화면 종료를 요청한다`() {
        viewModel.onIntent(VerificationLocationIntent.Back)

        assertEquals(1, navigationHelper.backCallCount)
    }

    @Test
    fun `Back intent 는 상태를 바꾸지 않는다`() {
        val before = viewModel.uiState.value

        viewModel.onIntent(VerificationLocationIntent.Back)

        assertEquals(before, viewModel.uiState.value)
    }

    @Test
    fun `TapMap 은 역지오코딩 결과의 category 와 address 를 pending 에 채운다`() =
        runTest {
            verificationRepository.reverseGeocodeResult =
                Place(name = "스포애니 강남점", lat = 37.1, lng = 127.1, address = "서울시 강남구", category = "스포츠,레저 > 헬스장")

            viewModel.onIntent(VerificationLocationIntent.TapMap(lat = 37.1, lng = 127.1))
            advanceUntilIdle()

            val pending = viewModel.uiState.value.pending
            assertEquals("스포애니 강남점", pending?.name)
            assertEquals("서울시 강남구", pending?.address)
            assertEquals("스포츠,레저 > 헬스장", pending?.category)
        }

    @Test
    fun `TapMap 역지오코딩 결과가 없으면 category address 는 null 이고 이름은 대체 문구다`() =
        runTest {
            verificationRepository.reverseGeocodeResult = null

            viewModel.onIntent(VerificationLocationIntent.TapMap(lat = 0.0, lng = 0.0))
            advanceUntilIdle()

            val pending = viewModel.uiState.value.pending
            assertEquals("선택한 위치", pending?.name)
            assertNull(pending?.address)
            assertNull(pending?.category)
        }

    @Test
    fun `SelectPlace 는 검색 결과의 category 를 pending 에 그대로 담는다`() {
        val place = Place(name = "이디야커피", lat = 37.2, lng = 127.2, address = "서울시 마포구", category = "음식점 > 카페")

        viewModel.onIntent(VerificationLocationIntent.SelectPlace(place))

        val pending = viewModel.uiState.value.pending
        assertEquals("음식점 > 카페", pending?.category)
        assertEquals("서울시 마포구", pending?.address)
    }

    @Test
    fun `SelectPlace 검색 결과에 category 가 없으면 pending 의 category 도 null 이다`() {
        val place = Place(name = "이름없는가게", lat = 37.2, lng = 127.2, address = null, category = null)

        viewModel.onIntent(VerificationLocationIntent.SelectPlace(place))

        assertNull(viewModel.uiState.value.pending?.category)
    }

    @Test
    fun `AddAnchor 는 pending 의 address 를 LocationPin address 로 담는다`() =
        runTest {
            verificationRepository.reverseGeocodeResult =
                Place(name = "헬스장", lat = 37.1, lng = 127.1, address = "서울시 강남구", category = "헬스장")
            viewModel.onIntent(VerificationLocationIntent.TapMap(lat = 37.1, lng = 127.1))
            advanceUntilIdle()

            viewModel.onIntent(VerificationLocationIntent.AddAnchor(radiusM = 600f))

            val anchor = viewModel.uiState.value.anchors.single()
            assertEquals("헬스장", anchor.label)
            assertEquals("서울시 강남구", anchor.address)
            assertEquals(600f, anchor.radiusM)
            // 앵커로 담으면 확인 대기 핀은 비워진다(기존 동작 유지 확인).
            assertNull(viewModel.uiState.value.pending)
        }

    @Test
    fun `AddAnchor 는 pending 에 address 가 없으면 LocationPin address 도 null 이다`() =
        runTest {
            verificationRepository.reverseGeocodeResult = null
            viewModel.onIntent(VerificationLocationIntent.TapMap(lat = 0.0, lng = 0.0))
            advanceUntilIdle()

            viewModel.onIntent(VerificationLocationIntent.AddAnchor(radiusM = 600f))

            val anchor = viewModel.uiState.value.anchors.single()
            assertNull(anchor.address)
        }

    @Test
    fun `AddAnchor 는 확인 대기 핀이 없으면 아무 것도 추가하지 않는다`() {
        viewModel.onIntent(VerificationLocationIntent.AddAnchor(radiusM = 600f))

        assertTrue(viewModel.uiState.value.anchors.isEmpty())
    }

    private class FakeNavigationHelper : NavigationHelper {
        var backCallCount = 0

        override val navigationFlow: Flow<NavSignal> = emptyFlow()

        override fun navigateByRoute(route: NavRoute) = Unit

        override fun navigateTo(page: Page) = Unit

        override fun navigateToBack() {
            backCallCount++
        }
    }

    private class FakeAnalyticsLogger : AnalyticsLogger {
        val events = mutableListOf<AnalyticsEvent>()

        override fun log(event: AnalyticsEvent) {
            events += event
        }

        override fun setUserId(id: String?) = Unit

        override fun setUserProperty(
            key: String,
            value: String,
        ) = Unit
    }

    private class FakeGeofenceRegistrar : GeofenceRegistrar {
        override suspend fun reconcile(targets: List<GeofenceTarget>) = Unit

        override suspend fun reconcilePersisted() = Unit

        override suspend fun bind(
            requestIdPrefix: String,
            targets: List<GeofenceTarget>,
        ) = Unit

        override suspend fun unbind(requestIdPrefix: String) = Unit

        override suspend fun clear() = Unit
    }

    private class FakeTokenRepository(
        private val userId: String?,
    ) : TokenRepository {
        override suspend fun saveTokens(token: Token) = Unit

        override suspend fun getAccessToken(): String? = null

        override fun cachedAccessToken(): String? = null

        override suspend fun getRefreshToken(): String? = null

        override suspend fun saveUserId(userId: String) = Unit

        override suspend fun getUserId(): String? = userId

        override suspend fun clear() = Unit

        override val isLoggedIn: Flow<Boolean> = flowOf(userId != null)
    }

    private class FakeVerificationRepository : VerificationRepository {
        var reverseGeocodeResult: Place? = null
        var setupChallengeResult: ChallengeSetupResult = ChallengeSetupResult(status = SetupStatus.READY, missing = emptyList())
        var searchPlacesResult: List<Place> = emptyList()

        override suspend fun submitIntro(intro: DeviceIntro): SyncPolicy = error("unused")

        override suspend fun sync(
            metadata: EnvelopeMetadata,
            batch: SignalBatch,
        ): SyncResult = error("unused")

        override suspend fun getProgress(filter: ProgressFilter): ProgressSnapshot = error("unused")

        override suspend fun getVerificationDetail(
            challengeId: String,
            logDays: Int,
        ): VerificationDetail = error("unused")

        override suspend fun setupChallenge(
            challengeId: String,
            anchors: List<LocationPin>,
            targetPackages: List<String>,
        ): ChallengeSetupResult = setupChallengeResult

        override suspend fun getMyLocation(challengeId: String): MyLocation? = null

        override suspend fun submitManual(
            challengeId: String,
            method: ManualMethod,
            targetDate: String?,
            imageUrl: String?,
            asFallback: Boolean,
        ): ManualSubmitResult = error("unused")

        override suspend fun searchPlaces(
            query: String,
            lat: Double?,
            lng: Double?,
            radiusM: Int?,
        ): List<Place> = searchPlacesResult

        override suspend fun reverseGeocode(
            lat: Double,
            lng: Double,
        ): Place? = reverseGeocodeResult
    }
}