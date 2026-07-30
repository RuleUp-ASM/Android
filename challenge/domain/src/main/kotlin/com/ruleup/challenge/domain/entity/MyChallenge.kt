package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.category.Category

/**
 * 내가 참여 중인 챌린지 목록 항목(명세: GET /challenges challenges[]).
 * 승인제 폐기로 멤버십은 항상 확정 상태다(memberStatus 없음). 내 역할은 [myRole].
 */
data class MyChallenge(
    val challengeId: String,
    val title: String,
    val description: String?,
    // 대표 이미지 (없으면 null)
    val imageUrl: String?,
    // 제목 기반 분류 (인식 불가 시 null)
    val category: Category?,
    val participationType: ParticipationType,
    val status: ChallengeStatus,
    val anonymity: Anonymity,
    // 현재 참여자 수
    val participantCount: Int,
    // 최대 참여 인원
    val maxParticipants: Int,
    // 그룹 참여 매너 온도 하한 (없으면 null)
    val minMannerTemperature: Double?,
    val repeatDays: List<RepeatDay>,
    val durationDays: Int,
    // ISO date
    val startDate: String,
    // ISO date
    val endDate: String,
    // 내 역할 (OWNER / MANAGER / MEMBER)
    val myRole: MemberRole,
)
