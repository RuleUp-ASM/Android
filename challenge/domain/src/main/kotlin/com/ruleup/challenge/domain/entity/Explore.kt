package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.category.Category

/**
 * 탐색 목록 정렬 기준(탐색 스펙 §3.2, 7종). 방향은 정의로 고정되어 사용자에게 노출하지 않는다.
 * 완주율은 템플릿 단위, 성공/실패 비율은 방 단위로 스코프가 다르다(정렬 순서 독립).
 */
enum class ExploreSort(
    val value: String,
) {
    // 이 템플릿에서 파생된 모든 챌린지의 현재 참여자 수 합 (내림차순)
    TEMPLATE_USAGE("TEMPLATE_USAGE"),

    // 이 챌린지 방의 현재 참여자 수 (내림차순)
    PARTICIPANTS("PARTICIPANTS"),

    // 템플릿 역대 완주자 비율 (내림차순, 누적 완료 참여자 10명 초과일 때만 값)
    COMPLETION_RATE("COMPLETION_RATE"),

    // 이 방의 성공 인원 비율 (내림차순, 참여자 10명 초과 AND 진행률 30% 이상일 때만 값)
    SUCCESS_FAIL_RATIO("SUCCESS_FAIL_RATIO"),

    // 챌린지 생성 시각 (내림차순) — 신규 챌린지 노출 담당
    RECENT("RECENT"),

    // 종료 시각 오름차순 — 마감 임박이 상단
    DEADLINE("DEADLINE"),

    // 최근 24시간 참여 속도 스코어 (내림차순, 10분 주기 배치 갱신)
    TRENDING("TRENDING"),
    ;

    companion object {
        fun fromValue(value: String?): ExploreSort? = entries.find { it.value == value }
    }
}

/**
 * 탐색 목록 필터(탐색 스펙 §3.1 · API 명세). 모든 조건은 AND 로 결합하며, null 은 미적용이다.
 * 홈 인기 섹션에는 적용하지 않는다.
 */
data class ExploreFilter(
    val category: Category? = null,
    val participationType: ParticipationType? = null,
    val verificationMethod: SelectedMethod? = null,
    // 매너 온도 컷: 클라이언트가 온도 값을 보내지 않고, true 면 서버가 토큰 사용자 기준으로
    // minMannerTemperature ≤ 내 매너 온도 인 챌린지만 반환한다(API 기본 true).
    val joinableOnly: Boolean = true,
) {
    // 필터 칩 배지 수. joinableOnly 는 기본 on 인 설정이라 세지 않는다.
    val activeCount: Int
        get() = listOfNotNull(category, participationType, verificationMethod).size

    companion object {
        val none = ExploreFilter()
    }
}

/** 홈 실시간 인기 항목(명세: GET /challenges/trending data.items[]). 서버가 Top 20 을 내려주고 홈은 상위 일부만 쓴다. */
data class TrendingChallenge(
    // 서버 산정 순위(1부터)
    val rank: Int,
    val challengeId: String,
    val title: String,
    // 현재 참여자 수 (카드 우측 "N명")
    val participantCount: Int,
)

/** 카테고리 그리드 항목(명세: GET /challenge-categories items[]). */
data class ChallengeCategoryCount(
    // 서버가 내려주는 표시명(예: "운동")
    val name: String,
    val activeChallengeCount: Int,
    // code 로 매칭한 앱 카테고리(아이콘·목록 필터 연결용). 매칭 실패 시 null
    val category: Category?,
)

/** 둘러보기 목록 카드 항목(명세: GET /challenges/explore challenges[]). */
data class ExploreChallenge(
    val challengeId: String,
    val title: String,
    val category: Category?,
    val participationType: ParticipationType,
    val verificationMethod: SelectedMethod,
    // 이 방의 현재 참여자 수
    val participantCount: Int,
    // 템플릿 완주율(0..1). 누적 완료 참여자 10명 이하면 null(표본 부족)
    val completionRate: Double?,
    // 이 방의 성공률(0..1). 참여자 10명 이하 또는 진행률 30% 미만이면 null(표본 부족)
    val successRate: Double?,
    // 템플릿 사용자 수(파생 챌린지 참여자 합)
    val templateUsageCount: Int,
    // ISO date. null 이면 상시(종료 기한 없음) 배지
    val endDate: String?,
)

/** 둘러보기 커서 페이지(탐색 스펙 §3.3). [nextCursor] 가 null 이면 마지막 페이지. */
data class ExploreResult(
    val totalCount: Int,
    val challenges: List<ExploreChallenge>,
    val nextCursor: String?,
)
