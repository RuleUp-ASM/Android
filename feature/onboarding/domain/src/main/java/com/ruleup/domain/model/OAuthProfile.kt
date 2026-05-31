package com.ruleup.domain.model

/**
 * 신규 사용자일 때 IdP 로부터 받아온 가입 화면 채움용 힌트.
 */
data class OAuthProfile(
    val email: String?,
    val profileImageUrlHint: String?,
)
