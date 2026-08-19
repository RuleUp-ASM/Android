package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier

/** 방장 종류 (명세 `ownerType`). 방장은 항상 정확히 1명이며, 승계자가 없으면 BOT 이 자리를 지킨다. */
enum class OwnerType(
    val value: String,
) {
    USER("USER"),
    BOT("BOT"),
    ;

    companion object {
        fun fromValue(value: String?): OwnerType = entries.find { it.value == value } ?: USER
    }
}

/** 챌린지 방장 (명세 `owner`). 봇방장이면 null 이다. */
data class ChallengeOwner(
    val userId: String,
    val nickname: String,
)

/**
 * 공개 상세의 진행 지표 (명세 `stats`).
 *
 * 표본이 적으면 왜곡되므로(2명 중 2명 성공 = 100%) 서버가 기준 미달 시 **null 로 내린다** —
 * 완주율은 10회 이상 참여 멤버 5명 이상, 유지율은 방 누적 진행 횟수 30회 이상일 때만 값이 있다.
 * null 을 0으로 접지 말 것. 화면은 "아직 참여자가 적어 값을 낼 수 없어요"로 안내한다.
 */
data class ChallengeStats(
    // 성공률 80% 이상인 사람의 비율 0~1
    val completionRate: Double?,
    // 확정 실패 없이 버티는 사람의 비율 0~1
    val retentionRate: Double?,
)

/** 입장 자격 (명세 `gate`). 비교 기준은 항상 **표시 티어**다 — 유예 밴드 사용자는 상위 티어로 판정된다. */
data class ChallengeGate(
    val minTier: Tier?,
    val myDisplayTier: Tier?,
    val eligible: Boolean,
)

/** 사이클 중간 입장 안내 (명세 `joinNote`). 사이클은 1주 고정이다. */
enum class JoinNote(
    val value: String,
) {
    // 다음 주 사이클 경계부터 판정
    NEXT_CYCLE("NEXT_CYCLE"),
    IMMEDIATE("IMMEDIATE"),
    ;

    companion object {
        fun fromValue(value: String?): JoinNote = entries.find { it.value == value } ?: IMMEDIATE
    }
}

/**
 * 챌린지 **공개 상세** (명세 `GET /challenges/{challengeId}`). 방에 들어간 뒤의 내부 화면은 `/room` 이 맡는다.
 *
 * **누가 조회하느냐에 따라 내용이 다르다.** 방장 본인은 자기가 입력한 제목·설명·이미지를 그대로 보고
 * [moderation] 으로 각 항목의 심사 상태까지 받는다. 남이 보면 심사 중이거나 거부된 항목이 가려진 채
 * 내려오고 [moderation] 은 null 이다.
 */
data class ChallengeDetail(
    val challengeId: String,
    val title: String,
    // 심사 중·거부 시 타인 화면에서는 빈 값
    val description: String?,
    val imageUrl: String?,
    val category: Category?,
    val mode: ChallengeMode,
    val visibility: ChallengeVisibility?,
    val status: ChallengeStatus,
    // 봇방장이면 null
    val owner: ChallengeOwner?,
    val ownerType: OwnerType,
    val participantCount: Int,
    val capacity: Int,
    // 정원이 찼어도 카드·상세는 노출한다 — 탈퇴로 자리가 날 수 있다
    val isFull: Boolean,
    val period: ChallengePeriod,
    val verification: VerificationConfig,
    val stats: ChallengeStats,
    val gate: ChallengeGate,
    // 지금 못 들어가는 이유 미리보기. null 이면 게이트를 통과한 상태다.
    val joinBlockReason: JoinBlockReason?,
    // REJOIN_COOLDOWN 일 때만
    val rejoinAvailableAt: String?,
    val joinNote: JoinNote,
    // 템플릿 복제 가능 여부 — 공개 그룹만 true
    val cloneable: Boolean,
    val myRole: MemberRole,
    // 방장 본인 조회에서만
    val moderation: ChallengeModeration?,
) {
    /** 참여 버튼을 활성할 수 있는지. 자격·정원·차단 사유를 모두 통과해야 한다. */
    val joinable: Boolean
        get() = joinBlockReason == null && gate.eligible && !isFull && myRole == MemberRole.NONE
}

/**
 * 챌린지를 찾을 수 없다 (명세 404 `CHALLENGE_NOT_FOUND`).
 *
 * 없음·비공개·삭제됨을 **구분하지 않는다** — 존재 은닉이 목적이므로 화면 문구도 하나로 통일한다.
 */
class ChallengeNotFoundException : Exception("찾을 수 없는 챌린지예요.")

/** 복제할 수 없는 방이다 (명세 403 `NOT_CLONEABLE`) — 비공개·솔로 방. */
class ChallengeNotCloneableException : Exception("이 챌린지는 복제할 수 없어요.")
