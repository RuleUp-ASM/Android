package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier

/** 멤버 초대 링크. */
data class ChallengeInvitation(
    val invitationId: String,
    val token: String,
    val inviteUrl: String,
    // 발급 +7일
    val expiresAt: String?,
)

/** 초대 링크가 가리키는 방 요약. */
data class InvitedChallenge(
    override val title: String,
    override val category: Category?,
    override val imageUrl: String?,
    val challengeId: String,
    val participantCount: Int,
    // null 이면 무제한
    val capacity: Int?,
    val minTier: Tier?,
    val startDate: String?,
    val endDate: String?,
) : Challenge

/** 초대 링크 미리보기. */
data class ChallengeInvitationPreview(
    val invitationId: String,
    val challenge: InvitedChallenge,
    val inviterNickname: String?,
    val joinable: Boolean,
    val blockReason: JoinBlockReason?,
    val expiresAt: String?,
)
