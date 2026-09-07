package com.ruleup.challenge.presentation.mychallenges

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.domain.entity.LeftType
import com.ruleup.challenge.presentation.mychallenges.viewmodel.FinishedPaging
import com.ruleup.challenge.presentation.mychallenges.viewmodel.MyChallengeSegment
import com.ruleup.challenge.presentation.mychallenges.viewmodel.MyChallengesIntent
import com.ruleup.challenge.presentation.mychallenges.viewmodel.MyChallengesState
import com.ruleup.challenge.presentation.renderScreen
import com.ruleup.verification.domain.entity.ChallengeProgress
import com.ruleup.verification.domain.entity.ProgressSnapshot
import com.ruleup.verification.domain.entity.TodayStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 내 챌린지 목록 (Figma 1134:1205 · 1162:2 · 1134:2085).
 *
 * 이 화면의 함정은 **아직 다 받지 않은 목록의 개수를 말하는 것**이다 — 받은 만큼만 세어 붙이면
 * 다음 장을 부를 때마다 숫자가 늘어 방이 새로 생긴 것처럼 보인다.
 */
@RunWith(RobolectricTestRunner::class)
class MyChallengesContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `참여한 방이 없으면 빈 목록 대신 둘러보기로 안내한다`() {
        render(MyChallengesState.initial.copy(isLoading = false))

        compose.onNodeWithText("아직 참여한 챌린지가 없어요").assertExists()
        compose.onNodeWithText("챌린지 둘러보기").assertExists()
    }

    @Test
    fun `조회에 실패하면 사유를 보여 준다`() {
        render(MyChallengesState.initial.copy(isLoading = false, errorMessage = "네트워크가 끊겼어요"))

        compose.onNodeWithText("네트워크가 끊겼어요").assertExists()
    }

    @Test
    fun `끝난 방을 아직 다 못 받았으면 개수를 붙이지 않는다`() {
        render(
            MyChallengesState.initial.copy(
                isLoading = false,
                inProgress = listOf(myChallenge()),
                finished = listOf(myChallenge(id = "done")),
                finishedPaging = FinishedPaging(null, completedHasNext = true, leftCursor = null, leftHasNext = false),
            ),
        )

        compose.onNodeWithText("완료 · 이탈").assertExists()
        compose.onNodeWithText("완료 · 이탈 1").assertDoesNotExist()
    }

    @Test
    fun `끝난 방을 다 받았으면 개수를 붙인다`() {
        render(
            MyChallengesState.initial.copy(
                isLoading = false,
                inProgress = listOf(myChallenge()),
                finished = listOf(myChallenge(id = "done")),
                finishedPaging = FinishedPaging(null, completedHasNext = false, leftCursor = null, leftHasNext = false),
            ),
        )

        compose.onNodeWithText("완료 · 이탈 1").assertExists()
    }

    @Test
    fun `이탈한 방은 완료와 구분해 표시한다`() {
        // 같은 뱃지로 그리면 강퇴당한 방이 완주한 방처럼 보인다.
        render(
            MyChallengesState.initial.copy(
                isLoading = false,
                segment = MyChallengeSegment.FINISHED,
                finished = listOf(myChallenge(id = "left", leftType = LeftType.KICK_FAIL, start = "2026-05-01", end = "2026-05-20")),
                finishedPaging = FinishedPaging(null, false, null, false),
            ),
        )

        compose.onNodeWithText("이탈").assertExists()
        compose.onNodeWithText("5.1 – 5.20 중단", substring = true).assertExists()
    }

    @Test
    fun `완료 방은 최종 성공률을 함께 보여 준다`() {
        render(
            MyChallengesState.initial.copy(
                isLoading = false,
                segment = MyChallengeSegment.FINISHED,
                finished = listOf(myChallenge(id = "done", successRate = 0.88)),
                finishedPaging = FinishedPaging(null, false, null, false),
            ),
        )

        compose.onNodeWithText("최종 88%").assertExists()
    }

    @Test
    fun `판정 이력이 없는 완료 방은 성공률 줄을 그리지 않는다`() {
        // 0% 로 접으면 하루도 판정받지 못하고 끝난 방과 전부 실패한 방이 같아 보인다.
        render(
            MyChallengesState.initial.copy(
                isLoading = false,
                segment = MyChallengeSegment.FINISHED,
                finished = listOf(myChallenge(id = "empty", successRate = null)),
                finishedPaging = FinishedPaging(null, false, null, false),
            ),
        )

        compose.onNodeWithText("최종", substring = true).assertDoesNotExist()
    }

    @Test
    fun `진행률을 못 받았으면 달성률을 지어내지 않는다`() {
        // 0% 로 그리면 아직 모르는 것이 실패로 보인다.
        render(MyChallengesState.initial.copy(isLoading = false, inProgress = listOf(myChallenge()), progress = null))

        compose.onNodeWithText("달성률").assertDoesNotExist()
    }

    @Test
    fun `진행률을 받으면 달성률을 함께 보여 준다`() {
        render(
            MyChallengesState.initial.copy(
                isLoading = false,
                inProgress = listOf(myChallenge(id = "ch1")),
                progress = snapshot("ch1", rate = 86.0, remainingDays = 12),
            ),
        )

        compose.onNodeWithText("달성률").assertExists()
        compose.onNodeWithText("86%").assertExists()
        compose.onNodeWithText("D-12").assertExists()
    }

    private fun snapshot(
        challengeId: String,
        rate: Double,
        remainingDays: Int,
    ) = ProgressSnapshot(
        asOf = "2026-09-01T00:00:00Z",
        challenges =
            listOf(
                ChallengeProgress(
                    challengeId = challengeId,
                    title = "아침 6시 기상",
                    category = null,
                    participationType = "GROUP",
                    status = "ACTIVE",
                    progressRate = rate,
                    successDays = 12,
                    targetDays = 14,
                    remainingDays = remainingDays,
                    todayTarget = true,
                    todayStatus = TodayStatus.PENDING,
                    lastSyncedAt = null,
                ),
            ),
    )

    private fun render(
        state: MyChallengesState,
        onIntent: (MyChallengesIntent) -> Unit = {},
    ) {
        compose.renderScreen { MyChallengesContent(state = state, onIntent = onIntent) }
    }
}
