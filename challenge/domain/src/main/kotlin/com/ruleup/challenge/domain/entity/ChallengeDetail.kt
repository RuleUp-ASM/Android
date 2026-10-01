package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier
import com.ruleup.domain.entity.user.User

/** 방장 종류. */
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

/** 공개 상세의 진행 지표. */
data class ChallengeStats(
    // 성공률 80% 이상인 사람의 비율 0~1
    val completionRate: Double?,
    // 확정 실패 없이 버티는 사람의 비율 0~1
    val retentionRate: Double?,
)

/** 입장 자격. */
data class ChallengeGate(
    val minTier: Tier?,
    val myDisplayTier: Tier?,
    val eligible: Boolean,
)

/** 사이클 중간 입장 안내. */
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

/** 챌린지 공개 상세. */
data class ChallengeDetail(
    override val title: String,
    override val category: Category?,
    override val imageUrl: String?,
    val challengeId: String,
    // 심사 중·거부 시 타인 화면에서는 빈 값
    val description: String?,
    val mode: ChallengeMode,
    val visibility: ChallengeVisibility?,
    val status: ChallengeStatus,
    // 봇방장이면 null
    val owner: User?,
    val ownerType: OwnerType,
    val participantCount: Int,
    // null 이면 무제한
    val capacity: Int?,
    // 정원이 찼어도 카드·상세는 노출한다
    val isFull: Boolean,
    val period: ChallengePeriod,
    val verification: VerificationConfig,
    val stats: ChallengeStats,
    val gate: ChallengeGate,
    // 지금 못 들어가는 이유 미리보기.
    val joinBlockReason: JoinBlockReason?,
    // REJOIN_COOLDOWN 일 때만
    val rejoinAvailableAt: String?,
    val joinNote: JoinNote,
    // 템플릿 복제 가능 여부
    val cloneable: Boolean,
    val myRole: MemberRole,
    // 방장 본인 조회에서만
    val moderation: ChallengeModeration?,
    val penalties: ChallengePenalties? = null,
    /** 주간 수행 횟수 1~7. 서버가 주지 않으면 null 이고, 설명에서 짐작하지 않는다. */
    val weeklyCount: Int? = null,
) : Challenge {
    /** 참여 버튼을 활성할 수 있는지. */
    val joinable: Boolean
        get() = joinBlockReason == null && gate.eligible && !isFull && myRole == MemberRole.NONE

    /** 오늘 수동 인증을 할 수 있는 방인가 */
    val manualCheckable: Boolean
        get() =
            verification.type == VerificationType.MANUAL &&
                myRole != MemberRole.NONE &&
                status == ChallengeStatus.ACTIVE
}

/** 챌린지를 찾을 수 없다. */
class ChallengeNotFoundException : Exception("찾을 수 없는 챌린지예요.")

/** 복제할 수 없는 방이다 */
class ChallengeNotCloneableException : Exception("이 챌린지는 복제할 수 없어요.")
