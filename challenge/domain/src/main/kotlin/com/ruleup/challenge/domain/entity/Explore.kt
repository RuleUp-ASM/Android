package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier

/**
 * 둘러보기 정렬 (명세 6종, 단일 적용). 방향은 정의로 고정돼 사용자에게 노출하지 않는다.
 *
 * [COMPLETION_RATE]·[SUCCESS_FAIL_RATIO] 는 표본 미달 방을 **목록에서 아예 제외**하므로, 이 정렬로
 * 결과가 0건이면 "조건에 맞는 방이 없다"가 아니라 "아직 기록이 충분한 방이 없다"로 안내해야 한다.
 */
enum class ExploreSort(
    val value: String,
) {
    // 24시간 신규 참여 수 — 홈 인기와 같은 기준
    POPULAR("POPULAR"),

    // 이 방의 현재 참여자 수
    PARTICIPANTS("PARTICIPANTS"),

    // 성공률 80% 이상인 사람의 비율
    COMPLETION_RATE("COMPLETION_RATE"),

    // 확정 실패 없이 버티는 사람의 비율
    SUCCESS_FAIL_RATIO("SUCCESS_FAIL_RATIO"),

    // 방 생성 시각 — 신규 노출 담당
    RECENT("RECENT"),

    // 종료 임박 순
    DEADLINE("DEADLINE"),
    ;

    /** 표본 미달 방이 목록에서 빠지는 정렬인지 — 빈 결과 문구를 가르는 기준. */
    val excludesLowSample: Boolean
        get() = this == COMPLETION_RATE || this == SUCCESS_FAIL_RATIO

    companion object {
        val default = POPULAR

        fun fromValue(value: String?): ExploreSort? = entries.find { it.value == value }
    }
}

/**
 * 둘러보기 필터 (명세 3종, 서로 AND 결합).
 *
 * [categories] 는 **복수 선택이고 내부는 OR** 다(하나라도 해당하면 노출). [eligibleOnly] 는 **기본 off** —
 * 켜면 초기 풀이 작아 빈 결과가 급증하기 때문이다(정책 가드레일). 홈 인기 섹션에는 필터를 적용하지 않는다.
 */
data class ExploreFilter(
    val categories: Set<Category> = emptySet(),
    val verifyType: VerificationType? = null,
    val eligibleOnly: Boolean = false,
) {
    /** 필터 칩 배지 수. 카테고리는 몇 개를 골랐든 한 덩어리로 센다. */
    val activeCount: Int
        get() = listOfNotNull(categories.takeIf { it.isNotEmpty() }, verifyType, eligibleOnly.takeIf { it }).size

    /** 서버 전송용 csv. 비어 있으면 파라미터 자체를 보내지 않는다(= 전체). */
    fun categoriesParam(): String? = categories.takeIf { it.isNotEmpty() }?.joinToString(",") { it.value }

    companion object {
        val none = ExploreFilter()
    }
}

/**
 * 홈 실시간 인기 항목 (명세 `GET /challenges/trending` items[]).
 *
 * **필터를 타지 않는다** — 내 티어로 못 들어가는 방도 노출하되 [joinable] 로 잠금만 표시한다(의도된 동작).
 * [participantCount] 는 순위와 같은 스냅샷 기준이라 **최대 1시간 지연**된 값이다("현재" 값이 아니다).
 */
data class TrendingChallenge(
    val rank: Int,
    val challengeId: String,
    val title: String,
    // 심사 통과 전이면 null — 카테고리 기본 이미지로 대체한다
    val imageUrl: String?,
    val category: Category?,
    val participantCount: Int,
    // 정렬 기준값
    val recentJoins24h: Int,
    val verificationType: VerificationType,
    val minTier: Tier?,
    // 내 표시 티어로 들어갈 수 있는지 — 잠금 아이콘용이지 필터가 아니다
    val joinable: Boolean,
    // ISO date, D-day 계산용
    val endDate: String?,
)

/** 인기 스냅샷 (명세 trending 응답). [calculatedAt] 은 순위 계산 기준 시각으로 최대 1시간 지연된다. */
data class TrendingSnapshot(
    val calculatedAt: String?,
    val items: List<TrendingChallenge>,
)

/** 카테고리 그리드 항목 (명세 `GET /challenge-categories` items[]). */
data class ChallengeCategoryCount(
    // 서버가 내려주는 표시명(예: "운동")
    val name: String,
    // 진행 중 **공개 그룹** 방 수 — 비공개·솔로·종료 방은 세지 않는다
    val activeGroupCount: Int,
    // code 로 매칭한 앱 카테고리(아이콘·필터 연결용). 매칭 실패 시 null
    val category: Category?,
)

/**
 * 둘러보기 카드 항목 (명세 `GET /challenges/explore` items[]).
 *
 * [completionRate]·[retentionRate] 의 **null 은 "표본 미달"이지 0이 아니다** — 화면은 값을 0%로 그리지 말고
 * 해당 영역을 통째로 숨겨야 한다. 정원이 찬 방([isFull])도 목록에서 빼지 않고 뱃지로만 구분한다 —
 * 탈퇴로 자리가 나거나 정원이 늘 수 있기 때문이다.
 */
data class ExploreChallenge(
    val challengeId: String,
    val title: String,
    val imageUrl: String?,
    val category: Category?,
    val verificationType: VerificationType,
    // 시작 전 — true 면 진행 지표가 전부 null 이다
    val startsSoon: Boolean,
    val participantCount: Int,
    val capacity: Int,
    val isFull: Boolean,
    val minTier: Tier?,
    // 내 표시 티어 기준 입장 가능 여부
    val eligible: Boolean,
    // 0~1, 표본 미달이면 null
    val completionRate: Double?,
    // 0~1, 표본 미달이면 null
    val retentionRate: Double?,
    // 종료까지 남은 일수
    val dday: Int?,
    val startDate: String?,
    val endDate: String?,
    val createdAt: String?,
) {
    /** 지표 영역을 그릴 수 있는지. 시작 전이거나 표본이 없으면 숨긴다. */
    val hasMetrics: Boolean
        get() = !startsSoon && (completionRate != null || retentionRate != null)
}

/**
 * 둘러보기 커서 페이지 (명세 explore 응답).
 *
 * **전체 개수는 제공하지 않는다** — 무한 스크롤이라 불필요하고, 매 요청 COUNT 쿼리가 p95 1초 목표에
 * 부담이 되기 때문이다(계약에서 제거).
 */
data class ExploreResult(
    val items: List<ExploreChallenge>,
    val nextCursor: String?,
    val hasNext: Boolean,
)

/** 정렬 값이 서버 정의 밖이다 (명세 400 `INVALID_SORT_TYPE`). 화면은 기본 정렬로 되돌린 뒤 재조회한다. */
class InvalidSortTypeException : Exception("정렬 조건을 다시 선택해 주세요.")

/** 필터 값이 서버 정의 밖이다 (명세 400 `INVALID_FILTER_VALUE`). 화면은 해당 필터를 초기화한다. */
class InvalidFilterValueException : Exception("필터 조건을 다시 선택해 주세요.")

/**
 * 커서가 손상·만료됐다 (명세 400 `CURSOR_INVALID`).
 * 화면은 **조용히 첫 페이지부터 재요청**한다 — 사용자가 인지할 필요가 없다.
 */
class CursorInvalidException : Exception("목록을 다시 불러옵니다.")
