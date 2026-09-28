package com.ruleup.domain.entity.user

/** 닉네임 검수 상태(LLM 비동기 검수). */
enum class NicknameStatus(
    val value: String,
) {
    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),

    /** 탈퇴 후 복원 중 기존 닉네임을 남이 선점했다. */
    CONFLICT("CONFLICT"),
    ;

    companion object {
        // 미지 값은 뱃지 없는 APPROVED 취급 (서버 enum 확장 대비)
        fun fromValue(value: String?): NicknameStatus = entries.find { it.value == value } ?: APPROVED
    }
}
