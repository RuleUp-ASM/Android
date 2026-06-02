package com.ruleup.domain.auth.model

/**
 * 닉네임 중복 체크 결과. 사용 불가일 때만 [reason] 이 채워진다.
 */
data class NicknameAvailability(
    val available: Boolean,
    val reason: String?,
)
