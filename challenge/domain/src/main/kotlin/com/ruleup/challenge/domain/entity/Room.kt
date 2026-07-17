package com.ruleup.challenge.domain.entity

/**
 * 내 오늘 인증 상태 (방 내부 명세 myTodayStatus — ChallengeMember.todayStatus 비정규화 값 그대로).
 */
enum class TodayVerificationStatus(
    val value: String,
) {
    SUCCESS("SUCCESS"),
    PENDING("PENDING"),
    FAILED("FAILED"),

    // 오늘이 판정 대상일이 아님 (반복 요일 밖)
    NOT_TARGET("NOT_TARGET"),
    NOT_REQUIRED("NOT_REQUIRED"),
    ;

    companion object {
        fun fromValue(value: String?): TodayVerificationStatus? = entries.find { it.value == value }
    }
}

/** 방 홈 요약 (명세 summary — 완주율·평균 매너·남은 기간·참여자 수). */
data class RoomSummary(
    val title: String,
    // 그룹 완주율 (%)
    val completionRate: Double,
    val avgMannerTemperature: Double,
    val remainingDays: Int,
    val participantCount: Int,
)

/**
 * 챌린지 방 홈 일괄 조회 (명세: GET /challenges/{id}/room). ACTIVE 멤버 전용 — 비멤버는 403.
 * 공지 쓰기 메뉴 노출 여부는 [myRole] 로 판정한다.
 */
data class ChallengeRoom(
    // 서버 합의: 미지 값은 MEMBER 취급 (운영 스프린트의 role 값 추가에 대비)
    val myRole: MemberRole,
    val summary: RoomSummary,
    // 단일 pin 정책 — 고정 공지는 항상 최대 1건
    val pinnedNotice: NoticeSummary?,
    val unreadNoticeCount: Int,
    // 상위 3
    val topRanking: List<RankingEntry>,
    val myTodayStatus: TodayVerificationStatus,
)
