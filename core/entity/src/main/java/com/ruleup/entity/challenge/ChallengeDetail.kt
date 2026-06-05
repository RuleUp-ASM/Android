package com.ruleup.entity.challenge

import com.ruleup.entity.user.InterestCategory

/** 챌린지 생성자 정보 (명세 3.3 owner). 익명 챌린지면 닉네임 마스킹. */
data class ChallengeOwner(
    val nickname: String,
)

/** 챌린지 통계(표시용, 명세 3.3 stats). */
data class ChallengeStats(
    // ACTIVE 멤버 수
    val participantCount: Int,
    // 참여자 평균 매너, 멤버 없으면 null
    val averageMannerTemperature: Double?,
    // 완주율(%), 인증 기능 전까지 항상 null
    val completionRate: Double?,
)

/** 참여 자격 (명세 3.3 eligibility, CH-04). */
data class ChallengeEligibility(
    val canJoin: Boolean,
    val myMannerTemperature: Double,
    // 그룹만
    val minMannerTemperature: Double?,
)

/**
 * 챌린지 상세 + 참여 자격 (명세 3.3 response).
 */
data class ChallengeDetail(
    val challengeId: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val category: InterestCategory?,
    val participationType: ParticipationType,
    val status: ChallengeStatus,
    val owner: ChallengeOwner,
    val repeatDays: List<RepeatDay>,
    val durationDays: Int,
    val startDate: String,
    val endDate: String,
    val verificationMethods: List<VerificationMethod>,
    val penalty: Penalty,
    val reward: Reward,
    val stats: ChallengeStats,
    val eligibility: ChallengeEligibility,
)
