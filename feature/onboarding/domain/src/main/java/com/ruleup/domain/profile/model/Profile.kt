package com.ruleup.domain.profile.model

import com.ruleup.core.model.InterestCategory

/**
 * 내 프로필 상세. 닉네임 변경 가능 시점 정보를 포함한다.
 * 날짜 필드는 ISO-8601 문자열이며, 표시 단계에서 시간 타입으로 변환한다.
 */
data class Profile(
    val id: String,
    val nickname: String,
    val email: String?,
    val profileImageUrl: String?,
    val nicknameChangedAt: String?,
    val nicknameChangeableAfter: String?,
    val mannerTemperature: Double,
    val interestCategories: List<InterestCategory>,
    val createdAt: String?,
)
