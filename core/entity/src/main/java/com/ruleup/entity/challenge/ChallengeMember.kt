package com.ruleup.entity.challenge

/** 멤버 상태 (명세 memberStatus). */
enum class MemberStatus(
    val value: String,
) {
    // 운영자 승인 대기
    PENDING("PENDING"),
    // 참여 확정
    ACTIVE("ACTIVE"),
    // 거절/탈퇴
    REMOVED("REMOVED"),
    ;

    companion object {
        fun fromValue(value: String?): MemberStatus? = entries.find { it.value == value }
    }
}

/** 멤버 역할 (명세 3.8 role). */
enum class MemberRole(
    val value: String,
) {
    OWNER("OWNER"),
    MEMBER("MEMBER"),
    ;

    companion object {
        fun fromValue(value: String?): MemberRole? = entries.find { it.value == value }
    }
}

/** 참여 승인/거절 액션 (명세 3.7 action). */
enum class MemberAction(
    val value: String,
) {
    APPROVE("APPROVE"),
    REJECT("REJECT"),
}

/** 멤버 목록 조회 필터 (명세 3.8 status 쿼리). PENDING/ALL 은 OWNER만. */
enum class MemberStatusFilter(
    val value: String,
) {
    ACTIVE("ACTIVE"),
    PENDING("PENDING"),
    ALL("ALL"),
}

/** 챌린지 멤버 (명세 3.8 members[]). */
data class ChallengeMember(
    val userId: String,
    // 익명이면 마스킹
    val nickname: String,
    // 익명이면 null
    val profileImageUrl: String?,
    val role: MemberRole,
    val status: MemberStatus,
    val mannerTemperature: Double,
    // ISO datetime, 참여(또는 신청) 시각
    val joinedAt: String,
)

/** 챌린지 멤버 목록 (명세 3.8 response). */
data class ChallengeMembers(
    val challengeId: String,
    val participantCount: Int,
    val members: List<ChallengeMember>,
)
