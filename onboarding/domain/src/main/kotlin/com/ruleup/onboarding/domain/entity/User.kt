package com.ruleup.onboarding.domain.entity

import com.ruleup.domain.entity.category.Category

/**
 * 로그인·가입 응답이 싣고 오는 사용자. onboarding 밖에서는 쓰지 않는다 —
 * 화면에 보이는 사용자 정보는 [com.ruleup.profile.domain.entity.Profile] 쪽이다.
 */
data class User(
    val id: String,
    val nickname: String,
    val email: String?,
    val profileImageUrl: String?,
    val mannerTemperature: Double,
    val interestCategories: List<Category>,
)

/** 가입 시 동의 항목. */
data class Agreement(
    val terms: Boolean,
    val privacy: Boolean,
    val marketing: Boolean,
)
