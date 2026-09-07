package com.ruleup.domain.entity.user

/**
 * 닉네임 검수 상태(LLM 비동기 검수).
 *
 * 본인 화면에는 검수 상태와 무관하게 본인이 정한 닉네임을 그대로 보여주고, 뱃지로만 상태를 알린다.
 */
enum class NicknameStatus(
    val value: String,
) {
    PENDING("PENDING"),
    APPROVED("APPROVED"),
    REJECTED("REJECTED"),

    /**
     * 탈퇴 후 복원 중 기존 닉네임을 남이 선점했다. 검수 결과가 아니라 **복원에서만 나오는 상태**다.
     * 클라는 닉네임을 바꾸기 전까지 홈 진입을 막는다.
     */
    CONFLICT("CONFLICT"),
    ;

    companion object {
        // 미지 값은 뱃지 없는 APPROVED 취급 (서버 enum 확장 대비)
        fun fromValue(value: String?): NicknameStatus = entries.find { it.value == value } ?: APPROVED
    }
}
