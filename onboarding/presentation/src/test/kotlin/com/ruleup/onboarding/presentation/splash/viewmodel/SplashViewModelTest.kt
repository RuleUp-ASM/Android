package com.ruleup.onboarding.presentation.splash.viewmodel

import com.ruleup.domain.entity.user.TermsVersions
import com.ruleup.domain.entity.user.Token
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page
import com.ruleup.domain.navigation.PendingDeepLink
import com.ruleup.domain.navigation.RouteAccessPolicy
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.domain.token.RefreshedSession
import com.ruleup.observability.domain.test.testObservability
import com.ruleup.onboarding.domain.auth.usecase.AutoLoginUseCase
import com.ruleup.onboarding.domain.fake.FakeAuthRepository
import com.ruleup.onboarding.domain.fake.FakeIntroRepository
import com.ruleup.onboarding.domain.fake.FakeTokenRepository
import com.ruleup.onboarding.domain.intro.entity.AppVersionGate
import com.ruleup.onboarding.domain.intro.entity.IntroInfo
import com.ruleup.onboarding.domain.intro.usecase.LoadIntroUseCase
import com.ruleup.onboarding.domain.navigation.HomePage
import com.ruleup.onboarding.domain.navigation.LoginPage
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
import kotlin.test.assertTrue

/**
 * 앱 진입. 순서가 곧 계약이다 — 버전 게이트가 먼저고, 걸리면 자동 로그인도 하지 않는다.
 * 업데이트 전에는 어떤 화면도 열지 않으므로 세션을 되살릴 이유가 없다.
 *
 * 딥링크는 **인증보다 먼저 도착한다.** 인증 전이면 목적지를 버리고 로그인으로 보내되, 잃어버린
 * 진입을 로그로 남긴다 — 초대 링크로 들어온 신규 사용자가 여기 걸린다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `세션이 살아 있으면 홈으로 보낸다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            viewModel(nav = nav, refreshToken = "rt").onIntent(SplashIntent.Check)

            assertEquals(HomePage, nav.pages.single())
            assertTrue(nav.replaced.isEmpty())
        }

    @Test
    fun `세션이 없으면 로그인으로 보낸다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            viewModel(nav = nav, refreshToken = null).onIntent(SplashIntent.Check)

            assertEquals(LoginPage, nav.pages.single())
        }

    @Test
    fun `강제 업데이트에 걸리면 자동 로그인도 하지 않고 아무 데도 가지 않는다`() =
        runTest {
            // 업데이트 전에는 어떤 화면도 열지 않으므로 세션을 되살릴 이유가 없다.
            val nav = RecordingNavigationHelper()
            val tokens = FakeTokenRepository("rt")
            val viewModel = viewModel(nav = nav, forceUpdate = true, tokens = tokens)

            viewModel.onIntent(SplashIntent.Check)

            assertTrue(viewModel.uiState.value.forceUpdate)
            assertTrue(nav.didNotMove)
        }

    @Test
    fun `보류된 딥링크가 있으면 부모 화면까지 함께 깔아 목적지로 보낸다`() =
        runTest {
            // 단순 이동이면 뒤로가기가 앱을 곧장 닫는다 — 그래서 스택 교체다.
            val nav = RecordingNavigationHelper()
            val pending = PendingDeepLink().apply { set(NavRoute("challenge/detail", mapOf("challengeId" to "ch1"))) }

            viewModel(nav = nav, refreshToken = "rt", pendingDeepLink = pending).onIntent(SplashIntent.Check)

            assertEquals("challenge/detail", nav.replaced.single().path)
            assertTrue(nav.pages.isEmpty())
        }

    @Test
    fun `인증 전에 온 딥링크는 버리고 로그인으로 보낸다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val pending = PendingDeepLink().apply { set(NavRoute("challenge/detail", mapOf("challengeId" to "ch1"))) }

            viewModel(nav = nav, refreshToken = null, pendingDeepLink = pending).onIntent(SplashIntent.Check)

            assertTrue(nav.replaced.isEmpty())
            assertEquals(LoginPage, nav.pages.single())
        }

    @Test
    fun `화면이 다시 만들어져도 진입 절차를 두 번 돌지 않는다`() =
        runTest {
            // 액티비티 재생성마다 인트로 조회와 토큰 재발급이 또 나가면 안 된다.
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(nav = nav, refreshToken = "rt")

            viewModel.onIntent(SplashIntent.Check)
            viewModel.onIntent(SplashIntent.Check)

            assertEquals(listOf<Page>(HomePage), nav.pages)
        }

    private fun viewModel(
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
        refreshToken: String? = "rt",
        forceUpdate: Boolean = false,
        pendingDeepLink: PendingDeepLink = PendingDeepLink(),
        tokens: FakeTokenRepository = FakeTokenRepository(refreshToken),
    ): SplashViewModel {
        val intro =
            FakeIntroRepository().apply {
                result =
                    IntroInfo(
                        versionGate =
                            AppVersionGate(forceUpdate = forceUpdate, devTestMsg = "점검 중", minAppVersion = "1.0.0"),
                        termsVersions = TermsVersions(emptyMap()),
                    )
            }
        return SplashViewModel(
            loadIntroUseCase = LoadIntroUseCase(intro),
            autoLoginUseCase =
                AutoLoginUseCase(
                    // 갱신 응답이 없으면 자동 로그인이 실패한다 — 세션이 살아 있는 경로를 실제로 만든다.
                    FakeAuthRepository().apply {
                        refreshResult = RefreshedSession(Token("at", "rt2", "Bearer", 3600), userId = "u-1")
                    },
                    tokens,
                    testObservability(),
                ),
            pendingDeepLink = pendingDeepLink,
            // 모르는 경로는 로그인을 요구한다 — 딥링크는 외부에서 들어오므로 안전한 쪽으로 실패한다.
            routeAccessPolicy = RouteAccessPolicy { true },
            navigationHelper = nav,
            observability = testObservability(),
        )
    }
}
