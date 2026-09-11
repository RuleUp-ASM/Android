package com.ruleup.support.presentation.list.viewmodel

import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.support.domain.entity.InquiryException
import com.ruleup.support.domain.entity.InquiryFailure
import com.ruleup.support.domain.entity.InquiryStatus
import com.ruleup.support.domain.fake.FakeInquiryRepository
import com.ruleup.support.domain.fake.inquirySummary
import com.ruleup.support.domain.navigation.InquiryDetailPage
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
 * 내 문의 내역. 답변을 푸시로도 알림함으로도 알리지 않기로 해(2026-09-11) **이 조회가 답변을
 * 알아채는 유일한 경로**다. 캐시를 두면 답변이 왔는데 옛 목록이 남는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InquiryListViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `들어올 때마다 다시 묻는다`() =
        runTest {
            val repo = FakeInquiryRepository(inquiries = { listOf(inquirySummary()) })
            val viewModel = viewModel(repo)

            viewModel.onIntent(InquiryListIntent.Load)
            viewModel.onIntent(InquiryListIntent.Load)

            assertEquals(2, repo.calls.count { it == "getInquiries" })
        }

    @Test
    fun `답변이 달린 문의는 새 답변으로 표시된다`() =
        runTest {
            val repo =
                FakeInquiryRepository(
                    inquiries = {
                        listOf(
                            inquirySummary(
                                inquiryId = "a",
                                status = InquiryStatus.ANSWERED,
                                answeredAt = "2026-09-06T11:08:00Z",
                            ),
                            inquirySummary(inquiryId = "b"),
                        )
                    },
                )
            val viewModel = viewModel(repo)

            viewModel.onIntent(InquiryListIntent.Load)

            val items = viewModel.uiState.value.items
            assertTrue(items.first().hasNewAnswer)
            assertFalse(items.last().hasNewAnswer)
        }

    @Test
    fun `조회에 실패해도 화면은 오류 문구로 서고 다시 시도할 수 있다`() =
        runTest {
            var attempt = 0
            val repo =
                FakeInquiryRepository(
                    inquiries = {
                        attempt++
                        if (attempt == 1) throw InquiryException(InquiryFailure.NETWORK, "끊김") else listOf(inquirySummary())
                    },
                )
            val viewModel = viewModel(repo)

            viewModel.onIntent(InquiryListIntent.Load)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals("지금은 연결이 불안정해요. 잠시 후 다시 시도해 주세요.", viewModel.uiState.value.errorMessage)

            viewModel.onIntent(InquiryListIntent.Retry)
            assertEquals(1, viewModel.uiState.value.items.size)
        }

    @Test
    fun `항목을 누르면 그 문의의 상세로 간다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(nav = nav)

            viewModel.onIntent(InquiryListIntent.Open("abc-123"))

            assertEquals(InquiryDetailPage("abc-123"), nav.pages.single())
        }

    private fun viewModel(
        repo: FakeInquiryRepository = FakeInquiryRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = InquiryListViewModel(inquiryRepository = repo, navigationHelper = nav)
}
