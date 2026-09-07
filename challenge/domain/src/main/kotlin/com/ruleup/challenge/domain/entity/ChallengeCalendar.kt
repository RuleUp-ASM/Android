package com.ruleup.challenge.domain.entity

/**
 * 챌린지 단위 월 캘린더의 하루 상태 (명세 `days[].status` — 2026-09-07 신규).
 *
 * **계정 단위 캘린더(`/me/calendar`)와 enum 이 다르다.** 그쪽은 여러 루틴을 합산해 `ALL_DONE`·
 * `PARTIAL` 이 있지만, 한 챌린지로 좁히면 하루 판정 대상이 1건이라 부분 성공이라는 상태가
 * 성립하지 않는다. 그래서 두 타입을 공유하지 않는다 — 합치면 화면이 절대 오지 않는 값을 그리는
 * 분기를 들고 있게 된다.
 */
enum class ChallengeDayStatus(
    val value: String,
) {
    DONE("DONE"),

    FAILED("FAILED"),

    // 귀속일은 지났고 확정 전 — 유예 창. 화면은 "실패 예정"으로 말한다
    CHECKING("CHECKING"),

    // 오늘. 아직 판정되지 않았다
    IN_PROGRESS("IN_PROGRESS"),
    ;

    /** 확정된 실패가 아니다 — 유예 창은 아직 뒤집힐 수 있다(인증 정책 §2.1). */
    val isSettledFailure: Boolean
        get() = this == FAILED

    companion object {
        /**
         * 미지 값은 null — 그 칸을 비운다. 모르는 상태를 [FAILED] 로 접으면 서버가 상태를 하나
         * 늘렸을 뿐인데 사용자에게 실패했다고 말하게 된다.
         */
        fun fromValue(value: String?): ChallengeDayStatus? = entries.find { it.value == value }
    }
}

/**
 * 판정 대상일 하루 (명세 `days[]`).
 *
 * **비대상일은 배열에 아예 없다.** 주 3회 방이면 한 달에 12~13칸만 온다 — 빈 날짜를 실패로
 * 채우면 안 된다.
 */
data class ChallengeCalendarDay(
    // YYYY-MM-DD
    val date: String,
    val status: ChallengeDayStatus?,
    // 그날 인증 건. 미제출이면 null
    val verificationId: String?,
    // 이의 신청 가능 여부 — 자격·기한 판단은 서버가 한다
    val appealable: Boolean,
)

/**
 * 챌린지 월 캘린더 (명세: GET /challenges/{id}/calendar — 2026-09-07 신규).
 *
 * 솔로 상세의 월 캘린더(Figma 1134:1930)가 쓴다. **참여 중이 아니어도 완료·이탈한 방의 내 기록은
 * 조회된다** — 마이페이지 §2-1 이 완료 방의 기록 열람을 보장한다.
 */
data class ChallengeCalendar(
    val challengeId: String,
    // YYYY-MM
    val month: String,
    val days: List<ChallengeCalendarDay>,
) {
    /** 날짜로 찾는다. 판정 대상이 아닌 날은 null 이다. */
    fun dayOf(date: String): ChallengeCalendarDay? = days.find { it.date == date }
}
