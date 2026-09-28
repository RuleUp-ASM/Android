package com.ruleup.profile.domain.entity

import com.ruleup.domain.entity.user.AccountStatus
import com.ruleup.domain.entity.user.LockInfo
import com.ruleup.domain.entity.user.NicknameStatus
import com.ruleup.domain.entity.user.Tier

/** 마이 홈 카운트. */
data class MyHomeCounts(
    val inProgress: Int,
    val completed: Int,
    val left: Int,
)

/** 마이 홈 일괄 조회. */
data class MyHome(
    val nickname: String,
    val nicknameStatus: NicknameStatus,
    val profileImageUrl: String?,
    val tier: Tier,
    // 누적 점수 0~2,000
    val score: Int,
    // 표시·방 입장 판정에 쓰는 티어.
    val displayTier: Tier,
    val counts: MyHomeCounts,
    val accountStatus: AccountStatus,
    val lockInfo: LockInfo?,
)
