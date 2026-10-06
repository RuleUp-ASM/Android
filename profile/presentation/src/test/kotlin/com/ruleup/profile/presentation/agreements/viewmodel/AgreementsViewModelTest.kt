package com.ruleup.profile.presentation.agreements.viewmodel

import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.onboarding.domain.fake.FakeIntroRepository
import com.ruleup.profile.domain.entity.AgreementState
import com.ruleup.profile.domain.entity.AgreementStatus
import com.ruleup.profile.domain.entity.AgreementVersionMismatchException
import com.ruleup.profile.presentation.fake.FakeAccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 약관 동의 관리. */
@OptIn(ExperimentalCoroutinesApi::class)
class AgreementsViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `한 번도 동의한 적 없는 항목도 현행 버전으로 보낼 수 있다`() =
        runTest {
            val repo = FakeAccountRepository(agreements = { status(version = null) }, submit = { status() })
            val viewModel = viewModel(repo)
            viewModel.onIntent(AgreementsIntent.Load)

            viewModel.onIntent(AgreementsIntent.Toggle(AgreementType.MARKETING, true))

            assertEquals(
                CURRENT_VERSION,
                repo.submitted
                    .single()
                    .single()
                    .version,
            )
        }

    @Test
    fun `토글은 동의했던 버전이 아니라 현행 버전을 실어 보낸다`() =
        runTest {
            val repo = FakeAccountRepository(agreements = { status(version = "1.0") }, submit = { status() })
            val viewModel = viewModel(repo)
            viewModel.onIntent(AgreementsIntent.Load)

            viewModel.onIntent(AgreementsIntent.Toggle(AgreementType.MARKETING, true))

            val sent = repo.submitted.single().single()
            assertEquals(AgreementType.MARKETING, sent.type)
            assertEquals(CURRENT_VERSION, sent.version)
            assertEquals(true, sent.agreed)
        }

    @Test
    fun `제출에 성공하면 부분 응답으로 덮지 않고 전체를 다시 받는다`() =
        runTest {
            // 응답은 갱신된 항목만 온다
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
    fun `선택 동의를 토글하면 응답을 기다리지 않고 화면부터 바꾼다`() =
        runTest {
            // 응답이 올 때까지 스위치가 제자리면 눌리지 않은 줄 알고 다시 누른다(#564).
            lateinit var viewModel: AgreementsViewModel
            var shownWhileSending: Boolean? = null
            val repo =
                FakeAccountRepository(
                    agreements = { status() },
                    submit = {
                        shownWhileSending =
                            viewModel.uiState.value.status
                                ?.of(AgreementType.MARKETING)
                                ?.agreed
                        status()
                    },
                )
            viewModel = viewModel(repo)
            viewModel.onIntent(AgreementsIntent.Load)

            viewModel.onIntent(AgreementsIntent.Toggle(AgreementType.MARKETING, true))

            assertEquals(true, shownWhileSending)
        }

    @Test
    fun `선택 동의 변경이 실패하면 요청 전 값으로 되돌리고 모달로 알린다`() =
        runTest {
            // 되돌리지 않으면 서버는 그대로인데 화면만 동의한 것처럼 남는다.
            val repo = FakeAccountRepository(agreements = { status() }, submit = { throw IllegalStateException("서버 오류") })
            val viewModel = viewModel(repo)
            val effects = mutableListOf<AgreementsEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }
            viewModel.onIntent(AgreementsIntent.Load)

            viewModel.onIntent(AgreementsIntent.Toggle(AgreementType.MARKETING, true))

            assertEquals(
                false,
                viewModel.uiState.value.status
                    ?.of(AgreementType.MARKETING)
                    ?.agreed,
            )
            assertEquals(1, effects.filterIsInstance<AgreementsEffect.ShowErrorDialog>().size)
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

    @Test
    fun `재동의는 사용자가 동의했던 버전이 아니라 현행 버전을 보낸다`() =
        runTest {
            val repo =
                FakeAccountRepository(
                    agreements = { status(version = "1.0", reconsent = listOf(AgreementType.TERMS_OF_SERVICE)) },
                    submit = { status() },
                )
            val viewModel = viewModel(repo)
            viewModel.onIntent(AgreementsIntent.Load)

            viewModel.onIntent(AgreementsIntent.Reconsent)

            assertEquals(
                CURRENT_VERSION,
                repo.submitted
                    .single()
                    .single()
                    .version,
            )
        }

    private fun viewModel(
        repo: FakeAccountRepository,
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
        currentVersion: String = CURRENT_VERSION,
    ) = AgreementsViewModel(
        accountRepository = repo,
        introRepository = FakeIntroRepository().apply { termsVersions(currentVersion) },
        navigationHelper = nav,
    )

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

    private companion object {
        /** GET /intro 가 내려주는 현행 약관 버전. */
        const val CURRENT_VERSION = "2.0"
    }
}
