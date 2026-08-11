package com.ruleup.challenge.domain.observability

import com.ruleup.challenge.domain.entity.ExploreFilter
import com.ruleup.challenge.domain.entity.ExploreSort
import com.ruleup.observability.domain.event.BusinessPayload
import com.ruleup.observability.domain.model.attributes

/**
 * 챌린지 탐색·생성 퍼널 이벤트. **기능 스펙 9번의 릴리즈 게이트**라, 하나라도 빠지면 탐색→참여
 * 전환율을 계산할 수 없어 릴리즈가 보류된다.
 *
 * 팩토리 시그니처가 곧 스키마다 — 파라미터 타입이 값 타입을 고정하므로 별도 스키마 선언을 두지
 * 않고, 출력을 그대로 박아 두는 단위 테스트로 검증한다([ChallengeEventsTest]).
 *
 * **`challenge_id` 가 노출 → 클릭 → 상세 → 참여까지 같은 값으로 이어져야** 전환율이 계산된다.
 * 그래서 카드에서 상세로 넘어갈 때 [ChallengeCardSource] 와 position 을 함께 싣는다.
 *
 * 서버 담당 이벤트(`draft_request`·`draft_result`·`challenge_create_result`·`moderation_result` 등)는
 * 여기 없다 — 생성 기능 스펙 9번이 BE 로 배정했다.
 */
object ChallengeEvents {
    // ---------- 탐색 ----------

    /** 탐색 홈 진입. 전환율의 분모다. */
    fun exploreHomeView(hasTrending: Boolean) =
        BusinessPayload.Custom(
            "explore_home_view",
            attributes { put("has_trending", hasTrending) },
        )

    /**
     * 인기 섹션 노출. 카드 단위가 아니라 **섹션 단위로 한 번** 보낸다 — 인기는 한 화면에 상위 N개가
     * 함께 들어와서 카드별로 쪼갤 이유가 없다.
     */
    fun trendingImpression(challengeIds: List<String>) =
        BusinessPayload.Custom(
            "trending_impression",
            attributes {
                put("challenge_ids", challengeIds.joinToString(","))
                put("rank_range", if (challengeIds.isEmpty()) "" else "1-${challengeIds.size}")
            },
        )

    /** 카테고리 타일 클릭. 카테고리 편중도를 낸다. */
    fun categoryGridClick(
        category: String,
        challengeCount: Int,
    ) = BusinessPayload.Custom(
        "category_grid_click",
        attributes {
            put("category", category)
            put("challenge_count", challengeCount.toLong())
        },
    )

    /** 목록 화면 진입. */
    fun exploreListView(
        entry: ExploreListEntry,
        sort: ExploreSort,
        filter: ExploreFilter,
    ) = BusinessPayload.Custom(
        "explore_list_view",
        attributes {
            put("entry", entry.value)
            put("sort", sort.value)
            put("filters", filter.describe())
        },
    )

    /** 필터 적용. 필터 사용률과 빈 결과율을 낸다. */
    fun exploreFilterApply(
        filter: ExploreFilter,
        resultCount: Int,
    ) = BusinessPayload.Custom(
        "explore_filter_apply",
        attributes {
            put("categories", filter.categories.joinToString(",") { it.value })
            put("verify_type", filter.verifyType?.value.orEmpty())
            put("eligible_only", filter.eligibleOnly)
            put("result_count", resultCount.toLong())
        },
    )

    /** 정렬 변경. 어디서 어디로 옮겼는지가 정렬 선호를 만든다. */
    fun exploreSortChange(
        from: ExploreSort,
        to: ExploreSort,
        resultCount: Int,
    ) = BusinessPayload.Custom(
        "explore_sort_change",
        attributes {
            put("sort_from", from.value)
            put("sort_to", to.value)
            put("result_count", resultCount.toLong())
        },
    )

    /**
     * 결과 0건 노출.
     *
     * [exploreFilterApply] 와 **중복 전송되는 게 정상**이다 — 필터 사용률과 빈 결과율은 분모가 달라서
     * 하나로 합치면 가드레일(빈 결과율 10% 이하)을 계산할 수 없다.
     */
    fun exploreEmptyResult(
        filter: ExploreFilter,
        sort: ExploreSort,
    ) = BusinessPayload.Custom(
        "explore_empty_result",
        attributes {
            put("filters", filter.describe())
            put("sort", sort.value)
        },
    )

    /**
     * 목록 카드 노출. **뷰포트 50% 이상 · 1초 이상**일 때만 보내고 세션 내 중복은 막는다.
     *
     * [hasMetrics] 는 완주율이 null 인 카드가 클릭률에 주는 영향을 보기 위한 값이다 — 표본 부족
     * 처리가 탐색 품질을 얼마나 깎는지 이 값 없이는 알 수 없다.
     */
    fun challengeCardImpression(
        challengeId: String,
        position: Int,
        sort: ExploreSort,
        isFull: Boolean,
        eligible: Boolean,
        hasMetrics: Boolean,
    ) = BusinessPayload.Custom(
        "challenge_card_impression",
        attributes {
            put("challenge_id", challengeId)
            put("position", position.toLong())
            put("sort", sort.value)
            put("is_full", isFull)
            put("eligible", eligible)
            put("has_metrics", hasMetrics)
        },
    )

