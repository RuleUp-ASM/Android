package com.ruleup.onboarding.presentation.splash.viewmodel

import com.ruleup.domain.entity.user.AccountRestriction
import com.ruleup.domain.entity.user.TermsVersions
import com.ruleup.domain.entity.user.Token
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page
import com.ruleup.domain.navigation.PendingDeepLink
import com.ruleup.domain.navigation.RouteAccessPolicy
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.domain.token.RefreshedSession
import com.ruleup.logging.domain.test.RecordingBizLogger
import com.ruleup.observability.domain.test.testObservability
import com.ruleup.onboarding.domain.account.AccountRestrictionProvider
import com.ruleup.onboarding.domain.auth.usecase.AutoLoginUseCase
import com.ruleup.onboarding.domain.fake.FakeAuthRepository
import com.ruleup.onboarding.domain.fake.FakeIntroRepository
import com.ruleup.onboarding.domain.fake.FakeTokenRepository
import com.ruleup.onboarding.domain.fake.FakeWalkthroughRepository
import com.ruleup.onboarding.domain.intro.entity.AppVersionGate
import com.ruleup.onboarding.domain.intro.entity.IntroInfo
import com.ruleup.onboarding.domain.intro.usecase.LoadIntroUseCase
import com.ruleup.onboarding.domain.navigation.HomePage
import com.ruleup.onboarding.domain.navigation.LoginPage
import com.ruleup.onboarding.domain.navigation.WalkthroughPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.net.SocketTimeoutException
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
    fun `인증 전에 온 딥링크는 보류한 채 로그인으로 보낸다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val pending = PendingDeepLink().apply { set(NavRoute("challenge/detail", mapOf("challengeId" to "ch1"))) }

            viewModel(nav = nav, refreshToken = null, pendingDeepLink = pending).onIntent(SplashIntent.Check)

            assertTrue(nav.replaced.isEmpty())
            assertEquals(LoginPage, nav.pages.single())
            assertEquals("challenge/detail", pending.consumeAfterLogin()?.path)
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

    @Test
    fun `소개를 안 본 미로그인 사용자는 워크쓰루로 보낸다`() =
        runTest {
            // 첫 실행에서 로그인 버튼부터 보여 주면 이 앱이 무엇을 하는지 모른 채 소셜 로그인을 만난다.
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(nav = nav, refreshToken = null, walkthroughSeen = false)

            viewModel.onIntent(SplashIntent.Check)

            assertEquals(
                WalkthroughPage.PATH,
                nav.pages
                    .single()
                    .toRoute()
                    .path,
            )
        }

    @Test
    fun `소개를 본 미로그인 사용자는 로그인으로 보낸다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(nav = nav, refreshToken = null, walkthroughSeen = true)

            viewModel.onIntent(SplashIntent.Check)

            assertEquals(
                LoginPage.PATH,
                nav.pages
                    .single()
                    .toRoute()
                    .path,
            )
        }

    @Test
    fun `로그인 정지 계정은 홈이 아니라 잠금 화면으로 간다`() =
        runTest {
            // 잠금 토큰으로는 제재 이력과 CS 문의 말고 전부 401 이 난다 — 홈으로 보내면 빈 화면만 본다.
            val nav = RecordingNavigationHelper()

            viewModel(nav = nav, restriction = AccountRestriction.Locked).onIntent(SplashIntent.Check)

            assertEquals(
                AppRoutes.ACCOUNT_LOCKED,
                nav.pages
                    .single()
                    .toRoute()
                    .path,
            )
        }

    @Test
    fun `기능 정지 계정은 잠그지 않고 홈으로 보낸다`() =
        runTest {
            // 신고 하나 막힌 계정을 앱 전체에서 내쫓으면 정지 범위보다 훨씬 큰 제재가 된다(SAN-05).
            val nav = RecordingNavigationHelper()

            viewModel(nav = nav, restriction = AccountRestriction.Feature("REPORT"))
                .onIntent(SplashIntent.Check)

            assertEquals(
                AppRoutes.HOME,
                nav.pages
                    .single()
                    .toRoute()
                    .path,
            )
        }

    @Test
    fun `연결이 끊기면 로그인 화면으로 보내지 않고 재시도를 기다린다`() =
        runTest {
            // 아직 며칠 남은 세션을 타임아웃 한 번으로 버리면 소셜 로그인부터 다시 해야 한다(ENV-03).
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(nav = nav, refreshError = SocketTimeoutException("timeout"))

            viewModel.onIntent(SplashIntent.Check)

            assertTrue(nav.pages.isEmpty())
            assertTrue(viewModel.uiState.value.connectionFailed)
        }

    @Test
    fun `다시 시도하면 연결 오류를 지우고 진입을 다시 판정한다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(nav = nav, refreshError = SocketTimeoutException("timeout"))
            viewModel.onIntent(SplashIntent.Check)

            viewModel.onIntent(SplashIntent.Retry)

            // 페이크는 계속 실패하지만, 재시도가 절차를 실제로 다시 돌았는지만 본다.
            assertTrue(viewModel.uiState.value.connectionFailed)
            assertTrue(nav.pages.isEmpty())
        }

    @Test
    fun `세션이 끝난 기존 가입자는 워크쓰루가 아니라 로그인으로 간다`() =
        runTest {
            // 소개를 건너뛰고 가입한 사용자는 isSeen 이 false 다. 그것만 보면 세션 만료가
            // "첫 실행"으로 읽혀 가입 전으로 되돌아간 화면이 열린다(AUTH-05 · NAV-10).
            val nav = RecordingNavigationHelper()

            viewModel(
                nav = nav,
                walkthroughSeen = false,
                tokens = FakeTokenRepository(refreshToken = "rt"),
                refreshError = RuntimeException("expired"),
            ).onIntent(SplashIntent.Check)

            assertEquals(LoginPage, nav.pages.single())
        }

    private fun viewModel(
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
        refreshToken: String? = "rt",
        forceUpdate: Boolean = false,
        pendingDeepLink: PendingDeepLink = PendingDeepLink(),
        tokens: FakeTokenRepository = FakeTokenRepository(refreshToken),
        // 대부분의 테스트는 진입 분기(홈·로그인·딥링크)를 보므로 소개를 이미 본 사용자로 둔다.
        walkthroughSeen: Boolean = true,
        restriction: AccountRestriction = AccountRestriction.None,
        refreshError: Throwable? = null,
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
                        this.refreshError = refreshError
                    },
                    tokens,
                    RecordingBizLogger(),
                ),
            accountRestrictionProvider = AccountRestrictionProvider { restriction },
            walkthroughRepository = FakeWalkthroughRepository(seen = walkthroughSeen),
            tokenRepository = tokens,
            pendingDeepLink = pendingDeepLink,
            // 모르는 경로는 로그인을 요구한다 — 딥링크는 외부에서 들어오므로 안전한 쪽으로 실패한다.
            routeAccessPolicy = RouteAccessPolicy { true },
            navigationHelper = nav,
            observability = testObservability(),
        )
    }
}
