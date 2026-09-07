package com.ruleup.profile.domain.entity

import com.ruleup.domain.entity.category.Category

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
    val interestCategories: List<Category>,
    val createdAt: String,
)

/** 관심 카테고리 마스터 (명세 4.7). */
data class CategoryCatalog(
    val maxSelectable: Int,
    val categories: List<Category>,
)

/**
 * 닉네임 검사 결과(POST /nicknames/check).
 *
 * 형식 위반도 에러가 아니라 200 + `valid=false, reason=FORMAT` 으로 온다 — 실시간 확인 UX 에서
 * 에러 봉투 분기를 없애려는 서버 결정이다.
 *
 * @property availableAt [NicknameCheckReason.RECENTLY_RELEASED] 일 때만 채워지는 잠금 해제 시각(ISO).
 */
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

    /** 누가 최근에 버린 닉네임. 사칭 방지로 1주간 아무도 못 쓴다. */
    RECENTLY_RELEASED,
    ;

    companion object {
        fun fromValue(value: String?): NicknameCheckReason? = entries.find { it.name == value }
    }
}
