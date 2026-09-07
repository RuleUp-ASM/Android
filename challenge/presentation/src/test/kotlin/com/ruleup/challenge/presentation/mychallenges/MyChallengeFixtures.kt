package com.ruleup.challenge.presentation.mychallenges

import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.LeftType
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.MyChallenge
import com.ruleup.challenge.domain.entity.MyChallengePage
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.domain.entity.category.Category

/** 목록 항목 픽스처. 테스트 본문에는 그 테스트가 신경 쓰는 값만 넘긴다. */
internal fun myChallenge(
    id: String = "ch1",
    title: String = "아침 6시 기상",
    mode: ChallengeMode = ChallengeMode.GROUP,
    status: ChallengeStatus = ChallengeStatus.ACTIVE,
    participantCount: Int = 8,
    weeklyCount: Int = 5,
    start: String = "2026-06-02",
    end: String = "2026-07-13",
    leftType: LeftType? = null,
    leftAt: String? = null,
) = MyChallenge(
    challengeId = id,
    title = title,
    description = null,
    imageUrl = null,
    category = Category.entries.first(),
    mode = mode,
    visibility = null,
    status = status,
    participantCount = participantCount,
    capacity = 10,
    minTier = null,
    weeklyCount = weeklyCount,
    period = ChallengePeriod(start = start, end = end),
    myRole = MemberRole.MEMBER,
    ownerType = OwnerType.USER,
    leftType = leftType,
    leftAt = leftAt,
)

internal fun page(
    vararg challenges: MyChallenge,
    nextCursor: String? = null,
) = MyChallengePage(challenges = challenges.toList(), nextCursor = nextCursor, hasNext = nextCursor != null)
