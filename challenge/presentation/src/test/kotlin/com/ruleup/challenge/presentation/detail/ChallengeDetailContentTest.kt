package com.ruleup.challenge.presentation.detail

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeGate
import com.ruleup.challenge.domain.entity.ChallengeMember
import com.ruleup.challenge.domain.entity.ChallengeMembers
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengePenalties
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.ChallengeStats
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.JoinNote
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.RoomSummary
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.presentation.clickPastGuard
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailIntent
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailState
import com.ruleup.challenge.presentation.detail.viewmodel.RankingScope
import com.ruleup.challenge.presentation.detail.viewmodel.RoomTab
import com.ruleup.challenge.presentation.renderScreen
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.User
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

/** 챌린지 상세. */
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

    @Test
    fun `비참여자는 표지에서 상세 내용 보기와 가입 버튼을 본다`() {
        render(loaded())

        compose.onNodeWithText("상세 내용 보기").assertExists()
        // 셋업 단계에 따라 화면이 정한 문구(가입하기 · 권한 허용하기 …)가 그대로 붙는다
        compose.onNodeWithText("참여하기").assertExists()
        compose.onNodeWithText("들어가기").assertDoesNotExist()
    }

    @Test
    fun `참여 중인 사람은 표지 없이 바로 방에 들어가 정보 피드 랭킹 세 탭을 본다`() {
        // 시안 C 확정 안 2 — 오늘 인증·세부 설정·기록은 정보 탭에 모은다(#582).
        val member =
            loaded().copy(
                detail = detail("평일 아침 헬스장 출석").copy(myRole = MemberRole.MEMBER),
                room =
                    ChallengeRoom(
                        myRole = MemberRole.MEMBER,
                        ownerType = OwnerType.USER,
                        summary =
                            RoomSummary(
                                title = "평일 아침 헬스장 출석",
                                roomSuccessRate = null,
                                remainingDays = 12,
                                participantCount = 3,
                                capacity = 4,
                            ),
                        topRanking = emptyList(),
                        myTodayStatus = null,
                    ),
            )
        render(member)

        // 이미 가입한 방은 「상세 내용 보기 / 들어가기」 표지를 거치지 않는다(#585).
        compose.onNodeWithText("들어가기").assertDoesNotExist()
        compose.onNodeWithText("정보").assertExists()
        compose.onNodeWithText("피드").assertExists()
        compose.onNodeWithText("랭킹").assertExists()
        compose.onNodeWithText("인증 규칙").assertExists()
        // 바로가기는 조건과 상관없이 같은 자리에 있다. 캘린더는 아래에 펼쳐져 있어 버튼이 없다
        compose.onNodeWithText("오늘 체크하기").assertExists()
        compose.onNodeWithText("권한 다시 연결").assertExists()
        compose.onNodeWithText("캘린더").assertDoesNotExist()
    }

    @Test
    fun `상세 내용 보기에서 꺼진 벌칙은 숨기지 않고 꺼짐으로 보여 준다`() {
        val state =
            loaded().copy(
                detail = detail("평일 아침 헬스장 출석").copy(penalties = ChallengePenalties(score = true, groupShare = false, watcher = true)),
            )
        render(state)

        compose.onNodeWithText("상세 내용 보기").clickPastGuard()

        compose.onNodeWithText("그룹에 공유").assertExists()
        compose.onNodeWithText("꺼짐").assertExists()
    }

    @Test
    fun `표지의 참여 인원을 누르면 가입 전에도 멤버 목록을 보여 주고 나가기는 두지 않는다`() {
        // 누가 있는 방인지 보고 가입을 정한다(#582). 가입 전이라 나갈 방이 없다.
        val members =
            ChallengeMembers(
                challengeId = "ch1",
                participantCount = 3,
                capacity = 4,
                members =
                    listOf(
                        ChallengeMember(
                            user = User(id = "u2", nickname = "서연", profileImageUrl = null),
                            role = MemberRole.MEMBER,
                            tier = null,
                            joinedAt = "2026-09-02T00:00:00+09:00",
                        ),
                    ),
            )
        render(loaded().copy(members = members))

        compose.onNodeWithText("참여 중", substring = true).clickPastGuard()

        compose.onNodeWithText("멤버").assertExists()
        compose.onAllNodesWithText("서연").onFirst().assertExists()
        compose.onNodeWithText("챌린지 나가기").assertDoesNotExist()
    }

    @Test
    fun `솔로 방에도 피드와 랭킹이 있고 랭킹은 챌린지 순위만 보인다`() {
        // 솔로는 방 안 순위가 없다 — 솔로끼리 비교하는 챌린지 순위뿐이다(명세 「챌린지 외 랭킹 조회」).
        val solo =
            loaded().copy(
                detail = detail("매일 걷기").copy(mode = ChallengeMode.SOLO, myRole = MemberRole.OWNER),
                selectedTab = RoomTab.RANKING,
                rankingScope = RankingScope.ROOM,
            )
        render(solo)

        compose.onNodeWithText("피드").assertExists()
        compose.onNodeWithText("랭킹").assertExists()
        compose.onNodeWithText(RankingScope.MEMBER.label).assertDoesNotExist()
    }

    @Test
    fun `방 안 메뉴에는 알림 끄기 신고 나가기만 있다`() {
        // 세부 설정·감시자·기록은 정보 탭에 있다(#585).
        val member =
            loaded().copy(
                detail = detail("평일 아침 헬스장 출석").copy(myRole = MemberRole.MEMBER),
                isMuted = false,
            )
        render(member)

        compose.onNodeWithContentDescription("더 보기").clickPastGuard()

        // 알림 끄기는 정보 탭 본문에도 있어 시트 것과 둘이 된다
        compose.onAllNodesWithText("이 챌린지 알림 끄기").assertCountEquals(2)
        compose.onNodeWithText("챌린지 신고").assertExists()
        compose.onNodeWithText("챌린지 나가기").assertExists()
        compose.onNodeWithText("상세 내용 보기").assertDoesNotExist()
        compose.onNodeWithText("감시자 관리").assertDoesNotExist()
    }

    private fun loaded(title: String = "평일 아침 헬스장 출석") = ChallengeDetailState.initial.copy(isLoading = false, detail = detail(title))

    private fun detail(title: String) =
        ChallengeDetail(
            title = title,
            category = Category.entries.first(),
            imageUrl = null,
            challengeId = "ch1",
            description = "평일 오전, 등록한 헬스장에 도착하면",
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
