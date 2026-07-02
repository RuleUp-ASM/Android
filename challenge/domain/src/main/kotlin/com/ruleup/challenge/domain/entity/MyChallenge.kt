package com.ruleup.challenge.domain.entity

import com.ruleup.entity.user.InterestCategory

/** 내 챌린지 목록 조회 범위(명세: GET /challenges scope). */
enum class MyChallengeScope(
    val value: String,
) {
    // 실제 참여 중인 멤버십(ACTIVE)만
    ACTIVE("ACTIVE"),

    // 승인 대기(PENDING) 포함 전체 멤버십
    ALL("ALL"),
}

/**
 * 내가 참여 중인 챌린지 목록 항목(명세: GET /challenges challenges[]).
 * 항목마다 내 멤버십 상태([memberStatus])를 함께 내려줘 참여 중(ACTIVE)과 승인 대기(PENDING)를 구분한다.
 */
data class MyChallenge(
    val challengeId: String,
    val title: String,
    val description: String?,
    // 대표 이미지 (없으면 null)
    val imageUrl: String?,
    // 제목 기반 분류 (인식 불가 시 null)
    val category: InterestCategory?,
    val participationType: ParticipationType,
    val status: ChallengeStatus,
    val anonymity: Anonymity,
    // 현재 ACTIVE 참여자 수
    val participantCount: Int,
    // 그룹 참여 매너 온도 하한 (없으면 null)
    val minMannerTemperature: Double?,
    val repeatDays: List<RepeatDay>,
    val durationDays: Int,
    // ISO date
    val startDate: String,
    // ISO date
    val endDate: String,
    // 내 멤버십 상태 (ACTIVE / PENDING)
    val memberStatus: MemberStatus,
)
