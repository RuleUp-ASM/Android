package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier

/** 둘러보기 정렬. */
enum class ExploreSort(
    val value: String,
) {
    // 24시간 신규 참여 수
    POPULAR("POPULAR"),

    // 이 방의 현재 참여자 수
    PARTICIPANTS("PARTICIPANTS"),

    // 성공률 80% 이상인 사람의 비율
    COMPLETION_RATE("COMPLETION_RATE"),

    // 확정 실패 없이 버티는 사람의 비율
    SUCCESS_FAIL_RATIO("SUCCESS_FAIL_RATIO"),

    // 방 생성 시각
    RECENT("RECENT"),

    // 종료 임박 순
    DEADLINE("DEADLINE"),
    ;

    /** 표본 미달 방이 목록에서 빠지는 정렬인지 */
    val excludesLowSample: Boolean
        get() = this == COMPLETION_RATE || this == SUCCESS_FAIL_RATIO

    companion object {
        val default = POPULAR

        fun fromValue(value: String?): ExploreSort? = entries.find { it.value == value }
    }
}

/** 둘러보기 필터. */
data class ExploreFilter(
    val categories: Set<Category> = emptySet(),
    val verifyType: VerificationType? = null,
    val eligibleOnly: Boolean = false,
) {
    /** 필터 칩 배지 수. */
    val activeCount: Int
        get() = listOfNotNull(categories.takeIf { it.isNotEmpty() }, verifyType, eligibleOnly.takeIf { it }).size

    /** 서버 전송용 csv. */
    fun categoriesParam(): String? = categories.takeIf { it.isNotEmpty() }?.joinToString(",") { it.value }

    companion object {
        val none = ExploreFilter()
    }
}

/** 홈 실시간 인기 항목. */
data class TrendingChallenge(
    val rank: Int,
    val challengeId: String,
    val title: String,
    // 심사 통과 전이면 null
    val imageUrl: String?,
    val category: Category?,
    val participantCount: Int,
    // 정렬 기준값
    val recentJoins24h: Int,
    val verificationType: VerificationType,
    val minTier: Tier?,
    // 내 표시 티어로 들어갈 수 있는지
    val joinable: Boolean,
    // ISO date, D-day 계산용
    val endDate: String?,
)

/** 인기 스냅샷. */
data class TrendingSnapshot(
    val calculatedAt: String?,
    val items: List<TrendingChallenge>,
)

/** 카테고리 그리드 항목. */
data class ChallengeCategoryCount(
    // 서버가 내려주는 표시명(예: "운동")
    val name: String,
    // 진행 중 공개 그룹 방 수
    val activeGroupCount: Int,
    // code 로 매칭한 앱 카테고리(아이콘·필터 연결용).
    val category: Category?,
)

/** 둘러보기 카드 항목. */
data class ExploreChallenge(
    val challengeId: String,
    val title: String,
    val imageUrl: String?,
    val category: Category?,
    val verificationType: VerificationType,
    // 시작 전
    val startsSoon: Boolean,
    val participantCount: Int,
    // null 이면 무제한
    val capacity: Int?,
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
    /** 지표 영역을 그릴 수 있는지. */
    val hasMetrics: Boolean
        get() = !startsSoon && (completionRate != null || retentionRate != null)
}

/** 둘러보기 커서 페이지. */
data class ExploreResult(
    val items: List<ExploreChallenge>,
    val nextCursor: String?,
    val hasNext: Boolean,
)

/** 정렬 값이 서버 정의 밖이다. */
class InvalidSortTypeException : Exception("정렬 조건을 다시 선택해 주세요.")

/** 필터 값이 서버 정의 밖이다. */
class InvalidFilterValueException : Exception("필터 조건을 다시 선택해 주세요.")

/** 커서가 손상·만료됐다. */
class CursorInvalidException : Exception("목록을 다시 불러옵니다.")
