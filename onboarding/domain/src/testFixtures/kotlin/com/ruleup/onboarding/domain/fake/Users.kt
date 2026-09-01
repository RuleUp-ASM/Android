package com.ruleup.onboarding.domain.fake

import com.ruleup.domain.entity.user.AccountStatus
import com.ruleup.domain.entity.user.LockInfo
import com.ruleup.domain.entity.user.NicknameStatus
import com.ruleup.domain.entity.user.Tier
import com.ruleup.domain.entity.user.User

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
