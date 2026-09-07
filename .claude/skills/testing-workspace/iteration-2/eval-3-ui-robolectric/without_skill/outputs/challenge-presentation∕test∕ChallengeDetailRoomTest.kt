package com.ruleup.challenge.presentation.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.domain.entity.DelegationStatus
import com.ruleup.challenge.domain.entity.DelegationTicket
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailIntent
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailState
import com.ruleup.challenge.presentation.detail.viewmodel.RoomTab
import com.ruleup.verification.domain.entity.AppealChance
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.domain.entity.TodayResultStatus
import com.ruleup.verification.domain.entity.UnacknowledgedResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * 참여 중인 그룹 방(방 홈). 공개 상세와 **같은 화면이 다른 옷을 입는다** — 제목이 방 이름이 되고,
 * 관리 동작이 ⋯ 로 모이고, 본문이 3탭이 된다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChallengeDetailRoomTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `방에 들어오면 제목이 방 이름이 된다`() {
        compose.showDetail(roomState())

        compose.onNodeWithText("아침 6:30 기상").assertIsDisplayed()
        compose.onNodeWithText("챌린지").assertDoesNotExist()
    }

    @Test
    fun `방장이 아니면 상단바에 메뉴 자체가 없다`() {
        // 규칙 변경은 방장 전용이다 — 공동 관리자에게도 진입점을 만들지 않는다.
        compose.showDetail(roomState(room = room(myRole = MemberRole.MANAGER)))

        compose.onNodeWithContentDescription("더 보기").assertDoesNotExist()
    }

    @Test
    fun `방장은 상단바 메뉴에서 수정으로 간다`() {
        val harness = compose.showDetail(ownerRoom())

        compose.clickDescribed("더 보기")
        compose.clickAwaited("챌린지 수정")

        harness.last<ChallengeDetailIntent.OpenSettings>()
    }

    @Test
    fun `인증 권한이 끊기면 배너로 알린다`() {
        // 자동 인증은 조용히 멈춘다 — 알리지 않으면 매일 실패가 쌓이다 강퇴로 간다.
        val harness =
            compose.showDetail(
                roomState().copy(
                    setup = setup(requiredPermissions = listOf("LOCATION")),
                    permissions = permissions(location = PermissionState.DENIED),
                ),
            )

        compose.clickText("인증에 필요한 권한이 꺼져 있어요 · 다시 연결하기")

        harness.last<ChallengeDetailIntent.OpenPermissionRepair>()
    }

    @Test
    fun `권한 현황을 아직 모르면 배너를 올리지 않는다`() {
        // 모른다고 경고하면 조회 실패가 곧 "권한 끊김"으로 읽힌다.
        compose.showDetail(roomState().copy(permissions = null))

        compose.onNodeWithText("인증에 필요한 권한이 꺼져 있어요 · 다시 연결하기").assertDoesNotExist()
    }

    @Test
    fun `앱이 모르는 권한 토큰은 끊긴 것으로 세지 않는다`() {
        // 서버가 토큰을 추가했다고 구버전 앱이 경고 배너를 상시 달고 있으면 안 된다.
        compose.showDetail(
            roomState().copy(
                setup = setup(requiredPermissions = listOf("NEW_FANCY_PERMISSION")),
                permissions = permissions(),
            ),
        )

        compose.onNodeWithText("인증에 필요한 권한이 꺼져 있어요 · 다시 연결하기").assertDoesNotExist()
    }

    @Test
    fun `로딩 중에는 배너를 올리지 않는다`() {
        compose.showDetail(
            roomState().copy(
                isLoading = true,
                setup = setup(requiredPermissions = listOf("LOCATION")),
                permissions = permissions(location = PermissionState.DENIED),
            ),
        )

        compose.onNodeWithText("인증에 필요한 권한이 꺼져 있어요 · 다시 연결하기").assertDoesNotExist()
    }

    @Test
    fun `미확인 판정은 진입하자마자 모달로 알린다`() {
        val harness = compose.showDetail(unacknowledgedState())

        compose.awaitText("오늘 인증을 놓쳤어요")
        compose.clickAwaited("확인")

        harness.last<ChallengeDetailIntent.AcknowledgeResult>()
    }

    @Test
    fun `스켈레톤 위에는 판정 결과를 올리지 않는다`() {
        // 뒤에 뭐가 있는지 모른 채 결과부터 마주치게 된다.
        compose.showDetail(unacknowledgedState().copy(isLoading = true))

        compose.onNodeWithText("오늘 인증을 놓쳤어요").assertDoesNotExist()
    }

    @Test
    fun `한 번 확인한 판정은 다시 올리지 않는다`() {
        // ack 가 실패해도 같은 모달을 다시 세우지 않는다.
        compose.showDetail(unacknowledgedState().copy(resultAcknowledged = true))

        compose.onNodeWithText("오늘 인증을 놓쳤어요").assertDoesNotExist()
    }

    @Test
    fun `정보 탭 헤더는 남은 날과 내 달성률을 편다`() {
        compose.showDetail(roomState().copy(ranking = ranking(mySuccessRate = 0.92)))

        compose.onNodeWithText("기상·수면").assertIsDisplayed()
        compose.onNodeWithText("D-12").assertIsDisplayed()
        compose.onNodeWithText("92%").assertIsDisplayed()
    }

    @Test
    fun `종료일 당일은 D-0 이 아니라 D-day 다`() {
        compose.showDetail(roomState(room = room(remainingDays = 0)))

        compose.onNodeWithText("D-day").assertIsDisplayed()
    }

    @Test
    fun `아직 등재되지 않았으면 달성률을 0으로 접지 않는다`() {
        // 0% 로 채우면 집계 전인 사람이 실패한 사람처럼 보인다.
        compose.showDetail(roomState().copy(ranking = null))

        compose.onNodeWithText("-").assertExists()
    }

    @Test
    fun `탭을 누르면 그 탭을 요청한다`() {
        val harness = compose.showDetail(roomState())

        compose.clickText("피드")
        assertEquals(RoomTab.FEED, harness.last<ChallengeDetailIntent.SelectTab>().tab)

        compose.clickText("랭킹")
        assertEquals(RoomTab.RANKING, harness.last<ChallengeDetailIntent.SelectTab>().tab)
    }

    @Test
    fun `정보 탭은 오늘 내 인증부터 보여준다`() {
        compose.showDetail(roomState().copy(todayResult = todayResult(status = TodayResultStatus.DONE)))

        compose.onNodeWithText("오늘 내 인증").assertIsDisplayed()
        compose.onNodeWithText("인증 완료").assertIsDisplayed()
        compose.onNodeWithText("7일 연속 성공 중이에요").assertExists()
    }

    @Test
    fun `검사 중은 실패로 보이지 않게 말한다`() {
        // 확정 전 유예 구간이라 성공·실패 양쪽으로 열려 있다.
        compose.showDetail(
            roomState().copy(todayResult = todayResult(status = TodayResultStatus.CHECKING, streak = null)),
        )

        compose.onNodeWithText("검사중").assertIsDisplayed()
        compose.onNodeWithText("최종 결과를 계산하고 있어요").assertExists()
    }

    @Test
    fun `나가기는 확인을 한 번 받는다`() {
        val harness = compose.showDetail(memberRoomWithMembers())

        compose.clickText("챌린지 나가기", scroll = true)

        compose.awaitText("챌린지에서 나갈까요?")
        assertTrue(harness.intents.none { it is ChallengeDetailIntent.LeaveChallenge })

        compose.clickAwaited("나가기")
        harness.last<ChallengeDetailIntent.LeaveChallenge>()
    }

    @Test
    fun `확인 창을 취소하면 아무 일도 일어나지 않는다`() {
        val harness = compose.showDetail(memberRoomWithMembers())

        compose.clickText("챌린지 나가기", scroll = true)
        compose.clickAwaited("취소")

        assertTrue(harness.intents.isEmpty())
        compose.onNodeWithText("챌린지에서 나갈까요?").assertDoesNotExist()
    }

    @Test
    fun `참여자가 남은 방은 방장도 삭제할 수 없다`() {
        compose.showDetail(
            ownerRoom().copy(
                members = members(member("u_me", "나", MemberRole.OWNER), member("u_2", "철수"), participantCount = 3),
            ),
        )

        compose.onNodeWithText("챌린지 삭제").assertDoesNotExist()
        compose.onNodeWithText("참여자가 있는 동안에는 삭제할 수 없어요. 방장 위임 후 나갈 수 있어요").assertExists()
    }

    @Test
    fun `혼자 남은 방장은 삭제할 수 있다`() {
        val harness =
            compose.showDetail(
                ownerRoom().copy(
                    members = members(member("u_me", "나", MemberRole.OWNER), participantCount = 1),
                ),
            )

        compose.clickText("챌린지 삭제", scroll = true)
        compose.awaitText("챌린지를 삭제할까요?")
        compose.clickAwaited("삭제")

        harness.last<ChallengeDetailIntent.DeleteChallenge>()
    }

    @Test
    fun `방장은 멤버를 공동 관리자로 올린다`() {
        val harness =
            compose.showDetail(
                ownerRoom().copy(
                    members = members(member("u_me", "나", MemberRole.OWNER), member("u_2", "철수"), participantCount = 2),
                ),
            )

        compose.clickText("⋯", scroll = true)
        compose.clickAwaited("공동 관리자 임명")

        assertEquals("u_2", harness.last<ChallengeDetailIntent.PromoteMember>().userId)
    }

    @Test
    fun `위임 요청은 누구에게 갔는지 배너로 남는다`() {
        // 요청은 7일 살아 있다 — 화면에 흔적이 없으면 같은 요청을 다시 보낸다.
        val harness =
            compose.showDetail(
                ownerRoom().copy(
                    members = members(member("u_me", "나", MemberRole.OWNER), member("u_2", "철수", MemberRole.MANAGER)),
                    pendingDelegation =
                        DelegationTicket(
                            delegationId = "dg_1",
                            status = DelegationStatus.PENDING,
                            expiresAt = "2026-09-07T00:00:00+09:00",
                        ),
                    pendingDelegationNickname = "철수",
                ),
            )

        compose.onNodeWithText("철수님에게 방장 위임을 요청했어요").assertExists()
        compose.clickText("취소", scroll = true)

        harness.last<ChallengeDetailIntent.CancelDelegation>()
    }

    @Test
    fun `요청이 나가는 동안에는 관리 동작이 다시 눌리지 않는다`() {
        // 탈퇴·삭제·권한 변경은 되돌리기 어렵다 — 응답을 기다리는 동안 두 번째 요청이 나가면 안 된다.
        compose.showDetail(
            ownerRoom().copy(
                members = members(member("u_me", "나", MemberRole.OWNER), participantCount = 1),
                isMemberActionLoading = true,
            ),
        )

        compose.clickText("챌린지 삭제", scroll = true)

        compose.onNodeWithText("챌린지를 삭제할까요?").assertDoesNotExist()
    }

    private fun ownerRoom(): ChallengeDetailState =
        roomState(
            detail = detail(myRole = MemberRole.OWNER),
            room = room(myRole = MemberRole.OWNER),
        )

    private fun memberRoomWithMembers(): ChallengeDetailState =
        roomState().copy(
            members = members(member("u_me", "나"), member("u_2", "철수"), participantCount = 2),
        )

    private fun unacknowledgedState(): ChallengeDetailState =
        roomState().copy(
            todayResult =
                todayResult(
                    status = TodayResultStatus.FAILED,
                    confirmedAt = null,
                    streak = null,
                    unacknowledged = UnacknowledgedResult(verificationId = "v_1", result = "FAILED"),
                    appeal = AppealChance(eligibleUntil = null, eligible = true),
                ),
        )
}
