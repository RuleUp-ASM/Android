// 제안 파일. 이 enum 은 이미 레포에 있다 —
// challenge/domain/src/main/kotlin/com/ruleup/challenge/domain/entity/Explore.kt 의 맨 위 블록이다.
// 아래는 그 블록을 그대로 대체하는 형태이고, 기존 대비 실제 변경은 fromValue 폴백 하나뿐이다.
// (RECENT vs LATEST 는 확인 전이라 손대지 않았다. RESPONSE.md 참고.)

package com.ruleup.challenge.domain.entity

/**
 * 둘러보기 정렬 (명세 6종, 단일 적용). 방향은 정의로 고정돼 사용자에게 노출하지 않는다.
 *
 * [COMPLETION_RATE]·[SUCCESS_FAIL_RATIO] 는 판정 표본이 10건 미만인 방을 **목록에서 아예 제외**하므로,
 * 이 정렬로 결과가 0건이면 "조건에 맞는 방이 없다"가 아니라 "아직 기록이 충분한 방이 없다"로 안내해야 한다.
 * 표본 컷은 서버가 적용한다 — 클라이언트는 [excludesLowSample] 로 문구만 가른다.
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

        /**
         * 딥링크·복원된 nav 인자용. 값이 없거나 서버 정의 밖이면 [default] 로 떨어뜨린다 —
         * 정렬 하나 때문에 화면이 못 뜨는 편보다 인기순으로 여는 편이 낫다.
         * (`ParamKind.fromValue` 와 같은 규약.)
         */
        fun fromValue(value: String?): ExploreSort = entries.find { it.value == value } ?: default
    }
}
