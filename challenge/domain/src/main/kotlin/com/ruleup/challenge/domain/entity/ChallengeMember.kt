package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.user.Tier

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

    /** 방장인가 — 삭제·위임·권한 변경은 방장만 한다. */
    val isOwner: Boolean
        get() = this == OWNER

    /** 공동 관리자인가. */
    val isManager: Boolean
        get() = this == MANAGER

    /** 참여자인가. [NONE] 은 아직 참여하지 않은 상태다. */
    val isMember: Boolean
        get() = this != NONE

    companion object {
        fun fromValue(value: String?): MemberRole? = entries.find { it.value == value }
    }
}

/** 챌린지 멤버 (명세 GET members[]). 승인제 폐기로 목록엔 확정 멤버만 — 상태 필드 없음. */
data class ChallengeMember(
    val userId: String,
    val nickname: String,
    val profileImageUrl: String?,
    val role: MemberRole,
    // 표시 티어. 구 매너 온도를 대체한다.
    val tier: Tier?,
    // ISO datetime, 참여 시각
    val joinedAt: String,
)

/** 챌린지 멤버 목록 (명세 GET members response). */
data class ChallengeMembers(
    val challengeId: String,
    val participantCount: Int,
    val capacity: Int,
    val members: List<ChallengeMember>,
)

/**
 * 챌린지 가입 결과 (명세 POST members 200).
 *
 * [requiredPermissions] 는 참고용이다 — 권한은 **가입 전에** 공개 상세의 `verification.requiredPermissions`
 * 로 이미 확보한 상태여야 한다. 가입 후 권한 거부를 탈퇴로 롤백하는 경로는 폐기됐다(탈퇴 감점·재입장
 * 1주 대기 부작용).
 */
data class JoinResult(
    // 판정이 시작되는 날짜. 사이클(1주 고정) 중간에 들어오면 다음 사이클 경계다.
    val countFromCycle: String?,
    val requiredPermissions: List<String>,
    // true 면 개인 인증 설정(앵커·대상 앱) 화면으로 보낸다.
    val personalSetupRequired: Boolean,
)

/**
 * 가입 차단 사유 (명세 409 `JOIN_BLOCKED` 의 `reason`). 공개 상세의 `joinBlockReason` 과 같은 enum 이라
 * 화면은 가입을 시도하기 전에도 같은 문구를 미리 보여줄 수 있다.
 */
enum class JoinBlockReason(
    val value: String,
) {
    // 비공개 방 — 초대 링크로만 입장 가능
    PRIVATE_INVITE_ONLY("PRIVATE_INVITE_ONLY"),

    // 재입장 대기 중 (자진 탈퇴 1주 / 강퇴 1주→2주→4주 배수)
    REJOIN_COOLDOWN("REJOIN_COOLDOWN"),

    // 동시 참여 무료 3개 초과 — BM 확정 대기
    FREE_LIMIT("FREE_LIMIT"),

    FULL("FULL"),

    // 표시 티어가 minTier 미만
    TIER_GATE("TIER_GATE"),

    // 해당 챌린지 영구 차단 — 사유는 설명하지 않는다
    BANNED("BANNED"),

    ALREADY_JOINED("ALREADY_JOINED"),

    CHALLENGE_COMPLETED("CHALLENGE_COMPLETED"),
    ;

    /** 이미 참여 중이라 막힌 경우 — 알릴 게 없어 조용히 방 상세로 전환한다. */
    val isAlreadyJoined: Boolean
        get() = this == ALREADY_JOINED

    /** 초대 전용이라 막힌 경우. */
    val isPrivateInviteOnly: Boolean
        get() = this == PRIVATE_INVITE_ONLY

    /** 막힌 순간의 상태가 곧 낡는가 — 정원은 수시로 변해 다시 받아야 뱃지가 맞는다. */
    val needsRefresh: Boolean
        get() = this == FULL

    companion object {
        fun fromValue(value: String?): JoinBlockReason? = entries.find { it.value == value }
    }
}

/**
 * 가입이 게이트에 막혔다 (명세 409 `JOIN_BLOCKED`). 사유별로 다른 문구·다음 행동을 제공한다.
 * [reason] 이 null 이면 서버가 앱이 모르는 사유를 보낸 것이므로 일반 안내로 떨어뜨린다.
 */
class JoinBlockedException(
    val reason: JoinBlockReason?,
    // REJOIN_COOLDOWN 일 때만 — 재입장 가능 시각(ISO)
    val rejoinAvailableAt: String? = null,
) : Exception("챌린지에 참여할 수 없습니다.")

/**
 * 챌린지 탈퇴 결과 (명세 DELETE members/me). 본인 success 이력이 있으면 탈퇴 패널티가 트리거된다.
 * 탈퇴 후에는 재입장 대기(자진 탈퇴 1주)가 걸린다 — 영구 차단은 아니다.
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

/**
 * 봇방장 방 클레임 결과 (명세 POST owner/claim).
 *
 * [graceUntil] 까지는 **방장 본인뿐 아니라 그 방의 잔류 멤버 전원이** 탈퇴해도 감점되지 않는다
 * (승계 면책). 손들고 방장이 되는 부담을 낮추려는 장치라, 화면도 이 시각을 근거로 안내한다.
 */
data class OwnerClaimResult(
    val myRole: MemberRole,
    // ISO datetime. 면책 기간이 없으면 null
    val graceUntil: String?,
)

/**
 * 선착순 클레임에서 밀렸다 (명세 409 `OWNER_ALREADY_EXISTS`).
 *
 * 조건부 UPDATE 라 경합에서 한 명만 성공한다 — 실패는 오류가 아니라 정상적인 결과이므로 화면은
 * 에러가 아니라 "이미 다른 분이 방장이 되었어요" 로 안내하고 방을 다시 받는다.
 */
class OwnerAlreadyExistsException : Exception("이미 다른 분이 방장이 되었어요.")
