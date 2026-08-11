package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier

/**
 * 내가 참여 중인 챌린지 목록 항목(명세: GET /challenges).
 * 승인제 폐기로 멤버십은 항상 확정 상태다(memberStatus 없음). 내 역할은 [myRole].
 */
data class MyChallenge(
    val challengeId: String,
    val title: String,
    val description: String?,
    // 대표 이미지 (없으면 기본 이미지)
    val imageUrl: String?,
    val category: Category?,
    val mode: ChallengeMode,
    val status: ChallengeStatus,
    val participantCount: Int,
    val capacity: Int,
    // 최소 입장 티어 (없으면 null)
    val minTier: Tier?,
    val period: ChallengePeriod,
    // 내 역할 (OWNER / MANAGER / MEMBER)
    val myRole: MemberRole,
) {
    /** 목록 탭·뱃지 판정. 시작 전 방은 진행 지표 대신 시작일 카운트다운을 보여준다. */
    val isUpcoming: Boolean
        get() = status == ChallengeStatus.UPCOMING
}
