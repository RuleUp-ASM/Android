package com.ruleup.challenge.domain.logging

import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.logging.domain.BizEvent
import com.ruleup.logging.domain.bizAttributes

/** 챌린지 탐색·생성 퍼널 이벤트. */
object ChallengeEvents {
    // 탐색

    /** 탐색 홈 진입. */
    fun exploreHomeView(hasTrending: Boolean) =
        BizEvent(
            "explore_home_view",
            bizAttributes { put("has_trending", hasTrending) },
        )

    /** 인기 섹션 노출. */
    fun trendingImpression(challengeIds: List<String>) =
        BizEvent(
            "trending_impression",
            bizAttributes {
                put("challenge_ids", challengeIds.joinToString(","))
                put("rank_range", if (challengeIds.isEmpty()) "" else "1-${challengeIds.size}")
            },
        )

    /** 카테고리 타일 클릭. */
    fun categoryGridClick(
        category: String,
        challengeCount: Int,
    ) = BizEvent(
        "category_grid_click",
        bizAttributes {
            put("category", category)
            put("challenge_count", challengeCount.toLong())
        },
    )

    fun exploreListView(
        entry: ExploreListEntry,
        sort: ExploreSort,
        filter: ExploreFilter,
    ) = BizEvent(
        "explore_list_view",
        bizAttributes {
            put("entry", entry.value)
            put("sort", sort.value)
            put("filters", filter.describe())
        },
    )

    /** 필터 적용. */
    fun exploreFilterApply(
        filter: ExploreFilter,
        resultCount: Int,
    ) = BizEvent(
        "explore_filter_apply",
        bizAttributes {
            put("categories", filter.categories.joinToString(",") { it.value })
            put("verify_type", filter.verifyType?.value.orEmpty())
            put("eligible_only", filter.eligibleOnly)
            put("result_count", resultCount.toLong())
        },
    )

    /** 정렬 변경. */
    fun exploreSortChange(
        from: ExploreSort,
        to: ExploreSort,
        resultCount: Int,
    ) = BizEvent(
        "explore_sort_change",
        bizAttributes {
            put("sort_from", from.value)
            put("sort_to", to.value)
            put("result_count", resultCount.toLong())
        },
    )

    /** 결과 0건 노출. */
    fun exploreEmptyResult(
        filter: ExploreFilter,
        sort: ExploreSort,
    ) = BizEvent(
        "explore_empty_result",
        bizAttributes {
            put("filters", filter.describe())
            put("sort", sort.value)
        },
    )

    /** 목록 카드 노출. */
    fun challengeCardImpression(
        challengeId: String,
        position: Int,
        sort: ExploreSort,
        isFull: Boolean,
        eligible: Boolean,
        hasMetrics: Boolean,
    ) = BizEvent(
        "challenge_card_impression",
        bizAttributes {
            put("challenge_id", challengeId)
            put("position", position.toLong())
            put("sort", sort.value)
            put("is_full", isFull)
            put("eligible", eligible)
            put("has_metrics", hasMetrics)
        },
    )

    /** 카드 클릭. */
    fun challengeCardClick(
        challengeId: String,
        position: Int,
        source: ChallengeCardSource,
        sort: ExploreSort?,
    ) = BizEvent(
        "challenge_card_click",
        bizAttributes {
            put("challenge_id", challengeId)
            put("position", position.toLong())
            put("source", source.value)
            // 인기 섹션은 정렬 개념이 없다
            sort?.let { put("sort", it.value) }
        },
    )

    /** 공개 상세 진입. */
    fun challengeDetailView(
        challengeId: String,
        source: ChallengeCardSource?,
        eligible: Boolean,
        isFull: Boolean,
    ) = BizEvent(
        "challenge_detail_view",
        bizAttributes {
            put("challenge_id", challengeId)
            source?.let { put("source", it.value) }
            put("eligible", eligible)
            put("is_full", isFull)
        },
    )

    // 방 내부

    /** 방 내부 진입. */
    fun roomView(
        challengeId: String,
        myRole: String,
        ownerType: String,
    ) = BizEvent(
        "room_view",
        bizAttributes {
            put("challenge_id", challengeId)
            put("my_role", myRole)
            put("owner_type", ownerType)
        },
    )

    /** 피드 다음 페이지 로드. */
    fun threadScroll(
        pageIndex: Int,
        itemCount: Int,
    ) = BizEvent(
        "thread_scroll",
        bizAttributes {
            put("page_index", pageIndex.toLong())
            put("item_count", itemCount.toLong())
        },
    )

