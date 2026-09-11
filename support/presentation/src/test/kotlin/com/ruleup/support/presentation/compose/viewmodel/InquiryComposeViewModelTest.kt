package com.ruleup.support.presentation.compose.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.support.domain.entity.InquiryCategory
import com.ruleup.support.domain.entity.InquiryException
import com.ruleup.support.domain.entity.InquiryFailure
import com.ruleup.support.domain.entity.InquiryReceipt
import com.ruleup.support.domain.entity.InquiryStatus
import com.ruleup.support.domain.fake.FakeInquiryRepository
import com.ruleup.support.domain.navigation.InquiryComposePage
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 문의 작성. **접수는 되돌릴 수 없다** — 수정·삭제 경로가 없고 재시도는 같은 내용을 한 건 더
 * 쌓으면서 하루 상한까지 깎는다. 그래서 이 화면이 막아야 할 것은 "덜 쓴 접수"가 아니라
 * "의도치 않은 두 번째 접수"다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InquiryComposeViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `본문이 열 자 미만이면 접수 버튼이 잠긴다`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onIntent(InquiryComposeIntent.BodyChanged("짧아요"))

            assertFalse(viewModel.uiState.value.canSubmit)
        }

    @Test
    fun `본문이 상한을 넘어도 입력을 자르지 않고 버튼만 잠근다`() =
        runTest {
            // 붙여넣기가 조용히 잘리면 사용자는 뒷부분이 지워진 걸 모른 채 접수한다.
            val long = "가".repeat(1_200)
            val viewModel = viewModel()

            viewModel.onIntent(InquiryComposeIntent.BodyChanged(long))

            assertEquals(long, viewModel.uiState.value.body)
            assertFalse(viewModel.uiState.value.canSubmit)
        }

    @Test
    fun `업로드가 끝나지 않은 사진이 있으면 접수하지 않는다`() {
        // 주소 없이 보내면 그 장은 서버에 닿지 않고, 사용자는 첨부한 줄 안다.
        // 업로드 중 상태는 코루틴이 끝나기 전 한순간이라, 그 순간의 상태를 직접 세워 본다.
        val state =
            InquiryComposeState
                .initial(InquiryCategory.VERIFICATION)
                .copy(
                    body = "기상 인증이 실패로 떴어요",
                    attachments = listOf(InquiryAttachment(uri = "content://1")),
                )

        assertTrue(state.attachments.single().uploading)
        assertFalse(state.canSubmit)
    }

    @Test
    fun `접수번호를 받은 뒤에는 같은 화면에서 다시 보내지 않는다`() {
        // 접수는 되돌릴 수 없다 — 시트 뒤에서 버튼이 다시 눌리면 같은 문의가 한 건 더 쌓인다.
        val state =
            InquiryComposeState
                .initial(InquiryCategory.VERIFICATION)
                .copy(body = "기상 인증이 실패로 떴어요", receiptId = "abc-123")

        assertFalse(state.canSubmit)
    }

    @Test
    fun `업로드에 실패한 사진은 빼고 접수한다`() =
        runTest {
            val repo =
                FakeInquiryRepository(
                    upload = { uri -> if (uri == "content://1") "/files/ok.jpg" else error("거절") },
                    submit = { receipt() },
                )
            val viewModel = viewModel(repo)
            viewModel.onIntent(InquiryComposeIntent.BodyChanged("기상 인증이 실패로 떴어요"))
            viewModel.onIntent(InquiryComposeIntent.ImagePicked("content://1"))
            viewModel.onIntent(InquiryComposeIntent.ImagePicked("content://2"))

            viewModel.onIntent(InquiryComposeIntent.Submit)

            assertEquals(listOf("/files/ok.jpg"), repo.submissions.single().imageUrls)
        }

    @Test
    fun `사진은 세 장까지만 받는다`() =
        runTest {
            val repo = FakeInquiryRepository(upload = { "/files/ok.jpg" })
            val viewModel = viewModel(repo)

            repeat(4) { viewModel.onIntent(InquiryComposeIntent.ImagePicked("content://$it")) }

            assertEquals(3, viewModel.uiState.value.attachments.size)
            assertFalse(viewModel.uiState.value.canAddImage)
        }

    @Test
    fun `접수 중에는 두 번째 요청이 나가지 않는다`() =
        runTest {
            // 두 번 눌리면 같은 문의가 두 건 쌓이고 하루 상한 3건이 2건으로 줄어든다.
            val repo = FakeInquiryRepository(submit = { receipt() })
            val viewModel = viewModel(repo)
            viewModel.onIntent(InquiryComposeIntent.BodyChanged("기상 인증이 실패로 떴어요"))

            viewModel.onIntent(InquiryComposeIntent.Submit)
            viewModel.onIntent(InquiryComposeIntent.Submit)

            assertEquals(1, repo.submissions.size)
        }

    @Test
    fun `접수에 성공하면 접수번호가 상태에 남아 시트가 뜬다`() =
        runTest {
            val repo = FakeInquiryRepository(submit = { receipt(inquiryId = "abc-123") })
            val viewModel = viewModel(repo)
            viewModel.onIntent(InquiryComposeIntent.BodyChanged("기상 인증이 실패로 떴어요"))

            viewModel.onIntent(InquiryComposeIntent.Submit)

            assertEquals("abc-123", viewModel.uiState.value.receiptId)
        }

    @Test
    fun `하루 상한 초과는 서버 문구를 그대로 보여 준다`() =
        runTest {
            // 남은 건수와 초기화 시각은 서버만 아는 값이라 앱이 다시 쓰면 그 숫자가 사라진다.
            val message = "오늘 접수 가능한 3건을 모두 사용했어요. 내일 다시 시도해 주세요."
            val repo =
                FakeInquiryRepository(
                    submit = { throw InquiryException(InquiryFailure.DAILY_LIMIT, message) },
                )
            val viewModel = viewModel(repo)
            viewModel.onIntent(InquiryComposeIntent.BodyChanged("기상 인증이 실패로 떴어요"))

            viewModel.onIntent(InquiryComposeIntent.Submit)

            assertEquals(message, viewModel.uiState.value.errorMessage)
            assertNull(viewModel.uiState.value.receiptId)
        }

    @Test
    fun `분류 인자가 없으면 기타로 연다`() =
        runTest {
            // 분류 없이 보내면 서버가 400 으로 막는다. 폼을 띄우되 보낼 수 있는 값이어야 한다.
            val viewModel = viewModel(handle = SavedStateHandle())

            assertEquals(InquiryCategory.ERROR_ETC, viewModel.uiState.value.category)
        }

    @Test
    fun `라우트 인자로 받은 분류가 그대로 접수에 실린다`() =
        runTest {
            val repo = FakeInquiryRepository(submit = { receipt() })
            val viewModel = viewModel(repo, category = InquiryCategory.REPORT_SANCTION)
            viewModel.onIntent(InquiryComposeIntent.BodyChanged("제재 재검토를 요청합니다"))

            viewModel.onIntent(InquiryComposeIntent.Submit)

            assertEquals(InquiryCategory.REPORT_SANCTION, repo.submissions.single().category)
        }

    @Test
    fun `본문 앞뒤 공백은 접수 전에 정리된다`() =
        runTest {
            val repo = FakeInquiryRepository(submit = { receipt() })
            val viewModel = viewModel(repo)

            viewModel.onIntent(InquiryComposeIntent.BodyChanged("  기상 인증이 실패로 떴어요  "))
            viewModel.onIntent(InquiryComposeIntent.Submit)

            assertEquals(
                "기상 인증이 실패로 떴어요",
                repo.submissions
                    .single()
                    .body.value,
            )
        }

    @Test
    fun `접수 완료를 확인하면 화면을 닫는다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(FakeInquiryRepository(submit = { receipt() }), nav = nav)

            viewModel.onIntent(InquiryComposeIntent.ConfirmReceipt)

            assertTrue(nav.backCount > 0)
        }

    private fun viewModel(
        repo: FakeInquiryRepository = FakeInquiryRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
        category: InquiryCategory = InquiryCategory.VERIFICATION,
        handle: SavedStateHandle = SavedStateHandle(mapOf(InquiryComposePage.ARG_CATEGORY to category.value)),
    ) = InquiryComposeViewModel(
        savedStateHandle = handle,
        inquiryRepository = repo,
        navigationHelper = nav,
    )

    private fun receipt(inquiryId: String = "3d6ad414-5fb6-81aa-8d11-ca6ffd272529") =
        InquiryReceipt(
            inquiryId = inquiryId,
            status = InquiryStatus.RECEIVED,
            createdAt = "2026-09-11T10:00:00Z",
        )
}
