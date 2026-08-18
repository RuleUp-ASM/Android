package com.ruleup.challenge.domain.observability

import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.domain.entity.category.Category
import com.ruleup.observability.domain.model.attributes
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 팩토리 출력을 그대로 고정한다. 이벤트 이름·키·값 타입이 곧 분석 백엔드와의 계약이라, 이름 하나가
 * 바뀌면 대시보드가 조용히 비는데 컴파일은 그대로 통과한다.
 */
class ChallengeEventsTest {
    @Test
    fun `기능 스펙 9번의 탐색 이벤트 이름을 전부 고정한다`() {
        // 14종 중 하나라도 이름이 바뀌면 전환율 계산이 끊긴다.
        val names =
            listOf(
                ChallengeEvents.exploreHomeView(true),
                ChallengeEvents.trendingImpression(listOf("a")),
                ChallengeEvents.categoryGridClick("EXERCISE", 3),
                ChallengeEvents.exploreListView(ExploreListEntry.ALL, ExploreSort.POPULAR, ExploreFilter.none),
                ChallengeEvents.exploreFilterApply(ExploreFilter.none, 0),
                ChallengeEvents.exploreSortChange(ExploreSort.POPULAR, ExploreSort.RECENT, 0),
                ChallengeEvents.exploreEmptyResult(ExploreFilter.none, ExploreSort.POPULAR),
                ChallengeEvents.challengeCardImpression("c1", 0, ExploreSort.POPULAR, false, true, true),
                ChallengeEvents.challengeCardClick("c1", 0, ChallengeCardSource.LIST, ExploreSort.POPULAR),
                ChallengeEvents.challengeDetailView("c1", ChallengeCardSource.LIST, true, false),
                ChallengeEvents.challengeJoinAttempt("c1", true, false),
                ChallengeEvents.challengeJoinResult("c1", true),
                ChallengeEvents.challengeCloneClick("c1"),
                ChallengeEvents.exploreListLoadMore(1, ExploreSort.POPULAR),
            ).map { it.name }

        assertEquals(
            listOf(
                "explore_home_view",
                "trending_impression",
                "category_grid_click",
                "explore_list_view",
                "explore_filter_apply",
                "explore_sort_change",
                "explore_empty_result",
                "challenge_card_impression",
                "challenge_card_click",
                "challenge_detail_view",
                "challenge_join_attempt",
                "challenge_join_result",
                "challenge_clone_click",
                "explore_list_load_more",
            ),
            names,
        )
    }

    @Test
    fun `방 내부 이벤트 이름을 고정한다`() {
        val names =
            listOf(
                ChallengeEvents.roomView("c1", "MEMBER", "USER", false),
                ChallengeEvents.threadScroll(1, 20),
                ChallengeEvents.rankingView(RankingViewScope.IN_ROOM, false),
                ChallengeEvents.roomEmptyStateView("BOT"),
            ).map { it.name }

        assertEquals(
            listOf("room_view", "thread_scroll", "ranking_view", "room_empty_state_view"),
            names,
        )
    }

    @Test
    fun `방 진입은 역할과 방장 유형을 함께 싣는다`() {
        // 봇방장 방과 유저 방장 방은 화면 구성이 다르다 — 둘을 섞으면 방문율 해석이 안 된다.
        val event = ChallengeEvents.roomView("c1", "OWNER", "BOT", hasPinnedNotice = true)

        assertEquals(
            attributes {
                put("challenge_id", "c1")
                put("my_role", "OWNER")
                put("owner_type", "BOT")
                put("has_pinned_notice", true)
            },
            event.attrs,
        )
    }

