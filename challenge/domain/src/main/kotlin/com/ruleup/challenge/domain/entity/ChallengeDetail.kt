package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.category.Category

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

/** 참여 자격 (명세 eligibility). canJoin 은 정원·기준온도·재참여·모더레이션·상태 종합 판정. */
data class ChallengeEligibility(
    val canJoin: Boolean,
    val myMannerTemperature: Double,
    // 그룹만
    val minMannerTemperature: Double?,
    // 재참여 차단 여부(탈퇴 이력). canJoin=false 사유 구분·안내용.
    val rejoinBlocked: Boolean,
)

/**
 * 챌린지 상세 + 참여 자격 (명세 3.3 response).
 */
data class ChallengeDetail(
    val challengeId: String,
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val category: Category?,
    val participationType: ParticipationType,
    val status: ChallengeStatus,
    // 최대 참여 인원 (SOLO 는 1)
    val maxParticipants: Int,
    // 이미지 모더레이션 상태 (모집 차단 판정용)
    val moderationStatus: ModerationStatus,
    // REJECTED 이미지 수정 기한(거부+1시간, OWNER 전용, ISO-8601). 그 외 null.
    val fixDeadline: String?,
    val owner: ChallengeOwner,
    val repeatDays: List<RepeatDay>,
    val durationDays: Int,
    val startDate: String,
    val endDate: String,
    val templateId: Int?,
    val verification: VerificationConfig?,
    val params: Map<String, ParamValue>,
    val penalty: Penalty,
    val reward: Reward,
    val stats: ChallengeStats,
    val eligibility: ChallengeEligibility,
)
