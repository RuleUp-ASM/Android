package com.ruleup.domain.entity.user

/** 계정 상태. */
enum class AccountStatus(
    val value: String,
) {
    ACTIVE("ACTIVE"),

    /** 열람 전용. */
    LOCKED("LOCKED"),

    /** 로그인 정지. */
    SUSPENDED("SUSPENDED"),
    ;

    companion object {
        /** 미지 값은 [ACTIVE] 로 본다 */
        fun fromValue(value: String?): AccountStatus = entries.find { it.value == value } ?: ACTIVE
    }
}

/** 잠금 사유와 해제 시각. */
data class LockInfo(
    val reason: String,
    val unlockAt: String,
)
