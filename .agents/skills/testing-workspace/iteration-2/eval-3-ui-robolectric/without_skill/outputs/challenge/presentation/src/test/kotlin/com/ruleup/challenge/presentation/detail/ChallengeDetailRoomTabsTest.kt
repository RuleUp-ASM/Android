package com.ruleup.challenge.presentation.detail

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.ThreadItemType
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailIntent
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailState
import com.ruleup.challenge.presentation.detail.viewmodel.RankingScope
import com.ruleup.challenge.presentation.detail.viewmodel.RoomTab
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * 방 상세의 피드·랭킹 탭. 둘 다 **비었을 때가 기본 상태**인 목록이라(자동 인증은 아무도 버튼을 누르지
 * 않는다) 빈 화면이 무엇을 말하는지가 목록 그 자체만큼 중요하다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ChallengeDetailRoomTabsTest {
    @get:Rule
    val compose = createComposeRule()

    private val today: LocalDate = LocalDate.now()

    @Test
    fun `첫 페이지를 받는 동안에는 스켈레톤만 있다`() {
        compose.showDetail(feed().copy(isThreadsLoading = true))

        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun `피드는 날짜로 묶고 성공과 실패를 글자로도 가른다`() {
        // 색만으로 가르면 색각 이상 사용자에게는 같은 카드다.
        compose.showDetail(
            feed().copy(
                threads =
                    listOf(
                        thread("t1", "철수", at(daysAgo = 0), streak = 3),
                        thread("t2", "영희", at(daysAgo = 1), userId = "u_me"),
                        thread(
                            "t3",
                            "민수",
                            at(daysAgo = 1),
                            type = ThreadItemType.VERIFY_FAIL,
                            failDate = today.minusDays(2).toString(),
                        ),
                    ),
            ),
        )

        compose.onNodeWithText("오늘").assertExists()
        compose.onNodeWithText("어제").assertExists()
        compose.onNodeWithText("06:12 · 연속 3일").assertExists()
        // 내 카드는 이름으로 표시된다 — 강조색만으로는 구분이 남지 않는다.
        compose.onNodeWithText("영희 (나)").assertExists()
        compose.onAllNodesWithText("성공").assertCountEquals(2)
        compose.onAllNodesWithText("실패").assertCountEquals(1)
    }

    @Test
    fun `연속 하루짜리는 자랑거리로 적지 않는다`() {
        compose.showDetail(
            feed().copy(threads = listOf(thread("t1", "철수", at(daysAgo = 0), streak = 1))),
        )

        compose.onNodeWithText("06:12").assertExists()
        compose.onNodeWithText("06:12 · 연속 1일").assertDoesNotExist()
    }

    @Test
    fun `피드를 못 받으면 다시 받을 길을 준다`() {
        val harness = compose.showDetail(feed().copy(threadsError = "피드를 불러오지 못했어요"))

        compose.onNodeWithText("피드를 불러오지 못했어요").assertExists()
        compose.clickText("다시 불러오기")

        harness.last<ChallengeDetailIntent.RetryThreads>()
    }

    @Test
    fun `이어받기에 실패해도 이미 받은 목록은 지우지 않는다`() {
        // 목록이 사라지면 스크롤 위치도 같이 사라진다.
        compose.showDetail(
            feed().copy(
                threads = listOf(thread("t1", "철수", at(daysAgo = 0))),
                threadsError = "더 불러오지 못했어요",
            ),
        )

        compose.onNodeWithText("철수").assertExists()
        compose.onNodeWithText("다시 불러오기").assertExists()
    }

    @Test
    fun `봇방장 방의 빈 피드는 방장 자리를 권한다`() {
        val harness = compose.showDetail(feed(ownerType = OwnerType.BOT))

        compose.onNodeWithText("아직 소식이 없어요\n이 방은 방장 자리가 비어 있어요").assertExists()
        compose.clickText("방장 되기")

        harness.last<ChallengeDetailIntent.ClaimOwner>()
    }

    @Test
    fun `이미 방장이 있는 방에는 방장 되기를 만들지 않는다`() {
        // 눌러도 409 로 막힌다 — 버튼을 아예 만들지 않는 쪽이 맞다.
        compose.showDetail(feed(ownerType = OwnerType.USER, myRole = MemberRole.MEMBER))

        compose.onNodeWithText("방장 되기").assertDoesNotExist()
        compose.onNodeWithText("곧 멤버들의 인증 소식이 올라와요").assertExists()
    }

    @Test
    fun `방장에게는 없는 기능 대신 기다림을 설명한다`() {
        compose.showDetail(feed(ownerType = OwnerType.USER, myRole = MemberRole.OWNER))

        compose.onNodeWithText("멤버들의 인증이 확정되면 여기에 쌓여요", substring = true).assertExists()
        compose.onNodeWithText("방장 되기").assertDoesNotExist()
    }

    @Test
    fun `랭킹 세그먼트는 비교 단위를 바꾼다`() {
        val harness = compose.showDetail(rankingTab())

        compose.clickText("방 순위")

        assertEquals(RankingScope.ROOM, harness.last<ChallengeDetailIntent.SelectRankingScope>().scope)
    }

    @Test
    fun `랭킹을 받는 동안에는 스켈레톤만 있다`() {
        compose.showDetail(rankingTab().copy(ranking = null, isRankingLoading = true))

        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test
    fun `랭킹을 못 받으면 빈 목록으로 위장하지 않는다`() {
        compose.showDetail(rankingTab().copy(ranking = null, isRankingLoading = false))

        compose.onNodeWithText("랭킹을 불러오지 못했어요").assertExists()
    }

    @Test
    fun `멤버 랭킹은 내 자리를 맨 위에 둔다`() {
        compose.showDetail(
            rankingTab().copy(
                ranking =
                    ranking(
                        myRank = 3,
                        mySuccessRate = 0.92,
                        items =
                            listOf(
                                rankEntry(rank = 1, userId = "u_1", nickname = "철수", successRate = 0.98),
                                rankEntry(rank = 2, userId = "u_2", nickname = "영희", successRate = 0.9),
                            ),
                    ),
                todayResult = todayResult(),
            ),
        )

        compose.onNodeWithText("내 순위").assertExists()
        compose.onNodeWithText("92").assertExists()
        // 연속 일수는 랭킹이 아니라 오늘 결과에서 온다.
        compose.onNodeWithText("연속").assertExists()
        compose.onNodeWithText("98%").assertExists()
        compose.onNodeWithText("인증 10회부터 순위에 올라요").assertExists()
    }

    @Test
    fun `오늘 결과가 없으면 연속 대신 참여 횟수를 보여준다`() {
        // 등재까지 얼마나 남았는지가 그다음으로 쓸모 있는 값이다.
        compose.showDetail(
            rankingTab().copy(ranking = ranking(participations = 24), todayResult = null),
        )

        compose.onNodeWithText("참여").assertExists()
        compose.onNodeWithText("24").assertExists()
        compose.onNodeWithText("연속").assertDoesNotExist()
    }

    @Test
    fun `등재 미달자는 목록에서 빼지 않고 등수만 비운다`() {
        // 빼 버리면 자기가 왜 안 보이는지 알 수 없고, 남은 사람들의 순번도 어긋난다.
        compose.showDetail(
            rankingTab().copy(
                ranking =
                    ranking(
                        items =
                            listOf(
                                rankEntry(rank = 1, userId = "u_1", nickname = "철수", successRate = 0.98),
                                rankEntry(rank = null, userId = "u_9", nickname = "막내", successRate = null),
                            ),
                    ),
            ),
        )

        compose.onNodeWithText("막내").assertExists()
        compose.onNodeWithText("인증 10회 미만").assertExists()
    }

    @Test
    fun `방 순위는 우리 방을 표시하고 갱신 주기를 밝힌다`() {
        // "방금 인증했는데 왜 안 오르지"에 대한 답이 이 각주다.
        compose.showDetail(
            rankingTab(scope = RankingScope.ROOM).copy(
                crossRanking =
                    crossRanking(
                        myRank = 7,
                        items =
                            listOf(
                                challengeRankEntry(rank = 1, challengeId = CHALLENGE_ID, title = "아침 6:30 기상"),
                                challengeRankEntry(rank = 2, challengeId = "ch_2", title = "달리기 방"),
                            ),
                    ),
            ),
        )

        compose.onNodeWithText("우리 방").assertExists()
        compose.onNodeWithText("7").assertExists()
        compose.onNodeWithText("아침 6:30 기상 (우리 방)").assertExists()
        compose.onNodeWithText("12명 · 누적 60회").assertExists()
        compose.onNodeWithText("매일 1회 갱신", substring = true).assertExists()
    }

    @Test
    fun `방 순위를 못 받으면 멤버 랭킹과 다른 문구로 알린다`() {
        compose.showDetail(
            rankingTab(scope = RankingScope.ROOM).copy(crossRanking = null, isCrossRankingLoading = false),
        )

        compose.onNodeWithText("방 순위를 불러오지 못했어요").assertExists()
    }

    @Test
    fun `아직 오른 방이 없으면 그 사실을 적는다`() {
        compose.showDetail(
            rankingTab(scope = RankingScope.ROOM).copy(crossRanking = crossRanking(items = emptyList())),
        )

        compose.onNodeWithText("아직 순위에 오른 방이 없어요").assertExists()
    }

    /** 서버가 KST 오프셋으로 내려주는 시각. 날짜 헤더가 "오늘"·"어제"로 고정되도록 오늘 기준으로 만든다. */
    private fun at(
        daysAgo: Long,
        time: String = "06:12",
    ): String = "${today.minusDays(daysAgo)}T$time:00+09:00"

    private fun feed(
        ownerType: OwnerType = OwnerType.USER,
        myRole: MemberRole = MemberRole.MEMBER,
    ): ChallengeDetailState =
        roomState(
            detail = detail(myRole = myRole),
            room = room(myRole = myRole, ownerType = ownerType),
        ).copy(selectedTab = RoomTab.FEED)

    private fun rankingTab(scope: RankingScope = RankingScope.MEMBER): ChallengeDetailState =
        roomState().copy(
            selectedTab = RoomTab.RANKING,
            rankingScope = scope,
            ranking = ranking(),
            todayResult = null,
        )
}
