package com.ruleup.challenge.domain.entity

/** 멤버 역할 (명세 role). OWNER(방장) / MANAGER(공동 관리자) / MEMBER(일반) / NONE(비멤버). */
enum class MemberRole(
    val value: String,
) {
    OWNER("OWNER"),
    MANAGER("MANAGER"),
    MEMBER("MEMBER"),
    NONE("NONE"),
    ;

    /** 방 관리 권한(공지 작성·수정·고정 등) 보유 여부. 방장·공동 관리자만 true. */
    val canManage: Boolean
        get() = this == OWNER || this == MANAGER

    companion object {
        fun fromValue(value: String?): MemberRole? = entries.find { it.value == value }
    }
}

/** 챌린지 멤버 (명세 GET members[]). 승인제 폐기로 목록엔 확정 멤버만 — 상태 필드 없음. */
data class ChallengeMember(
    val userId: String,
    // 익명이면 마스킹
    val nickname: String,
    // 익명이면 null
    val profileImageUrl: String?,
    val role: MemberRole,
    val mannerTemperature: Double,
    // ISO datetime, 참여 시각
    val joinedAt: String,
)

/** 챌린지 멤버 목록 (명세 GET members response). */
data class ChallengeMembers(
    val challengeId: String,
    val participantCount: Int,
    // 최대 참여 인원
    val maxParticipants: Int,
    val members: List<ChallengeMember>,
)

/**
 * 챌린지 가입 결과 (명세 POST members). 승인제 폐기로 성공 시 항상 ACTIVE 이므로 상태는 생략하고,
 * 자동 인증 챌린지의 가입 직후 권한 요청 플로우를 위해 [requiredPermissions] 만 전달한다(수동이면 빈 목록).
 */
data class JoinResult(
    val requiredPermissions: List<String>,
)

/**
 * 챌린지 탈퇴 결과 (명세 DELETE members/me). 본인 success 이력이 있으면 탈퇴 패널티가 트리거된다.
 * 탈퇴 시 해당 챌린지 재참여는 영구 불가.
 */
data class LeaveResult(
    val penaltyApplied: Boolean,
)

/** 공동 관리자 임명/해제 액션 (명세 PATCH members/{userId}/role). */
enum class RoleAction(
    val value: String,
) {
    PROMOTE("PROMOTE"),
    DEMOTE("DEMOTE"),
}

/** 역할 변경 결과 (명세 PATCH members/{userId}/role response). */
data class MemberRoleChange(
    val userId: String,
    val role: MemberRole,
)
