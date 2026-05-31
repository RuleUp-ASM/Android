package com.ruleup.core.model

/**
 * 로그인/가입 응답에 포함되는 사용자 요약 정보.
 */
data class User(
    val id: String,
    val nickname: String,
    val email: String?,
    val profileImageUrl: String?,
    val mannerTemperature: Double,
    val interestCategories: List<InterestCategory>,
)
