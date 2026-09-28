package com.ruleup.challenge.domain.entity

/** 초안 생성에 넣는 루틴 설명. */
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
