package com.ruleup.onboarding.presentation.intro.viewmodel

import com.ruleup.domain.entity.user.AccountStatus
import com.ruleup.domain.entity.user.NicknameStatus
import com.ruleup.domain.entity.user.Token
import com.ruleup.domain.navigation.Page
import com.ruleup.domain.test.RecordingMessageHelper
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.observability.domain.test.testObservability
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
import com.ruleup.onboarding.domain.navigation.HomePage
import com.ruleup.onboarding.domain.navigation.OnboardingNicknamePage
import com.ruleup.onboarding.domain.observability.SignupTimer
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
    fun `잠긴 계정도 홈은 열되 잠금 사유를 알려 준다`() =
        runTest {
            // 열람은 되므로 막지 않는다. 다만 왜 편집이 안 되는지 모르면 고장으로 읽힌다.
            val nav = RecordingNavigationHelper()
            val messages = RecordingMessageHelper()
            val viewModel =
                viewModel(
                    FakeAuthRepository().apply {
                        exchangeResult =
                            OAuthResult.ExistingUser(
                                AuthSession(token, testUser(accountStatus = AccountStatus.LOCKED)),
                                restored = false,
                            )
                    },
                    nav = nav,
                    messages = messages,
                )

            viewModel.onIntent(LoginIntent.AuthorizationReceived(authorization))

            assertEquals(listOf<Page>(HomePage), nav.pages)
            assertTrue(messages.allMessages.single().contains("잠겨"))
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
    ) = LoginViewModel(
        socialLoginUseCase = SocialLoginUseCase(auth, FakeDeviceIdentityRepository(), tokens, testObservability()),
        navigationHelper = nav,
        messageHelper = messages,
        observability = testObservability(),
        signupTimer = SignupTimer(),
        tokenRepository = tokens,
        signupSession = signupSession,
    )
}
