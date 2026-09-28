package com.ruleup.challenge.presentation.common

import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeGate
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeStats
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.JoinNote
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.domain.entity.category.Category

internal val previewChallenge =
    ChallengeDetail(
        title = "매일 아침 가볍게 걷기",
        category = Category.entries.first(),
        imageUrl = null,
        challengeId = "preview-challenge",
        description = "하루 30분, 함께 걷는 습관을 만들어요.",
        mode = ChallengeMode.GROUP,
        visibility = ChallengeVisibility.PUBLIC,
        status = ChallengeStatus.ACTIVE,
        owner = null,
        ownerType = OwnerType.USER,
        participantCount = 12,
        capacity = 30,
        isFull = false,
        period = ChallengePeriod("2026-09-01", "2026-09-30", remainingDays = 2),
        verification = VerificationConfig(VerificationType.MANUAL, VerificationMethod.SELF_CHECK),
        stats = ChallengeStats(completionRate = 0.8, retentionRate = 0.9),
        gate = ChallengeGate(minTier = null, myDisplayTier = null, eligible = true),
        joinBlockReason = null,
        rejoinAvailableAt = null,
        joinNote = JoinNote.IMMEDIATE,
        cloneable = true,
        myRole = MemberRole.MEMBER,
        moderation = null,
    )
