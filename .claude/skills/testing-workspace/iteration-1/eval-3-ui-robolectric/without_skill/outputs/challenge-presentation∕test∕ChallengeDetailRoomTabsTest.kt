package com.ruleup.challenge.presentation.detail

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.TodayVerificationStatus
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailIntent
import com.ruleup.challenge.presentation.detail.viewmodel.RankingScope
import com.ruleup.challenge.presentation.detail.viewmodel.RoomTab
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 참여 중인 그룹 방의 3탭(정보·피드·랭킹) 렌더. `room` 이 있을 때만 열리는 화면이라
 * 공개 상세와는 상단바부터 다르다.
 *
 * 탭 전환은 상태가 밖에서 오므로(단일 상태 + 인텐트) 여기서는 **탭을 누르면 올라가는 인텐트**와
 * **그 탭이 선택된 상태의 렌더**를 따로 본다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChallengeDetailRoomTabsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `방에 들어와 있으면 상단바가 방 이름으로 바뀐다`() {
        compose.showDetail(loadedState(room = room()))

        compose.onNodeWithText("아침 6:30 기상").assertIsDisplayed()
        // 비멤버가 보는 공개 상세의 상단바("챌린지")로 떨어지면 안 된다.
        compose.onNodeWithText("챌린지").assertDoesNotExist()
        compose.onNodeWithText("정보").assertIsDisplayed()
        compose.onNodeWithText("피드").assertIsDisplayed()
        compose.onNodeWithText("랭킹").assertIsDisplayed()
    }

    @Test
    fun `뒤로는 화면 콜백으로 나간다`() {
        var backs = 0
        compose.showDetail(loadedState(room = room()), onBack = { backs++ })

        compose.clickDescription("뒤로")

        assertEquals(1, backs)
    }

    @Test
    fun `방장에게만 상단바 관리 메뉴가 생긴다`() {
        val intents = RecordedIntents()
        compose.showDetail(loadedState(room = room(myRole = MemberRole.OWNER)), onIntent = intents::record)

        compose.clickDescription("더 보기")
        compose.clickText("챌린지 수정")

        assertEquals(ChallengeDetailIntent.OpenSettings, intents.last)
    }

    @Test
    fun `일반 멤버에게는 관리 메뉴 자리를 만들지 않는다`() {
        compose.showDetail(loadedState(room = room(myRole = MemberRole.MEMBER)))

        compose.onNodeWithContentDescription("더 보기").assertDoesNotExist()
    }

    @Test
    fun `정보 탭 헤더는 남은 날과 내 달성률을 세운다`() {
        compose.showDetail(loadedState(room = room(remainingDays = 12)).copy(ranking = ranking(mySuccessRate = 0.75)))

        compose.onNodeWithText("기상·수면").assertExists()
        compose.onNodeWithText("D-12").assertExists()
        compose.onNodeWithText("75%").assertExists()
    }

    @Test
    fun `랭킹에 등재되기 전에는 달성률을 0으로 접지 않는다`() {
        // 0% 로 채우면 아직 집계되지 않은 것이 실패로 보인다.
        compose.showDetail(loadedState(room = room()).copy(ranking = null))

        compose.onNodeWithText("-").assertExists()
    }

    @Test
    fun `종료일 당일은 D-0 이 아니라 D-day 로 적는다`() {
        compose.showDetail(loadedState(room = room(remainingDays = 0)))

        compose.onNodeWithText("D-day").assertExists()
    }

    @Test
    fun `정보 탭은 오늘 상태와 인증 규칙 진행 정보를 함께 편다`() {
        compose.showDetail(
            loadedState(room = room(myTodayStatus = TodayVerificationStatus.DONE, roomSuccessRate = 0.8)),
        )

        compose.onNodeWithText("오늘 내 인증").assertExists()
        compose.onNodeWithText("인증 완료").assertExists()
        compose.onNodeWithText("인증 규칙").assertExists()
        compose.onNodeWithText("8.1 – 8.28 · 4주").performScrollTo().assertExists()
        compose.onNodeWithText("12명 / 정원 30명 · 방장 홍길동").performScrollTo().assertExists()
        compose.onNodeWithText("80%").performScrollTo().assertExists()
    }

    @Test
    fun `판정 이력이 없는 방은 성공률을 0퍼센트로 적지 않는다`() {
        compose.showDetail(loadedState(room = room(roomSuccessRate = null)))

        compose.onNodeWithText("아직 집계 전").performScrollTo().assertExists()
    }

    @Test
    fun `등록할 게 없는 인증 방식에는 세부 설정 줄을 만들지 않는다`() {
        // 눌러도 아무것도 없는 항목이 남으면 설정이 빠진 것처럼 읽힌다.
        compose.showDetail(loadedState(room = room()).copy(setup = setup()))

        compose.onNodeWithText("내 세부 설정").assertDoesNotExist()
        compose.onNodeWithText("대상 앱").assertDoesNotExist()
    }

    @Test
    fun `대상 앱이 필요한 방만 앱 등록 진입점을 연다`() {
        val intents = RecordedIntents()
        compose.showDetail(
            loadedState(room = room()).copy(setup = setup(requiresTargetPackages = true)),
            onIntent = intents::record,
        )

        compose.onNodeWithText("대상 앱").performScrollTo().assertExists()
        compose.onNodeWithText("수정").performScrollTo().performGuardedClick()

        assertEquals(ChallengeDetailIntent.RegisterApps, intents.last)
    }

    @Test
    fun `탭을 누르면 전환 인텐트가 올라간다`() {
        val intents = RecordedIntents()
        compose.showDetail(loadedState(room = room()), onIntent = intents::record)

        compose.clickText("피드")

        assertEquals(ChallengeDetailIntent.SelectTab(RoomTab.FEED), intents.last)
    }

    @Test
    fun `피드와 랭킹 탭에서는 정보 탭 헤더를 접는다`() {
        // 목록이 화면을 꽉 채워야 해서 상단바와 탭만 남긴다.
        compose.showDetail(loadedState(room = room()).copy(selectedTab = RoomTab.FEED))

        compose.onNodeWithText("내 달성률").assertDoesNotExist()
        compose.onNodeWithText("D-12").assertDoesNotExist()
    }

    @Test
    fun `피드 첫 페이지를 받는 동안에는 스켈레톤만 둔다`() {
        compose.showDetail(
            loadedState(room = room()).copy(
                selectedTab = RoomTab.FEED,
                isThreadsLoading = true,
                threads = emptyList(),
            ),
        )

        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
    }

    @Test
    fun `피드 첫 페이지가 실패하면 다시 불러오기를 준다`() {
        val intents = RecordedIntents()
        compose.showDetail(
            loadedState(room = room()).copy(
                selectedTab = RoomTab.FEED,
                threadsError = "피드를 불러오지 못했어요",
            ),
            onIntent = intents::record,
        )

        compose.onNodeWithText("피드를 불러오지 못했어요").assertExists()
        compose.clickText("다시 불러오기")

        assertEquals(ChallengeDetailIntent.RetryThreads, intents.last)
    }

    @Test
    fun `봇방장 방의 빈 피드는 방장 되기로 유도한다`() {
        val intents = RecordedIntents()
        compose.showDetail(
            loadedState(room = room(ownerType = OwnerType.BOT, myRole = MemberRole.MEMBER)).copy(
                selectedTab = RoomTab.FEED,
            ),
            onIntent = intents::record,
        )

        compose.onNodeWithText("아직 소식이 없어요\n이 방은 방장 자리가 비어 있어요").assertExists()
        compose.clickText("방장 되기")

        assertEquals(ChallengeDetailIntent.ClaimOwner, intents.last)
    }

    @Test
    fun `방장이 있는 방의 빈 피드에는 방장 되기를 두지 않는다`() {
        // 이미 방장이 있는 방에서 누르면 서버가 409 로 막는다 — 버튼을 아예 만들지 않는다.
        compose.showDetail(
            loadedState(room = room(ownerType = OwnerType.USER)).copy(selectedTab = RoomTab.FEED),
        )

        compose.onNodeWithText("방장 되기").assertDoesNotExist()
        compose.onNodeWithText("곧 멤버들의 인증 소식이 올라와요").assertExists()
    }

    @Test
    fun `피드는 성공과 실패를 글자로도 구분한다`() {
        compose.showDetail(
            loadedState(room = room()).copy(
                selectedTab = RoomTab.FEED,
                threads = listOf(successThread(), failThread()),
            ),
        )

        compose.onNodeWithText("홍길동").assertExists()
        compose.onNodeWithText("06:12 · 연속 3일").assertExists()
        compose.onNodeWithText("성공").assertExists()
        compose.onNodeWithText("김관리").assertExists()
        // 실패는 늦게 흐르므로 날짜를 명시한 과거형이어야 지금 실패한 것처럼 읽히지 않는다.
        compose.onNodeWithText("7월 24일 인증을 놓쳤어요").assertExists()
        compose.onNodeWithText("실패").assertExists()
    }

    @Test
    fun `이어받기 실패는 이미 받은 목록을 지우지 않는다`() {
        compose.showDetail(
            loadedState(room = room()).copy(
                selectedTab = RoomTab.FEED,
                threads = listOf(successThread()),
                threadsError = "이어서 불러오지 못했어요",
            ),
        )

        compose.onNodeWithText("홍길동").assertExists()
        compose.onNodeWithText("이어서 불러오지 못했어요").assertExists()
    }

    @Test
    fun `멤버 랭킹은 내 요약과 등재 기준을 함께 보여준다`() {
        compose.showDetail(
            loadedState(room = room()).copy(
                selectedTab = RoomTab.RANKING,
                ranking = ranking(),
                myUserId = "u-me",
            ),
        )

        compose.onNodeWithText("내 순위").assertExists()
        compose.onNodeWithText("성공률").assertExists()
        compose.onNodeWithText("나나 (나)").assertExists()
        // 미등재자를 목록에서 빼면 자기가 왜 안 보이는지 알 수 없고 남의 순번도 어긋난다.
        compose.onNodeWithText("새싹").assertExists()
        compose.onNodeWithText("인증 10회 미만").assertExists()
        compose.onNodeWithText("인증 10회부터 순위에 올라요").performScrollTo().assertExists()
    }

    @Test
    fun `멤버 랭킹 조회에 실패하면 빈 목록이 아니라 실패라고 말한다`() {
        compose.showDetail(
            loadedState(room = room()).copy(
                selectedTab = RoomTab.RANKING,
                ranking = null,
                isRankingLoading = false,
            ),
        )

        compose.onNodeWithText("랭킹을 불러오지 못했어요").assertExists()
    }

    @Test
    fun `세그먼트를 누르면 방 순위로 전환 인텐트가 올라간다`() {
        val intents = RecordedIntents()
        compose.showDetail(
            loadedState(room = room()).copy(selectedTab = RoomTab.RANKING, ranking = ranking()),
            onIntent = intents::record,
        )

        compose.clickText("방 순위")

        assertEquals(ChallengeDetailIntent.SelectRankingScope(RankingScope.ROOM), intents.last)
    }

    @Test
    fun `방 순위는 우리 방을 표시해 목록에서 찾게 한다`() {
        compose.showDetail(
            loadedState(room = room()).copy(
                selectedTab = RoomTab.RANKING,
                rankingScope = RankingScope.ROOM,
                crossRanking = crossRanking(),
            ),
        )

        compose.onNodeWithText("우리 방").assertExists()
        compose.onNodeWithText("새벽 러닝").assertExists()
        compose.onNodeWithText("아침 6:30 기상 (우리 방)").assertExists()
        compose.onNodeWithText("20명 · 누적 300회").assertExists()
    }

    @Test
    fun `일반 멤버에게는 나가기만 열고 나가기 전에 한 번 묻는다`() {
        val intents = RecordedIntents()
        compose.showDetail(
            loadedState(room = room(myRole = MemberRole.MEMBER)).copy(members = members(), myUserId = "u-me"),
            onIntent = intents::record,
        )

        compose.onNodeWithText("챌린지 삭제").assertDoesNotExist()
        compose.onNodeWithText("챌린지 나가기").performScrollTo().performGuardedClick()

        compose.onNodeWithText("챌린지에서 나갈까요?").assertExists()
        compose.clickText("나가기")

        assertEquals(ChallengeDetailIntent.LeaveChallenge, intents.last)
    }

    @Test
    fun `방장은 혼자 남았을 때만 삭제할 수 있다`() {
        val intents = RecordedIntents()
        compose.showDetail(
            loadedState(room = room(myRole = MemberRole.OWNER)).copy(
                members = members(participantCount = 1, members = listOf(member("u-owner", "홍길동", MemberRole.OWNER))),
                myUserId = "u-owner",
            ),
            onIntent = intents::record,
        )

        compose.onNodeWithText("챌린지 삭제").performScrollTo().performGuardedClick()
        compose.onNodeWithText("챌린지를 삭제할까요?").assertExists()
        compose.clickText("삭제")

        assertEquals(ChallengeDetailIntent.DeleteChallenge, intents.last)
    }

    @Test
    fun `참여자가 남아 있는 방장에게는 삭제 대신 이유를 적는다`() {
        compose.showDetail(
            loadedState(room = room(myRole = MemberRole.OWNER)).copy(members = members(participantCount = 3)),
        )

        compose.onNodeWithText("챌린지 삭제").assertDoesNotExist()
        compose
            .onNodeWithText("참여자가 있는 동안에는 삭제할 수 없어요. 방장 위임 후 나갈 수 있어요")
            .performScrollTo()
            .assertExists()
    }

    @Test
    fun `확인 다이얼로그를 취소하면 아무것도 올라가지 않는다`() {
        val intents = RecordedIntents()
        compose.showDetail(
            loadedState(room = room(myRole = MemberRole.MEMBER)).copy(members = members()),
            onIntent = intents::record,
        )

        compose.onNodeWithText("챌린지 나가기").performScrollTo().performGuardedClick()
        compose.clickText("취소")

        compose.onNodeWithText("챌린지에서 나갈까요?").assertDoesNotExist()
        assertTrue(intents.all.isEmpty())
    }
}
