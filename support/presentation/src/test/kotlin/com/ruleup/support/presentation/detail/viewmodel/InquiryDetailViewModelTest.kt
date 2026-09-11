package com.ruleup.support.presentation.detail.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.support.domain.entity.InquiryException
import com.ruleup.support.domain.entity.InquiryFailure
import com.ruleup.support.domain.fake.FakeInquiryRepository
import com.ruleup.support.domain.fake.inquiryDetail
import com.ruleup.support.domain.navigation.InquiryCategoryPage
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 문의 상세. 열람 전용이라 보낼 요청이 조회 하나뿐이고, **남의 문의와 없는 문의를 같은 문구로**
 * 말해야 한다 — 구분해 주면 그 번호의 문의가 존재한다는 사실이 새어 나간다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InquiryDetailViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `라우트 인자의 문의를 조회한다`() =
        runTest {
            val repo = FakeInquiryRepository(detail = { inquiryDetail(inquiryId = it) })
            val viewModel = viewModel(repo, inquiryId = "abc-123")

            viewModel.onIntent(InquiryDetailIntent.Load)

            assertEquals(listOf("abc-123"), repo.openedIds)
            assertEquals(
                "abc-123",
                viewModel.uiState.value.detail
                    ?.inquiryId,
            )
        }

    @Test
    fun `인자가 없으면 서버를 부르지 않는다`() =
        runTest {
            // 빈 id 로 조회해 봐야 404 다. 왕복 한 번을 아끼고 같은 문구를 바로 보여 준다.
            val repo = FakeInquiryRepository(detail = { error("불려서는 안 된다") })
            val viewModel = viewModel(repo, handle = SavedStateHandle())

            viewModel.onIntent(InquiryDetailIntent.Load)

            assertTrue(repo.calls.isEmpty())
            assertEquals("찾을 수 없는 문의예요", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `남의 문의도 없는 문의와 같은 문구로 말한다`() =
        runTest {
            val repo =
                FakeInquiryRepository(
                    detail = { throw InquiryException(InquiryFailure.NOT_FOUND, "INQUIRY_NOT_FOUND") },
                )
            val viewModel = viewModel(repo)

            viewModel.onIntent(InquiryDetailIntent.Load)

            assertEquals("찾을 수 없는 문의예요", viewModel.uiState.value.errorMessage)
            assertNull(viewModel.uiState.value.detail)
        }

    @Test
    fun `다시 묻기는 재문의가 아니라 새 문의 작성으로 보낸다`() =
        runTest {
            // 답변 뒤 같은 스레드에 글을 더하는 계약이 없다 — 분류부터 다시 고른다.
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(nav = nav)

            viewModel.onIntent(InquiryDetailIntent.NewInquiry)

            assertEquals(InquiryCategoryPage, nav.pages.single())
        }

    private fun viewModel(
        repo: FakeInquiryRepository = FakeInquiryRepository(detail = { inquiryDetail() }),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
        inquiryId: String = "3d6ad414-5fb6-81aa-8d11-ca6ffd272529",
        handle: SavedStateHandle = SavedStateHandle(mapOf(InquiryDetailPage.ARG_INQUIRY_ID to inquiryId)),
    ) = InquiryDetailViewModel(
        savedStateHandle = handle,
        inquiryRepository = repo,
        navigationHelper = nav,
    )
}
