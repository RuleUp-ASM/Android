package com.ruleup.profile.presentation.sanctions.viewmodel

import com.ruleup.domain.entity.user.AccountStatus
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.profile.domain.entity.SanctionHistory
import com.ruleup.profile.presentation.fake.FakeAccountRepository
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

/**
 * 제재 이력. **잠금 상태에서도 열려야 하는 화면**이라 조회가 막히면 사용자는 자기가 왜 잠겼는지
 * 알 방법이 없다 — 실패해도 사유가 화면에 남아야 한다.
 *
 * 이력은 확정된 과거라 화면 안에서 바뀌지 않는다. 그래서 한 번 받으면 다시 받지 않는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SanctionsViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `불러오면 이력을 화면에 올리고 로딩을 끝낸다`() =
        runTest {
            val viewModel = viewModel(FakeAccountRepository(sanctions = { empty() }))

            viewModel.onIntent(SanctionsIntent.Load)

            assertEquals(
                AccountStatus.ACTIVE,
                viewModel.uiState.value.history
                    ?.accountStatus,
            )
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `이미 받아둔 이력이 있으면 다시 묻지 않는다`() =
        runTest {
            val repo = FakeAccountRepository(sanctions = { empty() })
            val viewModel = viewModel(repo)

            viewModel.onIntent(SanctionsIntent.Load)
            viewModel.onIntent(SanctionsIntent.Load)

            assertEquals(1, repo.calls.count { it == "getSanctions" })
        }

    @Test
    fun `조회에 실패하면 사유를 남긴다`() =
        runTest {
            // 잠금 사유를 볼 유일한 경로라, 빈 화면으로 두면 사용자가 상황을 알 방법이 없다.
            val viewModel = viewModel(FakeAccountRepository(sanctions = { throw IllegalStateException("서버 오류") }))

            viewModel.onIntent(SanctionsIntent.Load)

            assertEquals("서버 오류", viewModel.uiState.value.errorMessage)
        }

    private fun viewModel(
        repo: FakeAccountRepository,
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = SanctionsViewModel(accountRepository = repo, navigationHelper = nav)

    private fun empty() =
        SanctionHistory(
            accountStatus = AccountStatus.ACTIVE,
            activeSanction = null,
            admin = emptyList(),
            auto = emptyList(),
        )
}
