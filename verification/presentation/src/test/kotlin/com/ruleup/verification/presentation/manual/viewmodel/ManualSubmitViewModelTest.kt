package com.ruleup.verification.presentation.manual.viewmodel

import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.verification.domain.entity.AlreadyVerifiedException
import com.ruleup.verification.domain.entity.ManualSubmitResult
import com.ruleup.verification.domain.entity.TodayResult
import com.ruleup.verification.domain.entity.TodayResultStatus
import com.ruleup.verification.domain.entity.VerificationStreak
import com.ruleup.verification.domain.test.FakeVerificationRepository
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
 * 수동 인증 제출 화면의 전이.
 *
 * 이 화면의 계약은 셋이다 — **메모가 실제 요청에 실린다**, 제목을 못 받아도 체크는 된다,
 * 제출이 실패하면 서버 사실로 되돌리되 **이유는 지우지 않는다**. 셋 다 깨지면 사용자는 인증한 줄
 * 알고 하루를 넘긴다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ManualSubmitViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `제목 조회가 실패해도 오늘 상태로 화면을 세운다`() =
        runTest {
            // 제목 때문에 체크를 막으면 오늘 인증 자체를 못 한다. 체크에 필요한 건 challengeId 뿐이다.
            val repository =
                FakeVerificationRepository(
                    progress = { throw IllegalStateException("진행률 조회 실패") },
                    todayResult = { today(status = TodayResultStatus.IN_PROGRESS) },
                )
            val viewModel = viewModel(repository)

            viewModel.onIntent(ManualSubmitIntent.Load(CHALLENGE_ID))

            val state = viewModel.uiState.value
            assertEquals("", state.title)
            assertTrue(state.canSubmit)
        }

    @Test
    fun `메모를 적어 체크하면 그 메모가 요청에 실린다`() =
        runTest {
            // 이 화면이 따로 있는 이유가 메모다. 안 실리면 상세에서 바로 누르는 것과 다를 바 없다.
            var sentNote: String? = null
            val repository =
                FakeVerificationRepository(
                    progress = { throw IllegalStateException("쓰지 않는다") },
                    todayResult = { today(status = TodayResultStatus.IN_PROGRESS) },
                    submitManual = { _, _, note ->
                        sentNote = note
                        submitted()
                    },
                )
            val viewModel = viewModel(repository)
            viewModel.onIntent(ManualSubmitIntent.Load(CHALLENGE_ID))

            viewModel.onIntent(ManualSubmitIntent.NoteChanged("  아침 러닝 30분  "))
            viewModel.onIntent(ManualSubmitIntent.Submit)

            assertEquals("아침 러닝 30분", sentNote)
            assertTrue(viewModel.uiState.value.checked)
        }

    @Test
    fun `메모가 비어 있으면 note 를 보내지 않는다`() =
        runTest {
            // 빈 문자열을 보내면 서버 기록에 빈 메모가 남는다. 안 쓴 것과 빈 것은 다르다.
            var sent = "not-called"
            val repository =
                FakeVerificationRepository(
                    progress = { throw IllegalStateException("쓰지 않는다") },
                    todayResult = { today(status = TodayResultStatus.IN_PROGRESS) },
                    submitManual = { _, _, note ->
                        sent = note ?: "null"
                        submitted()
                    },
                )
            val viewModel = viewModel(repository)
            viewModel.onIntent(ManualSubmitIntent.Load(CHALLENGE_ID))

            viewModel.onIntent(ManualSubmitIntent.NoteChanged("   "))
            viewModel.onIntent(ManualSubmitIntent.Submit)

            assertEquals("null", sent)
        }

    @Test
    fun `이미 체크된 날이면 이유를 남긴 채 서버 상태로 되돌린다`() =
        runTest {
            // 되돌리기만 하고 문구를 지우면 사용자는 아무 일도 없었다고 읽는다.
            var done = false
            val repository =
                FakeVerificationRepository(
                    progress = { throw IllegalStateException("쓰지 않는다") },
                    todayResult = {
                        if (done) today(status = TodayResultStatus.DONE) else today(TodayResultStatus.IN_PROGRESS)
                    },
                    submitManual = { _, _, _ ->
                        done = true
                        throw AlreadyVerifiedException()
                    },
                )
            val viewModel = viewModel(repository)
            viewModel.onIntent(ManualSubmitIntent.Load(CHALLENGE_ID))

            viewModel.onIntent(ManualSubmitIntent.Submit)

            val state = viewModel.uiState.value
            assertTrue(state.checked)
            assertEquals("오늘은 이미 체크했어요", state.errorMessage)
            assertFalse(state.isLoading)
        }

    @Test
    fun `체크 해제는 오늘 인증 건 ID 로 부르고 서버 상태를 다시 받는다`() =
        runTest {
            // 해제하면 연속 일수의 기준이 바뀐다. 지우기만 하고 두면 화면 재진입 전까지
            // '아직 인증 전' 옆에 지난 연속 일수가 남는다(MAN-10 · VER-15).
            var cancelled: String? = null
            val repository =
                FakeVerificationRepository(
                    progress = { throw IllegalStateException("쓰지 않는다") },
                    todayResult = {
                        if (cancelled == null) {
                            today(status = TodayResultStatus.DONE)
                        } else {
                            // 해제하면 오늘 인증이 사라져 서버가 다시 계산한 값을 준다.
                            today(status = TodayResultStatus.IN_PROGRESS, streakAfter = 0)
                        }
                    },
                    cancelManual = { cancelled = it },
                )
            val viewModel = viewModel(repository)
            viewModel.onIntent(ManualSubmitIntent.Load(CHALLENGE_ID))

            viewModel.onIntent(ManualSubmitIntent.Uncheck)

            assertEquals(VERIFICATION_ID, cancelled)
            assertFalse(viewModel.uiState.value.checked)
            // 해제 전 값(9)이 남아 있으면 '아직 인증 전' 화면에 연속 일수가 같이 떠 있는다.
            assertEquals(0, viewModel.uiState.value.streakAfter)
        }

    @Test
    fun `오늘이 대상일이 아니면 체크 버튼을 열지 않는다`() =
        runTest {
            val repository =
                FakeVerificationRepository(
                    progress = { throw IllegalStateException("쓰지 않는다") },
                    todayResult = { today(status = TodayResultStatus.NOT_TARGET) },
                )
            val viewModel = viewModel(repository)

            viewModel.onIntent(ManualSubmitIntent.Load(CHALLENGE_ID))

            assertFalse(viewModel.uiState.value.canSubmit)
        }

    @Test
    fun `인자 없이 열리면 서버를 부르지 않는다`() =
        runTest {
            // 어느 챌린지인지 모르는 채 today 를 부르면 남의 방 상태를 물을 수도 있다.
            val repository = FakeVerificationRepository()
            val viewModel = viewModel(repository)

            viewModel.onIntent(ManualSubmitIntent.Load(""))

            assertEquals(emptyList(), repository.calls)
            assertEquals("어떤 챌린지인지 알 수 없어요", viewModel.uiState.value.errorMessage)
        }

    private fun viewModel(repository: FakeVerificationRepository) =
        ManualSubmitViewModel(
            verificationRepository = repository,
            navigationHelper = RecordingNavigationHelper(),
        )

    private fun today(
        status: TodayResultStatus,
        streakAfter: Int = 9,
    ) = TodayResult(
        date = "2026-09-14",
        verificationId = VERIFICATION_ID.takeIf { status == TodayResultStatus.DONE },
        status = status,
        window = "자정 마감",
        confirmedAt = null,
        failureReason = null,
        streak = VerificationStreak(before = 8, after = streakAfter),
        unacknowledged = null,
        appeal = null,
    )

    private fun submitted() =
        ManualSubmitResult(
            verificationId = VERIFICATION_ID,
            targetDate = "2026-09-14",
            status = TodayResultStatus.DONE,
            streak = VerificationStreak(before = 8, after = 9),
            scoreNote = "MANUAL_NO_SCORE",
        )

    private companion object {
        const val CHALLENGE_ID = "ch-1"
        const val VERIFICATION_ID = "vf-1"
    }
}