    /** 랭킹 조회. */
    fun rankingView(
        scope: RankingViewScope,
        myRankNull: Boolean,
    ) = BizEvent(
        "ranking_view",
        bizAttributes {
            put("scope", scope.value)
            put("my_rank_null", myRankNull)
        },
    )

    /** 피드 빈 상태 노출. */
    fun roomEmptyStateView(ownerType: String) =
        BizEvent(
            "room_empty_state_view",
            bizAttributes { put("owner_type", ownerType) },
        )

    /** 참여 버튼 클릭. */
    fun challengeJoinAttempt(
        challengeId: String,
        eligible: Boolean,
        isFull: Boolean,
    ) = BizEvent(
        "challenge_join_attempt",
        bizAttributes {
            put("challenge_id", challengeId)
            put("eligible", eligible)
            put("is_full", isFull)
        },
    )

    /** 참여 성공/실패. */
    fun challengeJoinResult(
        challengeId: String,
        success: Boolean,
        errorCode: String? = null,
    ) = BizEvent(
        "challenge_join_result",
        bizAttributes {
            put("challenge_id", challengeId)
            put("success", success)
            errorCode?.let { put("error_code", it) }
        },
    )

    /** 템플릿 복제 실행. */
    fun challengeCloneClick(challengeId: String) =
        BizEvent(
            "challenge_clone_click",
            bizAttributes { put("challenge_id", challengeId) },
        )

    /** 다음 페이지 로드. */
    fun exploreListLoadMore(
        pageIndex: Int,
        sort: ExploreSort,
    ) = BizEvent(
        "explore_list_load_more",
        bizAttributes {
            put("page_index", pageIndex.toLong())
            put("sort", sort.value)
        },
    )

    // 생성

    /** 생성 화면 진입. */
    fun createStart(entry: CreateEntry) =
        BizEvent(
            "create_start",
            bizAttributes { put("entry", entry.value) },
        )

    /** 경로 선택. */
    fun createPathSelect(path: CreatePath) =
        BizEvent(
            "create_path_select",
            bizAttributes { put("path", path.value) },
        )

    /** 확인 화면 항목 수정. */
    fun draftEdit(
        field: DraftField,
        autoToManual: Boolean? = null,
    ) = BizEvent(
        "draft_edit",
        bizAttributes {
            put("field", field.value)
            autoToManual?.let { put("auto_to_manual", it) }
        },
    )
}

/** 카드를 어디서 눌렀는지. */
enum class ChallengeCardSource(
    val value: String,
) {
    TRENDING("trending"),
    LIST("list"),
}

/** 목록 화면에 어떻게 들어왔는지. */
enum class ExploreListEntry(
    val value: String,
) {
    /** "전체 ›" */
    ALL("all"),

    /** 카테고리 타일 */
    CATEGORY("category"),
}

/** 랭킹 조회 범위. */
enum class RankingViewScope(
    val value: String,
) {
    IN_ROOM("IN_ROOM"),
    CROSS("CROSS"),
}

/** 생성 화면 진입 경로. */
enum class CreateEntry(
    val value: String,
) {
    HOME("home"),
    CHALLENGE_LIST_EMPTY("challenge_list_empty"),
    EXPLORE_EMPTY("explore_empty"),
    UNKNOWN("unknown"),
}

/** 초안을 만든 경로. */
enum class CreatePath(
    val value: String,
) {
    /** 추천 칩 */
    TEMPLATE("TEMPLATE"),

    /** 설명 입력 */
    PROMPT("PROMPT"),
}

/** 확인 화면에서 수정된 항목. */
enum class DraftField(
    val value: String,
) {
    TITLE("title"),
    DESCRIPTION("description"),
    IMAGE("imageUrl"),
    MODE("mode"),
    VISIBILITY("visibility"),
    RANKING_VISIBLE("rankingVisible"),
    CAPACITY("capacity"),
    MIN_TIER("minTier"),
    PERIOD("period"),
    WEEKLY_COUNT("weeklyCount"),
    PARAMS("params"),
    VERIFICATION("verification"),
    PENALTIES("penalties"),
}

/** 필터를 한 문자열로 접는다. */
private fun ExploreFilter.describe(): String {
    val parts =
        buildList {
            categories.takeIf { it.isNotEmpty() }?.let { add("categories=${it.joinToString("|") { c -> c.value }}") }
            verifyType?.let { add("verify=${it.value}") }
            if (eligibleOnly) add("eligible_only")
        }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(",") ?: "none"
}
