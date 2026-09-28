package com.ruleup.profile.domain.entity

import com.ruleup.domain.entity.user.Tier
import com.ruleup.domain.entity.user.User
import com.ruleup.domain.entity.user.UserIdentity

/** 타인 프로필. */
data class MemberProfile(
    val user: User,
    val tier: Tier,
    val completedChallengeCount: Int,
    val withdrawn: Boolean,
    val blocked: Boolean,
) : UserIdentity by user {
    val userId: String get() = user.id
}
