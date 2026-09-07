package com.ruleup.profile.presentation.agreements.viewmodel

import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.profile.domain.entity.AgreementState
import com.ruleup.profile.domain.entity.AgreementStatus
import com.ruleup.profile.domain.entity.AgreementVersionMismatchException
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
import kotlin.test.assertTrue

/**
 * 약관 동의 관리. 동의 시각이 **법적 증거**라 화면이 먼저 바뀌고 서버가 따라오는 낙관적 반영을
 * 하지 않는다 — 서버가 받아들인 것만 화면에 남아야 한다.
 *
 * 버전은 서버의 현행 값과 같아야 하므로, 모르는 채로 보내면 400 을 받는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AgreementsViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `버전을 모르는 항목은 보내지 않는다`() =
        runTest {
            // 서버는 현행 버전과 다른 값을 400 으로 막는다 — 모르면 시도 자체를 하지 않는다.
            val repo = FakeAccountRepository(agreements = { status(version = null) })
            val viewModel = viewModel(repo)
            viewModel.onIntent(AgreementsIntent.Load)

            viewModel.onIntent(AgreementsIntent.Toggle(AgreementType.MARKETING, true))

            assertTrue(repo.submitted.isEmpty())
        }

    @Test
    fun `토글하면 서버가 준 버전을 그대로 실어 보낸다`() =
        runTest {
            val repo = FakeAccountRepository(agreements = { status() }, submit = { status() })
            val viewModel = viewModel(repo)
            viewModel.onIntent(AgreementsIntent.Load)

            viewModel.onIntent(AgreementsIntent.Toggle(AgreementType.MARKETING, true))

            val sent = repo.submitted.single().single()
            assertEquals(AgreementType.MARKETING, sent.type)
            assertEquals("1.2", sent.version)
            assertEquals(true, sent.agreed)
        }

    @Test
    fun `제출에 성공하면 부분 응답으로 덮지 않고 전체를 다시 받는다`() =
        runTest {
            // 응답은 갱신된 항목만 온다 — 그걸로 화면을 덮으면 안 건드린 항목이 사라진다.
            val repo = FakeAccountRepository(agreements = { status() }, submit = { status() })
            val viewModel = viewModel(repo)
            viewModel.onIntent(AgreementsIntent.Load)

            viewModel.onIntent(AgreementsIntent.Toggle(AgreementType.MARKETING, true))

            assertEquals(2, repo.calls.count { it == "getAgreements" })
        }

    @Test
    fun `버전이 어긋나면 다시 받아 다음 시도가 성공할 수 있게 한다`() =
        runTest {
            val repo =
                FakeAccountRepository(
                    agreements = { status() },
                    submit = { throw AgreementVersionMismatchException() },
                )
            val viewModel = viewModel(repo)
            viewModel.onIntent(AgreementsIntent.Load)

            viewModel.onIntent(AgreementsIntent.Toggle(AgreementType.MARKETING, true))

            assertEquals(2, repo.calls.count { it == "getAgreements" })
        }

    @Test
    fun `재동의는 대상 전부를 한 번에 보낸다`() =
        runTest {
            // 개정 약관이 동시에 여럿일 수 있고 서버가 한 트랜잭션으로 처리한다.
            val repo =
                FakeAccountRepository(
                    agreements = {
                        status(
                            reconsent =
                                listOf(AgreementType.TERMS_OF_SERVICE, AgreementType.PRIVACY_POLICY),
                        )
                    },
                    submit = { status() },
                )
            val viewModel = viewModel(repo)
            viewModel.onIntent(AgreementsIntent.Load)

            viewModel.onIntent(AgreementsIntent.Reconsent)

            assertEquals(2, repo.submitted.single().size)
            assertTrue(repo.submitted.single().all { it.agreed })
        }

    private fun viewModel(
        repo: FakeAccountRepository,
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = AgreementsViewModel(accountRepository = repo, navigationHelper = nav)

    private fun status(
        version: String? = "1.2",
        reconsent: List<AgreementType> = emptyList(),
    ) = AgreementStatus(
        agreements =
            AgreementType.entries.map {
                AgreementState(
                    type = it,
                    required = it.required,
                    agreed = false,
                    version = version,
                    agreedAt = null,
                )
            },
        reconsentRequired = reconsent,
    )
}