    /** 카드 클릭. 상세 진입률의 분자다. */
    fun challengeCardClick(
        challengeId: String,
        position: Int,
        source: ChallengeCardSource,
        sort: ExploreSort?,
    ) = BusinessPayload.Custom(
        "challenge_card_click",
        attributes {
            put("challenge_id", challengeId)
            put("position", position.toLong())
            put("source", source.value)
            // 인기 섹션은 정렬 개념이 없다 — 키를 비워두는 대신 아예 넣지 않는다.
            sort?.let { put("sort", it.value) }
        },
    )

    /** 공개 상세 진입. 상세→참여 전환의 분모다. */
    fun challengeDetailView(
        challengeId: String,
        source: ChallengeCardSource?,
        eligible: Boolean,
        isFull: Boolean,
    ) = BusinessPayload.Custom(
        "challenge_detail_view",
        attributes {
            put("challenge_id", challengeId)
            source?.let { put("source", it.value) }
            put("eligible", eligible)
            put("is_full", isFull)
        },
    )

    /** 참여 버튼 클릭. 게이트에 막히기 전 시점이라 시도 수의 분모가 된다. */
    fun challengeJoinAttempt(
        challengeId: String,
        eligible: Boolean,
        isFull: Boolean,
    ) = BusinessPayload.Custom(
        "challenge_join_attempt",
        attributes {
            put("challenge_id", challengeId)
            put("eligible", eligible)
            put("is_full", isFull)
        },
    )

    /**
     * 참여 성공/실패. **탐색→참여 전환율의 분자**다.
     *
     * 생성 기능 스펙은 이 이벤트를 BE 담당으로 뒀지만, 탐색 스펙이 요구하는
     * "노출→클릭→참여를 잇는 `challenge_id` 일관 전달"은 클라이언트에서만 만들 수 있어 여기서도 남긴다.
     * 서버 로그와 중복되더라도 퍼널 계산의 근거가 갈라지는 것보다 낫다.
     *
     * @param errorCode 차단 사유(`JoinBlockReason`) 또는 에러 코드. 성공이면 null 이라 키를 넣지 않는다.
     */
    fun challengeJoinResult(
        challengeId: String,
        success: Boolean,
        errorCode: String? = null,
    ) = BusinessPayload.Custom(
        "challenge_join_result",
        attributes {
            put("challenge_id", challengeId)
            put("success", success)
            errorCode?.let { put("error_code", it) }
        },
    )

    /** 템플릿 복제 실행. 참여 대신 "직접 만들기"로 가는 수요 크기를 낸다. */
    fun challengeCloneClick(challengeId: String) =
        BusinessPayload.Custom(
            "challenge_clone_click",
            attributes { put("challenge_id", challengeId) },
        )

    /** 다음 페이지 로드. 스크롤 깊이를 낸다. */
    fun exploreListLoadMore(
        pageIndex: Int,
        sort: ExploreSort,
    ) = BusinessPayload.Custom(
        "explore_list_load_more",
        attributes {
            put("page_index", pageIndex.toLong())
            put("sort", sort.value)
        },
    )

    // ---------- 생성 ----------

    /**
     * 생성 화면 진입. 생성 전환율의 분모다.
     *
     * 진입점이 홈·목록 빈 상태·탐색 빈 결과 세 곳이라 [entry] 로 나눠야 경로별 전환율이 갈린다.
     * **프로그래매틱 화면 진입에서 중복 전송되지 않도록** 호출부가 1회만 보내야 한다.
     */
    fun createStart(entry: CreateEntry) =
        BusinessPayload.Custom(
            "create_start",
            attributes { put("entry", entry.value) },
        )

    /** 경로 선택. 추천 칩과 설명 입력의 비중을 낸다. */
    fun createPathSelect(path: CreatePath) =
        BusinessPayload.Custom(
            "create_path_select",
            attributes { put("path", path.value) },
        )

    /**
     * 확인 화면 항목 수정. **필드별 1회로 집계**한다 — 타이핑마다 보내면 수정률이 타이핑 양에 좌우된다.
     *
     * @param autoToManual 인증 방식을 자동에서 수동으로 바꾼 경우에만 채운다.
     */
    fun draftEdit(
        field: DraftField,
        autoToManual: Boolean? = null,
    ) = BusinessPayload.Custom(
        "draft_edit",
        attributes {
            put("field", field.value)
            autoToManual?.let { put("auto_to_manual", it) }
        },
    )
}

/** 카드를 어디서 눌렀는지. 인기 섹션과 목록의 전환력을 나눠 본다. */
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
    /** 추천 칩 — LLM 미경유 */
    TEMPLATE("TEMPLATE"),

    /** 설명 입력 — LLM 5-Step */
    PROMPT("PROMPT"),
}

/** 확인 화면에서 수정된 항목. 이름은 생성 요청 필드명을 그대로 쓴다. */
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

/**
 * 필터를 한 문자열로 접는다. `filters` 파라미터는 조합 분포를 보려는 것이라 키별로 쪼개지 않는다.
 * 아무것도 안 걸렸으면 "none" — 빈 문자열을 넣으면 집계에서 결측과 구분되지 않는다.
 */
private fun ExploreFilter.describe(): String {
    val parts =
        buildList {
            categories.takeIf { it.isNotEmpty() }?.let { add("categories=${it.joinToString("|") { c -> c.value }}") }
            verifyType?.let { add("verify=${it.value}") }
            if (eligibleOnly) add("eligible_only")
        }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(",") ?: "none"
}
