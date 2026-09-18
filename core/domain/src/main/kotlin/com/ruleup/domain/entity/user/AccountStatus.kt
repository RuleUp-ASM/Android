package com.ruleup.domain.entity.user

/**
 * 계정 상태.
 *
 * 영구 정지(`ACCOUNT_BANNED`)는 여기 없다 — 로그인 자체가 403 으로 막혀 응답 본문이 오지 않으므로
 * 상태값이 아니라 예외로 다룬다.
 */
enum class AccountStatus(
    val value: String,
) {
    ACTIVE("ACTIVE"),

    /** 열람 전용. 로그인은 되지만 프로필 편집 등 일부 기능이 막힌다. */
    LOCKED("LOCKED"),

    /**
     * 로그인 정지. 잠금 토큰만 발급돼 **제재 이력 열람과 CS 문의 외의 API 가 막힌다**
     * (제재 정책 §5.3). 화면은 잠금 화면으로 고정 진입한다.
     */
    SUSPENDED("SUSPENDED"),
    ;

    companion object {
        /** 미지 값은 [ACTIVE] 로 본다 — 서버 enum 확장이 정상 사용자를 잠그면 안 된다. */
        fun fromValue(value: String?): AccountStatus = entries.find { it.value == value } ?: ACTIVE
    }
}

/** 잠금 사유와 해제 시각. [AccountStatus.LOCKED] 일 때만 온다. */
data class LockInfo(
    val reason: String,
    val unlockAt: String,
)
