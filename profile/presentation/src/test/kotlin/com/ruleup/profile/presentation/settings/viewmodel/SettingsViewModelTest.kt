package com.ruleup.profile.presentation.settings.viewmodel

import com.ruleup.domain.entity.user.AccountStatus
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.onboarding.domain.auth.usecase.LogoutUseCase
import com.ruleup.onboarding.domain.auth.usecase.WithdrawUseCase
import com.ruleup.onboarding.domain.fake.FakeAuthRepository
import com.ruleup.onboarding.domain.fake.FakeTokenRepository
import com.ruleup.profile.domain.entity.ActiveSanction
import com.ruleup.profile.domain.entity.SanctionHistory
import com.ruleup.profile.presentation.fake.FakeAccountRepository
import com.ruleup.profile.presentation.fake.FakeProfileRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 설정 허브. 여기는 **진입점 목록**이라, 뱃지용 조회가 실패했다고 로그아웃·탈퇴 경로까지 막히면
 * 사용자가 계정을 어쩌지 못하게 된다.
 *
 * 로그아웃과 탈퇴는 실패 처리가 반대다 — 로그아웃은 서버가 실패해도 나가고, 탈퇴는 서버가
 * 받아들였을 때만 내보낸다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `뱃지 조회가 둘 다 실패해도 화면을 오류로 막지 않는다`() =
        runTest {
            val viewModel =
                viewModel(
                    FakeAccountRepository(
                        agreements = { throw IllegalStateException("조회 실패") },
                        sanctions = { throw IllegalStateException("조회 실패") },
                    ),
                )

            viewModel.onIntent(SettingsIntent.Load)

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(0, viewModel.uiState.value.reconsentCount)
        }

    @Test
    fun `한쪽만 실패하면 받은 쪽 뱃지는 그대로 보여 준다`() =
        runTest {
            val viewModel =
                viewModel(
                    FakeAccountRepository(
                        agreements = { throw IllegalStateException("조회 실패") },
                        sanctions = { history(active = true) },
                    ),
                )

            viewModel.onIntent(SettingsIntent.Load)

            assertTrue(viewModel.uiState.value.hasActiveSanction)
        }

    @Test
    fun `로그아웃은 서버 revoke 가 실패해도 로그인 화면으로 보낸다`() =
        runTest {
            // 토큰을 지우는 건 UseCase 계약이고, 화면은 나가는 것까지 책임진다.
            val nav = RecordingNavigationHelper()
            val auth = FakeAuthRepository().apply { logoutError = IllegalStateException("revoke 실패") }
            val viewModel = viewModel(FakeAccountRepository(), auth = auth, nav = nav)

            viewModel.onIntent(SettingsIntent.Logout)

            assertEquals(AppRoutes.LOGIN, nav.replaced.single().path)
        }

    @Test
    fun `탈퇴는 서버가 받아들였을 때만 내보낸다`() =
        runTest {
            // 실패했는데 내보내면 사용자는 탈퇴된 줄 알고 앱을 지운다.
            val nav = RecordingNavigationHelper()
            val auth = FakeAuthRepository().apply { withdrawError = IllegalStateException("탈퇴 실패") }
            val viewModel = viewModel(FakeAccountRepository(), auth = auth, nav = nav)

            viewModel.onIntent(SettingsIntent.Withdraw)

            assertTrue(nav.replaced.isEmpty())
        }

    @Test
    fun `탈퇴할 때 서버가 정한 확인 문구를 그대로 보낸다`() =
        runTest {
            // 문구가 어긋나면 400 이라 화면 문구가 아니라 계약이다.
            val auth = FakeAuthRepository()
            val viewModel = viewModel(FakeAccountRepository(), auth = auth)

            viewModel.onIntent(SettingsIntent.Withdraw)

            assertEquals("탈퇴할게요", auth.withdrawnWith)
        }

    private fun viewModel(
        repo: FakeAccountRepository,
        auth: FakeAuthRepository = FakeAuthRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
        // 프로필은 「연결된 계정」 표기 전용이라 실패해도 나머지 행은 그대로 그린다.
        profile: FakeProfileRepository = FakeProfileRepository(),
    ): SettingsViewModel {
        val tokens = FakeTokenRepository()
        return SettingsViewModel(
            accountRepository = repo,
            profileRepository = profile,
            logoutUseCase = LogoutUseCase(auth, tokens),
            withdrawUseCase = WithdrawUseCase(auth, tokens),
            navigationHelper = nav,
        )
    }

    private fun history(active: Boolean) =
        SanctionHistory(
            accountStatus = if (active) AccountStatus.LOCKED else AccountStatus.ACTIVE,
            activeSanction =
                if (active) {
                    ActiveSanction(
                        sanctionId = "s1",
                        track = null,
                        type = null,
                        featureCode = null,
                        reasonCode = null,
                        reasonText = null,
                        startsAt = null,
                        endsAt = null,
                        reviewRequestable = false,
                    )
                } else {
                    null
                },
            admin = emptyList(),
            auto = emptyList(),
        )
}
