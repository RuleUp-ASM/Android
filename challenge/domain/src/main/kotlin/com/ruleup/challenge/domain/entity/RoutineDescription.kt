package com.ruleup.challenge.domain.entity

/**
 * 초안 생성에 넣는 루틴 설명 (명세 draft).
 *
 * 앞뒤 공백을 털고 길이를 생성 시점에 검증한다 — [of] 를 거치지 않고는 만들 수 없어, 어느 화면에서
 * 들어오든 같은 규칙이 걸린다. 화면의 글자 수 카운터도 [MAX_LENGTH] 를 그대로 쓴다.
 */
@JvmInline
value class RoutineDescription private constructor(
    val value: String,
) {
    companion object {
        const val MAX_LENGTH = 200

        fun of(raw: String): RoutineDescription {
            val trimmed = raw.trim()
            require(trimmed.isNotEmpty()) { "루틴 설명을 입력해 주세요." }
            require(trimmed.length <= MAX_LENGTH) { "루틴 설명은 ${MAX_LENGTH}자까지예요." }
            return RoutineDescription(trimmed)
        }
    }
}