    @Test
    fun `랭킹 조회는 방 안과 방 밖을 다른 scope 로 남긴다`() {
        // 등재 기준(10회 대 50회)이 달라 my_rank_null 을 한 지표로 묶으면 해석이 안 된다.
        assertEquals(
            attributes {
                put("scope", "IN_ROOM")
                put("my_rank_null", true)
            },
            ChallengeEvents.rankingView(RankingViewScope.IN_ROOM, myRankNull = true).attrs,
        )
        assertEquals(
            attributes {
                put("scope", "CROSS")
                put("my_rank_null", false)
            },
            ChallengeEvents.rankingView(RankingViewScope.CROSS, myRankNull = false).attrs,
        )
    }

    @Test
    fun `Android 담당 생성 이벤트 3종만 여기 있다`() {
        // 나머지(draft_request·challenge_create_result·moderation_result 등)는 BE 담당이라 클라가 보내지 않는다.
        assertEquals("create_start", ChallengeEvents.createStart(CreateEntry.HOME).name)
        assertEquals("create_path_select", ChallengeEvents.createPathSelect(CreatePath.TEMPLATE).name)
        assertEquals("draft_edit", ChallengeEvents.draftEdit(DraftField.TITLE).name)
    }

    @Test
    fun `카드 노출은 표본 부족 여부를 함께 싣는다`() {
        // has_metrics 가 없으면 표본 부족 처리가 클릭률을 얼마나 깎는지 분석할 수 없다.
        val event =
            ChallengeEvents.challengeCardImpression(
                challengeId = "c1",
                position = 2,
                sort = ExploreSort.COMPLETION_RATE,
                isFull = true,
                eligible = false,
                hasMetrics = false,
            )

        assertEquals(
            attributes {
                put("challenge_id", "c1")
                put("position", 2L)
                put("sort", "COMPLETION_RATE")
                put("is_full", true)
                put("eligible", false)
                put("has_metrics", false)
            },
            event.attrs,
        )
    }

    @Test
    fun `인기 섹션 클릭에는 sort 키를 아예 넣지 않는다`() {
        // 인기 섹션은 정렬 개념이 없다 — 빈 문자열을 넣으면 집계에 가짜 분류가 하나 생긴다.
        val event = ChallengeEvents.challengeCardClick("c1", 0, ChallengeCardSource.TRENDING, sort = null)

        assertEquals(
            attributes {
                put("challenge_id", "c1")
                put("position", 0L)
                put("source", "trending")
            },
            event.attrs,
        )
    }

    @Test
    fun `참여 성공에는 error_code 키를 넣지 않는다`() {
        val event = ChallengeEvents.challengeJoinResult("c1", success = true)

        assertEquals(
            attributes {
                put("challenge_id", "c1")
                put("success", true)
            },
            event.attrs,
        )
    }

    @Test
    fun `필터 없는 조회는 filters 를 none 으로 남긴다`() {
        // 빈 문자열이면 집계에서 "결측"과 구분되지 않는다.
        val event = ChallengeEvents.exploreEmptyResult(ExploreFilter.none, ExploreSort.POPULAR)

        assertEquals(
            attributes {
                put("filters", "none")
                put("sort", "POPULAR")
            },
            event.attrs,
        )
    }

    @Test
    fun `필터 조합은 한 문자열로 접어 분포를 본다`() {
        val filter =
            ExploreFilter(
                categories = setOf(Category.EXERCISE),
                verifyType = VerificationType.AUTO,
                eligibleOnly = true,
            )

        val event = ChallengeEvents.exploreEmptyResult(filter, ExploreSort.DEADLINE)

        assertEquals(
            attributes {
                put("filters", "categories=EXERCISE,verify=AUTO,eligible_only")
                put("sort", "DEADLINE")
            },
            event.attrs,
        )
    }

    @Test
    fun `자동에서 수동으로 바꾼 경우에만 auto_to_manual 을 채운다`() {
        assertEquals(
            attributes { put("field", "verification") },
            ChallengeEvents.draftEdit(DraftField.VERIFICATION).attrs,
        )
        assertEquals(
            attributes {
                put("field", "verification")
                put("auto_to_manual", true)
            },
            ChallengeEvents.draftEdit(DraftField.VERIFICATION, autoToManual = true).attrs,
        )
    }
}
