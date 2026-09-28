package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier

/** 내 챌린지 목록 탭. */
enum class MyChallengeFilter(
    val value: String,
) {
    // UPCOMING + ACTIVE
    IN_PROGRESS("IN_PROGRESS"),

    // 완주·기간 만료
    COMPLETED("COMPLETED"),

    // 강퇴·중도 탈퇴·자동 탈퇴
    LEFT("LEFT"),
}

/** 방을 떠난 방식. */
enum class LeftType(
    val value: String,
) {
    SELF("SELF"),

    // 부정행위 검출
    KICK_CHEAT("KICK_CHEAT"),
    KICK_FAIL("KICK_FAIL"),
    KICK_PERMISSION("KICK_PERMISSION"),

    // 티어 미달 자동 탈퇴
    AUTO_TIER("AUTO_TIER"),

    // 계정 제재(잠금·영구 정지)에 따른 전 챌린지 자동 탈퇴
    AUTO_SANCTION("AUTO_SANCTION"),

    // 챌린지 직권 폐쇄에 따른 자동 탈퇴
    AUTO_CLOSED("AUTO_CLOSED"),

    // 폐지
    KICK_REPORT("KICK_REPORT"),
    KICK_BY_OWNER("KICK_BY_OWNER"),
    ;

    /** 내가 나간 것이 아니라 밀려난 것인가 */
    val isKicked: Boolean
        get() = this != SELF

    companion object {
        /** 미지 값은 null */
        fun fromValue(value: String?): LeftType? = entries.find { it.value == value }
    }
}

/** 내 챌린지 목록 항목. */
data class MyChallenge(
    val challengeId: String,
    val title: String,
    val description: String?,
    // 대표 이미지 (없으면 기본 이미지)
    val imageUrl: String?,
    val category: Category?,
    val mode: ChallengeMode,
    // 그룹만 값이 있다
    val visibility: ChallengeVisibility?,
    val status: ChallengeStatus,
    val participantCount: Int,
    // null 이면 무제한
    val capacity: Int?,
    // 최소 입장 티어 (없으면 null)
    val minTier: Tier?,
    // 주간 수행 횟수 1~7.
    val weeklyCount: Int,
    val period: ChallengePeriod,
    // 내 역할 (OWNER / MANAGER / MEMBER).
    val myRole: MemberRole,
    val ownerType: OwnerType,
    // LEFT 탭에서만.
    val leftType: LeftType?,
    // 이탈 시각 ISO-8601.
    val leftAt: String?,
    /** 이 방에서의 내 성공률 0~1. */
    val successRate: Double?,
) {
    /** 목록 탭·뱃지 판정. */
    val isUpcoming: Boolean
        get() = status == ChallengeStatus.UPCOMING
}

/** 내 챌린지 목록 한 페이지. */
data class MyChallengePage(
    val challenges: List<MyChallenge>,
    val nextCursor: String?,
    val hasNext: Boolean,
)
