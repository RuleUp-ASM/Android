package com.ruleup.onboarding.presentation.intro.viewmodel

import com.ruleup.domain.entity.user.AccountRestriction
import com.ruleup.domain.entity.user.AccountStatus
import com.ruleup.domain.entity.user.NicknameStatus
import com.ruleup.domain.entity.user.Token
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page
import com.ruleup.domain.navigation.PendingDeepLink
import com.ruleup.domain.test.RecordingMessageHelper
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.logging.domain.test.RecordingBizLogger
import com.ruleup.observability.domain.test.testObservability
import com.ruleup.onboarding.domain.account.AccountRestrictionProvider
import com.ruleup.onboarding.domain.auth.SignupSession
import com.ruleup.onboarding.domain.auth.entity.AuthSession
import com.ruleup.onboarding.domain.auth.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.auth.entity.OAuthProfile
import com.ruleup.onboarding.domain.auth.entity.OAuthProvider
import com.ruleup.onboarding.domain.auth.entity.OAuthResult
import com.ruleup.onboarding.domain.auth.usecase.SocialLoginUseCase
import com.ruleup.onboarding.domain.fake.FakeAuthRepository
import com.ruleup.onboarding.domain.fake.FakeDeviceIdentityRepository
import com.ruleup.onboarding.domain.fake.FakeTokenRepository
import com.ruleup.onboarding.domain.fake.testUser
import com.ruleup.onboarding.domain.logging.SignupTimer
import com.ruleup.onboarding.domain.navigation.HomePage
import com.ruleup.onboarding.domain.navigation.OnboardingNicknamePage
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
 * 소셜 로그인. 인증에 성공해도 **가는 곳이 네 갈래**다 — 기존 사용자, 잠금 계정, 닉네임을 선점당한
 * 복원, 신규 가입. 잘못 보내면 사용자가 가입을 마치지 못하거나 잠금 사유를 모른 채 막힌다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val authorization =
        OAuthAuthorization(
            provider = OAuthProvider.KAKAO,
            code = "code",
            codeVerifier = "verifier",
            redirectUri = "ruleup://oauth",
        )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `기존 사용자는 홈으로 보낸다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(existingUser(), nav = nav)

            viewModel.onIntent(LoginIntent.AuthorizationReceived(authorization))

            assertEquals(listOf<Page>(HomePage), nav.pages)
        }

    @Test
    fun `로그인 전에 받은 초대 링크가 있으면 홈 대신 그 목적지로 보낸다`() =
        runTest {
            // 가입·로그인을 마쳐도 목적지로 못 가면 초대 링크가 끊긴다(NAV-03).
            val nav = RecordingNavigationHelper()
            val pending = PendingDeepLink().apply { set(NavRoute("challenge/watcher/accept", mapOf("token" to "t1"))) }
            val viewModel = viewModel(existingUser(), nav = nav, pendingDeepLink = pending)

            viewModel.onIntent(LoginIntent.AuthorizationReceived(authorization))

            assertEquals("challenge/watcher/accept", nav.replaced.single().path)
            assertTrue(nav.pages.isEmpty())
        }

    @Test
    fun `기능만 정지된 계정은 홈을 열되 무엇이 막혔는지 알려 준다`() =
        runTest {
            // 열람은 되므로 막지 않는다. 다만 왜 신고가 안 되는지 모르면 고장으로 읽힌다.
            val nav = RecordingNavigationHelper()
            val messages = RecordingMessageHelper()
            val viewModel =
                viewModel(
                    FakeAuthRepository().apply {
                        exchangeResult =
                            OAuthResult.ExistingUser(
                                AuthSession(token, testUser(accountStatus = AccountStatus.SUSPENDED)),
                                restored = false,
                            )
                    },
                    nav = nav,
                    messages = messages,
                    restriction = AccountRestriction.Feature("REPORT"),
                )

            viewModel.onIntent(LoginIntent.AuthorizationReceived(authorization))

            assertEquals(listOf<Page>(HomePage), nav.pages)
            assertTrue(messages.allMessages.single().contains("신고"))
        }

    @Test
    fun `잠긴 계정은 재로그인해도 잠금 화면으로 간다`() =
        runTest {
            // 자동 로그인만 막고 여기를 열어 두면 로그아웃 후 재로그인이 잠금을 통째로 우회한다(AUTH-13).
            val nav = RecordingNavigationHelper()
            val viewModel =
                viewModel(
                    FakeAuthRepository().apply {
                        exchangeResult =
                            OAuthResult.ExistingUser(
                                AuthSession(token, testUser(accountStatus = AccountStatus.SUSPENDED)),
                                restored = false,
                            )
                    },
                    nav = nav,
                    restriction = AccountRestriction.Locked,
                )

            viewModel.onIntent(LoginIntent.AuthorizationReceived(authorization))

            assertEquals(
                AppRoutes.ACCOUNT_LOCKED,
                nav.pages
                    .single()
                    .toRoute()
                    .path,
            )
        }

    @Test
    fun `복원 중 닉네임을 선점당했으면 홈으로 보내지 않고 새 닉네임을 받는다`() =
        runTest {
            // 여기서 홈으로 보내면 남의 닉네임을 단 채로 앱을 쓰게 된다.
            val nav = RecordingNavigationHelper()
            val messages = RecordingMessageHelper()
            val viewModel =
                viewModel(
                    FakeAuthRepository().apply {
                        exchangeResult =
                            OAuthResult.ExistingUser(
                                AuthSession(token, testUser(nickname = "도전왕", nicknameStatus = NicknameStatus.CONFLICT)),
                                restored = true,
                            )
                    },
                    nav = nav,
                    messages = messages,
                )

            viewModel.onIntent(LoginIntent.AuthorizationReceived(authorization))

            assertEquals(listOf<Page>(OnboardingNicknamePage), nav.pages)
            assertTrue(messages.allMessages.single().contains("도전왕"))
        }

    @Test
    fun `신규 사용자는 가입 화면으로 보내고 가입 토큰은 백스택에 싣지 않는다`() =
        runTest {
            // 토큰을 인자로 넘기면 직렬화되어 saved state 에 남는다.
            val nav = RecordingNavigationHelper()
            val session = SignupSession()
            val viewModel = viewModel(newUser(), nav = nav, signupSession = session)

            viewModel.onIntent(LoginIntent.AuthorizationReceived(authorization))

            assertEquals(listOf<Page>(OnboardingNicknamePage), nav.pages)
            assertTrue(nav.routes.none { it.args.values.any { arg -> arg.contains("signup-token") } })
        }

    @Test
    fun `로그인에 실패하면 아무 데도 보내지 않고 진행 표시를 끝낸다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel =
                viewModel(FakeAuthRepository().apply { exchangeError = IllegalStateException("네트워크 오류") }, nav = nav)

            viewModel.onIntent(LoginIntent.AuthorizationReceived(authorization))

            assertTrue(nav.didNotMove)
            assertTrue(!viewModel.uiState.value.isLoading)
        }

    @Test
    fun `사용자가 인증 화면에서 취소하면 조용히 되돌린다`() =
        runTest {
            // 대부분 취소라 오류로 다루면 정상 동작에 경고가 뜬다.
            val nav = RecordingNavigationHelper()
            val messages = RecordingMessageHelper()
            val viewModel = viewModel(existingUser(), nav = nav, messages = messages)
            viewModel.onIntent(LoginIntent.LoginClicked(OAuthProvider.KAKAO))

            viewModel.onIntent(LoginIntent.AuthFailed(IllegalStateException("사용자 취소")))

            assertTrue(!viewModel.uiState.value.isLoading)
            assertTrue(messages.allMessages.isEmpty())
            assertTrue(nav.didNotMove)
        }

    private val token = Token("a", "r", "Bearer", 3600)

    private fun existingUser() =
        FakeAuthRepository().apply {
            exchangeResult = OAuthResult.ExistingUser(AuthSession(token, testUser()), restored = false)
        }

    private fun newUser() =
        FakeAuthRepository().apply {
            exchangeResult =
                OAuthResult.NewUser(
                    signupToken = "signup-token",
                    expiresInSeconds = 300,
                    profile = OAuthProfile(email = null, nicknameHint = "도전왕", profileImageUrlHint = null),
                )
        }

    private fun viewModel(
        auth: FakeAuthRepository,
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
        messages: RecordingMessageHelper = RecordingMessageHelper(),
        signupSession: SignupSession = SignupSession(),
        tokens: FakeTokenRepository = FakeTokenRepository(),
        pendingDeepLink: PendingDeepLink = PendingDeepLink(),
        restriction: AccountRestriction = AccountRestriction.None,
    ) = LoginViewModel(
        socialLoginUseCase = SocialLoginUseCase(auth, FakeDeviceIdentityRepository(), tokens, testObservability()),
        navigationHelper = nav,
        messageHelper = messages,
        observability = testObservability(),
        bizLogger = RecordingBizLogger(),
        signupTimer = SignupTimer(),
        tokenRepository = tokens,
        signupSession = signupSession,
        pendingDeepLink = pendingDeepLink,
        accountRestrictionProvider = AccountRestrictionProvider { restriction },
    )
}
