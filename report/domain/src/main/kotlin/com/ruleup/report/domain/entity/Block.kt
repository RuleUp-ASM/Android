package com.ruleup.report.domain.entity

import com.ruleup.domain.entity.user.User
import com.ruleup.domain.entity.user.UserIdentity

/** 내가 차단한 목록. */
data class BlockList(
    val users: List<BlockedUser>,
    val challenges: List<BlockedChallenge>,
) {
    /** 양쪽이 모두 비었는지 */
    val isEmpty: Boolean
        get() = users.isEmpty() && challenges.isEmpty()
}

/** 차단한 사용자. */
data class BlockedUser(
    val user: User,
    // ISO datetime.
    val blockedAt: String?,
) : UserIdentity by user {
    val userId: String get() = user.id
    val maskedNickname: String get() = user.nickname
}

/** 차단한 챌린지. */
data class BlockedChallenge(
    val challengeId: String,
    val maskedTitle: String,
    // 참여 중이면 탐색에서 지우는 대신 제목·이미지만 가린다(HiddenEffect.CHALLENGE_MASKED).
    val participating: Boolean,
    val blockedAt: String?,
)
