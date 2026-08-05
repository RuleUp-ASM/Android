package com.ruleup.onboarding.domain.fake

import com.ruleup.onboarding.domain.entity.AccountStatus
import com.ruleup.onboarding.domain.entity.LockInfo
import com.ruleup.onboarding.domain.entity.Tier
import com.ruleup.onboarding.domain.entity.User
import com.ruleup.profile.domain.entity.NicknameStatus

/** 테스트용 사용자. 관심 있는 필드만 덮어써서 쓴다. */
fun testUser(
    id: String = "u1",
    nickname: String = "nick",
    nicknameStatus: NicknameStatus = NicknameStatus.APPROVED,
    accountStatus: AccountStatus = AccountStatus.ACTIVE,
    lockInfo: LockInfo? = null,
    profileImageUrl: String? = null,
) = User(
    id = id,
    nickname = nickname,
    nicknameStatus = nicknameStatus,
    profileImageUrl = profileImageUrl,
    tier = Tier.BRONZE,
    score = 10,
    displayTier = Tier.BRONZE,
    interestCategories = emptyList(),
    onboardingCompleted = true,
    accountStatus = accountStatus,
    lockInfo = lockInfo,
)
