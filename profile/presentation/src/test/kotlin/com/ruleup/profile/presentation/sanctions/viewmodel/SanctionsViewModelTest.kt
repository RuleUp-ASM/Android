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

/** 제재 이력. */
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
            val viewModel = viewModel(FakeAccountRepository(sanctions = { throw IllegalStateException("서버 오류") }))

            viewModel.onIntent(SanctionsIntent.Load)

            assertEquals("제재 이력을 불러오지 못했어요", viewModel.uiState.value.errorMessage)
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
