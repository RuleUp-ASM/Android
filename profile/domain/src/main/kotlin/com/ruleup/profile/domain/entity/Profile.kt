package com.ruleup.profile.domain.entity

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.User
import com.ruleup.domain.entity.user.UserIdentity

/** 내 프로필. */
data class Profile(
    val user: User,
    val email: String?,
    // ISO 8601, null 이면 변경 이력 없음
    val nicknameChangedAt: String?,
    // ISO 8601, null 이면 즉시 변경 가능
    val nicknameChangeableAfter: String?,
    val mannerTemperature: Double,
    val interestCategories: List<Category>,
    val createdAt: String,
) : UserIdentity by user

/** 관심 카테고리 마스터. */
data class CategoryCatalog(
    val maxSelectable: Int,
    val categories: List<Category>,
)

/** 닉네임 검사 결과(POST /nicknames/check). */
data class NicknameCheck(
    // 형식 통과 여부(확인 전)
    val valid: Boolean,
    // 사용 가능 여부(확인 후)
    val available: Boolean,
    val reason: NicknameCheckReason?,
    val availableAt: String? = null,
)

enum class NicknameCheckReason {
    FORMAT,
    DUPLICATED,

    /** 누가 최근에 버린 닉네임. */
    RECENTLY_RELEASED,
    ;

    companion object {
        fun fromValue(value: String?): NicknameCheckReason? = entries.find { it.name == value }
    }
}
