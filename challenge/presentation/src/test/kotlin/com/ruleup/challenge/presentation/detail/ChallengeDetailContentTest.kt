package com.ruleup.challenge.presentation.detail

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeGate
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeStats
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.JoinNote
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.presentation.clickPastGuard
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailIntent
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailState
import com.ruleup.challenge.presentation.renderScreen
import com.ruleup.domain.entity.category.Category
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

/**
 * 챌린지 상세. 네 갈래(로딩·조회 실패·방 홈·공개 상세)로 갈리는데, **못 불러온 것과 없는 것을
 * 섞으면** 사용자가 남의 방을 기웃거리다 튕긴 것처럼 느낀다.
 *
 * 상단바 제목은 회귀 방지다 — 비참여자에게 리터럴 "챌린지" 를 보여 주던 버그를 #372 에서
 * 고쳤다(Figma `1134:1291` 은 챌린지 제목).
 */
@RunWith(RobolectricTestRunner::class)
class ChallengeDetailContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `불러오는 중에는 오류도 내용도 보여 주지 않는다`() {
        render(ChallengeDetailState.initial.copy(isLoading = true))

        compose.onNodeWithText("챌린지를 불러오지 못했어요").assertDoesNotExist()
    }

    @Test
    fun `조회에 실패하면 사유를 보여 준다`() {
        render(ChallengeDetailState.initial.copy(isLoading = false, detail = null, errorMessage = "네트워크가 끊겼어요"))

        compose.onNodeWithText("네트워크가 끊겼어요").assertExists()
    }

    @Test
    fun `사유를 모르는 실패도 빈 화면으로 두지 않는다`() {
        // 아무 말도 없으면 사용자는 앱이 멈춘 줄 안다.
        render(ChallengeDetailState.initial.copy(isLoading = false, detail = null, errorMessage = null))

        compose.onNodeWithText("챌린지를 불러오지 못했어요").assertExists()
    }

    @Test
    fun `비참여자에게도 챌린지 제목을 보여 준다`() {
        // 리터럴 "챌린지" 를 띄우던 회귀(#372). 어느 방을 보고 있는지 모르면 참여 판단을 못 한다.
        // 제목은 상단바와 본문 양쪽에 나오므로 "리터럴이 아니다" 쪽으로 못 박는다.
        render(loaded(title = "평일 아침 헬스장 출석"))

        compose.onAllNodesWithText("평일 아침 헬스장 출석").onFirst().assertExists()
        compose.onNodeWithText("챌린지").assertDoesNotExist()
    }

    @Test
    fun `참여 버튼을 누르면 그 행동이 화면 밖으로 올라간다`() {
        var ctaClicked = false
        compose.renderScreen {
            ChallengeDetailContent(
                state = loaded(),
                ctaLabel = "참여하기",
                onIntent = {},
                onBack = {},
                onCta = { ctaClicked = true },
            )
        }

        compose.onNodeWithText("참여하기").clickPastGuard()

        assertTrue(ctaClicked)
    }

    @Test
    fun `뒤로 가기는 화면 밖으로 올라간다`() {
        var backed = false
        compose.renderScreen {
            ChallengeDetailContent(
                state = loaded(),
                ctaLabel = "참여하기",
                onIntent = {},
                onBack = { backed = true },
                onCta = {},
            )
        }

        compose.onNodeWithContentDescription("뒤로").clickPastGuard()

        assertTrue(backed)
    }

    private fun loaded(title: String = "평일 아침 헬스장 출석") = ChallengeDetailState.initial.copy(isLoading = false, detail = detail(title))

    private fun detail(title: String) =
        ChallengeDetail(
            challengeId = "ch1",
            title = title,
            description = "평일 오전, 등록한 헬스장에 도착하면",
            imageUrl = null,
            category = Category.entries.first(),
            mode = ChallengeMode.GROUP,
            visibility = ChallengeVisibility.PUBLIC,
            status = ChallengeStatus.ACTIVE,
            owner = null,
            ownerType = OwnerType.USER,
            participantCount = 3,
            capacity = 4,
            isFull = false,
            period = ChallengePeriod(start = "2026-09-01", end = "2026-10-13"),
            verification =
                VerificationConfig(
                    type = VerificationType.entries.first(),
                    method = VerificationMethod.entries.first(),
                ),
            stats = ChallengeStats(completionRate = null, retentionRate = null),
            gate = ChallengeGate(minTier = null, myDisplayTier = null, eligible = true),
            joinBlockReason = null,
            rejoinAvailableAt = null,
            joinNote = JoinNote.IMMEDIATE,
            cloneable = false,
            myRole = MemberRole.NONE,
            moderation = null,
        )

    private fun render(
        state: ChallengeDetailState,
        onIntent: (ChallengeDetailIntent) -> Unit = {},
    ) {
        compose.renderScreen {
            ChallengeDetailContent(
                state = state,
                ctaLabel = "참여하기",
                onIntent = onIntent,
                onBack = {},
                onCta = {},
            )
        }
    }
}
