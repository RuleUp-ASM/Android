package com.ruleup.onboarding.presentation.onboarding.viewmodel

import com.ruleup.domain.entity.user.Gender
import com.ruleup.domain.navigation.PendingDeepLink
import com.ruleup.domain.test.FakeTokenRepository
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.logging.domain.test.RecordingBizLogger
import com.ruleup.observability.domain.test.testObservability
import com.ruleup.onboarding.domain.auth.SignupSession
import com.ruleup.onboarding.domain.auth.entity.OAuthProfile
import com.ruleup.onboarding.domain.auth.usecase.SignupUseCase
import com.ruleup.onboarding.domain.auth.usecase.ValidateBirthDateUseCase
import com.ruleup.onboarding.domain.fake.FakeAuthRepository
import com.ruleup.onboarding.domain.fake.FakeDeviceIdentityRepository
import com.ruleup.onboarding.domain.fake.FakeIntroRepository
import com.ruleup.onboarding.domain.fake.FakeProfileRepository
import com.ruleup.onboarding.domain.logging.SignupTimer
import com.ruleup.onboarding.presentation.common.AuthFailureUi
import com.ruleup.profile.domain.entity.NicknameCheck
import com.ruleup.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 가입 6단계. */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `형식이 틀린 닉네임은 서버에 묻지 않고 바로 알린다`() =
        runTest {
            // 왕복 없이 알려 줘야 타이핑 중 즉시 고칠 수 있다.
            val viewModel = viewModel()

            viewModel.onIntent(OnboardingIntent.SetNickName("!!"))

            assertFalse(viewModel.uiState.value.nicknameAvailable == true)
            assertTrue(
                viewModel.uiState.value.nicknameMessage
                    ?.isNotBlank() == true,
            )
        }

    @Test
    fun `유효한 닉네임 입력이 멈춘 지 250ms 뒤 서버 확인을 시작한다`() =
        runTest {
            val checked = mutableListOf<String>()
            val profiles =
                profileRepository {
                    checked += it
                    availableNickname()
                }
            val viewModel = viewModel(session = SignupSession(), profiles = profiles)

            viewModel.onIntent(OnboardingIntent.SetNickName("도전왕"))
            advanceTimeBy(249)
            runCurrent()
            assertTrue(checked.isEmpty())
            advanceTimeBy(1)
            runCurrent()

            assertEquals(listOf("도전왕"), checked)
            assertEquals(true, viewModel.uiState.value.nicknameAvailable)
        }

    @Test
    fun `확인한 닉네임을 지웠다가 다시 입력하면 다시 사용할 수 있다`() =
        runTest {
            val checked = mutableListOf<String>()
            val viewModel =
                viewModel(
                    session = SignupSession(),
                    profiles =
                        profileRepository {
                            checked += it
                            availableNickname()
                        },
                )
            viewModel.onIntent(OnboardingIntent.SetNickName("도전왕"))
            advanceTimeBy(600)
            runCurrent()
            viewModel.onIntent(OnboardingIntent.SetNickName(""))
            viewModel.onIntent(OnboardingIntent.SetNickName("도전왕"))
            advanceTimeBy(600)
            runCurrent()

            assertEquals(listOf("도전왕", "도전왕"), checked)
            assertEquals(true, viewModel.uiState.value.nicknameAvailable)
        }

    @Test
    fun `닉네임 확인이 실패해도 다시 입력하면 서버 확인 후 진행할 수 있다`() =
        runTest {
            var attempts = 0
            val viewModel =
                viewModel(
                    session = SignupSession(),
                    profiles =
                        profileRepository {
                            attempts++
                            if (attempts == 1) throw java.io.IOException("offline")
                            availableNickname()
                        },
                )

            viewModel.onIntent(OnboardingIntent.SetNickName("도전왕"))
            advanceTimeBy(250)
            runCurrent()
            assertEquals(null, viewModel.uiState.value.nicknameAvailable)
            assertTrue(
                viewModel.uiState.value.nicknameMessage
                    ?.contains("확인하지 못했어요") == true,
            )

            viewModel.onIntent(OnboardingIntent.SetNickName(""))
            viewModel.onIntent(OnboardingIntent.SetNickName("도전왕"))
            advanceTimeBy(250)
            runCurrent()
            assertEquals(2, attempts)
            assertEquals(true, viewModel.uiState.value.nicknameAvailable)
        }

    @Test
    fun `이전 닉네임 확인이 오래 걸려도 새 입력을 확인한다`() =
        runTest {
            val checked = mutableListOf<String>()
            val first = CompletableDeferred<NicknameCheck>()
            val profiles =
                profileRepository { name ->
                    checked += name
                    if (name == "도전왕") first.await() else availableNickname()
                }
            val viewModel = viewModel(session = SignupSession(), profiles = profiles)
            viewModel.onIntent(OnboardingIntent.SetNickName("도전왕"))
            advanceTimeBy(600)
            runCurrent()
            viewModel.onIntent(OnboardingIntent.SetNickName("새로운왕"))
            advanceTimeBy(600)
            runCurrent()

            assertEquals(listOf("도전왕", "새로운왕"), checked)
            assertEquals(true, viewModel.uiState.value.nicknameAvailable)
        }

    private fun profileRepository(check: suspend (String) -> NicknameCheck): ProfileRepository =
        object : ProfileRepository by FakeProfileRepository() {
            override suspend fun checkNickname(nickname: String): NicknameCheck = check(nickname)
        }

    private fun availableNickname() = NicknameCheck(valid = true, available = true, reason = null)

    @Test
    fun `가입 토큰이 만료됐으면 안내를 띄운 뒤 로그인부터 다시 하게 한다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(session = SignupSession(), nav = nav)
            val effects = collectEffects(viewModel)

            viewModel.onIntent(OnboardingIntent.Submit)

            val dialog = (effects.single() as OnboardingEffect.ShowFailure).ui as AuthFailureUi.Dialog
            assertTrue(dialog.restartFromLogin)
            assertTrue(nav.pages.isEmpty() && nav.replaced.isEmpty())
        }

    @Test
    fun `생년월일이 없으면 제출하지 않는다`() =
        runTest {
            val auth = FakeAuthRepository()
            val viewModel = viewModel(auth = auth, session = startedSession())
            val effects = collectEffects(viewModel)

            viewModel.onIntent(OnboardingIntent.Submit)

            assertTrue(auth.signedUpForm == null)
            assertTrue(effects.isNotEmpty())
        }

    @Test
    fun `성별이 없으면 제출하지 않는다`() =
        runTest {
            // 화면이 이미 막지만 상태 복원처럼 화면을 안 거치는 경로도 받친다.
            val auth = FakeAuthRepository()
            val viewModel = viewModel(auth = auth, session = startedSession())
            viewModel.onIntent(OnboardingIntent.SetBirthDate("19990327"))

            viewModel.onIntent(OnboardingIntent.Submit)

            assertTrue(auth.signedUpForm == null)
        }

    @Test
    fun `필수 약관에 동의하지 않으면 제출하지 않는다`() =
        runTest {
            val auth = FakeAuthRepository()
            val viewModel = viewModel(auth = auth, session = startedSession())
            viewModel.onIntent(OnboardingIntent.SetBirthDate("19990327"))
            viewModel.onIntent(OnboardingIntent.SetGender(Gender.FEMALE))

            viewModel.onIntent(OnboardingIntent.Submit)

            assertTrue(auth.signedUpForm == null)
        }

    @Test
    fun `첫 단계 뒤로가기는 이탈 확인을 띄운다`() =
        runTest {
            val viewModel = viewModel()
            val effects = collectEffects(viewModel)

            viewModel.onIntent(OnboardingIntent.BackFromFirstStep)

            assertEquals(listOf<OnboardingEffect>(OnboardingEffect.ConfirmExit), effects)
        }

    private fun TestScope.collectEffects(viewModel: OnboardingViewModel): List<OnboardingEffect> {
        val effects = mutableListOf<OnboardingEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }
        return effects
    }

    private fun startedSession() =
        SignupSession().apply {
            start("signup-token", OAuthProfile(email = null, nicknameHint = "도전왕", profileImageUrlHint = null))
        }

    private fun viewModel(
        auth: FakeAuthRepository = FakeAuthRepository(),
        session: SignupSession = startedSession(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
        profiles: ProfileRepository = FakeProfileRepository(),
    ): OnboardingViewModel =
        OnboardingViewModel(
            signupUseCase =
                SignupUseCase(auth, FakeDeviceIdentityRepository(), profiles, FakeTokenRepository(), testObservability()),
            validateBirthDateUseCase =
                ValidateBirthDateUseCase(Clock.fixed(Instant.parse("2026-09-01T00:00:00Z"), ZoneId.of("UTC"))),
            profileRepository = profiles,
            introRepository = FakeIntroRepository(),
            signupSession = session,
            signupTimer = SignupTimer(),
            bizLogger = RecordingBizLogger(),
            navigationHelper = nav,
            pendingDeepLink = PendingDeepLink(),
        )
}
