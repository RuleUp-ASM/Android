package com.ruleup.domain.entity.user

import com.ruleup.domain.entity.category.Category

/** 사용자. */
data class User(
    override val id: String,
    override val nickname: String,
    override val profileImageUrl: String?,
    val account: UserAccount? = null,
    val relationship: UserRelationship? = null,
) : UserIdentity

/** 본인 계정 정보. */
data class UserAccount(
    val nicknameStatus: NicknameStatus,
    val tier: Tier,
    val score: Int,
    val displayTier: Tier,
    val provider: SocialProvider?,
    val interestCategories: List<Category>,
    val onboardingCompleted: Boolean,
    val accountStatus: AccountStatus,
    val lockInfo: LockInfo?,
)

/** 사용자와의 관계. */
data class UserRelationship(
    val blocked: Boolean,
)
