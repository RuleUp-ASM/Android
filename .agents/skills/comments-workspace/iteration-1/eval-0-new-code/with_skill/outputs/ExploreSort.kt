package com.ruleup.challenge.domain.entity

/**
 * 둘러보기 목록 정렬 (명세 6종, 단일 적용). 기본은 [POPULAR].
 *
 * [COMPLETION_RATE]·[SUCCESS_FAIL_RATIO] 는 판정 표본 10건 미만인 방을 **목록에서 아예 뺀다** — 그래서
 * 이 정렬의 0건은 "조건에 맞는 방이 없어요"가 아니라 "아직 데이터가 모인 방이 없어요"다.
 */
enum class ExploreSort(
    val value: String,
) {
    POPULAR("POPULAR"),
    PARTICIPANTS("PARTICIPANTS"),
    COMPLETION_RATE("COMPLETION_RATE"),
    SUCCESS_FAIL_RATIO("SUCCESS_FAIL_RATIO"),
    LATEST("LATEST"),
    DEADLINE("DEADLINE"),
    ;

    /** 표본 미달 방이 목록에서 빠지는 정렬인지 — 빈 결과 문구를 가르는 기준. */
    val excludesLowSample: Boolean
        get() = this == COMPLETION_RATE || this == SUCCESS_FAIL_RATIO

    companion object {
        val default = POPULAR

        /** 미지 값은 [default] 로 떨어뜨린다 — 서버가 정렬을 늘렸을 때 구버전 앱이 목록을 통째로 못 열면 안 된다. */
        fun fromValue(value: String?): ExploreSort = entries.find { it.value == value } ?: default
    }
}
