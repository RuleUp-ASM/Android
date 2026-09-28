package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.user.User
import com.ruleup.domain.entity.user.UserIdentity

/** 내 오늘 인증 상태. */
enum class TodayVerificationStatus(
    val value: String,
) {
    // 인증 창이 아직 열려 있음
    IN_PROGRESS("IN_PROGRESS"),

    // 이대로면 실패
    FAIL_EXPECTED("FAIL_EXPECTED"),

    // 오늘 인증 완료
    DONE("DONE"),

    // 실패 확정
    FAILED("FAILED"),

    // 오늘은 판정 대상일이 아님
    NOT_TARGET("NOT_TARGET"),
    ;

    /** 실패로 확정됐는가. */
    val isFailure: Boolean
        get() = this == FAILED

    companion object {
        fun fromValue(value: String?): TodayVerificationStatus? = entries.find { it.value == value }
    }
}

/** 방 홈 요약. */
data class RoomSummary(
    val title: String,
    // 방 전체 성공률 0~1 = 성공 ÷ (성공+실패)
    val roomSuccessRate: Double?,
    val remainingDays: Int,
    val participantCount: Int,
    // null 이면 무제한
    val capacity: Int?,
)

/** 방 홈 랭킹 상위 3. */
data class RoomTopRanker(
    val rank: Int,
    val user: User,
    // 성공률 0~1
    val successRate: Double,
) : UserIdentity by user {
    val userId: String get() = user.id
}

/** 챌린지 방 내부 일괄 조회. */
data class ChallengeRoom(
    // 서버 합의: 미지 값은 MEMBER 취급 (운영 스프린트의 role 값 추가에 대비)
    val myRole: MemberRole,
    // BOT 이면 "방장 되기"(선착순 클레임) 진입점을 노출한다
    val ownerType: OwnerType,
    val summary: RoomSummary,
    // 상위 3.
    val topRanking: List<RoomTopRanker>,
    val myTodayStatus: TodayVerificationStatus?,
)
