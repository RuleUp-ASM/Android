package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.user.Tier
import com.ruleup.domain.entity.user.User
import com.ruleup.domain.entity.user.UserIdentity

/** 멤버 역할. */
enum class MemberRole(
    val value: String,
) {
    OWNER("OWNER"),
    MANAGER("MANAGER"),
    MEMBER("MEMBER"),
    NONE("NONE"),
    ;

    /** 방 관리 권한(공지 작성·수정·고정 등) 보유 여부. */
    val canManage: Boolean
        get() = this == OWNER || this == MANAGER

    /** 방장인가 */
    val isOwner: Boolean
        get() = this == OWNER

    val isManager: Boolean
        get() = this == MANAGER

    /** 참여자인가. */
    val isMember: Boolean
        get() = this != NONE

    companion object {
        fun fromValue(value: String?): MemberRole? = entries.find { it.value == value }
    }
}

/** 챌린지 멤버. */
data class ChallengeMember(
    val user: User,
    val role: MemberRole,
    // 표시 티어.
    val tier: Tier?,
    // ISO datetime, 참여 시각
    val joinedAt: String,
) : UserIdentity by user {
    val userId: String get() = user.id
}

/** 챌린지 멤버 목록. */
data class ChallengeMembers(
    val challengeId: String,
    val participantCount: Int,
    // null 이면 무제한
    val capacity: Int?,
    val members: List<ChallengeMember>,
)

/** 챌린지 가입 결과. */
data class JoinResult(
    // 판정이 시작되는 날짜.
    val countFromCycle: String?,
    val requiredPermissions: List<String>,
    // true 면 개인 인증 설정(앵커·대상 앱) 화면으로 보낸다.
    val personalSetupRequired: Boolean,
)

/** 가입 차단 사유. */
enum class JoinBlockReason(
    val value: String,
) {
    // 비공개 방
    PRIVATE_INVITE_ONLY("PRIVATE_INVITE_ONLY"),

    // 재입장 대기 중 (자진 탈퇴 1주 / 강퇴 1주→2주→4주 배수)
    REJOIN_COOLDOWN("REJOIN_COOLDOWN"),

    // 동시 참여 무료 3개 초과
    FREE_LIMIT("FREE_LIMIT"),

    FULL("FULL"),

    // 표시 티어가 minTier 미만
    TIER_GATE("TIER_GATE"),

    // 해당 챌린지 영구 차단
    BANNED("PERMANENT_BAN"),

    ALREADY_JOINED("ALREADY_JOINED"),

    CHALLENGE_COMPLETED("CHALLENGE_COMPLETED"),
    ;

    /** 이미 참여 중이라 막힌 경우 */
    val isAlreadyJoined: Boolean
        get() = this == ALREADY_JOINED

    val isPrivateInviteOnly: Boolean
        get() = this == PRIVATE_INVITE_ONLY

    /** 막힌 순간의 상태가 곧 낡는가 */
    val needsRefresh: Boolean
        get() = this == FULL

    companion object {
        fun fromValue(value: String?): JoinBlockReason? = entries.find { it.value == value }
    }
}

/** 가입이 게이트에 막혔다. */
class JoinBlockedException(
    val reason: JoinBlockReason?,
    // REJOIN_COOLDOWN 일 때만
    val rejoinAvailableAt: String? = null,
) : Exception("챌린지에 참여할 수 없습니다.")

/** 챌린지 탈퇴 결과. */
data class LeaveResult(
    val penaltyApplied: Boolean,
)
