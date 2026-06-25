package com.ruleup.entity.user

/** 내 프로필 (명세 4.8/4.9). */
data class Profile(
    val id: String,
    val nickname: String,
    val email: String?,
    val profileImageUrl: String?,
    // ISO 8601, null 이면 변경 이력 없음
    val nicknameChangedAt: String?,
    // ISO 8601, null 이면 즉시 변경 가능
    val nicknameChangeableAfter: String?,
    val mannerTemperature: Double,
    val interestCategories: List<InterestCategory>,
    val createdAt: String,
)

/** 관심 카테고리 마스터 (명세 4.7). */
data class CategoryCatalog(
    val maxSelectable: Int,
    val categories: List<InterestCategory>,
)

/** 닉네임 검사 결과 (명세 4.6). */
data class NicknameCheck(
    // 형식 통과 여부(확인 전)
    val valid: Boolean,
    // 사용 가능 여부(확인 후)
    val available: Boolean,
    val reason: NicknameCheckReason?,
)

enum class NicknameCheckReason {
    FORMAT,
    DUPLICATED,
    ;

    companion object {
        fun fromValue(value: String?): NicknameCheckReason? = entries.find { it.name == value }
    }
}
