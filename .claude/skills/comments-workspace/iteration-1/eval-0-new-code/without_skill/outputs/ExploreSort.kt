package com.ruleup.challenge.domain.entity

/**
 * 둘러보기 목록 정렬 (서버 6종).
 *
 * [COMPLETION_RATE]·[SUCCESS_FAIL_RATIO] 는 판정 표본이 [MIN_SAMPLE] 건 미만인 방을 **목록에서 아예
 * 제외**한다. 그래서 이 두 정렬에서 0건이 나온 것은 필터가 좁아서가 아니라 지표를 낼 만큼 기록이
 * 쌓인 방이 없다는 뜻이고, 화면은 [excludesLowSample] 로 두 상황의 빈 화면 문구를 갈라야 한다.
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

        /** 지표 정렬이 방을 목록에 남기는 최소 판정 건수. 서버 필터 기준이라 클라이언트가 바꿀 수 없다. */
        const val MIN_SAMPLE = 10

        /**
         * 서버 값을 정렬로 옮긴다.
         *
         * 모르는 값은 [default] 로 떨어뜨린다 — 서버가 정렬을 늘렸을 때 목록이 비거나 400 으로 깨지는
         * 대신 인기순으로 보이는 쪽이 낫다.
         */
        fun fromValue(value: String?): ExploreSort = entries.find { it.value == value } ?: default
    }
}
